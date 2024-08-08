package mist.symbolic

import java.util.concurrent.atomic.AtomicInteger

data class EngineStats(
  var finishedPaths: AtomicInteger = AtomicInteger(0),
  var droppedPaths: AtomicInteger = AtomicInteger(0),
  var satSolverResults: AtomicInteger = AtomicInteger(0),
  var unsatSolverResults: AtomicInteger = AtomicInteger(0),
  var unknownSolverResults: AtomicInteger = AtomicInteger(0),
  var failedJumpResolution: AtomicInteger = AtomicInteger(0),
  var failedFinishedCtxSolve: AtomicInteger = AtomicInteger(0),
  var breaks: AtomicInteger = AtomicInteger(0),
  var executionErrors: AtomicInteger = AtomicInteger(0),
)
