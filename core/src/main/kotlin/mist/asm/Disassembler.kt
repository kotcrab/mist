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

import kio.util.toWHex
import mist.io.BinLoader

/** @author Kotcrab */

class Disassembler(loader: BinLoader, private val funcDef: FunctionDef) {
    constructor(loader: BinLoader, funcName: String, funcOffset: Int, funcLen: Int)
            : this(loader, FunctionDef(funcName, funcOffset, funcLen))

    private companion object {
        const val COP1 = 0b010_001
        const val FMT_S = 16
        const val FMT_D = 17
        const val FMT_W = 20
    }

    val disassembly: Disassembly

    init {
        if (funcDef.offset % 4 != 0) error("offset must be multiply of 4")
        if (funcDef.len % 4 != 0) error("length must be multiply of 4")
        val decoded = mutableListOf<Instr>()

        repeat(funcDef.len / 4) { instrCount ->
            val vAddr = funcDef.offset + instrCount * 4
            val instr = loader.readInt(vAddr)
            if (instr == 0) {
                decoded.add(Instr(vAddr, Opcode.Nop))
                return@repeat
            }
            val opcode = instr ushr 26
            when (opcode) {
                0 -> decoded.add(disassembleRInstruction(vAddr, instr, instrCount))
                COP1 -> decoded.add(disassembleFpuInstruction(vAddr, instr, instrCount))
                else -> decoded.add(disassembleIAndJInstructions(vAddr, instr, instrCount, opcode))
            }
        }
        disassembly = Disassembly(funcDef, decoded)
    }

    private fun disassembleRInstruction(vAddr: Int, instr: Int, instrCount: Int): Instr {
        val rs = Reg.forId(instr ushr 21 and 0x1F)
        val rt = Reg.forId(instr ushr 16 and 0x1F)
        val rd = Reg.forId(instr ushr 11 and 0x1F)
        val shift = instr ushr 6 and 0x1F
        val funct = instr and 0x3F
        return when (funct) {
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
    }

    private fun disassembleFpuInstruction(vAddr: Int, instr: Int, instrCount: Int): Instr {
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
        return when (fmt) {
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
                error("FMT_D is not implemented")
            }
            else -> handleUnknownInstr(instrCount)
        }
    }

    private fun disassembleIAndJInstructions(vAddr: Int, instr: Int, instrCount: Int, opcode: Int): Instr {
        // only applies to I instruction
        val rs = Reg.forId(instr ushr 21 and 0x1F)
        val rt = Reg.forId(instr ushr 16 and 0x1F)
        val imm = (instr and 0xFFFF).toShort().toInt()
        // only applies to J instruction
        val pseudoAddress = instr and 0x3FFFFFF shl 2

        return when (opcode) {
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
                    else -> error("unrecognized type of REGIMM instruction: ${instr.toWHex()} at ${vAddr.toWHex()} rs: $rs rt: $rt")
                }
            }

            0b110_001 -> Instr(vAddr, Opcode.Lwc1, Operand.FpuReg(FpuReg.forId(rt.id)), Operand.Reg(rs), Operand.Imm(imm))
            0b111_001 -> Instr(vAddr, Opcode.Swc1, Operand.FpuReg(FpuReg.forId(rt.id)), Operand.Reg(rs), Operand.Imm(imm))

            else -> handleUnknownInstr(instrCount)
        }
    }

    private fun handleUnknownInstr(instrCount: Int): Nothing {
        error("unknown instruction at offset ${(instrCount * 4).toWHex()}, address ${(funcDef.offset + instrCount * 4).toWHex()}")
    }
}

class Disassembly(val def: FunctionDef, val instr: List<Instr>)

class FunctionDef(val name: String, val offset: Int, val len: Int)
