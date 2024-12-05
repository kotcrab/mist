package mist.asm

import mist.asm.mips.GprReg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OperandMatcherTest {
  private val reg = RegOperand(GprReg.S0)
  private val reg2 = RegOperand(GprReg.S1)
  private val imm = ImmOperand(0x42)
  private val imm2 = ImmOperand(0x43)

  @Test
  fun `match null op only`() {
    val m = isNull()
    assertThat(m.match(null)).isTrue()
    assertThat(m.match(reg)).isFalse()
    assertThat(m.match(imm)).isFalse()
  }

  @Test
  fun `match any`() {
    val m = anyOp()
    assertThat(m.match(null)).isTrue()
    assertThat(m.match(reg)).isTrue()
    assertThat(m.match(imm)).isTrue()
  }

  @Test
  fun `match any reg`() {
    val m = anyReg()
    assertThat(m.match(null)).isFalse()
    assertThat(m.match(reg)).isTrue()
    assertThat(m.match(reg2)).isTrue()
    assertThat(m.match(imm)).isFalse()
  }

  @Test
  fun `match single reg`() {
    val m = isReg(GprReg.S0)
    assertThat(m.match(null)).isFalse()
    assertThat(m.match(reg)).isTrue()
    assertThat(m.match(reg2)).isFalse()
    assertThat(m.match(imm)).isFalse()
  }

  @Test
  fun `match multiple reg`() {
    val m = isReg(GprReg.S0, GprReg.S1)
    assertThat(m.match(null)).isFalse()
    assertThat(m.match(reg)).isTrue()
    assertThat(m.match(reg2)).isTrue()
    assertThat(m.match(imm)).isFalse()
  }

  @Test
  fun `match any imm`() {
    val m = anyImm()
    assertThat(m.match(null)).isFalse()
    assertThat(m.match(reg)).isFalse()
    assertThat(m.match(imm)).isTrue()
    assertThat(m.match(imm2)).isTrue()
  }

  @Test
  fun `match single imm`() {
    val m = isImm(0x42)
    assertThat(m.match(null)).isFalse()
    assertThat(m.match(reg)).isFalse()
    assertThat(m.match(imm)).isTrue()
    assertThat(m.match(imm2)).isFalse()
  }

  @Test
  fun `match multiple imm`() {
    val m = isImm(0x42, 0x43)
    assertThat(m.match(null)).isFalse()
    assertThat(m.match(reg)).isFalse()
    assertThat(m.match(imm)).isTrue()
    assertThat(m.match(imm2)).isTrue()
  }
}
