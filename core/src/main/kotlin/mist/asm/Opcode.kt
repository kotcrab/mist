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

enum class Opcode(val type: Type,
                  val memoryRead: Boolean = false,
                  val memoryWrite: Boolean = false,
                  val branch: Boolean = false,
                  val branchLikely: Boolean = false,
                  val jump: Boolean = false,
                  val jumpLinkRegister: Boolean = false) {
    Lb(Type.Memory, memoryRead = true),
    Lbu(Type.Memory, memoryRead = true),
    Sb(Type.Memory, memoryWrite = true),

    Lh(Type.Memory, memoryRead = true),
    Lhu(Type.Memory, memoryRead = true),
    Sh(Type.Memory, memoryWrite = true),

    Lw(Type.Memory, memoryRead = true),
    Sw(Type.Memory, memoryWrite = true),

    Lwl(Type.Memory, memoryRead = true),
    Lwr(Type.Memory, memoryRead = true),
    Swl(Type.Memory, memoryWrite = true),
    Swr(Type.Memory, memoryWrite = true),

    Ll(Type.Memory, memoryRead = true),
    Sc(Type.Memory, memoryWrite = true),

    Addi(Type.Arithmetic),
    Addiu(Type.Arithmetic),
    Slti(Type.Arithmetic),
    Sltiu(Type.Arithmetic),
    Andi(Type.Arithmetic),
    Ori(Type.Arithmetic),
    Xori(Type.Arithmetic),
    Lui(Type.Arithmetic),

    Add(Type.Arithmetic),
    Addu(Type.Arithmetic),
    Sub(Type.Arithmetic),
    Subu(Type.Arithmetic),

    Slt(Type.Arithmetic),
    Sltu(Type.Arithmetic),
    And(Type.Arithmetic),
    Or(Type.Arithmetic),
    Xor(Type.Arithmetic),
    Nor(Type.Arithmetic),

    Sll(Type.Arithmetic),
    Srl(Type.Arithmetic),
    Sra(Type.Arithmetic),
    Sllv(Type.Arithmetic),
    Srlv(Type.Arithmetic),
    Srav(Type.Arithmetic),

    Mult(Type.Arithmetic),
    Multu(Type.Arithmetic),
    Div(Type.Arithmetic),
    Divu(Type.Arithmetic),
    Mfhi(Type.Arithmetic),
    Mthi(Type.Arithmetic),
    Mflo(Type.Arithmetic),
    Mtlo(Type.Arithmetic),

    J(Type.ControlFlow, jump = true),
    Jal(Type.ControlFlow, jump = true, jumpLinkRegister = true),
    Jr(Type.ControlFlow, jump = true),
    Jalr(Type.ControlFlow, jump = true, jumpLinkRegister = true),

    Beq(Type.ControlFlow, branch = true),
    Bne(Type.ControlFlow, branch = true),
    Blez(Type.ControlFlow, branch = true),
    Bgtz(Type.ControlFlow, branch = true),
    Beql(Type.ControlFlow, branch = true, branchLikely = true),
    Bnel(Type.ControlFlow, branch = true, branchLikely = true),
    Blezl(Type.ControlFlow, branch = true, branchLikely = true),
    Bgtzl(Type.ControlFlow, branch = true, branchLikely = true),

    Bltz(Type.ControlFlow, branch = true),
    Bgez(Type.ControlFlow, branch = true),
    Bltzal(Type.ControlFlow, branch = true),
    Bgezal(Type.ControlFlow, branch = true),
    Bltzl(Type.ControlFlow, branch = true, branchLikely = true),
    Bgezl(Type.ControlFlow, branch = true, branchLikely = true),
    Bltzall(Type.ControlFlow, branch = true, branchLikely = true),
    Bgezall(Type.ControlFlow, branch = true, branchLikely = true),

    Syscall(Type.Other),
    Break(Type.Other),

    Tge(Type.Trap),
    Tgeu(Type.Trap),
    Tlt(Type.Trap),
    Tltu(Type.Trap),
    Teq(Type.Trap),
    Tne(Type.Trap),

    Tgei(Type.Trap),
    Tgeiu(Type.Trap),
    Tlti(Type.Trap),
    Tltiu(Type.Trap),
    Teqi(Type.Trap),
    Tnei(Type.Trap),

    Nop(Type.Other),

    Sync(Type.Other),

    Lwc1(Type.Memory, memoryRead = true),
    Swc1(Type.Memory, memoryWrite = true),

    Mtc1(Type.Other),
    Mfc1(Type.Other),
    Ctc1(Type.Other),
    Cfc1(Type.Other),

    FpuAdd(Type.Arithmetic),
    FpuSub(Type.Arithmetic),
    FpuMul(Type.Arithmetic),
    FpuDiv(Type.Arithmetic),
    FpuAbs(Type.Arithmetic),
    FpuNeg(Type.Arithmetic),
    FpuSqrt(Type.Arithmetic),
    FpuRoundW(Type.Arithmetic),
    FpuTruncW(Type.Arithmetic),
    FpuCeilW(Type.Arithmetic),
    FpuFloorW(Type.Arithmetic),
    FpuCEq(Type.Arithmetic),
    FpuCLe(Type.Arithmetic),
    FpuCLt(Type.Arithmetic),
    FpuCvtSW(Type.Arithmetic),
    FpuCvtWS(Type.Arithmetic),
    FpuMov(Type.Arithmetic),

    FpuBc1f(Type.ControlFlow, branch = true),
    FpuBc1t(Type.ControlFlow, branch = true),
    FpuBc1tl(Type.ControlFlow, branch = true, branchLikely = true),
    FpuBc1fl(Type.ControlFlow, branch = true, branchLikely = true),
    ;

    override fun toString(): String {
        return super.toString().toLowerCase()
    }

    enum class Type {
        Memory,
        Arithmetic,
        /** Warning: all control flow type opcodes will be affected by branch delay slot */
        ControlFlow,
        Trap,
        Other
    }
}
