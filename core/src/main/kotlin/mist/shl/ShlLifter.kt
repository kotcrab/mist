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
import mist.asm.mips.*
import mist.asm.mips.GprReg
import mist.shl.ShlExpr.*
import mist.util.DecompLog
import mist.util.logTag

/** @author Kotcrab */

class ShlLifter(private val projectIO: ProjectIO, private val log: DecompLog) {
    val tag = logTag()

    fun lift(instr: List<MipsInstr>): List<ShlInstr> {
        val shl = mutableListOf<ShlInstr>()
        instr.forEach {
            if (it.hasFlag(MemoryRead)) {
                shl.add(
                    ShlAssignInstr(
                        it.addr, shlAuto(it.operands[0]),
                        ShlMemLoad(mapMemOpcode(it.opcode), ShlAdd(shlAuto(it.operands[1]), shlAuto(it.operands[2])))
                    )
                )
            } else if (it.hasFlag(MemoryWrite)) {
                shl.add(
                    ShlMemStoreInstr(
                        it.addr, ShlMemStore(
                            mapMemOpcode(it.opcode), ShlAdd(shlAuto(it.operands[1]), shlAuto(it.operands[2])),
                            shlAuto(it.operands[0])
                        )
                    )
                )
            } else {
                when (it.opcode) {
                    Addi, Addiu -> {
                        when {
                            it.op1AsReg() == GprReg.Zero -> shl.add(
                                ShlAssignInstr(
                                    it.addr,
                                    shlAuto(it.operands[0]),
                                    shlAuto(it.operands[2])
                                )
                            )
                            it.op2AsImm() == 0 -> shl.add(
                                ShlAssignInstr(
                                    it.addr,
                                    shlAuto(it.operands[0]),
                                    shlAuto(it.operands[1])
                                )
                            )
                            it.op2AsImm() < 0 -> shl.add(
                                ShlAssignInstr(
                                    it.addr, shlAuto(it.operands[0]),
                                    ShlSub(shlAuto(it.operands[1]), ShlConst(Math.abs(it.op2AsImm())))
                                )
                            )
                            else -> shl.add(
                                ShlAssignInstr(
                                    it.addr, shlAuto(it.operands[0]),
                                    ShlAdd(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                                )
                            )
                        }
                    }
                    Add, Addu -> {
                        if (it.op1AsReg() == GprReg.Zero && it.op2AsReg() == GprReg.Zero) {
                            shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlConst(0)))
                        } else if (it.op1AsReg() == GprReg.Zero) {
                            shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), shlAuto(it.operands[2])))
                        } else if (it.op2AsReg() == GprReg.Zero) {
                            shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), shlAuto(it.operands[1])))
                        } else {
                            shl.add(
                                ShlAssignInstr(
                                    it.addr, shlAuto(it.operands[0]),
                                    ShlAdd(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                                )
                            )
                        }
                    }
                    Slti, Sltiu, Slt, Sltu -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr, shlAuto(it.operands[0]),
                                ShlLessThan(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                            )
                        )
                    }
                    Andi, And -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr, shlAuto(it.operands[0]),
                                ShlAnd(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                            )
                        )
                    }
                    Ori, Or -> {
                        when {
                            it.matches(op2 = isReg(GprReg.Zero)) -> shl.add(
                                ShlAssignInstr(
                                    it.addr,
                                    shlAuto(it.operands[0]),
                                    shlAuto(it.operands[1])
                                )
                            )
                            it.matches(op1 = isReg(GprReg.Zero)) -> shl.add(
                                ShlAssignInstr(
                                    it.addr,
                                    shlAuto(it.operands[0]),
                                    shlAuto(it.operands[2])
                                )
                            )
                            else -> shl.add(
                                ShlAssignInstr(
                                    it.addr, shlAuto(it.operands[0]),
                                    ShlOr(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                                )
                            )
                        }
                    }
                    Xori, Xor -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr, shlAuto(it.operands[0]),
                                ShlXor(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                            )
                        )
                    }
                    Lui -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlConst(it.op1AsImm() shl 16)))
                    }
                    Sub, Subu -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr, shlAuto(it.operands[0]),
                                ShlSub(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                            )
                        )
                    }
                    Nor -> {
                        when {
                            it.op1AsReg() != GprReg.Zero && it.op2AsReg() == GprReg.Zero -> shl.add(
                                ShlAssignInstr(
                                    it.addr,
                                    shlAuto(it.operands[0]),
                                    ShlNeg(shlAuto(it.operands[1]))
                                )
                            )
                            it.op1AsReg() == GprReg.Zero && it.op2AsReg() != GprReg.Zero -> shl.add(
                                ShlAssignInstr(
                                    it.addr,
                                    shlAuto(it.operands[0]),
                                    ShlNeg(shlAuto(it.operands[2]))
                                )
                            )
                            else -> log.panic(tag, "nor can be only lifted to negation when one reg is set to reg zero")
                        }
                    }
                    Sll, Sllv -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr, shlAuto(it.operands[0]),
                                ShlSll(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                            )
                        )
                    }
                    Srl, Srlv -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr, shlAuto(it.operands[0]),
                                ShlSrl(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                            )
                        )
                    }
                    Sra, Srav -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr, shlAuto(it.operands[0]),
                                ShlSra(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                            )
                        )
                    }
                    Mult, Multu -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr, ShlVar(GprReg.Lo),
                                ShlAnd(
                                    ShlMul(shlAuto(it.operands[0]), shlAuto(it.operands[1])),
                                    ShlConst(0xFFFF)
                                )
                            )
                        )
                        shl.add(
                            ShlAssignInstr(
                                it.addr, ShlVar(GprReg.Hi),
                                ShlSrl(
                                    ShlMul(shlAuto(it.operands[0]), shlAuto(it.operands[1])),
                                    ShlConst(16)
                                )
                            )
                        )
                    }
                    Div, Divu -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr, ShlVar(GprReg.Lo),
                                ShlAnd(
                                    ShlDiv(shlAuto(it.operands[0]), shlAuto(it.operands[1])),
                                    ShlConst(0xFFFF)
                                )
                            )
                        )
                        shl.add(
                            ShlAssignInstr(
                                it.addr, ShlVar(GprReg.Hi),
                                ShlSrl(
                                    ShlDiv(shlAuto(it.operands[0]), shlAuto(it.operands[1])),
                                    ShlConst(16)
                                )
                            )
                        )
                    }
                    Mfhi -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlVar(GprReg.Hi)))
                    }
                    Mthi -> {
                        shl.add(ShlAssignInstr(it.addr, ShlVar(GprReg.Hi), shlAuto(it.operands[0])))
                    }
                    Mflo -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlVar(GprReg.Lo)))
                    }
                    Mtlo -> {
                        shl.add(ShlAssignInstr(it.addr, ShlVar(GprReg.Lo), shlAuto(it.operands[0])))
                    }
                    J, Jr -> {
                        shl.add(ShlJumpInstr(it.addr, false, shlAuto(it.operands[0])))
                    }
                    Jal -> {
                        val jalDef = projectIO.getFuncDefByOffset(it.op0AsImm())
                        if (jalDef == null) {
                            shl.add(ShlJumpInstr(it.addr, true, shlAuto(it.operands[0])))
                        } else {
                            var returnReg: ShlVar? = null
                            if (jalDef.returnType !in arrayOf("void")) {
                                returnReg = ShlVar("v0")
                            }
                            val args = mutableMapOf<String, ShlExpr>()
                            val freeArgs = shlArgRegisters.toMutableList()
                            jalDef.arguments.forEach { arg ->
                                if (freeArgs.remove(arg.register) == false) log.panic(
                                    tag,
                                    "conflicting func. arg definitions (free arg list exhausted)"
                                )
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
                            shl.add(ShlCallInstr(it.addr, returnReg, ShlCall(it.op0AsImm(), args)))
                        }
                    }
                    Jalr -> {
                        if (it.op0AsReg() != GprReg.Ra) log.panic(tag, "jalr doesn't use ra as return register")
                        shl.add(ShlJumpInstr(it.addr, true, shlAuto(it.operands[1])))
                    }
                    Nop -> {
                        shl.add(ShlNopInstr(it.addr))
                    }
                    Beq, Beql -> {
                        shl.add(
                            ShlBranchInstr(
                                it.addr, it.opcode == Beql,
                                ShlEqual(shlAuto(it.operands[0]), shlAuto(it.operands[1]))
                            )
                        )
                    }
                    Bne, Bnel -> {
                        shl.add(
                            ShlBranchInstr(
                                it.addr, it.opcode == Bnel,
                                ShlNotEqual(shlAuto(it.operands[0]), shlAuto(it.operands[1]))
                            )
                        )
                    }
                    Blez, Blezl -> {
                        shl.add(
                            ShlBranchInstr(
                                it.addr, it.opcode == Blezl,
                                ShlLessEqualThan(shlAuto(it.operands[0]), ShlConst(0))
                            )
                        )
                    }
                    Bgtz, Bgtzl -> {
                        shl.add(
                            ShlBranchInstr(
                                it.addr, it.opcode == Bgtzl,
                                ShlGreaterThan(shlAuto(it.operands[0]), ShlConst(0))
                            )
                        )
                    }
                    Bltz, Bltzl -> {
                        shl.add(
                            ShlBranchInstr(
                                it.addr, it.opcode == Bltzl,
                                ShlLessThan(shlAuto(it.operands[0]), ShlConst(0))
                            )
                        )
                    }
                    Bgez, Bgezl -> {
                        shl.add(
                            ShlBranchInstr(
                                it.addr, it.opcode == Bgezl,
                                ShlGreaterEqualThan(shlAuto(it.operands[0]), ShlConst(0))
                            )
                        )
                    }
                    Bltzal, Bltzall -> {
                        shl.add(
                            ShlBranchInstr(
                                it.addr, it.opcode == Bltzall,
                                ShlLessThan(shlAuto(it.operands[0]), ShlConst(0)), true
                            )
                        )
                    }
                    Bgezal, Bgezall -> {
                        shl.add(
                            ShlBranchInstr(
                                it.addr, it.opcode == Bgezall,
                                ShlGreaterEqualThan(shlAuto(it.operands[0]), ShlConst(0)), true
                            )
                        )
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
                    FpuMtc1 -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[1]), shlAuto(it.operands[0])))
                    }
                    FpuMfc1 -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), shlAuto(it.operands[1])))
                    }
