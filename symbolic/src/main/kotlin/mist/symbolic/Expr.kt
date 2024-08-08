package mist.symbolic

import io.ksmt.expr.KExpr
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBv32Sort
import io.ksmt.sort.KBvSort
import io.ksmt.utils.cast
import kio.util.toUnsignedLong
import kio.util.toWHex
import kotlin.math.max
import kotlin.math.min

sealed interface Expr {
  companion object {
    val ZERO = Const.of(0)
    val DEAD_VALUE = Const.of(0xDEADBEEF.toInt())
    val TRUE = Bool.of(true)
    val FALSE = Bool.of(false)
  }

  fun newSolverExpr(solver: Solver): KExpr<*>

  fun getComponents(): List<Expr>

  class Const private constructor(val value: Int) : BvExpr {
    companion object {
      fun of(value: Int): Const {
        return Const(value)
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      mkBv(value)
    }

    override fun getComponents(): List<Expr> {
      return emptyList()
    }

    override fun toString(): String {
      return value.toWHex()
    }
  }

  class Symbolic private constructor(val name: String) : BvExpr {
    companion object {
      fun of(name: String): Symbolic {
        return Symbolic(name)
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      mkConst(name, bv32Sort)
    }

    override fun getComponents(): List<Expr> {
      return emptyList()
    }

    override fun toString(): String {
      return name
    }
  }

  class Select private constructor(val address: BvExpr, val captureIndex: Int) : BvExpr {
    companion object {
      fun of(address: BvExpr, captureIndex: Int): Select {
        return Select(address, captureIndex)
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      mkArraySelect(solver.memoryExpressionAt(captureIndex), solver.cachedSolverExpr(address).cast())
    }

    override fun getComponents(): List<Expr> {
      return listOf(address)
    }

    override fun toString(): String {
      return "Select(address=$address, captureIndex=$captureIndex)"
    }
  }

  class Store private constructor(val address: BvExpr, val value: BvExpr) : BvExpr {
    companion object {
      fun of(address: BvExpr, value: BvExpr): Store {
        return Store(address, value)
      }
    }

    override fun newSolverExpr(solver: Solver) = error("Not implemented for Store")

    override fun getComponents(): List<Expr> {
      return listOf(address, value)
    }

    override fun toString(): String {
      return "Store(address=$address, value=$value)"
    }
  }

  class Concat private constructor(val msb: BvExpr, val lsb: BvExpr) : BvExpr {
    companion object {
      fun of(msb: BvExpr, lsb: BvExpr): Concat {
        return Concat(msb, lsb)
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      mkBvConcatExpr(solver.cachedSolverExpr(msb), solver.cachedSolverExpr(lsb))
    }

    override fun getComponents(): List<Expr> {
      return listOf(msb, lsb)
    }

    override fun toString(): String {
      return "Concat(msb=$msb, lsb=$lsb)"
    }
  }

  class Extract private constructor(val value: BvExpr, val high: Int, val low: Int) : BvExpr {
    companion object {
      fun of(value: BvExpr, high: Int, low: Int): BvExpr {
        require(high >= low)
        require(high - low + 1 == 8) { error("Extract can only produce 8 bit wide expression") }
        return if (value is Const) {
          val leftShift = 32 - high - 1
          Const.of((value.value shl (32 - high - 1)) ushr (low + leftShift))
        } else {
          Extract(value, high, low)
        }
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      mkBvExtractExpr(high, low, solver.cachedSolverExpr(value))
    }

    override fun getComponents(): List<Expr> {
      return listOf(value)
    }

    override fun toString(): String {
      return "Extract(value=$value, high=$high, low=$low)"
    }
  }

  class ExtractZeroExtend private constructor(val value: BvExpr, val high: Int, val low: Int) : BvExpr {
    companion object {
      fun of(value: BvExpr, high: Int, low: Int): BvExpr {
        require(high >= low)
        return if (value is Const) {
          val leftShift = 32 - high - 1
          Const.of((value.value shl (32 - high - 1)) ushr (low + leftShift))
        } else {
          ExtractZeroExtend(value, high, low)
        }
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      mkBvZeroExtensionExpr(32 - (high - low + 1), mkBvExtractExpr(high, low, solver.cachedSolverExpr(value)))
    }

    override fun getComponents(): List<Expr> {
      return listOf(value)
    }

    override fun toString(): String {
      return "ExtractZeroExtend(value=$value, high=$high, low=$low)"
    }
  }

