package mist.asm.mips

import mist.test.util.assertThatDisassemblerException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Cop2RegTest {
  @Test
  fun `return cop2 reg for id`() {
    arrayOf(
      0 to Cop2Reg.Cop2r0,
      1 to Cop2Reg.Cop2r1,
      2 to Cop2Reg.Cop2r2,
      3 to Cop2Reg.Cop2r3,
      4 to Cop2Reg.Cop2r4,
      5 to Cop2Reg.Cop2r5,
      6 to Cop2Reg.Cop2r6,
      7 to Cop2Reg.Cop2r7,
      8 to Cop2Reg.Cop2r8,
      9 to Cop2Reg.Cop2r9,
      10 to Cop2Reg.Cop2r10,
      11 to Cop2Reg.Cop2r11,
      12 to Cop2Reg.Cop2r12,
      13 to Cop2Reg.Cop2r13,
      14 to Cop2Reg.Cop2r14,
      15 to Cop2Reg.Cop2r15,
      16 to Cop2Reg.Cop2r16,
      17 to Cop2Reg.Cop2r17,
      18 to Cop2Reg.Cop2r18,
      19 to Cop2Reg.Cop2r19,
      20 to Cop2Reg.Cop2r20,
      21 to Cop2Reg.Cop2r21,
      22 to Cop2Reg.Cop2r22,
      23 to Cop2Reg.Cop2r23,
      24 to Cop2Reg.Cop2r24,
      25 to Cop2Reg.Cop2r25,
      26 to Cop2Reg.Cop2r26,
      27 to Cop2Reg.Cop2r27,
      28 to Cop2Reg.Cop2r28,
      29 to Cop2Reg.Cop2r29,
      30 to Cop2Reg.Cop2r30,
      31 to Cop2Reg.Cop2r31
    ).forEach { (id, reg) ->
      assertThat(Cop2Reg.forId(id)).isEqualTo(reg)
    }
  }

  @Test
  fun `don't return cop2 reg for -1 id`() {
    assertThatDisassemblerException().isThrownBy { Cop2Reg.forId(-1) }
  }

  @Test
  fun `don't return cop2 reg for invalid id`() {
    assertThatDisassemblerException().isThrownBy { Cop2Reg.forId(Integer.MAX_VALUE) }
  }
}
