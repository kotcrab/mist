package mist.ghidra.model

data class GhidraSymbols(
  val symbols: List<GhidraSymbol>
)

data class GhidraSymbol(
  val address: Long,
  val name: String,
  val type: String,
  val global: Boolean,
  val primary: Boolean,
  val pinned: Boolean,
  val externalEntryPoint: Boolean,
  val source: String,
  val preComment: String,
  val dataTypePathName: String?,
)