  class Insert private constructor(val destination: BvExpr, val source: BvExpr, val pos: Int, val size: Int) : BvExpr {
    companion object {
      fun of(destination: BvExpr, source: BvExpr, pos: Int, size: Int): BvExpr {
        return if (destination is Const && source is Const) {
          val rsMask = -1 ushr (32 - size)
          val rtMask = (rsMask shl pos).inv()
          Const.of((destination.value and rtMask) or (source.value and rsMask shl pos))
        } else {
          Insert(destination, source, pos, size)
        }
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      val rsMask = -1 ushr (32 - size)
      val rtMask = (rsMask shl pos).inv()
      mkBvOrExpr(
        mkBvAndExpr(solver.cachedSolverExpr(destination).cast(), mkBv(rtMask)),
        mkBvShiftLeftExpr(mkBvAndExpr(solver.cachedSolverExpr(source).cast(), mkBv(rsMask)), mkBv(pos)),
      )
    }

    override fun getComponents(): List<Expr> {
      return listOf(destination, source)
    }

    override fun toString(): String {
      return "Insert(destination=$destination, source=$source, pos=$pos, size=$size)"
    }
  }

  class Binary private constructor(val op: BinaryOp, val left: BvExpr, val right: BvExpr) : BvExpr {
    companion object {
      fun of(op: BinaryOp, left: BvExpr, right: BvExpr): BvExpr {
        // Simplifies move pseudo-instruction
        if (op == BinaryOp.Add && right is Const && right.value == 0) {
          return left
        }

        return if (left is Const && right is Const) {
          val l = left.value
          val r = right.value
          val newValue = when (op) {
            BinaryOp.Add -> l + r
            BinaryOp.Sub -> l - r
            BinaryOp.Max -> max(l, r)
            BinaryOp.Min -> min(l, r)
            BinaryOp.Slt -> if (l < r) 1 else 0
            BinaryOp.Sltu -> if (l.toUInt() < r.toUInt()) 1 else 0
            BinaryOp.And -> l and r
            BinaryOp.Or -> l or r
            BinaryOp.Xor -> l xor r
            BinaryOp.Nor -> (l or r).inv()
            BinaryOp.Sll -> l shl r
            BinaryOp.Srl -> l ushr r
            BinaryOp.Sra -> l shr r
            BinaryOp.MultLo -> (l.toLong() * r.toLong()).toInt()
            BinaryOp.MultHi -> ((l.toLong() * r.toLong()) ushr 32).toInt()
            BinaryOp.MultuLo -> (l.toUnsignedLong() * r.toUnsignedLong()).toInt()
            BinaryOp.MultuHi -> ((l.toUnsignedLong() * r.toUnsignedLong()) ushr 32).toInt()
            BinaryOp.Div -> if (r == 0) 0x77777777 else l / r
            BinaryOp.Divu -> if (r == 0) 0x77777777 else (l.toUInt() / r.toUInt()).toInt()
            BinaryOp.Mod -> if (r == 0) 0x77777777 else l % r
            BinaryOp.Modu -> if (r == 0) 0x77777777 else (l.toUInt() % r.toUInt()).toInt()
          }
          Const.of(newValue)
        } else {
          Binary(op, left, right)
        }
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      val l: KExpr<KBv32Sort> = solver.cachedSolverExpr(left).cast()
      val r: KExpr<KBv32Sort> = solver.cachedSolverExpr(right).cast()
      when (op) {
        BinaryOp.Add -> mkBvAddExpr(l, r)
        BinaryOp.Sub -> mkBvSubExpr(l, r)
        BinaryOp.Max -> mkIte(mkBvSignedGreaterExpr(l, r), l, r)
        BinaryOp.Min -> mkIte(mkBvSignedLessExpr(l, r), l, r)
        BinaryOp.Slt -> mkIte(mkBvSignedLessExpr(l, r), mkBv(1), mkBv(0))
        BinaryOp.Sltu -> mkIte(mkBvUnsignedLessExpr(l, r), mkBv(1), mkBv(0))
        BinaryOp.And -> mkBvAndExpr(l, r)
        BinaryOp.Or -> mkBvOrExpr(l, r)
        BinaryOp.Xor -> mkBvXorExpr(l, r)
        BinaryOp.Nor -> mkBvNorExpr(l, r)
        BinaryOp.Sll -> mkBvShiftLeftExpr(l, r)
        BinaryOp.Srl -> mkBvLogicalShiftRightExpr(l, r)
        BinaryOp.Sra -> mkBvArithShiftRightExpr(l, r)
        BinaryOp.MultLo -> mkBvExtractExpr(31, 0, mkBvMulExpr(mkBvSignExtensionExpr(32, l), mkBvSignExtensionExpr(32, r)))
        BinaryOp.MultHi -> mkBvExtractExpr(63, 32, mkBvMulExpr(mkBvSignExtensionExpr(32, l), mkBvSignExtensionExpr(32, r)))
        BinaryOp.MultuLo -> mkBvExtractExpr(31, 0, mkBvMulExpr(mkBvZeroExtensionExpr(32, l), mkBvZeroExtensionExpr(32, r)))
        BinaryOp.MultuHi -> mkBvExtractExpr(63, 32, mkBvMulExpr(mkBvZeroExtensionExpr(32, l), mkBvZeroExtensionExpr(32, r)))
        BinaryOp.Div -> mkBvSignedDivExpr(l, r)
        BinaryOp.Divu -> mkBvUnsignedDivExpr(l, r)
        BinaryOp.Mod -> mkBvSignedRemExpr(l, r) // TODO mod is actually rem in MIPS right?
        BinaryOp.Modu -> mkBvUnsignedRemExpr(l, r)
      }
    }

    override fun getComponents(): List<Expr> {
      return listOf(left, right)
    }

    override fun toString(): String {
      return "Binary(op=$op, left=$left, right=$right)"
    }
  }

