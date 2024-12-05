package mist.asm

import kio.util.toHex
import kio.util.toSignedHex

abstract class Operand

// TODO verify passed arguments + unit tests
class RegOperand(val reg: Reg, val name: String = reg.name, val bitsStart: Int = 0, val bitsSize: Int = reg.bitsSize) :
  Operand() {
  override fun toString(): String {
    return name
  }
}

class ImmOperand(val value: Int, val hintUnsigned: Boolean = false) : Operand() {
  fun toHintedUnsigned(): ImmOperand {
    return ImmOperand(value, true)
  }

  override fun toString(): String {
    return if (hintUnsigned) {
      value.toHex()
    } else {
      value.toSignedHex()
    }
  }
}

class FloatOperand(val value: Float) : Operand() {
  override fun toString(): String {
    return value.toString()
  }
}
