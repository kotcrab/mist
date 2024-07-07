package mist.symbolic

import io.ksmt.expr.KExpr
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.KBvSort
import mist.asm.Reg
import mist.asm.mips.GprReg
import kotlin.time.Duration

class Context private constructor(
  private val gpr: Array<BvExpr>,
  var lo: BvExpr,
  var hi: BvExpr,
  var pc: Int = 0,
  val memory: Memory,
  private val paths: MutableList<PathDecision>,
  private val assumptions: MutableList<BoolExpr>,
  private var solver: Solver?,
  val functionStates: FunctionStates,
  val symbolicBranches: BranchCounters,
  var executedInstrs: Int,
  val executedAddresses: MutableSet<Int>,
  val traceElements: MutableList<TraceElement>,
) {
  companion object {
    fun presetSymbolic(): Context {
      val ctx = Context()
      repeat(32) {
        ctx.writeGpr(GprReg.forId(it), Expr.Symbolic.of(GprReg.forId(it).name))
      }
      ctx.writeGpr(GprReg.S0, Expr.Const.of(Engine.DEAD_VALUE))
      ctx.writeGpr(GprReg.S1, Expr.Const.of(Engine.DEAD_VALUE))
      ctx.writeGpr(GprReg.S2, Expr.Const.of(Engine.DEAD_VALUE))
      ctx.writeGpr(GprReg.S3, Expr.Const.of(Engine.DEAD_VALUE))
      ctx.writeGpr(GprReg.S4, Expr.Const.of(Engine.DEAD_VALUE))
      ctx.writeGpr(GprReg.S5, Expr.Const.of(Engine.DEAD_VALUE))
      ctx.writeGpr(GprReg.S6, Expr.Const.of(Engine.DEAD_VALUE))
      ctx.writeGpr(GprReg.S7, Expr.Const.of(Engine.DEAD_VALUE))
      ctx.writeGpr(GprReg.Fp, Expr.Const.of(Engine.DEAD_VALUE))
      ctx.lo = Expr.Symbolic.of("lo")
      ctx.hi = Expr.Symbolic.of("hi")
      return ctx
    }
  }

  constructor() : this(
    gpr = Array(32) { Expr.ZERO },
    lo = Expr.ZERO,
    hi = Expr.ZERO,
    pc = 0,
    memory = Memory(),
    paths = mutableListOf(),
    assumptions = mutableListOf(),
    solver = null,
    functionStates = FunctionStates(),
    symbolicBranches = BranchCounters(),
    executedInstrs = 0,
    executedAddresses = mutableSetOf(),
    traceElements = mutableListOf(),
  ) {
    gpr[0] = Expr.ZERO
  }

  fun copyOf(): Context {
    return Context(
      gpr = gpr.copyOf(),
      lo = lo,
      hi = hi,
      pc = pc,
      memory = memory.copyOf(),
      paths = paths.toMutableList(),
      assumptions = assumptions.toMutableList(),
      solver = null,
      functionStates = functionStates.copyOf(),
      symbolicBranches = symbolicBranches.copyOf(),
      executedInstrs = executedInstrs,
      executedAddresses = executedAddresses.toMutableSet(),
      traceElements = traceElements.toMutableList(),
    )
  }

  fun readGpr(reg: Reg): BvExpr {
    require(reg is GprReg)
    return gpr[reg.id]
  }

  fun writeGpr(reg: Reg, value: BvExpr) {
    require(reg is GprReg)
    if (reg.id == 0) {
      return
    }
    gpr[reg.id] = value
  }

  fun assume(expr: BoolExpr) {
    if (solver != null) {
      error("Assumptions should be added before solver is created")
    }
    assumptions.add(expr)
  }

  fun addPathDecision(path: PathDecision) {
    paths.add(path)
    solver?.assert(path.condition)
  }

  fun lastPathDecision(offset: Int = 0): PathDecision? {
    return paths.getOrNull(paths.size - 1 - offset)
  }

  fun checkSolver(timeout: Duration? = null): KSolverStatus {
    return getOrCreateSolverContext().check(timeout)
  }

  fun solverModel(): KModel {
    return getOrCreateSolverContext().model()
  }

  fun solverEval(expr: BvExpr, isComplete: Boolean): KExpr<out KBvSort> {
    return getOrCreateSolverContext().eval(expr, isComplete)
  }

  inline fun <T> useSolver(block: () -> T): T {
    return try {
      block()
    } finally {
      closeSolver()
    }
  }

  fun closeSolver() {
    solver?.close()
    solver = null
  }

  private fun getOrCreateSolverContext(): Solver {
    if (solver == null) {
      solver = Solver(this).also { newSolver ->
        assumptions.forEach {
          newSolver.assert(it)
        }
        paths.forEach { path ->
          newSolver.assert(path.condition)
        }
      }
    }
    return solver!!
  }
}
