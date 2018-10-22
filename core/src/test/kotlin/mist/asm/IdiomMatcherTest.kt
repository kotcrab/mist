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

import io.mockk.spyk
import io.mockk.verify
import mist.asm.IdiomMatcher
import mist.asm.Instr
import mist.asm.Opcode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** @author Kotcrab */

class IdiomMatcherTest {
    private val emptyMatcher = object : IdiomMatcher<Int>(0) {
        override fun reset() {}

        override fun getPhases(): Array<(Instr) -> Boolean> {
            return emptyArray()
        }

        override fun matchedResult(relInstrs: List<Instr>): Int {
            return 0x42
        }
    }

    @Test
    fun `resets before matching`() {
        val matcher = spyk(emptyMatcher, recordPrivateCalls = true)
        matcher.matches(emptyList(), 0)
        verify { matcher["reset"]() }
    }

    @Test
    fun `return provided object when empty matcher matched empty list`() {
        assertThat(emptyMatcher.matches(emptyList(), 0)).isEqualTo(0x42)
    }

    @Test
    fun `calls match callback`() {
        var callbackCalled = false
        val matcher = object : IdiomMatcher<Unit>(1) {
            override fun reset() {}

            override fun getPhases(): Array<(Instr) -> Boolean> {
                return arrayOf({ instr -> instr.matches(Opcode.Nop) { callbackCalled = true } })
            }

            override fun matchedResult(relInstrs: List<Instr>) {}
        }
        matcher.matches(listOf(Instr(0x0, Opcode.Nop)), 0)
        assertThat(callbackCalled).isTrue()
    }

    @Test
    fun `respects max offset`() {
        val matcher = object : IdiomMatcher<Int>(2) {
            override fun reset() {}

            override fun getPhases(): Array<(Instr) -> Boolean> {
                return arrayOf(
                        { instr -> instr.matches(Opcode.Nop) },
                        { instr -> instr.matches(Opcode.Nop) }
                )
            }

            override fun matchedResult(relInstrs: List<Instr>): Int {
                return 0x42
            }
        }
        val result = matcher.matches(listOf(
                Instr(0x0, Opcode.Nop),
                Instr(0x0, Opcode.Addiu),
                Instr(0x0, Opcode.Nop)), 2)
        assertThat(result).isNull()
    }

    @Test
    fun `collects related instructions`() {
        var matchedResultCalled = false
        val matcher = object : IdiomMatcher<Int>(3) {
            override fun reset() {}

            override fun getPhases(): Array<(Instr) -> Boolean> {
                return arrayOf(
                        { instr -> instr.matches(Opcode.Nop) },
                        { instr -> instr.matches(Opcode.Nop) }
                )
            }

            override fun matchedResult(relInstrs: List<Instr>): Int {
                matchedResultCalled = true
                assertThat(relInstrs[0].addr).isEqualTo(8)
                assertThat(relInstrs[1].addr).isEqualTo(0)
                return 0x42
            }
        }
        val result = matcher.matches(listOf(
                Instr(0x0, Opcode.Nop),
                Instr(0x4, Opcode.Addiu),
                Instr(0x8, Opcode.Nop)), 2)
        assertThat(result).isEqualTo(0x42)
        assertThat(matchedResultCalled).isTrue()
    }
}
