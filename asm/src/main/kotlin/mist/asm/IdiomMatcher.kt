package mist.asm

class IdiomMatcher<Instruction : Instr, State, Result>(
  private val maxInstrOffset: Int,
  private val stateProvider: () -> State,
  private val resultTransform: (relInstrs: List<Instruction>, State) -> Result,
  private val phases: Array<IdiomPhase<Instruction, State>>
) {
  fun matches(instrs: List<Instruction>, startIdx: Int): Result? {
    val state = stateProvider()
    val (matches, relInstrs) = phasesMatches(instrs, state, startIdx)
    if (matches) return resultTransform(relInstrs, state)
    return null
  }

  private fun phasesMatches(instrs: List<Instruction>, state: State, startIdx: Int): Pair<Boolean, List<Instruction>> {
    val relInstrs: MutableList<Instruction> = mutableListOf()
    var phaseIdx = 0
    if (phases.isEmpty()) return Pair(true, relInstrs)
    repeat(maxInstrOffset) { offset ->
      val instrIdx = startIdx - offset
      if (instrIdx < 0) return Pair(false, relInstrs)
      val instr = instrs[instrIdx]
      if (phases[phaseIdx](instr, state)) {
        relInstrs.add(instr)
        phaseIdx++
        if (phaseIdx == phases.size) return Pair(true, relInstrs)
      }
    }
    return Pair(false, relInstrs)
  }
}

typealias IdiomPhase<Instruction, State> = (Instruction, State) -> Boolean
