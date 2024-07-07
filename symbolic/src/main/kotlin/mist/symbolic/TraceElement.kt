package mist.symbolic

import mist.asm.Reg

sealed interface TraceElement {
  val pc: Int

  data class ExecutionStart(override val pc: Int, val arguments: List<Expr>) : TraceElement, TraceSyncPoint

  data class FunctionCall(override val pc: Int, val address: Int, val name: String, val known: Boolean, val arguments: List<Expr>) : TraceElement,
    TraceSyncPoint

  data class FunctionReturn(override val pc: Int, val returnSize: Int?, val v0: Expr, val v1: Expr) : TraceElement, TraceSyncPoint

  data class JumpOutOfFunctionBody(override val pc: Int, val toAddress: Int, val sourceReg: Reg?) : TraceElement, TraceSyncPoint

  data class MemoryRead(override val pc: Int, val address: Expr, val size: Int, val value: Expr, val unsigned: Boolean) : TraceElement

  data class MemoryWrite(override val pc: Int, val address: Expr, val size: Int, val value: Expr) : TraceElement

  data class Sync(override val pc: Int, val value: Int) : TraceElement, TraceSyncPoint

  data class ModifyK1(override val pc: Int, val value: Expr) : TraceElement

  data class UseK1(override val pc: Int, val value: Expr) : TraceElement

  data class DidNotTerminateWithinLimit(override val pc: Int) : TraceElement, TraceSyncPoint
}

interface TraceSyncPoint
