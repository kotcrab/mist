package mist.ghidra.model

import com.fasterxml.jackson.annotation.JsonAnySetter

data class GhidraTypes(
  val types: List<GhidraType>
)

data class GhidraType(
  val kind: Kind,
  val name: String,
  val displayName: String,
  val pathName: String,
  val length: Int,
  val alignedLength: Int,
  val zeroLength: Boolean,
  val notYetDefined: Boolean,
  val description: String,
  @field:JsonAnySetter
  val properties: Map<String, Any>?,
) {
  enum class Kind {
    ENUM,
    TYPEDEF,
    POINTER,
    ARRAY,
    STRUCTURE,
    UNION,
    FUNCTION_DEFINITION,
    BUILT_IN,
    UNKNOWN,
  }

  data class EnumProperties(
    val members: List<EnumMember>
  )

  data class PointerProperties(
    val typePathName: String?
  )

  data class TypedefProperties(
    val typePathName: String,
    val baseTypePathName: String,
  )

  data class ArrayProperties(
    val typePathName: String,
    val elementLength: Int,
    val elementCount: Int
  )

  data class FunctionDefinitionProperties(
    val prototypeString: String,
    val returnTypePathName: String,
    val parameters: List<Parameter>,
  ) {
    data class Parameter(
      val ordinal: Int,
      val name: String,
      val dataTypePathName: String,
    )
  }

  data class BuiltInProperties(
    val group: String
  )

  data class CompositeProperties(
    val members: List<CompositeMember>
  )

  data class EnumMember(
    val name: String,
    val value: Long,
    val comment: String
  )

  data class CompositeMember(
    val fieldName: String,
    val ordinal: Int,
    val offset: Int,
    val length: Int,
    val typePathName: String?,
    val comment: String
  )
}
