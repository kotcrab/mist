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

import kio.util.toHex

/** @author Kotcrab */

class Instr constructor(val addr: Int, val opcode: Opcode, val op1: Operand?, val op2: Operand?, val op3: Operand?) {
    constructor(addr: Int, opcode: Opcode) : this(addr, opcode, null, null, null)
    constructor(addr: Int, opcode: Opcode, op1: Operand) : this(addr, opcode, op1, null, null)
    constructor(addr: Int, opcode: Opcode, op1: Operand, op2: Operand) : this(addr, opcode, op1, op2, null)

    fun isMemoryRead() = opcode.memoryRead
    fun isMemoryWrite() = opcode.memoryWrite
    fun isBranch() = opcode.branch
    fun isBranchLikely() = opcode.branchLikely
    fun isJump() = opcode.jump
    fun isJumpLinkRegister() = opcode.jumpLinkRegister

    fun op1AsReg() = (op1 as Operand.Reg).reg
    fun op2AsReg() = (op2 as Operand.Reg).reg
    fun op3AsReg() = (op3 as Operand.Reg).reg
    fun op1AsFpuReg() = (op1 as Operand.FpuReg).reg
    fun op2AsFpuReg() = (op2 as Operand.FpuReg).reg
    fun op3AsFpuReg() = (op3 as Operand.FpuReg).reg
    fun op1AsImm() = (op1 as Operand.Imm).value
    fun op2AsImm() = (op2 as Operand.Imm).value
    fun op3AsImm() = (op3 as Operand.Imm).value

    fun matches(opcode: Opcode? = null, op1: OperandMatcher = anyOp(), op2: OperandMatcher = anyOp(),
                op3: OperandMatcher = anyOp()): Boolean {
        if (opcode != null && this.opcode != opcode) return false
        if (arrayOf(op1.match(this.op1), op2.match(this.op2), op3.match(this.op3)).all { it == true }) return true
        return false
    }

    fun matchesExact(opcode: Opcode, op1: OperandMatcher = isNull(), op2: OperandMatcher = isNull(),
                     op3: OperandMatcher = isNull()): Boolean {
        if (this.opcode != opcode) return false
        if (arrayOf(op1.match(this.op1), op2.match(this.op2), op3.match(this.op3)).all { it == true }) return true
        return false
    }

    fun getLastImm(): Int {
        if (op3 is Operand.Imm) return op3.value
        if (op2 is Operand.Imm) return op2.value
        if (op1 is Operand.Imm) return op1.value
        error("instr ${toString()} does not have any immediate operands")
    }

