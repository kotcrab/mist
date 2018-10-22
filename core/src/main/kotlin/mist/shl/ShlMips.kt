/*
 * mist - interactive mips disassembler and decompiler
 * Copyright (C) 2018 Pawel Pastuszak
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package mist.shl

import kio.util.RuntimeTypeAdapterFactory
import kio.util.runtimeTypeAdapter
import kio.util.toHex
import kio.util.toInt
import mist.asm.FpuReg
import mist.asm.Reg
import mist.shl.ShlExpr.*
import kotlin.math.*

/** @author Kotcrab */

val shlArgRegisters = listOf("a0", "a1", "a2", "a3", "t0", "t1", "t2", "t3")

abstract class ShlInstr constructor(internal var addr: Int) {
    private constructor() : this(0x0) // GSON no-arg constructor

    companion object {
        fun provideGsonTypeAdapter(): RuntimeTypeAdapterFactory<ShlInstr> {
            return runtimeTypeAdapter(base = ShlInstr::class,
                    subTypes = arrayOf(
                            ShlMemStoreInstr::class,
                            ShlStructStoreInstr::class,
                            ShlAssignInstr::class,
                            ShlJumpInstr::class,
                            ShlCallInstr::class,
                            ShlBranchInstr::class,
                            ShlNopInstr::class),
                    legacySubTypes = arrayOf(
                            "SHLMemStoreInstr" to ShlMemStoreInstr::class,
                            "SHLStructStoreInstr" to ShlStructStoreInstr::class,
                            "SHLAssignInstr" to ShlAssignInstr::class,
                            "SHLJumpInstr" to ShlJumpInstr::class,
                            "SHLCallInstr" to ShlCallInstr::class,
                            "SHLBranchInstr" to ShlBranchInstr::class,
                            "SHLNopInstr" to ShlNopInstr::class)
            )
        }
    }

    var deadCode = false
    var comment = ""

    @Transient
    val prevDataFlowCtx = DataFlowCtx()
    @Transient
    val dataFlowCtx = DataFlowCtx()
    @Transient
    val reachFlowCtx = ReachFlowCtx()
    @Transient
    val prevReachFlowCtx = ReachFlowCtx()
    @Transient
    val liveFlowCtx = LiveFlowCtx()
    @Transient
    val nextLiveFlowCtx = LiveFlowCtx()

    /** Returns expression that is evaluated by the instruction to perform instruction operation. */
    abstract fun getReadExpr(): ShlExpr?

    /**
     * Returns expression that will be changed after instruction was executed. Write expression can only
     * contain single variable.
     */
    abstract fun getWriteExpr(): ShlExpr?

    /** Returns list of variables that is reset to unknown state after executing this instruction. */
    abstract fun getNullifiedVars(): Array<String>?

    /** Performs substitution of [srcExpr] with [newExpr]. Both write and read expression are changed. When [exact]
     * is true, then [srcExpr] must be exactly the same object as expression that should be replaced. */
    fun substituteExprs(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
        substituteReadExpr(srcExpr, exact, newExpr)
        substituteWriteExpr(srcExpr, exact, newExpr)
    }

    /** Performs substitution of [srcExpr] with [newExpr]. Only read expression is changed. When [exact]
     * is true, then [srcExpr] must be exactly the same object as expression that should be replaced. */
    abstract fun substituteReadExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr)

    /** Performs substitution of [srcExpr] with [newExpr]. Only write expression is changed. When [exact]
     * is true, then [srcExpr] must be exactly the same object as expression that should be replaced. */
    abstract fun substituteWriteExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr)

}

class ShlMemStoreInstr constructor(addr: Int, var expr: ShlMemStore) : ShlInstr(addr) {
    // GSON no-arg constructor
    private constructor() : this(0x0, ShlMemStore(ShlMemOpcode.Sw, ShlConst(0), ShlConst(0)))

    override fun getReadExpr(): ShlExpr = expr
    override fun getWriteExpr(): ShlExpr? = null
    override fun getNullifiedVars(): Array<String>? = null

    override fun substituteReadExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
        expr = expr.substitute(srcExpr, exact, newExpr) as ShlMemStore
    }

    override fun substituteWriteExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
    }

    override fun toString(): String {
        return "$expr"
    }
}

