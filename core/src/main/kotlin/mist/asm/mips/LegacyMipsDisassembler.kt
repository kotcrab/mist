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

package mist.asm.mips

import kio.util.toWHex
import mist.asm.*
import mist.asm.mips.MipsOpcode.*
import mist.io.BinLoader

/** @author Kotcrab */

class LegacyMipsDisassembler(val srcProcessor: LegacyMipsProcessor) : Disassembler<MipsInstr> {
    private companion object {
        const val SPECIAL = 0
        const val REGIMM = 1
        const val COP0 = 0b010_000
        const val COP1 = 0b010_001
        const val COP2 = 0b010_010
        const val COP3 = 0b010_011
        const val FMT_S = 16
        const val FMT_D = 17
        const val FMT_W = 20
    }

    override fun disassemble(loader: BinLoader, funcDef: FunctionDef): Disassembly<MipsInstr> {
        if (funcDef.offset % 4 != 0) error("offset must be a multiply of 4")
        if (funcDef.len % 4 != 0) error("length must be a multiply of 4")
        val decoded = mutableListOf<MipsInstr>()

        repeat(funcDef.len / 4) { instrCount ->
            val vAddr = funcDef.offset + instrCount * 4
            val instr = loader.readInt(vAddr)
            val opcode = instr ushr 26
            when {
                instr == 0 -> decoded.add(MipsInstr(vAddr, Nop))
                opcode == SPECIAL -> decoded.add(disasmSpecialInstr(vAddr, instr, instrCount))
                opcode == REGIMM -> decoded.add(disasmRegimmInstr(vAddr, instr, instrCount))
                opcode == COP1 -> decoded.add(disasmCop1Instr(vAddr, instr, instrCount))
                opcode == COP2 -> decoded.add(disasmCop2Instr(vAddr, instr, instrCount))
                opcode == COP3 -> decoded.add(disasmCop3Instr(vAddr, instr, instrCount))
                else -> decoded.add(disasmOpcodeInstr(vAddr, instr, instrCount, opcode))
            }
        }
        if (decoded.any { it.hasProcessor(srcProcessor) == false }) {
            error("generated disassembly uses opcodes not supported by specified processor")
        }
        return Disassembly(funcDef, decoded)
    }

