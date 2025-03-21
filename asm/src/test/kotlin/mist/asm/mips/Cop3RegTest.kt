package mist.asm.mips

import mist.test.util.assertThatDisassemblerException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Cop3RegTest {
  @Test
  fun `return cop3 reg for id`() {
    arrayOf(
      0 to Cop3Reg.Cop3r0,
      1 to Cop3Reg.Cop3r1,
      2 to Cop3Reg.Cop3r2,
      3 to Cop3Reg.Cop3r3,
      4 to Cop3Reg.Cop3r4,
      5 to Cop3Reg.Cop3r5,
      6 to Cop3Reg.Cop3r6,
      7 to Cop3Reg.Cop3r7,
      8 to Cop3Reg.Cop3r8,
      9 to Cop3Reg.Cop3r9,
      10 to Cop3Reg.Cop3r10,
      11 to Cop3Reg.Cop3r11,
      12 to Cop3Reg.Cop3r12,
      13 to Cop3Reg.Cop3r13,
      14 to Cop3Reg.Cop3r14,
      15 to Cop3Reg.Cop3r15,
      16 to Cop3Reg.Cop3r16,
      17 to Cop3Reg.Cop3r17,
      18 to Cop3Reg.Cop3r18,
      19 to Cop3Reg.Cop3r19,
      20 to Cop3Reg.Cop3r20,
      21 to Cop3Reg.Cop3r21,
      22 to Cop3Reg.Cop3r22,
      23 to Cop3Reg.Cop3r23,
      24 to Cop3Reg.Cop3r24,
      25 to Cop3Reg.Cop3r25,
      26 to Cop3Reg.Cop3r26,
      27 to Cop3Reg.Cop3r27,
      28 to Cop3Reg.Cop3r28,
      29 to Cop3Reg.Cop3r29,
      30 to Cop3Reg.Cop3r30,
      31 to Cop3Reg.Cop3r31
    ).forEach { (id, reg) ->
      assertThat(Cop3Reg.forId(id)).isEqualTo(reg)
    }
  }

  @Test
  fun `don't return cop3 reg for -1 id`() {
    assertThatDisassemblerException().isThrownBy { Cop3Reg.forId(-1) }
  }

  @Test
  fun `don't return cop3 reg for invalid id`() {
    assertThatDisassemblerException().isThrownBy { Cop3Reg.forId(Integer.MAX_VALUE) }
  }
}