//                    Ctc1(Type.Other),
//                    Cfc1(Type.Other),
//
                    FpuAddS -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr, shlAuto(it.operands[0]),
                                ShlAdd(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                            )
                        )
                    }
                    FpuSubS -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr, shlAuto(it.operands[0]),
                                ShlSub(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                            )
                        )
                    }
                    FpuMulS -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr,
                                shlAuto(it.operands[0]),
                                ShlMul(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                            )
                        )
                    }
                    FpuDivS -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr,
                                shlAuto(it.operands[0]),
                                ShlDiv(shlAuto(it.operands[1]), shlAuto(it.operands[2]))
                            )
                        )
                    }
                    FpuAbsS -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlAbs(shlAuto(it.operands[1]))))
                    }
                    FpuNegS -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlNeg(shlAuto(it.operands[1]))))
                    }
                    FpuSqrtS -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlSqrt(shlAuto(it.operands[1]))))
                    }
                    FpuRoundWS -> { //TODO untested
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlRound(shlAuto(it.operands[1]))))
                    }
                    FpuTruncWS -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlTrunc(shlAuto(it.operands[1]))))
                    }
                    FpuCeilWS -> { //TODO untested
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlCeil(shlAuto(it.operands[1]))))
                    }
                    FpuFloorWS -> { //TODO untested
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlFloor(shlAuto(it.operands[1]))))
                    }
                    FpuCEqS -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr,
                                ShlVar(FpuReg.Cc0),
                                ShlEqual(shlAuto(it.operands[0]), shlAuto(it.operands[1]))
                            )
                        )
                    }
                    FpuCLeS -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr,
                                ShlVar(FpuReg.Cc0),
                                ShlLessEqualThan(shlAuto(it.operands[0]), shlAuto(it.operands[1]))
                            )
                        )
                    }
                    FpuCLtS -> {
                        shl.add(
                            ShlAssignInstr(
                                it.addr,
                                ShlVar(FpuReg.Cc0),
                                ShlLessThan(shlAuto(it.operands[0]), shlAuto(it.operands[1]))
                            )
                        )
                    }
                    FpuCvtSW -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlToFloat(shlAuto(it.operands[1]))))
                    }
                    FpuCvtWS -> { //TODO untested
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), ShlToInt(shlAuto(it.operands[1]))))
                    }
                    FpuMovS -> {
                        shl.add(ShlAssignInstr(it.addr, shlAuto(it.operands[0]), shlAuto(it.operands[1])))
                    }

                    FpuBc1t, FpuBc1tl -> {
                        shl.add(
                            ShlBranchInstr(
                                it.addr, it.opcode == FpuBc1tl,
                                ShlEqual(ShlVar(FpuReg.Cc0), ShlConst(1)), false
                            )
                        )
                    }
                    FpuBc1f, FpuBc1fl -> {
                        shl.add(
                            ShlBranchInstr(
                                it.addr, it.opcode == FpuBc1fl,
                                ShlEqual(ShlVar(FpuReg.Cc0), ShlConst(0)), false
                            )
                        )
                    }

                    else -> log.panic(tag, "not implemented ${it.opcode}")
                }
            }
        }
        return shl
    }

    private fun mapMemOpcode(opcode: Opcode): ShlMemOpcode {
        return when (opcode) {
            Lb -> ShlMemOpcode.Lb
            Lbu -> ShlMemOpcode.Lbu
            Sb -> ShlMemOpcode.Sb
            Lh -> ShlMemOpcode.Lh
            Lhu -> ShlMemOpcode.Lhu
            Sh -> ShlMemOpcode.Sh
            Lw -> ShlMemOpcode.Lw
            Sw -> ShlMemOpcode.Sw
            Lwl -> ShlMemOpcode.Lwl
            Lwr -> ShlMemOpcode.Lwr
            Swl -> ShlMemOpcode.Swl
            Swr -> ShlMemOpcode.Swr
            Ll -> ShlMemOpcode.Ll
            Sc -> ShlMemOpcode.Sc
            Lwc1 -> ShlMemOpcode.Lw
            Swc1 -> ShlMemOpcode.Sw
            else -> log.panic(tag, "not an memory opcode: $opcode")
        }
    }

    private fun shlAuto(op: Operand?): ShlExpr {
        return when (op) {
            is RegOperand -> if (op.reg == GprReg.Zero) ShlConst(0) else ShlVar(op.reg)
            is ImmOperand -> ShlConst(op.value)
            else -> log.panic(tag, "can't convert operand to ShlExpr: $op")
        }
    }
}
