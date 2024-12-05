package mist.asm

object Operand0Ref : OperandIdxRef(0)

object Operand1Ref : OperandIdxRef(1)

object Operand2Ref : OperandIdxRef(2)

object Operand3Ref : OperandIdxRef(3)

object Operand4Ref : OperandIdxRef(4)

object Operand5Ref : OperandIdxRef(5)

object Operand6Ref : OperandIdxRef(6)

object Operand7Ref : OperandIdxRef(7)

object Operand8Ref : OperandIdxRef(8)

object Operand9Ref : OperandIdxRef(9)

object Operand10Ref : OperandIdxRef(10)

open class OperandIdxRef(val idx: Int) : OperandRef {
  override fun getReg(instr: Instr): Reg {
    return (instr.operands[idx] as RegOperand).reg
  }
}

data class OperandRegRef(val reg: Reg) : OperandRef {
  override fun getReg(instr: Instr): Reg {
    return reg
  }
}

interface OperandRef {
  fun getReg(instr: Instr): Reg
}
