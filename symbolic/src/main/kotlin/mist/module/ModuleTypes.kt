package mist.module

import kio.util.toWHex
import mist.asm.mips.GprReg
import mist.ghidra.GhidraClient
import mist.ghidra.model.GhidraType
import java.util.concurrent.ConcurrentHashMap

class ModuleTypes(
  ghidraClient: GhidraClient,
) {
  companion object {
    val regsFunctionArgs = listOf(GprReg.A0, GprReg.A1, GprReg.A2, GprReg.A3, GprReg.T0, GprReg.T1, GprReg.T2, GprReg.T3)
      .map { FunctionArg.Reg(it) }
  }

  private val ghidraTypes = ghidraClient.getTypes().associateBy { it.pathName }

  private val functionTypes = ghidraTypes.values
    .filter { it.kind == GhidraType.Kind.FUNCTION_DEFINITION }
    .associateBy({ it.name }, { it })

  private val functionArgsCache = ConcurrentHashMap<String, FunctionArgs>()
  private val functionArgsOverrides = ConcurrentHashMap<String, FunctionArgs>()
  private val functionReturnSizeOverrides = ConcurrentHashMap<String, Int>()

  fun getOrThrow(pathName: String): GhidraType {
    return get(pathName)
      ?: error("No such data type $pathName")
  }

  fun get(pathName: String): GhidraType? {
    return ghidraTypes[pathName]
  }

  fun findOrThrow(typeName: String): GhidraType {
    return ghidraTypes.values.single { it.pathName.endsWith(typeName) }
  }

  fun getFunctionArgs(name: String): List<FunctionArg>? {
    val overrideFunctionArgs = functionArgsOverrides[name]
    if (overrideFunctionArgs != null) {
      return overrideFunctionArgs.args
    }
    return functionArgsCache.getOrPut(name) {
      val functionType = getFunctionType(name)
      (functionType?.properties as? GhidraType.FunctionDefinitionProperties)
        ?.parameters
        ?.map { it.dataTypePathName }
        ?.let { convertTypesToFunctionArgs(it) }
        ?: FunctionArgs.Unknown
    }.args
  }

  fun getFunctionReturnSize(name: String): Int? {
    val overrideReturnSize = functionReturnSizeOverrides[name]
    if (overrideReturnSize != null) {
      return overrideReturnSize
    }
    val functionType = getFunctionType(name)
    val returnType = (functionType?.properties as? GhidraType.FunctionDefinitionProperties)?.returnTypePathName
      ?: return null
    return getOrThrow(returnType).length.div(4)
  }

  private fun getFunctionType(name: String): GhidraType? {
    return functionTypes[name] ?: functionTypes[name.replace("_0x", "_")]
  }

  fun addFunctionArgsOverrideFromDataTypes(name: String, pathNames: List<String>) {
    functionArgsOverrides[name] = convertTypesToFunctionArgs(pathNames)
  }

  private fun convertTypesToFunctionArgs(pathNames: List<String>): FunctionArgs.Known {
    val argsSequence = sequence {
      var aligned = true
      for (arg in regsFunctionArgs) {
        yield(arg to aligned)
        aligned = !aligned
      }
      var stackOffset = 0
      while (true) {
        yield(FunctionArg.Stack(stackOffset) to aligned)
        stackOffset += 4
        aligned = !aligned
      }
    }.iterator()

    val args = mutableListOf<FunctionArg>()
    pathNames.forEach {
      val length = getOrThrow(it).length
      if (length > 8) {
        error("Function arg type $it is larger than 8 bytes, this is likely a mistake in function definition")
      }
      val (arg, aligned) = argsSequence.next()
      if (length > 4) {
        if (aligned) {
          args.add(arg)
          args.add(argsSequence.next().first)
        } else {
          // ignore unaligned arg and add next two args which will be aligned
          args.add(argsSequence.next().first)
          args.add(argsSequence.next().first)
        }
      } else {
        args.add(arg)
      }
    }
    return FunctionArgs.Known(args)

  }

  fun addFunctionReturnSizeOverride(name: String, returnSize: Int) {
    functionReturnSizeOverrides[name] = returnSize
  }

  fun memberPathForOffset(type: GhidraType, offset: Int): String {
    if (type.kind != GhidraType.Kind.STRUCTURE) {
      return "+${offset.toWHex()}"
    }
    val members = (type.properties as GhidraType.CompositeProperties).members
    val member = members.sortedByDescending { it.offset }.first { it.offset <= offset }
    val innerOffset = offset - member.offset
    return if (innerOffset == 0) {
      ".${member.fieldName}"
    } else {
      ".${member.fieldName}+${innerOffset.toWHex()}"
    }
  }

  fun lookupStructMember(type: GhidraType, path: List<String>): Pair<Int, Int> {
    return lookupStructMember(type, 0, path)
  }

  private fun lookupStructMember(type: GhidraType, offsetSum: Int, path: List<String>): Pair<Int, Int> {
    val memberName = path[0].substringBeforeLast("[")
    val arrayAccess = path[0].endsWith("]")
    val arrayIndex = if (arrayAccess) path[0].substringAfterLast("[").dropLast(1).toInt() else 0

    val fullPath by lazy { path.joinToString(separator = ".") }
    if (type.kind != GhidraType.Kind.STRUCTURE) {
      error("$memberName is not a struct in $fullPath")
    }
    val members = (type.properties as GhidraType.CompositeProperties).members
    val member = members.find { it.fieldName == memberName }
      ?: error("$memberName member doesn't exist in type ${type.name} (full path was $fullPath)")
    val memberType = get(member.typePathName!!)
      ?: error("Missing type ${member.typePathName} for member $memberName")
    val memberOffsetSum = offsetSum + member.offset
    val hasMore = path.size > 1
    val hasMoreType: GhidraType
    val arrayMemberLength = if (arrayAccess) {
      if (memberType.kind != GhidraType.Kind.ARRAY) {
        error("$memberName is not an array in $fullPath")
      }
      val arrayProps = memberType.properties as GhidraType.ArrayProperties
      hasMoreType = get(arrayProps.typePathName)
        ?: error("Missing type ${arrayProps.typePathName} for array type")
      arrayProps.elementLength
    } else {
      hasMoreType = memberType
      0
    }
    val arrayOffsetSum = memberOffsetSum + arrayMemberLength * arrayIndex

    return if (hasMore) {
      lookupStructMember(hasMoreType, arrayOffsetSum, path.drop(1))
    } else if (arrayAccess) {
      arrayOffsetSum to arrayMemberLength
    } else {
      memberOffsetSum to member.length
    }
  }

  private sealed interface FunctionArgs {
    val args: List<FunctionArg>?

    data class Known(override val args: List<FunctionArg>) : FunctionArgs

    data object Unknown : FunctionArgs {
      override val args: List<FunctionArg>? = null
    }
  }
}
