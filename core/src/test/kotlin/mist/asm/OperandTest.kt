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

import mist.asm.FpuReg
import mist.asm.Operand
import mist.asm.Reg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** @author Kotcrab */

class OperandTest {
    @Test
    fun `converts reg to string`() {
        assertThat(Operand.Reg(Reg.s0).toString()).isEqualTo("s0")
    }

    @Test
    fun `converts fpu reg to string`() {
        assertThat(Operand.FpuReg(FpuReg.f0).toString()).isEqualTo("f0")
    }

    @Test
    fun `converts int to string`() {
        assertThat(Operand.Imm(0x42).toString()).isEqualTo("0x42")
    }
}
