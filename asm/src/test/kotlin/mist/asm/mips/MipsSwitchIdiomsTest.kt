package mist.asm.mips

import kmips.Label
import kmips.Reg.*
import kmips.assembleAsByteArray
import mist.asm.FunctionDef
import mist.asm.IdiomMatcher
import mist.asm.mips.allegrex.AllegrexDisassembler
import mist.test.util.MemBinLoader
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MipsSwitchIdiomsTest {
  @Test
  fun `match switch at idiom, beq sltiu variant`() {
    val bytes = assembleAsByteArray {
      val dummyLabel = Label()
      sltiu(a1, a0, 0x13)
      beq(a1, zero, dummyLabel)
      nop()
      sll(a0, a0, 0x2)
      lui(at, 0x8900)
      addu(at, at, a0)
      lw(at, -0x2240, at)
      jr(at)
      nop()
      label(dummyLabel)
    }
    verifySwitchMatch(bytes, MipsSwitchIdioms().atRegIdiom, 0x88FFDDC0.toInt())
  }

  @Test
  fun `match switch at idiom, beql slti variant`() {
    val bytes = assembleAsByteArray {
      val dummyLabel = Label()
      slti(a1, a0, 0x13)
      beql(a1, zero, dummyLabel)
      nop()
      sll(a0, a0, 0x2)
      lui(at, 0x8900)
      addu(at, at, a0)
      lw(at, -0x2240, at)
      jr(at)
      nop()
      label(dummyLabel)
    }
    verifySwitchMatch(bytes, MipsSwitchIdioms().atRegIdiom, 0x88FFDDC0.toInt())
  }

  @Test
  fun `match switch a0 idiom`() {
    val bytes = assembleAsByteArray {
      val dummyLabel = Label()
      sltiu(v0, s1, 0x13)
      beq(v0, zero, dummyLabel)
      lui(v1, 0x8900)
      sll(v0, s1, 0x2)
      addiu(v1, v1, 0x2240)
      addu(v0, v0, v1)
      lw(a0, 0x0, v0)
      jr(a0)
      nop()
      label(dummyLabel)
    }
    verifySwitchMatch(bytes, MipsSwitchIdioms().a0RegIdiom, 0x89002240.toInt())
  }

  private fun verifySwitchMatch(
    bytes: ByteArray,
    idiom: IdiomMatcher<MipsInstr, *, SwitchDescriptor>,
    expectedJumpTableLoc: Int
  ) {
    val dasm = AllegrexDisassembler().disassemble(MemBinLoader(bytes), FunctionDef("", 0, bytes.size))
    val result = idiom.matches(dasm.instrs, dasm.instrs.lastIndex)
    Assertions.assertThat(result).isNotNull()
    result!! // using !! because non-null was verified above
    assertThat(result.jumpTableLoc).isEqualTo(expectedJumpTableLoc)
    assertThat(result.relInstrs).isNotEmpty()
    assertThat(result.switchCaseCount).isEqualTo(0x13)
  }
}
