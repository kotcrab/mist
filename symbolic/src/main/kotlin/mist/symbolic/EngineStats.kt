package mist.symbolic

import java.util.concurrent.atomic.AtomicInteger

data class EngineStats(
  val finishedPaths: AtomicInteger = AtomicInteger(0),
  val droppedPaths: AtomicInteger = AtomicInteger(0),
  val satSolverResults: AtomicInteger = AtomicInteger(0),
  val unsatSolverResults: AtomicInteger = AtomicInteger(0),
  val unknownSolverResults: AtomicInteger = AtomicInteger(0),
  val failedJumpResolution: AtomicInteger = AtomicInteger(0),
  val failedFinishedCtxSolve: AtomicInteger = AtomicInteger(0),
  val breaks: AtomicInteger = AtomicInteger(0),
  val executionErrors: AtomicInteger = AtomicInteger(0),
) {
  fun snapshot(): Snapshot {
    return Snapshot(
      finishedPaths = finishedPaths.get(),
      droppedPaths = droppedPaths.get(),
      satSolverResults = satSolverResults.get(),
      unsatSolverResults = unsatSolverResults.get(),
      unknownSolverResults = unknownSolverResults.get(),
      failedJumpResolution = failedJumpResolution.get(),
      failedFinishedCtxSolve = failedFinishedCtxSolve.get(),
      breaks = breaks.get(),
      executionErrors = executionErrors.get(),
    )
  }

  data class Snapshot(
    val finishedPaths: Int,
    val droppedPaths: Int,
    val satSolverResults: Int,
    val unsatSolverResults: Int,
    val unknownSolverResults: Int,
    val failedJumpResolution: Int,
    val failedFinishedCtxSolve: Int,
    val breaks: Int,
    val executionErrors: Int,
  )
}
