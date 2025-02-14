package mist.ghidra.model

data class GhidraRelocations(
  val relocations: List<GhidraRelocation>
)

data class GhidraRelocation(
  val address: Long,
  val status: String,
  val type: Int,
  val values: LongArray,
  val bytes: ByteArray?,
  val symbolName: String? = null
)
