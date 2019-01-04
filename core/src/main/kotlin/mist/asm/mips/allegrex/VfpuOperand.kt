/*
 * mist - interactive disassembler and decompiler
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

package mist.asm.mips.allegrex

import mist.asm.Operand

/** @author Kotcrab */

class VfpuCmpOperand(val mode: VfpuCmpMode) : Operand() {
    override fun toString(): String {
        return mode.toString()
    }
}

enum class VfpuCmpMode(val id: Int) {
    FL(0),
    EQ(1),
    LT(2),
    LE(3),
    TR(4),
    NE(5),
    GE(6),
    GT(7),
    EZ(8),
    EN(9),
    EI(10),
    ES(11),
    NZ(12),
    NN(13),
    NI(14),
    NS(15);

    companion object {
        fun forId(modeId: Int): VfpuCmpMode {
            return values().first { it.id == modeId % 16 }
        }
    }
}


class VfpuCstOperand(val mode: VfpuCstMode) : Operand() {
    override fun toString(): String {
        return mode.stringRep
    }
}

enum class VfpuCstMode(val id: Int, val stringRep: String) {
    Undef(0, "(undef)"),
    MaxFloat(1, "MaxFloat"),
    Sqrt2(2, "Sqrt(2)"),
    Sqrt12(3, "Sqrt(1/2)"),
    Div2BySqrtPI(4, "2/Sqrt(PI)"),
    Div2ByPI(5, "2/PI"),
    Div1ByPI(6, "1/PI"),
    DivPIBy4(7, "PI/4"),
    DivPIBy2(8, "PI/2"),
    PI(9, "PI"),
    E(10, "e"),
    Log2Ofe(11, "Log2(e)"),
    Log10Ofe(12, "Log10(e)"),
    Ln2(13, "ln(2)"),
    Ln10(14, "ln(10)"),
    Times2PI(15, "2*PI"),
    DivPIBy6(16, "PI/6"),
    Log10Of2(17, "Log10(2)"),
    Log2Of10(18, "Log2(10)"),
    DivSqrt3By2(19, "Sqrt(3)/2"),
    ;

    companion object {
        fun forId(modeId: Int): VfpuCstMode {
            return values().firstOrNull { it.id == modeId } ?: Undef
        }
    }
}

class VfpuWbOperand : Operand() {
    override fun toString(): String {
        return "wb"
    }
}


class VfpuVpfxstOperand(
    val op1: VpfxstOp,
    val op2: VpfxstOp,
    val op3: VpfxstOp,
    val op4: VpfxstOp
) : Operand() {
    companion object {
        fun decode(instr: Int): VfpuVpfxstOperand {
            return VfpuVpfxstOperand(
                VpfxstOp.decode(instr, 0), VpfxstOp.decode(instr, 1),
                VpfxstOp.decode(instr, 2), VpfxstOp.decode(instr, 3)
            )
        }
    }

    override fun toString(): String {
        return "[$op1, $op2, $op3, $op4]"
    }
}

class VpfxstOp(val op: VpfxstOpBase, val negate: Boolean, val abs: Boolean) {
    companion object {
        fun decode(instr: Int, idx: Int): VpfxstOp {
            val opNum = instr ushr (idx * 2) and 3
            val abs = (instr ushr (8 + idx) and 1) == 1
            val negate = (instr ushr (16 + idx) and 1) == 1
            val constant = (instr ushr (12 + idx) and 1) == 1
            return if (constant) {
                VpfxstOp(VpfxstOpBase.constForId(if (abs) opNum + 4 else opNum), negate, false)
            } else {
                VpfxstOp(VpfxstOpBase.regForId(opNum), negate, abs)
            }
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (negate) sb.append("-")
        if (abs && !op.constant) sb.append("|")
        sb.append(op.toString())
        if (abs && !op.constant) sb.append("|")
        return sb.toString()
    }
}

enum class VpfxstOpBase(val id: Int, val constant: Boolean, val stringRep: String) {
    X(0, false, "X"),
    Y(1, false, "Y"),
    Z(2, false, "Z"),
    W(3, false, "W"),
    CZero(0, true, "0"),
    COne(1, true, "1"),
    CTwo(2, true, "2"),
    CHalf(3, true, "1/2"),
    CThree(4, true, "3"),
    CThird(5, true, "1/3"),
    CFourth(6, true, "1/4"),
    CSixth(7, true, "1/6");

    companion object {
        fun regForId(id: Int): VpfxstOpBase {
            return values().first { it.id == id && !it.constant }
        }

        fun constForId(id: Int): VpfxstOpBase {
            return values().first { it.id == id && it.constant }
        }
    }

    override fun toString(): String {
        return stringRep
    }
}


class VfpuVpfxdOperand(
    val op1: VpfxdOp,
    val op2: VpfxdOp,
    val op3: VpfxdOp,
    val op4: VpfxdOp
) : Operand() {
    companion object {
        fun decode(instr: Int): VfpuVpfxdOperand {
            return VfpuVpfxdOperand(
                VpfxdOp.decode(instr, 0), VpfxdOp.decode(instr, 1),
                VpfxdOp.decode(instr, 2), VpfxdOp.decode(instr, 3)
            )
        }
    }

    override fun toString(): String {
        return "[$op1, $op2, $op3, $op4]"
    }
}

class VpfxdOp(val op: VpfxdOpBase, val mask: Boolean) {
    companion object {
        fun decode(instr: Int, idx: Int): VpfxdOp {
            val opNum = instr ushr (idx * 2) and 3
            val mask = (instr ushr (8 + idx) and 1) == 1
            return VpfxdOp(VpfxdOpBase.forId(opNum), mask)
        }
    }

    override fun toString(): String {
        return "$op${if (mask) "M" else ""}"
    }
}

enum class VpfxdOpBase(val id: Int, val stringRep: String) {
    None(0, ""),
    ZeroToOne(1, "0:1"),
    X(2, "X"),
    MinusOneToOne(3, "-1:1");

    companion object {
        fun forId(id: Int): VpfxdOpBase {
            return values().first { it.id == id }
        }
    }

    override fun toString(): String {
        return stringRep
    }
}


class VfpuRotOperand(val ops: Array<VfpuRotOp>) : Operand() {
    companion object {
        fun decode(instr: Int, elemCount: Int): VfpuRotOperand {
            val imm = instr ushr 16 and 0x1F
            val ops = mutableListOf<VfpuRotOp>()
            val neg = (imm ushr 4 and 1 == 1)
            repeat(4) {
                ops.add(VfpuRotOp.Zero)
            }
            if (imm ushr 2 and 0b11 == imm and 0b11) {
                repeat(4) { idx ->
                    ops[idx] = VfpuRotOp.S
                }
            }
            ops[imm ushr 2 and 0b11] = VfpuRotOp.S
            ops[imm and 0b11] = VfpuRotOp.C
            return VfpuRotOperand(ops
                .map { if (it == VfpuRotOp.S && neg) VfpuRotOp.MinusS else it }
                .subList(0, elemCount)
                .toTypedArray())
        }
    }

    override fun toString(): String {
        return ops.joinToString(prefix = "[", postfix = "]")
    }
}

enum class VfpuRotOp(val stringRep: String) {
    S("S"),
    MinusS("-S"),
    C("C"),
    Zero("0");

    override fun toString(): String {
        return stringRep
    }
}
