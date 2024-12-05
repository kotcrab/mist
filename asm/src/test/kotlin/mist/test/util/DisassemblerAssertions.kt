package mist.test.util

import mist.asm.DisassemblerException
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowableTypeAssert

fun assertThatDisassemblerException(): ThrowableTypeAssert<DisassemblerException> {
  return Assertions.assertThatExceptionOfType(DisassemblerException::class.java)
}
