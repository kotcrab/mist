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

package mist

import kmips.Assembler
import kmips.FpuReg.*
import kmips.Label
import kmips.Reg.*
import kmips.assembleAsByteArray
import mist.asm.*
import mist.io.MemBinLoader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** @author Kotcrab */
class DisassemblerTest {
    private val labelTarget = 0x40
    private val labelExpected = (labelTarget - 0x4) / 0x4

    @Test fun testLb() = testInstr({ lb(a0, 0xCD, s0) }, { verify(Opcode.Lb, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testLbu() = testInstr({ lbu(a0, 0xCD, s0) }, { verify(Opcode.Lbu, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testSb() = testInstr({ sb(a0, 0xCD, s0) }, { verify(Opcode.Sb, Reg.a0, Reg.s0, 0xCD) })

    @Test fun testLh() = testInstr({ lh(a0, 0xCD, s0) }, { verify(Opcode.Lh, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testLhu() = testInstr({ lhu(a0, 0xCD, s0) }, { verify(Opcode.Lhu, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testSh() = testInstr({ sh(a0, 0xCD, s0) }, { verify(Opcode.Sh, Reg.a0, Reg.s0, 0xCD) })

    @Test fun testLw() = testInstr({ lw(a0, 0xCD, s0) }, { verify(Opcode.Lw, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testSw() = testInstr({ sw(a0, 0xCD, s0) }, { verify(Opcode.Sw, Reg.a0, Reg.s0, 0xCD) })

    @Test fun testLwl() = testInstr({ lwl(a0, 0xCD, s0) }, { verify(Opcode.Lwl, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testLwr() = testInstr({ lwr(a0, 0xCD, s0) }, { verify(Opcode.Lwr, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testSwl() = testInstr({ swl(a0, 0xCD, s0) }, { verify(Opcode.Swl, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testSwr() = testInstr({ swr(a0, 0xCD, s0) }, { verify(Opcode.Swr, Reg.a0, Reg.s0, 0xCD) })

    @Test fun testLl() = testInstr({ ll(a0, 0xCD, s0) }, { verify(Opcode.Ll, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testSc() = testInstr({ sc(a0, 0xCD, s0) }, { verify(Opcode.Sc, Reg.a0, Reg.s0, 0xCD) })

    @Test fun testAddi() = testInstr({ addi(a0, s0, 0xCD) }, { verify(Opcode.Addi, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testAddiu() = testInstr({ addiu(a0, s0, 0xCD) }, { verify(Opcode.Addiu, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testSlti() = testInstr({ slti(a0, s0, 0xCD) }, { verify(Opcode.Slti, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testSltiu() = testInstr({ sltiu(a0, s0, 0xCD) }, { verify(Opcode.Sltiu, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testAndi() = testInstr({ andi(a0, s0, 0xCD) }, { verify(Opcode.Andi, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testOri() = testInstr({ ori(a0, s0, 0xCD) }, { verify(Opcode.Ori, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testXori() = testInstr({ xori(a0, s0, 0xCD) }, { verify(Opcode.Xori, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testLui() = testInstr({ lui(a0, 0xCD) }, { verify(Opcode.Lui, Reg.a0, 0xCD) })

    @Test fun testAdd() = testInstr({ add(a0, s0, t0) }, { verify(Opcode.Add, Reg.a0, Reg.s0, Reg.t0) })
    @Test fun testAddu() = testInstr({ addu(a0, s0, t0) }, { verify(Opcode.Addu, Reg.a0, Reg.s0, Reg.t0) })
    @Test fun testSub() = testInstr({ sub(a0, s0, t0) }, { verify(Opcode.Sub, Reg.a0, Reg.s0, Reg.t0) })
    @Test fun testSubu() = testInstr({ subu(a0, s0, t0) }, { verify(Opcode.Subu, Reg.a0, Reg.s0, Reg.t0) })

    @Test fun testSlt() = testInstr({ slt(a0, s0, t0) }, { verify(Opcode.Slt, Reg.a0, Reg.s0, Reg.t0) })
    @Test fun testSltu() = testInstr({ sltu(a0, s0, t0) }, { verify(Opcode.Sltu, Reg.a0, Reg.s0, Reg.t0) })
    @Test fun testAnd() = testInstr({ and(a0, s0, t0) }, { verify(Opcode.And, Reg.a0, Reg.s0, Reg.t0) })
    @Test fun testOr() = testInstr({ or(a0, s0, t0) }, { verify(Opcode.Or, Reg.a0, Reg.s0, Reg.t0) })
    @Test fun testXor() = testInstr({ xor(a0, s0, t0) }, { verify(Opcode.Xor, Reg.a0, Reg.s0, Reg.t0) })
    @Test fun testNor() = testInstr({ nor(a0, s0, t0) }, { verify(Opcode.Nor, Reg.a0, Reg.s0, Reg.t0) })

    @Test fun testSll() = testInstr({ sll(a0, s0, 0x10) }, { verify(Opcode.Sll, Reg.a0, Reg.s0, 0x10) })
    @Test fun testSrl() = testInstr({ srl(a0, s0, 0x10) }, { verify(Opcode.Srl, Reg.a0, Reg.s0, 0x10) })
    @Test fun testSra() = testInstr({ sra(a0, s0, 0x10) }, { verify(Opcode.Sra, Reg.a0, Reg.s0, 0x10) })
    @Test fun testSllv() = testInstr({ sllv(a0, s0, t0) }, { verify(Opcode.Sllv, Reg.a0, Reg.s0, Reg.t0) })
    @Test fun testSrlv() = testInstr({ srlv(a0, s0, t0) }, { verify(Opcode.Srlv, Reg.a0, Reg.s0, Reg.t0) })
    @Test fun testSrav() = testInstr({ srav(a0, s0, t0) }, { verify(Opcode.Srav, Reg.a0, Reg.s0, Reg.t0) })

    @Test fun testMult() = testInstr({ mult(a0, s0) }, { verify(Opcode.Mult, Reg.a0, Reg.s0) })
    @Test fun testMultu() = testInstr({ multu(a0, s0) }, { verify(Opcode.Multu, Reg.a0, Reg.s0) })
    @Test fun testDiv() = testInstr({ div(a0, s0) }, { verify(Opcode.Div, Reg.a0, Reg.s0) })
    @Test fun testDivu() = testInstr({ divu(a0, s0) }, { verify(Opcode.Divu, Reg.a0, Reg.s0) })
    @Test fun testMfhi() = testInstr({ mfhi(a0) }, { verify(Opcode.Mfhi, Reg.a0) })
    @Test fun testMthi() = testInstr({ mthi(a0) }, { verify(Opcode.Mthi, Reg.a0) })
    @Test fun testMflo() = testInstr({ mflo(a0) }, { verify(Opcode.Mflo, Reg.a0) })
    @Test fun testMtlo() = testInstr({ mtlo(a0) }, { verify(Opcode.Mtlo, Reg.a0) })

    @Test fun testJ() = testInstr({ j(0x40) }, { verify(Opcode.J, 0x40) })
    @Test fun testJal() = testInstr({ jal(0x40) }, { verify(Opcode.Jal, 0x40) })
    @Test fun testJr() = testInstr({ jr(a0) }, { verify(Opcode.Jr, Reg.a0) })
    @Test fun testJalr() = testInstr({ jalr(a0) }, { verify(Opcode.Jalr, Reg.ra, Reg.a0) })

    @Test fun testBeq() = testInstr({ beq(a0, s0, testLabel()) }, { verify(Opcode.Beq, Reg.a0, Reg.s0, labelExpected) })
    @Test fun testBne() = testInstr({ bne(a0, s0, testLabel()) }, { verify(Opcode.Bne, Reg.a0, Reg.s0, labelExpected) })
    @Test fun testBlez() = testInstr({ blez(a0, testLabel()) }, { verify(Opcode.Blez, Reg.a0, labelExpected) })
    @Test fun testBgtz() = testInstr({ bgtz(a0, testLabel()) }, { verify(Opcode.Bgtz, Reg.a0, labelExpected) })
    @Test fun testBeql() = testInstr({ beql(a0, s0, testLabel()) }, { verify(Opcode.Beql, Reg.a0, Reg.s0, labelExpected) })
    @Test fun testBnel() = testInstr({ bnel(a0, s0, testLabel()) }, { verify(Opcode.Bnel, Reg.a0, Reg.s0, labelExpected) })
    @Test fun testBlezl() = testInstr({ blezl(a0, testLabel()) }, { verify(Opcode.Blezl, Reg.a0, labelExpected) })
    @Test fun testBgtzl() = testInstr({ bgtzl(a0, testLabel()) }, { verify(Opcode.Bgtzl, Reg.a0, labelExpected) })

    @Test fun testBltz() = testInstr({ bltz(a0, testLabel()) }, { verify(Opcode.Bltz, Reg.a0, labelExpected) })
    @Test fun testBgez() = testInstr({ bgez(a0, testLabel()) }, { verify(Opcode.Bgez, Reg.a0, labelExpected) })
    @Test fun testBltzal() = testInstr({ bltzal(a0, testLabel()) }, { verify(Opcode.Bltzal, Reg.a0, labelExpected) })
    @Test fun testBgezal() = testInstr({ bgezal(a0, testLabel()) }, { verify(Opcode.Bgezal, Reg.a0, labelExpected) })
    @Test fun testBltzl() = testInstr({ bltzl(a0, testLabel()) }, { verify(Opcode.Bltzl, Reg.a0, labelExpected) })
    @Test fun testBgezl() = testInstr({ bgezl(a0, testLabel()) }, { verify(Opcode.Bgezl, Reg.a0, labelExpected) })
    @Test fun testBltzall() = testInstr({ bltzall(a0, testLabel()) }, { verify(Opcode.Bltzall, Reg.a0, labelExpected) })
    @Test fun testBgezall() = testInstr({ bgezall(a0, testLabel()) }, { verify(Opcode.Bgezall, Reg.a0, labelExpected) })

    @Test fun testSyscall() = testInstr({ syscall(0xCD) }, { verify(Opcode.Syscall, 0xCD) })
    @Test fun testBreak() = testInstr({ `break`(0xCD) }, { verify(Opcode.Break, 0xCD) })

    @Test fun testTge() = testInstr({ tge(a0, s0, 0xCD) }, { verify(Opcode.Tge, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testTgeu() = testInstr({ tgeu(a0, s0, 0xCD) }, { verify(Opcode.Tgeu, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testTlt() = testInstr({ tlt(a0, s0, 0xCD) }, { verify(Opcode.Tlt, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testTltu() = testInstr({ tltu(a0, s0, 0xCD) }, { verify(Opcode.Tltu, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testTeq() = testInstr({ teq(a0, s0, 0xCD) }, { verify(Opcode.Teq, Reg.a0, Reg.s0, 0xCD) })
    @Test fun testTne() = testInstr({ tne(a0, s0, 0xCD) }, { verify(Opcode.Tne, Reg.a0, Reg.s0, 0xCD) })

    @Test fun testTgei() = testInstr({ tgei(a0, 0xCD) }, { verify(Opcode.Tgei, Reg.a0, 0xCD) })
    @Test fun testTgeiu() = testInstr({ tgeiu(a0, 0xCD) }, { verify(Opcode.Tgeiu, Reg.a0, 0xCD) })
    @Test fun testTlti() = testInstr({ tlti(a0, 0xCD) }, { verify(Opcode.Tlti, Reg.a0, 0xCD) })
    @Test fun testTltiu() = testInstr({ tltiu(a0, 0xCD) }, { verify(Opcode.Tltiu, Reg.a0, 0xCD) })
    @Test fun testTeqi() = testInstr({ teqi(a0, 0xCD) }, { verify(Opcode.Teqi, Reg.a0, 0xCD) })
    @Test fun testTnei() = testInstr({ tnei(a0, 0xCD) }, { verify(Opcode.Tnei, Reg.a0, 0xCD) })

    @Test fun testSync() = testInstr({ sync(0x10) }, { verify(Opcode.Sync, 0x10) })

    @Test fun testNop() = testInstr({ nop() }, { verify(Opcode.Nop) })

    @Test fun testLwc1() = testInstr({ lwc1(f10, 0xCD, s0) }, { verify(Opcode.Lwc1, FpuReg.f10, Reg.s0, 0xCD) })
    @Test fun testSwc1() = testInstr({ swc1(f10, 0xCD, s0) }, { verify(Opcode.Swc1, FpuReg.f10, Reg.s0, 0xCD) })

    @Test fun testMtc1() = testInstr({ mtc1(s0, f10) }, { verify(Opcode.Mtc1, Reg.s0, FpuReg.f10) })
    @Test fun testMfc1() = testInstr({ mfc1(s0, f10) }, { verify(Opcode.Mfc1, Reg.s0, FpuReg.f10) })
    @Test fun testCtc1() = testInstr({ ctc1(s0, f10) }, { verify(Opcode.Ctc1, Reg.s0, FpuReg.f10) })
    @Test fun testCfc1() = testInstr({ cfc1(s0, f10) }, { verify(Opcode.Cfc1, Reg.s0, FpuReg.f10) })

    @Test fun testFpuAddS() = testInstr({ add.s(f4, f14, f24) }, { verify(Opcode.FpuAdd, FpuReg.f4, FpuReg.f14, FpuReg.f24) })
    @Test fun testFpuSubS() = testInstr({ sub.s(f4, f14, f24) }, { verify(Opcode.FpuSub, FpuReg.f4, FpuReg.f14, FpuReg.f24) })
    @Test fun testFpuMulS() = testInstr({ mul.s(f4, f14, f24) }, { verify(Opcode.FpuMul, FpuReg.f4, FpuReg.f14, FpuReg.f24) })
    @Test fun testFpuDivS() = testInstr({ div.s(f4, f14, f24) }, { verify(Opcode.FpuDiv, FpuReg.f4, FpuReg.f14, FpuReg.f24) })
    @Test fun testFpuAbsS() = testInstr({ abs.s(f4, f14) }, { verify(Opcode.FpuAbs, FpuReg.f4, FpuReg.f14) })
    @Test fun testFpuNegS() = testInstr({ neg.s(f4, f14) }, { verify(Opcode.FpuNeg, FpuReg.f4, FpuReg.f14) })
    @Test fun testFpuSqrtS() = testInstr({ sqrt.s(f4, f14) }, { verify(Opcode.FpuSqrt, FpuReg.f4, FpuReg.f14) })
    @Test fun testFpuRoundS() = testInstr({ round.w.s(f4, f14) }, { verify(Opcode.FpuRoundW, FpuReg.f4, FpuReg.f14) })
    @Test fun testFpuTruncS() = testInstr({ trunc.w.s(f4, f14) }, { verify(Opcode.FpuTruncW, FpuReg.f4, FpuReg.f14) })
    @Test fun testFpuCeilS() = testInstr({ ceil.w.s(f4, f14) }, { verify(Opcode.FpuCeilW, FpuReg.f4, FpuReg.f14) })
    @Test fun testFpuFloorS() = testInstr({ floor.w.s(f4, f14) }, { verify(Opcode.FpuFloorW, FpuReg.f4, FpuReg.f14) })

    @Test fun testFpuCondEqS() = testInstr({ c.eq.s(f4, f14) }, { verify(Opcode.FpuCEq, FpuReg.f4, FpuReg.f14) })
    @Test fun testFpuCondLeS() = testInstr({ c.le.s(f4, f14) }, { verify(Opcode.FpuCLe, FpuReg.f4, FpuReg.f14) })
    @Test fun testFpuCondLtS() = testInstr({ c.lt.s(f4, f14) }, { verify(Opcode.FpuCLt, FpuReg.f4, FpuReg.f14) })

    @Test fun testFpuFpuCvtSW() = testInstr({ cvt.s.w(f4, f14) }, { verify(Opcode.FpuCvtSW, FpuReg.f4, FpuReg.f14) })
    @Test fun testFpuFpuCvtWS() = testInstr({ cvt.w.s(f4, f14) }, { verify(Opcode.FpuCvtWS, FpuReg.f4, FpuReg.f14) })
    @Test fun testFpuFpuMov() = testInstr({ mov.s(f4, f14) }, { verify(Opcode.FpuMov, FpuReg.f4, FpuReg.f14) })

    @Test fun testFpuFpuBc1f() = testInstr({ bc1f(testLabel()) }, { verify(Opcode.FpuBc1f, labelExpected) })
    @Test fun testFpuFpuBc1t() = testInstr({ bc1t(testLabel()) }, { verify(Opcode.FpuBc1t, labelExpected) })
    @Test fun testFpuFpuBc1tl() = testInstr({ bc1tl(testLabel()) }, { verify(Opcode.FpuBc1tl, labelExpected) })
    @Test fun testFpuFpuBc1fl() = testInstr({ bc1fl(testLabel()) }, { verify(Opcode.FpuBc1fl, labelExpected) })

    private fun testLabel() = Label().apply { address = labelTarget }

    private fun Instr.verify(opcode: Opcode, op1: Any? = null, op2: Any? = null, op3: Any? = null) {
        assertEquals(this.opcode, opcode)
        assertTrue(getSourceRegisters().intersect(getModifiedRegisters().asIterable()).isEmpty())
        val combinedRegisters = getSourceRegisters().union(getModifiedRegisters().asIterable()).toMutableList()
        arrayOf(Pair(this.op1, op1), Pair(this.op2, op2), Pair(this.op3, op3)).forEach {
            when {
                it.second == null -> assertNull(it.first)
                it.second is Reg -> {
                    val reg = (it.first as Operand.Reg).reg
                    assertEquals(it.second, reg)
                    combinedRegisters.remove(reg)
                }
                it.second is FpuReg -> {
                    val reg = (it.first as Operand.FpuReg).reg
                    assertEquals(it.second, reg)
                    combinedRegisters.remove(reg)
                }
                it.second is Int -> assertEquals(it.second as Int, (it.first as Operand.Imm).value)
                else -> println("Don't know how to verify $it, register or immediate expected")
            }
        }
        combinedRegisters.remove(Reg.ra)
        combinedRegisters.remove(Reg.pc)
        combinedRegisters.remove(Reg.LO)
        combinedRegisters.remove(Reg.HI)
        combinedRegisters.remove(FpuReg.cc)
        assertTrue(combinedRegisters.isEmpty())
    }

    private fun testInstr(assemble: Assembler.() -> Unit, checkResult: Instr.() -> Unit) {
        val bytes = assembleAsByteArray { assemble(this) }
        val result = disassemble(MemBinLoader(bytes), "UnitTest", 0, bytes.size)
        assertTrue(result.instr.isNotEmpty())
        checkResult(result.instr[0])
    }
}
