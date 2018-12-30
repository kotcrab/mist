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
import mist.asm.mips.MipsDisassembler.StrictCheck.*
import mist.asm.mips.MipsOpcode.*
import mist.asm.mips.allegrex.AllegrexOpcode.Max
import mist.asm.mips.allegrex.AllegrexOpcode.Min

/** @author Kotcrab */

//TODO VFPU instructions and other missing instructions from MIPS32

class AllegrexDisassembler(strict: Boolean = true) : MipsDisassembler(AllegrexProcessor, strict) {
    override fun disasmSpecialInstr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        val rs = RegOperand(GprReg.forId(instr ushr 21 and 0x1F))
        val rt = RegOperand(GprReg.forId(instr ushr 16 and 0x1F))
        val rd = RegOperand(GprReg.forId(instr ushr 11 and 0x1F))
        val shift = instr ushr 6 and 0x1F
        val funct = instr and 0x3F
        val ifStrict = StrictChecker()
        ifStrict.register(ZeroRs) { rs.reg.id == 0 }
        ifStrict.register(ZeroRt) { rt.reg.id == 0 }
        ifStrict.register(ZeroRd) { rd.reg.id == 0 }
        ifStrict.register(ZeroShift) { shift == 0 }
        return when {
            funct == 0b100_000 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Add, rd, rs, rt)
            funct == 0b100_001 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Addu, rd, rs, rt)
            funct == 0b100_100 && ifStrict(ZeroShift) -> MipsInstr(vAddr, And, rd, rs, rt)
            funct == 0b001_101 -> MipsInstr(vAddr, Break, ImmOperand(instr ushr 6 and 0xFFFF))
            funct == 0b011_010 && ifStrict(ZeroRd, ZeroShift) -> MipsInstr(vAddr, Div, rs, rt)
            funct == 0b011_011 && ifStrict(ZeroRd, ZeroShift) -> MipsInstr(vAddr, Divu, rs, rt)
            funct == 0b001_001 && ifStrict(ZeroRt, ZeroShift) -> MipsInstr(vAddr, Jalr, rd, rs)
            funct == 0b001_000 && ifStrict(ZeroRt, ZeroRd, ZeroShift) -> MipsInstr(vAddr, Jr, rs)
            funct == 0b010_000 && ifStrict(ZeroRs, ZeroRt, ZeroShift) -> MipsInstr(vAddr, Mfhi, rd)
            funct == 0b010_010 && ifStrict(ZeroRs, ZeroRt, ZeroShift) -> MipsInstr(vAddr, Mflo, rd)
            funct == 0b001_011 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Movn, rd, rs, rt)
            funct == 0b001_010 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Movz, rd, rs, rt)
            funct == 0b010_001 && ifStrict(ZeroRt, ZeroRd, ZeroShift) -> MipsInstr(vAddr, Mthi, rs)
            funct == 0b010_011 && ifStrict(ZeroRt, ZeroRd, ZeroShift) -> MipsInstr(vAddr, Mtlo, rs)
            funct == 0b011_000 && ifStrict(ZeroRd, ZeroShift) -> MipsInstr(vAddr, Mult, rs, rt)
            funct == 0b011_001 && ifStrict(ZeroRd, ZeroShift) -> MipsInstr(vAddr, Multu, rs, rt)
            funct == 0b100_111 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Nor, rd, rs, rt)
            funct == 0b100_101 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Or, rd, rs, rt)
            // note for ROTR / ROTRV: this doesn't match docs on purpose, normally only LSB of rs.reg.id / shift would be
            // checked, remaining bits would be checked using ifStrict
            funct == 0b000_010 && rs.reg.id == 1 && ifStrict { rs.reg.id ushr 1 == 0 } -> {
                MipsInstr(vAddr, Rotr, rd, rt, ImmOperand(shift))
            }
            funct == 0b000_110 && shift == 1 -> {
                MipsInstr(vAddr, Rotrv, rd, rt, rs)
            }
            funct == 0b000_000 && ifStrict(ZeroRs) -> MipsInstr(vAddr, Sll, rd, rt, ImmOperand(shift))
            funct == 0b000_100 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Sllv, rd, rt, rs)
            funct == 0b101_010 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Slt, rd, rs, rt)
            funct == 0b101_011 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Sltu, rd, rs, rt)
            funct == 0b000_011 && ifStrict(ZeroRs) -> MipsInstr(vAddr, Sra, rd, rt, ImmOperand(shift))
            funct == 0b000_111 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Srav, rd, rt, rs)
            // note for SRL / SRLV: this doesn't match docs on purpose, normally only LSB of rs.reg.id / shift would be
            // checked, remaining bits would be checked using ifStrict
            funct == 0b000_010 && rs.reg.id != 1 && ifStrict { rs.reg.id ushr 1 == 0 } -> {
                MipsInstr(vAddr, Srl, rd, rt, ImmOperand(shift))
            }
            funct == 0b000_110 && shift != 1 -> {
                MipsInstr(vAddr, Srlv, rd, rt, rs)
            }
            funct == 0b100_010 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Sub, rd, rs, rt)
            funct == 0b100_011 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Subu, rd, rs, rt)
            funct == 0b001_111 && ifStrict(ZeroRs, ZeroRt, ZeroRd) -> {
                MipsInstr(vAddr, Sync, ImmOperand(shift))
            }
            funct == 0b001_100 -> MipsInstr(vAddr, Syscall, ImmOperand(instr ushr 6 and 0xFFFF))
            funct == 0b110_100 -> MipsInstr(vAddr, Teq, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b110_000 -> MipsInstr(vAddr, Tge, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b110_001 -> MipsInstr(vAddr, Tgeu, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b110_010 -> MipsInstr(vAddr, Tlt, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b110_011 -> MipsInstr(vAddr, Tltu, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b110_110 -> MipsInstr(vAddr, Tne, rs, rt, ImmOperand(rd.reg.id shl 5 or shift))
            funct == 0b100_110 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Xor, rd, rs, rt)
            // Allegrex specific
            funct == 0b010_110 && ifStrict(ZeroShift, ZeroRt) -> MipsInstr(vAddr, Clz, rd, rs)
            funct == 0b010_111 && ifStrict(ZeroShift, ZeroRt) -> MipsInstr(vAddr, Clo, rd, rs)
            funct == 0b101_100 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Max, rd, rs, rt)
            funct == 0b101_101 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Min, rd, rs, rt)
            funct == 0b011_100 && ifStrict(ZeroShift, ZeroRd) -> MipsInstr(vAddr, Madd, rs, rt)
            funct == 0b011_101 && ifStrict(ZeroShift, ZeroRd) -> MipsInstr(vAddr, Maddu, rs, rt)
            funct == 0b101_110 && ifStrict(ZeroShift, ZeroRd) -> MipsInstr(vAddr, Msub, rs, rt)
            funct == 0b101_111 && ifStrict(ZeroShift, ZeroRd) -> MipsInstr(vAddr, Msubu, rs, rt)
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    override fun disasmRegimmInstr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        val rs = RegOperand(GprReg.forId(instr ushr 21 and 0x1F))
        val rt = instr ushr 16 and 0x1F
        val imm = ImmOperand((instr and 0xFFFF).toShort().toInt())
        val branchImm = ImmOperand(vAddr + 0x4 + imm.value * 0x4)
        return when (rt) {
            0b00001 -> MipsInstr(vAddr, Bgez, rs, branchImm)
            0b10001 -> MipsInstr(vAddr, Bgezal, rs, branchImm)
            0b10011 -> MipsInstr(vAddr, Bgezall, rs, branchImm)
            0b00011 -> MipsInstr(vAddr, Bgezl, rs, branchImm)
            0b00000 -> MipsInstr(vAddr, Bltz, rs, branchImm)
            0b10000 -> MipsInstr(vAddr, Bltzal, rs, branchImm)
            0b10010 -> MipsInstr(vAddr, Bltzall, rs, branchImm)
            0b00010 -> MipsInstr(vAddr, Bltzl, rs, branchImm)
            0b01100 -> MipsInstr(vAddr, Teqi, rs, imm)
            0b01000 -> MipsInstr(vAddr, Tgei, rs, imm)
            0b01001 -> MipsInstr(vAddr, Tgeiu, rs, imm)
            0b01010 -> MipsInstr(vAddr, Tlti, rs, imm)
            0b01011 -> MipsInstr(vAddr, Tltiu, rs, imm)
            0b01110 -> MipsInstr(vAddr, Tnei, rs, imm)
            0b11111 -> MipsInstr(vAddr, Synci, rs, imm)
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    override fun disasmCop0Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        val op = instr ushr 21 and 0x1F
        val rt = RegOperand(GprReg.forId(instr ushr 16 and 0x1F))
        val rd = RegOperand(GprReg.forId(instr ushr 11 and 0x1F))
        val co = instr ushr 25 and 0b1
        val tlop = instr and 0x3f
        val ifStrict = StrictChecker()
        ifStrict.register(ZeroRt) { rt.reg.id == 0 }
        ifStrict.register(ZeroRd) { rd.reg.id == 0 }
        return when {
            op == 0b01_011 && instr ushr 5 and 0b1 == 0 && rd.reg.id == 0xC
                    && ifStrict { instr and 0x1F == 0 } && ifStrict { instr ushr 6 and 0x1F == 0 } -> {
                MipsInstr(vAddr, Di, rt)
            }
            op == 0b01_011 && instr ushr 5 and 0b1 == 1 && rd.reg.id == 0xC
                    && ifStrict { instr and 0x1F == 0 } && ifStrict { instr ushr 6 and 0x1F == 0 } -> {
                MipsInstr(vAddr, Ei, rt)
            }
            op == 0b00_000 && ifStrict { instr ushr 3 and 0xFF == 0 } -> {
                MipsInstr(vAddr, Mfc0, rt, rd, ImmOperand(instr and 0b111))
            }
            op == 0b00_100 && ifStrict { instr ushr 3 and 0xFF == 0 } -> {
                MipsInstr(vAddr, Mtc0, rt, rd, ImmOperand(instr and 0b111))
            }
            co == 1 && tlop == 0b001_000 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Tlbp)
            co == 1 && tlop == 0b000_001 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Tlbr)
            co == 1 && tlop == 0b000_010 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Tlbwi)
            co == 1 && tlop == 0b000_110 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Tlbwr)
            co == 1 && tlop == 0b011_111 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Deret)
            co == 1 && tlop == 0b011_000 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Eret)
            co == 1 && tlop == 0b100_000 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Wait)
            op == 0b01_010 && ifStrict { instr and 0xFFF == 0 } -> MipsInstr(vAddr, Rdpgpr, rt, rd)
            op == 0b01_110 && ifStrict { instr and 0xFFF == 0 } -> MipsInstr(vAddr, Wrpgpr, rt, rd)
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
        val ifStrict = StrictChecker()
        ifStrict.register(ZeroFt) { ft.reg.id == 0 }
        ifStrict.register(ZeroFs) { fs.reg.id == 0 }
        ifStrict.register(ZeroFd) { fd.reg.id == 0 }
        ifStrict.register(ZeroFunct) { funct == 0 }
        return when {
            fmt == 0b00010 && ifStrict(ZeroFd, ZeroFunct) -> MipsInstr(vAddr, FpuCfc1, rt, fs)
            fmt == 0b00110 && ifStrict(ZeroFd, ZeroFunct) -> MipsInstr(vAddr, FpuCtc1, rt, fs)
            fmt == 0b00000 && ifStrict(ZeroFd, ZeroFunct) -> MipsInstr(vAddr, FpuMfc1, rt, fs)
            fmt == 0b00100 && ifStrict(ZeroFd, ZeroFunct) -> MipsInstr(vAddr, FpuMtc1, rt, fs)
            fmt == 0b01000 -> {
                val branchImm = (instr and 0xFFFF).toShort().toInt()
                val branchTarget = ImmOperand(vAddr + 0x4 + branchImm * 0x4)
                val ndtf = instr ushr 16 and 0b11
                val cc = RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111))
                // note: strict mode enabled is not checked here on purpose
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
                    funct == 0b000_101 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuAbsS, fd, fs)
                    funct == 0b000_000 -> MipsInstr(vAddr, FpuAddS, fd, fs, ft)
                    funct and 0b110_000 == 0b110_000 && ifStrict { funct ushr 6 and 0b11 == 0 } -> {
                        val cc = RegOperand(FpuReg.ccForId(instr ushr 8 and 0b111))
                        val cond = funct and 0b1111
                        if (strict && cc.reg !is FpuReg.Cc0) {
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
                    funct == 0b001_110 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCeilWS, fd, fs)
                    funct == 0b100_100 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtWS, fd, fs)
                    funct == 0b000_011 -> MipsInstr(vAddr, FpuDivS, fd, fs, ft)
                    funct == 0b001_111 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuFloorWS, fd, fs)
                    funct == 0b000_110 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuMovS, fd, fs)
                    funct == 0b010_010 -> MipsInstr(vAddr, FpuMovzS, fd, fs, rt)
                    funct == 0b000_010 -> MipsInstr(vAddr, FpuMulS, fd, fs, ft)
                    funct == 0b000_111 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuNegS, fd, fs)
                    funct == 0b010_101 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuRecipS, fd, fs)
                    funct == 0b001_100 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuRoundWS, fd, fs)
                    funct == 0b010_110 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuRsqrtS, fd, fs)
                    funct == 0b000_100 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuSqrtS, fd, fs)
                    funct == 0b000_001 -> MipsInstr(vAddr, FpuSubS, fd, fs, ft)
                    funct == 0b001_101 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuTruncWS, fd, fs)
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            fmt == MipsDefines.FMT_D -> {
                throw DisassemblerException("FMT_L is not supported on Allegrex")
            }
            fmt == MipsDefines.FMT_W -> {
                when {
                    funct == 0b100_000 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtSW, fd, fs)
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            fmt == MipsDefines.FMT_L -> {
                throw DisassemblerException("FMT_L is not supported on Allegrex")
            }
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    override fun disasmOpcodeInstr(vAddr: Int, instr: Int, instrCount: Int, opcode: Int): MipsInstr {
        val rs = RegOperand(GprReg.forId(instr ushr 21 and 0x1F))
        val rt = RegOperand(GprReg.forId(instr ushr 16 and 0x1F))
        val imm = ImmOperand((instr and 0xFFFF).toShort().toInt())
        val zeroExtendImm = ImmOperand(instr and 0xFFFF)
        val branchImm = ImmOperand(vAddr + 0x4 + imm.value * 0x4)
        val ifStrict = StrictChecker()
        ifStrict.register(ZeroRs) { rs.reg.id == 0 }
        ifStrict.register(ZeroRt) { rt.reg.id == 0 }
        ifStrict.register(ZeroImm) { imm.value == 0 }
        return when {
            opcode == 0b001_000 -> MipsInstr(vAddr, Addi, rt, rs, imm)
            opcode == 0b001_001 -> MipsInstr(vAddr, Addiu, rt, rs, imm)
            opcode == 0b001_100 -> MipsInstr(vAddr, Andi, rt, rs, zeroExtendImm)
            opcode == 0b000_100 -> MipsInstr(vAddr, Beq, rs, rt, branchImm)
            opcode == 0b010_100 -> MipsInstr(vAddr, Beql, rs, rt, branchImm)
            opcode == 0b000_111 && ifStrict(ZeroRt) -> MipsInstr(vAddr, Bgtz, rs, branchImm)
            opcode == 0b010_111 && ifStrict(ZeroRt) -> MipsInstr(vAddr, Bgtzl, rs, branchImm)
            opcode == 0b000_110 && ifStrict(ZeroRt) -> MipsInstr(vAddr, Blez, rs, branchImm)
            opcode == 0b010_110 && ifStrict(ZeroRt) -> MipsInstr(vAddr, Blezl, rs, branchImm)
            opcode == 0b000_101 -> MipsInstr(vAddr, Bne, rs, rt, branchImm)
            opcode == 0b010_101 -> MipsInstr(vAddr, Bnel, rs, rt, branchImm)
            opcode == 0b000_010 -> MipsInstr(vAddr, J, ImmOperand(instr and 0x3FFFFFF shl 2))
            opcode == 0b000_011 -> MipsInstr(vAddr, Jal, ImmOperand(instr and 0x3FFFFFF shl 2))
            opcode == 0b100_000 -> MipsInstr(vAddr, Lb, rt, rs, imm)
            opcode == 0b100_100 -> MipsInstr(vAddr, Lbu, rt, rs, imm)
            opcode == 0b100_001 -> MipsInstr(vAddr, Lh, rt, rs, imm)
            opcode == 0b100_101 -> MipsInstr(vAddr, Lhu, rt, rs, imm)
            opcode == 0b110_000 -> MipsInstr(vAddr, Ll, rt, rs, imm)
            opcode == 0b001_111 && ifStrict(ZeroRs) -> MipsInstr(vAddr, Lui, rt, zeroExtendImm)
            opcode == 0b100_011 -> MipsInstr(vAddr, Lw, rt, rs, imm)
            opcode == 0b110_001 -> MipsInstr(vAddr, Lwc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
            opcode == 0b100_010 -> MipsInstr(vAddr, Lwl, rt, rs, imm)
            opcode == 0b100_110 -> MipsInstr(vAddr, Lwr, rt, rs, imm)
            opcode == 0b001_101 -> MipsInstr(vAddr, Ori, rt, rs, zeroExtendImm)
            opcode == 0b101_000 -> MipsInstr(vAddr, Sb, rt, rs, imm)
            opcode == 0b111_000 -> MipsInstr(vAddr, Sc, rt, rs, imm)
            opcode == 0b101_001 -> MipsInstr(vAddr, Sh, rt, rs, imm)
            opcode == 0b001_010 -> MipsInstr(vAddr, Slti, rt, rs, imm)
            opcode == 0b001_011 -> MipsInstr(vAddr, Sltiu, rt, rs, imm.toHintedUnsigned())
            opcode == 0b101_011 -> MipsInstr(vAddr, Sw, rt, rs, imm)
            opcode == 0b111_001 -> MipsInstr(vAddr, Swc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
            opcode == 0b101_010 -> MipsInstr(vAddr, Swl, rt, rs, imm)
            opcode == 0b101_110 -> MipsInstr(vAddr, Swr, rt, rs, imm)
            opcode == 0b001_110 -> MipsInstr(vAddr, Xori, rt, rs, zeroExtendImm)
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }
}
