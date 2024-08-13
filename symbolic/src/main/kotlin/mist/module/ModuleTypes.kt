package mist.module

import kio.util.toWHex
import mist.ghidra.GhidraClient
import mist.ghidra.model.GhidraType
import java.util.concurrent.ConcurrentHashMap

class ModuleTypes(
  ghidraClient: GhidraClient,
) {
  private val ghidraTypes = ghidraClient.getTypes().associateBy { it.pathName }

  private val functionTypes = ghidraTypes.values
    .filter { it.kind == GhidraType.Kind.FUNCTION_DEFINITION }
    .associateBy({ it.name }, { it })

  private val functionArgsCountOverrides = ConcurrentHashMap<String, Int>()
  private val functionReturnSizeOverrides = ConcurrentHashMap<String, Int>()

  fun get(pathName: String): GhidraType? {
    return ghidraTypes[pathName]
  }

  fun findOrThrow(typeName: String): GhidraType {
    return ghidraTypes.values.single { it.pathName.endsWith(typeName) }
  }

  fun getFunctionArgsCount(name: String): Int? {
    val overrideArgsCount = functionArgsCountOverrides[name]
    if (overrideArgsCount != null) {
      return overrideArgsCount
    }
    val functionType = getFunctionType(name)
    return (functionType?.properties as? GhidraType.FunctionDefinitionProperties)?.parameters?.size
  }

  fun getFunctionReturnSize(name: String): Int? {
    val overrideReturnSize = functionReturnSizeOverrides[name]
    if (overrideReturnSize != null) {
      return overrideReturnSize
    }
    val functionType = getFunctionType(name)
    val returnType = (functionType?.properties as? GhidraType.FunctionDefinitionProperties)?.returnTypePathName ?: return null
    return get(returnType)?.length?.div(4)
  }

  private fun getFunctionType(name: String): GhidraType? {
    return functionTypes[name] ?: functionTypes[name.replace("_0x", "_")]
  }

  fun addFunctionArgsCountOverride(name: String, argsCount: Int) {
    functionArgsCountOverrides[name] = argsCount
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
      ?: error("$memberName doesn't exist in $fullPath")
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
}
