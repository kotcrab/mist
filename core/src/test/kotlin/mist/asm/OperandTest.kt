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

class OperandTest {
  @Test
  fun `converts reg to string`() {
    assertThat(RegOperand(GprReg.S0).toString()).isEqualTo("s0")
  }


  @Test
  fun `converts int to string`() {
    assertThat(ImmOperand(0x42).toString()).isEqualTo("0x42")
    assertThat(ImmOperand(-0x42).toString()).isEqualTo("-0x42")
  }
}
