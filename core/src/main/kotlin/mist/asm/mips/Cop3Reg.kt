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

sealed class Cop3Reg(name: String, id: Int) : Reg(name, id) {
    companion object {
        // TODO "by lazy" here is workaround for KT-8970
        private val cop3Regs by lazy {
            arrayOf(
                Cop3r0, Cop3r1, Cop3r2, Cop3r3, Cop3r4, Cop3r5, Cop3r6, Cop3r7, Cop3r8,
                Cop3r9, Cop3r10, Cop3r11, Cop3r12, Cop3r13, Cop3r14, Cop3r15, Cop3r16,
                Cop3r17, Cop3r18, Cop3r19, Cop3r20, Cop3r21, Cop3r22, Cop3r23, Cop3r24,
                Cop3r25, Cop3r26, Cop3r27, Cop3r28, Cop3r29, Cop3r30, Cop3r31
            )
        }

        fun forId(id: Int): Cop3Reg {
            return Reg.forId(values(), id)
        }

        fun values(): Array<Cop3Reg> {
            return cop3Regs
        }
    }

    object Cop3r0 : Cop3Reg("cop3r0", 0)
    object Cop3r1 : Cop3Reg("cop3r1", 1)
    object Cop3r2 : Cop3Reg("cop3r2", 2)
    object Cop3r3 : Cop3Reg("cop3r3", 3)
    object Cop3r4 : Cop3Reg("cop3r4", 4)
    object Cop3r5 : Cop3Reg("cop3r5", 5)
    object Cop3r6 : Cop3Reg("cop3r6", 6)
    object Cop3r7 : Cop3Reg("cop3r7", 7)
    object Cop3r8 : Cop3Reg("cop3r8", 8)
    object Cop3r9 : Cop3Reg("cop3r9", 9)
    object Cop3r10 : Cop3Reg("cop3r10", 10)
    object Cop3r11 : Cop3Reg("cop3r11", 11)
    object Cop3r12 : Cop3Reg("cop3r12", 12)
    object Cop3r13 : Cop3Reg("cop3r13", 13)
    object Cop3r14 : Cop3Reg("cop3r14", 14)
    object Cop3r15 : Cop3Reg("cop3r15", 15)
    object Cop3r16 : Cop3Reg("cop3r16", 16)
    object Cop3r17 : Cop3Reg("cop3r17", 17)
    object Cop3r18 : Cop3Reg("cop3r18", 18)
    object Cop3r19 : Cop3Reg("cop3r19", 19)
    object Cop3r20 : Cop3Reg("cop3r20", 20)
    object Cop3r21 : Cop3Reg("cop3r21", 21)
    object Cop3r22 : Cop3Reg("cop3r22", 22)
    object Cop3r23 : Cop3Reg("cop3r23", 23)
    object Cop3r24 : Cop3Reg("cop3r24", 24)
    object Cop3r25 : Cop3Reg("cop3r25", 25)
    object Cop3r26 : Cop3Reg("cop3r26", 26)
    object Cop3r27 : Cop3Reg("cop3r27", 27)
    object Cop3r28 : Cop3Reg("cop3r28", 28)
    object Cop3r29 : Cop3Reg("cop3r29", 29)
    object Cop3r30 : Cop3Reg("cop3r30", 30)
    object Cop3r31 : Cop3Reg("cop3r31", 31)
}
