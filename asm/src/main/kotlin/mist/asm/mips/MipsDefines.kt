package mist.asm.mips

object MipsDefines {
  const val SPECIAL = 0b000_000
  const val SPECIAL2 = 0b011_100
  const val SPECIAL3 = 0b011_111
  const val REGIMM = 0b000_001
  const val COP0 = 0b010_000
  const val COP1 = 0b010_001
  const val COP2 = 0b010_010
  const val COP3_COP1X = 0b010_011
  const val FMT_S = 16
  const val FMT_D = 17
  const val FMT_W = 20
  const val FMT_L = 21
  const val FMT3_S = 0
  const val FMT3_D = 1
  const val FMT3_W = 4
  const val FMT3_L = 5
}
