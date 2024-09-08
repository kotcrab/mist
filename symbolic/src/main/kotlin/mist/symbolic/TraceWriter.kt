package mist.symbolic

import kio.util.toWHex
import mist.ghidra.model.GhidraType
import mist.module.Module
import mist.module.ModuleSymbol
import java.io.File

class TraceWriter {
  fun writeToFile(
    module: Module,
    trace: Trace,
    file: File,
    traceCompareMessages: List<TraceComparator.Message>,
    proveMessages: List<String>
  ) {
    file.writeText(buildString {
      append(writeToString(traceCompareMessages))
      if (proveMessages.isNotEmpty()) {
        append(proveMessages.joinToString(separator = "\n", prefix = "Prove messages:\n", postfix = "\n\n"))
      }
      append("Trace:\n")
      append(writeToString(module, trace))
      append("\n")
    })
  }

  private fun writeToString(module: Module, trace: Trace): String {
    var level = 0
    return trace.elements
      .filterNot {
        (it is TraceElement.MemoryRead && (it.address as Expr.Const).value in Engine.assumedSpRange) ||
          (it is TraceElement.MemoryWrite && (it.address as Expr.Const).value in Engine.assumedSpRange)
      }
      .filter { it !is TraceElement.Branch }
      .joinToString(separator = "\n", postfix = "\n") {
        val prefix = "[${it.pc.toWHex()}] " + "%03d|".format(level)
        val suffix = if (it is TraceSyncPoint) "\n" else ""
        when (it) {
          is TraceElement.FunctionCall -> level++
          is TraceElement.FunctionReturn -> level--
          else -> {} // ignore
        }
        prefix + writeElementToString(module, trace.additionalAllocations, it) + suffix
      }
  }

  fun writeElementToString(module: Module, additionalAllocations: List<Pair<ModuleSymbol, GhidraType?>>, element: TraceElement): String {
    return when (element) {
      is TraceElement.ExecutionStart -> {
        "start args: ${element.arguments.joinToString(separator = ", ")}"
      }
      is TraceElement.FunctionCall -> {
        val knownSuffix = if (element.known) "" else " [!!! unknown args count !!!]"
        "${element.name}(${element.arguments.joinToString(separator = ", ")})$knownSuffix"
      }
      is TraceElement.FunctionReturn -> {
        when {
          element.returnsV1() -> "return ${element.v1}, ${element.v0}"
          element.returnsV0() -> "return ${element.v0}"
          element.returnSize == 0 -> "return"
          else -> "return ${element.v1}, ${element.v0} [!!! unknown return size !!!]"
        }
      }
      is TraceElement.JumpOutOfFunctionBody -> "out of body jump at ${element.pc.toWHex()} to ${element.toAddress.toWHex()} from reg ${element.sourceReg}"
      is TraceElement.MemoryRead -> "read${element.size}${if (element.unsigned) "u" else ""}${element.unaligned?.short ?: ""}" +
        "(${module.lookupAddress((element.address as Expr.Const).value, additionalAllocations).toAccessString()}) [value=${element.value}]" +
        (element.shift?.let { " [shift=$it]" } ?: "")
      is TraceElement.MemoryWrite -> "write${element.size}${element.unaligned?.short ?: ""}" +
        "(${module.lookupAddress((element.address as Expr.Const).value, additionalAllocations).toAccessString()}, ${element.value})" +
        (element.shift?.let { " [shift=$it]" } ?: "")
      is TraceElement.Sync -> "sync(${element.value.toWHex()})"
      is TraceElement.Break -> "break(${element.value.toWHex()})"
      is TraceElement.UseK1 -> "useK1() [value=${element.value}]"
      is TraceElement.ModifyK1 -> "modifyK1(${element.value})"
      is TraceElement.DidNotTerminateWithinLimit -> "--- did not terminate within limit, pc=${element.pc.toWHex()} ---"
      else -> error("Unhandled element type: $element")
    }
  }

  private fun writeToString(traceCompareMessages: List<TraceComparator.Message>): String {
    if (traceCompareMessages.isEmpty()) {
      return "No trace compare messages."
    }
    return traceCompareMessages.joinToString(separator = "\n", prefix = "Trace compare messages:\n", postfix = "\n\n")
  }
}
