package mist.simulator

sealed class TraceElement {
  data class ExecutionStart(val arguments: List<Int>) : TraceElement()

  data class MemoryRead(val address: Int, val size: Int, val value: Int, val unsigned: Boolean) : TraceElement()

  data class MemoryWrite(val address: Int, val size: Int, val value: Int) : TraceElement()

  data class FunctionCall(val address: Int, val name: String, val known: Boolean, val arguments: List<Int>) : TraceElement()

  data class JumpOutOfFunctionBody(val pc: Int, val address: Int) : TraceElement()

  data class FunctionReturn(val v0: Int, val v1: Int) : TraceElement()

  data class DidNotTerminateWithinLimit(val pc: Int) : TraceElement()

  data class Sync(val value: Int) : TraceElement()

  data class ModifyK1(val value: Int) : TraceElement()

  data class UseK1(val value: Int) : TraceElement()
}
