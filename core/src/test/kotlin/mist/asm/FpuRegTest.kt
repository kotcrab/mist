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

class FpuRegTest {
    @Test
    fun `return fpu reg for id`() {
        arrayOf(
                0 to FpuReg.f0,
                1 to FpuReg.f1,
                2 to FpuReg.f2,
                3 to FpuReg.f3,
                4 to FpuReg.f4,
                5 to FpuReg.f5,
                6 to FpuReg.f6,
                7 to FpuReg.f7,
                8 to FpuReg.f8,
                9 to FpuReg.f9,
                10 to FpuReg.f10,
                11 to FpuReg.f11,
                12 to FpuReg.f12,
                13 to FpuReg.f13,
                14 to FpuReg.f14,
                15 to FpuReg.f15,
                16 to FpuReg.f16,
                17 to FpuReg.f17,
                18 to FpuReg.f18,
                19 to FpuReg.f19,
                20 to FpuReg.f20,
                21 to FpuReg.f21,
                22 to FpuReg.f22,
                23 to FpuReg.f23,
                24 to FpuReg.f24,
                25 to FpuReg.f25,
                26 to FpuReg.f26,
                27 to FpuReg.f27,
                28 to FpuReg.f28,
                29 to FpuReg.f29,
                30 to FpuReg.f30,
                31 to FpuReg.f31
        ).forEach { (id, reg) ->
            assertThat(FpuReg.forId(id)).isEqualTo(reg)
        }
    }

    @Test
    fun `don't return fpu reg for -1 id`() {
        assertThatIllegalStateException().isThrownBy { FpuReg.forId(-1) }
    }

    @Test
    fun `don't return fpu reg for invalid id`() {
        assertThatIllegalStateException().isThrownBy { FpuReg.forId(Integer.MAX_VALUE) }
    }
}
