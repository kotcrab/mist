package mist.symbolic

import mist.ghidra.model.GhidraType
import mist.module.ModuleSymbol

class Trace(
  val elements: List<TraceElement>,
  val executedAddresses: Set<Int>,
  val additionalAllocations: List<Pair<ModuleSymbol, GhidraType?>>,
)
