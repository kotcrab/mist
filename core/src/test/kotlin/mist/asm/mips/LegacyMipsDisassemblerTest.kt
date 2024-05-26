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

package mist.asm.mips

import kio.util.swapBytes
import kmips.Assembler
import kmips.FpuReg.*
import kmips.Label
import kmips.Reg.*
import kmips.assembleAsByteArray
import mist.asm.*
import mist.asm.mips.MipsOpcode.*
import mist.test.util.MemBinLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/** @author Kotcrab */

class LegacyMipsDisassemblerTest {
  private val labelExpected = 0x40
  private val labelEncoded = labelExpected / 0x4 - 1

  @Test
  fun testAdd() = testInstr({ add(a0, s0, t0) }) { verify(Add, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testAddi() = testInstr({ addi(a0, s0, 0xCD) }) { verify(Addi, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testAddiu() = testInstr({ addiu(a0, s0, 0xCD) }) { verify(Addiu, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testAddu() = testInstr({ addu(a0, s0, t0) }) { verify(Addu, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testAnd() = testInstr({ and(a0, s0, t0) }) { verify(And, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testAndi() = testInstr({ andi(a0, s0, 0xCD) }) { verify(Andi, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testAndiZeroExtend() = testInstr({ andi(a0, s0, 0x8000) }) { verify(Andi, GprReg.A0, GprReg.S0, 0x8000) }

  @Test
  fun testBeq() = testInstr({ beq(a0, s0, testLabel()) }) { verify(Beq, GprReg.A0, GprReg.S0, labelExpected) }

  @Test
  fun testBeql() = testInstr({ beql(a0, s0, testLabel()) }) { verify(Beql, GprReg.A0, GprReg.S0, labelExpected) }

  @Test
  fun testBgez() = testInstr({ bgez(a0, testLabel()) }) { verify(Bgez, GprReg.A0, labelExpected) }

  @Test
  fun testBgezal() = testInstr({ bgezal(a0, testLabel()) }) { verify(Bgezal, GprReg.A0, labelExpected) }

  @Test
  fun testBgezall() = testInstr({ bgezall(a0, testLabel()) }) { verify(Bgezall, GprReg.A0, labelExpected) }

  @Test
  fun testBgezl() = testInstr({ bgezl(a0, testLabel()) }) { verify(Bgezl, GprReg.A0, labelExpected) }

  @Test
  fun testBgtz() = testInstr({ bgtz(a0, testLabel()) }) { verify(Bgtz, GprReg.A0, labelExpected) }

  @Test
  fun testBgtzl() = testInstr({ bgtzl(a0, testLabel()) }) { verify(Bgtzl, GprReg.A0, labelExpected) }

  @Test
  fun testBlez() = testInstr({ blez(a0, testLabel()) }) { verify(Blez, GprReg.A0, labelExpected) }

  @Test
  fun testBlezl() = testInstr({ blezl(a0, testLabel()) }) { verify(Blezl, GprReg.A0, labelExpected) }

  @Test
  fun testBltz() = testInstr({ bltz(a0, testLabel()) }) { verify(Bltz, GprReg.A0, labelExpected) }

  @Test
  fun testBltzal() = testInstr({ bltzal(a0, testLabel()) }) { verify(Bltzal, GprReg.A0, labelExpected) }

  @Test
  fun testBltzall() = testInstr({ bltzall(a0, testLabel()) }) { verify(Bltzall, GprReg.A0, labelExpected) }

  @Test
  fun testBltzl() = testInstr({ bltzl(a0, testLabel()) }) { verify(Bltzl, GprReg.A0, labelExpected) }

  @Test
  fun testBne() = testInstr({ bne(a0, s0, testLabel()) }) { verify(Bne, GprReg.A0, GprReg.S0, labelExpected) }

  @Test
  fun testBnel() = testInstr({ bnel(a0, s0, testLabel()) }) { verify(Bnel, GprReg.A0, GprReg.S0, labelExpected) }

  @Test
  fun testBreak() = testInstr({ `break`(0xCD) }) { verify(Break, 0xCD) }

  @Test
  fun testDiv() = testInstr({ div(a0, s0) }) { verify(Div, GprReg.A0, GprReg.S0) }

  @Test
  fun testDivu() = testInstr({ divu(a0, s0) }) { verify(Divu, GprReg.A0, GprReg.S0) }

  @Test
  fun testJ() = testInstr({ j(0x40) }) { verify(J, 0x40) }

  @Test
  fun testJal() = testInstr({ jal(0x40) }) { verify(Jal, 0x40) }

  @Test
  fun testJalr() = testInstr({ jalr(a0) }) { verify(Jalr, GprReg.Ra, GprReg.A0) }

  @Test
  fun testJr() = testInstr({ jr(a0) }) { verify(Jr, GprReg.A0) }

  @Test
  fun testLb() = testInstr({ lb(a0, 0xCD, s0) }) { verify(Lb, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testLbu() = testInstr({ lbu(a0, 0xCD, s0) }) { verify(Lbu, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testLdc1() = testEncodedInstr(0xD60400CD) { verify(Ldc1, FpuReg.F4, GprReg.S0, 0xCD) }

  @Test
  fun testLdc2() = testEncodedInstr(0xDA0400CD) { verify(Ldc2, Cop2Reg.Cop2r4, GprReg.S0, 0xCD) }

  @Test
  fun testLdc3() = testEncodedInstr(0xDE0400CD, MipsIIProcessor) { verify(Ldc3, Cop3Reg.Cop3r4, GprReg.S0, 0xCD) }

  @Test
  fun testLh() = testInstr({ lh(a0, 0xCD, s0) }) { verify(Lh, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testLhu() = testInstr({ lhu(a0, 0xCD, s0) }) { verify(Lhu, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testLl() = testInstr({ ll(a0, 0xCD, s0) }) { verify(Ll, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testLui() = testInstr({ lui(a0, 0xCD) }) { verify(Lui, GprReg.A0, 0xCD) }

  @Test
  fun testLuiZeroExtend() = testInstr({ lui(a0, 0x8000) }) { verify(Lui, GprReg.A0, 0x8000) }

  @Test
  fun testLw() = testInstr({ lw(a0, 0xCD, s0) }) { verify(Lw, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testLwc1() = testInstr({ lwc1(f10, 0xCD, s0) }) { verify(Lwc1, FpuReg.F10, GprReg.S0, 0xCD) }

  @Test
  fun testLwc2() = testEncodedInstr(0xCA0400CD) { verify(Lwc2, Cop2Reg.Cop2r4, GprReg.S0, 0xCD) }

  @Test
  fun testLwc3() = testEncodedInstr(0xCE0400CD, MipsIIProcessor) { verify(Lwc3, Cop3Reg.Cop3r4, GprReg.S0, 0xCD) }

  @Test
  fun testLwl() = testInstr({ lwl(a0, 0xCD, s0) }) { verify(Lwl, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testLwr() = testInstr({ lwr(a0, 0xCD, s0) }) { verify(Lwr, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testMfhi() = testInstr({ mfhi(a0) }) { verify(Mfhi, GprReg.A0) }

  @Test
  fun testMflo() = testInstr({ mflo(a0) }) { verify(Mflo, GprReg.A0) }

  @Test
  fun testMovn() = testEncodedInstr(0x208200B) { verify(Movn, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testMovz() = testEncodedInstr(0x208200A) { verify(Movz, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testMthi() = testInstr({ mthi(a0) }) { verify(Mthi, GprReg.A0) }

  @Test
  fun testMtlo() = testInstr({ mtlo(a0) }) { verify(Mtlo, GprReg.A0) }

  @Test
  fun testMult() = testInstr({ mult(a0, s0) }) { verify(Mult, GprReg.A0, GprReg.S0) }

  @Test
  fun testMultu() = testInstr({ multu(a0, s0) }) { verify(Multu, GprReg.A0, GprReg.S0) }

  @Test
  fun testNor() = testInstr({ nor(a0, s0, t0) }) { verify(Nor, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testOr() = testInstr({ or(a0, s0, t0) }) { verify(Or, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testOri() = testInstr({ ori(a0, s0, 0xCD) }) { verify(Ori, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testOriZeroExtend() = testInstr({ ori(a0, s0, 0x8000) }) { verify(Ori, GprReg.A0, GprReg.S0, 0x8000) }

  @Test
  fun testPref() = testEncodedInstr(0xCE0100CD) { verify(Pref, 1, GprReg.S0, 0xCD) }

  @Test
  fun testSb() = testInstr({ sb(a0, 0xCD, s0) }) { verify(Sb, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testSc() = testInstr({ sc(a0, 0xCD, s0) }) { verify(Sc, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testSdc1() = testEncodedInstr(0xF60400CD) { verify(Sdc1, FpuReg.F4, GprReg.S0, 0xCD) }

  @Test
  fun testSdc2() = testEncodedInstr(0xFA0400CD) { verify(Sdc2, Cop2Reg.Cop2r4, GprReg.S0, 0xCD) }

  @Test
  fun testSdc3() = testEncodedInstr(0xFE0400CD, MipsIIProcessor) { verify(Sdc3, Cop3Reg.Cop3r4, GprReg.S0, 0xCD) }

  @Test
  fun testSh() = testInstr({ sh(a0, 0xCD, s0) }) { verify(Sh, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testSll() = testInstr({ sll(a0, s0, 0x10) }) { verify(Sll, GprReg.A0, GprReg.S0, 0x10) }

  @Test
  fun testSllv() = testInstr({ sllv(a0, s0, t0) }) { verify(Sllv, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testSlt() = testInstr({ slt(a0, s0, t0) }) { verify(Slt, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testSlti() = testInstr({ slti(a0, s0, 0xCD) }) { verify(Slti, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testSltiu() = testInstr({ sltiu(a0, s0, 0xCD) }) { verify(Sltiu, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testSltiuUnsigned() =
    testInstr({ sltiu(a0, s0, 0xFFFF8000.toInt()) }) { verify(Sltiu, GprReg.A0, GprReg.S0, 0xFFFF8000.toInt()) }

  @Test
  fun testSltu() = testInstr({ sltu(a0, s0, t0) }) { verify(Sltu, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testSrl() = testInstr({ srl(a0, s0, 0x10) }) { verify(Srl, GprReg.A0, GprReg.S0, 0x10) }

  @Test
  fun testSrlv() = testInstr({ srlv(a0, s0, t0) }) { verify(Srlv, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testSra() = testInstr({ sra(a0, s0, 0x10) }) { verify(Sra, GprReg.A0, GprReg.S0, 0x10) }

  @Test
  fun testSrav() = testInstr({ srav(a0, s0, t0) }) { verify(Srav, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testSub() = testInstr({ sub(a0, s0, t0) }) { verify(Sub, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testSubu() = testInstr({ subu(a0, s0, t0) }) { verify(Subu, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testSw() = testInstr({ sw(a0, 0xCD, s0) }) { verify(Sw, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testSwc1() = testInstr({ swc1(f10, 0xCD, s0) }) { verify(Swc1, FpuReg.F10, GprReg.S0, 0xCD) }

  @Test
  fun testSwc2() = testEncodedInstr(0xEA0400CD) { verify(Swc2, Cop2Reg.Cop2r4, GprReg.S0, 0xCD) }

  @Test
  fun testSwc3() = testEncodedInstr(0xEE0400CD, MipsIIProcessor) { verify(Swc3, Cop3Reg.Cop3r4, GprReg.S0, 0xCD) }

  @Test
  fun testSwl() = testInstr({ swl(a0, 0xCD, s0) }) { verify(Swl, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testSwr() = testInstr({ swr(a0, 0xCD, s0) }) { verify(Swr, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testSync() = testInstr({ sync(0x10) }) { verify(Sync, 0x10) }

  @Test
  fun testSyscall() = testInstr({ syscall(0xCD) }) { verify(Syscall) }

  @Test
  fun testTeq() = testInstr({ teq(a0, s0, 0xCD) }) { verify(Teq, GprReg.A0, GprReg.S0) }

  @Test
  fun testTeqi() = testInstr({ teqi(a0, 0xCD) }) { verify(Teqi, GprReg.A0, 0xCD) }

  @Test
  fun testTge() = testInstr({ tge(a0, s0, 0xCD) }) { verify(Tge, GprReg.A0, GprReg.S0) }

  @Test
  fun testTgei() = testInstr({ tgei(a0, 0xCD) }) { verify(Tgei, GprReg.A0, 0xCD) }

  @Test
  fun testTgeiu() = testInstr({ tgeiu(a0, 0xCD) }) { verify(Tgeiu, GprReg.A0, 0xCD) }

  @Test
  fun testTgeu() = testInstr({ tgeu(a0, s0, 0xCD) }) { verify(Tgeu, GprReg.A0, GprReg.S0) }

  @Test
  fun testTlt() = testInstr({ tlt(a0, s0, 0xCD) }) { verify(Tlt, GprReg.A0, GprReg.S0) }

  @Test
  fun testTlti() = testInstr({ tlti(a0, 0xCD) }) { verify(Tlti, GprReg.A0, 0xCD) }

  @Test
  fun testTltiu() = testInstr({ tltiu(a0, 0xCD) }) { verify(Tltiu, GprReg.A0, 0xCD) }

  @Test
  fun testTltu() = testInstr({ tltu(a0, s0, 0xCD) }) { verify(Tltu, GprReg.A0, GprReg.S0) }

  @Test
  fun testTne() = testInstr({ tne(a0, s0, 0xCD) }) { verify(Tne, GprReg.A0, GprReg.S0) }

  @Test
  fun testTnei() = testInstr({ tnei(a0, 0xCD) }) { verify(Tnei, GprReg.A0, 0xCD) }

  @Test
  fun testXor() = testInstr({ xor(a0, s0, t0) }) { verify(Xor, GprReg.A0, GprReg.S0, GprReg.T0) }

  @Test
  fun testXori() = testInstr({ xori(a0, s0, 0xCD) }) { verify(Xori, GprReg.A0, GprReg.S0, 0xCD) }

  @Test
  fun testXoriZeroExtend() = testInstr({ xori(a0, s0, 0x8000) }) { verify(Xori, GprReg.A0, GprReg.S0, 0x8000) }

  @Test
  fun testNop() = testInstr({ nop() }) { verify(Nop) }

  @Test
  fun testFpuAbsS() = testInstr({ abs.s(f4, f14) }) { verify(FpuAbsS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuAbsD() = testInstr({ abs.d(f4, f14) }) { verify(FpuAbsD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuAddS() = testInstr({ add.s(f4, f14, f24) }) { verify(FpuAddS, FpuReg.F4, FpuReg.F14, FpuReg.F24) }

  @Test
  fun testFpuAddD() = testInstr({ add.d(f4, f14, f24) }) { verify(FpuAddD, FpuReg.F4, FpuReg.F14, FpuReg.F24) }

  @Test
  fun testFpuFpuBc1f() = testInstr({ bc1f(testLabel()) }, MipsIIIProcessor) { verify(FpuBc1f, labelExpected) }

  @Test
  fun testFpuBc1fCcAny() =
    testEncodedInstr(0x450C0000 + labelEncoded) { verify(FpuBc1fCcAny, FpuReg.Cc3, labelExpected) }

  @Test
  fun testFpuBc1fl() = testInstr({ bc1fl(testLabel()) }, MipsIIIProcessor) { verify(FpuBc1fl, labelExpected) }

  @Test
  fun testFpuBc1flCcAny() =
    testEncodedInstr(0x450E0000 + labelEncoded) { verify(FpuBc1flCcAny, FpuReg.Cc3, labelExpected) }


  @Test
  fun testFpuBc1t() = testInstr({ bc1t(testLabel()) }, MipsIIIProcessor) { verify(FpuBc1t, labelExpected) }

  @Test
  fun testFpuBc1tCcAny() =
    testEncodedInstr(0x450D0000 + labelEncoded) { verify(FpuBc1tCcAny, FpuReg.Cc3, labelExpected) }

  @Test
  fun testFpuBc1tl() = testInstr({ bc1tl(testLabel()) }, MipsIIIProcessor) { verify(FpuBc1tl, labelExpected) }

  @Test
  fun testFpuBc1tlCcAny() =
    testEncodedInstr(0x450F0000 + labelEncoded) { verify(FpuBc1tlCcAny, FpuReg.Cc3, labelExpected) }

  @Test
  fun testFpuCFSCcAny() = testCond(false, 3, 0b0000, FpuCFSCcAny)

  @Test
  fun testFpuCUnSCcAny() = testCond(false, 3, 0b0001, FpuCUnSCcAny)

  @Test
  fun testFpuCEqSCcAny() = testCond(false, 3, 0b0010, FpuCEqSCcAny)

  @Test
  fun testFpuCUeqSCcAny() = testCond(false, 3, 0b0011, FpuCUeqSCcAny)

  @Test
  fun testFpuCOltSCcAny() = testCond(false, 3, 0b0100, FpuCOltSCcAny)

  @Test
  fun testFpuCUltSCcAny() = testCond(false, 3, 0b0101, FpuCUltSCcAny)

  @Test
  fun testFpuCOleSCcAny() = testCond(false, 3, 0b0110, FpuCOleSCcAny)

  @Test
  fun testFpuCUleSCcAny() = testCond(false, 3, 0b0111, FpuCUleSCcAny)

  @Test
  fun testFpuCSfSCcAny() = testCond(false, 3, 0b1000, FpuCSfSCcAny)

  @Test
  fun testFpuCNgleSCcAny() = testCond(false, 3, 0b1001, FpuCNgleSCcAny)

  @Test
  fun testFpuCSeqSCcAny() = testCond(false, 3, 0b1010, FpuCSeqSCcAny)

  @Test
  fun testFpuCNglSCcAny() = testCond(false, 3, 0b1011, FpuCNglSCcAny)

  @Test
  fun testFpuCLtSCcAny() = testCond(false, 3, 0b1100, FpuCLtSCcAny)

  @Test
  fun testFpuCNgeSCcAny() = testCond(false, 3, 0b1101, FpuCNgeSCcAny)

  @Test
  fun testFpuCLeSCcAny() = testCond(false, 3, 0b1110, FpuCLeSCcAny)

  @Test
  fun testFpuCNgtSCcAny() = testCond(false, 3, 0b1111, FpuCNgtSCcAny)

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
  fun testFpuCFDCcAny() = testCond(true, 3, 0b0000, FpuCFDCcAny)

  @Test
  fun testFpuCUnDCcAny() = testCond(true, 3, 0b0001, FpuCUnDCcAny)

  @Test
  fun testFpuCEqDCcAny() = testCond(true, 3, 0b0010, FpuCEqDCcAny)

  @Test
  fun testFpuCUeqDCcAny() = testCond(true, 3, 0b0011, FpuCUeqDCcAny)

  @Test
  fun testFpuCOltDCcAny() = testCond(true, 3, 0b0100, FpuCOltDCcAny)

  @Test
  fun testFpuCUltDCcAny() = testCond(true, 3, 0b0101, FpuCUltDCcAny)

  @Test
  fun testFpuCOleDCcAny() = testCond(true, 3, 0b0110, FpuCOleDCcAny)

  @Test
  fun testFpuCUleDCcAny() = testCond(true, 3, 0b0111, FpuCUleDCcAny)

  @Test
  fun testFpuCSfDCcAny() = testCond(true, 3, 0b1000, FpuCSfDCcAny)

  @Test
  fun testFpuCNgleDCcAny() = testCond(true, 3, 0b1001, FpuCNgleDCcAny)

  @Test
  fun testFpuCSeqDCcAny() = testCond(true, 3, 0b1010, FpuCSeqDCcAny)

  @Test
  fun testFpuCNglDCcAny() = testCond(true, 3, 0b1011, FpuCNglDCcAny)

  @Test
  fun testFpuCLtDCcAny() = testCond(true, 3, 0b1100, FpuCLtDCcAny)

  @Test
  fun testFpuCNgeDCcAny() = testCond(true, 3, 0b1101, FpuCNgeDCcAny)

  @Test
  fun testFpuCLeDCcAny() = testCond(true, 3, 0b1110, FpuCLeDCcAny)

  @Test
  fun testFpuCNgtDCcAny() = testCond(true, 3, 0b1111, FpuCNgtDCcAny)

  @Test
  fun testFpuCFD() = testCond(true, 0, 0b0000, FpuCFD)

  @Test
  fun testFpuCUnD() = testCond(true, 0, 0b0001, FpuCUnD)

  @Test
  fun testFpuCEqD() = testCond(true, 0, 0b0010, FpuCEqD)

  @Test
  fun testFpuCUeqD() = testCond(true, 0, 0b0011, FpuCUeqD)

  @Test
  fun testFpuCOltD() = testCond(true, 0, 0b0100, FpuCOltD)

  @Test
  fun testFpuCUltD() = testCond(true, 0, 0b0101, FpuCUltD)

  @Test
  fun testFpuCOleD() = testCond(true, 0, 0b0110, FpuCOleD)

  @Test
  fun testFpuCUleD() = testCond(true, 0, 0b0111, FpuCUleD)

  @Test
  fun testFpuCSfD() = testCond(true, 0, 0b1000, FpuCSfD)

  @Test
  fun testFpuCNgleD() = testCond(true, 0, 0b1001, FpuCNgleD)

  @Test
  fun testFpuCSeqD() = testCond(true, 0, 0b1010, FpuCSeqD)

  @Test
  fun testFpuCNglD() = testCond(true, 0, 0b1011, FpuCNglD)

  @Test
  fun testFpuCLtD() = testCond(true, 0, 0b1100, FpuCLtD)

  @Test
  fun testFpuCNgeD() = testCond(true, 0, 0b1101, FpuCNgeD)

  @Test
  fun testFpuCLeD() = testCond(true, 0, 0b1110, FpuCLeD)

  @Test
  fun testFpuCNgtD() = testCond(true, 0, 0b1111, FpuCNgtD)

  @Test
  fun testFpuCeilLS() = testEncodedInstr(0x4600710A) { verify(FpuCeilLS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCeilLD() = testEncodedInstr(0x4620710A) { verify(FpuCeilLD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCeilWS() = testInstr({ ceil.w.s(f4, f14) }) { verify(FpuCeilWS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCeilWD() = testInstr({ ceil.w.d(f4, f14) }) { verify(FpuCeilWD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCfc1() = testInstr({ cfc1(s0, f10) }) { verify(FpuCfc1, GprReg.S0, FpuReg.F10) }

  @Test
  fun testFpuCtc1() = testInstr({ ctc1(s0, f10) }) { verify(FpuCtc1, GprReg.S0, FpuReg.F10) }

  @Test
  fun testFpuCvtDS() = testEncodedInstr(0x46007121) { verify(FpuCvtDS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCvtDW() = testEncodedInstr(0x46807121) { verify(FpuCvtDW, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCvtDL() = testEncodedInstr(0x46A07121) { verify(FpuCvtDL, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCvtLS() = testEncodedInstr(0x46007125) { verify(FpuCvtLS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCvtLD() = testEncodedInstr(0x46207125) { verify(FpuCvtLD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCvtSD() = testEncodedInstr(0x46207120) { verify(FpuCvtSD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCvtSW() = testEncodedInstr(0x46807120) { verify(FpuCvtSW, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCvtSL() = testEncodedInstr(0x46A07120) { verify(FpuCvtSL, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCvtWS() = testEncodedInstr(0x46007124) { verify(FpuCvtWS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuCvtWD() = testEncodedInstr(0x46207124) { verify(FpuCvtWD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuDivS() = testInstr({ div.s(f4, f14, f24) }) { verify(FpuDivS, FpuReg.F4, FpuReg.F14, FpuReg.F24) }

  @Test
  fun testFpuDivD() = testInstr({ div.d(f4, f14, f24) }) { verify(FpuDivD, FpuReg.F4, FpuReg.F14, FpuReg.F24) }

  @Test
  fun testFpuFloorLS() = testEncodedInstr(0x4600710B) { verify(FpuFloorLS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuFloorLD() = testEncodedInstr(0x4620710B) { verify(FpuFloorLD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuFloorWS() = testInstr({ floor.w.s(f4, f14) }) { verify(FpuFloorWS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuFloorWD() = testInstr({ floor.w.d(f4, f14) }) { verify(FpuFloorWD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuLdxc1() = testEncodedInstr(0x4E040101) { verify(FpuLdxc1, FpuReg.F4, GprReg.S0, GprReg.A0) }

  @Test
  fun testFpuLwxc1() = testEncodedInstr(0x4E040100) { verify(FpuLwxc1, FpuReg.F4, GprReg.S0, GprReg.A0) }

  @Test
  fun testFpuMaddS() = testEncodedInstr(0x4D106120) { verify(FpuMaddS, FpuReg.F4, FpuReg.F8, FpuReg.F12, FpuReg.F16) }

  @Test
  fun testFpuMaddD() = testEncodedInstr(0x4D106121) { verify(FpuMaddD, FpuReg.F4, FpuReg.F8, FpuReg.F12, FpuReg.F16) }

  @Test
  fun testFpuMfc1() = testInstr({ mfc1(s0, f10) }) { verify(FpuMfc1, GprReg.S0, FpuReg.F10) }

  @Test
  fun testFpuMovS() = testInstr({ mov.s(f4, f14) }) { verify(FpuMovS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuMovD() = testInstr({ mov.d(f4, f14) }) { verify(FpuMovD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuMovf() = testEncodedInstr(0x20C2001) { verify(FpuMovf, GprReg.A0, GprReg.S0, FpuReg.Cc3) }

  @Test
  fun testFpuMovfS() = testEncodedInstr(0x460C4111) { verify(FpuMovfS, FpuReg.F4, FpuReg.F8, FpuReg.Cc3) }

  @Test
  fun testFpuMovfD() = testEncodedInstr(0x462C4111) { verify(FpuMovfD, FpuReg.F4, FpuReg.F8, FpuReg.Cc3) }

  @Test
  fun testFpuMovnS() = testEncodedInstr(0x46104113) { verify(FpuMovnS, FpuReg.F4, FpuReg.F8, GprReg.S0) }

  @Test
  fun testFpuMovnD() = testEncodedInstr(0x46304113) { verify(FpuMovnD, FpuReg.F4, FpuReg.F8, GprReg.S0) }

  @Test
  fun testFpuMovt() = testEncodedInstr(0x20D2001) { verify(FpuMovt, GprReg.A0, GprReg.S0, FpuReg.Cc3) }

  @Test
  fun testFpuMovtS() = testEncodedInstr(0x460D4111) { verify(FpuMovtS, FpuReg.F4, FpuReg.F8, FpuReg.Cc3) }

  @Test
  fun testFpuMovtD() = testEncodedInstr(0x462D4111) { verify(FpuMovtD, FpuReg.F4, FpuReg.F8, FpuReg.Cc3) }

  @Test
  fun testFpuMovzS() = testEncodedInstr(0x46104112) { verify(FpuMovzS, FpuReg.F4, FpuReg.F8, GprReg.S0) }

  @Test
  fun testFpuMovzD() = testEncodedInstr(0x46304112) { verify(FpuMovzD, FpuReg.F4, FpuReg.F8, GprReg.S0) }

  @Test
  fun testFpuMsubS() = testEncodedInstr(0x4D106128) { verify(FpuMsubS, FpuReg.F4, FpuReg.F8, FpuReg.F12, FpuReg.F16) }

  @Test
  fun testFpuMsubD() = testEncodedInstr(0x4D106129) { verify(FpuMsubD, FpuReg.F4, FpuReg.F8, FpuReg.F12, FpuReg.F16) }

  @Test
  fun testFpuMtc1() = testInstr({ mtc1(s0, f10) }) { verify(FpuMtc1, GprReg.S0, FpuReg.F10) }

  @Test
  fun testFpuMulS() = testInstr({ mul.s(f4, f14, f24) }) { verify(FpuMulS, FpuReg.F4, FpuReg.F14, FpuReg.F24) }

  @Test
  fun testFpuMulD() = testInstr({ mul.d(f4, f14, f24) }) { verify(FpuMulD, FpuReg.F4, FpuReg.F14, FpuReg.F24) }

  @Test
  fun testFpuNegS() = testInstr({ neg.s(f4, f14) }) { verify(FpuNegS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuNegD() = testInstr({ neg.d(f4, f14) }) { verify(FpuNegD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuNmaddS() =
    testEncodedInstr(0x4D106130) { verify(FpuNmaddS, FpuReg.F4, FpuReg.F8, FpuReg.F12, FpuReg.F16) }

  @Test
  fun testFpuNmaddD() =
    testEncodedInstr(0x4D106131) { verify(FpuNmaddD, FpuReg.F4, FpuReg.F8, FpuReg.F12, FpuReg.F16) }

  @Test
  fun testFpuNmsubS() =
    testEncodedInstr(0x4D106138) { verify(FpuNmsubS, FpuReg.F4, FpuReg.F8, FpuReg.F12, FpuReg.F16) }

  @Test
  fun testFpuNmsubD() =
    testEncodedInstr(0x4D106139) { verify(FpuNmsubD, FpuReg.F4, FpuReg.F8, FpuReg.F12, FpuReg.F16) }

  @Test
  fun testFpuPrefx() = testEncodedInstr(0x4E04010F) { verify(FpuPrefx, FpuReg.F4, GprReg.S0, GprReg.A0) }

  @Test
  fun testFpuRecipS() = testEncodedInstr(0x46007115) { verify(FpuRecipS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuRecipD() = testEncodedInstr(0x46207115) { verify(FpuRecipD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuRoundLS() = testEncodedInstr(0x46007108) { verify(FpuRoundLS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuRoundLD() = testEncodedInstr(0x46207108) { verify(FpuRoundLD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuRoundWS() = testInstr({ round.w.s(f4, f14) }) { verify(FpuRoundWS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuRoundWD() = testInstr({ round.w.d(f4, f14) }) { verify(FpuRoundWD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuRsqrtS() = testEncodedInstr(0x46007116) { verify(FpuRsqrtS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuRsqrtD() = testEncodedInstr(0x46207116) { verify(FpuRsqrtD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuSdxc1() = testEncodedInstr(0x4E040109) { verify(FpuSdxc1, FpuReg.F4, GprReg.S0, GprReg.A0) }

  @Test
  fun testFpuSqrtS() = testInstr({ sqrt.s(f4, f14) }) { verify(FpuSqrtS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuSqrtD() = testInstr({ sqrt.d(f4, f14) }) { verify(FpuSqrtD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuSubS() = testInstr({ sub.s(f4, f14, f24) }) { verify(FpuSubS, FpuReg.F4, FpuReg.F14, FpuReg.F24) }

  @Test
  fun testFpuSubD() = testInstr({ sub.d(f4, f14, f24) }) { verify(FpuSubD, FpuReg.F4, FpuReg.F14, FpuReg.F24) }

  @Test
  fun testFpuSwxc1() = testEncodedInstr(0x4E040108) { verify(FpuSwxc1, FpuReg.F4, GprReg.S0, GprReg.A0) }

  @Test
  fun testFpuTruncLS() = testEncodedInstr(0x46007109) { verify(FpuTruncLS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuTruncLD() = testEncodedInstr(0x46207109) { verify(FpuTruncLD, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuTruncWS() = testInstr({ trunc.w.s(f4, f14) }) { verify(FpuTruncWS, FpuReg.F4, FpuReg.F14) }

  @Test
  fun testFpuTruncWD() = testInstr({ trunc.w.d(f4, f14) }) { verify(FpuTruncWD, FpuReg.F4, FpuReg.F14) }

  private fun testLabel() = Label().apply { address = labelExpected }

  private fun Instr.verify(opcode: Opcode, op0: Any? = null, op1: Any? = null, op2: Any? = null, op3: Any? = null) {
    assertThat(this.opcode).isEqualTo(opcode)
    assertThat(this.opcode.use.intersect(opcode.modify.asIterable())).isEmpty()
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
    if (expected is FpuCCondFmtCcAny) {
      testEncodedInstr(codedInstr, MipsIVProcessor) {
        verify(expected, FpuReg.ccForId(cc), FpuReg.F4, FpuReg.F14)
      }
    } else {
      testEncodedInstr(codedInstr, MipsIIIProcessor) {
        verify(expected, FpuReg.F4, FpuReg.F14)
      }
    }
  }

  private fun testEncodedInstr(
    instr: Long,
    processor: LegacyMipsProcessor = MipsIVProcessor,
    checkResult: Instr.() -> Unit
  ) {
    if (instr and 0xFFFFFFFFL != instr) error("too big coded instr value")
    testEncodedInstr(instr.toInt(), processor, checkResult)
  }

  private fun testEncodedInstr(
    instr: Int,
    processor: LegacyMipsProcessor = MipsIVProcessor,
    checkResult: Instr.() -> Unit
  ) {
    val result = LegacyMipsDisassembler(processor).disassemble(
      MemBinLoader(ByteBuffer.allocate(4).putInt(instr.swapBytes()).array()),
      FunctionDef("UnitTest", 0, 4)
    )
    assertThat(result.instr).isNotEmpty()
    checkResult(result.instr[0])
  }

  private fun testInstr(
    assemble: Assembler.() -> Unit,
    processor: LegacyMipsProcessor = MipsIVProcessor,
    checkResult: Instr.() -> Unit
  ) {
    val bytes = assembleAsByteArray { assemble(this) }
    val result = LegacyMipsDisassembler(processor).disassemble(
      MemBinLoader(bytes),
      FunctionDef("UnitTest", 0, bytes.size)
    )
    assertThat(result.instr).isNotEmpty()
    checkResult(result.instr[0])
  }
}