class ShlStructStoreInstr constructor(addr: Int, var expr: ShlStructStore) : ShlInstr(addr) {
    // GSON no-arg constructor
    private constructor() : this(0x0, ShlStructStore(ShlMemOpcode.Sw, -1, ShlConst(0), ShlConst(0)))

    override fun getReadExpr(): ShlExpr = expr
    override fun getWriteExpr(): ShlExpr? = null
    override fun getNullifiedVars(): Array<String>? = null

    override fun substituteReadExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
        expr = expr.substitute(srcExpr, exact, newExpr) as ShlStructStore
    }

    override fun substituteWriteExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
    }

    override fun toString(): String {
        return "$expr"
    }
}

class ShlAssignInstr constructor(addr: Int, var dest: ShlExpr, var src: ShlExpr) : ShlInstr(addr) {
    private constructor() : this(0x0, ShlConst(0), ShlConst(0)) // GSON no-arg constructor

    override fun getReadExpr() = src
    override fun getWriteExpr() = if (dest is ShlMemLoad) null else dest
    override fun getNullifiedVars(): Array<String>? = null

    override fun substituteReadExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
        src = src.substitute(srcExpr, exact, newExpr)
    }

    override fun substituteWriteExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
        if (dest is ShlMemLoad) return
        dest = dest.substitute(srcExpr, exact, newExpr)
    }

    override fun toString(): String {
        return "$dest = $src"
    }
}

class ShlJumpInstr constructor(addr: Int, var link: Boolean, var dest: ShlExpr) : ShlInstr(addr) {
    private constructor() : this(0x0, false, ShlConst(0)) // GSON no-arg constructor

    override fun getReadExpr() = dest
    override fun getWriteExpr(): ShlExpr? = null
    override fun getNullifiedVars(): Array<String> = if (link) arrayOf(
            Reg.at, Reg.v0, Reg.v1,
            Reg.a0, Reg.a1, Reg.a2, Reg.a3,
            Reg.t0, Reg.t1, Reg.t2, Reg.t3, Reg.t4, Reg.t5, Reg.t6, Reg.t7, Reg.t8, Reg.t9,
            Reg.hi, Reg.lo).map { it.toString() }.toTypedArray() else emptyArray()

    override fun toString(): String {
        return "j${if (link) "al" else ""} $dest"
    }

    override fun substituteReadExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
        dest = dest.substitute(srcExpr, exact, newExpr)
    }

    override fun substituteWriteExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
    }
}

class ShlCallInstr constructor(addr: Int, var returnReg: ShlVar?, var callExpr: ShlCall) : ShlInstr(addr) {
    private constructor() : this(0x0, null, ShlCall(0)) // GSON no-arg constructor

    override fun getReadExpr(): ShlExpr = callExpr
    override fun getWriteExpr(): ShlExpr? = returnReg
    override fun getNullifiedVars(): Array<String> {
        val args = mutableListOf(
                Reg.at, Reg.v0, Reg.v1,
                Reg.a0, Reg.a1, Reg.a2, Reg.a3,
                Reg.t0, Reg.t1, Reg.t2, Reg.t3, Reg.t4, Reg.t5, Reg.t6, Reg.t7, Reg.t8, Reg.t9,
                Reg.hi, Reg.lo)
        returnReg?.let {
            if (it.varName == "v0" || it.varName == "v1") {
                args.remove(Reg.valueOf(it.varName))
            }
        }
        return args.map { it.toString() }.toTypedArray()
    }

    override fun substituteReadExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
        callExpr = callExpr.substitute(srcExpr, exact, newExpr) as ShlCall
    }

    override fun substituteWriteExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
        returnReg?.let {
            returnReg = it.substitute(srcExpr, exact, newExpr) as ShlVar
        }
    }

    override fun toString(): String {
        return if (returnReg != null) {
            "$returnReg = $callExpr"
        } else {
            "$callExpr"
        }
    }
}

class ShlBranchInstr constructor(addr: Int, var likely: Boolean, var cond: ShlExpr, var link: Boolean = false) : ShlInstr(addr) {
    private constructor() : this(0x0, false, ShlCall(0), false) // GSON no-arg constructor

    override fun getReadExpr() = cond
    override fun getWriteExpr(): ShlExpr? = null
    override fun getNullifiedVars(): Array<String>? = null

