package mist.symbolic

import io.ksmt.expr.KBitVec32Value
import io.ksmt.expr.KBitVec8Value
import io.ksmt.solver.KModel
import io.ksmt.solver.model.KFuncInterpVarsFree
import io.ksmt.solver.model.KModelImpl
import io.ksmt.sort.KBv8Sort
import io.ksmt.utils.cast
import kio.util.toWHex
import java.io.File

class ModelWriter {
  fun writeToFile(model: KModel, functionStates: FunctionStates, file: File) {
    file.writeText(writeToString(model, functionStates))
  }

  private fun writeToString(model: KModel, functionStates: FunctionStates): String {
    val sb = StringBuilder()
    sb.appendLine("// model declarations")
    model.declarations.forEach { decl ->
      when {
        decl.name == "ram" -> {
          val ram = model.interpretation(decl)?.toString()
          if (ram?.startsWith("(asArray array!fresh!") == false) {
            error("RAM doesn't match the expected declaration: $ram")
          }
        }
        decl.name.startsWith("array!fresh!") -> {
          val ram: KFuncInterpVarsFree<KBv8Sort> = model.interpretation(decl).cast()
          ram.entries.map { entry ->
            val key: KBitVec32Value = entry.args.first().cast()
            val value: KBitVec8Value = entry.value.cast()
            if (key.intValue in Engine.assumedSpRange) {
              println("WARN: Model sets value in the stack range: ${key.intValue.toWHex()}")
            }
            sb.appendLine("ram:${key.intValue.toWHex()}=${value.byteValue.toWHex()}")
          }
        }
        else -> {
          val value: KBitVec32Value = model.interpretation(decl)!!.default.cast()
          sb.appendLine("${decl.name}=${value.intValue.toWHex()}")
        }
      }
    }
    sb.appendLine("\n// function return values")
    functionStates.getAll("fun:").forEach { (stateKey, counter) ->
      val name = stateKey.removePrefix("fun:")
      val ctx = (model as KModelImpl).ctx
      val registers = listOf("v0", "v1")
      repeat(counter.value) { executionNumber ->
        registers.forEach { register ->
          val symbolicName = "fun:$register:$executionNumber:$name"
          val value: KBitVec32Value = model.eval(ctx.mkConst(symbolicName, ctx.bv32Sort), isComplete = true).cast()
          sb.appendLine("$symbolicName=${value.intValue.toWHex()}")
        }
      }
    }
    return sb.toString()
  }
}
