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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mist.asm.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

/** @author Kotcrab */

class InstrTest {
    @Test
    fun `match operands`() {
        val instr = Instr(0, Opcode.Nop, Operand.Imm(0))
        assertThat(instr.matches()).isTrue()
        assertThat(instr.matches(Opcode.Nop)).isTrue()
        assertThat(instr.matches(Opcode.Nop, op1 = anyReg())).isFalse()
        assertThat(instr.matchesExact(Opcode.Nop)).isFalse()
    }

    @Test
    fun `match call all operands`() {
        val matcher = mockk<OperandMatcher>()
        every { matcher.match(any()) }.returns(true)
        val instr = Instr(0, Opcode.Nop, Operand.Imm(0))
        assertThat(instr.matches(Opcode.Nop, matcher, matcher, matcher)).isTrue()
        verify(exactly = 3) { matcher.match(any()) }
    }

    @Test
    fun `return last imm`() {
        assertThat(Instr(0, Opcode.Nop,
                Operand.Imm(2), Operand.Imm(1), Operand.Imm(0)).getLastImm()).isEqualTo(0)
        assertThat(Instr(0, Opcode.Nop,
                Operand.Imm(1), Operand.Imm(0), Operand.Reg(Reg.zero)).getLastImm()).isEqualTo(0)
        assertThat(Instr(0, Opcode.Nop,
                Operand.Imm(0), Operand.Reg(Reg.zero), Operand.Reg(Reg.zero)).getLastImm()).isEqualTo(0)
        assertThatIllegalStateException().isThrownBy {
            Instr(0, Opcode.Nop, Operand.Reg(Reg.zero), Operand.Reg(Reg.zero), Operand.Reg(Reg.zero)).getLastImm()
        }
    }

    @Test
    fun `convert to string`() {
        assertThat(Instr(0, Opcode.Nop).toString()).isEqualTo("0x0: nop")
        assertThat(Instr(0, Opcode.Nop, Operand.Reg(Reg.a0)).toString()).isEqualTo("0x0: nop a0")
        assertThat(Instr(0, Opcode.Nop, Operand.Reg(Reg.a0), Operand.Imm(1)).toString())
                .isEqualTo("0x0: nop a0, 0x1")
        assertThat(Instr(0, Opcode.Nop, Operand.Reg(Reg.a0), Operand.Imm(1), Operand.Imm(5)).toString())
                .isEqualTo("0x0: nop a0, 0x1, 0x5")
        assertThat(Instr(0, Opcode.Sw, Operand.Reg(Reg.a0), Operand.Reg(Reg.s0), Operand.Imm(1)).toString())
                .isEqualTo("0x0: sw a0, 0x1(s0)")
    }
}
