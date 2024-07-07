package mist.symbolic

import mist.asm.mips.GprReg
import java.io.File

class ModelLoader {
  fun loadFromFile(file: File, ctx: Context, functionLibrary: FunctionLibrary): FunctionLibrary {
    val functionReplays = mutableMapOf<String, MutableMap<Int, BvExpr>>()
    file.forEachLine { line ->
      val lineParts = line.split("=")
      val parts = lineParts[0].split(":")
      val value = Expr.Const.of(Integer.parseUnsignedInt(lineParts[1], 16))
      when {
        parts[0] == "fun" && parts[1] == "v0" -> {
          val executionNumber = parts[2].toInt()
          val name = parts[3]
          functionReplays.getOrPut(name) { mutableMapOf() }[executionNumber] = value
        }
        parts[0] == "ram" -> {
          val address = Expr.Const.of(Integer.parseUnsignedInt(parts[1], 16))
          ctx.memory.writeByte(address, value)
        }
        parts[0] == "lo" -> {
          ctx.lo = value
        }
        parts[0] == "hi" -> {
          ctx.hi = value
        }
        else -> {
          val reg = GprReg.values().firstOrNull { it.name == parts[0] }
            ?: error("Unknown symbolic variable in model: ${parts[0]}")
          ctx.writeGpr(reg, value)
        }
      }
    }
    return functionLibrary.transform { name, functionHandler ->
      if (functionHandler is SymbolicFunctionHandler) {
        ReplaySymbolicFunctionHandler(functionReplays.getOrElse(name) { emptyMap() }, functionHandler)
      } else {
        functionHandler
      }
    }
  }
}
