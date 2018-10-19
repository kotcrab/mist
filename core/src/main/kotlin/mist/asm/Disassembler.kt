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
import kio.util.toWHex
import mist.io.BinLoader

/** @author Kotcrab */
class FunctionDef(val name: String, val offset: Int, val len: Int)

fun disassemble(loader: BinLoader, function: FunctionDef): Disassembly {
    return disassemble(loader, function.name, function.offset, function.len)
}

fun disassemble(loader: BinLoader, funcName: String, funcOffset: Int, funcLen: Int): Disassembly {
    val COP1 = 0b010_001
    val FMT_S = 16
    val FMT_D = 17
    val FMT_W = 20

    if (funcOffset % 4 != 0) error("offset must be multiply of 4")
    if (funcLen % 4 != 0) error("length must be multiply of 4")
    val decoded = mutableListOf<Instr>()

    fun handleUnknownInstr(instrCount: Int): Nothing {
        error("unknown instruction at offset ${(instrCount * 4).toWHex()}, address ${(funcOffset + instrCount * 4).toWHex()}")
    }

    repeat(funcLen / 4) { instrCount ->
        val vAddr = funcOffset + instrCount * 4
        val instr = loader.readInt(vAddr)
        if (instr == 0) {
            decoded.add(Instr(vAddr, Opcode.Nop))
            return@repeat
        }
        val opcode = instr ushr 26
        if (opcode == 0) { // R instruction
            val rs = Reg.forId(instr ushr 21 and 0x1F)
            val rt = Reg.forId(instr ushr 16 and 0x1F)
            val rd = Reg.forId(instr ushr 11 and 0x1F)
            val shift = instr ushr 6 and 0x1F
            val funct = instr and 0x3F

            val instance = when (funct) {
                0b100_000 -> Instr(vAddr, Opcode.Add, Operand.Reg(rd), Operand.Reg(rs), Operand.Reg(rt))
                0b100_001 -> Instr(vAddr, Opcode.Addu, Operand.Reg(rd), Operand.Reg(rs), Operand.Reg(rt))
                0b100_010 -> Instr(vAddr, Opcode.Sub, Operand.Reg(rd), Operand.Reg(rs), Operand.Reg(rt))
                0b100_011 -> Instr(vAddr, Opcode.Subu, Operand.Reg(rd), Operand.Reg(rs), Operand.Reg(rt))

                0b101_010 -> Instr(vAddr, Opcode.Slt, Operand.Reg(rd), Operand.Reg(rs), Operand.Reg(rt))
                0b101_011 -> Instr(vAddr, Opcode.Sltu, Operand.Reg(rd), Operand.Reg(rs), Operand.Reg(rt))
                0b100_100 -> Instr(vAddr, Opcode.And, Operand.Reg(rd), Operand.Reg(rs), Operand.Reg(rt))
                0b100_101 -> Instr(vAddr, Opcode.Or, Operand.Reg(rd), Operand.Reg(rs), Operand.Reg(rt))
                0b100_110 -> Instr(vAddr, Opcode.Xor, Operand.Reg(rd), Operand.Reg(rs), Operand.Reg(rt))
                0b100_111 -> Instr(vAddr, Opcode.Nor, Operand.Reg(rd), Operand.Reg(rs), Operand.Reg(rt))

                0b000_000 -> Instr(vAddr, Opcode.Sll, Operand.Reg(rd), Operand.Reg(rt), Operand.Imm(shift))
                0b000_010 -> Instr(vAddr, Opcode.Srl, Operand.Reg(rd), Operand.Reg(rt), Operand.Imm(shift))
                0b000_011 -> Instr(vAddr, Opcode.Sra, Operand.Reg(rd), Operand.Reg(rt), Operand.Imm(shift))
                0b000_100 -> Instr(vAddr, Opcode.Sllv, Operand.Reg(rd), Operand.Reg(rt), Operand.Reg(rs))
                0b000_110 -> Instr(vAddr, Opcode.Srlv, Operand.Reg(rd), Operand.Reg(rt), Operand.Reg(rs))
                0b000_111 -> Instr(vAddr, Opcode.Srav, Operand.Reg(rd), Operand.Reg(rt), Operand.Reg(rs))

                0b011_000 -> Instr(vAddr, Opcode.Mult, Operand.Reg(rs), Operand.Reg(rt))
                0b011_001 -> Instr(vAddr, Opcode.Multu, Operand.Reg(rs), Operand.Reg(rt))
                0b011_010 -> Instr(vAddr, Opcode.Div, Operand.Reg(rs), Operand.Reg(rt))
                0b011_011 -> Instr(vAddr, Opcode.Divu, Operand.Reg(rs), Operand.Reg(rt))
                0b010_000 -> Instr(vAddr, Opcode.Mfhi, Operand.Reg(rd))
                0b010_001 -> Instr(vAddr, Opcode.Mthi, Operand.Reg(rs))
                0b010_010 -> Instr(vAddr, Opcode.Mflo, Operand.Reg(rd))
                0b010_011 -> Instr(vAddr, Opcode.Mtlo, Operand.Reg(rs))

                0b001_000 -> Instr(vAddr, Opcode.Jr, Operand.Reg(rs))
                0b001_001 -> Instr(vAddr, Opcode.Jalr, Operand.Reg(rd), Operand.Reg(rs))

                0b001_100 -> Instr(vAddr, Opcode.Syscall, Operand.Imm(instr ushr 6 and 0xFFFF))
                0b001_101 -> Instr(vAddr, Opcode.Break, Operand.Imm(instr ushr 6 and 0xFFFF))

                0b110_000 -> Instr(vAddr, Opcode.Tge, Operand.Reg(rs), Operand.Reg(rt), Operand.Imm(rd.id shl 5 or shift))
                0b110_001 -> Instr(vAddr, Opcode.Tgeu, Operand.Reg(rs), Operand.Reg(rt), Operand.Imm(rd.id shl 5 or shift))
                0b110_010 -> Instr(vAddr, Opcode.Tlt, Operand.Reg(rs), Operand.Reg(rt), Operand.Imm(rd.id shl 5 or shift))
                0b110_011 -> Instr(vAddr, Opcode.Tltu, Operand.Reg(rs), Operand.Reg(rt), Operand.Imm(rd.id shl 5 or shift))
                0b110_100 -> Instr(vAddr, Opcode.Teq, Operand.Reg(rs), Operand.Reg(rt), Operand.Imm(rd.id shl 5 or shift))
                0b110_110 -> Instr(vAddr, Opcode.Tne, Operand.Reg(rs), Operand.Reg(rt), Operand.Imm(rd.id shl 5 or shift))

                0b001_111 -> Instr(vAddr, Opcode.Sync, Operand.Imm(shift))

                else -> handleUnknownInstr(instrCount)
            }
            decoded.add(instance)
        } else if (opcode == COP1) { //FPU
            // Only for FPU R instruction
            val rt = Reg.forId(instr ushr 16 and 0x1F)
            // Only for FPU instruction
            val fmt = instr ushr 21 and 0x1F
            val ft = FpuReg.forId(instr ushr 16 and 0x1F)
            val fs = FpuReg.forId(instr ushr 11 and 0x1F)
            val fd = FpuReg.forId(instr ushr 6 and 0x1F)
            val funct = instr and 0x3F
            // Only for branch instruction
            val branchTarget = (instr and 0xFFFF).toShort().toInt()

            val instance = when (fmt) {
                0b00100 -> Instr(vAddr, Opcode.Mtc1, Operand.Reg(rt), Operand.FpuReg(fs))
                0b00000 -> Instr(vAddr, Opcode.Mfc1, Operand.Reg(rt), Operand.FpuReg(fs))
                0b00110 -> Instr(vAddr, Opcode.Ctc1, Operand.Reg(rt), Operand.FpuReg(fs))
                0b00010 -> Instr(vAddr, Opcode.Cfc1, Operand.Reg(rt), Operand.FpuReg(fs))
                0b01000 -> {
                    when (rt.id) {
                        0b00 -> Instr(vAddr, Opcode.FpuBc1f, Operand.Imm(branchTarget))
                        0b01 -> Instr(vAddr, Opcode.FpuBc1t, Operand.Imm(branchTarget))
                        0b11 -> Instr(vAddr, Opcode.FpuBc1tl, Operand.Imm(branchTarget))
                        0b10 -> Instr(vAddr, Opcode.FpuBc1fl, Operand.Imm(branchTarget))
                        else -> handleUnknownInstr(instrCount)
                    }
                }
                FMT_S -> {
                    when (funct) {
                        0b000_000 -> Instr(vAddr, Opcode.FpuAdd, Operand.FpuReg(fd), Operand.FpuReg(fs), Operand.FpuReg(ft))
                        0b000_001 -> Instr(vAddr, Opcode.FpuSub, Operand.FpuReg(fd), Operand.FpuReg(fs), Operand.FpuReg(ft))
                        0b000_010 -> Instr(vAddr, Opcode.FpuMul, Operand.FpuReg(fd), Operand.FpuReg(fs), Operand.FpuReg(ft))
                        0b000_011 -> Instr(vAddr, Opcode.FpuDiv, Operand.FpuReg(fd), Operand.FpuReg(fs), Operand.FpuReg(ft))
                        0b000_101 -> Instr(vAddr, Opcode.FpuAbs, Operand.FpuReg(fd), Operand.FpuReg(fs))
                        0b000_111 -> Instr(vAddr, Opcode.FpuNeg, Operand.FpuReg(fd), Operand.FpuReg(fs))
                        0b000_100 -> Instr(vAddr, Opcode.FpuSqrt, Operand.FpuReg(fd), Operand.FpuReg(fs))
                        0b001_100 -> Instr(vAddr, Opcode.FpuRoundW, Operand.FpuReg(fd), Operand.FpuReg(fs))
                        0b001_101 -> Instr(vAddr, Opcode.FpuTruncW, Operand.FpuReg(fd), Operand.FpuReg(fs))
                        0b001_110 -> Instr(vAddr, Opcode.FpuCeilW, Operand.FpuReg(fd), Operand.FpuReg(fs))
                        0b001_111 -> Instr(vAddr, Opcode.FpuFloorW, Operand.FpuReg(fd), Operand.FpuReg(fs))

                        0b100_100 -> Instr(vAddr, Opcode.FpuCvtWS, Operand.FpuReg(fd), Operand.FpuReg(fs))
                        0b11_0010 -> Instr(vAddr, Opcode.FpuCEq, Operand.FpuReg(fs), Operand.FpuReg(ft))
                        0b11_1110 -> Instr(vAddr, Opcode.FpuCLe, Operand.FpuReg(fs), Operand.FpuReg(ft))
                        0b11_1100 -> Instr(vAddr, Opcode.FpuCLt, Operand.FpuReg(fs), Operand.FpuReg(ft))
                        0b000_110 -> Instr(vAddr, Opcode.FpuMov, Operand.FpuReg(fd), Operand.FpuReg(fs))
                        else -> handleUnknownInstr(instrCount)
                    }
                }
                FMT_W -> {
                    when (funct) {
                        0b100_000 -> Instr(vAddr, Opcode.FpuCvtSW, Operand.FpuReg(fd), Operand.FpuReg(fs))
                        else -> handleUnknownInstr(instrCount)
                    }
                }
                FMT_D -> {
                    error("FMT_D is unimplemented because PSP does not support it")
                }
                else -> handleUnknownInstr(instrCount)
            }
            decoded.add(instance)

        } else { // I or J instruction
            // only applies to I instruction
            val rs = Reg.forId(instr ushr 21 and 0x1F)
            val rt = Reg.forId(instr ushr 16 and 0x1F)
            val imm = (instr and 0xFFFF).toShort().toInt()
            // only applies to J instruction
            val pseudoAddress = instr and 0x3FFFFFF shl 2
            val instance = when (opcode) {
                0b100_000 -> Instr(vAddr, Opcode.Lb, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b100_100 -> Instr(vAddr, Opcode.Lbu, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b101_000 -> Instr(vAddr, Opcode.Sb, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))

                0b100_001 -> Instr(vAddr, Opcode.Lh, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b100_101 -> Instr(vAddr, Opcode.Lhu, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b101_001 -> Instr(vAddr, Opcode.Sh, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))

                0b100_011 -> Instr(vAddr, Opcode.Lw, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b101_011 -> Instr(vAddr, Opcode.Sw, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))

                0b100_010 -> Instr(vAddr, Opcode.Lwl, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b100_110 -> Instr(vAddr, Opcode.Lwr, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b101_010 -> Instr(vAddr, Opcode.Swl, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b101_110 -> Instr(vAddr, Opcode.Swr, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))

                0b110_000 -> Instr(vAddr, Opcode.Ll, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b111_000 -> Instr(vAddr, Opcode.Sc, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))

                0b001_000 -> Instr(vAddr, Opcode.Addi, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b001_001 -> Instr(vAddr, Opcode.Addiu, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b001_010 -> Instr(vAddr, Opcode.Slti, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b001_011 -> Instr(vAddr, Opcode.Sltiu, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b001_100 -> Instr(vAddr, Opcode.Andi, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b001_101 -> Instr(vAddr, Opcode.Ori, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b001_110 -> Instr(vAddr, Opcode.Xori, Operand.Reg(rt), Operand.Reg(rs), Operand.Imm(imm))
                0b001_111 -> Instr(vAddr, Opcode.Lui, Operand.Reg(rt), Operand.Imm(imm))

                0b000_010 -> Instr(vAddr, Opcode.J, Operand.Imm(pseudoAddress))
                0b000_011 -> Instr(vAddr, Opcode.Jal, Operand.Imm(pseudoAddress))

                0b000_100 -> Instr(vAddr, Opcode.Beq, Operand.Reg(rs), Operand.Reg(rt), Operand.Imm(imm))
                0b000_101 -> Instr(vAddr, Opcode.Bne, Operand.Reg(rs), Operand.Reg(rt), Operand.Imm(imm))
                0b000_110 -> Instr(vAddr, Opcode.Blez, Operand.Reg(rs), Operand.Imm(imm))
                0b000_111 -> Instr(vAddr, Opcode.Bgtz, Operand.Reg(rs), Operand.Imm(imm))
                0b010_100 -> Instr(vAddr, Opcode.Beql, Operand.Reg(rs), Operand.Reg(rt), Operand.Imm(imm))
                0b010_101 -> Instr(vAddr, Opcode.Bnel, Operand.Reg(rs), Operand.Reg(rt), Operand.Imm(imm))
                0b010_110 -> Instr(vAddr, Opcode.Blezl, Operand.Reg(rs), Operand.Imm(imm))
                0b010_111 -> Instr(vAddr, Opcode.Bgtzl, Operand.Reg(rs), Operand.Imm(imm))

                0b000_001 -> {
                    when (rt.id) {
                        0b00000 -> Instr(vAddr, Opcode.Bltz, Operand.Reg(rs), Operand.Imm(imm))
                        0b00001 -> Instr(vAddr, Opcode.Bgez, Operand.Reg(rs), Operand.Imm(imm))
                        0b10000 -> Instr(vAddr, Opcode.Bltzal, Operand.Reg(rs), Operand.Imm(imm))
                        0b10001 -> Instr(vAddr, Opcode.Bgezal, Operand.Reg(rs), Operand.Imm(imm))
                        0b00010 -> Instr(vAddr, Opcode.Bltzl, Operand.Reg(rs), Operand.Imm(imm))
                        0b00011 -> Instr(vAddr, Opcode.Bgezl, Operand.Reg(rs), Operand.Imm(imm))
                        0b10010 -> Instr(vAddr, Opcode.Bltzall, Operand.Reg(rs), Operand.Imm(imm))
                        0b10011 -> Instr(vAddr, Opcode.Bgezall, Operand.Reg(rs), Operand.Imm(imm))

                        0b01000 -> Instr(vAddr, Opcode.Tgei, Operand.Reg(rs), Operand.Imm(imm))
                        0b01001 -> Instr(vAddr, Opcode.Tgeiu, Operand.Reg(rs), Operand.Imm(imm))
                        0b01010 -> Instr(vAddr, Opcode.Tlti, Operand.Reg(rs), Operand.Imm(imm))
                        0b01011 -> Instr(vAddr, Opcode.Tltiu, Operand.Reg(rs), Operand.Imm(imm))
                        0b01100 -> Instr(vAddr, Opcode.Teqi, Operand.Reg(rs), Operand.Imm(imm))
                        0b01110 -> Instr(vAddr, Opcode.Tnei, Operand.Reg(rs), Operand.Imm(imm))
                        else -> error("unrecognized type of REGIMM instruction: ${instr.toWHex()} at ${vAddr.toWHex()} RS: $rs RT: $rt")
                    }
                }

                0b110_001 -> Instr(vAddr, Opcode.Lwc1, Operand.FpuReg(FpuReg.forId(rt.id)), Operand.Reg(rs), Operand.Imm(imm))
                0b111_001 -> Instr(vAddr, Opcode.Swc1, Operand.FpuReg(FpuReg.forId(rt.id)), Operand.Reg(rs), Operand.Imm(imm))

                else -> handleUnknownInstr(instrCount)
            }
            decoded.add(instance)
        }
    }
    return Disassembly(FunctionDef(funcName, funcOffset, funcLen), decoded)
}

class Disassembly(val def: FunctionDef, val instr: List<Instr>)

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

    fun op1AsReg() = op1!!.asReg()
    fun op2AsReg() = op2!!.asReg()
    fun op3AsReg() = op3!!.asReg()
    fun op1AsFpuReg() = op1!!.asFpuReg()
    fun op2AsFpuReg() = op2!!.asFpuReg()
    fun op3AsFpuReg() = op3!!.asFpuReg()
    fun op1AsImm() = op1!!.asImm()
    fun op2AsImm() = op2!!.asImm()
    fun op3AsImm() = op3!!.asImm()

    fun matches(opcode: Opcode? = null, op1: OperandMatcher = isAny(), op2: OperandMatcher = isAny(), op3: OperandMatcher = isAny()): Boolean {
        if (opcode != null && this.opcode != opcode) return false
        if (arrayOf(op1.match(this.op1), op2.match(this.op2), op3.match(this.op3)).all { it == true }) return true
        return false
    }

    fun matchesExact(opcode: Opcode, op1: OperandMatcher = isNull(), op2: OperandMatcher = isNull(), op3: OperandMatcher = isNull()): Boolean {
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

            Opcode.Mult -> arrayOf(Reg.HI, Reg.LO)
            Opcode.Multu -> arrayOf(Reg.HI, Reg.LO)
            Opcode.Div -> arrayOf(Reg.HI, Reg.LO)
            Opcode.Divu -> arrayOf(Reg.HI, Reg.LO)
            Opcode.Mfhi -> arrayOf(op1AsReg())
            Opcode.Mthi -> arrayOf(Reg.HI)
            Opcode.Mflo -> arrayOf(op1AsReg())
            Opcode.Mtlo -> arrayOf(Reg.LO)

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
            Opcode.Mfhi -> arrayOf(Reg.HI)
            Opcode.Mthi -> arrayOf(op1AsReg())
            Opcode.Mflo -> arrayOf(Reg.LO)
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
        if (isMemoryRead() || isMemoryWrite()) return "${addr.toWHex()}: $opcode $op1, $op3($op2)"
        if (op1 == null) return "${addr.toWHex()}: $opcode"
        if (op2 == null) return "${addr.toWHex()}: $opcode $op1"
        if (op3 == null) return "${addr.toWHex()}: $opcode $op1, $op2"
        return "${addr.toWHex()}: $opcode $op1, $op2, $op3"
    }
}

fun isNull() = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return true
        return false
    }
}

fun isAny() = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        return true
    }
}

fun anyReg(vararg regs: Reg) = object : OperandMatcher {
    override fun match(op: Operand?): Boolean {
        if (op == null) return false
        if (op !is Operand.Reg) return false
        return op.reg in regs
    }
}

fun isReg() = object : OperandMatcher {
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

fun isImm() = object : OperandMatcher {
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

interface OperandMatcher {
    fun match(op: Operand?): Boolean
}

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

    fun asReg(): mist.asm.Reg {
        return (this as Reg).reg
    }

    fun asFpuReg(): mist.asm.FpuReg {
        return (this as FpuReg).reg
    }

    fun asImm(): Int {
        return (this as Imm).value
    }
}

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

enum class Reg(val id: Int) {
    zero(0),
    at(1),
    v0(2), v1(3),
    a0(4), a1(5), a2(6), a3(7),
    t0(8), t1(9), t2(10), t3(11), t4(12), t5(13), t6(14), t7(15), t8(24), t9(25),
    s0(16), s1(17), s2(18), s3(19), s4(20), s5(21), s6(22), s7(23),
    k0(26), k1(27),
    gp(28), sp(29), fp(30), ra(31),
    // special purpose - directly inaccessible registers
    pc(-1),
    LO(-1), HI(-1);

    companion object {
        fun forId(id: Int): Reg {
            if (id == -1) error("can't return directly inaccessible register")
            values().forEach {
                if (id == it.id) return it
            }
            error("no such register id: $id")
        }
    }
}

enum class FpuReg(val id: Int) {
    f0(0), f1(1), f2(2), f3(3), f4(4),
    f5(5), f6(6), f7(7), f8(8), f9(9),
    f10(10), f11(11), f12(12), f13(13), f14(14),
    f15(15), f16(16), f17(17), f18(18), f19(19),
    f20(20), f21(21), f22(22), f23(23), f24(24),
    f25(25), f26(26), f27(27), f28(28), f29(29),
    f30(30), f31(31),
    // special purpose - directly inaccessible registers
    cc(-1);

    companion object {
        fun forId(id: Int): FpuReg {
            if (id == -1) error("can't return directly inaccessible register")
            values().forEach {
                if (id == it.id) return it
            }
            error("no such fpu register id: $id")
        }
    }
}
