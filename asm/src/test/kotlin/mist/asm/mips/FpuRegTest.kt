package mist.asm.mips

import mist.test.util.assertThatDisassemblerException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FpuRegTest {
  @Test
  fun `return fpu reg for id`() {
    arrayOf(
      0 to FpuReg.F0,
      1 to FpuReg.F1,
      2 to FpuReg.F2,
      3 to FpuReg.F3,
      4 to FpuReg.F4,
      5 to FpuReg.F5,
      6 to FpuReg.F6,
      7 to FpuReg.F7,
      8 to FpuReg.F8,
      9 to FpuReg.F9,
      10 to FpuReg.F10,
      11 to FpuReg.F11,
      12 to FpuReg.F12,
      13 to FpuReg.F13,
      14 to FpuReg.F14,
      15 to FpuReg.F15,
      16 to FpuReg.F16,
      17 to FpuReg.F17,
      18 to FpuReg.F18,
      19 to FpuReg.F19,
      20 to FpuReg.F20,
      21 to FpuReg.F21,
      22 to FpuReg.F22,
      23 to FpuReg.F23,
      24 to FpuReg.F24,
      25 to FpuReg.F25,
      26 to FpuReg.F26,
      27 to FpuReg.F27,
      28 to FpuReg.F28,
      29 to FpuReg.F29,
      30 to FpuReg.F30,
      31 to FpuReg.F31
    ).forEach { (id, reg) ->
      assertThat(FpuReg.forId(id)).isEqualTo(reg)
    }
  }

  @Test
  fun `return fpu cc reg for id`() {
    arrayOf(
      0 to FpuReg.Cc0,
      1 to FpuReg.Cc1,
      2 to FpuReg.Cc2,
      3 to FpuReg.Cc3,
      4 to FpuReg.Cc4,
      5 to FpuReg.Cc5,
      6 to FpuReg.Cc6,
      7 to FpuReg.Cc7
    ).forEach { (id, reg) ->
      assertThat(FpuReg.ccForId(id)).isEqualTo(reg)
    }
  }

  @Test
  fun `values contains special purpose fpu registers`() {
    val values = FpuReg.values()
    assertThat(values.contains(FpuReg.Cc0)).isTrue()
    assertThat(values.contains(FpuReg.Cc1)).isTrue()
    assertThat(values.contains(FpuReg.Cc2)).isTrue()
    assertThat(values.contains(FpuReg.Cc3)).isTrue()
    assertThat(values.contains(FpuReg.Cc4)).isTrue()
    assertThat(values.contains(FpuReg.Cc5)).isTrue()
    assertThat(values.contains(FpuReg.Cc6)).isTrue()
    assertThat(values.contains(FpuReg.Cc7)).isTrue()
  }

  @Test
  fun `don't return fpu reg for -1 id`() {
    assertThatDisassemblerException().isThrownBy { FpuReg.forId(-1) }
  }

  @Test
  fun `don't return fpu reg for invalid id`() {
    assertThatDisassemblerException().isThrownBy { FpuReg.forId(Integer.MAX_VALUE) }
  }
}
