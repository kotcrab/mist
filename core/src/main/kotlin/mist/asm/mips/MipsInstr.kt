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

import kio.util.toHex
import mist.asm.*

/** @author Kotcrab */

class MipsInstr : Instr {
    constructor(addr: Int, opcode: MipsOpcode) : super(addr, opcode, emptyArray())

    constructor(addr: Int, opcode: MipsOpcode, op0: Operand) : super(addr, opcode, arrayOf(op0))

    constructor(addr: Int, opcode: MipsOpcode, op0: Operand, op1: Operand) : super(addr, opcode, arrayOf(op0, op1))

    constructor(addr: Int, opcode: MipsOpcode, op0: Operand, op1: Operand, op2: Operand)
            : super(addr, opcode, arrayOf(op0, op1, op2))

    constructor(addr: Int, opcode: MipsOpcode, op0: Operand, op1: Operand, op2: Operand, op3: Operand)
            : super(addr, opcode, arrayOf(op0, op1, op2, op3))

    fun op0AsReg() = (operands[0] as RegOperand).reg
    fun op1AsReg() = (operands[1] as RegOperand).reg
    fun op2AsReg() = (operands[2] as RegOperand).reg
    fun op3AsReg() = (operands[3] as RegOperand).reg
    fun op0AsImm() = (operands[0] as ImmOperand).value
    fun op1AsImm() = (operands[1] as ImmOperand).value
    fun op2AsImm() = (operands[2] as ImmOperand).value
    fun op3AsImm() = (operands[3] as ImmOperand).value

    fun matchesExact(
        opcode: Opcode, op0: OperandMatcher = isNull(), op1: OperandMatcher = isNull(),
        op2: OperandMatcher = isNull(), op3: OperandMatcher = isNull()
    ): Boolean {
        return super.matchesExact(opcode, op0, op1, op2, op3)
    }

    fun matches(
        opcode: Opcode? = null, op0: OperandMatcher = anyOp(), op1: OperandMatcher = anyOp(),
        op2: OperandMatcher = anyOp(), op3: OperandMatcher = anyOp()
    ): Boolean {
        return super.matches(opcode, op0, op1, op2, op3)
    }

    override fun toString(): String {
        return when {
            operands.size == 3 && (hasFlag(MemoryRead) || hasFlag(MemoryWrite)) -> {
                "${addr.toHex()}: ${opcode.mnemonic} ${operands[0]}, ${operands[2]}(${operands[1]})"
            }
            operands.isEmpty() -> "${addr.toHex()}: ${opcode.mnemonic}"
            else -> "${addr.toHex()}: ${opcode.mnemonic} ${operands.joinToString()}"
        }
    }
}
