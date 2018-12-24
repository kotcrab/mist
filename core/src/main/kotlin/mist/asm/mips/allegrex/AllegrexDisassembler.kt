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

package mist.asm.mips.allegrex

import mist.asm.DisassemblerException
import mist.asm.ImmOperand
import mist.asm.RegOperand
import mist.asm.mips.*
import mist.asm.mips.MipsOpcode.*

/** @author Kotcrab */

//TODO VFPU instructions and other missing instructions from MIPS32

class AllegrexDisassembler : MipsDisassembler(AllegrexProcessor) {
    override fun disasmSpecialInstr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        val rs = RegOperand(GprReg.forId(instr ushr 21 and 0x1F))
        val rt = RegOperand(GprReg.forId(instr ushr 16 and 0x1F))
        val rd = RegOperand(GprReg.forId(instr ushr 11 and 0x1F))
        val shift = instr ushr 6 and 0x1F
        val funct = instr and 0x3F
        return when {
            funct == 0b100_000 && shift == 0 -> MipsInstr(vAddr, Add, rd, rs, rt)
            funct == 0b100_001 && shift == 0 -> MipsInstr(vAddr, Addu, rd, rs, rt)
            funct == 0b100_100 && shift == 0 -> MipsInstr(vAddr, And, rd, rs, rt)
            funct == 0b001_101 -> MipsInstr(vAddr, Break, ImmOperand(instr ushr 6 and 0xFFFF))
            funct == 0b011_010 && rd.reg.id == 0 && shift == 0 -> MipsInstr(vAddr, Div, rs, rt)
            funct == 0b011_011 && rd.reg.id == 0 && shift == 0 -> MipsInstr(vAddr, Divu, rs, rt)
            funct == 0b001_001 && rt.reg.id == 0 && shift == 0 -> MipsInstr(vAddr, Jalr, rd, rs)
            funct == 0b001_000 && rt.reg.id == 0 && rd.reg.id == 0 && shift == 0 -> MipsInstr(vAddr, Jr, rs)
            funct == 0b010_000 && rs.reg.id == 0 && rt.reg.id == 0 && shift == 0 -> MipsInstr(vAddr, Mfhi, rd)
            funct == 0b010_010 && rs.reg.id == 0 && rt.reg.id == 0 && shift == 0 -> MipsInstr(vAddr, Mflo, rd)
//            funct == 0b001_011 && shift == 0 -> MipsInstr(vAddr, Movn, rd, rs, rt)
//            funct == 0b001_010 && shift == 0 -> MipsInstr(vAddr, Movz, rd, rs, rt)
            funct == 0b010_001 && rt.reg.id == 0 && rd.reg.id == 0 && shift == 0 -> MipsInstr(vAddr, Mthi, rs)
            funct == 0b010_011 && rt.reg.id == 0 && rd.reg.id == 0 && shift == 0 -> MipsInstr(vAddr, Mtlo, rs)
            funct == 0b011_000 && rd.reg.id == 0 && shift == 0 -> MipsInstr(vAddr, Mult, rs, rt)
            funct == 0b011_001 && rd.reg.id == 0 && shift == 0 -> MipsInstr(vAddr, Multu, rs, rt)
            funct == 0b100_111 && shift == 0 -> MipsInstr(vAddr, Nor, rd, rs, rt)
            funct == 0b100_101 && shift == 0 -> MipsInstr(vAddr, Or, rd, rs, rt)
            funct == 0b000_000 && rs.reg.id == 0 -> MipsInstr(vAddr, Sll, rd, rt, ImmOperand(shift))
            funct == 0b000_100 && shift == 0 -> MipsInstr(vAddr, Sllv, rd, rt, rs)
            funct == 0b101_010 && shift == 0 -> MipsInstr(vAddr, Slt, rd, rs, rt)
            funct == 0b101_011 && shift == 0 -> MipsInstr(vAddr, Sltu, rd, rs, rt)
            funct == 0b000_011 && rs.reg.id == 0 -> MipsInstr(vAddr, Sra, rd, rt, ImmOperand(shift))
            funct == 0b000_111 && shift == 0 -> MipsInstr(vAddr, Srav, rd, rt, rs)
            funct == 0b000_010 && rs.reg.id == 0 -> MipsInstr(vAddr, Srl, rd, rt, ImmOperand(shift))
            funct == 0b000_110 && shift == 0 -> MipsInstr(vAddr, Srlv, rd, rt, rs)
            funct == 0b100_010 && shift == 0 -> MipsInstr(vAddr, Sub, rd, rs, rt)
            funct == 0b100_011 && shift == 0 -> MipsInstr(vAddr, Subu, rd, rs, rt)
            funct == 0b001_111 && rs.reg.id == 0 && rt.reg.id == 0 && rd.reg.id == 0 -> {
                MipsInstr(vAddr, Sync, ImmOperand(shift))
            }
            funct == 0b001_100 -> MipsInstr(vAddr, Syscall, ImmOperand(instr ushr 6 and 0xFFFF))
            funct == 0b110_100 -> MipsInstr(vAddr, Teq, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b110_000 -> MipsInstr(vAddr, Tge, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b110_001 -> MipsInstr(vAddr, Tgeu, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b110_010 -> MipsInstr(vAddr, Tlt, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b110_011 -> MipsInstr(vAddr, Tltu, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b110_110 -> MipsInstr(vAddr, Tne, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b100_110 && shift == 0 -> MipsInstr(vAddr, Xor, rd, rs, rt)
//            funct == 0b000_001 && instr ushr 16 and 0b11 == 0 && shift == 0 -> {
//                MipsInstr(vAddr, FpuMovf, rd, rs, RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111)))
//            }
//            funct == 0b000_001 && instr ushr 16 and 0b11 == 1 && shift == 0 -> {
//                MipsInstr(vAddr, FpuMovt, rd, rs, RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111)))
//            }
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    override fun disasmRegimmInstr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        val rs = RegOperand(GprReg.forId(instr ushr 21 and 0x1F))
        val rt = instr ushr 16 and 0x1F
        val imm = ImmOperand((instr and 0xFFFF).toShort().toInt())
        return when (rt) {
            0b00001 -> MipsInstr(vAddr, Bgez, rs, imm)
            0b10001 -> MipsInstr(vAddr, Bgezal, rs, imm)
            0b10011 -> MipsInstr(vAddr, Bgezall, rs, imm)
            0b00011 -> MipsInstr(vAddr, Bgezl, rs, imm)
            0b00000 -> MipsInstr(vAddr, Bltz, rs, imm)
            0b10000 -> MipsInstr(vAddr, Bltzal, rs, imm)
            0b10010 -> MipsInstr(vAddr, Bltzall, rs, imm)
            0b00010 -> MipsInstr(vAddr, Bltzl, rs, imm)
            0b01100 -> MipsInstr(vAddr, Teqi, rs, imm)
            0b01000 -> MipsInstr(vAddr, Tgei, rs, imm)
            0b01001 -> MipsInstr(vAddr, Tgeiu, rs, imm)
            0b01010 -> MipsInstr(vAddr, Tlti, rs, imm)
            0b01011 -> MipsInstr(vAddr, Tltiu, rs, imm)
            0b01110 -> MipsInstr(vAddr, Tnei, rs, imm)
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    override fun disasmCop1Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        // only for FPU R instruction
        val rt = RegOperand(GprReg.forId(instr ushr 16 and 0x1F))
        // only for FPU instruction
        val fmt = instr ushr 21 and 0x1F
        val ft = RegOperand(FpuReg.forId(instr ushr 16 and 0x1F))
        val fs = RegOperand(FpuReg.forId(instr ushr 11 and 0x1F))
        val fd = RegOperand(FpuReg.forId(instr ushr 6 and 0x1F))
        val funct = instr and 0x3F
        return when {
            fmt == 0b00010 && fd.reg.id == 0 && funct == 0 -> MipsInstr(vAddr, FpuCfc1, rt, fs)
            fmt == 0b00110 && fd.reg.id == 0 && funct == 0 -> MipsInstr(vAddr, FpuCtc1, rt, fs)
            fmt == 0b00000 && fd.reg.id == 0 && funct == 0 -> MipsInstr(vAddr, FpuMfc1, rt, fs)
            fmt == 0b00100 && fd.reg.id == 0 && funct == 0 -> MipsInstr(vAddr, FpuMtc1, rt, fs)
            fmt == 0b01000 -> {
                val branchTarget = ImmOperand((instr and 0xFFFF).toShort().toInt())
                val ndtf = instr ushr 16 and 0b11
                val cc = RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111))
                if (cc.reg !is FpuReg.Cc0) {
                    throw DisassemblerException("Allegrex can't use non 0 condition code (cc)")
                }
                when (ndtf) {
                    0b00 -> MipsInstr(vAddr, FpuBc1f, branchTarget)
                    0b10 -> MipsInstr(vAddr, FpuBc1fl, branchTarget)
                    0b01 -> MipsInstr(vAddr, FpuBc1t, branchTarget)
                    0b11 -> MipsInstr(vAddr, FpuBc1tl, branchTarget)
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            fmt == MipsDefines.FMT_S -> {
                when {
                    funct == 0b000_101 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuAbsS, fd, fs)
                    funct == 0b000_000 -> MipsInstr(vAddr, FpuAddS, fd, fs, ft)
                    funct and 0b110_000 == 0b110_000 -> {
                        val cc = RegOperand(FpuReg.ccForId(instr ushr 8 and 0b111))
                        val cond = funct and 0b1111
                        if (cc.reg !is FpuReg.Cc0) {
                            throw DisassemblerException("Allegrex can't use non 0 condition code (cc)")
                        }
                        when (cond) {
                            0b0000 -> MipsInstr(vAddr, FpuCFS, fs, ft)
                            0b0001 -> MipsInstr(vAddr, FpuCUnS, fs, ft)
                            0b0010 -> MipsInstr(vAddr, FpuCEqS, fs, ft)
                            0b0011 -> MipsInstr(vAddr, FpuCUeqS, fs, ft)
                            0b0100 -> MipsInstr(vAddr, FpuCOltS, fs, ft)
                            0b0101 -> MipsInstr(vAddr, FpuCUltS, fs, ft)
                            0b0110 -> MipsInstr(vAddr, FpuCOleS, fs, ft)
                            0b0111 -> MipsInstr(vAddr, FpuCUleS, fs, ft)
                            0b1000 -> MipsInstr(vAddr, FpuCSfS, fs, ft)
                            0b1001 -> MipsInstr(vAddr, FpuCNgleS, fs, ft)
                            0b1010 -> MipsInstr(vAddr, FpuCSeqS, fs, ft)
                            0b1011 -> MipsInstr(vAddr, FpuCNglS, fs, ft)
                            0b1100 -> MipsInstr(vAddr, FpuCLtS, fs, ft)
                            0b1101 -> MipsInstr(vAddr, FpuCNgeS, fs, ft)
                            0b1110 -> MipsInstr(vAddr, FpuCLeS, fs, ft)
                            0b1111 -> MipsInstr(vAddr, FpuCNgtS, fs, ft)
                            else -> handleUnknownInstr(vAddr, instrCount)
                        }
                    }
                    funct == 0b001_110 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuCeilWS, fd, fs)
                    funct == 0b100_100 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuCvtWS, fd, fs)
                    funct == 0b000_011 -> MipsInstr(vAddr, FpuDivS, fd, fs, ft)
                    funct == 0b001_111 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuFloorWS, fd, fs)
                    funct == 0b000_110 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuMovS, fd, fs)
//                    funct == 0b010_001 && ft.reg.id and 0b11 == 0 -> {
//                        MipsInstr(vAddr, FpuMovfS, fd, fs, RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111)))
//                    }
//                    funct == 0b010_011 -> MipsInstr(vAddr, FpuMovnS, fd, fs, rt)
//                    funct == 0b010_001 && ft.reg.id and 0b11 == 1 -> {
//                        MipsInstr(vAddr, FpuMovtS, fd, fs, RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111)))
//                    }
//                    funct == 0b010_010 -> MipsInstr(vAddr, FpuMovzS, fd, fs, rt)
                    funct == 0b000_010 -> MipsInstr(vAddr, FpuMulS, fd, fs, ft)
                    funct == 0b000_111 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuNegS, fd, fs)
//                    funct == 0b010_101 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuRecipS, fd, fs)
                    funct == 0b001_100 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuRoundWS, fd, fs)
//                    funct == 0b010_110 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuRsqrtS, fd, fs)
                    funct == 0b000_100 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuSqrtS, fd, fs)
                    funct == 0b000_001 -> MipsInstr(vAddr, FpuSubS, fd, fs, ft)
                    funct == 0b001_101 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuTruncWS, fd, fs)
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            fmt == MipsDefines.FMT_D -> {
                throw DisassemblerException("FMT_L is not supported on Allegrex")
            }
            fmt == MipsDefines.FMT_W -> {
                when {
                    funct == 0b100_000 && ft.reg.id == 0 -> MipsInstr(vAddr, FpuCvtSW, fd, fs)
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            fmt == MipsDefines.FMT_L -> {
                throw DisassemblerException("FMT_L is not supported on Allegrex")
            }
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    // -- todo continue here

    override fun disasmOpcodeInstr(vAddr: Int, instr: Int, instrCount: Int, opcode: Int): MipsInstr {
        val rs = RegOperand(GprReg.forId(instr ushr 21 and 0x1F))
        val rt = RegOperand(GprReg.forId(instr ushr 16 and 0x1F))
        val imm = ImmOperand((instr and 0xFFFF).toShort().toInt())
        return when {
            opcode == 0b001_000 -> MipsInstr(vAddr, Addi, rt, rs, imm)
            opcode == 0b001_001 -> MipsInstr(vAddr, Addiu, rt, rs, imm)
            opcode == 0b001_100 -> MipsInstr(vAddr, Andi, rt, rs, imm)
            opcode == 0b000_100 -> MipsInstr(vAddr, Beq, rs, rt, imm)
            opcode == 0b010_100 -> MipsInstr(vAddr, Beql, rs, rt, imm)
            opcode == 0b000_111 -> MipsInstr(vAddr, Bgtz, rs, imm)
            opcode == 0b010_111 -> MipsInstr(vAddr, Bgtzl, rs, imm)
            opcode == 0b000_110 -> MipsInstr(vAddr, Blez, rs, imm)
            opcode == 0b010_110 -> MipsInstr(vAddr, Blezl, rs, imm)
            opcode == 0b000_101 -> MipsInstr(vAddr, Bne, rs, rt, imm)
            opcode == 0b010_101 -> MipsInstr(vAddr, Bnel, rs, rt, imm)
            opcode == 0b000_010 -> MipsInstr(vAddr, J, ImmOperand(instr and 0x3FFFFFF shl 2))
            opcode == 0b000_011 -> MipsInstr(vAddr, Jal, ImmOperand(instr and 0x3FFFFFF shl 2))
            opcode == 0b100_000 -> MipsInstr(vAddr, Lb, rt, rs, imm)
            opcode == 0b100_100 -> MipsInstr(vAddr, Lbu, rt, rs, imm)
//            opcode == 0b110_101 -> MipsInstr(vAddr, Ldc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
            opcode == 0b100_001 -> MipsInstr(vAddr, Lh, rt, rs, imm)
            opcode == 0b100_101 -> MipsInstr(vAddr, Lhu, rt, rs, imm)
            opcode == 0b110_000 -> MipsInstr(vAddr, Ll, rt, rs, imm)
            opcode == 0b001_111 -> MipsInstr(vAddr, Lui, rt, imm)
            opcode == 0b100_011 -> MipsInstr(vAddr, Lw, rt, rs, imm)
            opcode == 0b110_001 -> MipsInstr(vAddr, Lwc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
            opcode == 0b100_010 -> MipsInstr(vAddr, Lwl, rt, rs, imm)
            opcode == 0b100_110 -> MipsInstr(vAddr, Lwr, rt, rs, imm)
            opcode == 0b001_101 -> MipsInstr(vAddr, Ori, rt, rs, imm)
//            opcode == 0b110_011 -> MipsInstr(vAddr, Pref, ImmOperand(rt.reg.id), rs, imm)
            opcode == 0b101_000 -> MipsInstr(vAddr, Sb, rt, rs, imm)
            opcode == 0b111_000 -> MipsInstr(vAddr, Sc, rt, rs, imm)
//            opcode == 0b111_101 -> MipsInstr(vAddr, Sdc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
            opcode == 0b101_001 -> MipsInstr(vAddr, Sh, rt, rs, imm)
            opcode == 0b001_010 -> MipsInstr(vAddr, Slti, rt, rs, imm)
            opcode == 0b001_011 -> MipsInstr(vAddr, Sltiu, rt, rs, imm)
            opcode == 0b101_011 -> MipsInstr(vAddr, Sw, rt, rs, imm)
            opcode == 0b111_001 -> MipsInstr(vAddr, Swc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
            opcode == 0b101_010 -> MipsInstr(vAddr, Swl, rt, rs, imm)
            opcode == 0b101_110 -> MipsInstr(vAddr, Swr, rt, rs, imm)
            opcode == 0b001_110 -> MipsInstr(vAddr, Xori, rt, rs, imm)
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }
}
