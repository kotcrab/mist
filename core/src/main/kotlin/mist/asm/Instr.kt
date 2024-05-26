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

/** @author Kotcrab */

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
