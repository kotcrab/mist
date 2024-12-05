package mist.asm

abstract class Instr(val addr: Int, val opcode: Opcode, val operands: Array<out Operand>) {
  fun hasFlag(flag: OpcodeFlag): Boolean {
    return opcode.flags.contains(flag)
  }

  fun hasProcessor(processor: Processor): Boolean {
    return opcode.processors.contains(processor)
  }

  fun getUsedRegisters(): List<Reg> {
    return getOpcodeRegisters(opcode.use)
  }

  fun getModifiedRegisters(): List<Reg> {
    return getOpcodeRegisters(opcode.modify)
  }

  private fun getOpcodeRegisters(refs: Array<out OperandRef>): List<Reg> {
    val regs = mutableListOf<Reg>()
    refs.forEach { regs.add(it.getReg(this)) }
    return regs
  }

  protected fun matches(opcode: Opcode? = null, vararg matchers: OperandMatcher): Boolean {
    if (opcode != null && this.opcode != opcode) return false
    return matchers
      .mapIndexed { idx, matcher -> matcher.match(operands.getOrNull(idx)) }
      .all { it == true }
  }

  protected fun matchesExact(opcode: Opcode, vararg matchers: OperandMatcher): Boolean {
    if (this.opcode != opcode) return false
    return matchers
      .mapIndexed { idx, matcher -> matcher.match(operands.getOrNull(idx)) }
      .all { it == true }
  }
}
