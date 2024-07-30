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
  private val responses: Map<Int, BvExpr>,
  private val delegate: SymbolicFunctionHandler,
) : NamedFunctionHandler {
  override val name = delegate.name

  override fun handle(ctx: Context): Boolean {
    delegate.handle(ctx)
    val executionNumber = ctx.functionStates.getAndIncrement("__replay_${name}")
    val result = ctx.readGpr(GprReg.V0)
    if (result !is Expr.Const) {
      val overrideResult = responses[executionNumber] ?: let {
        println("REPLAY: No response to replay result of $name, execution number $executionNumber, pc=${ctx.pc.toWHex()}")
        Expr.Const.of(delegate.constValue)
      }
      ctx.writeGpr(GprReg.V0, overrideResult)
    }
    return false
  }
}

class SymbolicFunctionHandler(
  name: String,
  symbolicExecutionLimit: Int = Integer.MAX_VALUE,
  val constValue: Int = Engine.DEAD_VALUE,
  private val constraints: Context.() -> List<BvExpr> = { emptyList() },
) : ResultFunctionHandler(name, {
  val executionNumber = functionStates.getAndIncrement(name)
  if (executionNumber >= symbolicExecutionLimit) {
    Expr.Const.of(constValue)
  } else {
    val symbolicVariable = Expr.Symbolic.of("fun:v0:$executionNumber:$name")
    constraints()
      .takeIf { it.isNotEmpty() }
      ?.map { constraint -> Expr.Condition.of(ConditionOp.Eq, symbolicVariable, constraint) }
      ?.reduce { acc, condition -> Expr.Or.of(acc, condition) }
      ?.let { assume(it) }
    symbolicVariable
  }
})

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
