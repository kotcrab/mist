package mist.symbolic

import mist.asm.mips.GprReg

class ExprBuilder(private val ctx: Context) {
  val a0 = GprReg.A0.expr
  val a1 = GprReg.A1.expr
  val a2 = GprReg.A2.expr
  val a3 = GprReg.A3.expr
  val t0 = GprReg.T0.expr
  val t1 = GprReg.T1.expr
  val t2 = GprReg.T2.expr
  val t3 = GprReg.T3.expr

  inline val Int.expr get() = Expr.Const.of(this)

  val GprReg.expr get() = ctx.readGpr(this)

  infix fun BvExpr.eqOrNull(right: BvExpr): BoolExpr {
    return (this eq right) or (this eq 0.expr)
  }

  infix fun BvExpr.eq(right: BvExpr): BoolExpr {
    return Expr.Condition.of(ConditionOp.Eq, this, right)
  }

  infix fun BvExpr.neq(right: BvExpr): BoolExpr {
    return Expr.Not.of(Expr.Condition.of(ConditionOp.Eq, this, right))
  }

  infix fun BvExpr.ge(right: BvExpr): BoolExpr {
    return Expr.Condition.of(ConditionOp.Ge, this, right)
  }

  infix fun BvExpr.gt(right: BvExpr): BoolExpr {
    return Expr.Condition.of(ConditionOp.Gt, this, right)
  }

  infix fun BvExpr.le(right: BvExpr): BoolExpr {
    return Expr.Condition.of(ConditionOp.Le, this, right)
  }

  infix fun BvExpr.lt(right: BvExpr): BoolExpr {
    return Expr.Condition.of(ConditionOp.Lt, this, right)
  }

  infix fun BoolExpr.and(right: BoolExpr): BoolExpr {
    return Expr.And.of(this, right)
  }

  infix fun BoolExpr.or(right: BoolExpr): BoolExpr {
    return Expr.Or.of(this, right)
  }
}
