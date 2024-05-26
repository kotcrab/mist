package mist.ghidra.model

data class GhidraFunctions(
  val functions: List<GhidraFunction>
)

data class GhidraFunction(
  val name: String,
  val entryPoint: Long,
  val addressRanges: List<AddressRange>,
) {
  data class AddressRange(
    val minAddress: Long,
    val maxAddress: Long,
  ) {
    fun length() = (maxAddress - minAddress + 1).toInt()
  }
}