  class Unary private constructor(val op: UnaryOp, val value: BvExpr) : BvExpr {
    companion object {
      fun of(op: UnaryOp, value: BvExpr): BvExpr {
        return if (value is Const) {
          val v = value.value
          val newValue = when (op) {
            UnaryOp.ZebMem -> v
            UnaryOp.SebMem -> v shl 24 shr 24
            UnaryOp.ZehMem -> v
            UnaryOp.SehMem -> v shl 16 shr 16
            UnaryOp.Seb -> v shl 24 shr 24
            UnaryOp.Seh -> v shl 16 shr 16
          }
          Const.of(newValue)
        } else {
          Unary(op, value)
        }
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      val v = solver.cachedSolverExpr(value)
      when (op) {
        UnaryOp.ZebMem -> mkBvZeroExtensionExpr(24, v)
        UnaryOp.SebMem -> mkBvSignExtensionExpr(24, v)
        UnaryOp.ZehMem -> mkBvZeroExtensionExpr(16, v)
        UnaryOp.SehMem -> mkBvSignExtensionExpr(16, v)
        UnaryOp.Seb -> mkBvSignExtensionExpr(24, mkBvExtractExpr(7, 0, v))
        UnaryOp.Seh -> mkBvSignExtensionExpr(16, mkBvExtractExpr(15, 0, v))
      }
    }

    override fun getComponents(): List<Expr> {
      return listOf(value)
    }

    override fun toString(): String {
      return "Unary(op=$op, value=$value)"
    }
  }

  class ValueIf private constructor(val condition: BoolExpr, val trueValue: BvExpr, val falseValue: BvExpr) : BvExpr {
    companion object {
      fun of(condition: BoolExpr, trueValue: BvExpr, falseValue: BvExpr): BvExpr {
        return if (condition is Bool) {
          if (condition.value) {
            trueValue
          } else {
            falseValue
          }
        } else {
          ValueIf(condition, trueValue, falseValue)
        }
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      val t: KExpr<KBv32Sort> = solver.cachedSolverExpr(trueValue).cast()
      val f: KExpr<KBv32Sort> = solver.cachedSolverExpr(falseValue).cast()
      mkIte(solver.cachedSolverExpr(condition), t, f)
    }

    override fun getComponents(): List<Expr> {
      return listOf(condition, trueValue, falseValue)
    }

    override fun toString(): String {
      return "ValueIf(condition=$condition, trueValue=$trueValue, falseValue=$falseValue)"
    }
  }

