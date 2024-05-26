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

package mist.asm.mips

import mist.test.util.assertThatDisassemblerException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** @author Kotcrab */

class GprRegTest {
  @Test
  fun `return gpr reg for id`() {
    arrayOf(
      0 to GprReg.Zero,
      1 to GprReg.At,
      2 to GprReg.V0,
      3 to GprReg.V1,
      4 to GprReg.A0,
      5 to GprReg.A1,
      6 to GprReg.A2,
      7 to GprReg.A3,
      8 to GprReg.T0,
      9 to GprReg.T1,
      10 to GprReg.T2,
      11 to GprReg.T3,
      12 to GprReg.T4,
      13 to GprReg.T5,
      14 to GprReg.T6,
      15 to GprReg.T7,
      16 to GprReg.S0,
      17 to GprReg.S1,
      18 to GprReg.S2,
      19 to GprReg.S3,
      20 to GprReg.S4,
      21 to GprReg.S5,
      22 to GprReg.S6,
      23 to GprReg.S7,
      24 to GprReg.T8,
      25 to GprReg.T9,
      26 to GprReg.K0,
      27 to GprReg.K1,
      28 to GprReg.Gp,
      29 to GprReg.Sp,
      30 to GprReg.Fp,
      31 to GprReg.Ra
    ).forEach { (id, reg) ->
      assertThat(GprReg.forId(id)).isEqualTo(reg)
    }
  }

  @Test
  fun `values contains special purpose gpr registers`() {
    val values = GprReg.values()
    assertThat(values.contains(GprReg.Pc)).isTrue()
    assertThat(values.contains(GprReg.Lo)).isTrue()
    assertThat(values.contains(GprReg.Hi)).isTrue()
  }

  @Test
  fun `don't return gpr reg for -1 id`() {
    assertThatDisassemblerException().isThrownBy { GprReg.forId(-1) }
  }

  @Test
  fun `don't return gpr reg for invalid id`() {
    assertThatDisassemblerException().isThrownBy { GprReg.forId(Integer.MAX_VALUE) }
  }
}
