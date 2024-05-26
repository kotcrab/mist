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

package mist.shl

import mist.asm.FunctionDef

/** @author Kotcrab */

class ShlProject {
  val functionDefs = mutableListOf<ShlFunctionDef>()
}

//rename to ShlFuncDef?
class ShlFunctionDef(var name: String, val offset: Int, val len: Int, var reversed: Boolean) {
  var returnType = "void?"
  val arguments = mutableListOf<ShlArgumentDef>()

  fun toLLDef(): FunctionDef {
    return FunctionDef(name, offset, len)
  }

  fun toDeclarationString(includeReturnType: Boolean = true): String {
    val args = arguments.joinToString { "${it.type} ${it.name} = ${it.register}" }
    val funcReturn = if (includeReturnType) "$returnType " else ""
    return "$funcReturn$name($args)"
  }

  fun toCallString(): String {
    val args = arguments.joinToString { "${it.name} = ${it.register}" }
    return "$name($args)"
  }

  override fun toString(): String {
    return toDeclarationString()
  }
}

class ShlArgumentDef(val type: String, val name: String, val register: String)

interface ShlDefsChanged {
  fun shlDefsChanged(type: ShlDefsChangeType)
}

enum class ShlDefsChangeType {
  FunctionDefs, Types
}
