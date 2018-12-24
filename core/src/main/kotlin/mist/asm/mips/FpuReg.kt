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

import mist.asm.Reg

/** @author Kotcrab */

sealed class FpuReg(name: String, id: Int) : Reg(name, id) {
    companion object {
        // warning: "by lazy" here is workaround for KT-8970
        private val fpuRegs by lazy {
            arrayOf(
                F0, F1, F2, F3,
                F4, F5, F6, F7,
                F8, F9, F10, F11,
                F12, F13, F14, F15,
                F16, F17, F18, F19,
                F20, F21, F22, F23,
                F24, F25, F26, F27,
                F28, F29, F30, F31,
                Cc0, Cc1, Cc2, Cc3,
                Cc4, Cc5, Cc6, Cc7
            )
        }

        fun forId(id: Int): FpuReg {
            return Reg.forId(values(), id)
        }

        fun ccForId(ccId: Int): FpuReg {
            return when (ccId) {
                0 -> FpuReg.Cc0
                1 -> FpuReg.Cc1
                2 -> FpuReg.Cc2
                3 -> FpuReg.Cc3
                4 -> FpuReg.Cc4
                5 -> FpuReg.Cc5
                6 -> FpuReg.Cc6
                7 -> FpuReg.Cc7
                else -> error("no such cc register id: $ccId")
            }
        }

        fun values(): Array<FpuReg> {
            return fpuRegs
        }
    }

    object F0 : FpuReg("f0", 0)
    object F1 : FpuReg("f1", 1)
    object F2 : FpuReg("f2", 2)
    object F3 : FpuReg("f3", 3)
    object F4 : FpuReg("f4", 4)
    object F5 : FpuReg("f5", 5)
    object F6 : FpuReg("f6", 6)
    object F7 : FpuReg("f7", 7)
    object F8 : FpuReg("f8", 8)
    object F9 : FpuReg("f9", 9)
    object F10 : FpuReg("f10", 10)
    object F11 : FpuReg("f11", 11)
    object F12 : FpuReg("f12", 12)
    object F13 : FpuReg("f13", 13)
    object F14 : FpuReg("f14", 14)
    object F15 : FpuReg("f15", 15)
    object F16 : FpuReg("f16", 16)
    object F17 : FpuReg("f17", 17)
    object F18 : FpuReg("f18", 18)
    object F19 : FpuReg("f19", 19)
    object F20 : FpuReg("f20", 20)
    object F21 : FpuReg("f21", 21)
    object F22 : FpuReg("f22", 22)
    object F23 : FpuReg("f23", 23)
    object F24 : FpuReg("f24", 24)
    object F25 : FpuReg("f25", 25)
    object F26 : FpuReg("f26", 26)
    object F27 : FpuReg("f27", 27)
    object F28 : FpuReg("f28", 28)
    object F29 : FpuReg("f29", 29)
    object F30 : FpuReg("f30", 30)
    object F31 : FpuReg("f31", 31)

    // special purpose - directly inaccessible registers

    object Cc0 : FpuReg("cc0", -1)
    object Cc1 : FpuReg("cc1", -1)
    object Cc2 : FpuReg("cc2", -1)
    object Cc3 : FpuReg("cc3", -1)
    object Cc4 : FpuReg("cc4", -1)
    object Cc5 : FpuReg("cc5", -1)
    object Cc6 : FpuReg("cc6", -1)
    object Cc7 : FpuReg("cc7", -1)
}