  class Condition private constructor(val op: ConditionOp, val left: BvExpr, val right: BvExpr) : BoolExpr {
    companion object {
      fun of(op: ConditionOp, left: BvExpr, right: BvExpr): BoolExpr {
        return if (left is Const && right is Const) {
          val l = left.value
          val r = right.value
          when (op) {
            ConditionOp.Eq -> if (l == r) TRUE else FALSE
            ConditionOp.Neq -> if (l != r) TRUE else FALSE
            ConditionOp.Ge -> if (l >= r) TRUE else FALSE
            ConditionOp.Gt -> if (l > r) TRUE else FALSE
            ConditionOp.Le -> if (l <= r) TRUE else FALSE
            ConditionOp.Lt -> if (l < r) TRUE else FALSE
          }
        } else {
          Condition(op, left, right)
        }
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      val l: KExpr<KBv32Sort> = solver.cachedSolverExpr(left).cast()
      val r: KExpr<KBv32Sort> = solver.cachedSolverExpr(right).cast()
      when (op) {
        ConditionOp.Eq -> mkEq(l, r)
        ConditionOp.Neq -> mkNot(mkEq(l, r))
        ConditionOp.Ge -> mkBvSignedGreaterOrEqualExpr(l, r)
        ConditionOp.Gt -> mkBvSignedGreaterExpr(l, r)
        ConditionOp.Le -> mkBvSignedLessOrEqualExpr(l, r)
        ConditionOp.Lt -> mkBvSignedLessExpr(l, r)
      }
    }

    override fun getComponents(): List<Expr> {
      return listOf(left, right)
    }

    override fun toString(): String {
      return "Condition(op=$op, left=$left, right=$right)"
    }
  }

  class And private constructor(val left: BoolExpr, val right: BoolExpr) : BoolExpr {
    companion object {
      fun of(left: BoolExpr, right: BoolExpr): BoolExpr {
        return if (left is Bool && right is Bool) {
          Bool.of(left.value and right.value)
        } else {
          And(left, right)
        }
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      val l: KExpr<KBoolSort> = solver.cachedSolverExpr(left).cast()
      val r: KExpr<KBoolSort> = solver.cachedSolverExpr(right).cast()
      mkAnd(l, r)
    }

    override fun getComponents(): List<Expr> {
      return listOf(left, right)
    }

    override fun toString(): String {
      return "And(left=$left, right=$right)"
    }
  }

  class Or private constructor(val left: BoolExpr, val right: BoolExpr) : BoolExpr {
    companion object {
      fun of(left: BoolExpr, right: BoolExpr): BoolExpr {
        return if (left is Bool && right is Bool) {
          Bool.of(left.value or right.value)
        } else {
          Or(left, right)
        }
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      val l: KExpr<KBoolSort> = solver.cachedSolverExpr(left).cast()
      val r: KExpr<KBoolSort> = solver.cachedSolverExpr(right).cast()
      mkOr(l, r)
    }

    override fun getComponents(): List<Expr> {
      return listOf(left, right)
    }

    override fun toString(): String {
      return "Or(left=$left, right=$right)"
    }
  }

  class Not private constructor(val value: BoolExpr) : BoolExpr {
    companion object {
      fun of(value: BoolExpr): BoolExpr {
        return if (value is Bool) {
          value.negated()
        } else {
          Not(value)
        }
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      mkNot(solver.cachedSolverExpr(value))
    }

    override fun getComponents(): List<Expr> {
      return listOf(value)
    }

    override fun toString(): String {
      return "Not(value=$value)"
    }
  }

  class Bool private constructor(val value: Boolean) : BoolExpr {
    companion object {
      fun of(value: Boolean): Bool {
        return Bool(value)
      }
    }

    override fun newSolverExpr(solver: Solver) = solver.make {
      if (value) mkTrue() else mkFalse()
    }

    override fun getComponents(): List<Expr> {
      return emptyList()
    }

    fun negated(): Bool {
      return if (value) FALSE else TRUE
    }

    override fun toString(): String {
      return "Bool(value=$value)"
    }
  }
}

interface BvExpr : Expr {
  override fun newSolverExpr(solver: Solver): KExpr<out KBvSort>
}

interface BoolExpr : Expr {
  override fun newSolverExpr(solver: Solver): KExpr<KBoolSort>
}
