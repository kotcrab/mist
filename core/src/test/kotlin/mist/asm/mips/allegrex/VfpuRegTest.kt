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

package mist.asm.mips.allegrex

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

/** @author Kotcrab */

class VfpuRegTest {
    @Test
    fun `return vfpu reg for id`() {
        val regs = arrayOf(
                0x00 to VfpuReg.V00,
                0x01 to VfpuReg.V01,
                0x02 to VfpuReg.V02,
                0x03 to VfpuReg.V03,
                0x04 to VfpuReg.V04,
                0x05 to VfpuReg.V05,
                0x06 to VfpuReg.V06,
                0x07 to VfpuReg.V07,
                0x08 to VfpuReg.V08,
                0x09 to VfpuReg.V09,
                0x0A to VfpuReg.V0A,
                0x0B to VfpuReg.V0B,
                0x0C to VfpuReg.V0C,
                0x0D to VfpuReg.V0D,
                0x0E to VfpuReg.V0E,
                0x0F to VfpuReg.V0F,
                0x10 to VfpuReg.V10,
                0x11 to VfpuReg.V11,
                0x12 to VfpuReg.V12,
                0x13 to VfpuReg.V13,
                0x14 to VfpuReg.V14,
                0x15 to VfpuReg.V15,
                0x16 to VfpuReg.V16,
                0x17 to VfpuReg.V17,
                0x18 to VfpuReg.V18,
                0x19 to VfpuReg.V19,
                0x1A to VfpuReg.V1A,
                0x1B to VfpuReg.V1B,
                0x1C to VfpuReg.V1C,
                0x1D to VfpuReg.V1D,
                0x1E to VfpuReg.V1E,
                0x1F to VfpuReg.V1F,
                0x20 to VfpuReg.V20,
                0x21 to VfpuReg.V21,
                0x22 to VfpuReg.V22,
                0x23 to VfpuReg.V23,
                0x24 to VfpuReg.V24,
                0x25 to VfpuReg.V25,
                0x26 to VfpuReg.V26,
                0x27 to VfpuReg.V27,
                0x28 to VfpuReg.V28,
                0x29 to VfpuReg.V29,
                0x2A to VfpuReg.V2A,
                0x2B to VfpuReg.V2B,
                0x2C to VfpuReg.V2C,
                0x2D to VfpuReg.V2D,
                0x2E to VfpuReg.V2E,
                0x2F to VfpuReg.V2F,
                0x30 to VfpuReg.V30,
                0x31 to VfpuReg.V31,
                0x32 to VfpuReg.V32,
                0x33 to VfpuReg.V33,
                0x34 to VfpuReg.V34,
                0x35 to VfpuReg.V35,
                0x36 to VfpuReg.V36,
                0x37 to VfpuReg.V37,
                0x38 to VfpuReg.V38,
                0x39 to VfpuReg.V39,
                0x3A to VfpuReg.V3A,
                0x3B to VfpuReg.V3B,
                0x3C to VfpuReg.V3C,
                0x3D to VfpuReg.V3D,
                0x3E to VfpuReg.V3E,
                0x3F to VfpuReg.V3F,
                0x40 to VfpuReg.V40,
                0x41 to VfpuReg.V41,
                0x42 to VfpuReg.V42,
                0x43 to VfpuReg.V43,
                0x44 to VfpuReg.V44,
                0x45 to VfpuReg.V45,
                0x46 to VfpuReg.V46,
                0x47 to VfpuReg.V47,
                0x48 to VfpuReg.V48,
                0x49 to VfpuReg.V49,
                0x4A to VfpuReg.V4A,
                0x4B to VfpuReg.V4B,
                0x4C to VfpuReg.V4C,
                0x4D to VfpuReg.V4D,
                0x4E to VfpuReg.V4E,
                0x4F to VfpuReg.V4F,
                0x50 to VfpuReg.V50,
                0x51 to VfpuReg.V51,
                0x52 to VfpuReg.V52,
                0x53 to VfpuReg.V53,
                0x54 to VfpuReg.V54,
                0x55 to VfpuReg.V55,
                0x56 to VfpuReg.V56,
                0x57 to VfpuReg.V57,
                0x58 to VfpuReg.V58,
                0x59 to VfpuReg.V59,
                0x5A to VfpuReg.V5A,
                0x5B to VfpuReg.V5B,
                0x5C to VfpuReg.V5C,
                0x5D to VfpuReg.V5D,
                0x5E to VfpuReg.V5E,
                0x5F to VfpuReg.V5F,
                0x60 to VfpuReg.V60,
                0x61 to VfpuReg.V61,
                0x62 to VfpuReg.V62,
                0x63 to VfpuReg.V63,
                0x64 to VfpuReg.V64,
                0x65 to VfpuReg.V65,
                0x66 to VfpuReg.V66,
                0x67 to VfpuReg.V67,
                0x68 to VfpuReg.V68,
                0x69 to VfpuReg.V69,
                0x6A to VfpuReg.V6A,
                0x6B to VfpuReg.V6B,
                0x6C to VfpuReg.V6C,
                0x6D to VfpuReg.V6D,
                0x6E to VfpuReg.V6E,
                0x6F to VfpuReg.V6F,
                0x70 to VfpuReg.V70,
                0x71 to VfpuReg.V71,
                0x72 to VfpuReg.V72,
                0x73 to VfpuReg.V73,
                0x74 to VfpuReg.V74,
                0x75 to VfpuReg.V75,
                0x76 to VfpuReg.V76,
                0x77 to VfpuReg.V77,
                0x78 to VfpuReg.V78,
                0x79 to VfpuReg.V79,
                0x7A to VfpuReg.V7A,
                0x7B to VfpuReg.V7B,
                0x7C to VfpuReg.V7C,
                0x7D to VfpuReg.V7D,
                0x7E to VfpuReg.V7E,
                0x7F to VfpuReg.V7F
        )
        assertThat(regs.size).isEqualTo(128)
        regs.forEach { (id, reg) ->
            assertThat(VfpuReg.forId(id)).isEqualTo(reg)
        }
    }

    @Test
    fun `values contains special purpose fpu registers`() {
        val values = VfpuReg.values()
        assertThat(values.contains(VfpuReg.Pfxs)).isTrue()
        assertThat(values.contains(VfpuReg.Pfxt)).isTrue()
        assertThat(values.contains(VfpuReg.Pfxd)).isTrue()
        assertThat(values.contains(VfpuReg.Cc)).isTrue()
        assertThat(values.contains(VfpuReg.Rev)).isTrue()
        assertThat(values.contains(VfpuReg.Rcx0)).isTrue()
        assertThat(values.contains(VfpuReg.Rcx1)).isTrue()
        assertThat(values.contains(VfpuReg.Rcx2)).isTrue()
        assertThat(values.contains(VfpuReg.Rcx3)).isTrue()
        assertThat(values.contains(VfpuReg.Rcx4)).isTrue()
        assertThat(values.contains(VfpuReg.Rcx5)).isTrue()
        assertThat(values.contains(VfpuReg.Rcx6)).isTrue()
        assertThat(values.contains(VfpuReg.Rcx7)).isTrue()
    }

    @Test
    fun `don't return vfpu reg for -1 id`() {
        assertThatIllegalStateException().isThrownBy { VfpuReg.forId(-1) }
    }

    @Test
    fun `don't return vfpu reg for invalid id`() {
        assertThatIllegalStateException().isThrownBy { VfpuReg.forId(Integer.MAX_VALUE) }
    }
}
