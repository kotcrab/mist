package mist.ghidra.model

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore

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
  val rawProperties: Map<String, Any> = mutableMapOf(),
  @JsonIgnore
  val properties: Properties? = null
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
  ) : Properties

  data class PointerProperties(
    val typePathName: String?
  ) : Properties

  data class TypedefProperties(
    val typePathName: String,
    val baseTypePathName: String,
  ) : Properties

  data class ArrayProperties(
    val typePathName: String,
    val elementLength: Int,
    val elementCount: Int
  ) : Properties

  data class FunctionDefinitionProperties(
    val prototypeString: String,
    val returnTypePathName: String,
    val parameters: List<Parameter>,
  ) : Properties {
    data class Parameter(
      val ordinal: Int,
      val name: String,
      val dataTypePathName: String,
    )
  }

  data class BuiltInProperties(
    val group: String
  ) : Properties

  data class CompositeProperties(
    val members: List<CompositeMember>
  ) : Properties

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

  sealed interface Properties
}
