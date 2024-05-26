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

import mist.asm.Reg

/** @author Kotcrab */

sealed class VfpuReg(
  val sName: String,
  val pName: String = sName,
  val tName: String = sName,
  val qName: String = sName,
  val mName: String = sName,
  val eName: String = sName,
  val teName: String = sName,
  id: Int = -1,
  bitsSize: Int = 32
) : Reg(sName, id, bitsSize) {
  companion object {
    // warning: "by lazy" here is workaround for KT-8970
    private val vfpuRegs: Array<VfpuReg> by lazy {
      arrayOf(
        V00, V01, V02, V03, V04, V05, V06, V07, V08, V09, V0A, V0B, V0C, V0D, V0E, V0F,
        V10, V11, V12, V13, V14, V15, V16, V17, V18, V19, V1A, V1B, V1C, V1D, V1E, V1F,
        V20, V21, V22, V23, V24, V25, V26, V27, V28, V29, V2A, V2B, V2C, V2D, V2E, V2F,
        V30, V31, V32, V33, V34, V35, V36, V37, V38, V39, V3A, V3B, V3C, V3D, V3E, V3F,
        V40, V41, V42, V43, V44, V45, V46, V47, V48, V49, V4A, V4B, V4C, V4D, V4E, V4F,
        V50, V51, V52, V53, V54, V55, V56, V57, V58, V59, V5A, V5B, V5C, V5D, V5E, V5F,
        V60, V61, V62, V63, V64, V65, V66, V67, V68, V69, V6A, V6B, V6C, V6D, V6E, V6F,
        V70, V71, V72, V73, V74, V75, V76, V77, V78, V79, V7A, V7B, V7C, V7D, V7E, V7F,
        Spfx, Tpfx, Dpfx, Cc, Inf4, Rsv5, Rsv6, Rev, Rcx0, Rcx1, Rcx2, Rcx3, Rcx4, Rcx5, Rcx6, Rcx7
      )
    }

    fun forId(id: Int): VfpuReg {
      return Reg.forId(values(), id)
    }

    fun controlForId(id: Int): VfpuReg {
      return when (id) {
        0 -> Spfx
        1 -> Tpfx
        2 -> Dpfx
        3 -> Cc
        4 -> Inf4
        5 -> Rsv5
        6 -> Rsv6
        7 -> Rev
        8 -> Rcx0
        9 -> Rcx1
        10 -> Rcx2
        11 -> Rcx3
        12 -> Rcx4
        13 -> Rcx5
        14 -> Rcx6
        15 -> Rcx7
        else -> Reg.forId(values(), id)
      }
    }

    fun values(): Array<VfpuReg> {
      return vfpuRegs
    }
  }

  // TODO refactor
  object V00 : VfpuReg("S000.s", "C000.p", "C000.t", "C000.q", "M000.q", "E000.q", "R000.q", 0x00)
  object V01 : VfpuReg("S010.s", "C010.p", "C010.t", "C010.q", "M010.q", "E001.q", "R001.q", 0x01)
  object V02 : VfpuReg("S020.s", "C020.p", "C020.t", "C020.q", "M020.q", "E002.q", "R002.q", 0x02)
  object V03 : VfpuReg("S030.s", "C030.p", "C030.t", "C030.q", "M030.q", "E003.q", "R003.q", 0x03)
  object V04 : VfpuReg("S100.s", "C100.p", "C100.t", "C100.q", "M100.q", "E100.q", "R100.q", 0x04)
  object V05 : VfpuReg("S110.s", "C110.p", "C110.t", "C110.q", "M110.q", "E101.q", "R101.q", 0x05)
  object V06 : VfpuReg("S120.s", "C120.p", "C120.t", "C120.q", "M120.q", "E102.q", "R102.q", 0x06)
  object V07 : VfpuReg("S130.s", "C130.p", "C130.t", "C130.q", "M130.q", "E103.q", "R103.q", 0x07)
  object V08 : VfpuReg("S200.s", "C200.p", "C200.t", "C200.q", "M200.q", "E200.q", "R200.q", 0x08)
  object V09 : VfpuReg("S210.s", "C210.p", "C210.t", "C210.q", "M210.q", "E201.q", "R201.q", 0x09)
  object V0A : VfpuReg("S220.s", "C220.p", "C220.t", "C220.q", "M220.q", "E202.q", "R202.q", 0x0A)
  object V0B : VfpuReg("S230.s", "C230.p", "C230.t", "C230.q", "M230.q", "E203.q", "R203.q", 0x0B)
  object V0C : VfpuReg("S300.s", "C300.p", "C300.t", "C300.q", "M300.q", "E300.q", "R300.q", 0x0C)
  object V0D : VfpuReg("S310.s", "C310.p", "C310.t", "C310.q", "M310.q", "E301.q", "R301.q", 0x0D)
  object V0E : VfpuReg("S320.s", "C320.p", "C320.t", "C320.q", "M320.q", "E302.q", "R302.q", 0x0E)
  object V0F : VfpuReg("S330.s", "C330.p", "C330.t", "C330.q", "M330.q", "E303.q", "R303.q", 0x0F)
  object V10 : VfpuReg("S400.s", "C400.p", "C400.t", "C400.q", "M400.q", "E400.q", "R400.q", 0x10)
  object V11 : VfpuReg("S410.s", "C410.p", "C410.t", "C410.q", "M410.q", "E401.q", "R401.q", 0x11)
  object V12 : VfpuReg("S420.s", "C420.p", "C420.t", "C420.q", "M420.q", "E402.q", "R402.q", 0x12)
  object V13 : VfpuReg("S430.s", "C430.p", "C430.t", "C430.q", "M430.q", "E403.q", "R403.q", 0x13)
  object V14 : VfpuReg("S500.s", "C500.p", "C500.t", "C500.q", "M500.q", "E500.q", "R500.q", 0x14)
  object V15 : VfpuReg("S510.s", "C510.p", "C510.t", "C510.q", "M510.q", "E501.q", "R501.q", 0x15)
  object V16 : VfpuReg("S520.s", "C520.p", "C520.t", "C520.q", "M520.q", "E502.q", "R502.q", 0x16)
  object V17 : VfpuReg("S530.s", "C530.p", "C530.t", "C530.q", "M530.q", "E503.q", "R503.q", 0x17)
  object V18 : VfpuReg("S600.s", "C600.p", "C600.t", "C600.q", "M600.q", "E600.q", "R600.q", 0x18)
  object V19 : VfpuReg("S610.s", "C610.p", "C610.t", "C610.q", "M610.q", "E601.q", "R601.q", 0x19)
  object V1A : VfpuReg("S620.s", "C620.p", "C620.t", "C620.q", "M620.q", "E602.q", "R602.q", 0x1A)
  object V1B : VfpuReg("S630.s", "C630.p", "C630.t", "C630.q", "M630.q", "E603.q", "R603.q", 0x1B)
  object V1C : VfpuReg("S700.s", "C700.p", "C700.t", "C700.q", "M700.q", "E700.q", "R700.q", 0x1C)
  object V1D : VfpuReg("S710.s", "C710.p", "C710.t", "C710.q", "M710.q", "E701.q", "R701.q", 0x1D)
  object V1E : VfpuReg("S720.s", "C720.p", "C720.t", "C720.q", "M720.q", "E702.q", "R702.q", 0x1E)
  object V1F : VfpuReg("S730.s", "C730.p", "C730.t", "C730.q", "M730.q", "E703.q", "R703.q", 0x1F)
  object V20 : VfpuReg("S001.s", "R000.p", "R000.t", "R000.q", "E000.q", "M000.q", "C000.q", 0x20)
  object V21 : VfpuReg("S011.s", "R001.p", "R001.t", "R001.q", "E001.q", "M010.q", "C010.q", 0x21)
  object V22 : VfpuReg("S021.s", "R002.p", "R002.t", "R002.q", "E002.q", "M020.q", "C020.q", 0x22)
  object V23 : VfpuReg("S031.s", "R003.p", "R003.t", "R003.q", "E003.q", "M030.q", "C030.q", 0x23)
  object V24 : VfpuReg("S101.s", "R100.p", "R100.t", "R100.q", "E100.q", "M100.q", "C100.q", 0x24)
  object V25 : VfpuReg("S111.s", "R101.p", "R101.t", "R101.q", "E101.q", "M110.q", "C110.q", 0x25)
  object V26 : VfpuReg("S121.s", "R102.p", "R102.t", "R102.q", "E102.q", "M120.q", "C120.q", 0x26)
  object V27 : VfpuReg("S131.s", "R103.p", "R103.t", "R103.q", "E103.q", "M130.q", "C130.q", 0x27)
  object V28 : VfpuReg("S201.s", "R200.p", "R200.t", "R200.q", "E200.q", "M200.q", "C200.q", 0x28)
  object V29 : VfpuReg("S211.s", "R201.p", "R201.t", "R201.q", "E201.q", "M210.q", "C210.q", 0x29)
  object V2A : VfpuReg("S221.s", "R202.p", "R202.t", "R202.q", "E202.q", "M220.q", "C220.q", 0x2A)
  object V2B : VfpuReg("S231.s", "R203.p", "R203.t", "R203.q", "E203.q", "M230.q", "C230.q", 0x2B)
  object V2C : VfpuReg("S301.s", "R300.p", "R300.t", "R300.q", "E300.q", "M300.q", "C300.q", 0x2C)
  object V2D : VfpuReg("S311.s", "R301.p", "R301.t", "R301.q", "E301.q", "M310.q", "C310.q", 0x2D)
  object V2E : VfpuReg("S321.s", "R302.p", "R302.t", "R302.q", "E302.q", "M320.q", "C320.q", 0x2E)
  object V2F : VfpuReg("S331.s", "R303.p", "R303.t", "R303.q", "E303.q", "M330.q", "C330.q", 0x2F)
  object V30 : VfpuReg("S401.s", "R400.p", "R400.t", "R400.q", "E400.q", "M400.q", "C400.q", 0x30)
  object V31 : VfpuReg("S411.s", "R401.p", "R401.t", "R401.q", "E401.q", "M410.q", "C410.q", 0x31)
  object V32 : VfpuReg("S421.s", "R402.p", "R402.t", "R402.q", "E402.q", "M420.q", "C420.q", 0x32)
  object V33 : VfpuReg("S431.s", "R403.p", "R403.t", "R403.q", "E403.q", "M430.q", "C430.q", 0x33)
  object V34 : VfpuReg("S501.s", "R500.p", "R500.t", "R500.q", "E500.q", "M500.q", "C500.q", 0x34)
  object V35 : VfpuReg("S511.s", "R501.p", "R501.t", "R501.q", "E501.q", "M510.q", "C510.q", 0x35)
  object V36 : VfpuReg("S521.s", "R502.p", "R502.t", "R502.q", "E502.q", "M520.q", "C520.q", 0x36)
  object V37 : VfpuReg("S531.s", "R503.p", "R503.t", "R503.q", "E503.q", "M530.q", "C530.q", 0x37)
  object V38 : VfpuReg("S601.s", "R600.p", "R600.t", "R600.q", "E600.q", "M600.q", "C600.q", 0x38)
  object V39 : VfpuReg("S611.s", "R601.p", "R601.t", "R601.q", "E601.q", "M610.q", "C610.q", 0x39)
  object V3A : VfpuReg("S621.s", "R602.p", "R602.t", "R602.q", "E602.q", "M620.q", "C620.q", 0x3A)
  object V3B : VfpuReg("S631.s", "R603.p", "R603.t", "R603.q", "E603.q", "M630.q", "C630.q", 0x3B)
  object V3C : VfpuReg("S701.s", "R700.p", "R700.t", "R700.q", "E700.q", "M700.q", "C700.q", 0x3C)
  object V3D : VfpuReg("S711.s", "R701.p", "R701.t", "R701.q", "E701.q", "M710.q", "C710.q", 0x3D)
  object V3E : VfpuReg("S721.s", "R702.p", "R702.t", "R702.q", "E702.q", "M720.q", "C720.q", 0x3E)
  object V3F : VfpuReg("S731.s", "R703.p", "R703.t", "R703.q", "E703.q", "M730.q", "C730.q", 0x3F)
  object V40 : VfpuReg("S002.s", "C002.p", "C001.t", "C002.q", "M002.q", "E020.q", "R010.q", 0x40)
  object V41 : VfpuReg("S012.s", "C012.p", "C011.t", "C012.q", "M012.q", "E021.q", "R011.q", 0x41)
  object V42 : VfpuReg("S022.s", "C022.p", "C021.t", "C022.q", "M022.q", "E022.q", "R012.q", 0x42)
  object V43 : VfpuReg("S032.s", "C032.p", "C031.t", "C032.q", "M032.q", "E023.q", "R013.q", 0x43)
  object V44 : VfpuReg("S102.s", "C102.p", "C101.t", "C102.q", "M102.q", "E120.q", "R110.q", 0x44)
  object V45 : VfpuReg("S112.s", "C112.p", "C111.t", "C112.q", "M112.q", "E121.q", "R111.q", 0x45)
  object V46 : VfpuReg("S122.s", "C122.p", "C121.t", "C122.q", "M122.q", "E122.q", "R112.q", 0x46)
  object V47 : VfpuReg("S132.s", "C132.p", "C131.t", "C132.q", "M132.q", "E123.q", "R113.q", 0x47)
  object V48 : VfpuReg("S202.s", "C202.p", "C201.t", "C202.q", "M202.q", "E220.q", "R210.q", 0x48)
  object V49 : VfpuReg("S212.s", "C212.p", "C211.t", "C212.q", "M212.q", "E221.q", "R211.q", 0x49)
  object V4A : VfpuReg("S222.s", "C222.p", "C221.t", "C222.q", "M222.q", "E222.q", "R212.q", 0x4A)
  object V4B : VfpuReg("S232.s", "C232.p", "C231.t", "C232.q", "M232.q", "E223.q", "R213.q", 0x4B)
  object V4C : VfpuReg("S302.s", "C302.p", "C301.t", "C302.q", "M302.q", "E320.q", "R310.q", 0x4C)
  object V4D : VfpuReg("S312.s", "C312.p", "C311.t", "C312.q", "M312.q", "E321.q", "R311.q", 0x4D)
  object V4E : VfpuReg("S322.s", "C322.p", "C321.t", "C322.q", "M322.q", "E322.q", "R312.q", 0x4E)
  object V4F : VfpuReg("S332.s", "C332.p", "C331.t", "C332.q", "M332.q", "E323.q", "R313.q", 0x4F)
  object V50 : VfpuReg("S402.s", "C402.p", "C401.t", "C402.q", "M402.q", "E420.q", "R410.q", 0x50)
  object V51 : VfpuReg("S412.s", "C412.p", "C411.t", "C412.q", "M412.q", "E421.q", "R411.q", 0x51)
  object V52 : VfpuReg("S422.s", "C422.p", "C421.t", "C422.q", "M422.q", "E422.q", "R412.q", 0x52)
  object V53 : VfpuReg("S432.s", "C432.p", "C431.t", "C432.q", "M432.q", "E423.q", "R413.q", 0x53)
  object V54 : VfpuReg("S502.s", "C502.p", "C501.t", "C502.q", "M502.q", "E520.q", "R510.q", 0x54)
  object V55 : VfpuReg("S512.s", "C512.p", "C511.t", "C512.q", "M512.q", "E521.q", "R511.q", 0x55)
  object V56 : VfpuReg("S522.s", "C522.p", "C521.t", "C522.q", "M522.q", "E522.q", "R512.q", 0x56)
  object V57 : VfpuReg("S532.s", "C532.p", "C531.t", "C532.q", "M532.q", "E523.q", "R513.q", 0x57)
  object V58 : VfpuReg("S602.s", "C602.p", "C601.t", "C602.q", "M602.q", "E620.q", "R610.q", 0x58)
  object V59 : VfpuReg("S612.s", "C612.p", "C611.t", "C612.q", "M612.q", "E621.q", "R611.q", 0x59)
  object V5A : VfpuReg("S622.s", "C622.p", "C621.t", "C622.q", "M622.q", "E622.q", "R612.q", 0x5A)
  object V5B : VfpuReg("S632.s", "C632.p", "C631.t", "C632.q", "M632.q", "E623.q", "R613.q", 0x5B)
  object V5C : VfpuReg("S702.s", "C702.p", "C701.t", "C702.q", "M702.q", "E720.q", "R710.q", 0x5C)
  object V5D : VfpuReg("S712.s", "C712.p", "C711.t", "C712.q", "M712.q", "E721.q", "R711.q", 0x5D)
  object V5E : VfpuReg("S722.s", "C722.p", "C721.t", "C722.q", "M722.q", "E722.q", "R712.q", 0x5E)
  object V5F : VfpuReg("S732.s", "C732.p", "C731.t", "C732.q", "M732.q", "E723.q", "R713.q", 0x5F)
  object V60 : VfpuReg("S003.s", "R020.p", "R010.t", "R020.q", "E020.q", "M002.q", "C001.q", 0x60)
  object V61 : VfpuReg("S013.s", "R021.p", "R011.t", "R021.q", "E021.q", "M012.q", "C011.q", 0x61)
  object V62 : VfpuReg("S023.s", "R022.p", "R012.t", "R022.q", "E022.q", "M022.q", "C021.q", 0x62)
  object V63 : VfpuReg("S033.s", "R023.p", "R013.t", "R023.q", "E023.q", "M032.q", "C031.q", 0x63)
  object V64 : VfpuReg("S103.s", "R120.p", "R110.t", "R120.q", "E120.q", "M102.q", "C101.q", 0x64)
  object V65 : VfpuReg("S113.s", "R121.p", "R111.t", "R121.q", "E121.q", "M112.q", "C111.q", 0x65)
  object V66 : VfpuReg("S123.s", "R122.p", "R112.t", "R122.q", "E122.q", "M122.q", "C121.q", 0x66)
  object V67 : VfpuReg("S133.s", "R123.p", "R113.t", "R123.q", "E123.q", "M132.q", "C131.q", 0x67)
  object V68 : VfpuReg("S203.s", "R220.p", "R210.t", "R220.q", "E220.q", "M202.q", "C201.q", 0x68)
  object V69 : VfpuReg("S213.s", "R221.p", "R211.t", "R221.q", "E221.q", "M212.q", "C211.q", 0x69)
  object V6A : VfpuReg("S223.s", "R222.p", "R212.t", "R222.q", "E222.q", "M222.q", "C221.q", 0x6A)
  object V6B : VfpuReg("S233.s", "R223.p", "R213.t", "R223.q", "E223.q", "M232.q", "C231.q", 0x6B)
  object V6C : VfpuReg("S303.s", "R320.p", "R310.t", "R320.q", "E320.q", "M302.q", "C301.q", 0x6C)
  object V6D : VfpuReg("S313.s", "R321.p", "R311.t", "R321.q", "E321.q", "M312.q", "C311.q", 0x6D)
  object V6E : VfpuReg("S323.s", "R322.p", "R312.t", "R322.q", "E322.q", "M322.q", "C321.q", 0x6E)
  object V6F : VfpuReg("S333.s", "R323.p", "R313.t", "R323.q", "E323.q", "M332.q", "C331.q", 0x6F)
  object V70 : VfpuReg("S403.s", "R420.p", "R410.t", "R420.q", "E420.q", "M402.q", "C401.q", 0x70)
  object V71 : VfpuReg("S413.s", "R421.p", "R411.t", "R421.q", "E421.q", "M412.q", "C411.q", 0x71)
  object V72 : VfpuReg("S423.s", "R422.p", "R412.t", "R422.q", "E422.q", "M422.q", "C421.q", 0x72)
  object V73 : VfpuReg("S433.s", "R423.p", "R413.t", "R423.q", "E423.q", "M432.q", "C431.q", 0x73)
  object V74 : VfpuReg("S503.s", "R520.p", "R510.t", "R520.q", "E520.q", "M502.q", "C501.q", 0x74)
  object V75 : VfpuReg("S513.s", "R521.p", "R511.t", "R521.q", "E521.q", "M512.q", "C511.q", 0x75)
  object V76 : VfpuReg("S523.s", "R522.p", "R512.t", "R522.q", "E522.q", "M522.q", "C521.q", 0x76)
  object V77 : VfpuReg("S533.s", "R523.p", "R513.t", "R523.q", "E523.q", "M532.q", "C531.q", 0x77)
  object V78 : VfpuReg("S603.s", "R620.p", "R610.t", "R620.q", "E620.q", "M602.q", "C601.q", 0x78)
  object V79 : VfpuReg("S613.s", "R621.p", "R611.t", "R621.q", "E621.q", "M612.q", "C611.q", 0x79)
  object V7A : VfpuReg("S623.s", "R622.p", "R612.t", "R622.q", "E622.q", "M622.q", "C621.q", 0x7A)
  object V7B : VfpuReg("S633.s", "R623.p", "R613.t", "R623.q", "E623.q", "M632.q", "C631.q", 0x7B)
  object V7C : VfpuReg("S703.s", "R720.p", "R710.t", "R720.q", "E720.q", "M702.q", "C701.q", 0x7C)
  object V7D : VfpuReg("S713.s", "R721.p", "R711.t", "R721.q", "E721.q", "M712.q", "C711.q", 0x7D)
  object V7E : VfpuReg("S723.s", "R722.p", "R712.t", "R722.q", "E722.q", "M722.q", "C721.q", 0x7E)
  object V7F : VfpuReg("S733.s", "R723.p", "R713.t", "R723.q", "E723.q", "M732.q", "C731.q", 0x7F)

  // special purpose - directly inaccessible registers

  object Spfx : VfpuReg("SPFX")
  object Tpfx : VfpuReg("TPFX")
  object Dpfx : VfpuReg("DPFX")
  object Cc : VfpuReg("CC")
  object Inf4 : VfpuReg("INF4")
  object Rsv5 : VfpuReg("RSV5")
  object Rsv6 : VfpuReg("RSV6")
  object Rev : VfpuReg("REV")
  object Rcx0 : VfpuReg("RCX0")
  object Rcx1 : VfpuReg("RCX1")
  object Rcx2 : VfpuReg("RCX2")
  object Rcx3 : VfpuReg("RCX3")
  object Rcx4 : VfpuReg("RCX4")
  object Rcx5 : VfpuReg("RCX5")
  object Rcx6 : VfpuReg("RCX6")
  object Rcx7 : VfpuReg("RCX7")
}
