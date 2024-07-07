package mist.symbolic

import kio.util.toWHex
import mist.module.Module
import java.io.File

class TraceWriter {
  fun writeToFile(module: Module, trace: Trace, file: File, traceCompareMessages: List<TraceComparator.Message>) {
    file.writeText(writeToString(module, trace) + "\n" + writeToString(traceCompareMessages))
  }

  private fun writeToString(module: Module, trace: Trace): String {
    var level = 0
    return trace.elements
      .filterNot {
        (it is TraceElement.MemoryRead && (it.address as Expr.Const).value in Engine.assumedSpRange) ||
          (it is TraceElement.MemoryWrite && (it.address as Expr.Const).value in Engine.assumedSpRange)
      }
      .joinToString(separator = "\n", postfix = "\n") {
        val prefix = "[${it.pc.toWHex()}] " + "%03d|".format(level)
        when (it) {
          is TraceElement.FunctionCall -> level++
          is TraceElement.FunctionReturn -> level--
          else -> {} // ignore
        }
        prefix + writeElementToString(module, it)
      }
  }

  fun writeElementToString(module: Module, element: TraceElement): String {
    return when (element) {
      is TraceElement.ExecutionStart -> {
        "start args: ${element.arguments.joinToString(separator = ", ")}"
      }
      is TraceElement.FunctionCall -> {
        val knownSuffix = if (element.known) "" else " [!!! unknown args count !!!]"
        "${element.name}(${element.arguments.joinToString(separator = ", ")})$knownSuffix"
      }
      is TraceElement.FunctionReturn -> {
        when (element.returnSize) {
          0 -> "return"
          1 -> "return ${element.v0}"
          2 -> "return ${element.v1}, ${element.v0}"
          else -> "return ${element.v1}, ${element.v0} [!!! unknown return size !!!]"
        }
      }
      is TraceElement.JumpOutOfFunctionBody -> "out of body jump at ${element.pc.toWHex()} to ${element.toAddress.toWHex()} from reg ${element.sourceReg}"
      is TraceElement.MemoryRead -> "read${element.size}${if (element.unsigned) "u" else ""}" +
        "(${module.lookupAddress((element.address as Expr.Const).value).toAccessString()}) [value=${element.value}]"
      is TraceElement.MemoryWrite -> "write${element.size}(${
        module.lookupAddress((element.address as Expr.Const).value).toAccessString()
      }, ${element.value})"
      is TraceElement.Sync -> "sync(${element.value})"
      is TraceElement.UseK1 -> "useK1() [value=${element.value}]"
      is TraceElement.ModifyK1 -> "modifyK1(${element.value})"
      is TraceElement.DidNotTerminateWithinLimit -> "--- did not terminate within limit, pc=${element.pc.toWHex()} ---"
    }
  }

  private fun writeToString(traceCompareMessages: List<TraceComparator.Message>): String {
    if (traceCompareMessages.isEmpty()) {
      return "No trace compare messages."
    }
    val noPcSet = " (none) "
    return traceCompareMessages.joinToString(separator = "\n", prefix = "Trace compare messages:\n", postfix = "\n") {
      "[${it.relatedExpectedPc?.toWHex() ?: noPcSet}/${it.relatedActualPc?.toWHex() ?: noPcSet}]: ${it.message}"
    }
  }
}
