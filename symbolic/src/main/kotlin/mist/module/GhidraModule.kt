package mist.module

import kio.KioInputStream
import kio.KioOutputStream
import kio.LERandomAccessFile
import kio.util.align
import kio.util.toHex
import kio.util.toWHex
import kio.util.writeString
import mist.asm.Disassembler
import mist.asm.mips.GprReg
import mist.asm.mips.MipsInstr
import mist.asm.mips.MipsOpcode
import mist.asm.mips.allegrex.AllegrexDisassembler
import mist.ghidra.GhidraClient
import mist.ghidra.model.GhidraFunction
import mist.ghidra.model.GhidraSymbol
import mist.io.BinLoader
import mist.symbolic.Context
import mist.symbolic.Expr
import java.io.ByteArrayOutputStream
import java.io.File

class GhidraModule(
  disassembler: Disassembler<MipsInstr>,
  moduleTypes: ModuleTypes,
  private val ghidraClient: GhidraClient,
  private val exportedFunctions: List<ModuleExport>,
) : Module(disassembler, moduleTypes) {
  private val ghidraFunctions = ghidraClient.getFunctions()
  private val ghidraMemoryBlocks = ghidraClient.getMemoryBlocks()
  private val ghidraSymbols = ghidraClient.getSymbols()
  private val ghidraRelocations by lazy { ghidraClient.getRelocations() }

  private val initialMemory = ghidraMemoryBlocks
    .filter { it.initialized && it.addressSpaceName == "ram" }
    .map { it.start.toInt() to ghidraClient.getMemory(it.start, it.size.toInt()) }

  override val functions: Map<Int, ModuleFunction> = run {
    val loader = createModuleMemory().loader
    ghidraFunctions
      .map {
        if (it.addressRanges.size > 1) {
          println("WARN: Multiple address ranges in function: ${it.name}")
        }
        ModuleFunction(
          name = it.name,
          entryPoint = it.entryPoint.toInt(),
          maxAddress = it.addressRanges.first().maxAddress.toInt(),
          length = it.addressRanges.first().length(),
          type = classifyFunction(disassembler, loader, it),
        )
      }
      .sortedBy { it.entryPoint }
      .associateBy { it.entryPoint }
  }

  fun registerGlobal(name: String, init: Boolean): ModuleGlobal {
    val symbol = ghidraSymbols.find { it.name == name }
      ?: error("No such symbol: $name")
    val dataType = types.getOrThrow(symbol.dataTypePathName!!)
    val moduleSymbol = ModuleSymbol(symbol.name, symbol.address.toInt(), dataType.length)
    return registerGlobal(moduleSymbol, dataType, init)
  }

  private fun classifyFunction(disassembler: Disassembler<MipsInstr>, loader: BinLoader, function: GhidraFunction): ModuleFunction.Type {
    val firstInstruction = disassembler.disassembleInstruction(loader, function.entryPoint.toInt())
    val secondInstruction by lazy { disassembler.disassembleInstruction(loader, function.entryPoint.toInt() + 4) }
    return when {
      exportedFunctions.any {
        val endsWithNid = !function.name.startsWith("FUN_", ignoreCase = true) &&
          function.name.endsWith("_${it.nid}", ignoreCase = true)
        function.name == it.name || endsWithNid
      } -> {
        ModuleFunction.Type.EXPORT
      }
      firstInstruction.opcode == MipsOpcode.Jr && firstInstruction.op0AsReg() == GprReg.Ra && secondInstruction.opcode == MipsOpcode.Nop -> {
        ModuleFunction.Type.IMPORT
      }
      else -> {
        ModuleFunction.Type.IMPLEMENTATION
      }
    }
  }

  fun getGhidraFunctionOrNull(name: String): GhidraFunction? {
    return ghidraFunctions.firstOrNull { it.name == name }
  }

  override fun createModuleMemory(): ModuleMemory {
    val memory = ModuleMemory()
    initialMemory.forEach { (start, data) ->
      data.forEachIndexed { index, byte ->
        memory.writeByte(start + index, byte.toInt())
      }
    }
    return memory
  }

  override fun writeMemoryToContext(ctx: Context) {
    initialMemory.forEach { (start, data) ->
      data.forEachIndexed { index, byte ->
        ctx.memory.writeByte(Expr.Const.of(start + index), Expr.Const.of(byte.toInt()))
      }
    }
  }

  fun delink(outFile: File, externGlobals: Set<String>) {
    val memory = createModuleMemory()

    // Undo relocations
    ghidraRelocations.forEach { reloc ->
      (reloc.bytes ?: error("Missing original bytes for relocation")).forEachIndexed { index, byte ->
        memory.writeByte((reloc.address + index).toInt(), byte.toInt())
      }
    }

    // Convert to zero base
    val baseAddress = initialMemory.minOf { it.first }
    val maxAddress = initialMemory.maxOf { it.first + it.second.size }
    val programBytes = ByteArray(maxAddress - baseAddress)
    repeat(programBytes.size) { index ->
      programBytes[index] = memory.readByte(baseAddress + index).toByte()
    }

    // Prepare symbols
    val strTab = ElfStringAllocator()

    val combinedSectionSymbol = ElfSymbol(".combined", strTab.getOrPut(".combined"), 0, 0, 3, 0, 1) // LOCAL | SECTION
    val weakFunctionSymbols = functions.values
      .filter { it.type == ModuleFunction.Type.EXPORT || it.type == ModuleFunction.Type.IMPLEMENTATION }
      .map { ElfSymbol(it.name, strTab.getOrPut(it.name), it.entryPoint - baseAddress, it.length, 0x22, 0, 1) } // WEAK | FUNC
    val externFunctionSymbols = functions.values
      .filter { it.type == ModuleFunction.Type.IMPORT }
      .map { ElfSymbol(it.name, strTab.getOrPut(it.name), 0, 0, 0x10, 0, 0) } // GLOBAL | NOTYPE
    val externDataSymbols = ghidraSymbols.filter { it.name in externGlobals }
      .map { ElfSymbol(it.name, strTab.getOrPut(it.name), 0, 0, 0x10, 0, 0) } // GLOBAL | NOTYPE

    val symbols = listOf(ElfSymbol("", 0, 0, 0, 0, 0, 0), combinedSectionSymbol) + weakFunctionSymbols + externFunctionSymbols + externDataSymbols
//    val assumedTextEnd = functions.values.maxOf { it.entryPoint + it.length + 1 }

    val diasm = AllegrexDisassembler()
    // Convert relocations
    val elfRelocations = mutableListOf<ElfAllegrexRelocation>()
    val relocs = RelocParser.parseRelocationsTypeB(KioInputStream(ghidraClient.getMemory("segment_2::0x0", 0x3e2)), 2, listOf(0, 0x3f80), false)
      .let {
        val mutReloc = it.toMutableList()
        mutReloc.add(11, mutReloc.removeAt(18)) // need to move one LO reloc to match with the actual HI reloc :/
        mutReloc.toList()
      }
    var lastHiImm: Int? = null
    var lastTargetSymbol: GhidraSymbol? = null

    relocs.forEachIndexed { index, reloc ->
      println("$index ${reloc.offset.toWHex()} $reloc")
      when (reloc.type) {
        AllegrexElfRelocationConstants.R_MIPS_32 -> elfRelocations.add( // TODO this ignores address base
          ElfAllegrexRelocation(
            reloc.offset,
            AllegrexElfRelocationConstants.R_MIPS_32,
            1
          )
        )
        AllegrexElfRelocationConstants.R_MIPS_X_HI16 -> {
          val origInstr = diasm.disassembleInstruction(memory.loader, reloc.offset + baseAddress)
          if (origInstr.opcode != MipsOpcode.Lui) {
            error("Unexpected HI16 opcode: $origInstr")
          }
          val target = origInstr.op1AsImm() + reloc.addressBase + reloc.addend
          lastHiImm = origInstr.op1AsImm()
          val targetSymbol = ghidraSymbols.firstOrNull { it.address == target + baseAddress.toLong() }
            ?: error("Unknown symbol for $reloc")
          lastTargetSymbol = targetSymbol
          val symbolIndex = symbols.indexOfFirst { it.name == targetSymbol.name }
          if (symbolIndex != -1) {
            // remap to global symbol so need to clear immediate value
            println("Mapping to global ${targetSymbol.name}")
            programBytes[reloc.offset] = 0
            programBytes[reloc.offset + 1] = 0
            elfRelocations.add(
              ElfAllegrexRelocation(
                reloc.offset,
                AllegrexElfRelocationConstants.R_MIPS_HI16,
                symbolIndex
              )
            )
          } else {
            // local symbol
            if (target ushr 16 != origInstr.op1AsImm().toLong()) {
              error("Unimplemented, need to modify instr")
            }
            println("Leaving as local ${targetSymbol.name}")
            elfRelocations.add(
              ElfAllegrexRelocation(
                reloc.offset,
                AllegrexElfRelocationConstants.R_MIPS_HI16,
                1
              )
            )
          }
        }
        AllegrexElfRelocationConstants.R_MIPS_LO16 -> {
          val hiImm = lastHiImm
            ?: error("Missing HI reloc for LO reloc")
          val origInstr = diasm.disassembleInstruction(memory.loader, reloc.offset + baseAddress)
          if (origInstr.opcode !in listOf(MipsOpcode.Lw, MipsOpcode.Sw, MipsOpcode.Addi, MipsOpcode.Addiu)) {
            error("Unexpected LO16 opcode: $origInstr")
          }
          val target = hiImm + reloc.addressBase + origInstr.op2AsImm()
          val targetSymbol = ghidraSymbols.firstOrNull { it.address == target + baseAddress.toLong() }
            ?: error("Unknown symbol for $reloc")
          println("$origInstr ${origInstr.op2AsImm()}")
          if (lastTargetSymbol != targetSymbol) {
            error("HI/LO mismatched symbols: $lastTargetSymbol != $targetSymbol")
          }
          val symbolIndex = symbols.indexOfFirst { it.name == targetSymbol.name }

          if (symbolIndex != -1) {
            // remap to global symbol so need to clear immediate value
            println("Mapping to global ${targetSymbol.name}")
            programBytes[reloc.offset] = 0
            programBytes[reloc.offset + 1] = 0
            elfRelocations.add(
              ElfAllegrexRelocation(
                reloc.offset,
                AllegrexElfRelocationConstants.R_MIPS_LO16,
                symbolIndex
              )
            )
          } else {
            // local symbol
            val targetLow = target
            programBytes[reloc.offset] = targetLow.toByte()
            programBytes[reloc.offset + 1] = (targetLow ushr 8).toByte()
            println("Leaving as local ${targetSymbol.name}")
            elfRelocations.add(
              ElfAllegrexRelocation(
                reloc.offset,
                AllegrexElfRelocationConstants.R_MIPS_LO16,
                1
              )
            )
          }

        }
        AllegrexElfRelocationConstants.R_MIPS_X_J26 -> elfRelocations.add(
          ElfAllegrexRelocation(
            reloc.offset,
            AllegrexElfRelocationConstants.R_MIPS_26,
            1
          )
        )
        AllegrexElfRelocationConstants.R_MIPS_X_JAL26 -> {
          if (reloc.addressBase != 0L || reloc.offsetBase != 0L) {
            error("Not implemented base != 0: $reloc")
          }
          programBytes[reloc.offset] = 0
          programBytes[reloc.offset + 1] = 0
          programBytes[reloc.offset + 2] = 0
          programBytes[reloc.offset + 3] = 0xC
          val origInstr = diasm.disassembleInstruction(memory.loader, reloc.offset + baseAddress)
          if (origInstr.opcode != MipsOpcode.Jal) {
            error("Unexpected X_JAL26 opcode: $origInstr")
          }
          val target = origInstr.op0AsImm()
          val targetSymbol = ghidraSymbols.firstOrNull { it.address == target + baseAddress.toLong() }
            ?: error("Unknown symbol for $reloc")
          val symbolIndex = symbols.indexOfFirst { it.name == targetSymbol.name }
          if (symbolIndex <= 0) {
            error("No exported symbol for $reloc")
          }
          elfRelocations.add(
            ElfAllegrexRelocation(
              reloc.offset,
              AllegrexElfRelocationConstants.R_MIPS_26,
              symbolIndex
            )
          )
        }
        else -> error("Unsupported relocation type: ${reloc.type.toHex()} at ${reloc.offset.toWHex()} ($reloc)")
      }
    }

    // Prepare sections
    val shStrTab = ElfStringAllocator()

    val sections = listOf(
      ElfSection(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // UNDEF
      ElfSection(shStrTab.getOrPut(".combined"), 1, 7, 0, -1, -1, 0, 0, 4, 0),
      ElfSection(shStrTab.getOrPut(".rel.combined"), 9, 0x40, 0, -1, -1, 3, 1, 4, 8),
      ElfSection(shStrTab.getOrPut(".symtab"), 2, 0, 0, -1, -1, 4, 0xB, 4, 0x10),
      ElfSection(shStrTab.getOrPut(".strtab"), 3, 0, 0, -1, -1, 0, 0, 1, 0),
      ElfSection(shStrTab.getOrPut(".shstrtab"), 3, 0, 0, -1, -1, 0, 0, 1, 0),
    )

    // Write object file
    LERandomAccessFile(outFile).use {
      it.setLength(0)
      // 0x00
      it.writeByte(0x7F)
      it.writeString("ELF")
      it.write(byteArrayOf(0x01, 0x01, 0x01, 0x00))
      it.writeInt(0)
      it.writeInt(0)

      // 0x10
      it.writeShort(0x01)
      it.writeShort(0x08)
      it.writeInt(1)
      it.writeInt(0)
      it.writeInt(0)

      // 0x20
      val sectionHeaderOffset = it.filePointer
      it.writeInt(0)
      it.writeInt(0x10A23001)
      it.writeShort(0x34)
      it.writeShort(0)
      it.writeShort(0)
      it.writeShort(0x28)

      // 0x30
      it.writeShort(sections.size)
      it.writeShort(sections.lastIndex) // .shstrtab index

      // 0x34
      val programStart = it.filePointer
      it.write(programBytes)
      it.align(4)

      val programRelStart = it.filePointer
      elfRelocations.forEach { relocation ->
        it.writeInt(relocation.offset)
        it.writeByte(relocation.type)
        it.writeByte(relocation.symbol)
        it.writeByte(relocation.symbol shr 8)
        it.writeByte(relocation.symbol shr 16)
      }

      val symTabStart = it.filePointer
      symbols.forEach { symbol ->
        it.writeInt(symbol.nameOffset)
        it.writeInt(symbol.value)
        it.writeInt(symbol.size)
        it.writeByte(symbol.info.toInt())
        it.writeByte(symbol.other.toInt())
        it.writeShort(symbol.sectionIndex)
      }

      val strTabStart = it.filePointer
      val strTabBytes = strTab.toByteArray()
      it.write(strTabBytes)
      it.align(4)

      val shStrTabStart = it.filePointer
      val shStrTabBytes = shStrTab.toByteArray()
      it.write(shStrTabBytes)
      it.align(4)

      // Write sections
      val sectionsStart = it.filePointer

      it.seek(sectionHeaderOffset)
      it.writeInt(sectionsStart.toInt())
      it.seek(sectionsStart)

      sections.forEach { section ->
        it.writeInt(section.name)
        it.writeInt(section.type)
        it.writeInt(section.flags)
        it.writeInt(section.address)

        if (section.offset == -1) {
          it.writeInt(
            when (shStrTab.lookup(section.name)) {
              ".combined" -> programStart
              ".rel.combined" -> programRelStart
              ".symtab" -> symTabStart
              ".strtab" -> strTabStart
              ".shstrtab" -> shStrTabStart
              else -> error("Unknown section: ${section.name}")
            }.toInt()
          )
        } else {
          it.writeInt(section.offset)
        }

        if (section.size == -1) {
          it.writeInt(
            when (shStrTab.lookup(section.name)) {
              ".combined" -> programBytes.size
              ".rel.combined" -> elfRelocations.size * 8
              ".symtab" -> symbols.size * 0x10
              ".strtab" -> strTabBytes.size
              ".shstrtab" -> shStrTabBytes.size
              else -> error("Unknown section: ${section.name}")
            }.toInt()
          )
        } else {
          it.writeInt(section.size)
        }

        it.writeInt(section.link)
        it.writeInt(section.info)
        it.writeInt(section.addressAlign)
        it.writeInt(section.entrySize)
      }
    }
  }
}

private class ElfStringAllocator {
  val cache = mutableMapOf<String, Int>()
  val bytes = KioOutputStream(ByteArrayOutputStream())

  init {
    bytes.writeByte(0)
  }

  fun getOrPut(text: String): Int {
    return cache.getOrPut(text) {
      val pos = bytes.pos()
      bytes.writeNullTerminatedString(text)
      pos
    }
  }

  fun lookup(address: Int): String {
    return cache.entries.first { it.value == address }.key
  }

  fun toByteArray(): ByteArray {
    return bytes.getAsByteArrayOutputStream().toByteArray()
  }
}

private data class ElfAllegrexRelocation(
  val offset: Int,
  val type: Int,
  val symbol: Int,
)

private data class ElfSymbol(
  val name: String,
  val nameOffset: Int,
  val value: Int,
  val size: Int,
  val info: Byte,
  val other: Byte,
  val sectionIndex: Int
)

private data class ElfSection(
  val name: Int,
  val type: Int,
  val flags: Int,
  val address: Int,
  val offset: Int,
  val size: Int,
  val link: Int,
  val info: Int,
  val addressAlign: Int,
  val entrySize: Int,
)
