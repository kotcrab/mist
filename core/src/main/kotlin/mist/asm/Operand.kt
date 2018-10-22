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

import kio.util.toSignedHex

/** @author Kotcrab */

sealed class Operand {
    class Reg(val reg: mist.asm.Reg) : Operand() {
        override fun toString(): String {
            return reg.name
        }
    }

    class FpuReg(val reg: mist.asm.FpuReg) : Operand() {
        override fun toString(): String {
            return reg.name
        }
    }

    class Imm(val value: Int) : Operand() {
        override fun toString(): String {
            return value.toSignedHex()
        }
    }
}
