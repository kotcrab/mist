package mist.suite

import mist.ghidra.model.GhidraType
import mist.module.ModuleTypes
import mist.symbolic.BvExpr
import mist.symbolic.Context
import mist.symbolic.Expr
import mist.symbolic.UnaryOp

class TypedAllocation(
  private val ctx: Context,
  private val moduleTypes: ModuleTypes,
  val address: Expr.Const,
  private val type: GhidraType,
  private val elements: Int,
) {
  fun writeField(path: String, value: Int) {
    writeField(path, Expr.Const.of(value))
  }

  fun writeField(path: String, typeAddress: TypedAllocation) {
    writeField(path, typeAddress.address)
  }

  fun writeField(path: String, value: BvExpr) {
    val (elementOffset, memberPath) = parsePath(path)
    val (fieldOffset, fieldLength) = moduleTypes.lookupStructMember(type, memberPath)
    ctx.memory.write(address.plus(elementOffset + fieldOffset), fieldLength, value)
  }

  fun fieldExpr(path: String, unsigned: Boolean = false, captureIndex: Int = -1): BvExpr {
    val (elementOffset, memberPath) = parsePath(path)
    val (fieldOffset, fieldLength) = moduleTypes.lookupStructMember(type, memberPath)
    val baseAddress = address.plus(elementOffset + fieldOffset)
    return when (fieldLength) {
      1 -> {
        val b0 = Expr.Select.of(baseAddress, captureIndex)
        Expr.Unary.of(if (unsigned) UnaryOp.ZebMem else UnaryOp.SebMem, b0)
      }
      2 -> {
        val b0 = Expr.Select.of(baseAddress, captureIndex)
        val b1 = Expr.Select.of(baseAddress.plus(1), captureIndex)
        Expr.Unary.of(if (unsigned) UnaryOp.ZehMem else UnaryOp.SehMem, Expr.Concat.of(b1, b0))
      }
      4 -> {
        val b0 = Expr.Select.of(baseAddress, captureIndex)
        val b1 = Expr.Select.of(baseAddress.plus(1), captureIndex)
        val b2 = Expr.Select.of(baseAddress.plus(2), captureIndex)
        val b3 = Expr.Select.of(baseAddress.plus(3), captureIndex)
        Expr.Concat.of(b3, Expr.Concat.of(b2, Expr.Concat.of(b1, b0)))
      }
      else -> error("Can't get field as memory reference, not a standard size: $fieldLength")
    }
  }

  private fun parsePath(path: String): Pair<Int, List<String>> {
    val parts = path.split("|", limit = 2)
    val memberPath = parts.last().split(".")
    return if (parts.size == 1) {
      0 to memberPath
    } else {
      val index = Integer.decode(parts[0])
      if (index > elements) {
        error("Out of bounds array access, $index can't be larger than $elements")
      }
      index * type.length to memberPath
    }
  }
}
