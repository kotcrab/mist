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

import mist.asm.mips.GprReg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** @author Kotcrab */

class OperandMatcherTest {
    private val reg = RegOperand(GprReg.S0)
    private val reg2 = RegOperand(GprReg.S1)
    private val imm = ImmOperand(0x42)
    private val imm2 = ImmOperand(0x43)

    @Test
    fun `match null op only`() {
        val m = isNull()
        assertThat(m.match(null)).isTrue()
        assertThat(m.match(reg)).isFalse()
        assertThat(m.match(imm)).isFalse()
    }

    @Test
    fun `match any`() {
        val m = anyOp()
        assertThat(m.match(null)).isTrue()
        assertThat(m.match(reg)).isTrue()
        assertThat(m.match(imm)).isTrue()
    }

    @Test
    fun `match any reg`() {
        val m = anyReg()
        assertThat(m.match(null)).isFalse()
        assertThat(m.match(reg)).isTrue()
        assertThat(m.match(reg2)).isTrue()
        assertThat(m.match(imm)).isFalse()
    }

    @Test
    fun `match single reg`() {
        val m = isReg(GprReg.S0)
        assertThat(m.match(null)).isFalse()
        assertThat(m.match(reg)).isTrue()
        assertThat(m.match(reg2)).isFalse()
        assertThat(m.match(imm)).isFalse()
    }

    @Test
    fun `match multiple reg`() {
        val m = isReg(GprReg.S0, GprReg.S1)
        assertThat(m.match(null)).isFalse()
        assertThat(m.match(reg)).isTrue()
        assertThat(m.match(reg2)).isTrue()
        assertThat(m.match(imm)).isFalse()
    }

    @Test
    fun `match any imm`() {
        val m = anyImm()
        assertThat(m.match(null)).isFalse()
        assertThat(m.match(reg)).isFalse()
        assertThat(m.match(imm)).isTrue()
        assertThat(m.match(imm2)).isTrue()
    }

    @Test
    fun `match single imm`() {
        val m = isImm(0x42)
        assertThat(m.match(null)).isFalse()
        assertThat(m.match(reg)).isFalse()
        assertThat(m.match(imm)).isTrue()
        assertThat(m.match(imm2)).isFalse()
    }

    @Test
    fun `match multiple imm`() {
        val m = isImm(0x42, 0x43)
        assertThat(m.match(null)).isFalse()
        assertThat(m.match(reg)).isFalse()
        assertThat(m.match(imm)).isTrue()
        assertThat(m.match(imm2)).isTrue()
    }
}
