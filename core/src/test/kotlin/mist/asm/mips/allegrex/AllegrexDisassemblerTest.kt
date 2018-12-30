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

import kio.util.swapBytes
import kmips.Assembler
import kmips.FpuReg.*
import kmips.Label
import kmips.Reg.*
import kmips.assembleAsByteArray
import mist.asm.*
import mist.asm.mips.*
import mist.asm.mips.MipsOpcode.*
import mist.asm.mips.allegrex.AllegrexOpcode.Max
import mist.asm.mips.allegrex.AllegrexOpcode.Min
import mist.test.util.MemBinLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/** @author Kotcrab */

class AllegrexDisassemblerTest {
    private val labelExpected = 0x40

    @Test
    fun testAdd() = testInstr({ add(a0, s0, t0) }, { verify(Add, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testAddi() = testInstr({ addi(a0, s0, 0xCD) }, { verify(Addi, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testAddiu() = testInstr({ addiu(a0, s0, 0xCD) }, { verify(Addiu, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testAddu() = testInstr({ addu(a0, s0, t0) }, { verify(Addu, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testAnd() = testInstr({ and(a0, s0, t0) }, { verify(And, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testAndi() = testInstr({ andi(a0, s0, 0xCD) }, { verify(Andi, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testAndiZeroExtend() = testInstr({ andi(a0, s0, 0x8000) }, { verify(Andi, GprReg.A0, GprReg.S0, 0x8000) })

    @Test
    fun testBeq() = testInstr({ beq(a0, s0, testLabel()) }, { verify(Beq, GprReg.A0, GprReg.S0, labelExpected) })

    @Test
    fun testBeql() = testInstr({ beql(a0, s0, testLabel()) }, { verify(Beql, GprReg.A0, GprReg.S0, labelExpected) })

    @Test
    fun testBgez() = testInstr({ bgez(a0, testLabel()) }, { verify(Bgez, GprReg.A0, labelExpected) })

    @Test
    fun testBgezal() = testInstr({ bgezal(a0, testLabel()) }, { verify(Bgezal, GprReg.A0, labelExpected) })

    @Test
    fun testBgezall() = testInstr({ bgezall(a0, testLabel()) }, { verify(Bgezall, GprReg.A0, labelExpected) })

    @Test
    fun testBgezl() = testInstr({ bgezl(a0, testLabel()) }, { verify(Bgezl, GprReg.A0, labelExpected) })

    @Test
    fun testBgtz() = testInstr({ bgtz(a0, testLabel()) }, { verify(Bgtz, GprReg.A0, labelExpected) })

    @Test
    fun testBgtzl() = testInstr({ bgtzl(a0, testLabel()) }, { verify(Bgtzl, GprReg.A0, labelExpected) })

    @Test
    fun testBlez() = testInstr({ blez(a0, testLabel()) }, { verify(Blez, GprReg.A0, labelExpected) })

    @Test
    fun testBlezl() = testInstr({ blezl(a0, testLabel()) }, { verify(Blezl, GprReg.A0, labelExpected) })

    @Test
    fun testBltz() = testInstr({ bltz(a0, testLabel()) }, { verify(Bltz, GprReg.A0, labelExpected) })

    @Test
    fun testBltzal() = testInstr({ bltzal(a0, testLabel()) }, { verify(Bltzal, GprReg.A0, labelExpected) })

    @Test
    fun testBltzall() = testInstr({ bltzall(a0, testLabel()) }, { verify(Bltzall, GprReg.A0, labelExpected) })

    @Test
    fun testBltzl() = testInstr({ bltzl(a0, testLabel()) }, { verify(Bltzl, GprReg.A0, labelExpected) })

    @Test
    fun testBne() = testInstr({ bne(a0, s0, testLabel()) }, { verify(Bne, GprReg.A0, GprReg.S0, labelExpected) })

    @Test
    fun testBnel() = testInstr({ bnel(a0, s0, testLabel()) }, { verify(Bnel, GprReg.A0, GprReg.S0, labelExpected) })

    @Test
    fun testBreak() = testInstr({ `break`(0xCD) }, { verify(Break, 0xCD) })

    @Test
    fun testDiv() = testInstr({ div(a0, s0) }, { verify(Div, GprReg.A0, GprReg.S0) })

    @Test
    fun testDivu() = testInstr({ divu(a0, s0) }, { verify(Divu, GprReg.A0, GprReg.S0) })

    @Test
    fun testJ() = testInstr({ j(0x40) }, { verify(J, 0x40) })

    @Test
    fun testJal() = testInstr({ jal(0x40) }, { verify(Jal, 0x40) })

    @Test
    fun testJalr() = testInstr({ jalr(a0) }, { verify(Jalr, GprReg.Ra, GprReg.A0) })

    @Test
    fun testJr() = testInstr({ jr(a0) }, { verify(Jr, GprReg.A0) })

    @Test
    fun testLb() = testInstr({ lb(a0, 0xCD, s0) }, { verify(Lb, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testLbu() = testInstr({ lbu(a0, 0xCD, s0) }, { verify(Lbu, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testLh() = testInstr({ lh(a0, 0xCD, s0) }, { verify(Lh, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testLhu() = testInstr({ lhu(a0, 0xCD, s0) }, { verify(Lhu, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testLl() = testInstr({ ll(a0, 0xCD, s0) }, { verify(Ll, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testLui() = testInstr({ lui(a0, 0xCD) }, { verify(Lui, GprReg.A0, 0xCD) })

    @Test
    fun testLuiZeroExtend() = testInstr({ lui(a0, 0x8000) }, { verify(Lui, GprReg.A0, 0x8000) })

    @Test
    fun testLw() = testInstr({ lw(a0, 0xCD, s0) }, { verify(Lw, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testLwc1() = testInstr({ lwc1(f10, 0xCD, s0) }, { verify(Lwc1, FpuReg.F10, GprReg.S0, 0xCD) })

    @Test
    fun testLwl() = testInstr({ lwl(a0, 0xCD, s0) }, { verify(Lwl, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testLwr() = testInstr({ lwr(a0, 0xCD, s0) }, { verify(Lwr, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testMfhi() = testInstr({ mfhi(a0) }, { verify(Mfhi, GprReg.A0) })

    @Test
    fun testMflo() = testInstr({ mflo(a0) }, { verify(Mflo, GprReg.A0) })

    @Test
    fun testMthi() = testInstr({ mthi(a0) }, { verify(Mthi, GprReg.A0) })

    @Test
    fun testMtlo() = testInstr({ mtlo(a0) }, { verify(Mtlo, GprReg.A0) })

    @Test
    fun testMult() = testInstr({ mult(a0, s0) }, { verify(Mult, GprReg.A0, GprReg.S0) })

    @Test
    fun testMultu() = testInstr({ multu(a0, s0) }, { verify(Multu, GprReg.A0, GprReg.S0) })

    @Test
    fun testNor() = testInstr({ nor(a0, s0, t0) }, { verify(Nor, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testOr() = testInstr({ or(a0, s0, t0) }, { verify(Or, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testOri() = testInstr({ ori(a0, s0, 0xCD) }, { verify(Ori, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testOriZeroExtend() = testInstr({ ori(a0, s0, 0x8000) }, { verify(Ori, GprReg.A0, GprReg.S0, 0x8000) })

    @Test
    fun testRotr() = testEncodedInstr(0x00302402) { verify(Rotr, GprReg.A0, GprReg.S0, 0x10) }

    @Test
    fun testRotrv() = testEncodedInstr(0x01102046) { verify(Rotrv, GprReg.A0, GprReg.S0, GprReg.T0) }

    @Test
    fun testSb() = testInstr({ sb(a0, 0xCD, s0) }, { verify(Sb, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testSc() = testInstr({ sc(a0, 0xCD, s0) }, { verify(Sc, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testSh() = testInstr({ sh(a0, 0xCD, s0) }, { verify(Sh, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testSll() = testInstr({ sll(a0, s0, 0x10) }, { verify(Sll, GprReg.A0, GprReg.S0, 0x10) })

    @Test
    fun testSllv() = testInstr({ sllv(a0, s0, t0) }, { verify(Sllv, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testSlt() = testInstr({ slt(a0, s0, t0) }, { verify(Slt, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testSlti() = testInstr({ slti(a0, s0, 0xCD) }, { verify(Slti, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testSltiu() = testInstr({ sltiu(a0, s0, 0xCD) }, { verify(Sltiu, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testSltiuUnsigned() =
        testInstr({ sltiu(a0, s0, 0xFFFF8000.toInt()) }) { verify(Sltiu, GprReg.A0, GprReg.S0, 0xFFFF8000.toInt()) }

    @Test
    fun testSltu() = testInstr({ sltu(a0, s0, t0) }, { verify(Sltu, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testSrl() = testInstr({ srl(a0, s0, 0x10) }, { verify(Srl, GprReg.A0, GprReg.S0, 0x10) })

    @Test
    fun testSrlv() = testInstr({ srlv(a0, s0, t0) }, { verify(Srlv, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testSrlv0xC6() = testEncodedInstr(0x000000C6) { verify(Srlv, GprReg.Zero, GprReg.Zero, GprReg.Zero) }

    @Test
    fun testSra() = testInstr({ sra(a0, s0, 0x10) }, { verify(Sra, GprReg.A0, GprReg.S0, 0x10) })

    @Test
    fun testSrav() = testInstr({ srav(a0, s0, t0) }, { verify(Srav, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testSub() = testInstr({ sub(a0, s0, t0) }, { verify(Sub, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testSubu() = testInstr({ subu(a0, s0, t0) }, { verify(Subu, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testSw() = testInstr({ sw(a0, 0xCD, s0) }, { verify(Sw, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testSwc1() = testInstr({ swc1(f10, 0xCD, s0) }, { verify(Swc1, FpuReg.F10, GprReg.S0, 0xCD) })

    @Test
    fun testSwl() = testInstr({ swl(a0, 0xCD, s0) }, { verify(Swl, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testSwr() = testInstr({ swr(a0, 0xCD, s0) }, { verify(Swr, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testSync() = testInstr({ sync(0x10) }, { verify(Sync, 0x10) })

    @Test
    fun testSyscall() = testInstr({ syscall(0xCD) }, { verify(Syscall, 0xCD) })

    @Test
    fun testTeq() = testInstr({ teq(a0, s0, 0xCD) }, { verify(Teq, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testTeqi() = testInstr({ teqi(a0, 0xCD) }, { verify(Teqi, GprReg.A0, 0xCD) })

    @Test
    fun testTge() = testInstr({ tge(a0, s0, 0xCD) }, { verify(Tge, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testTgei() = testInstr({ tgei(a0, 0xCD) }, { verify(Tgei, GprReg.A0, 0xCD) })

    @Test
    fun testTgeiu() = testInstr({ tgeiu(a0, 0xCD) }, { verify(Tgeiu, GprReg.A0, 0xCD) })

    @Test
    fun testTgeu() = testInstr({ tgeu(a0, s0, 0xCD) }, { verify(Tgeu, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testTlt() = testInstr({ tlt(a0, s0, 0xCD) }, { verify(Tlt, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testTlti() = testInstr({ tlti(a0, 0xCD) }, { verify(Tlti, GprReg.A0, 0xCD) })

    @Test
    fun testTltiu() = testInstr({ tltiu(a0, 0xCD) }, { verify(Tltiu, GprReg.A0, 0xCD) })

    @Test
    fun testTltu() = testInstr({ tltu(a0, s0, 0xCD) }, { verify(Tltu, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testTne() = testInstr({ tne(a0, s0, 0xCD) }, { verify(Tne, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testTnei() = testInstr({ tnei(a0, 0xCD) }, { verify(Tnei, GprReg.A0, 0xCD) })

    @Test
    fun testXor() = testInstr({ xor(a0, s0, t0) }, { verify(Xor, GprReg.A0, GprReg.S0, GprReg.T0) })

    @Test
    fun testXori() = testInstr({ xori(a0, s0, 0xCD) }, { verify(Xori, GprReg.A0, GprReg.S0, 0xCD) })

    @Test
    fun testXoriZeroExtend() = testInstr({ xori(a0, s0, 0x8000) }, { verify(Xori, GprReg.A0, GprReg.S0, 0x8000) })

    @Test
    fun testNop() = testInstr({ nop() }, { verify(Nop) })

    @Test
    fun testFpuAbsS() = testInstr({ abs.s(f4, f14) }, { verify(FpuAbsS, FpuReg.F4, FpuReg.F14) })

    @Test
    fun testFpuAddS() = testInstr({ add.s(f4, f14, f24) }, { verify(FpuAddS, FpuReg.F4, FpuReg.F14, FpuReg.F24) })

    @Test
    fun testFpuFpuBc1f() = testInstr({ bc1f(testLabel()) }, { verify(FpuBc1f, labelExpected) })

    @Test
    fun testFpuFpuBc1fl() = testInstr({ bc1fl(testLabel()) }, { verify(FpuBc1fl, labelExpected) })

    @Test
    fun testFpuFpuBc1t() = testInstr({ bc1t(testLabel()) }, { verify(FpuBc1t, labelExpected) })

    @Test
    fun testFpuFpuBc1tl() = testInstr({ bc1tl(testLabel()) }, { verify(FpuBc1tl, labelExpected) })

    @Test
    fun testFpuCFS() = testCond(false, 0, 0b0000, FpuCFS)

    @Test
    fun testFpuCUnS() = testCond(false, 0, 0b0001, FpuCUnS)

    @Test
    fun testFpuCEqS() = testCond(false, 0, 0b0010, FpuCEqS)

    @Test
    fun testFpuCUeqS() = testCond(false, 0, 0b0011, FpuCUeqS)

    @Test
    fun testFpuCOltS() = testCond(false, 0, 0b0100, FpuCOltS)

    @Test
    fun testFpuCUltS() = testCond(false, 0, 0b0101, FpuCUltS)

    @Test
    fun testFpuCOleS() = testCond(false, 0, 0b0110, FpuCOleS)

    @Test
    fun testFpuCUleS() = testCond(false, 0, 0b0111, FpuCUleS)

    @Test
    fun testFpuCSfS() = testCond(false, 0, 0b1000, FpuCSfS)

    @Test
    fun testFpuCNgleS() = testCond(false, 0, 0b1001, FpuCNgleS)

    @Test
    fun testFpuCSeqS() = testCond(false, 0, 0b1010, FpuCSeqS)

    @Test
    fun testFpuCNglS() = testCond(false, 0, 0b1011, FpuCNglS)

    @Test
    fun testFpuCLtS() = testCond(false, 0, 0b1100, FpuCLtS)

    @Test
    fun testFpuCNgeS() = testCond(false, 0, 0b1101, FpuCNgeS)

    @Test
    fun testFpuCLeS() = testCond(false, 0, 0b1110, FpuCLeS)

    @Test
    fun testFpuCNgtS() = testCond(false, 0, 0b1111, FpuCNgtS)

    @Test
    fun testFpuCeilWS() = testInstr({ ceil.w.s(f4, f14) }, { verify(FpuCeilWS, FpuReg.F4, FpuReg.F14) })

    @Test
    fun testCfc1() = testInstr({ cfc1(s0, f10) }, { verify(FpuCfc1, GprReg.S0, FpuReg.F10) })

    @Test
    fun testCtc1() = testInstr({ ctc1(s0, f10) }, { verify(FpuCtc1, GprReg.S0, FpuReg.F10) })

    @Test
    fun testFpuFpuCvtSW() = testInstr({ cvt.s.w(f4, f14) }, { verify(FpuCvtSW, FpuReg.F4, FpuReg.F14) })

    @Test
    fun testFpuFpuCvtWS() = testInstr({ cvt.w.s(f4, f14) }, { verify(FpuCvtWS, FpuReg.F4, FpuReg.F14) })

    @Test
    fun testFpuDivS() = testInstr({ div.s(f4, f14, f24) }, { verify(FpuDivS, FpuReg.F4, FpuReg.F14, FpuReg.F24) })

    @Test
    fun testFpuFloorWS() = testInstr({ floor.w.s(f4, f14) }, { verify(FpuFloorWS, FpuReg.F4, FpuReg.F14) })

    @Test
    fun testMfc1() = testInstr({ mfc1(s0, f10) }, { verify(FpuMfc1, GprReg.S0, FpuReg.F10) })

    @Test
    fun testFpuFpuMovS() = testInstr({ mov.s(f4, f14) }, { verify(FpuMovS, FpuReg.F4, FpuReg.F14) })

    @Test
    fun testMtc1() = testInstr({ mtc1(s0, f10) }, { verify(FpuMtc1, GprReg.S0, FpuReg.F10) })

    @Test
    fun testFpuMulS() = testInstr({ mul.s(f4, f14, f24) }, { verify(FpuMulS, FpuReg.F4, FpuReg.F14, FpuReg.F24) })

    @Test
    fun testFpuNegS() = testInstr({ neg.s(f4, f14) }, { verify(FpuNegS, FpuReg.F4, FpuReg.F14) })

    @Test
    fun testFpuRoundWS() = testInstr({ round.w.s(f4, f14) }, { verify(FpuRoundWS, FpuReg.F4, FpuReg.F14) })

    @Test
    fun testFpuSqrtS() = testInstr({ sqrt.s(f4, f14) }, { verify(FpuSqrtS, FpuReg.F4, FpuReg.F14) })

    @Test
    fun testFpuSubS() = testInstr({ sub.s(f4, f14, f24) }, { verify(FpuSubS, FpuReg.F4, FpuReg.F14, FpuReg.F24) })

    @Test
    fun testFpuTruncWS() = testInstr({ trunc.w.s(f4, f14) }, { verify(FpuTruncWS, FpuReg.F4, FpuReg.F14) })

    @Test
    fun testClo() = testEncodedInstr(0x2002017) { verify(Clo, GprReg.A0, GprReg.S0) }

    @Test
    fun testClz() = testEncodedInstr(0x2002016) { verify(Clz, GprReg.A0, GprReg.S0) }

    @Test
    fun testMax() = testEncodedInstr(0x208202C) { verify(Max, GprReg.A0, GprReg.S0, GprReg.T0) }

    @Test
    fun testMin() = testEncodedInstr(0x208202D) { verify(Min, GprReg.A0, GprReg.S0, GprReg.T0) }

    @Test
    fun testMadd() = testEncodedInstr(0x208001C) { verify(Madd, GprReg.S0, GprReg.T0) }

    @Test
    fun testMaddu() = testEncodedInstr(0x208001D) { verify(Maddu, GprReg.S0, GprReg.T0) }

    @Test
    fun testMsub() = testEncodedInstr(0x208002E) { verify(Msub, GprReg.S0, GprReg.T0) }

    @Test
    fun testMsubu() = testEncodedInstr(0x208002F) { verify(Msubu, GprReg.S0, GprReg.T0) }

    @Test
    fun testSynci() = testEncodedInstr(0x61F00CD) { verify(Synci, GprReg.S0, 0xCD) }

    @Test
    fun testDi() = testEncodedInstr(0x41706000) { verify(Di, GprReg.S0) }

    @Test
    fun testEi() = testEncodedInstr(0x41706020) { verify(Ei, GprReg.S0) }

    @Test
    fun testMfc0() = testEncodedInstr(0x40104001) { verify(Mfc0, GprReg.S0, GprReg.T0, 1) }

    @Test
    fun testMtc0() = testEncodedInstr(0x40904001) { verify(Mtc0, GprReg.S0, GprReg.T0, 1) }

    @Test
    fun testRdpgpr() = testEncodedInstr(0x41504000) { verify(Rdpgpr, GprReg.S0, GprReg.T0) }

    @Test
    fun testWrpgpr() = testEncodedInstr(0x41D04000) { verify(Wrpgpr, GprReg.S0, GprReg.T0) }

    @Test
    fun testTlbp() = testEncodedInstr(0x42000008) { verify(Tlbp) }

    @Test
    fun testTlbr() = testEncodedInstr(0x42000001) { verify(Tlbr) }

    @Test
    fun testTlbwi() = testEncodedInstr(0x42000002) { verify(Tlbwi) }

    @Test
    fun testTlbwr() = testEncodedInstr(0x42000006) { verify(Tlbwr) }

    @Test
    fun testDeret() = testEncodedInstr(0x4200001F) { verify(Deret) }

    @Test
    fun testEret() = testEncodedInstr(0x42000018) { verify(Eret) }

    @Test
    fun testWait() = testEncodedInstr(0x42000020) { verify(Wait) }

    private fun testLabel() = Label().apply { address = labelExpected }

    private fun Instr.verify(opcode: Opcode, op0: Any? = null, op1: Any? = null, op2: Any? = null, op3: Any? = null) {
        assertThat(this.opcode).isEqualTo(opcode)
        if (opcode !in arrayOf(Madd, Maddu, Msub, Msubu)) {
            assertThat(this.opcode.use.intersect(opcode.modify.asIterable())).isEmpty()
        }
        val combinedRegisters = opcode.use.union(opcode.modify.asIterable()).toMutableList()
        val ops = arrayOf(op0, op1, op2, op3).mapIndexed { index, op -> Pair(this.operands.getOrNull(index), op) }
        ops.forEachIndexed { index, it ->
            when {
                it.second == null -> assertThat(it.first).isNull()
                it.second is GprReg || it.second is FpuReg || it.second is Cop2Reg || it.second is Cop3Reg -> {
                    val reg = (it.first as RegOperand).reg
                    assertThat(it.second).isEqualTo(reg)
                    combinedRegisters.removeIf { it is OperandIdxRef && it.idx == index }
                }
                it.second is Int -> assertThat(it.second as Int).isEqualTo((it.first as ImmOperand).value)
                else -> error("don't know how to verify $it, register or immediate expected")
            }
        }
        combinedRegisters.remove(OperandRegRef(GprReg.Ra))
        combinedRegisters.remove(OperandRegRef(GprReg.Pc))
        combinedRegisters.remove(OperandRegRef(GprReg.Lo))
        combinedRegisters.remove(OperandRegRef(GprReg.Hi))
        combinedRegisters.remove(OperandRegRef(FpuReg.Cc0))
        assertThat(combinedRegisters).isEmpty()
    }

    private fun testCond(useDFmt: Boolean, cc: Int, cond: Int, expected: MipsOpcode) {
        val fmt = if (useDFmt) 17 else 16
        val codedInstr = (0b010001 shl 26) or (fmt shl 21) or (FpuReg.F14.id shl 16) or (FpuReg.F4.id shl 11) or
                (cc and 0b111 shl 8) or (0b0011 shl 4) or (cond and 0b1111)
        testEncodedInstr(codedInstr) {
            verify(expected, FpuReg.F4, FpuReg.F14)
        }
    }

    private fun testEncodedInstr(
        instr: Long,
        checkResult: Instr.() -> Unit
    ) {
        if (instr and 0xFFFFFFFFL != instr) error("too big coded instr value")
        testEncodedInstr(instr.toInt(), checkResult)
    }

    private fun testEncodedInstr(
        instr: Int,
        checkResult: Instr.() -> Unit
    ) {
        val result = AllegrexDisassembler().disassemble(
            MemBinLoader(ByteBuffer.allocate(4).putInt(instr.swapBytes()).array()),
            FunctionDef("UnitTest", 0, 4)
        )
        assertThat(result.instr).isNotEmpty()
        checkResult(result.instr[0])
    }

    private fun testInstr(
        assemble: Assembler.() -> Unit,
        checkResult: Instr.() -> Unit
    ) {
        val bytes = assembleAsByteArray { assemble(this) }
        val result = AllegrexDisassembler().disassemble(
            MemBinLoader(bytes),
            FunctionDef("UnitTest", 0, bytes.size)
        )
        assertThat(result.instr).isNotEmpty()
        checkResult(result.instr[0])
    }
}