    /** Returns register list that were possibly modified by this instruction */
    fun getModifiedRegisters(): Array<Any> {
        return when (opcode) {
            Opcode.Lb -> arrayOf(op1AsReg())
            Opcode.Lbu -> arrayOf(op1AsReg())
            Opcode.Sb -> arrayOf()

            Opcode.Lh -> arrayOf(op1AsReg())
            Opcode.Lhu -> arrayOf(op1AsReg())
            Opcode.Sh -> arrayOf()

            Opcode.Lw -> arrayOf(op1AsReg())
            Opcode.Sw -> arrayOf()

            Opcode.Lwl -> arrayOf(op1AsReg())
            Opcode.Lwr -> arrayOf(op1AsReg())
            Opcode.Swl -> arrayOf()
            Opcode.Swr -> arrayOf()

            Opcode.Ll -> arrayOf(op1AsReg())
            Opcode.Sc -> arrayOf()

            Opcode.Addi -> arrayOf(op1AsReg())
            Opcode.Addiu -> arrayOf(op1AsReg())
            Opcode.Slti -> arrayOf(op1AsReg())
            Opcode.Sltiu -> arrayOf(op1AsReg())
            Opcode.Andi -> arrayOf(op1AsReg())
            Opcode.Ori -> arrayOf(op1AsReg())
            Opcode.Xori -> arrayOf(op1AsReg())
            Opcode.Lui -> arrayOf(op1AsReg())

            Opcode.Add -> arrayOf(op1AsReg())
            Opcode.Addu -> arrayOf(op1AsReg())
            Opcode.Sub -> arrayOf(op1AsReg())
            Opcode.Subu -> arrayOf(op1AsReg())

            Opcode.Slt -> arrayOf(op1AsReg())
            Opcode.Sltu -> arrayOf(op1AsReg())
            Opcode.And -> arrayOf(op1AsReg())
            Opcode.Or -> arrayOf(op1AsReg())
            Opcode.Xor -> arrayOf(op1AsReg())
            Opcode.Nor -> arrayOf(op1AsReg())

            Opcode.Sll -> arrayOf(op1AsReg())
            Opcode.Srl -> arrayOf(op1AsReg())
            Opcode.Sra -> arrayOf(op1AsReg())
            Opcode.Sllv -> arrayOf(op1AsReg())
            Opcode.Srlv -> arrayOf(op1AsReg())
            Opcode.Srav -> arrayOf(op1AsReg())

            Opcode.Mult -> arrayOf(Reg.hi, Reg.lo)
            Opcode.Multu -> arrayOf(Reg.hi, Reg.lo)
            Opcode.Div -> arrayOf(Reg.hi, Reg.lo)
            Opcode.Divu -> arrayOf(Reg.hi, Reg.lo)
            Opcode.Mfhi -> arrayOf(op1AsReg())
            Opcode.Mthi -> arrayOf(Reg.hi)
            Opcode.Mflo -> arrayOf(op1AsReg())
            Opcode.Mtlo -> arrayOf(Reg.lo)

            Opcode.J -> arrayOf(Reg.pc)
            Opcode.Jal -> arrayOf(Reg.pc, Reg.ra)
            Opcode.Jr -> arrayOf(Reg.pc)
            Opcode.Jalr -> arrayOf(Reg.pc, op1AsReg())

            Opcode.Beq -> arrayOf(Reg.pc)
            Opcode.Bne -> arrayOf(Reg.pc)
            Opcode.Blez -> arrayOf(Reg.pc)
            Opcode.Bgtz -> arrayOf(Reg.pc)
            Opcode.Beql -> arrayOf(Reg.pc)
            Opcode.Bnel -> arrayOf(Reg.pc)
            Opcode.Blezl -> arrayOf(Reg.pc)
            Opcode.Bgtzl -> arrayOf(Reg.pc)

            Opcode.Bltz -> arrayOf(Reg.pc)
            Opcode.Bgez -> arrayOf(Reg.pc)
            Opcode.Bltzal -> arrayOf(Reg.pc)
            Opcode.Bgezall -> arrayOf(Reg.pc)
            Opcode.Bltzl -> arrayOf(Reg.pc)
            Opcode.Bgezl -> arrayOf(Reg.pc)
            Opcode.Bltzall -> arrayOf(Reg.pc)
            Opcode.Bgezal -> arrayOf(Reg.pc)

            Opcode.Syscall -> arrayOf()
            Opcode.Break -> arrayOf()

            Opcode.Tge -> arrayOf()
            Opcode.Tgeu -> arrayOf()
            Opcode.Tlt -> arrayOf()
            Opcode.Tltu -> arrayOf()
            Opcode.Teq -> arrayOf()
            Opcode.Tne -> arrayOf()

            Opcode.Tgei -> arrayOf()
            Opcode.Tgeiu -> arrayOf()
            Opcode.Tlti -> arrayOf()
            Opcode.Tltiu -> arrayOf()
            Opcode.Teqi -> arrayOf()
            Opcode.Tnei -> arrayOf()

            Opcode.Sync -> arrayOf()

            Opcode.Nop -> arrayOf()

            Opcode.Lwc1 -> arrayOf(op1AsFpuReg())
            Opcode.Swc1 -> arrayOf()

            Opcode.Mtc1 -> arrayOf(op2AsFpuReg())
            Opcode.Mfc1 -> arrayOf(op1AsReg())
            Opcode.Ctc1 -> arrayOf(op2AsFpuReg())
            Opcode.Cfc1 -> arrayOf(op1AsReg())

            Opcode.FpuAdd -> arrayOf(op1AsFpuReg())
            Opcode.FpuSub -> arrayOf(op1AsFpuReg())
            Opcode.FpuMul -> arrayOf(op1AsFpuReg())
            Opcode.FpuDiv -> arrayOf(op1AsFpuReg())
            Opcode.FpuAbs -> arrayOf(op1AsFpuReg())
            Opcode.FpuNeg -> arrayOf(op1AsFpuReg())
            Opcode.FpuSqrt -> arrayOf(op1AsFpuReg())
            Opcode.FpuRoundW -> arrayOf(op1AsFpuReg())
            Opcode.FpuTruncW -> arrayOf(op1AsFpuReg())
            Opcode.FpuCeilW -> arrayOf(op1AsFpuReg())
            Opcode.FpuFloorW -> arrayOf(op1AsFpuReg())

            Opcode.FpuCEq -> arrayOf(FpuReg.cc)
            Opcode.FpuCLe -> arrayOf(FpuReg.cc)
            Opcode.FpuCLt -> arrayOf(FpuReg.cc)

            Opcode.FpuCvtSW -> arrayOf(op1AsFpuReg())
            Opcode.FpuCvtWS -> arrayOf(op1AsFpuReg())

            Opcode.FpuMov -> arrayOf(op1AsFpuReg())

            Opcode.FpuBc1f -> arrayOf(Reg.pc)
            Opcode.FpuBc1t -> arrayOf(Reg.pc)
            Opcode.FpuBc1tl -> arrayOf(Reg.pc)
            Opcode.FpuBc1fl -> arrayOf(Reg.pc)
        }
    }

