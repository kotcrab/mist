package mist.io

import kio.KioInputStream
import kio.util.arrayCopy
import kio.util.toHex
import kio.util.toWHex
import mist.util.MistLogger
import mist.util.logTag
import java.nio.charset.Charset

class PspElfLoader(private val elf: ElfFile, private val logger: MistLogger) : BinLoader {
  private val tag = logTag()
  private val fragments = mutableMapOf<IntRange, MemoryFragment>()

  val memoryRanges: List<IntRange>

  init {
    preLoadCheck()
    val elfBytes = KioInputStream(elf.bytes)
    loadProgramHeaders(elfBytes)
    if (elf.header.type != ElfType.EXEC) {
      applyRelocations(elfBytes)
    }
    memoryRanges = fragments.keys.toList()
  }

  private fun preLoadCheck() {
    logger.info(tag, "load PSP elf, entry point ${elf.header.entry.toHex()}")
    if (elf.ident.clazz != 0x1 || elf.ident.data != 0x1 || elf.ident.version != 0x1 || elf.header.version != 0x1) {
      logger.warn(tag, "expected elf ident class, data and version fields to be set to 0x1, ignoring")
    }
    if (elf.ident.abi != 0x0 || elf.ident.abiVersion != 0x0) {
      logger.warn(tag, "expected elf ident abi and abi version fields to be set to 0x0, ignoring")
    }
    if (elf.header.machine != ElfMachine.MIPS) {
      logger.panic(tag, "expected elf header machine field to be MIPS")
    }
  }

  private fun loadProgramHeaders(elfBytes: KioInputStream) {
    elf.progHeaders.forEach { prog ->
      if (prog.type == ElfProgHeaderType.LOAD) {
        logger.trace(
          tag, "load program header at ${prog.vAddr.toHex()}, memSize: ${prog.memSize.toHex()}, " +
            "fileSize: ${prog.fileSize.toHex()}"
        )
        val vRange = IntRange(prog.vAddr, prog.vAddr + prog.memSize - 1)
        val vBytes = ByteArray(prog.memSize)
        elfBytes.setPos(prog.offset)
        val bytes = elfBytes.readBytes(prog.fileSize)
        arrayCopy(src = bytes, dest = vBytes)
        fragments[vRange] = MemoryFragment(vBytes)
      }
    }
  }

  private fun applyRelocations(elfBytes: KioInputStream) {
    logger.trace(tag, "elf needs relocation")
    elf.sectHeaders.forEach { sect ->
      if (sect.type == ElfSectHeaderType.REL || sect.type == ElfSectHeaderType.RELA) {
        logger.panic(tag, "expected relocation segment to be of PSP specific type but got SHL_REL or SHT_RELA")
      }
      if (sect.type != ElfSectHeaderType.PSP_RELOC) return@forEach
      if (sect.entSize != 0x8) {
        logger.panic(tag, "expected relocation entry size to be 0x8")
      }
      val entryCount = sect.size / sect.entSize
      logger.trace(
        tag,
        "apply relocations from section at ${sect.offset.toHex()}, entry count: ${entryCount.toHex()}"
      )
      elfBytes.setPos(sect.offset)
      val relocations = mutableListOf<Relocation>()
      repeat(entryCount) {
        val addr = elfBytes.readInt()
        val reloc = elfBytes.readInt()
        val type = reloc and 0xFF
        val offsetIdx = reloc shr 8 and 0xFF
        val relocateToIdx = reloc shr 16 and 0xFF
        relocations.add(
          Relocation(
            addr,
            type,
            elf.progHeaders[offsetIdx].vAddr,
            elf.progHeaders[relocateToIdx].vAddr // + 0x8804000 // image base load address can be added here
          )
        )
      }
      relocations.forEachIndexed { relocIdx, reloc ->
        val addr = reloc.addr + reloc.offset
        val data = readInt(addr)
        val relocateTo = reloc.relocTo
        var newData = 0
        when (reloc.type) {
          MipsRelocationType.MIPS_16 -> {
            newData = relocate(data, 0xFFFF, relocateTo)
          }
          MipsRelocationType.MIPS_32 -> {
            newData += relocateTo
          }
          MipsRelocationType.MIPS_26 -> {
            newData = relocate(data, 0x3FFFFFF, relocateTo shr 2) // j, jal don't include 2 lowest bits
          }
          MipsRelocationType.MIPS_HI16 -> {
            var newAddr = data shl 16
            var found = false
            for (subReloc in relocations.subList(relocIdx + 1, relocations.size)) {
              if (subReloc.type == MipsRelocationType.MIPS_LO16) {
                val loData = readInt(subReloc.addr + subReloc.offset)
                val lo = (loData and 0xFFFF).toShort() // must be treated as signed for next addition
                newAddr += lo
                newAddr += relocateTo
                val newLo = (newAddr and 0xFFFF).toShort() // must be treated as signed for next subtraction
                val newHi = (newAddr - newLo) ushr 16
                newData = (data and 0xFFFF0000.toInt()) or newHi
                found = true
                break
              }
            }
            if (found == false) {
              logger.panic(tag, "failed to relocate MIPS_HI16")
            }
          }
          MipsRelocationType.MIPS_LO16 -> {
            newData = relocate(data, 0xFFFF, relocateTo)
          }
          else -> logger.panic(tag, "unsupported relocation type: ${reloc.type.toHex()}")
        }
        if (newData != 0 && newData != data) {
          writeInt(addr, newData)
        }
      }
    }
  }

