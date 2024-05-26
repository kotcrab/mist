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

import kio.util.toHex
import kio.util.toSignedHex

/** @author Kotcrab */

abstract class Operand

// TODO verify passed arguments + unit tests
class RegOperand(val reg: Reg, val name: String = reg.name, val bitsStart: Int = 0, val bitsSize: Int = reg.bitsSize) :
  Operand() {
  override fun toString(): String {
    return name
  }
}

// TODO rename ot I32Operand?
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
