package mist.module

import mist.asm.Disassembler
import mist.asm.mips.GprReg
import mist.asm.mips.MipsInstr
import mist.asm.mips.MipsOpcode
import mist.ghidra.GhidraClient
import mist.ghidra.model.GhidraFunction
import mist.ghidra.model.GhidraType
import mist.io.BinLoader

class GhidraModule(
  disassembler: Disassembler<MipsInstr>,
  moduleTypes: ModuleTypes,
  ghidraClient: GhidraClient,
  private val exportedFunctions: List<ModuleExport>,
) : Module(disassembler, moduleTypes) {
  private val ghidraFunctions = ghidraClient.getFunctions()
  private val ghidraMemoryBlocks = ghidraClient.getMemoryBlocks()
  private val ghidraSymbols = ghidraClient.getSymbols()

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

  fun registerGlobal(name: String): Pair<ModuleSymbol, GhidraType> {
    val symbol = ghidraSymbols.find { it.name == name }
      ?: error("No such symbol: $name")
    val dataType = moduleTypes.get(symbol.dataTypePathName!!)
      ?: error("No such data type: ${symbol.dataTypePathName}")
    val moduleSymbol = ModuleSymbol(symbol.name, symbol.address, dataType.length)
    registerGlobal(moduleSymbol, dataType)
    return moduleSymbol to dataType
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
}