    override fun toString(): String {
        return "b${if (link) "al" else ""}${if (likely) "l" else ""} $cond"
    }

    override fun substituteReadExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
        cond = cond.substitute(srcExpr, exact, newExpr)
    }

    override fun substituteWriteExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
    }
}

class ShlNopInstr constructor(addr: Int) : ShlInstr(addr) {
    private constructor() : this(0x0) // GSON no-arg constructor

    override fun getReadExpr(): ShlExpr? = null
    override fun getWriteExpr(): ShlExpr? = null
    override fun getNullifiedVars(): Array<String>? = null

    override fun toString(): String {
        return "nop"
    }

    override fun substituteReadExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
    }

    override fun substituteWriteExpr(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr) {
    }
}

sealed class ShlExpr {
    companion object {
        fun provideGsonTypeAdapter(): RuntimeTypeAdapterFactory<ShlExpr> {
            return runtimeTypeAdapter(base = ShlExpr::class,
                    subTypes = arrayOf(
                            ShlVar::class,
                            ShlConst::class,
                            ShlString::class,
                            ShlGlobalRef::class,
                            ShlFuncPointer::class,
                            ShlFloat::class,
                            ShlMemLoad::class,
                            ShlMemStore::class,
                            ShlStructLoad::class,
                            ShlStructStore::class,
                            ShlCall::class,

                            ShlUnaryExpr::class,
                            ShlNeg::class,
                            ShlAbs::class,
                            ShlSqrt::class,
                            ShlRound::class,
                            ShlTrunc::class,
                            ShlCeil::class,
                            ShlFloor::class,
                            ShlToInt::class,
                            ShlToFloat::class,

                            ShlBinaryExpr::class,
                            ShlAdd::class,
                            ShlSub::class,
                            ShlMul::class,
                            ShlDiv::class,
                            ShlAnd::class,
                            ShlOr::class,
                            ShlXor::class,
                            ShlSll::class,
                            ShlSrl::class,
                            ShlSra::class,
                            ShlLessThan::class,
                            ShlLessEqualThan::class,
                            ShlGreaterThan::class,
                            ShlGreaterEqualThan::class,
                            ShlEqual::class,
                            ShlNotEqual::class),
                    legacySubTypes = arrayOf(
                            "SHLVar" to ShlVar::class,
                            "SHLConst" to ShlConst::class,
                            "SHLString" to ShlString::class,
                            "SHLGlobalRef" to ShlGlobalRef::class,
                            "SHLFuncPointer" to ShlFuncPointer::class,
                            "SHLFloat" to ShlFloat::class,
                            "SHLMemLoad" to ShlMemLoad::class,
                            "SHLMemStore" to ShlMemStore::class,
                            "SHLStructLoad" to ShlStructLoad::class,
                            "SHLStructStore" to ShlStructStore::class,
                            "SHLCall" to ShlCall::class,

                            "SHLUnaryExpr" to ShlUnaryExpr::class,
                            "SHLNeg" to ShlNeg::class,
                            "SHLAbs" to ShlAbs::class,
                            "SHLSqrt" to ShlSqrt::class,
                            "SHLRound" to ShlRound::class,
                            "SHLTrunc" to ShlTrunc::class,
                            "SHLCeil" to ShlCeil::class,
                            "SHLFloor" to ShlFloor::class,
                            "SHLToInt" to ShlToInt::class,
                            "SHLToFloat" to ShlToFloat::class,

                            "SHLBinaryExpr" to ShlBinaryExpr::class,
                            "SHLAdd" to ShlAdd::class,
                            "SHLSub" to ShlSub::class,
                            "SHLMul" to ShlMul::class,
                            "SHLDiv" to ShlDiv::class,
                            "SHLAnd" to ShlAnd::class,
                            "SHLOr" to ShlOr::class,
                            "SHLXor" to ShlXor::class,
                            "SHLSll" to ShlSll::class,
                            "SHLSrl" to ShlSrl::class,
                            "SHLSra" to ShlSra::class,
                            "SHLLessThan" to ShlLessThan::class,
                            "SHLLessEqualThan" to ShlLessEqualThan::class,
                            "SHLGreaterThan" to ShlGreaterThan::class,
                            "SHLGreaterEqualThan" to ShlGreaterEqualThan::class,
                            "SHLEqual" to ShlEqual::class,
                            "SHLNotEqual" to ShlNotEqual::class)
            )
        }
    }

