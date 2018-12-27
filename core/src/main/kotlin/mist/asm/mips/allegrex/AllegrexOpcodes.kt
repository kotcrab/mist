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

// TODO should not be necessary after implementing all VFPU disassembly
@file:Suppress("unused")

package mist.asm.mips.allegrex

import mist.asm.*
import mist.asm.mips.BranchLikely
import mist.asm.mips.GprReg
import mist.asm.mips.MipsOpcode

/** @author Kotcrab */

abstract class AllegrexOpcode(
    mnemonic: String,
    flags: Array<out OpcodeFlag> = emptyArray(),
    modify: Array<out OperandRef> = emptyArray(),
    use: Array<out OperandRef> = emptyArray()
) : MipsOpcode(mnemonic, processors = arrayOf(AllegrexProcessor), flags = flags, modify = modify, use = use) {
    object Max : AllegrexOpcode("max", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Min : AllegrexOpcode("min", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
}

// These are likely to be incomplete and specify wrong flags, used and modified operands

abstract class VfpuOpcode(
    mnemonic: String,
    flags: Array<out OpcodeFlag> = emptyArray(),
    modify: Array<out OperandRef> = emptyArray(),
    use: Array<out OperandRef> = emptyArray()
) : AllegrexOpcode(mnemonic, flags = arrayOf(Vfpu, *flags), modify = modify, use = use) {
    object Bvf : VfpuOpcode(
        "bvf",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(OperandRegRef(VfpuReg.Cc))
    )

    object Bvfl : VfpuOpcode(
        "bvfl",
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(OperandRegRef(VfpuReg.Cc))
    )

    object Bvt : VfpuOpcode(
        "bvt",
        flags = arrayOf(Branch, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(OperandRegRef(VfpuReg.Cc))
    )

    object Bvtl : VfpuOpcode(
        "bvtl",
        flags = arrayOf(Branch, BranchLikely, DelaySlot),
        modify = arrayOf(OperandRegRef(GprReg.Pc)),
        use = arrayOf(OperandRegRef(VfpuReg.Cc))
    )

    object LvS : VfpuOpcode(
        "lv.s",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object LvQ : VfpuOpcode(
        "lv.q",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object SvS : VfpuOpcode(
        "sv.s",
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object SvQ : VfpuOpcode(
        "sv.q",
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object LvlQ : VfpuOpcode(
        "lvl.q",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object LvrQ : VfpuOpcode(
        "lvr.q",
        flags = arrayOf(MemoryRead),
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object SvlQ : VfpuOpcode(
        "svl.q",
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object SvrQ : VfpuOpcode(
        "svr.q",
        flags = arrayOf(MemoryWrite),
        use = arrayOf(Operand0Ref, Operand1Ref)
    )

    object Mtv : VfpuOpcode(
        "mtv",
        modify = arrayOf(Operand1Ref),
        use = arrayOf(Operand0Ref)
    )

    object Mfv : VfpuOpcode(
        "mfv",
        modify = arrayOf(Operand0Ref),
        use = arrayOf(Operand1Ref)
    )

    object Vmfvc : VfpuOpcode("vmfvc", use = arrayOf(Operand0Ref))
    object Vmtvc : VfpuOpcode("vmtvc", modify = arrayOf(Operand0Ref))

    object Vflush : VfpuOpcode("vflush")

    object VaddS : VfpuOpcode("vadd.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VaddP : VfpuOpcode("vadd.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VaddT : VfpuOpcode("vadd.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VaddQ : VfpuOpcode("vadd.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VsclP : VfpuOpcode("vscl.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VsclT : VfpuOpcode("vscl.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VsclQ : VfpuOpcode("vscl.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VsubS : VfpuOpcode("vsub.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VsubP : VfpuOpcode("vsub.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VsubT : VfpuOpcode("vsub.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VsubQ : VfpuOpcode("vsub.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VdivS : VfpuOpcode("vdiv.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VdivP : VfpuOpcode("vdiv.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VdivT : VfpuOpcode("vdiv.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VdivQ : VfpuOpcode("vdiv.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VmulS : VfpuOpcode("vmul.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VmulP : VfpuOpcode("vmul.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VmulT : VfpuOpcode("vmul.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VmulQ : VfpuOpcode("vmul.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VdotT : VfpuOpcode("vdot.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VdotP : VfpuOpcode("vdot.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VdotQ : VfpuOpcode("vdot.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VhdpT : VfpuOpcode("vhdp.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VhdpP : VfpuOpcode("vhdp.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VhdpQ : VfpuOpcode("vhdp.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VidtP : VfpuOpcode("vidt.p", modify = arrayOf(Operand0Ref))
    object VidtT : VfpuOpcode("vidt.t", modify = arrayOf(Operand0Ref))
    object VidtQ : VfpuOpcode("vidt.q", modify = arrayOf(Operand0Ref))

    object VabsS : VfpuOpcode("vabs.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VabsP : VfpuOpcode("vabs.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VabsT : VfpuOpcode("vabs.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VabsQ : VfpuOpcode("vabs.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VnegS : VfpuOpcode("vneg.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VnegP : VfpuOpcode("vneg.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VnegT : VfpuOpcode("vneg.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VnegQ : VfpuOpcode("vneg.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VsgnS : VfpuOpcode("vsgn.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VsgnP : VfpuOpcode("vsgn.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VsgnT : VfpuOpcode("vsgn.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VsgnQ : VfpuOpcode("vsgn.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VminS : VfpuOpcode("vmin.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VminP : VfpuOpcode("vmin.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VminT : VfpuOpcode("vmin.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VminQ : VfpuOpcode("vmin.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VmaxS : VfpuOpcode("vmax.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VmaxP : VfpuOpcode("vmax.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VmaxT : VfpuOpcode("vmax.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VmaxQ : VfpuOpcode("vmax.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object Vtfm2P : VfpuOpcode("vtfm2.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Vtfm3T : VfpuOpcode("vtfm3.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Vtfm4Q : VfpuOpcode("vtfm4.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object Vhtfm2P : VfpuOpcode("vhtfm2.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Vhtfm3T : VfpuOpcode("vhtfm3.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object Vhtfm4Q : VfpuOpcode("vhtfm4.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VcmpS :
        VfpuOpcode("vcmp.s", modify = arrayOf(OperandRegRef(VfpuReg.Cc)), use = arrayOf(Operand1Ref, Operand2Ref))

    object VcmpP :
        VfpuOpcode("vcmp.p", modify = arrayOf(OperandRegRef(VfpuReg.Cc)), use = arrayOf(Operand1Ref, Operand2Ref))

    object VcmpT :
        VfpuOpcode("vcmp.t", modify = arrayOf(OperandRegRef(VfpuReg.Cc)), use = arrayOf(Operand1Ref, Operand2Ref))

    object VcmpQ :
        VfpuOpcode("vcmp.q", modify = arrayOf(OperandRegRef(VfpuReg.Cc)), use = arrayOf(Operand1Ref, Operand2Ref))

    object VcstS : VfpuOpcode("vcst.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcstP : VfpuOpcode("vcst.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcstT : VfpuOpcode("vcst.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcstQ : VfpuOpcode("vcst.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VscmpS : VfpuOpcode("vscmp.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VscmpP : VfpuOpcode("vscmp.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VscmpT : VfpuOpcode("vscmp.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VscmpQ : VfpuOpcode("vscmp.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VsgeS : VfpuOpcode("vsge.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VsgeP : VfpuOpcode("vsge.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VsgeT : VfpuOpcode("vsge.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VsgeQ : VfpuOpcode("vsge.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VsltS : VfpuOpcode("vslt.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VsltP : VfpuOpcode("vslt.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VsltT : VfpuOpcode("vslt.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VsltQ : VfpuOpcode("vslt.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object Vi2ucQ : VfpuOpcode("vi2uc.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vi2cQ : VfpuOpcode("vi2c.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vi2usP : VfpuOpcode("vi2us.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vi2usQ : VfpuOpcode("vi2us.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vi2sP : VfpuOpcode("vi2s.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vi2sQ : VfpuOpcode("vi2s.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VmovS : VfpuOpcode("vmov.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VmovP : VfpuOpcode("vmov.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VmovT : VfpuOpcode("vmov.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VmovQ : VfpuOpcode("vmov.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vsat0S : VfpuOpcode("vsat0.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vsat0P : VfpuOpcode("vsat0.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vsat0T : VfpuOpcode("vsat0.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vsat0Q : VfpuOpcode("vsat0.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vsat1S : VfpuOpcode("vsat1.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vsat1P : VfpuOpcode("vsat1.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vsat1T : VfpuOpcode("vsat1.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vsat1Q : VfpuOpcode("vsat1.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VzeroS : VfpuOpcode("vzero.s", modify = arrayOf(Operand0Ref))
    object VzeroP : VfpuOpcode("vzero.p", modify = arrayOf(Operand0Ref))
    object VzeroT : VfpuOpcode("vzero.t", modify = arrayOf(Operand0Ref))
    object VzeroQ : VfpuOpcode("vzero.q", modify = arrayOf(Operand0Ref))

    object VoneS : VfpuOpcode("vone.s", modify = arrayOf(Operand0Ref))
    object VoneP : VfpuOpcode("vone.p", modify = arrayOf(Operand0Ref))
    object VoneT : VfpuOpcode("vone.t", modify = arrayOf(Operand0Ref))
    object VoneQ : VfpuOpcode("vone.q", modify = arrayOf(Operand0Ref))

    object VrcpS : VfpuOpcode("vrcp.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VrcpP : VfpuOpcode("vrcp.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VrcpT : VfpuOpcode("vrcp.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VrcpQ : VfpuOpcode("vrcp.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VrsqS : VfpuOpcode("vrsq.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VrsqP : VfpuOpcode("vrsq.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VrsqT : VfpuOpcode("vrsq.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VrsqQ : VfpuOpcode("vrsq.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VsinS : VfpuOpcode("vsin.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VsinP : VfpuOpcode("vsin.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VsinT : VfpuOpcode("vsin.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VsinQ : VfpuOpcode("vsin.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VcosS : VfpuOpcode("vcos.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcosP : VfpuOpcode("vcos.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcosT : VfpuOpcode("vcos.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcosQ : VfpuOpcode("vcos.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vexp2S : VfpuOpcode("vexp2.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vexp2P : VfpuOpcode("vexp2.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vexp2T : VfpuOpcode("vexp2.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vexp2Q : VfpuOpcode("vexp2.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vlog2S : VfpuOpcode("vlog2.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vlog2P : VfpuOpcode("vlog2.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vlog2T : VfpuOpcode("vlog2.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vlog2Q : VfpuOpcode("vlog2.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VsqrtS : VfpuOpcode("vsqrt.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VsqrtP : VfpuOpcode("vsqrt.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VsqrtT : VfpuOpcode("vsqrt.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VsqrtQ : VfpuOpcode("vsqrt.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VasinS : VfpuOpcode("vasin.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VasinP : VfpuOpcode("vasin.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VasinT : VfpuOpcode("vasin.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VasinQ : VfpuOpcode("vasin.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VnrcpS : VfpuOpcode("vnrcp.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VnrcpP : VfpuOpcode("vnrcp.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VnrcpT : VfpuOpcode("vnrcp.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VnrcpQ : VfpuOpcode("vnrcp.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VnsinS : VfpuOpcode("vnsin.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VnsinP : VfpuOpcode("vnsin.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VnsinT : VfpuOpcode("vnsin.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VnsinQ : VfpuOpcode("vnsin.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vrexp2S : VfpuOpcode("vrexp2.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vrexp2P : VfpuOpcode("vrexp2.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vrexp2T : VfpuOpcode("vrexp2.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vrexp2Q : VfpuOpcode("vrexp2.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VrndiS : VfpuOpcode("vrndi.s", modify = arrayOf(Operand0Ref))
    object VrndiP : VfpuOpcode("vrndi.p", modify = arrayOf(Operand0Ref))
    object VrndiT : VfpuOpcode("vrndi.t", modify = arrayOf(Operand0Ref))
    object VrndiQ : VfpuOpcode("vrndi.q", modify = arrayOf(Operand0Ref))

    object Vrndf1S : VfpuOpcode("vrndf1.s", modify = arrayOf(Operand0Ref))
    object Vrndf1P : VfpuOpcode("vrndf1.p", modify = arrayOf(Operand0Ref))
    object Vrndf1T : VfpuOpcode("vrndf1.t", modify = arrayOf(Operand0Ref))
    object Vrndf1Q : VfpuOpcode("vrndf1.q", modify = arrayOf(Operand0Ref))

    object Vrndf2S : VfpuOpcode("vrndf2.s", modify = arrayOf(Operand0Ref))
    object Vrndf2P : VfpuOpcode("vrndf2.p", modify = arrayOf(Operand0Ref))
    object Vrndf2T : VfpuOpcode("vrndf2.t", modify = arrayOf(Operand0Ref))
    object Vrndf2Q : VfpuOpcode("vrndf2.q", modify = arrayOf(Operand0Ref))

    object Vf2hP : VfpuOpcode("vf2h.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2hQ : VfpuOpcode("vf2h.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vsrt1Q : VfpuOpcode("vsrt1.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vsrt2Q : VfpuOpcode("vsrt2.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vsrt3Q : VfpuOpcode("vsrt3.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vsrt4Q : VfpuOpcode("vsrt4.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vbfy1P : VfpuOpcode("vbfy1.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vbfy1Q : VfpuOpcode("vbfy1.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vbfy2Q : VfpuOpcode("vbfy2.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VocpS : VfpuOpcode("vocp.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VocpP : VfpuOpcode("vocp.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VocpT : VfpuOpcode("vocp.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VocpQ : VfpuOpcode("vocp.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VfadP : VfpuOpcode("vfad.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VfadT : VfpuOpcode("vfad.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VfadQ : VfpuOpcode("vfad.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VavgP : VfpuOpcode("vavg.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VavgT : VfpuOpcode("vavg.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VavgQ : VfpuOpcode("vavg.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vf2inS : VfpuOpcode("vf2in.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2inP : VfpuOpcode("vf2in.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2inT : VfpuOpcode("vf2in.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2inQ : VfpuOpcode("vf2in.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vf2izS : VfpuOpcode("vf2iz.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2izP : VfpuOpcode("vf2iz.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2izT : VfpuOpcode("vf2iz.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2izQ : VfpuOpcode("vf2iz.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vf2iuS : VfpuOpcode("vf2iu.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2iuP : VfpuOpcode("vf2iu.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2iuT : VfpuOpcode("vf2iu.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2iuQ : VfpuOpcode("vf2iu.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vf2idS : VfpuOpcode("vf2id.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2idP : VfpuOpcode("vf2id.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2idT : VfpuOpcode("vf2id.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vf2idQ : VfpuOpcode("vf2id.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vi2fS : VfpuOpcode("vi2f.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vi2fP : VfpuOpcode("vi2f.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vi2fT : VfpuOpcode("vi2f.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vi2fQ : VfpuOpcode("vi2f.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VcmovS : VfpuOpcode("vcmov.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcmovP : VfpuOpcode("vcmov.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcmovT : VfpuOpcode("vcmov.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcmovQ : VfpuOpcode("vcmov.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VcmovtS : VfpuOpcode("vcmovt.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcmovtP : VfpuOpcode("vcmovt.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcmovtT : VfpuOpcode("vcmovt.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcmovtQ : VfpuOpcode("vcmovt.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VcmovfS : VfpuOpcode("vcmovf.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcmovfP : VfpuOpcode("vcmovf.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcmovfT : VfpuOpcode("vcmovf.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VcmovfQ : VfpuOpcode("vcmovf.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VmmulP : VfpuOpcode("vmmul.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VmmulT : VfpuOpcode("vmmul.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VmmulQ : VfpuOpcode("vmmul.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VmsclP : VfpuOpcode("vmscl.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VmsclT : VfpuOpcode("vmscl.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VmsclQ : VfpuOpcode("vmscl.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VqmulQ : VfpuOpcode("vqmul.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VmmovP : VfpuOpcode("vmmov.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VmmovT : VfpuOpcode("vmmov.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VmmovQ : VfpuOpcode("vmmov.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VmidtP : VfpuOpcode("vmidt.p", modify = arrayOf(Operand0Ref))
    object VmidtT : VfpuOpcode("vmidt.t", modify = arrayOf(Operand0Ref))
    object VmidtQ : VfpuOpcode("vmidt.q", modify = arrayOf(Operand0Ref))

    object VmzeroP : VfpuOpcode("vmzero.p", modify = arrayOf(Operand0Ref))
    object VmzeroT : VfpuOpcode("vmzero.t", modify = arrayOf(Operand0Ref))
    object VmzeroQ : VfpuOpcode("vmzero.q", modify = arrayOf(Operand0Ref))

    object VmoneP : VfpuOpcode("vmone.p", modify = arrayOf(Operand0Ref))
    object VmoneT : VfpuOpcode("vmone.t", modify = arrayOf(Operand0Ref))
    object VmoneQ : VfpuOpcode("vmone.q", modify = arrayOf(Operand0Ref))

    object VrotP : VfpuOpcode("vrot.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VrotT : VfpuOpcode("vrot.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VrotQ : VfpuOpcode("vrot.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vt4444Q : VfpuOpcode("vt4444.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vt5551Q : VfpuOpcode("vt5551.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vt5650Q : VfpuOpcode("vt5650.q", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VcrsT : VfpuOpcode("vcrs.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VcrspT : VfpuOpcode("vcrsp.t", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object VdetP : VfpuOpcode("vdet.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))

    object Vus2iS : VfpuOpcode("vus2i.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vus2iP : VfpuOpcode("vus2i.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vs2iS : VfpuOpcode("vs2i.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vs2iP : VfpuOpcode("vs2i.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vh2fS : VfpuOpcode("vh2f.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vh2fP : VfpuOpcode("vh2f.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object VsocpS : VfpuOpcode("vsocp.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VsocpP : VfpuOpcode("vsocp.p", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))

    object Vuc2iS : VfpuOpcode("vuc2i.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object Vc2iS : VfpuOpcode("vc2i.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VrndsS : VfpuOpcode("vrnds.s")
    object VsbzS : VfpuOpcode("vsbz.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VsbnS : VfpuOpcode("vsbn.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref, Operand2Ref))
    object VlgbS : VfpuOpcode("vlgb.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object VwbnS : VfpuOpcode("vwbn.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand1Ref))
    object ViimS : VfpuOpcode("viim.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand0Ref))
    object VfimS : VfpuOpcode("vfim.s", modify = arrayOf(Operand0Ref), use = arrayOf(Operand0Ref))

    object VpfxsS : VfpuOpcode("vpfxs")
    object VpfxtS : VfpuOpcode("vpfxt")
    object VpfxdS : VfpuOpcode("vpfxd")
}
