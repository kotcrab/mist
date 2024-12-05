package mist.asm.mips

import mist.asm.Reg

sealed class GprReg(name: String, id: Int, bitsSize: Int = 32) : Reg(name, id, bitsSize) {
  companion object {
    // warning: "by lazy" here is workaround for KT-8970
    private val gprRegs by lazy {
      arrayOf(
        Zero,
        At,
        V0, V1,
        A0, A1, A2, A3,
        T0, T1, T2, T3, T4, T5, T6, T7, T8, T9,
        S0, S1, S2, S3, S4, S5, S6, S7,
        K0, K1,
        Gp, Sp, Fp, Ra,
        Pc, Lo, Hi
      )
    }

    fun forId(id: Int): GprReg {
      return Reg.forId(values(), id)
    }

    fun values(): Array<GprReg> {
      return gprRegs
    }
  }

  object Zero : GprReg("zero", 0)
  object At : GprReg("at", 1)
  object V0 : GprReg("v0", 2)
  object V1 : GprReg("v1", 3)
  object A0 : GprReg("a0", 4)
  object A1 : GprReg("a1", 5)
  object A2 : GprReg("a2", 6)
  object A3 : GprReg("a3", 7)
  object T0 : GprReg("t0", 8)
  object T1 : GprReg("t1", 9)
  object T2 : GprReg("t2", 10)
  object T3 : GprReg("t3", 11)
  object T4 : GprReg("t4", 12)
  object T5 : GprReg("t5", 13)
  object T6 : GprReg("t6", 14)
  object T7 : GprReg("t7", 15)
  object T8 : GprReg("t8", 24)
  object T9 : GprReg("t9", 25)
  object S0 : GprReg("s0", 16)
  object S1 : GprReg("s1", 17)
  object S2 : GprReg("s2", 18)
  object S3 : GprReg("s3", 19)
  object S4 : GprReg("s4", 20)
  object S5 : GprReg("s5", 21)
  object S6 : GprReg("s6", 22)
  object S7 : GprReg("s7", 23)
  object K0 : GprReg("k0", 26)
  object K1 : GprReg("k1", 27)
  object Gp : GprReg("gp", 28)
  object Sp : GprReg("sp", 29)
  object Fp : GprReg("fp", 30)
  object Ra : GprReg("ra", 31)

  // special purpose - directly inaccessible registers

  object Pc : GprReg("pc", -1)
  object Lo : GprReg("lo", -1)
  object Hi : GprReg("hi", -1)
}