    // Higher level Shl expressions

    class ShlAdd(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("+", true, left, right) {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlAdd(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            if (evalLeft is ShlConst && evalLeft.value == 0) {
                return evalRight
            }
            if (evalRight is ShlConst && evalRight.value == 0) {
                return evalLeft
            }
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst(evalLeft.value + evalRight.value)
            } else {
                mutate(evalLeft, evalRight)
            }
        }
    }

    class ShlSub(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("-", false, left, right) {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlSub(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            if (evalRight is ShlConst && evalRight.value == 0) {
                return evalLeft
            }
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst(evalLeft.value - evalRight.value)
            } else {
                mutate(evalLeft, evalRight)
            }
        }
    }

    class ShlMul(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("*", true, left, right) {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlMul(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst(evalLeft.value * evalRight.value)
            } else {
                mutate(evalLeft, evalRight)
            }
        }
    }

    class ShlDiv(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("/", false, left, right) {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlDiv(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst(evalLeft.value / evalRight.value)
            } else {
                mutate(evalLeft, evalRight)
            }
        }
    }

    class ShlNeg(expr: ShlExpr) : ShlUnaryExpr("~", false, expr) {
        override fun mutate(newExpr: ShlExpr) = ShlNeg(newExpr)

        override fun evaluate(): ShlExpr {
            val evalExpr = expr.evaluate()
            return when (evalExpr) {
                is ShlConst -> ShlConst(evalExpr.value.inv())
                else -> evalExpr
            }
        }
    }

    class ShlAnd(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("&", true, left, right) {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlAnd(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst(evalLeft.value and evalRight.value)
            } else {
                mutate(evalLeft, evalRight)
            }
        }
    }

    class ShlOr(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("|", true, left, right) {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlOr(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst(evalLeft.value or evalRight.value)
            } else {
                mutate(evalLeft, evalRight)
            }
        }
    }

    class ShlXor(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("^", true, left, right) {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlXor(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst(evalLeft.value xor evalRight.value)
            } else {
                mutate(evalLeft, evalRight)
            }
        }
    }

    class ShlSll(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("<<", false, left, right) {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlSll(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst(evalLeft.value shl evalRight.value)
            } else {
                mutate(evalLeft, evalRight)
            }
        }
    }

    class ShlSrl(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr(">>>", false, left, right) {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlSrl(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst(evalLeft.value ushr evalRight.value)
            } else {
                mutate(evalLeft, evalRight)
            }
        }
    }

    class ShlSra(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr(">>", false, left, right) {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlSra(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst(evalLeft.value shr evalRight.value)
            } else {
                mutate(evalLeft, evalRight)
            }
        }
    }

    class ShlLessThan(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("<", false, left, right), ShlEquality {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlLessThan(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst((evalLeft.value < evalRight.value).toInt())
            } else {
                mutate(evalLeft, evalRight)
            }
        }

        override fun negate(): ShlExpr {
            return ShlGreaterEqualThan(left, right)
        }

        override fun flip(): ShlExpr {
            return ShlGreaterThan(right, left)
        }
    }

    class ShlLessEqualThan(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("<=", false, left, right), ShlEquality {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlLessEqualThan(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst((evalLeft.value <= evalRight.value).toInt())
            } else {
                mutate(evalLeft, evalRight)
            }
        }

        override fun negate(): ShlExpr {
            return ShlGreaterThan(left, right)
        }

        override fun flip(): ShlExpr {
            return ShlGreaterEqualThan(right, left)
        }
    }

    class ShlGreaterThan(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr(">", false, left, right), ShlEquality {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlGreaterThan(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst((evalLeft.value > evalRight.value).toInt())
            } else {
                mutate(evalLeft, evalRight)
            }
        }

        override fun negate(): ShlExpr {
            return ShlLessEqualThan(left, right)
        }

        override fun flip(): ShlExpr {
            return ShlLessThan(right, left)
        }
    }

    class ShlGreaterEqualThan(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr(">=", false, left, right), ShlEquality {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlGreaterEqualThan(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst((evalLeft.value >= evalRight.value).toInt())
            } else {
                mutate(evalLeft, evalRight)
            }
        }

        override fun negate(): ShlExpr {
            return ShlLessThan(left, right)
        }

        override fun flip(): ShlExpr {
            return ShlLessEqualThan(right, left)
        }
    }

