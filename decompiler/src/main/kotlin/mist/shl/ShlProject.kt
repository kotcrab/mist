package mist.shl

import mist.asm.FunctionDef

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
