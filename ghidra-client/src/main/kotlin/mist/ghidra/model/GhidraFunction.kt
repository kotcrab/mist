package mist.ghidra.model

data class GhidraFunctions(
  val functions: List<GhidraFunction>
)

data class GhidraFunction(
  val name: String,
  val returnTypePathName: String,
  val entryPoint: Long,
  val parameters: List<Parameter>,
  val hasVarArgs: Boolean,
  val addressRanges: List<AddressRange>,
) {
  data class AddressRange(
    val minAddress: Long,
    val maxAddress: Long,
  ) {
    fun length() = (maxAddress - minAddress + 1).toInt()
  }

  data class Parameter(
    val ordinal: Int,
    val name: String,
    val dataTypePathName: String,
  )
}
