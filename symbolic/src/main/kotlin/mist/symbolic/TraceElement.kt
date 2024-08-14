package mist.symbolic

import mist.asm.Reg

sealed interface TraceElement {
  val pc: Int

  data class ExecutionStart(
    override val pc: Int,
    val arguments: List<BvExpr>
  ) : TraceElement, TraceSyncPoint

  data class FunctionCall(
    override val pc: Int,
    val address: Int,
    val name: String,
    val known: Boolean,
    val arguments: List<BvExpr>
  ) : TraceElement, TraceSyncPoint

  data class FunctionReturn(
    override val pc: Int,
    val returnSize: Int?,
    val v0: BvExpr,
    val v1: BvExpr
  ) : TraceElement, TraceSyncPoint {
    fun returnsV0() = (returnSize ?: 0) >= 1

    fun returnsV1() = (returnSize ?: 0) >= 2
  }

  data class JumpOutOfFunctionBody(
    override val pc: Int,
    val toAddress: Int,
    val sourceReg: Reg?
  ) : TraceElement, TraceSyncPoint

  data class MemoryRead(
    override val pc: Int,
    val address: BvExpr,
    val size: Int,
    val value: BvExpr,
    val unsigned: Boolean = false,
    val unaligned: UnalignedMemoryAccess? = null,
    val shift: BvExpr? = null
  ) : TraceElement

  data class MemoryWrite(
    override val pc: Int,
    val address: BvExpr,
    val size: Int,
    val value: BvExpr,
    val unaligned: UnalignedMemoryAccess? = null,
    val shift: BvExpr? = null
  ) : TraceElement

  data class Sync(
    override val pc: Int,
    val value: Int
  ) : TraceElement, TraceSyncPoint

  data class Break(
    override val pc: Int,
    val value: Int
  ) : TraceElement, TraceSyncPoint

  data class ModifyK1(
    override val pc: Int,
    val value: BvExpr
  ) : TraceElement

  data class UseK1(
    override val pc: Int,
    val value: BvExpr
  ) : TraceElement

  data class Branch(
    override val pc: Int,
    val taken: Boolean
  ) : TraceElement

  data class DidNotTerminateWithinLimit(
    override val pc: Int
  ) : TraceElement, TraceSyncPoint

  enum class UnalignedMemoryAccess(val short: String) {
    Left("l"),
    Right("r")
  }
}

interface TraceSyncPoint
