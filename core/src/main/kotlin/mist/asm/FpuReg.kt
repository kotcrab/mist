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

/** @author Kotcrab */

@Suppress("EnumEntryName")
enum class FpuReg(val id: Int) {
    f0(0), f1(1), f2(2), f3(3), f4(4),
    f5(5), f6(6), f7(7), f8(8), f9(9),
    f10(10), f11(11), f12(12), f13(13), f14(14),
    f15(15), f16(16), f17(17), f18(18), f19(19),
    f20(20), f21(21), f22(22), f23(23), f24(24),
    f25(25), f26(26), f27(27), f28(28), f29(29),
    f30(30), f31(31),
    // special purpose - directly inaccessible registers
    cc(-1);

    companion object {
        fun forId(id: Int): FpuReg {
            if (id == -1) error("can't return directly inaccessible register")
            values().forEach {
                if (id == it.id) return it
            }
            error("no such fpu register id: $id")
        }
    }
}
