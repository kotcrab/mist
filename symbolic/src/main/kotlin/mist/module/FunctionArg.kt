package mist.module

import mist.asm.mips.GprReg
import mist.symbolic.BinaryOp
import mist.symbolic.BvExpr
import mist.symbolic.Context
import mist.symbolic.Expr

sealed interface FunctionArg {
  class Reg(private val reg: GprReg) : FunctionArg {
    override fun read(ctx: Context): BvExpr {
      return ctx.readGpr(reg)
    }
  }

  class Stack(private val offset: Int) : FunctionArg {
    override fun read(ctx: Context): BvExpr {
      return ctx.memory.readWord(Expr.Binary.of(BinaryOp.Add, ctx.readGpr(GprReg.Sp), Expr.Const.of(offset)))
    }
  }

  fun read(ctx: Context): BvExpr
}
