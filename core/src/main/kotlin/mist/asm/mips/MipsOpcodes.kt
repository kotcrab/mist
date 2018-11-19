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

// TODO should not be necessary after implementing all disassemblers
@file:Suppress("unused")

package mist.asm.mips

import mist.asm.*

/** @author Kotcrab */

//TODO all opcodes needs to be verified if present on MIPS32
//TODO add MIPS32 opcodes

abstract class MipsOpcode(
    mnemonic: String,
    processors: Array<out MipsProcessor> = mipsCommon(),
    flags: Array<out OpcodeFlag> = emptyArray(),
    modify: Array<out OperandRef> = emptyArray(),
    use: Array<out OperandRef> = emptyArray()
) : Opcode(mnemonic, processors, flags, use, modify) {
    object Add : MipsOpcode("add", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object Addi : MipsOpcode("addi", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Addiu : MipsOpcode("addiu", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Addu : MipsOpcode("addu", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object And : MipsOpcode("and", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object Andi : MipsOpcode("andi", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Beq : MipsOpcode(
        "beq",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Beql : MipsOpcode(
        "beql",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Bgez : MipsOpcode(
        "bgez",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Bgezal : MipsOpcode(
        "bgezal",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Bgezall : MipsOpcode(
        "bgezall",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Bgezl : MipsOpcode(
        "bgezl",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Bgtz : MipsOpcode(
        "bgtz",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Bgtzl : MipsOpcode(
        "bgtzl",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Blez : MipsOpcode(
        "blez",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Blezl : MipsOpcode(
        "blezl",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Bltz : MipsOpcode(
        "bltz",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Bltzal : MipsOpcode(
        "bltzal",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Bltzall : MipsOpcode(
        "bltzall",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Bltzl : MipsOpcode(
        "bltzl",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Bne : MipsOpcode(
        "bne",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Bnel : MipsOpcode(
        "bnel",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Break : MipsOpcode("break", flags = arrayOf(Trap))

// Cop0-3 instructions should be only used as fallback when no specific coprocessor instruction can be resolved

    object Cop0 : MipsOpcode("cop0")

    object Cop1 : MipsOpcode("cop1")

    object Cop2 : MipsOpcode("cop2")

    object Cop3 : MipsOpcode("cop3")

    object Div : MipsOpcode(
        "div",
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Divu : MipsOpcode(
        "divu",
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)),
        use = arrayOf(Operand0Ref, Operand1Ref)
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
        modify = arrayOf(OperandRegRef(GprReg.Pc), Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Jr : MipsOpcode(
        "jr",
        flags = arrayOf(Jump, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object Lb : MipsOpcode(
        "lb",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Lbu : MipsOpcode(
        "lbu",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Ldc1 : MipsOpcode(
        "ldc1",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu, MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Ldc2 : MipsOpcode(
        "ldc2",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Ldc3 : MipsOpcode(
        "ldc3",
        processors = arrayOf(MipsIProcessor, MipsIIProcessor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Lh : MipsOpcode(
        "lh",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Lhu : MipsOpcode(
        "lhu",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Ll : MipsOpcode(
        "ll",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Lui : MipsOpcode("lui", modify = arrayOf(Operand0Ref))

    object Lw : MipsOpcode(
        "lw",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Lwc1 : MipsOpcode(
        "lwc1",
        flags = arrayOf(Fpu, MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Lwc2 : MipsOpcode(
        "lwc2",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Lwc3 : MipsOpcode(
        "lwc3",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Lwl : MipsOpcode(
        "lwl",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Lwr : MipsOpcode(
        "lwr",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Mfhi : MipsOpcode(
        "mfhi",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(OperandRegRef(GprReg.Hi))
    )

    object Mflo : MipsOpcode(
        "mflo",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(OperandRegRef(GprReg.Lo))
    )

    object Movn : MipsOpcode(
        "movn",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Movz : MipsOpcode(
        "movz",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Mthi : MipsOpcode(
        "mthi",
        modify = arrayOf(OperandRegRef(GprReg.Hi)),
        use = arrayOf(Operand0Ref)
    )

    object Mtlo : MipsOpcode(
        "mtlo",
        modify = arrayOf(OperandRegRef(GprReg.Lo)),
        use = arrayOf(Operand0Ref)
    )

    object Mult : MipsOpcode(
        "mult",
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Multu : MipsOpcode(
        "multu",
        modify = arrayOf(OperandRegRef(GprReg.Lo), OperandRegRef(GprReg.Hi)),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Nop : MipsOpcode("nop")

    object Nor : MipsOpcode(
        "nor",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Or : MipsOpcode(
        "or",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Ori : MipsOpcode(
        "ori",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Pref : MipsOpcode(
        "pref",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        use = arrayOf(Operand1Ref)
    )

    object Sb : MipsOpcode(
        "sb",
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sc : MipsOpcode(
        "sc",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sdc1 : MipsOpcode(
        "sdc1",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu, MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sdc2 : MipsOpcode(
        "sdc2",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sdc3 : MipsOpcode(
        "sdc3",
        processors = arrayOf(MipsIProcessor, MipsIIProcessor),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sh : MipsOpcode(
        "sh",
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sll : MipsOpcode(
        "sll",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Sllv : MipsOpcode(
        "sllv",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Slt : MipsOpcode(
        "slt",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Slti : MipsOpcode(
        "slti",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Sltiu : MipsOpcode(
        "sltiu",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Sltu : MipsOpcode(
        "sltu",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Sra : MipsOpcode(
        "sra",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Srav : MipsOpcode(
        "srav",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Srl : MipsOpcode(
        "srl",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Srlv : MipsOpcode(
        "srlv",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Sub : MipsOpcode(
        "sub",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Subu : MipsOpcode(
        "subu",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Sw : MipsOpcode(
        "sw",
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Swc1 : MipsOpcode(
        "swc1",
        flags = arrayOf(Fpu, MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Swc2 : MipsOpcode(
        "swc2",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Swc3 : MipsOpcode(
        "swc3",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Swl : MipsOpcode(
        "swl",
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Swr : MipsOpcode(
        "swr",
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Sync : MipsOpcode("sync")

    object Syscall : MipsOpcode("syscall", flags = arrayOf(Trap))

    object Teq : MipsOpcode(
        "teq",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Teqi : MipsOpcode(
        "teqi",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Tge : MipsOpcode(
        "tge",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Tgei : MipsOpcode(
        "tgei",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Tgeiu : MipsOpcode(
        "tgeiu",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Tgeu : MipsOpcode(
        "tgeu",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Tlt : MipsOpcode(
        "tlt",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Tlti : MipsOpcode(
        "tlti",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Tltiu : MipsOpcode(
        "tltiu",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Tltu : MipsOpcode(
        "tltu",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Tne : MipsOpcode(
        "tne",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Tnei : MipsOpcode(
        "tnei",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Trap),
        use = arrayOf(Operand0Ref)
    )

    object Xor : MipsOpcode(
        "xor",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object Xori : MipsOpcode(
        "xori",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

// FPU

    object FpuAbsS : MipsOpcode(
        "abs.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuAbsD : MipsOpcode(
        "abs.d",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuAddS : MipsOpcode(
        "add.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuAddD : MipsOpcode(
        "add.d",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuBc1f : MipsOpcode(
        "bc1f",
        flags = arrayOf(Fpu, Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(OperandRegRef(FpuReg.Cc0))
    )

    object FpuBc1fCcAny : MipsOpcode(
        "bc1f",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu, Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object FpuBc1fl : MipsOpcode(
        "bc1fl",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu, Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(OperandRegRef(FpuReg.Cc0))
    )

    object FpuBc1flCcAny : MipsOpcode(
        "bc1fl",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu, Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object FpuBc1t : MipsOpcode(
        "bc1t",
        flags = arrayOf(Fpu, Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(OperandRegRef(FpuReg.Cc0))
    )

    object FpuBc1tCcAny : MipsOpcode(
        "bc1t",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu, Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
    )

    object FpuBc1tl : MipsOpcode(
        "bc1tl",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu, Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(OperandRegRef(FpuReg.Cc0))
    )

    object FpuBc1tlCcAny : MipsOpcode(
        "bc1tl",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu, Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(Operand0Ref)
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
    object FpuCUnSCcAny : FpuCCondFmtCcAny("c.un.s")
    object FpuCUnDCcAny : FpuCCondFmtCcAny("c.un.d")
    object FpuCEqSCcAny : FpuCCondFmtCcAny("c.eq.s")
    object FpuCEqDCcAny : FpuCCondFmtCcAny("c.eq.d")
    object FpuCUeqSCcAny : FpuCCondFmtCcAny("c.ueq.s")
    object FpuCUeqDCcAny : FpuCCondFmtCcAny("c.ueq.d")
    object FpuCOltSCcAny : FpuCCondFmtCcAny("c.olt.s")
    object FpuCOltDCcAny : FpuCCondFmtCcAny("c.olt.d")
    object FpuCUltSCcAny : FpuCCondFmtCcAny("c.ult.s")
    object FpuCUltDCcAny : FpuCCondFmtCcAny("c.ult.d")
    object FpuCOleSCcAny : FpuCCondFmtCcAny("c.ole.s")
    object FpuCOleDCcAny : FpuCCondFmtCcAny("c.ole.d")
    object FpuCUleSCcAny : FpuCCondFmtCcAny("c.ule.s")
    object FpuCUleDCcAny : FpuCCondFmtCcAny("c.ule.d")
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
    object FpuCNgleSCcAny : FpuCCondFmtCcAny("c.ngle.s")
    object FpuCNgleDCcAny : FpuCCondFmtCcAny("c.ngle.d")
    object FpuCSeqSCcAny : FpuCCondFmtCcAny("c.seq.s")
    object FpuCSeqDCcAny : FpuCCondFmtCcAny("c.seq.d")
    object FpuCNglSCcAny : FpuCCondFmtCcAny("c.ngl.s")
    object FpuCNglDCcAny : FpuCCondFmtCcAny("c.ngl.d")
    object FpuCLtSCcAny : FpuCCondFmtCcAny("c.lt.s")
    object FpuCLtDCcAny : FpuCCondFmtCcAny("c.lt.d")
    object FpuCNgeSCcAny : FpuCCondFmtCcAny("c.nge.s")
    object FpuCNgeDCcAny : FpuCCondFmtCcAny("c.nge.d")
    object FpuCLeSCcAny : FpuCCondFmtCcAny("c.le.s")
    object FpuCLeDCcAny : FpuCCondFmtCcAny("c.le.d")
    object FpuCNgtSCcAny : FpuCCondFmtCcAny("c.ngt.s")
    object FpuCNgtDCcAny : FpuCCondFmtCcAny("c.ngt.d")

    abstract class FpuCCondFmt(mnemonic: String, allegrex: Boolean = true) : MipsOpcode(
        mnemonic,
        processors = mipsCommon(allegrex = allegrex),
        flags = arrayOf(Fpu),
        modify = arrayOf(OperandRegRef(FpuReg.Cc0)),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    abstract class FpuCCondFmtCcAny(mnemonic: String) : MipsOpcode(
        mnemonic,
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuCeilLS : MipsOpcode(
        "ceil.l.s",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCeilLD : MipsOpcode(
        "ceil.l.d",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCeilWS : MipsOpcode(
        "ceil.w.s",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCeilWD : MipsOpcode(
        "ceil.w.d",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCfc1 : MipsOpcode(
        "cfc1",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCtc1 : MipsOpcode(
        "ctc1",
        flags = arrayOf(Fpu),
        modify = arrayOf(),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object FpuCvtDS : MipsOpcode(
        "cvt.d.s",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCvtDW : MipsOpcode(
        "cvt.d.w",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCvtDL : MipsOpcode(
        "cvt.d.l",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCvtLS : MipsOpcode(
        "cvt.l.s",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCvtLD : MipsOpcode(
        "cvt.l.d",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCvtSD : MipsOpcode(
        "cvt.s.d",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCvtSW : MipsOpcode(
        "cvt.s.w",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCvtSL : MipsOpcode(
        "cvt.s.l",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCvtWS : MipsOpcode(
        "cvt.w.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuCvtWD : MipsOpcode(
        "cvt.w.d",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuDivS : MipsOpcode(
        "div.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuDivD : MipsOpcode(
        "div.d",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuFloorLS : MipsOpcode(
        "floor.l.s",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuFloorLD : MipsOpcode(
        "floor.l.d",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuFloorWS : MipsOpcode(
        "floor.w.s",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuFloorWD : MipsOpcode(
        "floor.w.d",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuLdxc1 : MipsOpcode(
        "ldxc1",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuLwxc1 : MipsOpcode(
        "lwxc1",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuMaddS : MipsOpcode(
        "madd.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuMaddD : MipsOpcode(
        "madd.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuMfc1 : MipsOpcode(
        "mfc1",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuMovS : MipsOpcode(
        "mov.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuMovD : MipsOpcode(
        "mov.d",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuMovf : MipsOpcode(
        "movf",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovfS : MipsOpcode(
        "movf.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovfD : MipsOpcode(
        "movf.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovnS : MipsOpcode(
        "movn.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovnD : MipsOpcode(
        "movn.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovt : MipsOpcode(
        "movt",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovtS : MipsOpcode(
        "movt.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovtD : MipsOpcode(
        "movt.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovzS : MipsOpcode(
        "movz.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMovzD : MipsOpcode(
        "movz.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMsubS : MipsOpcode(
        "msub.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuMsubD : MipsOpcode(
        "msub.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuMtc1 : MipsOpcode(
        "mtc1",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand1Ref),
        use = arrayOf(Operand0Ref)
    )

    object FpuMulS : MipsOpcode(
        "mul.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuMulD : MipsOpcode(
        "mul.d",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuNegS : MipsOpcode(
        "neg.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuNegD : MipsOpcode(
        "neg.d",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuNmaddS : MipsOpcode(
        "nmadd.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuNmaddD : MipsOpcode(
        "nmadd.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuNmsubS : MipsOpcode(
        "nmsub.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuNmsubD : MipsOpcode(
        "nmsub.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref, Operand3Ref)
    )

    object FpuPrefx : MipsOpcode(
        "prefx",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        use = arrayOf(Operand1Ref)
    )

    object FpuRecipS : MipsOpcode(
        "recip.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuRecipD : MipsOpcode(
        "recip.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuRoundLS : MipsOpcode(
        "round.l.s",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuRoundLD : MipsOpcode(
        "round.l.d",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuRoundWS : MipsOpcode(
        "round.w.s",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuRoundWD : MipsOpcode(
        "round.w.d",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuRsqrtS : MipsOpcode(
        "rsqrt.s",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuRsqrtD : MipsOpcode(
        "rsqrt.d",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuSdc1 : MipsOpcode(
        "sdc1",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(MemoryWrite, Fpu),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object FpuSdxc1 : MipsOpcode(
        "sdxc1",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(MemoryWrite, Fpu),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object FpuSqrtS : MipsOpcode(
        "sqrt.s",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuSqrtD : MipsOpcode(
        "sqrt.d",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuSubS : MipsOpcode(
        "sub.s",
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuSubD : MipsOpcode(
        "sub.d",
        processors = mipsCommon(allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref, Operand2Ref)
    )

    object FpuSwxc1 : MipsOpcode(
        "swxc1",
        processors = mipsCommon(legacyOrigin = MipsIVProcessor, allegrex = false),
        flags = arrayOf(MemoryWrite, Fpu),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object FpuTruncLS : MipsOpcode(
        "trunc.l.s",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuTruncLD : MipsOpcode(
        "trunc.l.d",
        processors = mipsCommon(legacyOrigin = MipsIIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuTruncWS : MipsOpcode(
        "trunc.w.s",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object FpuTruncWD : MipsOpcode(
        "trunc.w.d",
        processors = mipsCommon(legacyOrigin = MipsIIProcessor, allegrex = false),
        flags = arrayOf(Fpu),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )
}
