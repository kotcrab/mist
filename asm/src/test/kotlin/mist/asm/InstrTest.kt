package mist.asm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InstrTest {
  object TestReg : Reg("test", 0, 32)
  object TestReg2 : Reg("test2", 1, 32)
  object TestProcessor : Processor("test")
  object TestOpcode : Opcode(
    "test",
    "",
    arrayOf(TestProcessor),
    arrayOf(MemoryRead),
    modify = arrayOf(Operand0Ref),
    use = arrayOf(Operand1Ref)
  )

  object TestInstr : Instr(0x0, TestOpcode, arrayOf(RegOperand(TestReg), RegOperand(TestReg2)))

  @Test
  fun `checks if opcode has flag`() {
    assertThat(TestInstr.hasFlag(MemoryRead)).isTrue()
  }

  @Test
  fun `checks if opcode has processor`() {
    assertThat(TestInstr.hasProcessor(TestProcessor)).isTrue()
  }

  @Test
  fun `gets used registers`() {
    assertThat(TestInstr.getUsedRegisters()).containsExactly(TestReg2)
  }

  @Test
  fun `gets modified registers`() {
    assertThat(TestInstr.getModifiedRegisters()).containsExactly(TestReg)
  }
}
