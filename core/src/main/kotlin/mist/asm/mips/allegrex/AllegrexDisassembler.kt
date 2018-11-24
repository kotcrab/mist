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

package mist.asm.mips.allegrex

import kio.util.toWHex
import mist.asm.*
import mist.asm.mips.FpuReg
import mist.asm.mips.GprReg
import mist.asm.mips.MipsInstr
import mist.asm.mips.MipsOpcode.*
import mist.io.BinLoader

/** @author Kotcrab */

//TODO VFPU instructions and other missing instructions from MIPS32
//TODO this should be updated using LegacyMipsDisassembler as a base

class AllegrexDisassembler : Disassembler<MipsInstr> {
    private companion object {
        const val COP1 = 0b010_001
        const val FMT_S = 16
        const val FMT_D = 17
        const val FMT_W = 20
    }

    override fun disassemble(loader: BinLoader, funcDef: FunctionDef): Disassembly<MipsInstr> {
        if (funcDef.offset % 4 != 0) error("offset must be multiply of 4")
        if (funcDef.len % 4 != 0) error("length must be multiply of 4")
        val decoded = mutableListOf<MipsInstr>()

        repeat(funcDef.len / 4) { instrCount ->
            val vAddr = funcDef.offset + instrCount * 4
            val instr = loader.readInt(vAddr)
            if (instr == 0) {
                decoded.add(MipsInstr(vAddr, Nop))
                return@repeat
            }
            val opcode = instr ushr 26
            when (opcode) {
                0 -> decoded.add(disassembleRInstruction(vAddr, instr, instrCount))
                COP1 -> decoded.add(disassembleFpuInstruction(vAddr, instr, instrCount))
                else -> decoded.add(disassembleIAndJInstructions(vAddr, instr, instrCount, opcode))
            }
        }
        return Disassembly(funcDef, decoded)
    }

