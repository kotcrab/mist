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
    object V00 : VfpuReg("S000", "C000", "C000", "C000", "M000", "E000", "R000",0x00)
    object V01 : VfpuReg("S010", "C010", "C010", "C010", "M010", "E001", "R001",0x01)
    object V02 : VfpuReg("S020", "C020", "C020", "C020", "M020", "E002", "R002",0x02)
    object V03 : VfpuReg("S030", "C030", "C030", "C030", "M030", "E003", "R003",0x03)
    object V04 : VfpuReg("S100", "C100", "C100", "C100", "M100", "E100", "R100",0x04)
    object V05 : VfpuReg("S110", "C110", "C110", "C110", "M110", "E101", "R101",0x05)
    object V06 : VfpuReg("S120", "C120", "C120", "C120", "M120", "E102", "R102",0x06)
    object V07 : VfpuReg("S130", "C130", "C130", "C130", "M130", "E103", "R103",0x07)
    object V08 : VfpuReg("S200", "C200", "C200", "C200", "M200", "E200", "R200",0x08)
    object V09 : VfpuReg("S210", "C210", "C210", "C210", "M210", "E201", "R201",0x09)
    object V0A : VfpuReg("S220", "C220", "C220", "C220", "M220", "E202", "R202",0x0A)
    object V0B : VfpuReg("S230", "C230", "C230", "C230", "M230", "E203", "R203",0x0B)
    object V0C : VfpuReg("S300", "C300", "C300", "C300", "M300", "E300", "R300",0x0C)
    object V0D : VfpuReg("S310", "C310", "C310", "C310", "M310", "E301", "R301",0x0D)
    object V0E : VfpuReg("S320", "C320", "C320", "C320", "M320", "E302", "R302",0x0E)
    object V0F : VfpuReg("S330", "C330", "C330", "C330", "M330", "E303", "R303",0x0F)
    object V10 : VfpuReg("S400", "C400", "C400", "C400", "M400", "E400", "R400",0x10)
    object V11 : VfpuReg("S410", "C410", "C410", "C410", "M410", "E401", "R401",0x11)
    object V12 : VfpuReg("S420", "C420", "C420", "C420", "M420", "E402", "R402",0x12)
    object V13 : VfpuReg("S430", "C430", "C430", "C430", "M430", "E403", "R403",0x13)
    object V14 : VfpuReg("S500", "C500", "C500", "C500", "M500", "E500", "R500",0x14)
    object V15 : VfpuReg("S510", "C510", "C510", "C510", "M510", "E501", "R501",0x15)
    object V16 : VfpuReg("S520", "C520", "C520", "C520", "M520", "E502", "R502",0x16)
    object V17 : VfpuReg("S530", "C530", "C530", "C530", "M530", "E503", "R503",0x17)
    object V18 : VfpuReg("S600", "C600", "C600", "C600", "M600", "E600", "R600",0x18)
    object V19 : VfpuReg("S610", "C610", "C610", "C610", "M610", "E601", "R601",0x19)
    object V1A : VfpuReg("S620", "C620", "C620", "C620", "M620", "E602", "R602",0x1A)
    object V1B : VfpuReg("S630", "C630", "C630", "C630", "M630", "E603", "R603",0x1B)
    object V1C : VfpuReg("S700", "C700", "C700", "C700", "M700", "E700", "R700",0x1C)
    object V1D : VfpuReg("S710", "C710", "C710", "C710", "M710", "E701", "R701",0x1D)
    object V1E : VfpuReg("S720", "C720", "C720", "C720", "M720", "E702", "R702",0x1E)
    object V1F : VfpuReg("S730", "C730", "C730", "C730", "M730", "E703", "R703",0x1F)
    object V20 : VfpuReg("S001", "R000", "R000", "R000", "E000", "M000", "C000",0x20)
    object V21 : VfpuReg("S011", "R001", "R001", "R001", "E001", "M010", "C010",0x21)
    object V22 : VfpuReg("S021", "R002", "R002", "R002", "E002", "M020", "C020",0x22)
    object V23 : VfpuReg("S031", "R003", "R003", "R003", "E003", "M030", "C030",0x23)
    object V24 : VfpuReg("S101", "R100", "R100", "R100", "E100", "M100", "C100",0x24)
    object V25 : VfpuReg("S111", "R101", "R101", "R101", "E101", "M110", "C110",0x25)
    object V26 : VfpuReg("S121", "R102", "R102", "R102", "E102", "M120", "C120",0x26)
    object V27 : VfpuReg("S131", "R103", "R103", "R103", "E103", "M130", "C130",0x27)
    object V28 : VfpuReg("S201", "R200", "R200", "R200", "E200", "M200", "C200",0x28)
    object V29 : VfpuReg("S211", "R201", "R201", "R201", "E201", "M210", "C210",0x29)
    object V2A : VfpuReg("S221", "R202", "R202", "R202", "E202", "M220", "C220",0x2A)
    object V2B : VfpuReg("S231", "R203", "R203", "R203", "E203", "M230", "C230",0x2B)
    object V2C : VfpuReg("S301", "R300", "R300", "R300", "E300", "M300", "C300",0x2C)
    object V2D : VfpuReg("S311", "R301", "R301", "R301", "E301", "M310", "C310",0x2D)
    object V2E : VfpuReg("S321", "R302", "R302", "R302", "E302", "M320", "C320",0x2E)
    object V2F : VfpuReg("S331", "R303", "R303", "R303", "E303", "M330", "C330",0x2F)
    object V30 : VfpuReg("S401", "R400", "R400", "R400", "E400", "M400", "C400",0x30)
    object V31 : VfpuReg("S411", "R401", "R401", "R401", "E401", "M410", "C410",0x31)
    object V32 : VfpuReg("S421", "R402", "R402", "R402", "E402", "M420", "C420",0x32)
    object V33 : VfpuReg("S431", "R403", "R403", "R403", "E403", "M430", "C430",0x33)
    object V34 : VfpuReg("S501", "R500", "R500", "R500", "E500", "M500", "C500",0x34)
    object V35 : VfpuReg("S511", "R501", "R501", "R501", "E501", "M510", "C510",0x35)
    object V36 : VfpuReg("S521", "R502", "R502", "R502", "E502", "M520", "C520",0x36)
    object V37 : VfpuReg("S531", "R503", "R503", "R503", "E503", "M530", "C530",0x37)
    object V38 : VfpuReg("S601", "R600", "R600", "R600", "E600", "M600", "C600",0x38)
    object V39 : VfpuReg("S611", "R601", "R601", "R601", "E601", "M610", "C610",0x39)
    object V3A : VfpuReg("S621", "R602", "R602", "R602", "E602", "M620", "C620",0x3A)
    object V3B : VfpuReg("S631", "R603", "R603", "R603", "E603", "M630", "C630",0x3B)
    object V3C : VfpuReg("S701", "R700", "R700", "R700", "E700", "M700", "C700",0x3C)
    object V3D : VfpuReg("S711", "R701", "R701", "R701", "E701", "M710", "C710",0x3D)
    object V3E : VfpuReg("S721", "R702", "R702", "R702", "E702", "M720", "C720",0x3E)
    object V3F : VfpuReg("S731", "R703", "R703", "R703", "E703", "M730", "C730",0x3F)
    object V40 : VfpuReg("S002", "C002", "C001", "C002", "M002", "E020", "R010",0x40)
    object V41 : VfpuReg("S012", "C012", "C011", "C012", "M012", "E021", "R011",0x41)
    object V42 : VfpuReg("S022", "C022", "C021", "C022", "M022", "E022", "R012",0x42)
    object V43 : VfpuReg("S032", "C032", "C031", "C032", "M032", "E023", "R013",0x43)
    object V44 : VfpuReg("S102", "C102", "C101", "C102", "M102", "E120", "R110",0x44)
    object V45 : VfpuReg("S112", "C112", "C111", "C112", "M112", "E121", "R111",0x45)
    object V46 : VfpuReg("S122", "C122", "C121", "C122", "M122", "E122", "R112",0x46)
    object V47 : VfpuReg("S132", "C132", "C131", "C132", "M132", "E123", "R113",0x47)
    object V48 : VfpuReg("S202", "C202", "C201", "C202", "M202", "E220", "R210",0x48)
    object V49 : VfpuReg("S212", "C212", "C211", "C212", "M212", "E221", "R211",0x49)
    object V4A : VfpuReg("S222", "C222", "C221", "C222", "M222", "E222", "R212",0x4A)
    object V4B : VfpuReg("S232", "C232", "C231", "C232", "M232", "E223", "R213",0x4B)
    object V4C : VfpuReg("S302", "C302", "C301", "C302", "M302", "E320", "R310",0x4C)
    object V4D : VfpuReg("S312", "C312", "C311", "C312", "M312", "E321", "R311",0x4D)
    object V4E : VfpuReg("S322", "C322", "C321", "C322", "M322", "E322", "R312",0x4E)
    object V4F : VfpuReg("S332", "C332", "C331", "C332", "M332", "E323", "R313",0x4F)
    object V50 : VfpuReg("S402", "C402", "C401", "C402", "M402", "E420", "R410",0x50)
    object V51 : VfpuReg("S412", "C412", "C411", "C412", "M412", "E421", "R411",0x51)
    object V52 : VfpuReg("S422", "C422", "C421", "C422", "M422", "E422", "R412",0x52)
    object V53 : VfpuReg("S432", "C432", "C431", "C432", "M432", "E423", "R413",0x53)
    object V54 : VfpuReg("S502", "C502", "C501", "C502", "M502", "E520", "R510",0x54)
    object V55 : VfpuReg("S512", "C512", "C511", "C512", "M512", "E521", "R511",0x55)
    object V56 : VfpuReg("S522", "C522", "C521", "C522", "M522", "E522", "R512",0x56)
    object V57 : VfpuReg("S532", "C532", "C531", "C532", "M532", "E523", "R513",0x57)
    object V58 : VfpuReg("S602", "C602", "C601", "C602", "M602", "E620", "R610",0x58)
    object V59 : VfpuReg("S612", "C612", "C611", "C612", "M612", "E621", "R611",0x59)
    object V5A : VfpuReg("S622", "C622", "C621", "C622", "M622", "E622", "R612",0x5A)
    object V5B : VfpuReg("S632", "C632", "C631", "C632", "M632", "E623", "R613",0x5B)
    object V5C : VfpuReg("S702", "C702", "C701", "C702", "M702", "E720", "R710",0x5C)
    object V5D : VfpuReg("S712", "C712", "C711", "C712", "M712", "E721", "R711",0x5D)
    object V5E : VfpuReg("S722", "C722", "C721", "C722", "M722", "E722", "R712",0x5E)
    object V5F : VfpuReg("S732", "C732", "C731", "C732", "M732", "E723", "R713",0x5F)
    object V60 : VfpuReg("S003", "R020", "R010", "R020", "E020", "M002", "C001",0x60)
    object V61 : VfpuReg("S013", "R021", "R011", "R021", "E021", "M012", "C011",0x61)
    object V62 : VfpuReg("S023", "R022", "R012", "R022", "E022", "M022", "C021",0x62)
    object V63 : VfpuReg("S033", "R023", "R013", "R023", "E023", "M032", "C031",0x63)
    object V64 : VfpuReg("S103", "R120", "R110", "R120", "E120", "M102", "C101",0x64)
    object V65 : VfpuReg("S113", "R121", "R111", "R121", "E121", "M112", "C111",0x65)
    object V66 : VfpuReg("S123", "R122", "R112", "R122", "E122", "M122", "C121",0x66)
    object V67 : VfpuReg("S133", "R123", "R113", "R123", "E123", "M132", "C131",0x67)
    object V68 : VfpuReg("S203", "R220", "R210", "R220", "E220", "M202", "C201",0x68)
    object V69 : VfpuReg("S213", "R221", "R211", "R221", "E221", "M212", "C211",0x69)
    object V6A : VfpuReg("S223", "R222", "R212", "R222", "E222", "M222", "C221",0x6A)
    object V6B : VfpuReg("S233", "R223", "R213", "R223", "E223", "M232", "C231",0x6B)
    object V6C : VfpuReg("S303", "R320", "R310", "R320", "E320", "M302", "C301",0x6C)
    object V6D : VfpuReg("S313", "R321", "R311", "R321", "E321", "M312", "C311",0x6D)
    object V6E : VfpuReg("S323", "R322", "R312", "R322", "E322", "M322", "C321",0x6E)
    object V6F : VfpuReg("S333", "R323", "R313", "R323", "E323", "M332", "C331",0x6F)
    object V70 : VfpuReg("S403", "R420", "R410", "R420", "E420", "M402", "C401",0x70)
    object V71 : VfpuReg("S413", "R421", "R411", "R421", "E421", "M412", "C411",0x71)
    object V72 : VfpuReg("S423", "R422", "R412", "R422", "E422", "M422", "C421",0x72)
    object V73 : VfpuReg("S433", "R423", "R413", "R423", "E423", "M432", "C431",0x73)
    object V74 : VfpuReg("S503", "R520", "R510", "R520", "E520", "M502", "C501",0x74)
    object V75 : VfpuReg("S513", "R521", "R511", "R521", "E521", "M512", "C511",0x75)
    object V76 : VfpuReg("S523", "R522", "R512", "R522", "E522", "M522", "C521",0x76)
    object V77 : VfpuReg("S533", "R523", "R513", "R523", "E523", "M532", "C531",0x77)
    object V78 : VfpuReg("S603", "R620", "R610", "R620", "E620", "M602", "C601",0x78)
    object V79 : VfpuReg("S613", "R621", "R611", "R621", "E621", "M612", "C611",0x79)
    object V7A : VfpuReg("S623", "R622", "R612", "R622", "E622", "M622", "C621",0x7A)
    object V7B : VfpuReg("S633", "R623", "R613", "R623", "E623", "M632", "C631",0x7B)
    object V7C : VfpuReg("S703", "R720", "R710", "R720", "E720", "M702", "C701",0x7C)
    object V7D : VfpuReg("S713", "R721", "R711", "R721", "E721", "M712", "C711",0x7D)
    object V7E : VfpuReg("S723", "R722", "R712", "R722", "E722", "M722", "C721",0x7E)
    object V7F : VfpuReg("S733", "R723", "R713", "R723", "E723", "M732", "C731",0x7F)

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
