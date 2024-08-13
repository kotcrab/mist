package mist.symbolic

import io.ksmt.KContext
import io.ksmt.solver.bitwuzla.KBitwuzlaSolver

class SolverContext : AutoCloseable {
  val kCtx = KContext(
    operationMode = KContext.OperationMode.SINGLE_THREAD, // Disables thread safety
    astManagementMode = KContext.AstManagementMode.NO_GC,
    simplificationMode = KContext.SimplificationMode.SIMPLIFY,
  )
  val solver = KBitwuzlaSolver(kCtx)
  val initialRam = kCtx.mkConst("ram", kCtx.mkArraySort(kCtx.bv32Sort, kCtx.bv8Sort))

  override fun close() {
    solver.close()
    kCtx.close()
  }
}