  private fun relocate(data: Int, mask: Int, relocateTo: Int): Int {
    return (data and mask.inv()) or (((data and mask) + relocateTo) and mask)
  }

  fun readByte(at: Int): Int {
    for ((range, mem) in fragments) {
      if (at in range) {
        return mem.readByte(at - range.first)
      }
    }
    logger.panic(tag, "${at.toWHex()} not mapped to any elf fragment")
  }

  private fun writeInt(at: Int, newValue: Int) {
    for ((range, mem) in fragments) {
      if (at in range) {
        mem.writeInt(at - range.first, newValue)
        return
      }
    }
    logger.panic(tag, "${at.toWHex()} not mapped to any elf fragment")
  }

  override fun readInt(at: Int): Int {
    for ((range, mem) in fragments) {
      if (at in range) {
        return mem.readInt(at - range.first)
      }
    }
    logger.panic(tag, "${at.toWHex()} not mapped to any elf fragment")
  }

  override fun readString(at: Int, charset: Charset): String {
    for ((range, mem) in fragments) {
      if (at in range) {
        return mem.readString(at - range.first, charset)
      }
    }
    logger.panic(tag, "${at.toWHex()} not mapped to any elf fragment")
  }

  class MemoryFragment(val bytes: ByteArray) {
    private val bytesReader = KioInputStream(bytes)

    fun readByte(at: Int): Int {
      if (at > bytes.size) error("read out of bounds: ${at.toWHex()}")
      return bytes[at].toInt() and 0xFF
    }

    fun writeInt(at: Int, newValue: Int) {
      if (at > bytes.size) error("write out of bounds: ${at.toWHex()}")
      bytes[at] = (newValue and 0xFF).toByte()
      bytes[at + 1] = (newValue shr 8 and 0xFF).toByte()
      bytes[at + 2] = (newValue shr 16 and 0xFF).toByte()
      bytes[at + 3] = (newValue shr 24 and 0xFF).toByte()
    }

    fun readInt(at: Int): Int {
      if (at > bytes.size) error("read out of bounds: ${at.toWHex()}")
      bytesReader.setPos(at)
      return bytesReader.readInt()
    }

    fun readString(at: Int, charset: Charset): String {
      if (at > bytes.size) error("read out of bounds: ${at.toWHex()}")
      bytesReader.setPos(at)
      return bytesReader.readNullTerminatedString(charset)
    }
  }

  data class Relocation(val addr: Int, val type: Int, val offset: Int, val relocTo: Int)
}

private object MipsRelocationType {
  const val MIPS_16 = 1
  const val MIPS_32 = 2
  const val MIPS_26 = 4
  const val MIPS_HI16 = 5
  const val MIPS_LO16 = 6
}
