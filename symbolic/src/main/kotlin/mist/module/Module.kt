package mist.module

import mist.asm.Disassembler
import mist.asm.FunctionDef
import mist.asm.mips.MipsInstr
import mist.ghidra.model.GhidraType
import mist.symbolic.Context
import mist.symbolic.Expr

abstract class Module(
  private val disassembler: Disassembler<MipsInstr>,
  val types: ModuleTypes
) {
  abstract val functions: Map<Int, ModuleFunction>

  private val typedGlobals = mutableListOf<ModuleGlobal>()

  abstract fun createModuleMemory(): ModuleMemory

  abstract fun writeMemoryToContext(ctx: Context)

  fun writeGlobalsToContext(ctx: Context) {
    val moduleMemory = createModuleMemory()
    typedGlobals
      .filter { it.init }
      .forEach { (symbol) ->
        repeat(symbol.length) { offset ->
          val at = symbol.address + offset
          ctx.memory.writeByte(Expr.Const.of(at), Expr.Const.of(moduleMemory.readByte(at)))
        }
      }
  }

  protected fun registerGlobal(symbol: ModuleSymbol, types: GhidraType, init: Boolean): ModuleGlobal {
    val global = ModuleGlobal(symbol, types, init)
    typedGlobals.add(global)
    return global
  }

  fun globals(): List<ModuleGlobal> {
    return typedGlobals.toList()
  }

  fun lookupGlobal(name: String): Pair<Int, Int> {
    val (symbol) = typedGlobals.find { it.symbol.name == name }
      ?: error("No such typed global: $name")
    return symbol.address to symbol.length
  }

  fun lookupGlobalMember(path: String): Pair<Int, Int> {
    val parts = path.split(".")
    val (symbol, type) = typedGlobals.find { it.symbol.name == parts[0] }
      ?: error("No such typed global: ${parts[0]}")
    val (fieldOffset, fieldLength) = types.lookupStructMember(type, parts.drop(1))
    return (symbol.address + fieldOffset) to fieldLength
  }

  fun lookupAddress(address: Int, additionalAllocations: List<Pair<ModuleSymbol, GhidraType?>>): ModuleAddress {
    val moduleAddress = ModuleAddress(address, null)
    val checkAddress = if (moduleAddress.isUncached()) moduleAddress.cachedAddress() else address
    val typedSymbol = (typedGlobals.map { it.symbol to it.type } + additionalAllocations).find { (symbol) ->
      checkAddress.toUInt() >= symbol.address.toUInt() &&
        checkAddress.toUInt() <= symbol.address.toUInt() + symbol.length.toUInt() - 1u
    }
    if (typedSymbol == null) {
      return moduleAddress
    }
    val (symbol, type) = typedSymbol
    if (type == null) {
      return moduleAddress
    }
    val localOffset = checkAddress - symbol.address
    // TODO member path won't work for array
    return moduleAddress.copy(symbol = ModuleAddress.Symbol(symbol.name, localOffset, types.memberPathForOffset(type, localOffset)))
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

  fun calculateCoverages(executedAddresses: Set<Int>, excludedFunctions: Set<String>): Map<String, Coverage> {
    return functions.values
      .filter { it.type != ModuleFunction.Type.IMPORT }
      .filterNot { it.name in excludedFunctions }
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