    class ShlEqual(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("==", true, left, right), ShlEquality {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlEqual(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst((evalLeft.value == evalRight.value).toInt())
            } else {
                mutate(evalLeft, evalRight)
            }
        }

        override fun negate(): ShlExpr {
            return ShlNotEqual(left, right)
        }

        override fun flip(): ShlExpr {
            return ShlEqual(right, left)
        }
    }

    class ShlNotEqual(left: ShlExpr, right: ShlExpr) : ShlBinaryExpr("!=", true, left, right), ShlEquality {
        override fun mutate(newLeft: ShlExpr, newRight: ShlExpr) = ShlNotEqual(newLeft, newRight)

        override fun evaluate(): ShlExpr {
            val evalLeft = left.evaluate()
            val evalRight = right.evaluate()
            return if (evalLeft is ShlConst && evalRight is ShlConst) {
                ShlConst((evalLeft.value != evalRight.value).toInt())
            } else {
                mutate(evalLeft, evalRight)
            }
        }

        override fun negate(): ShlExpr {
            return ShlEqual(left, right)
        }

        override fun flip(): ShlExpr {
            return ShlNotEqual(right, left)
        }
    }

    class ShlAbs(expr: ShlExpr) : ShlUnaryExpr("abs", true, expr) {
        override fun mutate(newExpr: ShlExpr) = ShlAbs(newExpr)

        override fun evaluate(): ShlExpr {
            val evalExpr = expr.evaluate()
            return when (evalExpr) {
                is ShlFloat -> ShlFloat(abs(evalExpr.value))
                //TODO not sure if ShlConst should be supported, this is an FPU instruction
                else -> evalExpr
            }
        }
    }

    class ShlSqrt(expr: ShlExpr) : ShlUnaryExpr("sqrt", true, expr) {
        override fun mutate(newExpr: ShlExpr) = ShlSqrt(newExpr)

        override fun evaluate(): ShlExpr {
            val evalExpr = expr.evaluate()
            return when (evalExpr) {
                is ShlFloat -> ShlFloat(sqrt(evalExpr.value))
                //TODO not sure if ShlConst should be supported, this is an FPU instruction
                else -> evalExpr
            }
        }
    }

    class ShlRound(expr: ShlExpr) : ShlUnaryExpr("round", true, expr) {
        override fun mutate(newExpr: ShlExpr) = ShlRound(newExpr)

        override fun evaluate(): ShlExpr {
            val evalExpr = expr.evaluate()
            return when (evalExpr) {
                is ShlFloat -> ShlFloat(round(evalExpr.value))
                else -> evalExpr
            }
        }
    }

    class ShlTrunc(expr: ShlExpr) : ShlUnaryExpr("trunc", true, expr) {
        override fun mutate(newExpr: ShlExpr) = ShlTrunc(newExpr)

        override fun evaluate(): ShlExpr {
            val evalExpr = expr.evaluate()
            return when (evalExpr) {
                is ShlFloat -> ShlFloat(truncate(evalExpr.value))
                else -> evalExpr
            }
        }
    }

    class ShlCeil(expr: ShlExpr) : ShlUnaryExpr("ceil", true, expr) {
        override fun mutate(newExpr: ShlExpr) = ShlCeil(newExpr)

        override fun evaluate(): ShlExpr {
            val evalExpr = expr.evaluate()
            return when (evalExpr) {
                is ShlFloat -> ShlFloat(ceil(evalExpr.value))
                else -> evalExpr
            }
        }
    }

    class ShlFloor(expr: ShlExpr) : ShlUnaryExpr("floor", true, expr) {
        override fun mutate(newExpr: ShlExpr) = ShlFloor(newExpr)

        override fun evaluate(): ShlExpr {
            val evalExpr = expr.evaluate()
            return when (evalExpr) {
                is ShlFloat -> ShlFloat(floor(evalExpr.value))
                else -> evalExpr
            }
        }
    }

    class ShlToInt(expr: ShlExpr) : ShlUnaryExpr("toInt", true, expr) {
        override fun mutate(newExpr: ShlExpr) = ShlToInt(newExpr)

