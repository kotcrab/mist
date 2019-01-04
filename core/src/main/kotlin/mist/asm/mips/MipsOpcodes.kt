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

package mist.asm.mips

import mist.asm.*
import mist.asm.mips.allegrex.AllegrexProcessor

/** @author Kotcrab */

abstract class MipsOpcode(
    mnemonic: String,
    processors: Array<out MipsProcessor> = mipsCommon(),
    flags: Array<out OpcodeFlag> = emptyArray(),
    modify: Array<out OperandRef> = emptyArray(),
    use: Array<out OperandRef> = emptyArray()
) : Opcode(mnemonic, "", processors, flags, use, modify) {

    object Add : MipsOpcode("add", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Addi : MipsOpcode("addi", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Addiu : MipsOpcode("addiu", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Addu : MipsOpcode("addu", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object And : MipsOpcode("and", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Andi : MipsOpcode("andi", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Beq : MipsOpcode(
        "beq", flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Beql : MipsOpcode(
        "beql", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Bgez : MipsOpcode(
        "bgez", flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bgezal : MipsOpcode(
        "bgezal", flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bgezall : MipsOpcode(
        "bgezall", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bgezl : MipsOpcode(
        "bgezl", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bgtz : MipsOpcode(
        "bgtz",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bgtzl : MipsOpcode(
        "bgtzl", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Blez : MipsOpcode(
        "blez",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Blezl : MipsOpcode(
        "blezl", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bltz : MipsOpcode(
        "bltz",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bltzal : MipsOpcode(
        "bltzal",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bltzall : MipsOpcode(
        "bltzall", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bltzl : MipsOpcode(
        "bltzl", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bne : MipsOpcode(
        "bne",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Bnel : MipsOpcode(
        "bnel", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Break : MipsOpcode("break", flags = arrayOf(Trap))

    // Cop0-3 instructions should be only used as fallback when no specific coprocessor instruction can be resolved

    object Cop0 : MipsOpcode("cop0")
    object Cop1 : MipsOpcode("cop1")
    object Cop2 : MipsOpcode("cop2", processors = mipsCommon(allegrex = false))
    object Cop3 : MipsOpcode("cop3", processors = arrayOf(MipsIProcessor, MipsIIProcessor))

    object Div : MipsOpcode(
        "div",
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)), use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Divu : MipsOpcode(
        "divu",
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)), use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object J : MipsOpcode(
        "j",
        flags = arrayOf(Jump, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc))
    )

    object Jal : MipsOpcode(
        "jal",
        flags = arrayOf(Jump, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc), OperandRegRef(GprReg.Ra))
    )

    object Jalr : MipsOpcode(
        "jalr",
        flags = arrayOf(Jump, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc), Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Jr : MipsOpcode(
        "jr",
        flags = arrayOf(Jump, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Lb : MipsOpcode(
        "lb",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lbu : MipsOpcode(
        "lbu",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Ldc1 : MipsOpcode(
        "ldc1", processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu, MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Ldc2 : MipsOpcode(
        "ldc2", processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Ldc3 : MipsOpcode(
        "ldc3", processors = arrayOf(MipsIProcessor, MipsIIProcessor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lh : MipsOpcode(
        "lh",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lhu : MipsOpcode(
        "lhu",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Ll : MipsOpcode(
        "ll", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lui : MipsOpcode(
        "lui",
        modify = arrayOf(Operand0Ref)
    )

    object Lw : MipsOpcode(
        "lw",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lwc1 : MipsOpcode(
        "lwc1",
        flags = arrayOf(Fpu, MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lwc2 : MipsOpcode(
        "lwc2", processors = mipsCommon(allegrex = false),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lwc3 : MipsOpcode(
        "lwc3", processors = arrayOf(MipsIProcessor, MipsIIProcessor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lwl : MipsOpcode(
        "lwl",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lwr : MipsOpcode(
        "lwr",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Mfhi : MipsOpcode("mfhi", modify = arrayOf(Operand0Ref), use = arrayOf(OperandRegRef(GprReg.Hi)))
    object Mflo : MipsOpcode("mflo", modify = arrayOf(Operand0Ref), use = arrayOf(OperandRegRef(GprReg.Lo)))

    object Movn : MipsOpcode(
        "movn", processors = mipsCommon(legacyOrigin = MipsIVProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Movz : MipsOpcode(
        "movz", processors = mipsCommon(legacyOrigin = MipsIVProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Mthi : MipsOpcode("mthi", modify = arrayOf(OperandRegRef(GprReg.Hi)), use = arrayOf(Operand0Ref))
    object Mtlo : MipsOpcode("mtlo", modify = arrayOf(OperandRegRef(GprReg.Lo)), use = arrayOf(Operand0Ref))

    object Mult : MipsOpcode(
        "mult",
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)), use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Multu : MipsOpcode(
        "multu",
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)), use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Nop : MipsOpcode("nop")
    object Nor : MipsOpcode("nor", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Or : MipsOpcode("or", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Ori : MipsOpcode("ori", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Pref : MipsOpcode(
        "pref",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        use = arrayOf(Operand1Ref)
    )

    object Sb : MipsOpcode("sb", flags = arrayOf(MemoryWrite), use = arrayOf(Operand0Ref, Operand1Ref))

    object Sc : MipsOpcode(
        "sc", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sdc1 : MipsOpcode(
        "sdc1", processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu, MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sdc2 : MipsOpcode(
        "sdc2", processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sdc3 : MipsOpcode(
        "sdc3", processors = arrayOf(MipsIProcessor, MipsIIProcessor),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sh : MipsOpcode("sh", flags = arrayOf(MemoryWrite), use = arrayOf(Operand0Ref, Operand1Ref))
    object Sll : MipsOpcode("sll", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Sllv : MipsOpcode("sllv", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Slt : MipsOpcode("slt", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Slti : MipsOpcode("slti", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Sltiu : MipsOpcode("sltiu", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Sltu : MipsOpcode("sltu", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Sra : MipsOpcode("sra", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Srav : MipsOpcode("srav", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Srl : MipsOpcode("srl", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Srlv : MipsOpcode("srlv", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Sub : MipsOpcode("sub", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Subu : MipsOpcode("subu", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Sw : MipsOpcode("sw", flags = arrayOf(MemoryWrite), use = arrayOf(Operand0Ref, Operand1Ref))
    object Swc1 : MipsOpcode("swc1", flags = arrayOf(Fpu, MemoryWrite), use = arrayOf(Operand0Ref, Operand1Ref))

    object Swc2 : MipsOpcode(
        "swc2", processors = mipsCommon(allegrex = false),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Swc3 : MipsOpcode(
        "swc3",
        processors = arrayOf(MipsIProcessor, MipsIIProcessor),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Swl : MipsOpcode("swl", flags = arrayOf(MemoryWrite), use = arrayOf(Operand0Ref, Operand1Ref))
    object Swr : MipsOpcode("swr", flags = arrayOf(MemoryWrite), use = arrayOf(Operand0Ref, Operand1Ref))
    object Sync : MipsOpcode("sync")
    object Syscall : MipsOpcode("syscall", flags = arrayOf(Trap))

    object Teq : MipsOpcode(
        "teq", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Teqi : MipsOpcode(
        "teqi", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Tge : MipsOpcode(
        "tge", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Tgei : MipsOpcode(
        "tgei", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Tgeiu : MipsOpcode(
        "tgeiu", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Tgeu : MipsOpcode(
        "tgeu", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Tlt : MipsOpcode(
        "tlt", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Tlti : MipsOpcode(
        "tlti", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Tltiu : MipsOpcode(
        "tltiu", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Tltu : MipsOpcode(
        "tltu", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Tne : MipsOpcode(
        "tne", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Tnei : MipsOpcode(
        "tnei", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Xor : MipsOpcode("xor", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Xori : MipsOpcode("xori", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    // ----- FPU -----

    object FpuAbsS : MipsOpcode(
        "abs.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuAbsD : MipsOpcode(
        "abs.d", processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )


    object FpuAddS : MipsOpcode(
        "add.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuAddD : MipsOpcode(
        "add.d", processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuBc1f : MipsOpcode(
        "bc1f", processors = arrayOf(MipsIProcessor, MipsIIProcessor, MipsIIIProcessor, AllegrexProcessor),
        flags = arrayOf(Fpu, Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(OperandRegRef(FpuReg.Cc0))
    )

    object FpuBc1fCcAny : MipsOpcode(
        "bc1f", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu, Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object FpuBc1fl : MipsOpcode(
        "bc1fl", processors = arrayOf(MipsIIProcessor, MipsIIIProcessor, AllegrexProcessor),
        flags = arrayOf(Fpu, Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(OperandRegRef(FpuReg.Cc0))
    )

    object FpuBc1flCcAny : MipsOpcode(
        "bc1fl", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu, Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object FpuBc1t : MipsOpcode(
        "bc1t", processors = arrayOf(MipsIProcessor, MipsIIProcessor, MipsIIIProcessor, AllegrexProcessor),
        flags = arrayOf(Fpu, Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(OperandRegRef(FpuReg.Cc0))
    )

    object FpuBc1tCcAny : MipsOpcode(
        "bc1t", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu, Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object FpuBc1tl : MipsOpcode(
        "bc1tl", processors = arrayOf(MipsIIProcessor, MipsIIIProcessor, AllegrexProcessor),
        flags = arrayOf(Fpu, Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(OperandRegRef(FpuReg.Cc0))
    )

    object FpuBc1tlCcAny : MipsOpcode(
        "bc1tl", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu, Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object FpuCFS : FpuCCondFmt("c.f.s")
    object FpuCFD : FpuCCondFmt("c.f.d", allegrex = false)
    object FpuCUnS : FpuCCondFmt("c.un.s")
    object FpuCUnD : FpuCCondFmt("c.un.d", allegrex = false)
    object FpuCEqS : FpuCCondFmt("c.eq.s")
    object FpuCEqD : FpuCCondFmt("c.eq.d", allegrex = false)
    object FpuCUeqS : FpuCCondFmt("c.ueq.s")
    object FpuCUeqD : FpuCCondFmt("c.ueq.d", allegrex = false)
    object FpuCOltS : FpuCCondFmt("c.olt.s")
    object FpuCOltD : FpuCCondFmt("c.olt.d", allegrex = false)
    object FpuCUltS : FpuCCondFmt("c.ult.s")
    object FpuCUltD : FpuCCondFmt("c.ult.d", allegrex = false)
    object FpuCOleS : FpuCCondFmt("c.ole.s")
    object FpuCOleD : FpuCCondFmt("c.ole.d", allegrex = false)
    object FpuCUleS : FpuCCondFmt("c.ule.s")
    object FpuCUleD : FpuCCondFmt("c.ule.d", allegrex = false)
    object FpuCFSCcAny : FpuCCondFmtCcAny("c.f.s")
    object FpuCFDCcAny : FpuCCondFmtCcAny("c.f.d")
    object FpuCFPSCcAny : FpuCCondFmtCcAny("c.f.ps", fromMips32r2 = true)
    object FpuCUnSCcAny : FpuCCondFmtCcAny("c.un.s")
    object FpuCUnDCcAny : FpuCCondFmtCcAny("c.un.d")
    object FpuCUnPSCcAny : FpuCCondFmtCcAny("c.un.ps", fromMips32r2 = true)
    object FpuCEqSCcAny : FpuCCondFmtCcAny("c.eq.s")
    object FpuCEqDCcAny : FpuCCondFmtCcAny("c.eq.d")
    object FpuCEqPSCcAny : FpuCCondFmtCcAny("c.eq.ps", fromMips32r2 = true)
    object FpuCUeqSCcAny : FpuCCondFmtCcAny("c.ueq.s")
    object FpuCUeqDCcAny : FpuCCondFmtCcAny("c.ueq.d")
    object FpuCUeqPSCcAny : FpuCCondFmtCcAny("c.ueq.ps", fromMips32r2 = true)
    object FpuCOltSCcAny : FpuCCondFmtCcAny("c.olt.s")
    object FpuCOltDCcAny : FpuCCondFmtCcAny("c.olt.d")
    object FpuCOltPSCcAny : FpuCCondFmtCcAny("c.olt.ps", fromMips32r2 = true)
    object FpuCUltSCcAny : FpuCCondFmtCcAny("c.ult.s")
    object FpuCUltDCcAny : FpuCCondFmtCcAny("c.ult.d")
    object FpuCUltPSCcAny : FpuCCondFmtCcAny("c.ult.ps", fromMips32r2 = true)
    object FpuCOleSCcAny : FpuCCondFmtCcAny("c.ole.s")
    object FpuCOleDCcAny : FpuCCondFmtCcAny("c.ole.d")
    object FpuCOlePSCcAny : FpuCCondFmtCcAny("c.ole.ps", fromMips32r2 = true)
    object FpuCUleSCcAny : FpuCCondFmtCcAny("c.ule.s")
    object FpuCUleDCcAny : FpuCCondFmtCcAny("c.ule.d")
    object FpuCUlePSCcAny : FpuCCondFmtCcAny("c.ule.ps", fromMips32r2 = true)
    object FpuCSfS : FpuCCondFmt("c.sf.s")
    object FpuCSfD : FpuCCondFmt("c.sf.d", allegrex = false)
    object FpuCNgleS : FpuCCondFmt("c.ngle.s")
    object FpuCNgleD : FpuCCondFmt("c.ngle.d", allegrex = false)
    object FpuCSeqS : FpuCCondFmt("c.seq.s")
    object FpuCSeqD : FpuCCondFmt("c.seq.d", allegrex = false)
    object FpuCNglS : FpuCCondFmt("c.ngl.s")
    object FpuCNglD : FpuCCondFmt("c.ngl.d", allegrex = false)
    object FpuCLtS : FpuCCondFmt("c.lt.s")
    object FpuCLtD : FpuCCondFmt("c.lt.d", allegrex = false)
    object FpuCNgeS : FpuCCondFmt("c.nge.s")
    object FpuCNgeD : FpuCCondFmt("c.nge.d", allegrex = false)
    object FpuCLeS : FpuCCondFmt("c.le.s")
    object FpuCLeD : FpuCCondFmt("c.le.d", allegrex = false)
    object FpuCNgtS : FpuCCondFmt("c.ngt.s")
    object FpuCNgtD : FpuCCondFmt("c.ngt.d", allegrex = false)
    object FpuCSfSCcAny : FpuCCondFmtCcAny("c.sf.s")
    object FpuCSfDCcAny : FpuCCondFmtCcAny("c.sf.d")
    object FpuCSfPSCcAny : FpuCCondFmtCcAny("c.sf.ps", fromMips32r2 = true)
    object FpuCNgleSCcAny : FpuCCondFmtCcAny("c.ngle.s")
    object FpuCNgleDCcAny : FpuCCondFmtCcAny("c.ngle.d")
    object FpuCNglePSCcAny : FpuCCondFmtCcAny("c.ngle.ps", fromMips32r2 = true)
    object FpuCSeqSCcAny : FpuCCondFmtCcAny("c.seq.s")
    object FpuCSeqDCcAny : FpuCCondFmtCcAny("c.seq.d")
    object FpuCSeqPSCcAny : FpuCCondFmtCcAny("c.seq.ps", fromMips32r2 = true)
    object FpuCNglSCcAny : FpuCCondFmtCcAny("c.ngl.s")
    object FpuCNglDCcAny : FpuCCondFmtCcAny("c.ngl.d")
    object FpuCNglPSCcAny : FpuCCondFmtCcAny("c.ngl.ps", fromMips32r2 = true)
    object FpuCLtSCcAny : FpuCCondFmtCcAny("c.lt.s")
    object FpuCLtDCcAny : FpuCCondFmtCcAny("c.lt.d")
    object FpuCLtPSCcAny : FpuCCondFmtCcAny("c.lt.ps", fromMips32r2 = true)
    object FpuCNgeSCcAny : FpuCCondFmtCcAny("c.nge.s")
    object FpuCNgeDCcAny : FpuCCondFmtCcAny("c.nge.d")
    object FpuCNgePSCcAny : FpuCCondFmtCcAny("c.nge.ps", fromMips32r2 = true)
    object FpuCLeSCcAny : FpuCCondFmtCcAny("c.le.s")
    object FpuCLeDCcAny : FpuCCondFmtCcAny("c.le.d")
    object FpuCLePSCcAny : FpuCCondFmtCcAny("c.le.ps", fromMips32r2 = true)
    object FpuCNgtSCcAny : FpuCCondFmtCcAny("c.ngt.s")
    object FpuCNgtDCcAny : FpuCCondFmtCcAny("c.ngt.d")
    object FpuCNgtPSCcAny : FpuCCondFmtCcAny("c.ngt.ps", fromMips32r2 = true)

    abstract class FpuCCondFmt(mnemonic: String, allegrex: Boolean = true) : MipsOpcode(
        mnemonic,
        processors = mutableListOf<MipsProcessor>(MipsIProcessor, MipsIIProcessor, MipsIIIProcessor)
            .apply { if (allegrex) add(AllegrexProcessor) }
            .toTypedArray(),
        flags = arrayOf(Fpu),
        modify = arrayOf(OperandRegRef(FpuReg.Cc0)),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    abstract class FpuCCondFmtCcAny(mnemonic: String, fromMips32r2: Boolean = false) : MipsOpcode(
        mnemonic,
        processors = if (fromMips32r2) {
            mipsModern(Mips32r2Processor)
        } else {
            mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false)
        },
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuCeilLS : MipsOpcode(
        "ceil.l.s",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCeilLD : MipsOpcode(
        "ceil.l.d",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCeilWS : MipsOpcode(
        "ceil.w.s", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCeilWD : MipsOpcode(
        "ceil.w.d", processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCfc1 : MipsOpcode(
        "cfc1",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCtc1 : MipsOpcode(
        "ctc1",
        flags = arrayOf(Fpu),
        modify = arrayOf(), use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object FpuCvtDS : MipsOpcode(
        "cvt.d.s", processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCvtDW : MipsOpcode(
        "cvt.d.w", processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCvtDL : MipsOpcode(
        "cvt.d.l",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCvtLS : MipsOpcode(
        "cvt.l.s",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCvtLD : MipsOpcode(
        "cvt.l.d",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCvtSD : MipsOpcode(
        "cvt.s.d", processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCvtSW : MipsOpcode(
        "cvt.s.w",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCvtSL : MipsOpcode(
        "cvt.s.l",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCvtWS : MipsOpcode(
        "cvt.w.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCvtWD : MipsOpcode(
        "cvt.w.d", processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuDivS : MipsOpcode(
        "div.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuDivD : MipsOpcode(
        "div.d", processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuFloorLS : MipsOpcode(
        "floor.l.s",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuFloorLD : MipsOpcode(
        "floor.l.d",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuFloorWS : MipsOpcode(
        "floor.w.s", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuFloorWD : MipsOpcode(
        "floor.w.d", processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuLdxc1 : MipsOpcode(
        "ldxc1",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuLwxc1 : MipsOpcode(
        "lwxc1",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMaddS : MipsOpcode(
        "madd.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuMaddD : MipsOpcode(
        "madd.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuMfc1 : MipsOpcode(
        "mfc1",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuMovS : MipsOpcode(
        "mov.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuMovD : MipsOpcode(
        "mov.d", processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuMovf : MipsOpcode(
        "movf", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovfS : MipsOpcode(
        "movf.s", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovfD : MipsOpcode(
        "movf.d", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovnS : MipsOpcode(
        "movn.s", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovnD : MipsOpcode(
        "movn.d", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovt : MipsOpcode(
        "movt", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovtS : MipsOpcode(
        "movt.s", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovtD : MipsOpcode(
        "movt.d", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovzS : MipsOpcode(
        "movz.s", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovzD : MipsOpcode(
        "movz.d", processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMsubS : MipsOpcode(
        "msub.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuMsubD : MipsOpcode(
        "msub.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuMtc1 : MipsOpcode(
        "mtc1",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand1Ref), use = arrayOf(Operand0Ref)
    )

    object FpuMulS : MipsOpcode(
        "mul.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMulD : MipsOpcode(
        "mul.d", processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuNegS : MipsOpcode(
        "neg.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuNegD : MipsOpcode(
        "neg.d", processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuNmaddS : MipsOpcode(
        "nmadd.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuNmaddD : MipsOpcode(
        "nmadd.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuNmsubS : MipsOpcode(
        "nmsub.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuNmsubD : MipsOpcode(
        "nmsub.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuPrefx : MipsOpcode(
        "prefx",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        use = arrayOf(Operand1Ref)
    )

    object FpuRecipS : MipsOpcode(
        "recip.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuRecipD : MipsOpcode(
        "recip.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuRoundLS : MipsOpcode(
        "round.l.s",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuRoundLD : MipsOpcode(
        "round.l.d",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuRoundWS : MipsOpcode(
        "round.w.s", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuRoundWD : MipsOpcode(
        "round.w.d", processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuRsqrtS : MipsOpcode(
        "rsqrt.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuRsqrtD : MipsOpcode(
        "rsqrt.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuSdxc1 : MipsOpcode(
        "sdxc1",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(MemoryWrite, Fpu),
        use = arrayOf(Operand0Ref, Operand1Ref, Operand2Ref)
    )

    object FpuSqrtS : MipsOpcode(
        "sqrt.s", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuSqrtD : MipsOpcode(
        "sqrt.d", processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuSubS : MipsOpcode(
        "sub.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuSubD : MipsOpcode(
        "sub.d", processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuSwxc1 : MipsOpcode(
        "swxc1",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(MemoryWrite, Fpu),
        use = arrayOf(Operand0Ref, Operand1Ref, Operand2Ref)
    )

    object FpuTruncLS : MipsOpcode(
        "trunc.l.s",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuTruncLD : MipsOpcode(
        "trunc.l.d",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, modernOrigin = Mips32r2Processor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuTruncWS : MipsOpcode(
        "trunc.w.s", processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuTruncWD : MipsOpcode(
        "trunc.w.d", processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    // ----- Specific to MIPS32 and Allegrex -----

    object FpuAbsPS : MipsOpcode(
        "abs.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuAddPS : MipsOpcode(
        "add.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuAlnvPS : MipsOpcode(
        "alnv.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Bc2f : MipsOpcode(
        "bc2f", processors = mipsModern(),
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bc2fl : MipsOpcode(
        "bc2fl", processors = mipsModern(),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bc2t : MipsOpcode(
        "bc2t", processors = mipsModern(),
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Bc2tl : MipsOpcode(
        "bc2tl", processors = mipsModern(),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Cache : MipsOpcode(
        "cache", processors = arrayOf(*mipsModern(), AllegrexProcessor),
        use = arrayOf(Operand1Ref)
    )

    object Cachee : MipsOpcode(
        "cachee", processors = mipsModern(Mips32r3Processor),
        use = arrayOf(Operand1Ref)
    )

    object Cfc2 : MipsOpcode(
        "cfc2", processors = mipsModern(),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Clo : MipsOpcode(
        "clo", processors = arrayOf(*mipsModern(), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Clz : MipsOpcode(
        "clz", processors = arrayOf(*mipsModern(), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Ctc2 : MipsOpcode(
        "ctc2", processors = mipsModern(),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object FpuCvtPSS : MipsOpcode(
        "cvt.ps.s", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuCvtSPL : MipsOpcode(
        "cvt.s.pl", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuCvtSPU : MipsOpcode(
        "cvt.s.pu", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Deret : MipsOpcode("deret", processors = arrayOf(*mipsModern(), AllegrexProcessor)) // EJTAG

    object Di : MipsOpcode(
        "di", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf()
    )

    object Ehb : MipsOpcode("ehb", processors = mipsModern(Mips32r2Processor))

    object Ei : MipsOpcode(
        "ei", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf()
    )

    object Eret : MipsOpcode("eret", processors = arrayOf(*mipsModern(), AllegrexProcessor))

    object Eretnc : MipsOpcode("eretnc", processors = mipsModern(Mips32r5Processor))

    object Ext : MipsOpcode(
        "ext", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Ins : MipsOpcode(
        "ins", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object JalrHb : MipsOpcode(
        "jalr.hb", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Jump, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc), Operand0Ref), use = arrayOf(Operand1Ref)
    )

    // JALX instruction was skipped because it switches ISA mode to microMIPS32 or MIPS16e

    object JrHb : MipsOpcode(
        "jr.hb", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Jump, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)), use = arrayOf(Operand0Ref)
    )

    object Lbe : MipsOpcode(
        "lbe", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lbue : MipsOpcode(
        "lbue", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lhe : MipsOpcode(
        "lhe", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lhue : MipsOpcode(
        "lhue", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lle : MipsOpcode(
        "lle", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuLuxc1 : MipsOpcode(
        "luxc1", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu, MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Lwe : MipsOpcode(
        "lwe", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lwle : MipsOpcode(
        "lwle", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Lwre : MipsOpcode(
        "lwre", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Madd : MipsOpcode(
        "madd", processors = arrayOf(*mipsModern(), AllegrexProcessor),
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)),
        use = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi), Operand0Ref, Operand1Ref)
    )

    object FpuMaddPS : MipsOpcode(
        "madd.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object Maddu : MipsOpcode(
        "maddu", processors = arrayOf(*mipsModern(), AllegrexProcessor),
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)),
        use = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi), Operand0Ref, Operand1Ref)
    )

    object Mfc0 : MipsOpcode(
        "mfc0", processors = arrayOf(*mipsModern(), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Mfc2 : MipsOpcode(
        "mfc2", processors = mipsModern(),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Mfhc0 : MipsOpcode(
        "mfhc0", processors = mipsModern(Mips32r5Processor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Mfhc1 : MipsOpcode(
        "mfhc1", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Mfhc2 : MipsOpcode(
        "mfhc2", processors = mipsModern(Mips32r2Processor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuMovPS : MipsOpcode(
        "mov.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuMovfPS : MipsOpcode(
        "movf.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovnPS : MipsOpcode(
        "movn.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovtPS : MipsOpcode(
        "movt.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovzPS : MipsOpcode(
        "movz.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Msub : MipsOpcode(
        "msub", processors = arrayOf(*mipsModern(), AllegrexProcessor),
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)),
        use = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi), Operand0Ref, Operand1Ref)
    )

    object FpuMsubPS : MipsOpcode(
        "msub.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object Msubu : MipsOpcode(
        "msubu", processors = arrayOf(*mipsModern(), AllegrexProcessor),
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)),
        use = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi), Operand0Ref, Operand1Ref)
    )

    object Mtc0 : MipsOpcode(
        "mtc0", processors = arrayOf(*mipsModern(), AllegrexProcessor),
        modify = arrayOf(Operand1Ref), use = arrayOf(Operand0Ref)
    )

    object Mtc2 : MipsOpcode(
        "mtc2", processors = mipsModern(),
        modify = arrayOf(Operand1Ref), use = arrayOf(Operand0Ref)
    )

    object Mthc0 : MipsOpcode(
        "mthc0", processors = mipsModern(Mips32r5Processor),
        modify = arrayOf(Operand1Ref), use = arrayOf(Operand0Ref)
    )

    object Mthc1 : MipsOpcode(
        "mthc1", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand1Ref), use = arrayOf(Operand0Ref)
    )

    object Mthc2 : MipsOpcode(
        "mthc2", processors = mipsModern(Mips32r2Processor),
        modify = arrayOf(Operand1Ref), use = arrayOf(Operand0Ref)
    )

    object Mul : MipsOpcode(
        "mul", processors = mipsModern(),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMulPS : MipsOpcode(
        "mul.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuNegPS : MipsOpcode(
        "neg.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object FpuNmaddPS : MipsOpcode(
        "nmadd.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuNmsubPS : MipsOpcode(
        "nmsub.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object Pause : MipsOpcode("pause", processors = mipsModern(Mips32r2Processor))

    object FpuPllPS : MipsOpcode(
        "pll.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuPluPS : MipsOpcode(
        "plu.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Prefe : MipsOpcode(
        "prefe", processors = mipsModern(Mips32r3Processor),
        use = arrayOf(Operand1Ref)
    )

    object FpuPulPS : MipsOpcode(
        "pul.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuPuuPS : MipsOpcode(
        "puu.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Rdhwr : MipsOpcode(
        "rdhwr", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Rdpgpr : MipsOpcode(
        "rdpgpr", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Rotr : MipsOpcode(
        "rotr", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Rotrv : MipsOpcode(
        "rotrv", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Sbe : MipsOpcode(
        "sbe", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sce : MipsOpcode(
        "sce", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sdbbp : MipsOpcode("sdbbp", processors = mipsModern()) // EJTAG

    object Seb : MipsOpcode(
        "seb", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object Seh : MipsOpcode(
        "seh", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )

    object She : MipsOpcode(
        "she", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object SsNop : MipsOpcode("ssnop", processors = mipsModern())

    object FpuSubPs : MipsOpcode(
        "sub.ps", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuSuxc1 : MipsOpcode(
        "suxc1", processors = mipsModern(Mips32r2Processor),
        flags = arrayOf(MemoryWrite, Fpu),
        use = arrayOf(Operand0Ref, Operand1Ref, Operand2Ref)
    )

    object Swe : MipsOpcode(
        "swe", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Swle : MipsOpcode(
        "swle", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Swre : MipsOpcode(
        "swre", processors = mipsModern(Mips32r3Processor),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Synci : MipsOpcode("synci", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor))
    object Tlbinv : MipsOpcode("tlbinv", processors = mipsModern())
    object Tlbinvf : MipsOpcode("tlbinvf", processors = mipsModern())
    object Tlbp : MipsOpcode("tlbp", processors = arrayOf(*mipsModern(), AllegrexProcessor))
    object Tlbr : MipsOpcode("tlbr", processors = arrayOf(*mipsModern(), AllegrexProcessor))
    object Tlbwi : MipsOpcode("tlbwi", processors = arrayOf(*mipsModern(), AllegrexProcessor))
    object Tlbwr : MipsOpcode("tlbwr", processors = arrayOf(*mipsModern(), AllegrexProcessor))
    object Wait : MipsOpcode("wait", processors = arrayOf(*mipsModern(), AllegrexProcessor))

    object Wrpgpr : MipsOpcode(
        "wrpgpr", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Wsbh : MipsOpcode(
        "wsbh", processors = arrayOf(*mipsModern(Mips32r2Processor), AllegrexProcessor),
        modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref)
    )
}
