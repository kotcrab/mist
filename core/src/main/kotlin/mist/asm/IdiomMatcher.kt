/*
 * mist - interactive disassembler and decompiler
 * Copyright (C) 2018 Pawel Pastuszak
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package mist.asm

class IdiomMatcher<Instruction : Instr, State, Result>(
    private val maxOffset: Int,
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
        repeat(maxOffset) { offset ->
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
