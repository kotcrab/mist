package mist.symbolic

import io.ksmt.expr.KBitVec32Value
import io.ksmt.expr.KBitVec8Value
import io.ksmt.solver.KModel
import io.ksmt.solver.model.KFuncInterpVarsFree
import io.ksmt.sort.KBv8Sort
import io.ksmt.utils.cast
import kio.util.toWHex
import java.io.File

class ModelWriter {
  fun writeToFile(model: KModel, file: File) {
    file.writeText(writeToString(model))
  }

  private fun writeToString(model: KModel): String {
    val sb = StringBuilder()
    model.declarations.forEach { decl ->
      when (decl.name) {
        "ram" -> {
          val ram = model.interpretation(decl)?.toString()
          if (ram != "(asArray array!fresh!0)" && ram != "(asArray array!fresh!1)") {
            error("RAM doesn't match the expected declaration: $ram")
          }
        }
        in setOf("array!fresh!0", "array!fresh!1") -> {
          val ram: KFuncInterpVarsFree<KBv8Sort> = model.interpretation(decl).cast()
          ram.entries.map { entry ->
            val key: KBitVec32Value = entry.args.first().cast()
            val value: KBitVec8Value = entry.value.cast()
            sb.appendLine("ram:${key.intValue.toWHex()}=${value.byteValue.toWHex()}")
          }
        }
        else -> {
          val value: KBitVec32Value = model.interpretation(decl)!!.default.cast()
          sb.appendLine("${decl.name}=${value.intValue.toWHex()}")
        }
      }
    }
    return sb.toString()
  }
}
