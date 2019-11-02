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
import mist.asm.FloatOperand
import mist.asm.ImmOperand
import mist.asm.RegOperand
import mist.asm.mips.*
import mist.asm.mips.MipsDisassembler.StrictCheck.*
import mist.asm.mips.MipsOpcode.*
import mist.asm.mips.allegrex.AllegrexOpcode.*
import mist.asm.mips.allegrex.VfpuOpcode.*

/** @author Kotcrab */

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
            funct == 0b001_101 -> MipsInstr(vAddr, Break, ImmOperand(instr ushr 6 and 0x1FFFFF))
            funct == 0b011_010 && ifStrict(ZeroRd, ZeroShift) -> MipsInstr(vAddr, Div, rs, rt)
            funct == 0b011_011 && ifStrict(ZeroRd, ZeroShift) -> MipsInstr(vAddr, Divu, rs, rt)
            funct == 0b001_001 && ifStrict(ZeroRt) -> MipsInstr(vAddr, Jalr, rd, rs)
            funct == 0b001_000 && ifStrict(ZeroRt, ZeroRd) -> MipsInstr(vAddr, Jr, rs)
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
            funct == 0b000_110 && shift == 1 && ifStrict { shift ushr 1 == 0 } -> {
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
            funct == 0b000_110 && shift != 1 && ifStrict { shift ushr 1 == 0 } -> {
                MipsInstr(vAddr, Srlv, rd, rt, rs)
            }
            funct == 0b100_010 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Sub, rd, rs, rt)
            funct == 0b100_011 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Subu, rd, rs, rt)
            funct == 0b001_111 && ifStrict(ZeroRs, ZeroRt, ZeroRd) -> {
                MipsInstr(vAddr, Sync, ImmOperand(shift))
            }
            funct == 0b001_100 -> MipsInstr(vAddr, Syscall)
            funct == 0b110_100 -> MipsInstr(vAddr, Teq, rs, rt)
            funct == 0b110_000 -> MipsInstr(vAddr, Tge, rs, rt)
            funct == 0b110_001 -> MipsInstr(vAddr, Tgeu, rs, rt)
            funct == 0b110_010 -> MipsInstr(vAddr, Tlt, rs, rt)
            funct == 0b110_011 -> MipsInstr(vAddr, Tltu, rs, rt)
            funct == 0b110_110 -> MipsInstr(vAddr, Tne, rs, rt)
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

    override fun disasmSpecial2Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
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
            funct == 0b100100 && ifStrict(ZeroRs, ZeroShift) -> MipsInstr(vAddr, Mfic, rt, rd)
            funct == 0b100110 && ifStrict(ZeroRs, ZeroShift) -> MipsInstr(vAddr, Mtic, rt, rd)
            funct == 0b000000 && ifStrict(ZeroRs, ZeroRt, ZeroRd, ZeroShift) -> MipsInstr(vAddr, Halt)
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    override fun disasmSpecial3Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
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
            funct == 0b000_000 -> MipsInstr(vAddr, Ext, rt, rs, ImmOperand(shift), ImmOperand(rd.reg.id + 1))
            funct == 0b000_100 -> {
                MipsInstr(vAddr, Ins, rt, rs, ImmOperand(shift), ImmOperand(rd.reg.id + 1 - shift).toHintedUnsigned())
            }
            funct == 0b111_011 && ifStrict(ZeroRs, ZeroShift) -> MipsInstr(vAddr, Rdhwr, rt, rd)
            (funct == 0b100_000 || funct == 0b011_000) && shift == 0b00010 && ifStrict(ZeroRs) -> {
                MipsInstr(vAddr, Wsbh, rd, rt)
            }
            (funct == 0b100_000 || funct == 0b011_000) && shift == 0b00011 && ifStrict(ZeroRs) -> {
                MipsInstr(vAddr, Wsbw, rd, rt)
            }
            (funct == 0b100_000 || funct == 0b011_000) && shift == 0b10000 && ifStrict(ZeroRs) -> {
                MipsInstr(vAddr, Seb, rd, rt)
            }
            (funct == 0b100_000 || funct == 0b011_000) && shift == 0b10100 && ifStrict(ZeroRs) -> {
                MipsInstr(vAddr, Bitrev, rd, rt)
            }
            (funct == 0b100_000 || funct == 0b011_000) && shift == 0b11000 && ifStrict(ZeroRs) -> {
                MipsInstr(vAddr, Seh, rd, rt)
            }
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    override fun disasmRegimmInstr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        val rs = RegOperand(GprReg.forId(instr ushr 21 and 0x1F))
        val rt = instr ushr 16 and 0x1F
        val imm = ImmOperand((instr and 0xFFFF).toShort().toInt())
        val branchImm = ImmOperand(vAddr + 0x4 + imm.value * 0x4, hintUnsigned = true)
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
            op == 0b00_000 && ifStrict { instr and 0x7FF == 0 } -> {
                MipsInstr(vAddr, Mfc0, rt, rd)
            }
            op == 0b00_100 && ifStrict { instr and 0x7FF == 0 } -> {
                MipsInstr(vAddr, Mtc0, rt, rd)
            }
            co == 1 && tlop == 0b001_000 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Tlbp)
            co == 1 && tlop == 0b000_001 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Tlbr)
            co == 1 && tlop == 0b000_010 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Tlbwi)
            co == 1 && tlop == 0b000_110 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Tlbwr)
            co == 1 && tlop == 0b011_111 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Deret)
            co == 1 && tlop == 0b011_000 && ifStrict { instr ushr 6 and 0x7FFFF == 0 } -> MipsInstr(vAddr, Eret)
            co == 1 && tlop == 0b100_000 -> MipsInstr(vAddr, Wait)
            op == 0b01_010 && ifStrict { instr and 0x7FF == 0 } -> MipsInstr(vAddr, Rdpgpr, rt, rd)
            op == 0b01_110 && ifStrict { instr and 0x7FF == 0 } -> MipsInstr(vAddr, Wrpgpr, rt, rd)
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

    override fun disasmCop2Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        val op = instr ushr 21 and 0x1F
        val vd = RegOperand(VfpuReg.forId(instr and 0x7F))
        val vdc = RegOperand(VfpuReg.controlForId(instr and 0x7F))
