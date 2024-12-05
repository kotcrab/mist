package mist.asm

fun isNull() = object : OperandMatcher {
  override fun match(op: Operand?): Boolean {
    if (op == null) return true
    return false
  }
}

fun anyOp() = object : OperandMatcher {
  override fun match(op: Operand?): Boolean {
    return true
  }
}

fun anyReg() = object : OperandMatcher {
  override fun match(op: Operand?): Boolean {
    if (op == null) return false
    if (op !is RegOperand) return false
    return true
  }
}

fun isReg(reg: Reg) = object : OperandMatcher {
  override fun match(op: Operand?): Boolean {
    if (op == null) return false
    if (op !is RegOperand) return false
    return op.reg == reg
  }
}

fun isReg(vararg regs: Reg) = object : OperandMatcher {
  override fun match(op: Operand?): Boolean {
    if (op == null) return false
    if (op !is RegOperand) return false
    return op.reg in regs
  }
}

fun anyImm() = object : OperandMatcher {
  override fun match(op: Operand?): Boolean {
    if (op == null) return false
    if (op !is ImmOperand) return false
    return true
  }
}

fun isImm(value: Int) = object : OperandMatcher {
  override fun match(op: Operand?): Boolean {
    if (op == null) return false
    if (op !is ImmOperand) return false
    return op.value == value
  }
}

fun isImm(vararg values: Int) = object : OperandMatcher {
  override fun match(op: Operand?): Boolean {
    if (op == null) return false
    if (op !is ImmOperand) return false
    return op.value in values
  }
}

interface OperandMatcher {
  fun match(op: Operand?): Boolean
}
