package mist.asm.mips

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mist.asm.ImmOperand
import mist.asm.OperandMatcher
import mist.asm.RegOperand
import mist.asm.anyReg
import mist.asm.mips.MipsOpcode.Nop
import mist.asm.mips.MipsOpcode.Sw
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MipsInstrTest {
  @Test
  fun `convert operands to reg`() {
    val instr = MipsInstr(
      0, Nop, RegOperand(GprReg.A0), RegOperand(GprReg.A1), RegOperand(GprReg.A2),
      RegOperand(GprReg.A3)
    )
    assertThat(instr.op0AsReg()).isEqualTo(GprReg.A0)
    assertThat(instr.op1AsReg()).isEqualTo(GprReg.A1)
    assertThat(instr.op2AsReg()).isEqualTo(GprReg.A2)
    assertThat(instr.op3AsReg()).isEqualTo(GprReg.A3)
  }

  @Test
  fun `convert operands to imm`() {
    val instr = MipsInstr(
      0, Nop, ImmOperand(0x42), ImmOperand(0x43), ImmOperand(0x44),
      ImmOperand(0x45)
    )
    assertThat(instr.op0AsImm()).isEqualTo(0x42)
    assertThat(instr.op1AsImm()).isEqualTo(0x43)
    assertThat(instr.op2AsImm()).isEqualTo(0x44)
    assertThat(instr.op3AsImm()).isEqualTo(0x45)
  }

  @Test
  fun `match operands`() {
    val instr = MipsInstr(0, Nop, ImmOperand(0))
    assertThat(instr.matches()).isTrue()
    assertThat(instr.matches(Nop)).isTrue()
    assertThat(instr.matches(Nop, anyReg())).isFalse()
    assertThat(instr.matchesExact(Nop)).isFalse()
  }

  @Test
  fun `match call all operands`() {
    val matcher = mockk<OperandMatcher>()
    every { matcher.match(any()) }.returns(true)
    val instr = MipsInstr(0, Nop, ImmOperand(0))
    assertThat(instr.matches(Nop, matcher, matcher, matcher)).isTrue()
    verify(exactly = 3) { matcher.match(any()) }
  }

  @Test
  fun `convert to string`() {
    assertThat(MipsInstr(0, Nop).toString()).isEqualTo("0x0: nop")
    assertThat(MipsInstr(0, Nop, RegOperand(GprReg.A0)).toString()).isEqualTo("0x0: nop a0")
    assertThat(MipsInstr(0, Nop, RegOperand(GprReg.A0), ImmOperand(1)).toString())
      .isEqualTo("0x0: nop a0, 0x1")
    assertThat(MipsInstr(0, Nop, RegOperand(GprReg.A0), ImmOperand(1), ImmOperand(5)).toString())
      .isEqualTo("0x0: nop a0, 0x1, 0x5")
    assertThat(MipsInstr(0, Nop, RegOperand(GprReg.A0), ImmOperand(1), ImmOperand(5), ImmOperand(9)).toString())
      .isEqualTo("0x0: nop a0, 0x1, 0x5, 0x9")
    assertThat(MipsInstr(0, Sw, RegOperand(GprReg.A0), RegOperand(GprReg.S0), ImmOperand(1)).toString())
      .isEqualTo("0x0: sw a0, 0x1(s0)")
  }
}
