package mist.module

import kio.util.toWHex
import mist.simulator.Interpreter
import mist.simulator.Trace
import mist.simulator.TraceElement
import java.io.File

class TraceWriter {
  fun writeToFile(module: Module, trace: Trace, file: File) {
    file.writeText(writeToString(module, trace))
  }

  fun writeToString(module: Module, trace: Trace): String {
    val spRange = (Interpreter.INITIAL_SP - 0x10000)..Interpreter.INITIAL_SP
    var level = 0
    return trace.elements
      .filterNot {
        (it is TraceElement.MemoryRead && it.address in spRange) ||
          (it is TraceElement.MemoryWrite && it.address in spRange)
      }
      .joinToString(separator = "\n", postfix = "\n") {
        val levelStr = "%03d| ".format(level)
        levelStr + when (it) {
          is TraceElement.ExecutionStart -> {
            "start args: ${it.arguments.joinToString(separator = ", ") { arg -> arg.toWHex() }}"
          }
          is TraceElement.FunctionCall -> {
            level++
            val knownSuffix = if (it.known) "" else " [!!! could not determinate args !!!]"
            "${it.name}(${it.arguments.joinToString(separator = ", ") { arg -> arg.toWHex() }})$knownSuffix"
          }
          is TraceElement.FunctionReturn -> {
            level--
            "return ${it.v0.toWHex()}" // , ${it.v1.toWHex()}
          }
          is TraceElement.JumpOutOfFunctionBody -> "out of body jump at ${it.pc.toWHex()} to ${it.address.toWHex()}"
          is TraceElement.MemoryRead -> "read${it.size}${if (it.unsigned) "u" else ""}" +
            "(${module.lookupAddress(it.address)}) [value=${it.value.toWHex()}]"
          is TraceElement.MemoryWrite -> "write${it.size}(${module.lookupAddress(it.address)}, ${it.value.toWHex()})"
          is TraceElement.DidNotTerminateWithinLimit -> "--- did not terminate within limit, pc=${it.pc.toWHex()} ---"
          is TraceElement.Sync -> "sync(${it.value})"
          is TraceElement.UseK1 -> "useK1(${it.value})"
          is TraceElement.ModifyK1 -> "modifyK1(${it.value})"
        }
      }
  }
}
