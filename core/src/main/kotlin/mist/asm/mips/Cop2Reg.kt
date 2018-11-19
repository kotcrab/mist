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

package mist.asm.mips

import mist.asm.Reg

/** @author Kotcrab */

sealed class Cop2Reg(name: String, id: Int) : Reg(name, id) {
    companion object {
        // TODO "by lazy" here is workaround for KT-8970
        private val cop2Regs by lazy {
            arrayOf(
                Cop2r0, Cop2r1, Cop2r2, Cop2r3, Cop2r4, Cop2r5, Cop2r6, Cop2r7, Cop2r8,
                Cop2r9, Cop2r10, Cop2r11, Cop2r12, Cop2r13, Cop2r14, Cop2r15, Cop2r16,
                Cop2r17, Cop2r18, Cop2r19, Cop2r20, Cop2r21, Cop2r22, Cop2r23, Cop2r24,
                Cop2r25, Cop2r26, Cop2r27, Cop2r28, Cop2r29, Cop2r30, Cop2r31
            )
        }

        fun forId(id: Int): Cop2Reg {
            if (id == -1) error("can't return directly inaccessible register")
            values().forEach {
                if (id == it.id) return it
            }
            error("no such register id: $id")
        }

        fun values(): Array<Cop2Reg> {
            return cop2Regs
        }
    }

    object Cop2r0 : Cop2Reg("cop2r0", 0)
    object Cop2r1 : Cop2Reg("cop2r1", 1)
    object Cop2r2 : Cop2Reg("cop2r2", 2)
    object Cop2r3 : Cop2Reg("cop2r3", 3)
    object Cop2r4 : Cop2Reg("cop2r4", 4)
    object Cop2r5 : Cop2Reg("cop2r5", 5)
    object Cop2r6 : Cop2Reg("cop2r6", 6)
    object Cop2r7 : Cop2Reg("cop2r7", 7)
    object Cop2r8 : Cop2Reg("cop2r8", 8)
    object Cop2r9 : Cop2Reg("cop2r9", 9)
    object Cop2r10 : Cop2Reg("cop2r10", 10)
    object Cop2r11 : Cop2Reg("cop2r11", 11)
    object Cop2r12 : Cop2Reg("cop2r12", 12)
    object Cop2r13 : Cop2Reg("cop2r13", 13)
    object Cop2r14 : Cop2Reg("cop2r14", 14)
    object Cop2r15 : Cop2Reg("cop2r15", 15)
    object Cop2r16 : Cop2Reg("cop2r16", 16)
    object Cop2r17 : Cop2Reg("cop2r17", 17)
    object Cop2r18 : Cop2Reg("cop2r18", 18)
    object Cop2r19 : Cop2Reg("cop2r19", 19)
    object Cop2r20 : Cop2Reg("cop2r20", 20)
    object Cop2r21 : Cop2Reg("cop2r21", 21)
    object Cop2r22 : Cop2Reg("cop2r22", 22)
    object Cop2r23 : Cop2Reg("cop2r23", 23)
    object Cop2r24 : Cop2Reg("cop2r24", 24)
    object Cop2r25 : Cop2Reg("cop2r25", 25)
    object Cop2r26 : Cop2Reg("cop2r26", 26)
    object Cop2r27 : Cop2Reg("cop2r27", 27)
    object Cop2r28 : Cop2Reg("cop2r28", 28)
    object Cop2r29 : Cop2Reg("cop2r29", 29)
    object Cop2r30 : Cop2Reg("cop2r30", 30)
    object Cop2r31 : Cop2Reg("cop2r31", 31)
}
