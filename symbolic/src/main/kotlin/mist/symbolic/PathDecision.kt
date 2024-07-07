package mist.symbolic

data class PathDecision(
  val fromAddress: Int,
  val toAddress: Int,
  val condition: BoolExpr,
)
