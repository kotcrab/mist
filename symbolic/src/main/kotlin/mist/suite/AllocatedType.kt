package mist.suite

import mist.ghidra.model.GhidraType
import mist.module.ModuleTypes
import mist.symbolic.BvExpr
import mist.symbolic.Context
import mist.symbolic.Expr
import mist.symbolic.UnaryOp

class AllocatedType(
  private val ctx: Context,
  private val moduleTypes: ModuleTypes,
  val address: Expr.Const,
  private val type: GhidraType,
) {
  fun writeField(path: String, value: BvExpr) {
    val (fieldOffset, fieldLength) = moduleTypes.lookupStructMember(type, path.split("."))
    ctx.memory.write(address.plus(fieldOffset), fieldLength, value)
  }

  fun fieldExpr(path: String, unsigned: Boolean = false, captureIndex: Int = -1): BvExpr {
    val (fieldOffset, fieldLength) = moduleTypes.lookupStructMember(type, path.split("."))
    return when (fieldLength) {
      1 -> {
        val b0 = Expr.Select.of(address.plus(fieldOffset), captureIndex)
        Expr.Unary.of(if (unsigned) UnaryOp.ZebMem else UnaryOp.SebMem, b0)
      }
      2 -> {
        val b0 = Expr.Select.of(address.plus(fieldOffset), captureIndex)
        val b1 = Expr.Select.of(address.plus(fieldOffset + 1), captureIndex)
        Expr.Unary.of(if (unsigned) UnaryOp.ZehMem else UnaryOp.SehMem, Expr.Concat.of(b1, b0))
      }
      4 -> {
        val b0 = Expr.Select.of(address.plus(fieldOffset), captureIndex)
        val b1 = Expr.Select.of(address.plus(fieldOffset + 1), captureIndex)
        val b2 = Expr.Select.of(address.plus(fieldOffset + 2), captureIndex)
        val b3 = Expr.Select.of(address.plus(fieldOffset + 3), captureIndex)
        Expr.Concat.of(b3, Expr.Concat.of(b2, Expr.Concat.of(b1, b0)))
      }
      else -> error("Can't get field as memory reference, not a standard size: $fieldLength")
    }
  }
}
