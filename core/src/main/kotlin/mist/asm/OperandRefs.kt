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

package mist.asm

/** @author Kotcrab */

object Operand0Ref : OperandIdxRef(0)

object Operand1Ref : OperandIdxRef(1)

object Operand2Ref : OperandIdxRef(2)

object Operand3Ref : OperandIdxRef(3)

object Operand4Ref : OperandIdxRef(4)

object Operand5Ref : OperandIdxRef(5)

object Operand6Ref : OperandIdxRef(6)

object Operand7Ref : OperandIdxRef(7)

object Operand8Ref : OperandIdxRef(8)

object Operand9Ref : OperandIdxRef(9)

object Operand10Ref : OperandIdxRef(10)

open class OperandIdxRef(val idx: Int) : OperandRef {
    override fun getReg(instr: Instr): Reg {
        return (instr.operands[idx] as RegOperand).reg
    }
}

data class OperandRegRef(val reg: Reg) : OperandRef {
    override fun getReg(instr: Instr): Reg {
        return reg
    }
}

interface OperandRef {
    fun getReg(instr: Instr): Reg
}
