package mist.module

import mist.asm.Disassembler
import mist.asm.FunctionDef
import mist.asm.mips.MipsInstr
import mist.ghidra.model.GhidraType
import mist.symbolic.Context

abstract class Module(
  private val disassembler: Disassembler<MipsInstr>,
  val types: ModuleTypes
) {
  abstract val functions: Map<Int, ModuleFunction>

  private val typedGlobals = mutableListOf<Pair<ModuleSymbol, GhidraType>>()

  abstract fun createModuleMemory(): ModuleMemory

  abstract fun writeMemoryToContext(ctx: Context)

  protected fun registerGlobal(symbol: ModuleSymbol, types: GhidraType) {
    typedGlobals.add(symbol to types)
  }

  fun lookupGlobal(name: String): Pair<Int, Int> {
    val (symbol, _) = typedGlobals.find { (symbol, _) -> symbol.name == name }
      ?: error("No such typed global: $name")
    return symbol.address.toInt() to symbol.length
  }

  fun lookupGlobalMember(path: String): Pair<Int, Int> {
    val parts = path.split(".")
    val (symbol, type) = typedGlobals.find { (symbol, _) -> symbol.name == parts[0] }
      ?: error("No such typed global: ${parts[0]}")
    val (fieldOffset, fieldLength) = types.lookupStructMember(type, parts.drop(1))
    return (symbol.address + fieldOffset).toInt() to fieldLength
  }

  fun lookupAddress(address: Int, additionalAllocations: List<Pair<ModuleSymbol, GhidraType>>): ModuleAddress {
    val moduleAddress = ModuleAddress(address, null)
    val checkAddress = if (moduleAddress.isUncached()) moduleAddress.cachedAddress() else address
    val global = (typedGlobals.asSequence() + additionalAllocations).find { (symbol, type) ->
      checkAddress.toUInt() >= symbol.address.toUInt() &&
        checkAddress.toUInt() <= symbol.address.toUInt() + type.length.toUInt() - 1u
    }
    return if (global != null) {
      val (symbol, type) = global
      val localOffset = checkAddress - symbol.address.toInt()
      moduleAddress.copy(symbol = ModuleAddress.Symbol(symbol.name, localOffset, types.memberPathForOffset(type, localOffset)))
    } else {
      moduleAddress
    }
  }

  fun findTypeOrThrow(typeName: String): GhidraType {
    return types.findOrThrow(typeName)
  }

  fun getFunctionOrThrow(functionName: String): ModuleFunction {
    return getFunction(functionName) ?: error("No such function $functionName")
  }

  fun getFunction(functionName: String): ModuleFunction? {
    return functions.values.firstOrNull { it.name == functionName }
  }

  fun getFunctionByAddress(address: Int): ModuleFunction? {
    return functions.values.find {
      address.toUInt() >= it.entryPoint.toUInt() && address.toUInt() <= it.maxAddress.toUInt()
    }
  }

  fun calculateCoverages(executedAddresses: Set<Int>): Map<String, Coverage> {
    return functions.values
      .filter { it.type != ModuleFunction.Type.IMPORT }
      .sortedBy { it.entryPoint }
      .associateBy({ it.name }, { it.calculateCoverage(executedAddresses) })
  }

  fun disassembleWithCoverage(function: ModuleFunction, executedAddresses: Set<Int>): String {
    val instrs = disassembler.disassemble(
      createModuleMemory().loader,
      FunctionDef(function.name, function.entryPoint, function.length)
    ).instrs

    return instrs.joinToString("\n", postfix = "\n") {
      val prefix = if (it.addr in executedAddresses) " " else "-"
      "$prefix $it"
    }
  }
}
