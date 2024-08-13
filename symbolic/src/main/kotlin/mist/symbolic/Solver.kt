package mist.symbolic

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.*
import io.ksmt.utils.cast
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Solver(
  private val ctx: Context,
  private val defaultSolverTimeout: Duration = 3.seconds,
  private val solverCtx: SolverContext = SolverContext(),
  private val symbolicNamePrefix: String = ""
) : AutoCloseable {
  private val kCtx = solverCtx.kCtx
  private val solver = solverCtx.solver

  private val bvExprCache = mutableMapOf<BvExpr, KExpr<out KBvSort>>()
  private val boolExprCache = mutableMapOf<BoolExpr, KExpr<KBoolSort>>()

  private val initialRam = solverCtx.initialRam
  private val memoryExpressions: MutableList<MemoryKExpr> = mutableListOf()

  fun <T> make(block: KContext.(namePrefix: String) -> T): T {
    return block(kCtx, symbolicNamePrefix)
  }

  fun cachedSolverExpr(expr: BvExpr): KExpr<out KBvSort> {
    return bvExprCache.getOrPut(expr) { expr.newSolverExpr(this) }
  }

  fun cachedSolverExpr(expr: BoolExpr): KExpr<KBoolSort> {
    return boolExprCache.getOrPut(expr) { expr.newSolverExpr(this) }
  }

  fun memoryExpressionAt(index: Int): MemoryKExpr {
    return if (index < 0) initialRam else memoryExpressions[index]
  }

  fun updateMemoryExpressions() {
    ctx.memory.captures.subList(memoryExpressions.size, ctx.memory.captures.size).forEach { stores ->
      val newExpression = stores.fold(memoryExpressions.lastOrNull() ?: initialRam) { acc, store ->
        val storeValue = when (store.value) {
          is Expr.Const -> kCtx.mkBv(store.value.value.toByte())
          else -> cachedSolverExpr(store.value)
        }
        kCtx.mkArrayStore(acc, cachedSolverExpr(store.address).cast(), storeValue.cast())
      }
      memoryExpressions.add(newExpression)
    }
  }

  fun assert(expr: BoolExpr) {
    updateMemoryExpressions()
    solver.assert(cachedSolverExpr(expr))
  }

  fun check(timeout: Duration? = null): KSolverStatus {
    updateMemoryExpressions()
    return solver.check(timeout ?: defaultSolverTimeout)
  }

  fun eval(expr: BvExpr, isComplete: Boolean): KExpr<out KBvSort> {
    return model().eval(cachedSolverExpr(expr), isComplete)
  }

  fun model(): KModel {
    return solver.model()
  }

  override fun close() {
    solverCtx.close()
  }
}

typealias MemoryKExpr = KExpr<KArraySort<KBv32Sort, KBv8Sort>>
