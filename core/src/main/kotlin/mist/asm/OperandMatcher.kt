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

fun isNull() = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return true
        return false
    }
}

fun anyOp() = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        return true
    }
}

fun anyReg() = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return false
        if (op !is Operand.Reg) return false
        return true
    }
}

fun isReg(reg: Reg) = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return false
        if (op !is Operand.Reg) return false
        return op.reg == reg
    }
}


fun isReg(vararg regs: Reg) = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return false
        if (op !is Operand.Reg) return false
        return op.reg in regs
    }
}

fun anyFpuReg() = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return false
        if (op !is Operand.FpuReg) return false
        return true
    }
}

fun isFpuReg(reg: FpuReg) = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return false
        if (op !is Operand.FpuReg) return false
        return op.reg == reg
    }
}


fun isFpuReg(vararg regs: FpuReg) = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return false
        if (op !is Operand.FpuReg) return false
        return op.reg in regs
    }
}

fun anyImm() = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return false
        if (op !is Operand.Imm) return false
        return true
    }
}

fun isImm(value: Int) = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return false
        if (op !is Operand.Imm) return false
        return op.value == value
    }
}

fun isImm(vararg values: Int) = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return false
        if (op !is Operand.Imm) return false
        return op.value in values
    }
}

interface OperandMatcher {
    fun match(op: Operand?): Boolean
}
