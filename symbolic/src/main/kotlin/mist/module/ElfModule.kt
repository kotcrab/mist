package mist.module

import mist.asm.Disassembler
import mist.asm.mips.MipsInstr
import mist.ghidra.model.GhidraType
import mist.io.ElfFile
import mist.io.PspElfLoader
import mist.util.DecompLog
import java.io.File

class ElfModule(
  disassembler: Disassembler<MipsInstr>,
  moduleTypes: ModuleTypes,
  elfFile: File,
  mapFile: File,
  exports: List<ModuleExport>,
  private val functionNameOverrides: Map<String, String>
) : Module(disassembler, moduleTypes) {
  private val loader = PspElfLoader(ElfFile(elfFile), DecompLog())
  private val symbols = parseSymbols(mapFile)

  override val functions = symbolsToModuleFunctions(symbols, exports)

  override fun createModuleMemory(): ModuleMemory {
    val memory = ModuleMemory()
    loader.memoryRanges.forEach { range ->
      range.forEach { at ->
        memory.writeByte(at, loader.readByte(at))
      }
    }
    return memory
  }

  fun registerGlobal(name: String, type: GhidraType) {
    val symbol = symbols.globals.find { it.name == name }
      ?: error("No such symbol: $name")
    registerGlobal(ModuleSymbol(symbol.name, symbol.address.toLong(), symbol.length), type)
  }

  private fun symbolsToModuleFunctions(symbols: Symbols, exports: List<ModuleExport>): Map<Int, ModuleFunction> {
    val moduleFunctions = mutableListOf<ModuleFunction>()
    symbols.functions.forEach {
      val type = if (exports.any { export -> export.name == it.name }) ModuleFunction.Type.EXPORT else ModuleFunction.Type.IMPLEMENTATION
      moduleFunctions.add(it.toModuleFunction(type, functionNameOverrides))
    }
    symbols.imports.forEach {
      moduleFunctions.add(it.toModuleFunction(ModuleFunction.Type.IMPORT, functionNameOverrides))
    }
    return moduleFunctions.associateBy { it.entryPoint }
  }

  private fun parseSymbols(mapFile: File): Symbols {
    val lines = mapFile.readLines()
    val functions = parseSymbolsSection(lines, ".text")
    val imports = parseSymbolsSection(lines, ".sceStub.text")
    val globals = listOf(".bss", ".data", ".rodata", ".testdata")
      .flatMap { section -> parseSymbolsSection(lines, section) }
    return Symbols(functions, imports, globals)
  }

  private fun parseSymbolsSection(lines: List<String>, section: String): List<Symbol> {
    val fromIndex = lines.indexOfFirst { it.startsWith("$section ") }
    if (fromIndex == -1) {
      return emptyList()
    }
    val sectionLines = lines.subList(
      fromIndex = fromIndex,
      toIndex = lines.size
    )
      .let {
        val sectionEndIndex = it.indexOfFirst { line -> line.isBlank() }
          .takeIf { index -> index != -1 }
        it.subList(0, sectionEndIndex ?: it.size)
      }

    val symbols = mutableListOf<Symbol>()

    val splitRegex = "\\s+".toRegex()
    var sourceAddress = 0
    var sourceLength = 0
    var source = ""
    var validSource = false
    sectionLines.forEach { line ->
      val lineParts = line.trim().split(splitRegex)
      if (line.startsWith(" $section")) {
        if (lineParts.size == 4) {
          sourceAddress = Integer.parseUnsignedInt(lineParts[1].substring(2), 16)
          sourceLength = Integer.parseUnsignedInt(lineParts[2].substring(2), 16)
          source = lineParts[3]
          validSource = true
        } else {
          validSource = false
        }
      } else if (line.startsWith("LOAD")) {
        validSource = false
      } else if (validSource && lineParts.size == 2) {
        val address = Integer.parseUnsignedInt(lineParts[0].substring(2), 16)
        val name = lineParts[1]
        symbols.add(Symbol(source, sourceAddress, sourceLength, name, address, 0))
      }
    }
    return symbols.windowed(size = 2, partialWindows = true) { elements ->
      val symbol = elements[0]
      val nextSymbol = elements.getOrNull(1)
      val nextAddress = when {
        nextSymbol == null || symbol.source != nextSymbol.source -> symbol.sourceAddress + symbol.sourceLength
        else -> nextSymbol.address
      }
      symbol.copy(length = nextAddress - symbol.address)
    }
  }

  private data class Symbols(
    val functions: List<Symbol>,
    val imports: List<Symbol>,
    val globals: List<Symbol>,
  )

  private data class Symbol(
    val source: String,
    val sourceAddress: Int,
    val sourceLength: Int,
    val name: String,
    val address: Int,
    val length: Int,
  ) {
    fun toModuleFunction(type: ModuleFunction.Type, nameOverrides: Map<String, String>): ModuleFunction {
      return ModuleFunction(nameOverrides.getOrDefault(name, name), address, address + length - 1, length, type)
    }
  }
}
