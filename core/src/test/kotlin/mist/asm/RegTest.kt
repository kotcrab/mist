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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

/** @author Kotcrab */

class RegTest {
    @Test
    fun `return reg for id`() {
        arrayOf(
                0 to Reg.zero,
                1 to Reg.at,
                2 to Reg.v0,
                3 to Reg.v1,
                4 to Reg.a0,
                5 to Reg.a1,
                6 to Reg.a2,
                7 to Reg.a3,
                8 to Reg.t0,
                9 to Reg.t1,
                10 to Reg.t2,
                11 to Reg.t3,
                12 to Reg.t4,
                13 to Reg.t5,
                14 to Reg.t6,
                15 to Reg.t7,
                16 to Reg.s0,
                17 to Reg.s1,
                18 to Reg.s2,
                19 to Reg.s3,
                20 to Reg.s4,
                21 to Reg.s5,
                22 to Reg.s6,
                23 to Reg.s7,
                24 to Reg.t8,
                25 to Reg.t9,
                26 to Reg.k0,
                27 to Reg.k1,
                28 to Reg.gp,
                29 to Reg.sp,
                30 to Reg.fp,
                31 to Reg.ra
        ).forEach { (id, reg) ->
            assertThat(Reg.forId(id)).isEqualTo(reg)
        }
    }

    @Test
    fun `don't return reg for -1 id`() {
        assertThatIllegalStateException().isThrownBy { Reg.forId(-1) }
    }

    @Test
    fun `don't return reg for invalid id`() {
        assertThatIllegalStateException().isThrownBy { Reg.forId(Integer.MAX_VALUE) }
    }
}