    private fun disasmSpecialInstr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
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
            funct == 0b001_011 && shift == 0 -> MipsInstr(vAddr, Movn, rd, rs, rt)
            funct == 0b001_010 && shift == 0 -> MipsInstr(vAddr, Movz, rd, rs, rt)
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
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    private fun disasmCop1Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        // only for FPU R instruction
        val rt = GprReg.forId(instr ushr 16 and 0x1F)
        // only for FPU instruction
        val fmt = instr ushr 21 and 0x1F
        val ft = FpuReg.forId(instr ushr 16 and 0x1F)
        val fs = FpuReg.forId(instr ushr 11 and 0x1F)
        val fd = FpuReg.forId(instr ushr 6 and 0x1F)
        val funct = instr and 0x3F
        // only for branch instruction
        val branchTarget = (instr and 0xFFFF).toShort().toInt()
        return when (fmt) {
//            0b00100 -> MipsInstr(vAddr, FpuMtc1, RegOperand(rt), RegOperand(fs))
//            0b00000 -> MipsInstr(vAddr, FpuMfc1, RegOperand(rt), RegOperand(fs))
//            0b00110 -> MipsInstr(vAddr, FpuCtc1, RegOperand(rt), RegOperand(fs))
//            0b00010 -> MipsInstr(vAddr, FpuCfc1, RegOperand(rt), RegOperand(fs))
            0b01000 -> {
                when (rt.id) {
//                    0b00 -> MipsInstr(vAddr, FpuBc1f, ImmOperand(branchTarget))
//                    0b01 -> MipsInstr(vAddr, FpuBc1t, ImmOperand(branchTarget))
//                    0b11 -> MipsInstr(vAddr, FpuBc1tl, ImmOperand(branchTarget))
//                    0b10 -> MipsInstr(vAddr, FpuBc1fl, ImmOperand(branchTarget))
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            FMT_S -> {
                when (funct) {
//                    0b000_000 -> MipsInstr(vAddr, FpuAddS, RegOperand(fd), RegOperand(fs), RegOperand(ft))
//                    0b000_001 -> MipsInstr(vAddr, FpuSubS, RegOperand(fd), RegOperand(fs), RegOperand(ft))
//                    0b000_010 -> MipsInstr(vAddr, FpuMulS, RegOperand(fd), RegOperand(fs), RegOperand(ft))
//                    0b000_011 -> MipsInstr(vAddr, FpuDivS, RegOperand(fd), RegOperand(fs), RegOperand(ft))
//                    0b000_101 -> MipsInstr(vAddr, FpuAbsS, RegOperand(fd), RegOperand(fs))
//                    0b000_111 -> MipsInstr(vAddr, FpuNegS, RegOperand(fd), RegOperand(fs))
//                    0b000_100 -> MipsInstr(vAddr, FpuSqrtS, RegOperand(fd), RegOperand(fs))
//                    0b001_100 -> MipsInstr(vAddr, FpuRoundWS, RegOperand(fd), RegOperand(fs))
//                    0b001_101 -> MipsInstr(vAddr, FpuTruncWS, RegOperand(fd), RegOperand(fs))
//                    0b001_110 -> MipsInstr(vAddr, FpuCeilWS, RegOperand(fd), RegOperand(fs))
//                    0b001_111 -> MipsInstr(vAddr, FpuFloorWS, RegOperand(fd), RegOperand(fs))
//
//                    0b100_100 -> MipsInstr(vAddr, FpuCvtWS, RegOperand(fd), RegOperand(fs))
//                    0b11_0010 -> MipsInstr(vAddr, FpuCEqS, RegOperand(fs), RegOperand(ft))
//                    0b11_1110 -> MipsInstr(vAddr, FpuCLeS, RegOperand(fs), RegOperand(ft))
//                    0b11_1100 -> MipsInstr(vAddr, FpuCLtS, RegOperand(fs), RegOperand(ft))
//                    0b000_110 -> MipsInstr(vAddr, FpuMovS, RegOperand(fd), RegOperand(fs))
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            FMT_W -> {
                when (funct) {
//                    0b100_000 -> MipsInstr(vAddr, FpuCvtSW, RegOperand(fd), RegOperand(fs))
                    else -> handleUnknownInstr(vAddr, instrCount)
                }
            }
            FMT_D -> {
                handleUnknownInstr(vAddr, instrCount)
            }
            else -> MipsInstr(vAddr, Cop1, ImmOperand(instr and 0x3FFFFFF))
        }
    }

    private fun disasmCop2Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        return MipsInstr(vAddr, Cop2, ImmOperand(instr and 0x3FFFFFF))
    }

    private fun disasmCop3Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
        if (srcProcessor !in arrayOf(MipsIProcessor, MipsIIProcessor)) {
            error("COP3 is only defined on MIPS I and MIPS II architecture levels")
        }
        return MipsInstr(vAddr, Cop3, ImmOperand(instr and 0x3FFFFFF))
    }

    private fun disasmRegimmInstr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
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

    private fun disasmOpcodeInstr(vAddr: Int, instr: Int, instrCount: Int, opcode: Int): MipsInstr {
        // only applies to I instruction
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
            opcode == 0b110_101 -> MipsInstr(vAddr, Ldc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
            opcode == 0b110_110 -> MipsInstr(vAddr, Ldc2, RegOperand(Cop2Reg.forId(rt.reg.id)), rs, imm)
            opcode == 0b110_111 && srcProcessor in arrayOf(MipsIProcessor, MipsIIProcessor) -> {
                MipsInstr(vAddr, Ldc3, RegOperand(Cop3Reg.forId(rt.reg.id)), rs, imm)
            }
            opcode == 0b100_001 -> MipsInstr(vAddr, Lh, rt, rs, imm)
            opcode == 0b100_101 -> MipsInstr(vAddr, Lhu, rt, rs, imm)
            opcode == 0b110_000 -> MipsInstr(vAddr, Ll, rt, rs, imm)
            opcode == 0b001_111 -> MipsInstr(vAddr, Lui, rt, imm)
            opcode == 0b100_011 -> MipsInstr(vAddr, Lw, rt, rs, imm)
            opcode == 0b110_001 -> MipsInstr(vAddr, Lwc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
            opcode == 0b110_010 -> MipsInstr(vAddr, Lwc2, RegOperand(Cop2Reg.forId(rt.reg.id)), rs, imm)
            opcode == 0b110_011 && srcProcessor in arrayOf(MipsIProcessor, MipsIIProcessor) -> {
                MipsInstr(vAddr, Lwc3, RegOperand(Cop3Reg.forId(rt.reg.id)), rs, imm)
            }
            opcode == 0b100_010 -> MipsInstr(vAddr, Lwl, rt, rs, imm)
            opcode == 0b100_110 -> MipsInstr(vAddr, Lwr, rt, rs, imm)
            opcode == 0b001_101 -> MipsInstr(vAddr, Ori, rt, rs, imm)
            opcode == 0b110_011 -> MipsInstr(vAddr, Pref, ImmOperand(rt.reg.id), rs, imm)
            opcode == 0b101_000 -> MipsInstr(vAddr, Sb, rt, rs, imm)
            opcode == 0b111_000 -> MipsInstr(vAddr, Sc, rt, rs, imm)
            opcode == 0b111_101 -> MipsInstr(vAddr, Sdc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
            opcode == 0b111_110 -> MipsInstr(vAddr, Sdc2, RegOperand(Cop2Reg.forId(rt.reg.id)), rs, imm)
            opcode == 0b111_111 && srcProcessor in arrayOf(MipsIProcessor, MipsIIProcessor) -> {
                MipsInstr(vAddr, Sdc3, RegOperand(Cop3Reg.forId(rt.reg.id)), rs, imm)
            }
            opcode == 0b101_001 -> MipsInstr(vAddr, Sh, rt, rs, imm)
            opcode == 0b001_010 -> MipsInstr(vAddr, Slti, rt, rs, imm)
            opcode == 0b001_011 -> MipsInstr(vAddr, Sltiu, rt, rs, imm)
            opcode == 0b101_011 -> MipsInstr(vAddr, Sw, rt, rs, imm)
            opcode == 0b111_001 -> MipsInstr(vAddr, Swc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
            opcode == 0b111_010 -> MipsInstr(vAddr, Swc2, RegOperand(Cop2Reg.forId(rt.reg.id)), rs, imm)
            opcode == 0b111_011 && srcProcessor in arrayOf(MipsIProcessor, MipsIIProcessor) -> {
                MipsInstr(vAddr, Swc3, RegOperand(Cop3Reg.forId(rt.reg.id)), rs, imm)
            }
            opcode == 0b101_010 -> MipsInstr(vAddr, Swl, rt, rs, imm)
            opcode == 0b101_110 -> MipsInstr(vAddr, Swr, rt, rs, imm)
            opcode == 0b001_110 -> MipsInstr(vAddr, Xori, rt, rs, imm)
            else -> handleUnknownInstr(vAddr, instrCount)
        }
    }

    private fun handleUnknownInstr(vAddr: Int, instrCount: Int): Nothing {
        error("unknown instruction at offset ${(instrCount * 4).toWHex()}, address ${vAddr.toWHex()}")
    }
}
