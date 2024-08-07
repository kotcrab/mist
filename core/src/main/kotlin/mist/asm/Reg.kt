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

abstract class Reg(val name: String, val id: Int, val bitsSize: Int) {
  protected companion object {
    fun <T : Reg> forId(regs: Array<T>, id: Int): T {
      if (id == -1) throw DisassemblerException("can't return directly inaccessible register")
      regs.forEach {
        if (id == it.id) return it
      }
      throw DisassemblerException("no such register id: $id")
    }
  }

  override fun toString(): String {
    return name
  }
}