    /** Returns register list that were used in this instruction to perform its operation */
    fun getSourceRegisters(): Array<Any> {
        return when (opcode) {
            Opcode.Lb -> arrayOf(op2AsReg())
            Opcode.Lbu -> arrayOf(op2AsReg())
            Opcode.Sb -> arrayOf(op1AsReg(), op2AsReg())

            Opcode.Lh -> arrayOf(op2AsReg())
            Opcode.Lhu -> arrayOf(op2AsReg())
            Opcode.Sh -> arrayOf(op1AsReg(), op2AsReg())

            Opcode.Lw -> arrayOf(op2AsReg())
            Opcode.Sw -> arrayOf(op1AsReg(), op2AsReg())

            Opcode.Lwl -> arrayOf(op2AsReg())
            Opcode.Lwr -> arrayOf(op2AsReg())
            Opcode.Swl -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Swr -> arrayOf(op1AsReg(), op2AsReg())

            Opcode.Ll -> arrayOf(op2AsReg())
            Opcode.Sc -> arrayOf(op1AsReg(), op2AsReg())

            Opcode.Addi -> arrayOf(op2AsReg())
            Opcode.Addiu -> arrayOf(op2AsReg())
            Opcode.Slti -> arrayOf(op2AsReg())
            Opcode.Sltiu -> arrayOf(op2AsReg())
            Opcode.Andi -> arrayOf(op2AsReg())
            Opcode.Ori -> arrayOf(op2AsReg())
            Opcode.Xori -> arrayOf(op2AsReg())
            Opcode.Lui -> arrayOf()

            Opcode.Add -> arrayOf(op2AsReg(), op3AsReg())
            Opcode.Addu -> arrayOf(op2AsReg(), op3AsReg())
            Opcode.Sub -> arrayOf(op2AsReg(), op3AsReg())
            Opcode.Subu -> arrayOf(op2AsReg(), op3AsReg())

            Opcode.Slt -> arrayOf(op2AsReg(), op3AsReg())
            Opcode.Sltu -> arrayOf(op2AsReg(), op3AsReg())
            Opcode.And -> arrayOf(op2AsReg(), op3AsReg())
            Opcode.Or -> arrayOf(op2AsReg(), op3AsReg())
            Opcode.Xor -> arrayOf(op2AsReg(), op3AsReg())
            Opcode.Nor -> arrayOf(op2AsReg(), op3AsReg())

            Opcode.Sll -> arrayOf(op2AsReg())
            Opcode.Srl -> arrayOf(op2AsReg())
            Opcode.Sra -> arrayOf(op2AsReg())
            Opcode.Sllv -> arrayOf(op2AsReg(), op3AsReg())
            Opcode.Srlv -> arrayOf(op2AsReg(), op3AsReg())
            Opcode.Srav -> arrayOf(op2AsReg(), op3AsReg())

            Opcode.Mult -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Multu -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Div -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Divu -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Mfhi -> arrayOf(Reg.hi)
            Opcode.Mthi -> arrayOf(op1AsReg())
            Opcode.Mflo -> arrayOf(Reg.lo)
            Opcode.Mtlo -> arrayOf(op1AsReg())

            Opcode.J -> arrayOf()
            Opcode.Jal -> arrayOf()
            Opcode.Jr -> arrayOf(op1AsReg())
            Opcode.Jalr -> arrayOf(op2AsReg())

            Opcode.Beq -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Bne -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Blez -> arrayOf(op1AsReg())
            Opcode.Bgtz -> arrayOf(op1AsReg())
            Opcode.Beql -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Bnel -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Blezl -> arrayOf(op1AsReg())
            Opcode.Bgtzl -> arrayOf(op1AsReg())

            Opcode.Bltz -> arrayOf(op1AsReg())
            Opcode.Bgez -> arrayOf(op1AsReg())
            Opcode.Bltzal -> arrayOf(op1AsReg())
            Opcode.Bgezal -> arrayOf(op1AsReg())
            Opcode.Bgezl -> arrayOf(op1AsReg())
            Opcode.Bltzl -> arrayOf(op1AsReg())
            Opcode.Bltzall -> arrayOf(op1AsReg())
            Opcode.Bgezall -> arrayOf(op1AsReg())

            Opcode.Syscall -> arrayOf()
            Opcode.Break -> arrayOf()

            Opcode.Tge -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Tgeu -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Tlt -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Tltu -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Teq -> arrayOf(op1AsReg(), op2AsReg())
            Opcode.Tne -> arrayOf(op1AsReg(), op2AsReg())

            Opcode.Tgei -> arrayOf(op1AsReg())
            Opcode.Tgeiu -> arrayOf(op1AsReg())
            Opcode.Tlti -> arrayOf(op1AsReg())
            Opcode.Tltiu -> arrayOf(op1AsReg())
            Opcode.Teqi -> arrayOf(op1AsReg())
            Opcode.Tnei -> arrayOf(op1AsReg())

            Opcode.Sync -> arrayOf()

            Opcode.Nop -> arrayOf()

            Opcode.Lwc1 -> arrayOf(op2AsReg())
            Opcode.Swc1 -> arrayOf(op1AsFpuReg(), op2AsReg())

            Opcode.Mtc1 -> arrayOf(op1AsReg())
            Opcode.Mfc1 -> arrayOf(op2AsFpuReg())
            Opcode.Ctc1 -> arrayOf(op1AsReg())
            Opcode.Cfc1 -> arrayOf(op2AsFpuReg())

            Opcode.FpuAdd -> arrayOf(op2AsFpuReg(), op3AsFpuReg())
            Opcode.FpuSub -> arrayOf(op2AsFpuReg(), op3AsFpuReg())
            Opcode.FpuMul -> arrayOf(op2AsFpuReg(), op3AsFpuReg())
            Opcode.FpuDiv -> arrayOf(op2AsFpuReg(), op3AsFpuReg())
            Opcode.FpuAbs -> arrayOf(op2AsFpuReg())
            Opcode.FpuNeg -> arrayOf(op2AsFpuReg())
            Opcode.FpuSqrt -> arrayOf(op2AsFpuReg())
            Opcode.FpuRoundW -> arrayOf(op2AsFpuReg())
            Opcode.FpuTruncW -> arrayOf(op2AsFpuReg())
            Opcode.FpuCeilW -> arrayOf(op2AsFpuReg())
            Opcode.FpuFloorW -> arrayOf(op2AsFpuReg())

            Opcode.FpuCEq -> arrayOf(op1AsFpuReg(), op2AsFpuReg())
            Opcode.FpuCLe -> arrayOf(op1AsFpuReg(), op2AsFpuReg())
            Opcode.FpuCLt -> arrayOf(op1AsFpuReg(), op2AsFpuReg())

            Opcode.FpuCvtSW -> arrayOf(op2AsFpuReg())
            Opcode.FpuCvtWS -> arrayOf(op2AsFpuReg())

            Opcode.FpuMov -> arrayOf(op2AsFpuReg())

            Opcode.FpuBc1f -> arrayOf(FpuReg.cc)
            Opcode.FpuBc1t -> arrayOf(FpuReg.cc)
            Opcode.FpuBc1tl -> arrayOf(FpuReg.cc)
            Opcode.FpuBc1fl -> arrayOf(FpuReg.cc)
        }
    }

    override fun toString(): String {
        if (isMemoryRead() || isMemoryWrite()) return "${addr.toHex()}: $opcode $op1, $op3($op2)"
        if (op1 == null) return "${addr.toHex()}: $opcode"
        if (op2 == null) return "${addr.toHex()}: $opcode $op1"
        if (op3 == null) return "${addr.toHex()}: $opcode $op1, $op2"
        return "${addr.toHex()}: $opcode $op1, $op2, $op3"
    }
}
