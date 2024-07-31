package mist.symbolic

import kio.util.toWHex
import kmips.Assembler
import kmips.Endianness
import mist.asm.mips.GprReg
import mist.module.ModuleMemory

class ProvidedFunctionHandler(
  override val name: String,
  private val moduleMemory: ModuleMemory,
  assemble: Assembler.() -> Unit,
) : NamedFunctionHandler {
  private val address = moduleMemory.currentBufferAlloc

  init {
    val assembler = Assembler(address, Endianness.Little)
    assembler.assemble()
    val bytes = assembler.assembleAsByteArray()
    moduleMemory.allocate(bytes.size)
    bytes.forEachIndexed { index, byte ->
      moduleMemory.writeByte(address + index, byte.toInt())
    }
  }

  override fun handle(ctx: Context): Boolean {
    ctx.pc = address
    return true
  }
}

class ReplaySymbolicFunctionHandler(
  private val responsesV0: Map<Int, BvExpr>,
  private val responsesV1: Map<Int, BvExpr>,
  private val delegate: SymbolicFunctionHandler,
) : NamedFunctionHandler {
  override val name = delegate.name

  override fun handle(ctx: Context): Boolean {
    delegate.handle(ctx)
    val executionNumber = ctx.functionStates.getAndIncrement("__replay_${name}")
    if (ctx.readGpr(GprReg.V0) is Expr.Const && ctx.readGpr(GprReg.V1) is Expr.Const) {
      return false
    }
    val responseV0 = responsesV0[executionNumber] ?: let {
      println("REPLAY: No response to replay v0 result of $name, execution number $executionNumber, pc=${ctx.pc.toWHex()}")
      Expr.Const.of(delegate.constValueV0)
    }
    val responseV1 = responsesV1[executionNumber] ?: Expr.Const.of(delegate.constValueV1)
    ctx.writeGpr(GprReg.V0, responseV0)
    ctx.writeGpr(GprReg.V1, responseV1)
    return false
  }
}

class SymbolicFunctionHandler(
  override val name: String,
  private val symbolicExecutionLimit: Int = Integer.MAX_VALUE,
  val constValueV0: Int = Engine.DEAD_VALUE,
  val constValueV1: Int = Engine.DEAD_VALUE,
  private val constraints: Context.() -> List<BvExpr> = { emptyList() },
) : NamedFunctionHandler {
  override fun handle(ctx: Context): Boolean {
    val executionNumber = ctx.functionStates.getAndIncrement(name)
    if (executionNumber >= symbolicExecutionLimit) {
      DefaultFunctionHandler.handle(ctx)
      ctx.writeGpr(GprReg.V0, Expr.Const.of(constValueV0))
      ctx.writeGpr(GprReg.V1, Expr.Const.of(constValueV1))
    } else {
      val symbolicVariableV0 = Expr.Symbolic.of("fun:v0:$executionNumber:$name")
      val symbolicVariableV1 = Expr.Symbolic.of("fun:v1:$executionNumber:$name")
      constraints(ctx)
        .takeIf { it.isNotEmpty() }
        ?.map { constraint -> Expr.Condition.of(ConditionOp.Eq, symbolicVariableV0, constraint) }
        ?.reduce { acc, condition -> Expr.Or.of(acc, condition) }
        ?.let { ctx.assume(it) }
      DefaultFunctionHandler.handle(ctx)
      ctx.writeGpr(GprReg.V0, symbolicVariableV0)
      ctx.writeGpr(GprReg.V1, symbolicVariableV1)
    }
    return false
  }
}

open class ResultFunctionHandler(
  override val name: String,
  private val resultProvider: Context.() -> BvExpr = { Expr.DEAD_VALUE }
) : NamedFunctionHandler {
  override fun handle(ctx: Context): Boolean {
    val result = resultProvider(ctx)
    DefaultFunctionHandler.handle(ctx)
    ctx.writeGpr(GprReg.V0, result)
    return false
  }
}

object DefaultFunctionHandler : FunctionHandler {
  private val DEAD_VALUE = Expr.Const.of(Engine.DEAD_VALUE)

  private val nullifiedRegisters = arrayOf(
    GprReg.At,
    GprReg.V0, GprReg.V1,
    GprReg.A0, GprReg.A1, GprReg.A2, GprReg.A3,
    GprReg.T0, GprReg.T1, GprReg.T2, GprReg.T3, GprReg.T4, GprReg.T5, GprReg.T6, GprReg.T7, GprReg.T8, GprReg.T9,
  )

  override fun handle(ctx: Context): Boolean {
    nullifiedRegisters.forEach {
      ctx.writeGpr(it, DEAD_VALUE)
      ctx.lo = DEAD_VALUE
      ctx.hi = DEAD_VALUE
    }
    return false
  }
}

interface NamedFunctionHandler : FunctionHandler {
  val name: String
}

interface FunctionHandler {
  fun handle(ctx: Context): Boolean
}
