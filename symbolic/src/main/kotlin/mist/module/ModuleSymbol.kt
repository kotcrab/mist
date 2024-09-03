package mist.module

data class ModuleSymbol(
  val name: String,
  val address: Long, // TODO int?
  val length: Int
)
