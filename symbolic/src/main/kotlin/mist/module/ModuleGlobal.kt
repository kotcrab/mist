package mist.module

import mist.ghidra.model.GhidraType

data class ModuleGlobal(
  val symbol: ModuleSymbol,
  val type: GhidraType,
  val init: Boolean
)
