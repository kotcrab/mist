package mist.module

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kio.util.toWHex
import mist.asm.FunctionDef
import mist.asm.mips.GprReg
import mist.asm.mips.MipsOpcode
import mist.asm.mips.allegrex.AllegrexDisassembler
import mist.ghidra.GhidraClient
import mist.ghidra.model.GhidraFunction
import mist.ghidra.model.GhidraSymbol
import mist.ghidra.model.GhidraType
import mist.simulator.Context
import mist.simulator.ContextBinLoader
import mist.simulator.Interpreter
import mist.simulator.Trace
import mist.util.commonObjectMapper
import kotlin.random.Random

class Module(
  private val ghidraClient: GhidraClient,
  private val exportedFunctions: List<ModuleExport>,
  private val objectMapper: ObjectMapper = commonObjectMapper,
) {
  private val disasm = AllegrexDisassembler()

  private val ghidraFunctions = ghidraClient.getFunctions()
  private val ghidraMemoryBlocks = ghidraClient.getMemoryBlocks()
  private val ghidraSymbols = ghidraClient.getSymbols()
  private val ghidraTypes = ghidraClient.getTypes()
  private val ghidraTypesByPath = ghidraTypes.associateBy { it.pathName }

  private val initialMemory = ghidraMemoryBlocks
    .filter { it.initialized && it.addressSpaceName == "ram" }
    .map { it.start.toInt() to ghidraClient.getMemory(it.start, it.size.toInt()) }

  private val knownSymbols = mutableListOf<Pair<GhidraSymbol, GhidraType>>()

  val functions: Map<Int, ModuleFunction> = run {
    val context = createContext()
    val loader = ContextBinLoader(context)
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
          type = classifyFunction(loader, it),
        )
      }
      .sortedBy { it.entryPoint }
      .associateBy { it.entryPoint }
  }

  private val functionTypes = run {
    ghidraTypes.filter { it.kind == GhidraType.Kind.FUNCTION_DEFINITION }
      .associateBy(
        { it.name }, {
          Pair(it, objectMapper.convertValue<GhidraType.FunctionDefinitionProperties>(it.properties!!))
        }
      )
  }

  fun registerGlobal(name: String) {
    val symbol = ghidraSymbols.find { it.name == name }
      ?: error("Symbol '$name' not found")
    val dataType = ghidraTypes.find { it.pathName == symbol.dataTypePathName }
      ?: error("Data type '${symbol.dataTypePathName}' not found")
    knownSymbols.add(symbol to dataType)
  }


  fun lookupSymbolAddress(name: String): Int {
    val (symbol, _) = knownSymbols.find { (symbol, _) -> symbol.name == name }
      ?: error("No such symbol: $name")
    return symbol.address.toInt()
  }

  fun lookupSymbolFieldAddress(path: String): Pair<Int, Int> {
    val parts = path.split(".")
    val (symbol, type) = knownSymbols.find { (symbol, _) -> symbol.name == parts[0] }
      ?: error("No such symbol: ${parts[0]}")
    val (fieldOffset, fieldLength) = lookupStructField(path, type, 0, parts.drop(1))
    return (symbol.address + fieldOffset).toInt() to fieldLength
  }

  private fun lookupStructField(path: String, type: GhidraType, offsetSum: Int, parts: List<String>): Pair<Int, Int> {
    val memberName = parts[0].substringBeforeLast("[")
    val arrayAccess = parts[0].endsWith("]")
    val arrayIndex = if (arrayAccess) parts[0].substringAfterLast("[").dropLast(1).toInt() else 0

    return if (type.kind == GhidraType.Kind.STRUCTURE) {
      val members = objectMapper.convertValue<GhidraType.CompositeProperties>(type.properties!!).members
      val member = members.find { it.fieldName == memberName }
        ?: error("No such field: $memberName in $path")
      val memberType = ghidraTypesByPath[member.typePathName]
        ?: error("No such type: ${member.typePathName}")
      val hasMore = parts.size > 1
      val memberOffsetSum = offsetSum + member.offset
      val hasMoreType: GhidraType
      val arrayMemberLength = if (arrayAccess) {
        if (memberType.kind != GhidraType.Kind.ARRAY) {
          error("Not a array: $memberName in $path")
        }
        val arrayProps = objectMapper.convertValue<GhidraType.ArrayProperties>(memberType.properties!!)
        hasMoreType = ghidraTypesByPath[arrayProps.typePathName]
          ?: error("No such type: ${arrayProps.typePathName}")
        arrayProps.elementLength
      } else {
        hasMoreType = memberType
        0
      }
      val arrayOffsetSum = memberOffsetSum + arrayMemberLength * arrayIndex

      if (hasMore) {
        lookupStructField(path, hasMoreType, arrayOffsetSum, parts.drop(1))
      } else if (arrayAccess) {
        arrayOffsetSum to arrayMemberLength
      } else {
        memberOffsetSum to member.length
      }
    } else {
      error("Not a struct: $memberName in $path")
    }
  }

  fun lookupAddress(address: Int): String {
    var uncachedPrefix = ""
    var uncachedSuffix = ""
    var checkAddress = address
    if ((checkAddress and 0x1FFFFFFF.inv()) == 0xA0000000.toInt()) { // KUNCACHED address
      checkAddress = checkAddress and 0x1FFFFFFF or 0x80000000.toInt()
      uncachedPrefix = "KUNCACHED("
      uncachedSuffix = ")"
    }
    val pair = knownSymbols.find { (symbol, dataType) ->
      checkAddress.toUInt() >= symbol.address.toUInt() &&
        checkAddress.toUInt() <= symbol.address.toUInt() + dataType.length.toUInt()
    }
    val result = if (pair != null) {
      val (symbol, type) = pair
      val localOffset = checkAddress - symbol.address.toInt()
      if (type.kind == GhidraType.Kind.STRUCTURE) {
        val members = objectMapper.convertValue<GhidraType.CompositeProperties>(type.properties!!).members
        val member = members.sortedByDescending { it.offset }.first { it.offset <= localOffset }
        val innerOffset = localOffset - member.offset
        if (innerOffset == 0) {
          "${symbol.name}.${member.fieldName}"
        } else {
          "${symbol.name}.${member.fieldName}+${innerOffset.toWHex()}"
        }
      } else {
        "${symbol.name}+${localOffset.toWHex()}"
      }
    } else {
      address.toWHex()
    }
    return "$uncachedPrefix$result$uncachedSuffix"
  }

  fun getFunctionOrThrow(functionName: String): ModuleFunction {
    return getFunction(functionName) ?: error("No such function $functionName")
  }

  fun getFunction(functionName: String): ModuleFunction? {
    return functions.values.firstOrNull { it.name == functionName }
  }

  fun getFunctionTypeArgumentCount(name: String): Int? {
    return (functionTypes[name] ?: functionTypes[name.replace("_0x", "_")])
      ?.second?.parameters?.size
  }

  fun functionForAddress(address: Int): ModuleFunction? {
    return functions.values.find {
      address.toUInt() >= it.entryPoint.toUInt() && address.toUInt() <= it.maxAddress.toUInt()
    }
  }

  fun execute(address: Int, configureCtx: Context.() -> Unit): Trace {
    val context = createContext()
    context.configureCtx()
    val binLoader = ContextBinLoader(context)
    return Interpreter(binLoader, disasm, this, context)
      .execute(address)
  }

  private fun classifyFunction(loader: ContextBinLoader, function: GhidraFunction): ModuleFunction.Type {
    val firstInstruction = disasm.disassembleInstruction(loader, function.entryPoint.toInt())
    return when {
      exportedFunctions.any {
        val endsWithNid = !function.name.startsWith("FUN_", ignoreCase = true) &&
          function.name.endsWith("_${it.nid}", ignoreCase = true)
        function.name == it.name || endsWithNid
      } -> {
        ModuleFunction.Type.EXPORT
      }
      firstInstruction.opcode == MipsOpcode.Jr && firstInstruction.op0AsReg() == GprReg.Ra -> {
        ModuleFunction.Type.IMPORT
      }
      else -> {
        ModuleFunction.Type.IMPLEMENTATION
      }
    }
  }

  private fun createContext(): Context {
    val context = Context()
    // might optimize this
    initialMemory.forEach { (start, data) ->
      data.forEachIndexed { index, byte ->
        context.memory.writeByte(start + index, byte.toInt())
      }
    }
    knownSymbols.forEach { (symbol, type) ->
      val random = Random(symbol.name.hashCode())
      val address = symbol.address.toInt()
      repeat(type.length) {
        context.memory.writeByte(address + it, random.nextInt())
      }
    }
    context.writeGpr(GprReg.A0, 0x3)
    context.writeGpr(GprReg.A1, 0x4)
    context.writeGpr(GprReg.A2, 0x5)
    context.writeGpr(GprReg.A3, 0x6)
    context.writeGpr(GprReg.T0, 0x7)
    context.writeGpr(GprReg.T1, 0x8)
    context.writeGpr(GprReg.T2, 0x9)
    context.writeGpr(GprReg.T3, 0xA)
    // SP will be initialed to random by memory
    return context
  }

  fun calculateCoverages(executedAddresses: Set<Int>): Map<String, Coverage> {
    return functions.values
      .filter { it.type != ModuleFunction.Type.IMPORT }
      .sortedBy { it.entryPoint }
      .associateBy({ it.name }, { it.calculateCoverage(executedAddresses) })
  }

  fun disassembleWithCoverage(function: ModuleFunction, executedAddresses: MutableSet<Int>): String {
    val context = createContext()
    val loader = ContextBinLoader(context)
    val disasm = disasm.disassemble(loader, FunctionDef(function.name, function.entryPoint, function.length))

    return disasm.instr.joinToString("\n", postfix = "\n") {
      val prefix = if (it.addr in executedAddresses) "  " else "- "
      "$prefix$it"
    }
  }
}
