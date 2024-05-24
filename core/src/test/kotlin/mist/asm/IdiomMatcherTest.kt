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

import kmips.Instruction
import mist.asm.mips.MipsInstr
import mist.asm.mips.MipsOpcode.Addiu
import mist.asm.mips.MipsOpcode.Nop
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** @author Kotcrab */

class IdiomMatcherTest {
    @Test
    fun `return provided object when empty matcher matched empty list`() {
        val emptyMatcher = IdiomMatcher<MipsInstr, Unit, Int>(0, {}, { _, _ -> 0x42 }, emptyArray())
        assertThat(emptyMatcher.matches(emptyList(), 0)).isEqualTo(0x42)
    }

    @Test
    fun `respects max instruction offset`() {
        val matcher = IdiomMatcher<MipsInstr, Unit, Int>(2, {}, { _, _ -> 0x42 }, arrayOf(
            { instr, _ -> instr.matches(Nop) },
            { instr, _ -> instr.matches(Nop) }
        ))
        val instr = listOf(
            MipsInstr(0x0, Nop),
            MipsInstr(0x0, Addiu),
            MipsInstr(0x0, Nop)
        )
        val result = matcher.matches(instr, 2)
        assertThat(result).isNull()
    }

    @Test
    fun `recreates state for each run`() {
        var stateCallCount = 0
        val emptyMatcher = IdiomMatcher<MipsInstr, Unit, Int>(0, { stateCallCount += 1 }, { _, _ -> 0x42 }, emptyArray())
        emptyMatcher.matches(emptyList(), 0)
        emptyMatcher.matches(emptyList(), 0)
        emptyMatcher.matches(emptyList(), 0)
        assertThat(stateCallCount).isEqualTo(3)
    }

    @Test
    fun `collects related instructions and passes state to result transform`() {
        var matchedResultCalled = false
        val matcher = IdiomMatcher<MipsInstr, Int, Int>(3, { 0xFF }, { relInstrs, state ->
            matchedResultCalled = true
            assertThat(state).isEqualTo(0xFF)
            assertThat(relInstrs[0].addr).isEqualTo(8)
            assertThat(relInstrs[1].addr).isEqualTo(0)
            0x42
        }, arrayOf(
            { instr, _ -> instr.matches(Nop) },
            { instr, _ -> instr.matches(Nop) }
        ))
        val instr = listOf(
            MipsInstr(0x0, Nop),
            MipsInstr(0x4, Addiu),
            MipsInstr(0x8, Nop)
        )
        val result = matcher.matches(instr, 2)
        assertThat(result).isEqualTo(0x42)
        assertThat(matchedResultCalled).isTrue()
    }
}
