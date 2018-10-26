/*
 * mist - interactive mips disassembler and decompiler
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

/** @author Kotcrab */

abstract class IdiomMatcher<Result>(private val maxOffset: Int) {
    fun matches(instrs: List<Instr>, startIdx: Int): Result? {
        reset()
        val (matches, relInstrs) = phasesMatches(instrs, startIdx)
        if (matches) return matchedResult(relInstrs)
        return null
    }

    private fun phasesMatches(instrs: List<Instr>, startIdx: Int): Pair<Boolean, List<Instr>> {
        val relInstrs: MutableList<Instr> = mutableListOf()
        val phases = getPhases()
        var phaseIdx = 0
        if (phases.isEmpty()) return Pair(true, relInstrs)
        repeat(maxOffset) { offset ->
            val instrIdx = startIdx - offset
            if (instrIdx < 0) return Pair(false, relInstrs)
            val instr = instrs[instrIdx]
            if (phases[phaseIdx].invoke(instr)) {
                relInstrs.add(instr)
                phaseIdx++
                if (phaseIdx == phases.size) return Pair(true, relInstrs)
            }
        }
        return Pair(false, relInstrs)
    }

    protected abstract fun reset()

    protected abstract fun getPhases(): Array<(Instr) -> Boolean>

    protected abstract fun matchedResult(relInstrs: List<Instr>): Result

    protected fun Instr.matches(opcode: Opcode? = null, op1: OperandMatcher = anyOp(), op2: OperandMatcher = anyOp(),
                                op3: OperandMatcher = anyOp(), matchedCallback: Instr.() -> Unit): Boolean {
        val matches = matches(opcode, op1, op2, op3)
        if (matches) matchedCallback()
        return matches
    }
}