        override fun evaluate(): ShlExpr {
            //TODO FpuCvtWS opcode is not used in test image so just ignoring this for now
            return expr.evaluate()
        }
    }

    class ShlToFloat(expr: ShlExpr) : ShlUnaryExpr("toFloat", true, expr) {
        override fun mutate(newExpr: ShlExpr) = ShlToFloat(newExpr)

        override fun evaluate(): ShlExpr {
            val evalExpr = expr.evaluate()
            return when (evalExpr) {
                is ShlConst -> ShlFloat(Float.fromBits(evalExpr.value))
                else -> evalExpr
            }
        }
    }

    // Base level Shl expressions

    class ShlVar(val varName: String) : ShlExpr(), ShlSubstitutable {
        constructor(reg: Reg) : this(reg.toString())
        constructor(reg: FpuReg) : this(reg.toString())

        override fun getUsedVars() = arrayOf(varName)

        override fun toString(): String {
            return varName
        }

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            if (srcExpr !is ShlVar) return this
            if (exact && this !== srcExpr) return this
            if (varName != srcExpr.varName) return this
            return newExpr
        }

        override fun evaluate(): ShlExpr {
            return this
        }

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            if (varName != (other as ShlVar).varName) return false
            return true
        }
    }

    class ShlConst(val value: Int) : ShlExpr(), ShlSubstitutable {
        override fun getUsedVars(): Array<String> = emptyArray()

        override fun toString() = value.toHex()

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            if (srcExpr !is ShlConst) return this
            if (exact && this !== srcExpr) return this
            if (value != srcExpr.value) return this
            return newExpr
        }

        override fun evaluate(): ShlExpr = this

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            if (value != (other as ShlConst).value) return false
            return true
        }
    }

    class ShlString(val addr: Int, val value: String) : ShlExpr(), ShlSubstitutable {
        override fun getUsedVars(): Array<String> = emptyArray()

        override fun toString() = "\"$value\""

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            if (srcExpr !is ShlString) return this
            if (exact && this !== srcExpr) return this
            if (addr != srcExpr.addr) return this
            if (value != srcExpr.value) return this
            return newExpr
        }

        override fun evaluate(): ShlExpr = this

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            if (value != (other as ShlString).value) return false
            return true
        }
    }

    class ShlGlobalRef(val addr: Int) : ShlExpr(), ShlSubstitutable {
        override fun getUsedVars(): Array<String> = emptyArray()

        override fun toString() = "((__global)${addr.toHex()})"

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            if (srcExpr !is ShlGlobalRef) return this
            if (exact && this !== srcExpr) return this
            if (addr != srcExpr.addr) return this
            return newExpr
        }

        override fun evaluate(): ShlExpr = this

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            if (addr != (other as ShlGlobalRef).addr) return false
            return true
        }
    }

    class ShlFuncPointer(val addr: Int) : ShlExpr(), ShlSubstitutable {
        override fun getUsedVars(): Array<String> = emptyArray()

        override fun toString() = "((__func*)${addr.toHex()})"

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            if (srcExpr !is ShlFuncPointer) return this
            if (exact && this !== srcExpr) return this
            if (addr != srcExpr.addr) return this
            return newExpr
        }

        override fun evaluate(): ShlExpr = this

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            if (addr != (other as ShlFuncPointer).addr) return false
            return true
        }
    }

    class ShlFloat(val value: Float) : ShlExpr(), ShlSubstitutable {
        override fun getUsedVars(): Array<String> = emptyArray()

        override fun toString() = value.toString()

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            if (srcExpr !is ShlFloat) return this
            if (exact && this !== srcExpr) return this
            if (value != srcExpr.value) return this
            return newExpr
        }

        override fun evaluate(): ShlExpr = this

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            if (value != (other as ShlFloat).value) return false
            return true
        }
    }

    // Shl expressions combiners

    class ShlMemLoad(val op: ShlMemOpcode, val expr: ShlExpr) : ShlExpr(), ShlCompoundExpr {
        override fun getUsedVars() = arrayOf(*expr.getUsedVars())

        init {
            if (op.load == false) error("only load memory operation is allowed")
        }

        override fun toString(): String {
            return "$op[${expr.toExprString()}]"
        }

        override fun evaluate(): ShlExpr {
            return ShlMemLoad(op, expr.evaluate())
        }

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            return ShlMemLoad(op, expr.substitute(srcExpr, exact, newExpr))
        }

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            val otherExpr = other as ShlMemLoad
            if (op != otherExpr.op) return false
            if (!expr.compareExpr(otherExpr.expr)) return false
            return true
        }
    }

    class ShlMemStore(val op: ShlMemOpcode, val memExpr: ShlExpr, val valExpr: ShlExpr) : ShlExpr(), ShlCompoundExpr {
        override fun getUsedVars() = arrayOf(*memExpr.getUsedVars(), *valExpr.getUsedVars())

        init {
            if (op.store == false) error("only store memory operation is allowed")
        }

        override fun toString(): String {
            return "$op[${memExpr.toExprString()}] = ${valExpr.toExprString()}"
        }

        override fun evaluate(): ShlExpr {
            return ShlMemStore(op, memExpr.evaluate(), valExpr.evaluate())
        }

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            return ShlMemStore(op, memExpr.substitute(srcExpr, exact, newExpr), valExpr.substitute(srcExpr, exact, newExpr))
        }

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            val otherExpr = other as ShlMemStore
            if (op != otherExpr.op) return false
            if (!memExpr.compareExpr(otherExpr.memExpr)) return false
            if (!valExpr.compareExpr(otherExpr.valExpr)) return false
            return true
        }
    }

    class ShlStructLoad(val op: ShlMemOpcode, val refTid: Int, val expr: ShlExpr) : ShlExpr(), ShlCompoundExpr {
        override fun getUsedVars() = arrayOf(*expr.getUsedVars())

        init {
            if (op.load == false) error("only load memory operation is allowed")
        }

        override fun toString(): String {
            return "$op(__struct$refTid, at = ${expr.toExprString()})"
        }

        override fun evaluate(): ShlExpr {
            return ShlStructLoad(op, refTid, expr.evaluate())
        }

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            return ShlStructLoad(op, refTid, expr.substitute(srcExpr, exact, newExpr))
        }

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            val otherExpr = other as ShlStructLoad
            if (op != otherExpr.op) return false
            if (refTid != otherExpr.refTid) return false
            if (!expr.compareExpr(otherExpr.expr)) return false
            return true
        }
    }

    class ShlStructStore(val op: ShlMemOpcode, val refTid: Int, val memExpr: ShlExpr, val valExpr: ShlExpr) : ShlExpr(), ShlCompoundExpr {
        override fun getUsedVars() = arrayOf(*memExpr.getUsedVars(), *valExpr.getUsedVars())

        init {
            if (op.store == false) error("only store memory operation is allowed")
        }

        override fun toString(): String {
            return "$op(__struct$refTid, at = ${memExpr.toExprString()}, store = ${valExpr.toExprString()})"
        }

        override fun evaluate(): ShlExpr {
            return ShlStructStore(op, refTid, memExpr.evaluate(), valExpr.evaluate())
        }

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            return ShlStructStore(op, refTid, memExpr.substitute(srcExpr, exact, newExpr), valExpr.substitute(srcExpr, exact, newExpr))
        }

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            val otherExpr = other as ShlStructStore
            if (op != otherExpr.op) return false
            if (refTid != otherExpr.refTid) return false
            if (!memExpr.compareExpr(otherExpr.memExpr)) return false
            if (!valExpr.compareExpr(otherExpr.valExpr)) return false
            return true
        }
    }

    class ShlCall(val dest: Int, val args: MutableMap<String, ShlExpr> = mutableMapOf()) : ShlExpr() {
        override fun toString(): String {
            return "((__func*)${dest.toHex()})(${argsToCallString()})"
        }

        fun toCallString(funcName: String): String {
            return "$funcName(${argsToCallString()})"
        }

        private fun argsToCallString(): String {
            return args.map { Pair(Reg.valueOf(it.key).id, "${it.key} = ${it.value}") }
                    .sortedBy { it.first }
                    .joinToString { it.second }
        }

        override fun getUsedVars(): Array<String> {
            val vars = mutableListOf<String>()
            args.forEach { _, expr ->
                vars.addAll(expr.getUsedVars())
            }
            return vars.toTypedArray()
        }

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            val newArgs = mutableMapOf<String, ShlExpr>()
            args.forEach { reg, expr ->
                newArgs[reg] = expr.substitute(srcExpr, exact, newExpr)
            }
            return ShlCall(dest, newArgs)
        }

        override fun evaluate(): ShlCall {
            val newArgs = mutableMapOf<String, ShlExpr>()
            args.forEach { reg, expr ->
                newArgs[reg] = expr.evaluate()
            }
            return ShlCall(dest, newArgs)
        }

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            val otherExpr = other as ShlCall
            if (dest != otherExpr.dest) return false
            val allArgs = mutableListOf<String>()
            allArgs.addAll(args.keys)
            allArgs.addAll(other.args.keys)
            allArgs.forEach { arg ->
                val expr1 = args[arg]
                val expr2 = other.args[arg]
                if (expr1 == null || expr2 == null) return false
                if (!expr1.compareExpr(expr2)) return false
            }
            return true
        }
    }

    abstract class ShlUnaryExpr(val op: String, val wrapParenthesis: Boolean, val expr: ShlExpr) : ShlExpr(), ShlCompoundExpr {
        override fun getUsedVars() = arrayOf(*expr.getUsedVars())

        override fun toString(): String {
            return if (wrapParenthesis) "$op(${expr.toExprString()})" else "$op${expr.toExprString()}"
        }

        protected abstract fun mutate(newExpr: ShlExpr): ShlExpr

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            return mutate(expr.substitute(srcExpr, exact, newExpr))
        }

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            val otherExpr = other as ShlUnaryExpr
            if (op != otherExpr.op) return false
            if (wrapParenthesis != otherExpr.wrapParenthesis) return false
            if (!expr.compareExpr(otherExpr.expr)) return false
            return true
        }
    }

    abstract class ShlBinaryExpr(val op: String, val commutative: Boolean, val left: ShlExpr, val right: ShlExpr) : ShlExpr(), ShlCompoundExpr {
        override fun getUsedVars() = arrayOf(*left.getUsedVars(), *right.getUsedVars())

        override fun toString(): String {
            return "${left.toExprString()} $op ${right.toExprString()}"
        }

        protected abstract fun mutate(newLeft: ShlExpr, newRight: ShlExpr): ShlExpr

        override fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr {
            return mutate(left.substitute(srcExpr, exact, newExpr), right.substitute(srcExpr, exact, newExpr))
        }

        override fun compareExpr(other: ShlExpr): Boolean {
            if (javaClass != other.javaClass) return false
            val otherExpr = other as ShlBinaryExpr
            if (op != otherExpr.op) return false
            if (commutative != otherExpr.commutative) return false
            if (commutative) {
                return ((left.compareExpr(otherExpr.left) && right.compareExpr(otherExpr.right))
                        || (left.compareExpr(otherExpr.right) && right.compareExpr(otherExpr.left)))
            } else {
                return left.compareExpr(otherExpr.left) && right.compareExpr(otherExpr.right)
            }
        }
    }

    interface ShlCompoundExpr

    interface ShlSubstitutable

    interface ShlEquality {
        fun negate(): ShlExpr
        fun flip(): ShlExpr
    }

    abstract fun substitute(srcExpr: ShlSubstitutable, exact: Boolean, newExpr: ShlExpr): ShlExpr
    abstract fun getUsedVars(): Array<String>
    abstract fun evaluate(): ShlExpr

    fun toExprString(): String {
        return if (this is ShlCompoundExpr) "(${this})" else "${this}"
    }

    abstract fun compareExpr(other: ShlExpr): Boolean
}

enum class ShlMemOpcode(val store: Boolean = false, val load: Boolean = false, val accessSize: Int) {
    Lb(load = true, accessSize = 1),
    Lbu(load = true, accessSize = 1),
    Sb(store = true, accessSize = 1),
    Lh(load = true, accessSize = 2),
    Lhu(load = true, accessSize = 2),
    Sh(store = true, accessSize = 2),
    Lw(load = true, accessSize = 4),
    Sw(store = true, accessSize = 4),
    Lwl(load = true, accessSize = 4),
    Lwr(load = true, accessSize = 4),
    Swl(store = true, accessSize = 4),
    Swr(store = true, accessSize = 4),
    Ll(load = true, accessSize = 4),
    Sc(store = true, accessSize = 4),
    //Lwc1(read = true), -> lifter remaps to lw
    //Swc1(write = true), -> lifter remaps to sw
}