    private fun disassembleRInstruction(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        val rs = GprReg.forId(instr ushr 21 and 0x1F)
        val rt = GprReg.forId(instr ushr 16 and 0x1F)
        val rd = GprReg.forId(instr ushr 11 and 0x1F)
        val shift = instr ushr 6 and 0x1F
        val funct = instr and 0x3F
        return when (funct) {
            0b100_000 -> MipsInstr(vAddr, Add, RegOperand(rd), RegOperand(rs), RegOperand(rt))
            0b100_001 -> MipsInstr(vAddr, Addu, RegOperand(rd), RegOperand(rs), RegOperand(rt))
            0b100_010 -> MipsInstr(vAddr, Sub, RegOperand(rd), RegOperand(rs), RegOperand(rt))
            0b100_011 -> MipsInstr(vAddr, Subu, RegOperand(rd), RegOperand(rs), RegOperand(rt))

            0b101_010 -> MipsInstr(vAddr, Slt, RegOperand(rd), RegOperand(rs), RegOperand(rt))
            0b101_011 -> MipsInstr(vAddr, Sltu, RegOperand(rd), RegOperand(rs), RegOperand(rt))
            0b100_100 -> MipsInstr(vAddr, And, RegOperand(rd), RegOperand(rs), RegOperand(rt))
            0b100_101 -> MipsInstr(vAddr, Or, RegOperand(rd), RegOperand(rs), RegOperand(rt))
            0b100_110 -> MipsInstr(vAddr, Xor, RegOperand(rd), RegOperand(rs), RegOperand(rt))
            0b100_111 -> MipsInstr(vAddr, Nor, RegOperand(rd), RegOperand(rs), RegOperand(rt))

            0b000_000 -> MipsInstr(vAddr, Sll, RegOperand(rd), RegOperand(rt), ImmOperand(shift))
            0b000_010 -> MipsInstr(vAddr, Srl, RegOperand(rd), RegOperand(rt), ImmOperand(shift))
            0b000_011 -> MipsInstr(vAddr, Sra, RegOperand(rd), RegOperand(rt), ImmOperand(shift))
            0b000_100 -> MipsInstr(vAddr, Sllv, RegOperand(rd), RegOperand(rt), RegOperand(rs))
            0b000_110 -> MipsInstr(vAddr, Srlv, RegOperand(rd), RegOperand(rt), RegOperand(rs))
            0b000_111 -> MipsInstr(vAddr, Srav, RegOperand(rd), RegOperand(rt), RegOperand(rs))

            0b011_000 -> MipsInstr(vAddr, Mult, RegOperand(rs), RegOperand(rt))
            0b011_001 -> MipsInstr(vAddr, Multu, RegOperand(rs), RegOperand(rt))
            0b011_010 -> MipsInstr(vAddr, Div, RegOperand(rs), RegOperand(rt))
            0b011_011 -> MipsInstr(vAddr, Divu, RegOperand(rs), RegOperand(rt))
            0b010_000 -> MipsInstr(vAddr, Mfhi, RegOperand(rd))
            0b010_001 -> MipsInstr(vAddr, Mthi, RegOperand(rs))
            0b010_010 -> MipsInstr(vAddr, Mflo, RegOperand(rd))
            0b010_011 -> MipsInstr(vAddr, Mtlo, RegOperand(rs))

            0b001_000 -> MipsInstr(vAddr, Jr, RegOperand(rs))
            0b001_001 -> MipsInstr(vAddr, Jalr, RegOperand(rd), RegOperand(rs))

            0b001_100 -> MipsInstr(vAddr, Syscall, ImmOperand(instr ushr 6 and 0xFFFF))
            0b001_101 -> MipsInstr(vAddr, Break, ImmOperand(instr ushr 6 and 0xFFFF))

            0b110_000 -> MipsInstr(vAddr, Tge, RegOperand(rs), RegOperand(rt), ImmOperand(rd.id shl 5 or shift))
            0b110_001 -> MipsInstr(vAddr, Tgeu, RegOperand(rs), RegOperand(rt), ImmOperand(rd.id shl 5 or shift))
            0b110_010 -> MipsInstr(vAddr, Tlt, RegOperand(rs), RegOperand(rt), ImmOperand(rd.id shl 5 or shift))
            0b110_011 -> MipsInstr(vAddr, Tltu, RegOperand(rs), RegOperand(rt), ImmOperand(rd.id shl 5 or shift))
            0b110_100 -> MipsInstr(vAddr, Teq, RegOperand(rs), RegOperand(rt), ImmOperand(rd.id shl 5 or shift))
            0b110_110 -> MipsInstr(vAddr, Tne, RegOperand(rs), RegOperand(rt), ImmOperand(rd.id shl 5 or shift))

            0b001_111 -> MipsInstr(vAddr, Sync, ImmOperand(shift))

            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    private fun disassembleFpuInstruction(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        // Only for FPU R instruction
        val rt = GprReg.forId(instr ushr 16 and 0x1F)
        // Only for FPU instruction
        val fmt = instr ushr 21 and 0x1F
        val ft = FpuReg.forId(instr ushr 16 and 0x1F)
        val fs = FpuReg.forId(instr ushr 11 and 0x1F)
        val fd = FpuReg.forId(instr ushr 6 and 0x1F)
        val funct = instr and 0x3F
        // Only for branch instruction
        val branchTarget = (instr and 0xFFFF).toShort().toInt()
        return when (fmt) {
            0b00100 -> MipsInstr(vAddr, FpuMtc1, RegOperand(rt), RegOperand(fs))
            0b00000 -> MipsInstr(vAddr, FpuMfc1, RegOperand(rt), RegOperand(fs))
            0b00110 -> MipsInstr(vAddr, FpuCtc1, RegOperand(rt), RegOperand(fs))
            0b00010 -> MipsInstr(vAddr, FpuCfc1, RegOperand(rt), RegOperand(fs))
            0b01000 -> {
                when (rt.id) {
                    0b00 -> MipsInstr(vAddr, FpuBc1f, ImmOperand(branchTarget))
                    0b01 -> MipsInstr(vAddr, FpuBc1t, ImmOperand(branchTarget))
                    0b11 -> MipsInstr(vAddr, FpuBc1tl, ImmOperand(branchTarget))
                    0b10 -> MipsInstr(vAddr, FpuBc1fl, ImmOperand(branchTarget))
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            FMT_S -> {
                when (funct) {
                    0b000_000 -> MipsInstr(vAddr, FpuAddS, RegOperand(fd), RegOperand(fs), RegOperand(ft))
                    0b000_001 -> MipsInstr(vAddr, FpuSubS, RegOperand(fd), RegOperand(fs), RegOperand(ft))
                    0b000_010 -> MipsInstr(vAddr, FpuMulS, RegOperand(fd), RegOperand(fs), RegOperand(ft))
                    0b000_011 -> MipsInstr(vAddr, FpuDivS, RegOperand(fd), RegOperand(fs), RegOperand(ft))
                    0b000_101 -> MipsInstr(vAddr, FpuAbsS, RegOperand(fd), RegOperand(fs))
                    0b000_111 -> MipsInstr(vAddr, FpuNegS, RegOperand(fd), RegOperand(fs))
                    0b000_100 -> MipsInstr(vAddr, FpuSqrtS, RegOperand(fd), RegOperand(fs))
                    0b001_100 -> MipsInstr(vAddr, FpuRoundWS, RegOperand(fd), RegOperand(fs))
                    0b001_101 -> MipsInstr(vAddr, FpuTruncWS, RegOperand(fd), RegOperand(fs))
                    0b001_110 -> MipsInstr(vAddr, FpuCeilWS, RegOperand(fd), RegOperand(fs))
                    0b001_111 -> MipsInstr(vAddr, FpuFloorWS, RegOperand(fd), RegOperand(fs))

                    0b100_100 -> MipsInstr(vAddr, FpuCvtWS, RegOperand(fd), RegOperand(fs))
                    0b11_0010 -> MipsInstr(vAddr, FpuCEqS, RegOperand(fs), RegOperand(ft))
                    0b11_1110 -> MipsInstr(vAddr, FpuCLeS, RegOperand(fs), RegOperand(ft))
                    0b11_1100 -> MipsInstr(vAddr, FpuCLtS, RegOperand(fs), RegOperand(ft))
                    0b000_110 -> MipsInstr(vAddr, FpuMovS, RegOperand(fd), RegOperand(fs))
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            FMT_W -> {
                when (funct) {
                    0b100_000 -> MipsInstr(vAddr, FpuCvtSW, RegOperand(fd), RegOperand(fs))
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            FMT_D -> {
                error("FMT_D is not supported on Allegrex")
            }
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    private fun disassembleIAndJInstructions(vAddr: Int, instr: Int, instrCount: Int, opcode: Int): MipsInstr {
        // only applies to I instruction
        val rs = GprReg.forId(instr ushr 21 and 0x1F)
        val rt = GprReg.forId(instr ushr 16 and 0x1F)
        val imm = (instr and 0xFFFF).toShort().toInt()
        // only applies to J instruction
        val pseudoAddress = instr and 0x3FFFFFF shl 2

        return when (opcode) {
            0b100_000 -> MipsInstr(vAddr, Lb, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b100_100 -> MipsInstr(vAddr, Lbu, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b101_000 -> MipsInstr(vAddr, Sb, RegOperand(rt), RegOperand(rs), ImmOperand(imm))

            0b100_001 -> MipsInstr(vAddr, Lh, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b100_101 -> MipsInstr(vAddr, Lhu, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b101_001 -> MipsInstr(vAddr, Sh, RegOperand(rt), RegOperand(rs), ImmOperand(imm))

            0b100_011 -> MipsInstr(vAddr, Lw, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b101_011 -> MipsInstr(vAddr, Sw, RegOperand(rt), RegOperand(rs), ImmOperand(imm))

            0b100_010 -> MipsInstr(vAddr, Lwl, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b100_110 -> MipsInstr(vAddr, Lwr, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b101_010 -> MipsInstr(vAddr, Swl, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b101_110 -> MipsInstr(vAddr, Swr, RegOperand(rt), RegOperand(rs), ImmOperand(imm))

            0b110_000 -> MipsInstr(vAddr, Ll, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b111_000 -> MipsInstr(vAddr, Sc, RegOperand(rt), RegOperand(rs), ImmOperand(imm))

            0b001_000 -> MipsInstr(vAddr, Addi, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b001_001 -> MipsInstr(vAddr, Addiu, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b001_010 -> MipsInstr(vAddr, Slti, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b001_011 -> MipsInstr(vAddr, Sltiu, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b001_100 -> MipsInstr(vAddr, Andi, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b001_101 -> MipsInstr(vAddr, Ori, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b001_110 -> MipsInstr(vAddr, Xori, RegOperand(rt), RegOperand(rs), ImmOperand(imm))
            0b001_111 -> MipsInstr(vAddr, Lui, RegOperand(rt), ImmOperand(imm))

            0b000_010 -> MipsInstr(vAddr, J, ImmOperand(pseudoAddress))
            0b000_011 -> MipsInstr(vAddr, Jal, ImmOperand(pseudoAddress))

            0b000_100 -> MipsInstr(vAddr, Beq, RegOperand(rs), RegOperand(rt), ImmOperand(imm))
            0b000_101 -> MipsInstr(vAddr, Bne, RegOperand(rs), RegOperand(rt), ImmOperand(imm))
            0b000_110 -> MipsInstr(vAddr, Blez, RegOperand(rs), ImmOperand(imm))
            0b000_111 -> MipsInstr(vAddr, Bgtz, RegOperand(rs), ImmOperand(imm))
            0b010_100 -> MipsInstr(vAddr, Beql, RegOperand(rs), RegOperand(rt), ImmOperand(imm))
            0b010_101 -> MipsInstr(vAddr, Bnel, RegOperand(rs), RegOperand(rt), ImmOperand(imm))
            0b010_110 -> MipsInstr(vAddr, Blezl, RegOperand(rs), ImmOperand(imm))
            0b010_111 -> MipsInstr(vAddr, Bgtzl, RegOperand(rs), ImmOperand(imm))

            0b000_001 -> {
                when (rt.id) {
                    0b00000 -> MipsInstr(vAddr, Bltz, RegOperand(rs), ImmOperand(imm))
                    0b00001 -> MipsInstr(vAddr, Bgez, RegOperand(rs), ImmOperand(imm))
                    0b10000 -> MipsInstr(vAddr, Bltzal, RegOperand(rs), ImmOperand(imm))
                    0b10001 -> MipsInstr(vAddr, Bgezal, RegOperand(rs), ImmOperand(imm))
                    0b00010 -> MipsInstr(vAddr, Bltzl, RegOperand(rs), ImmOperand(imm))
                    0b00011 -> MipsInstr(vAddr, Bgezl, RegOperand(rs), ImmOperand(imm))
                    0b10010 -> MipsInstr(vAddr, Bltzall, RegOperand(rs), ImmOperand(imm))
                    0b10011 -> MipsInstr(vAddr, Bgezall, RegOperand(rs), ImmOperand(imm))

                    0b01000 -> MipsInstr(vAddr, Tgei, RegOperand(rs), ImmOperand(imm))
                    0b01001 -> MipsInstr(vAddr, Tgeiu, RegOperand(rs), ImmOperand(imm))
                    0b01010 -> MipsInstr(vAddr, Tlti, RegOperand(rs), ImmOperand(imm))
                    0b01011 -> MipsInstr(vAddr, Tltiu, RegOperand(rs), ImmOperand(imm))
                    0b01100 -> MipsInstr(vAddr, Teqi, RegOperand(rs), ImmOperand(imm))
                    0b01110 -> MipsInstr(vAddr, Tnei, RegOperand(rs), ImmOperand(imm))
                    else -> error("unrecognized type of REGIMM instruction: ${instr.toWHex()} at ${vAddr.toWHex()} rs: $rs rt: $rt")
                }
            }

            0b110_001 -> MipsInstr(vAddr, Lwc1, RegOperand(FpuReg.forId(rt.id)), RegOperand(rs), ImmOperand(imm))
            0b111_001 -> MipsInstr(vAddr, Swc1, RegOperand(FpuReg.forId(rt.id)), RegOperand(rs), ImmOperand(imm))

            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    private fun handleUnknownInstr(vAddr: Int, instrCount: Int): Nothing {
        error("unknown instruction at offset ${(instrCount * 4).toWHex()}, address ${vAddr.toWHex()}")
    }
}
