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

package mist.asm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** @author Kotcrab */

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