//        val vs = RegOperand(VfpuReg.forId(instr ushr 8 and 0x7F))
//        val vt = RegOperand(VfpuReg.forId(instr ushr 16 and 0x7F))
        val rt = RegOperand(GprReg.forId(instr ushr 16 and 0x1F))
        // TODO vfpu unit tests
        return when (op) {
            0b00011 -> {
                val c = instr ushr 7 and 0b1
                if (c == 0) {
                    MipsInstr(vAddr, Mfv, rt, vd)
                } else {
                    MipsInstr(vAddr, Mfvc, rt, vdc)
                }
            }
            0b00111 -> {
                val c = instr ushr 7 and 0b1
                if (c == 0) {
                    MipsInstr(vAddr, Mtv, rt, vd)
                } else {
                    MipsInstr(vAddr, Mtvc, rt, vdc)
                }
            }
            0b01000 -> {
                val branchImm = (instr and 0xFFFF).toShort().toInt()
                val branchTarget = ImmOperand(vAddr + 0x4 + branchImm * 0x4)
                val ndtf = instr ushr 16 and 0b11
                val ccVal = instr ushr 18 and 0b111
                val cc = RegOperand(VfpuReg.Cc, "CC[$ccVal]", bitsStart = ccVal, bitsSize = 1)
                when (ndtf) {
                    0b00 -> MipsInstr(vAddr, Bvf, cc, branchTarget)
                    0b10 -> MipsInstr(vAddr, Bvfl, cc, branchTarget)
                    0b01 -> MipsInstr(vAddr, Bvt, cc, branchTarget)
                    0b11 -> MipsInstr(vAddr, Bvtl, cc, branchTarget)
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
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
            opcode == 0b101_111 -> MipsInstr(vAddr, Cache, rt, rs, imm)
            // VFPU memory
            opcode == 0b110_010 -> {
                val vt = VfpuReg.forId((instr and 0b11 shl 5) or (instr ushr 16 and 0x1F))
                MipsInstr(vAddr, LvS, RegOperand(vt), rs, ImmOperand((imm.value shr 2) * 4))
            }
            opcode == 0b110_101 -> {
                val vt = VfpuReg.forId((instr and 0b1 shl 5) or (instr ushr 16 and 0x1F))
                val vtQ = RegOperand(vt, vt.qName)
                val vimm = ImmOperand((imm.value shr 2) * 4)
                when {
                    instr ushr 1 and 1 == 0 -> MipsInstr(vAddr, LvlQ, vtQ, rs, vimm)
                    instr ushr 1 and 1 == 1 -> MipsInstr(vAddr, LvrQ, vtQ, rs, vimm)
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            opcode == 0b110_110 -> {
                val vt = VfpuReg.forId((instr and 0b1 shl 5) or (instr ushr 16 and 0x1F))
                val vtQ = RegOperand(vt, vt.qName)
                val vimm = ImmOperand((imm.value shr 2) * 4)
                when {
                    instr ushr 1 and 1 == 0 -> MipsInstr(vAddr, LvQ, vtQ, rs, vimm)
                    instr ushr 1 and 1 == 1 -> MipsInstr(vAddr, LvQ, vtQ, rs, vimm, VfpuWbOperand())
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            opcode == 0b111_010 -> {
                val vt = VfpuReg.forId((instr and 0b11 shl 5) or (instr ushr 16 and 0x1F))
                MipsInstr(vAddr, SvS, RegOperand(vt), rs, ImmOperand((imm.value shr 2) * 4))
            }
            opcode == 0b111_101 -> {
                val vt = VfpuReg.forId((instr and 0b1 shl 5) or (instr ushr 16 and 0x1F))
                val vtQ = RegOperand(vt, vt.qName)
                val vimm = ImmOperand((imm.value shr 2) * 4)
                when {
                    instr ushr 1 and 1 == 0 -> MipsInstr(vAddr, SvlQ, vtQ, rs, vimm)
                    instr ushr 1 and 1 == 1 -> MipsInstr(vAddr, SvrQ, vtQ, rs, vimm)
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            opcode == 0b111_110 -> {
                val vt = VfpuReg.forId((instr and 0b1 shl 5) or (instr ushr 16 and 0x1F))
                val vtQ = RegOperand(vt, vt.qName)
                val vimm = ImmOperand((imm.value shr 2) * 4)
                when {
                    instr ushr 1 and 1 == 0 -> MipsInstr(vAddr, SvQ, vtQ, rs, vimm)
                    instr ushr 1 and 1 == 1 -> MipsInstr(vAddr, SvQ, vtQ, rs, vimm, VfpuWbOperand())
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }

            else -> handleVfpuInstr(vAddr, instr, instrCount, opcode)
        }
    }

    private fun handleVfpuInstr(vAddr: Int, instr: Int, instrCount: Int, op: Int): MipsInstr {
        val op3 = instr ushr 23 and 0b111
        val c0 = instr ushr 7 and 1
        val c1 = instr ushr 15 and 1
        val vt = VfpuReg.forId(instr ushr 16 and 0x7F)
        val vs = VfpuReg.forId(instr ushr 8 and 0x7F)
        val vd = VfpuReg.forId(instr and 0x7F)
        val vtS = RegOperand(vt, vt.sName)
        val vsS = RegOperand(vs, vs.sName)
        val vdS = RegOperand(vd, vd.sName)
        val vtP = RegOperand(vt, vt.pName)
        val vsP = RegOperand(vs, vs.pName)
        val vdP = RegOperand(vd, vd.pName)
        val vtT = RegOperand(vt, vt.tName)
        val vsT = RegOperand(vs, vs.tName)
        val vdT = RegOperand(vd, vd.tName)
        val vtQ = RegOperand(vt, vt.qName)
        val vsQ = RegOperand(vs, vs.qName)
        val vdQ = RegOperand(vd, vd.qName)

        val vtM = RegOperand(vt, vt.mName)
        val vsM = RegOperand(vs, vs.mName)
        val vdM = RegOperand(vd, vd.mName)
//        val vtE = RegOperand(vt, vt.eName)
        val vsE = RegOperand(vs, vs.eName)
//        val vdE = RegOperand(vd, vd.eName)

        val vtTM = RegOperand(vt, vt.tName.replace("C", "M").replace("R", "E"))
        val vsTM = RegOperand(vs, vs.tName.replace("C", "M").replace("R", "E"))
        val vdTM = RegOperand(vd, vd.tName.replace("C", "M").replace("R", "E"))
//        val vtTE = RegOperand(vt, vt.teName.replace("C", "M").replace("R", "E"))
        val vsTE = RegOperand(vs, vs.teName.replace("C", "M").replace("R", "E"))
//        val vdTE = RegOperand(vd, vd.teName.replace("C", "M").replace("R", "E"))

        return when {
            op == 0b011_000 && op3 == 0b000 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VaddS, vdS, vsS, vtS)
            op == 0b011_000 && op3 == 0b000 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VaddP, vdP, vsP, vtP)
            op == 0b011_000 && op3 == 0b000 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VaddT, vdT, vsT, vtT)
            op == 0b011_000 && op3 == 0b000 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VaddQ, vdQ, vsQ, vtQ)

            op == 0b011_000 && op3 == 0b001 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VsubS, vdS, vsS, vtS)
            op == 0b011_000 && op3 == 0b001 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VsubP, vdP, vsP, vtP)
            op == 0b011_000 && op3 == 0b001 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VsubT, vdT, vsT, vtT)
            op == 0b011_000 && op3 == 0b001 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VsubQ, vdQ, vsQ, vtQ)

            op == 0b011_000 && op3 == 0b010 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VsbnS, vdS, vsS, vtS)
            op == 0b011_000 && op3 == 0b010 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VsbnP, vdP, vsP, vtP)
            op == 0b011_000 && op3 == 0b010 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VsbnT, vdT, vsT, vtT)
            op == 0b011_000 && op3 == 0b010 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VsbnQ, vdQ, vsQ, vtQ)

            op == 0b011_000 && op3 == 0b111 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VdivS, vdS, vsS, vtS)
            op == 0b011_000 && op3 == 0b111 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VdivP, vdP, vsP, vtP)
            op == 0b011_000 && op3 == 0b111 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VdivT, vdT, vsT, vtT)
            op == 0b011_000 && op3 == 0b111 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VdivQ, vdQ, vsQ, vtQ)

            op == 0b011_001 && op3 == 0b000 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VmulS, vdS, vsS, vtS)
            op == 0b011_001 && op3 == 0b000 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VmulP, vdP, vsP, vtP)
            op == 0b011_001 && op3 == 0b000 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VmulT, vdT, vsT, vtT)
            op == 0b011_001 && op3 == 0b000 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VmulQ, vdQ, vsQ, vtQ)

            op == 0b011_001 && op3 == 0b001 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VdotS, vdS, vsS, vtS)
            op == 0b011_001 && op3 == 0b001 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VdotP, vdS, vsP, vtP)
            op == 0b011_001 && op3 == 0b001 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VdotT, vdS, vsT, vtT)
            op == 0b011_001 && op3 == 0b001 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VdotQ, vdS, vsQ, vtQ)

            op == 0b011_001 && op3 == 0b010 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VsclS, vdS, vsS, vtS)
            op == 0b011_001 && op3 == 0b010 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VsclP, vdP, vsP, vtS)
            op == 0b011_001 && op3 == 0b010 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VsclT, vdT, vsT, vtS)
            op == 0b011_001 && op3 == 0b010 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VsclQ, vdQ, vsQ, vtS)

            op == 0b011_001 && op3 == 0b100 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VhdpS, vdS, vsS, vtS)
            op == 0b011_001 && op3 == 0b100 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VhdpP, vdS, vsP, vtP)
            op == 0b011_001 && op3 == 0b100 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VhdpT, vdS, vsT, vtT)
            op == 0b011_001 && op3 == 0b100 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VhdpQ, vdS, vsQ, vtQ)

            op == 0b011_001 && op3 == 0b101 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VcrsT, vdT, vsT, vtT)

            op == 0b011_001 && op3 == 0b110 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VdetS, vdS, vsS, vtS)
            op == 0b011_001 && op3 == 0b110 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VdetP, vdS, vsP, vtP)
            op == 0b011_001 && op3 == 0b110 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VdetT, vdS, vsT, vtT)
            op == 0b011_001 && op3 == 0b110 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VdetQ, vdS, vsQ, vtQ)

            op == 0b011_011 && op3 == 0b000 && c1 == 0 && c0 == 0 -> {
                MipsInstr(vAddr, VcmpS, VfpuCmpOperand(VfpuCmpMode.forId(vd.id)), vsS, vtS)
            }
            op == 0b011_011 && op3 == 0b000 && c1 == 0 && c0 == 1 -> {
                MipsInstr(vAddr, VcmpP, VfpuCmpOperand(VfpuCmpMode.forId(vd.id)), vsP, vtP)
            }
            op == 0b011_011 && op3 == 0b000 && c1 == 1 && c0 == 0 -> {
                MipsInstr(vAddr, VcmpT, VfpuCmpOperand(VfpuCmpMode.forId(vd.id)), vsT, vtT)
            }
            op == 0b011_011 && op3 == 0b000 && c1 == 1 && c0 == 1 -> {
                MipsInstr(vAddr, VcmpQ, VfpuCmpOperand(VfpuCmpMode.forId(vd.id)), vsQ, vtQ)
            }

            op == 0b011_011 && op3 == 0b010 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VminS, vdS, vsS, vtS)
            op == 0b011_011 && op3 == 0b010 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VminP, vdP, vsP, vtP)
            op == 0b011_011 && op3 == 0b010 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VminT, vdT, vsT, vtT)
            op == 0b011_011 && op3 == 0b010 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VminQ, vdQ, vsQ, vtQ)

            op == 0b011_011 && op3 == 0b011 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VmaxS, vdS, vsS, vtS)
            op == 0b011_011 && op3 == 0b011 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VmaxP, vdP, vsP, vtP)
            op == 0b011_011 && op3 == 0b011 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VmaxT, vdT, vsT, vtT)
            op == 0b011_011 && op3 == 0b011 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VmaxQ, vdQ, vsQ, vtQ)

            op == 0b011_011 && op3 == 0b101 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VscmpS, vdS, vsS, vtS)
            op == 0b011_011 && op3 == 0b101 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VscmpP, vdP, vsP, vtP)
            op == 0b011_011 && op3 == 0b101 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VscmpT, vdT, vsT, vtT)
            op == 0b011_011 && op3 == 0b101 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VscmpQ, vdQ, vsQ, vtQ)

            op == 0b011_011 && op3 == 0b110 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VsgeS, vdS, vsS, vtS)
            op == 0b011_011 && op3 == 0b110 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VsgeP, vdP, vsP, vtP)
            op == 0b011_011 && op3 == 0b110 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VsgeT, vdT, vsT, vtT)
            op == 0b011_011 && op3 == 0b110 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VsgeQ, vdQ, vsQ, vtQ)

            op == 0b011_011 && op3 == 0b111 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VsltS, vdS, vsS, vtS)
            op == 0b011_011 && op3 == 0b111 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VsltP, vdP, vsP, vtP)
            op == 0b011_011 && op3 == 0b111 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VsltT, vdT, vsT, vtT)
            op == 0b011_011 && op3 == 0b111 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VsltQ, vdQ, vsQ, vtQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b000000 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VmovS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000000 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VmovP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000000 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VmovT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000000 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VmovQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b000001 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VabsS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000001 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VabsP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000001 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VabsT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000001 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VabsQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b000010 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VnegS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000010 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VnegP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000010 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VnegT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000010 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VnegQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b000011 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VidtS, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000011 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VidtP, vdP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000011 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VidtT, vdT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000011 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VidtQ, vdQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b000100 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vsat0S, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000100 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vsat0P, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000100 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vsat0T, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000100 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vsat0Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b000101 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vsat1S, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000101 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vsat1P, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000101 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vsat1T, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000101 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vsat1Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b000110 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VzeroS, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000110 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VzeroP, vdP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000110 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VzeroT, vdT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000110 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VzeroQ, vdQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b000111 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VoneS, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000111 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VoneP, vdP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000111 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VoneT, vdT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b000111 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VoneQ, vdQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b010000 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VrcpS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010000 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VrcpP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010000 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VrcpT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010000 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VrcpQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b010001 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VrsqS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010001 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VrsqP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010001 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VrsqT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010001 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VrsqQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b010010 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VsinS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010010 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VsinP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010010 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VsinT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010010 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VsinQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b010011 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VcosS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010011 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VcosP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010011 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VcosT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010011 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VcosQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b010100 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vexp2S, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010100 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vexp2P, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010100 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vexp2T, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010100 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vexp2Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b010101 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vlog2S, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010101 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vlog2P, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010101 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vlog2T, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010101 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vlog2Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b010110 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VsqrtS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010110 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VsqrtP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010110 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VsqrtT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010110 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VsqrtQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b010111 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VasinS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010111 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VasinP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010111 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VasinT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b010111 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VasinQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b011000 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VnrcpS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b011000 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VnrcpP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b011000 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VnrcpT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b011000 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VnrcpQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b011010 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VnsinS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b011010 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VnsinP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b011010 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VnsinT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b011010 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VnsinQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b11100 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vrexp2S, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b11100 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vrexp2P, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b11100 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vrexp2T, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b11100 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vrexp2Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b100000 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VrndsS, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100000 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VrndsP, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100000 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VrndsT, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100000 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VrndsQ, vdS)

            op == 0b110_100 && op3 == 0 && vt.id == 0b100001 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VrndiS, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100001 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VrndiP, vdP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100001 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VrndiT, vdT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100001 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VrndiQ, vdQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b100010 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vrndf1S, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100010 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vrndf1P, vdP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100010 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vrndf1T, vdT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100010 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vrndf1Q, vdQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b100011 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vrndf2S, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100011 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vrndf2P, vdP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100011 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vrndf2T, vdT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b100011 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vrndf2Q, vdQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b101100 -> MipsInstr(vAddr, Vsbz, vdS, vsS)

            op == 0b110_100 && op3 == 0 && vt.id == 0b110010 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vf2hP, vdS, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b110010 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vf2hQ, vdP, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b110011 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vh2fS, vdP, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b110011 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vh2fP, vdP, vsP)

            op == 0b110_100 && op3 == 0 && vt.id == 0b110111 -> MipsInstr(vAddr, Vlgb, vdS, vsS)

            op == 0b110_100 && op3 == 0 && vt.id == 0b111000 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vuc2iS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111000 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vuc2iP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111000 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vuc2iT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111000 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vuc2iQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b111001 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vc2iS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111001 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vc2iP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111001 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vc2iT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111001 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vc2iQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b111010 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vus2iS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111010 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vus2iP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111010 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vus2iT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111010 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vus2iQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b111011 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vs2iS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111011 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vs2iP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111011 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vs2iT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111011 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vs2iQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b111100 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vi2ucS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111100 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vi2ucP, vdS, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111100 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vi2ucT, vdS, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111100 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vi2ucQ, vdS, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b111101 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vi2cP, vdS, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111101 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vi2cQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b111110 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vi2usP, vdS, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111110 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vi2usQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b111111 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vi2sP, vdS, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b111111 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vi2sQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1000000 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vsrt1S, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000000 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vsrt1P, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000000 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vsrt1T, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000000 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vsrt1Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1000001 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vsrt2S, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000001 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vsrt2P, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000001 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vsrt2T, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000001 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vsrt2Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1000010 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vbfy1S, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000010 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vbfy1P, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000010 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vbfy1T, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000010 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vbfy1Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1000011 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vbfy2S, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000011 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vbfy2P, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000011 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vbfy2T, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000011 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vbfy2Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1000100 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VocpS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000100 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VocpP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000100 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VocpT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000100 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VocpQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1000101 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, VsocpS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000101 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, VsocpP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000101 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, VsocpT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000101 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, VsocpQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1000110 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VfadS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000110 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VfadP, vdS, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000110 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VfadT, vdS, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000110 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VfadQ, vdS, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1000111 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VavgS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000111 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VavgP, vdS, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000111 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VavgT, vdS, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1000111 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VavgQ, vdS, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1001000 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vsrt3S, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1001000 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vsrt3P, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1001000 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vsrt3T, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1001000 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vsrt3Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1001001 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vsrt4S, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1001001 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vsrt4P, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1001001 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vsrt4T, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1001001 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vsrt4Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1001010 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VsgnS, vdS, vsS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1001010 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VsgnP, vdP, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1001010 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VsgnT, vdT, vsT)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1001010 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VsgnQ, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1010000 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vmfvc, vsS, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1010000 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vmfvc, vsS, RegOperand(VfpuReg.controlForId(vd.id)))
            op == 0b110_100 && op3 == 0 && vt.id == 0b1010000 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vmfvc, vsS, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1010000 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vmfvc, vsS, RegOperand(VfpuReg.controlForId(vd.id)))

            op == 0b110_100 && op3 == 0 && vt.id == 0b1010001 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vmtvc, vsS, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1010001 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vmtvc, vsS, RegOperand(VfpuReg.controlForId(vd.id)))
            op == 0b110_100 && op3 == 0 && vt.id == 0b1010001 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vmtvc, vsS, vdS)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1010001 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vmtvc, vsS, RegOperand(VfpuReg.controlForId(vd.id)))

            op == 0b110_100 && op3 == 0 && vt.id == 0b1011001 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vt4444P, vdS, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1011001 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vt4444Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1011010 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vt5551P, vdS, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1011010 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vt5551Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id == 0b1011011 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vt5650P, vdS, vsP)
            op == 0b110_100 && op3 == 0 && vt.id == 0b1011011 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vt5650Q, vdQ, vsQ)

            op == 0b110_100 && op3 == 0 && vt.id ushr 5 == 0b11 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, VcstS, vdS, VfpuCstOperand(VfpuCstMode.forId(vt.id and 0x1F)))
            op == 0b110_100 && op3 == 0 && vt.id ushr 5 == 0b11 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, VcstP, vdS, VfpuCstOperand(VfpuCstMode.forId(vt.id and 0x1F)))
            op == 0b110_100 && op3 == 0 && vt.id ushr 5 == 0b11 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, VcstT, vdS, VfpuCstOperand(VfpuCstMode.forId(vt.id and 0x1F)))
            op == 0b110_100 && op3 == 0 && vt.id ushr 5 == 0b11 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, VcstQ, vdS, VfpuCstOperand(VfpuCstMode.forId(vt.id and 0x1F)))

            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b00 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vf2inS, vdS, vsS, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b00 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vf2inP, vdP, vsP, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b00 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vf2inT, vdT, vsT, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b00 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vf2inQ, vdQ, vsQ, ImmOperand(vt.id and 0x1F))

            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b01 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vf2izS, vdS, vsS, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b01 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vf2izP, vdP, vsP, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b01 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vf2izT, vdT, vsT, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b01 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vf2izQ, vdQ, vsQ, ImmOperand(vt.id and 0x1F))

            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b10 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vf2iuS, vdS, vsS, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b10 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vf2iuP, vdP, vsP, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b10 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vf2iuT, vdT, vsT, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b10 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vf2iuQ, vdQ, vsQ, ImmOperand(vt.id and 0x1F))

            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b11 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vf2idS, vdS, vsS, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b11 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vf2idP, vdP, vsP, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b11 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vf2idT, vdT, vsT, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b100 && vt.id ushr 5 == 0b11 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vf2idQ, vdQ, vsQ, ImmOperand(vt.id and 0x1F))

            op == 0b110_100 && op3 == 0b101 && vt.id ushr 5 == 0b00 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, Vi2fS, vdS, vsS, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b101 && vt.id ushr 5 == 0b00 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, Vi2fP, vdP, vsP, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b101 && vt.id ushr 5 == 0b00 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, Vi2fT, vdT, vsT, ImmOperand(vt.id and 0x1F))
            op == 0b110_100 && op3 == 0b101 && vt.id ushr 5 == 0b00 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, Vi2fQ, vdQ, vsQ, ImmOperand(vt.id and 0x1F))

            op == 0b110_100 && op3 == 0b101 && vt.id ushr 3 == 0b0100 -> {
                val ccVal = vt.id and 0b111
                val cc = RegOperand(VfpuReg.Cc, "CC[$ccVal]", bitsStart = ccVal, bitsSize = 1)
                when {
                    c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VcmovtS, vdS, vsS, cc)
                    c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VcmovtP, vdP, vsP, cc)
                    c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VcmovtT, vdT, vsT, cc)
                    c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VcmovtQ, vdQ, vsQ, cc)
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            op == 0b110_100 && op3 == 0b101 && vt.id ushr 3 == 0b0101 -> {
                val ccVal = vt.id and 0b111
                val cc = RegOperand(VfpuReg.Cc, "CC[$ccVal]", bitsStart = ccVal, bitsSize = 1)
                when {
                    c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VcmovfS, vdS, vsS, cc)
                    c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VcmovfP, vdP, vsP, cc)
                    c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VcmovfT, vdT, vsT, cc)
                    c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VcmovfQ, vdQ, vsQ, cc)
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }

            op == 0b110_100 && op3 ushr 1 == 0b11 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, VwbnSS, vdS, vsS, ImmOperand(op3 and 0b1 shl 7 or vt.id))
            op == 0b110_100 && op3 ushr 1 == 0b11 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, VwbnSP, vdP, vsP, ImmOperand(op3 and 0b1 shl 7 or vt.id))
            op == 0b110_100 && op3 ushr 1 == 0b11 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, VwbnST, vdT, vsT, ImmOperand(op3 and 0b1 shl 7 or vt.id))
            op == 0b110_100 && op3 ushr 1 == 0b11 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, VwbnSQ, vdQ, vsQ, ImmOperand(op3 and 0b1 shl 7 or vt.id))

            op == 0b110_111 && op3 ushr 1 == 0b00 -> MipsInstr(vAddr, Vpfxs, VfpuVpfxstOperand.decode(instr))
            op == 0b110_111 && op3 ushr 1 == 0b01 -> MipsInstr(vAddr, Vpfxt, VfpuVpfxstOperand.decode(instr))
            op == 0b110_111 && op3 ushr 1 == 0b10 -> MipsInstr(vAddr, Vpfxd, VfpuVpfxdOperand.decode(instr))

            op == 0b110_111 && op3 == 0b110 ->
                MipsInstr(vAddr, ViimS, vtS, ImmOperand(instr and 0xFFFF))
            op == 0b110_111 && op3 == 0b111 ->
                MipsInstr(vAddr, VfimS, vtS, FloatOperand(vfpuToFloat(instr and 0xFFFF)))

            op == 0b111_100 && op3 == 0 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VmmulS, vdM, vsE, vtM)
            op == 0b111_100 && op3 == 0 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VmmulP, vdM, vsE, vtM)
            op == 0b111_100 && op3 == 0 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VmmulT, vdTM, vsTE, vtTM)
            op == 0b111_100 && op3 == 0 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VmmulQ, vdM, vsE, vtM)

            op == 0b111_100 && op3 == 0b001 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, Vhtfm1S, vdS, vsM, vtS)
            op == 0b111_100 && op3 == 0b001 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vtfm2P, vdP, vsM, vtP)

            op == 0b111_100 && op3 == 0b010 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, Vhtfm2P, vdP, vsM, vtP)
            op == 0b111_100 && op3 == 0b010 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vtfm3T, vdT, vsTM, vtT)
            op == 0b111_100 && op3 == 0b011 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, Vhtfm3T, vdT, vsTM, vtT)
            op == 0b111_100 && op3 == 0b011 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, Vtfm4Q, vdP, vsM, vtP)

            op == 0b111_100 && op3 == 0b100 && c1 == 0 && c0 == 0 -> MipsInstr(vAddr, VmsclS, vdM, vsM, vtS)
            op == 0b111_100 && op3 == 0b100 && c1 == 0 && c0 == 1 -> MipsInstr(vAddr, VmsclP, vdM, vsM, vtS)
            op == 0b111_100 && op3 == 0b100 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VmsclT, vdTM, vsTM, vtS)
            op == 0b111_100 && op3 == 0b100 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VmsclQ, vdM, vsM, vtS)

            op == 0b111_100 && op3 == 0b101 && c1 == 1 && c0 == 0 -> MipsInstr(vAddr, VcrspT, vdT, vsT, vtT)
            op == 0b111_100 && op3 == 0b101 && c1 == 1 && c0 == 1 -> MipsInstr(vAddr, VqmulQ, vdQ, vsQ, vtQ)

            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0000 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, VmmovS, vdM, vsM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0000 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, VmmovP, vdM, vsM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0000 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, VmmovT, vdTM, vsTM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0000 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, VmmovQ, vdM, vsM)

            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0011 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, VmidtS, vdM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0011 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, VmidtP, vdM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0011 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, VmidtT, vdTM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0011 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, VmidtQ, vdM)

            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0110 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, VmzeroS, vdM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0110 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, VmzeroP, vdM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0110 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, VmzeroT, vdTM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0110 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, VmzeroQ, vdM)

            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0111 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, VmoneS, vdM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0111 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, VmoneP, vdM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0111 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, VmoneT, vdTM)
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0 && vt.id and 0b1111 == 0b0111 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, VmoneQ, vdM)

            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0b01 && c1 == 0 && c0 == 0 ->
                MipsInstr(vAddr, VrotS, vdS, vsS, VfpuRotOperand.decode(instr, 1))
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0b01 && c1 == 0 && c0 == 1 ->
                MipsInstr(vAddr, VrotP, vdP, vsS, VfpuRotOperand.decode(instr, 2))
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0b01 && c1 == 1 && c0 == 0 ->
                MipsInstr(vAddr, VrotT, vdT, vsS, VfpuRotOperand.decode(instr, 3))
            op == 0b111_100 && op3 == 0b111 && vt.id ushr 5 == 0b01 && c1 == 1 && c0 == 1 ->
                MipsInstr(vAddr, VrotQ, vdQ, vsS, VfpuRotOperand.decode(instr, 4))

            op == 0b111_111 -> MipsInstr(vAddr, Vflush)
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    private fun vfpuToFloat(hbits: Int): Float {
        var mant = hbits and 0x03ff // 10 bits mantissa
        var exp = hbits and 0x7c00 // 5 bits exponent

        if (exp == 0x7c00) { // NaN/Inf
            exp = 0x3fc00 // -> NaN/Inf
        } else if (exp != 0) { // normalized value
            exp += 0x1c000 // exp - 15 + 127
            if (mant == 0 && exp > 0x1c400) {
                // smooth transition
                return java.lang.Float.intBitsToFloat(hbits and 0x8000 shl 16 or (exp shl 13))
            }
        } else if (mant != 0) { // && exp==0 -> subnormal
            exp = 0x1c400 // make it normal
            do {
                mant = mant shl 1 // mantissa * 2
                exp -= 0x400 // decrease exp by 1
            } while ((mant and 0x400) == 0) // while not normal
            mant = mant and 0x3ff // discard subnormal bit
        }
        // combine all parts
        // (sign  << ( 31 - 15 )) or (value << ( 23 - 10 ))
        return java.lang.Float.intBitsToFloat((((hbits and 0x8000) shl 16) or ((exp or mant) shl 13)))
    }
}
