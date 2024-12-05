package mist.module

import mist.asm.Disassembler
import mist.asm.mips.GprReg
import mist.asm.mips.MipsInstr
import mist.asm.mips.MipsOpcode
import mist.ghidra.GhidraClient
import mist.ghidra.model.GhidraFunction
import mist.io.BinLoader
import mist.symbolic.Context
import mist.symbolic.Expr

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
    return ModuleMemory.fromData(initialMemory)
  }

  override fun writeMemoryToContext(ctx: Context) {
    initialMemory.forEach { (start, data) ->
      data.forEachIndexed { index, byte ->
        ctx.memory.writeByte(Expr.Const.of(start + index), Expr.Const.of(byte.toInt()))
      }
    }
  }
}
