package mist.asm.mips

import mist.asm.OpcodeFlag

abstract class MipsOpcodeFlag : OpcodeFlag()

object BranchLikely : MipsOpcodeFlag()
