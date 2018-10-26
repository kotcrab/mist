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
enum class Reg(val id: Int) {
    zero(0),
    at(1),
    v0(2), v1(3),
    a0(4), a1(5), a2(6), a3(7),
    t0(8), t1(9), t2(10), t3(11), t4(12), t5(13), t6(14), t7(15), t8(24), t9(25),
    s0(16), s1(17), s2(18), s3(19), s4(20), s5(21), s6(22), s7(23),
    k0(26), k1(27),
    gp(28), sp(29), fp(30), ra(31),
    // special purpose - directly inaccessible registers
    pc(-1),
    lo(-1), hi(-1);

    companion object {
        fun forId(id: Int): Reg {
            if (id == -1) error("can't return directly inaccessible register")
            values().forEach {
                if (id == it.id) return it
            }
            error("no such register id: $id")
        }
    }
}
