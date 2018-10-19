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

package mist.shl

import mist.asm.*
import mist.io.ProjectIO
import mist.shl.ShlExpr.*
import mist.util.DecompLog
import mist.util.logTag

/** @author Kotcrab */

class ShlLifter(private val projectIO: ProjectIO, private val log: DecompLog) {
    val tag = logTag()

    fun lift(instr: List<Instr>): List<ShlInstr> {
        val shl = mutableListOf<ShlInstr>()
        instr.forEach {
            if (it.isMemoryRead()) {
                shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                        ShlMemLoad(mapMemOpcode(it.opcode), ShlAdd(shlAuto(it.op2), shlAuto(it.op3)))))
            } else if (it.isMemoryWrite()) {
                shl.add(ShlMemStoreInstr(it.addr, ShlMemStore(mapMemOpcode(it.opcode), ShlAdd(shlAuto(it.op2), shlAuto(it.op3)),
                        shlAuto(it.op1))))
            } else {
                when (it.opcode) {
                    Opcode.Addi, Opcode.Addiu -> {
                        when {
                            it.op2AsReg() == Reg.zero -> shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), shlAuto(it.op3)))
                            it.op3AsImm() == 0 -> shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), shlAuto(it.op2)))
                            it.op3AsImm() < 0 -> shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                    ShlSub(shlAuto(it.op2), ShlConst(Math.abs(it.op3AsImm())))))
                            else -> shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                    ShlAdd(shlAuto(it.op2), shlAuto(it.op3))))
                        }
                    }
                    Opcode.Add, Opcode.Addu -> {
                        if (it.op2AsReg() == Reg.zero && it.op3AsReg() == Reg.zero) {
                            shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlConst(0)))
                        } else if (it.op2AsReg() == Reg.zero) {
                            shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), shlAuto(it.op3)))
                        } else if (it.op3AsReg() == Reg.zero) {
                            shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), shlAuto(it.op2)))
                        } else {
                            shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                    ShlAdd(shlAuto(it.op2), shlAuto(it.op3))))
                        }
                    }
                    Opcode.Slti, Opcode.Sltiu, Opcode.Slt, Opcode.Sltu -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                ShlLessThan(shlAuto(it.op2), shlAuto(it.op3))))
                    }
                    Opcode.Andi, Opcode.And -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                ShlAnd(shlAuto(it.op2), shlAuto(it.op3))))
                    }
                    Opcode.Ori, Opcode.Or -> {
                        when {
                            it.matches(op3 = isReg(Reg.zero)) -> shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), shlAuto(it.op2)))
                            it.matches(op2 = isReg(Reg.zero)) -> shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), shlAuto(it.op3)))
                            else -> shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                    ShlOr(shlAuto(it.op2), shlAuto(it.op3))))
                        }
                    }
                    Opcode.Xori, Opcode.Xor -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                ShlXor(shlAuto(it.op2), shlAuto(it.op3))))
                    }
                    Opcode.Lui -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlConst(it.op2AsImm() shl 16)))
                    }
                    Opcode.Sub, Opcode.Subu -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                ShlSub(shlAuto(it.op2), shlAuto(it.op3))))
                    }
                    Opcode.Nor -> {
                        when {
                            it.op2AsReg() != Reg.zero && it.op3AsReg() == Reg.zero -> shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlNeg(shlAuto(it.op2))))
                            it.op2AsReg() == Reg.zero && it.op3AsReg() != Reg.zero -> shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlNeg(shlAuto(it.op3))))
                            else -> log.panic(tag, "nor can be only lifted to negation when one reg is set to reg zero")
                        }
                    }
                    Opcode.Sll, Opcode.Sllv -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                ShlSll(shlAuto(it.op2), shlAuto(it.op3))))
                    }
                    Opcode.Srl, Opcode.Srlv -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                ShlSrl(shlAuto(it.op2), shlAuto(it.op3))))
                    }
                    Opcode.Sra, Opcode.Srav -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                ShlSra(shlAuto(it.op2), shlAuto(it.op3))))
                    }
                    Opcode.Mult, Opcode.Multu -> {
                        shl.add(ShlAssignInstr(it.addr, ShlVar(Reg.LO),
                                ShlAnd(
                                        ShlMul(shlAuto(it.op1), shlAuto(it.op2)),
                                        ShlConst(0xFFFF)
                                )))
                        shl.add(ShlAssignInstr(it.addr, ShlVar(Reg.HI),
                                ShlSrl(
                                        ShlMul(shlAuto(it.op1), shlAuto(it.op2)),
                                        ShlConst(16)
                                )))
                    }
                    Opcode.Div, Opcode.Divu -> {
                        shl.add(ShlAssignInstr(it.addr, ShlVar(Reg.LO),
                                ShlAnd(
                                        ShlDiv(shlAuto(it.op1), shlAuto(it.op2)),
                                        ShlConst(0xFFFF)
                                )))
                        shl.add(ShlAssignInstr(it.addr, ShlVar(Reg.HI),
                                ShlSrl(
                                        ShlDiv(shlAuto(it.op1), shlAuto(it.op2)),
                                        ShlConst(16)
                                )))
                    }
                    Opcode.Mfhi -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlVar(Reg.HI)))
                    }
                    Opcode.Mthi -> {
                        shl.add(ShlAssignInstr(it.addr, ShlVar(Reg.HI), shlAuto(it.op1)))
                    }
                    Opcode.Mflo -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlVar(Reg.LO)))
                    }
                    Opcode.Mtlo -> {
                        shl.add(ShlAssignInstr(it.addr, ShlVar(Reg.LO), shlAuto(it.op1)))
                    }
                    Opcode.J, Opcode.Jr -> {
                        shl.add(ShlJumpInstr(it.addr, false, shlAuto(it.op1)))
                    }
                    Opcode.Jal -> {
                        val jalDef = projectIO.getFuncDefByOffset(it.op1AsImm())
                        if (jalDef == null) {
                            shl.add(ShlJumpInstr(it.addr, true, shlAuto(it.op1)))
                        } else {
                            var returnReg: ShlVar? = null
                            if (jalDef.returnType !in arrayOf("void")) {
                                returnReg = ShlVar("v0")
                            }
                            val args = mutableMapOf<String, ShlExpr>()
                            val freeArgs = shlArgRegisters.toMutableList()
                            jalDef.arguments.forEach { arg ->
                                if (freeArgs.remove(arg.register) == false) log.panic(tag, "conflicting func. arg definitions (free arg list exhausted)")
                                if (arg.type.endsWith("...")) { //vararg
                                    args[arg.register] = ShlVar(arg.register)
                                    freeArgs.forEach { freeArg ->
                                        args[freeArg] = ShlVar(freeArg)
                                    }
                                    freeArgs.clear()
                                } else {
                                    args[arg.register] = ShlVar(arg.register)
                                }
                            }
                            shl.add(ShlCallInstr(it.addr, returnReg, ShlCall(it.op1AsImm(), args)))
                        }
                    }
                    Opcode.Jalr -> {
                        if (it.op1AsReg() != Reg.ra) log.panic(tag, "jalr doesn't use ra as return register")
                        shl.add(ShlJumpInstr(it.addr, true, shlAuto(it.op2)))
                    }
                    Opcode.Nop -> {
                        shl.add(ShlNopInstr(it.addr))
                    }
                    Opcode.Beq, Opcode.Beql -> {
                        shl.add(ShlBranchInstr(it.addr, it.opcode == Opcode.Beql,
                                ShlEqual(shlAuto(it.op1), shlAuto(it.op2))))
                    }
                    Opcode.Bne, Opcode.Bnel -> {
                        shl.add(ShlBranchInstr(it.addr, it.opcode == Opcode.Bnel,
                                ShlNotEqual(shlAuto(it.op1), shlAuto(it.op2))))
                    }
                    Opcode.Blez, Opcode.Blezl -> {
                        shl.add(ShlBranchInstr(it.addr, it.opcode == Opcode.Blezl,
                                ShlLessEqualThan(shlAuto(it.op1), ShlConst(0))))
                    }
                    Opcode.Bgtz, Opcode.Bgtzl -> {
                        shl.add(ShlBranchInstr(it.addr, it.opcode == Opcode.Bgtzl,
                                ShlGreaterThan(shlAuto(it.op1), ShlConst(0))))
                    }
                    Opcode.Bltz, Opcode.Bltzl -> {
                        shl.add(ShlBranchInstr(it.addr, it.opcode == Opcode.Bltzl,
                                ShlLessThan(shlAuto(it.op1), ShlConst(0))))
                    }
                    Opcode.Bgez, Opcode.Bgezl -> {
                        shl.add(ShlBranchInstr(it.addr, it.opcode == Opcode.Bgezl,
                                ShlGreaterEqualThan(shlAuto(it.op1), ShlConst(0))))
                    }
                    Opcode.Bltzal, Opcode.Bltzall -> {
                        shl.add(ShlBranchInstr(it.addr, it.opcode == Opcode.Bltzall,
                                ShlLessThan(shlAuto(it.op1), ShlConst(0)), true))
                    }
                    Opcode.Bgezal, Opcode.Bgezall -> {
                        shl.add(ShlBranchInstr(it.addr, it.opcode == Opcode.Bgezall,
                                ShlGreaterEqualThan(shlAuto(it.op1), ShlConst(0)), true))
                    }

