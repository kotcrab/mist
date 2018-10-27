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

import kmips.Label
import kmips.Reg.*
import kmips.assembleAsByteArray
import mist.test.util.MemBinLoader
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** @author Kotcrab */

class SwitchIdiomTest {
    @Test
    fun `match switch at idiom, beq sltiu variant`() {
        val bytes = assembleAsByteArray {
            val dummyLabel = Label()
            sltiu(a1, a0, 0x13)
            beq(a1, zero, dummyLabel)
            nop()
            sll(a0, a0, 0x2)
            lui(at, 0x8900)
            addu(at, at, a0)
            lw(at, -0x2240, at)
            jr(at)
            nop()
            label(dummyLabel)
        }
        verifySwitchMatch(bytes, SwitchAtRegIdiom(), 0x88FFDDC0.toInt())
    }

    @Test
    fun `match switch at idiom, beql slti variant`() {
        val bytes = assembleAsByteArray {
            val dummyLabel = Label()
            slti(a1, a0, 0x13)
            beql(a1, zero, dummyLabel)
            nop()
            sll(a0, a0, 0x2)
            lui(at, 0x8900)
            addu(at, at, a0)
            lw(at, -0x2240, at)
            jr(at)
            nop()
            label(dummyLabel)
        }
        verifySwitchMatch(bytes, SwitchAtRegIdiom(), 0x88FFDDC0.toInt())
    }

    @Test
    fun `match switch a0 idiom`() {
        val bytes = assembleAsByteArray {
            val dummyLabel = Label()
            sltiu(v0, s1, 0x13)
            beq(v0, zero, dummyLabel)
            lui(v1, 0x8900)
            sll(v0, s1, 0x2)
            addiu(v1, v1, 0x2240)
            addu(v0, v0, v1)
            lw(a0, 0x0, v0)
            jr(a0)
            nop()
            label(dummyLabel)
        }
        verifySwitchMatch(bytes, SwitchA0RegIdiom(), 0x89002240.toInt())
    }

    private fun verifySwitchMatch(bytes: ByteArray, idiom: IdiomMatcher<SwitchDescriptor>, expectedJumpTableLoc: Int) {
        val dasm = Disassembler(MemBinLoader(bytes), FunctionDef("", 0, bytes.size)).disassembly
        val result = idiom.matches(dasm.instr, dasm.instr.lastIndex)
        Assertions.assertThat(result).isNotNull()
        result!!
        assertThat(result.jumpTableLoc).isEqualTo(expectedJumpTableLoc)
        assertThat(result.relInstrs).isNotEmpty()
        assertThat(result.switchCaseCount).isEqualTo(0x13)
    }
}
