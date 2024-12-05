package mist.asm

import mist.asm.mips.GprReg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OperandTest {
  @Test
  fun `converts reg to string`() {
    assertThat(RegOperand(GprReg.S0).toString()).isEqualTo("s0")
  }


  @Test
  fun `converts int to string`() {
    assertThat(ImmOperand(0x42).toString()).isEqualTo("0x42")
    assertThat(ImmOperand(-0x42).toString()).isEqualTo("-0x42")
  }
}