//                    Syscall(Type.Other),
//                    Break(Type.Other),
//
//                    Tge(Type.Trap),
//                    Tgeu(Type.Trap),
//                    Tlt(Type.Trap),
//                    Tltu(Type.Trap),
//                    Teq(Type.Trap),
//                    Tne(Type.Trap),
//
//                    Tgei(Type.Trap),
//                    Tgeiu(Type.Trap),
//                    Tlti(Type.Trap),
//                    Tltiu(Type.Trap),
//                    Teqi(Type.Trap),
//                    Tnei(Type.Trap),
//
//                    Sync(Type.Other),
//
                    Opcode.Mtc1 -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op2), shlAuto(it.op1)))
                    }
                    Opcode.Mfc1 -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), shlAuto(it.op2)))
                    }
//                    Ctc1(Type.Other),
//                    Cfc1(Type.Other),
//
                    Opcode.FpuAdd -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                ShlAdd(shlAuto(it.op2), shlAuto(it.op3))))
                    }
                    Opcode.FpuSub -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1),
                                ShlSub(shlAuto(it.op2), shlAuto(it.op3))))
                    }
                    Opcode.FpuMul -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlMul(shlAuto(it.op2), shlAuto(it.op3))))
                    }
                    Opcode.FpuDiv -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlDiv(shlAuto(it.op2), shlAuto(it.op3))))
                    }
                    Opcode.FpuAbs -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlAbs(shlAuto(it.op2))))
                    }
                    Opcode.FpuNeg -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlNeg(shlAuto(it.op2))))
                    }
                    Opcode.FpuSqrt -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlSqrt(shlAuto(it.op2))))
                    }
                    Opcode.FpuRoundW -> { //TODO untested
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlRound(shlAuto(it.op2))))
                    }
                    Opcode.FpuTruncW -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlTrunc(shlAuto(it.op2))))
                    }
                    Opcode.FpuCeilW -> { //TODO untested
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlCeil(shlAuto(it.op2))))
                    }
                    Opcode.FpuFloorW -> { //TODO untested
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlFloor(shlAuto(it.op2))))
                    }
                    Opcode.FpuCEq -> {
                        shl.add(ShlAssignInstr(it.addr, ShlVar(FpuReg.cc), ShlEqual(shlAuto(it.op1), shlAuto(it.op2))))
                    }
                    Opcode.FpuCLe -> {
                        shl.add(ShlAssignInstr(it.addr, ShlVar(FpuReg.cc), ShlLessEqualThan(shlAuto(it.op1), shlAuto(it.op2))))
                    }
                    Opcode.FpuCLt -> {
                        shl.add(ShlAssignInstr(it.addr, ShlVar(FpuReg.cc), ShlLessThan(shlAuto(it.op1), shlAuto(it.op2))))
                    }
                    Opcode.FpuCvtSW -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlToFloat(shlAuto(it.op2))))
                    }
                    Opcode.FpuCvtWS -> { //TODO untested
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), ShlToInt(shlAuto(it.op2))))
                    }
                    Opcode.FpuMov -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.op1), shlAuto(it.op2)))
                    }

                    Opcode.FpuBc1t, Opcode.FpuBc1tl -> {
                        shl.add(ShlBranchInstr(it.addr, it.opcode == Opcode.FpuBc1tl,
                                ShlEqual(ShlVar(FpuReg.cc), ShlConst(1)), false))
                    }
                    Opcode.FpuBc1f, Opcode.FpuBc1fl -> {
                        shl.add(ShlBranchInstr(it.addr, it.opcode == Opcode.FpuBc1fl,
                                ShlEqual(ShlVar(FpuReg.cc), ShlConst(0)), false))
                    }

                    else -> log.panic(tag, "not implemented ${it.opcode}")
                }
            }
        }
        return shl
    }

    private fun mapMemOpcode(opcode: Opcode): ShlMemOpcode {
        return when (opcode) {
            Opcode.Lb -> ShlMemOpcode.Lb
            Opcode.Lbu -> ShlMemOpcode.Lbu
            Opcode.Sb -> ShlMemOpcode.Sb
            Opcode.Lh -> ShlMemOpcode.Lh
            Opcode.Lhu -> ShlMemOpcode.Lhu
            Opcode.Sh -> ShlMemOpcode.Sh
            Opcode.Lw -> ShlMemOpcode.Lw
            Opcode.Sw -> ShlMemOpcode.Sw
            Opcode.Lwl -> ShlMemOpcode.Lwl
            Opcode.Lwr -> ShlMemOpcode.Lwr
            Opcode.Swl -> ShlMemOpcode.Swl
            Opcode.Swr -> ShlMemOpcode.Swr
            Opcode.Ll -> ShlMemOpcode.Ll
            Opcode.Sc -> ShlMemOpcode.Sc
            Opcode.Lwc1 -> ShlMemOpcode.Lw
            Opcode.Swc1 -> ShlMemOpcode.Sw
            else -> log.panic(tag, "not an memory opcode: $opcode")
        }
    }

    private fun shlAuto(op: Operand?): ShlExpr {
        return when (op) {
            is Operand.Reg -> if (op.reg == Reg.zero) ShlConst(0) else ShlVar(op.reg)
            is Operand.FpuReg -> ShlVar(op.reg)
            is Operand.Imm -> ShlConst(op.value)
            else -> log.panic(tag, "can't convert operand to ShlExpr: $op")
        }
    }
}
