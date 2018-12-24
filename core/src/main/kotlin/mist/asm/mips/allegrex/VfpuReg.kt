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

sealed class VfpuReg(name: String, id: Int) : Reg(name, id) {
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
                Pfxs, Pfxt, Pfxd, Cc, Inf4, Rev, Rcx0, Rcx1, Rcx2, Rcx3, Rcx4, Rcx5, Rcx6, Rcx7
            )
        }

        fun forId(id: Int): VfpuReg {
            return Reg.forId(values(), id)
        }

        fun values(): Array<VfpuReg> {
            return vfpuRegs
        }
    }

    object V00 : VfpuReg("v00", 0x00)
    object V01 : VfpuReg("v01", 0x01)
    object V02 : VfpuReg("v02", 0x02)
    object V03 : VfpuReg("v03", 0x03)
    object V04 : VfpuReg("v04", 0x04)
    object V05 : VfpuReg("v05", 0x05)
    object V06 : VfpuReg("v06", 0x06)
    object V07 : VfpuReg("v07", 0x07)
    object V08 : VfpuReg("v08", 0x08)
    object V09 : VfpuReg("v09", 0x09)
    object V0A : VfpuReg("v0a", 0x0A)
    object V0B : VfpuReg("v0b", 0x0B)
    object V0C : VfpuReg("v0c", 0x0C)
    object V0D : VfpuReg("v0d", 0x0D)
    object V0E : VfpuReg("v0e", 0x0E)
    object V0F : VfpuReg("v0f", 0x0F)
    object V10 : VfpuReg("v10", 0x10)
    object V11 : VfpuReg("v11", 0x11)
    object V12 : VfpuReg("v12", 0x12)
    object V13 : VfpuReg("v13", 0x13)
    object V14 : VfpuReg("v14", 0x14)
    object V15 : VfpuReg("v15", 0x15)
    object V16 : VfpuReg("v16", 0x16)
    object V17 : VfpuReg("v17", 0x17)
    object V18 : VfpuReg("v18", 0x18)
    object V19 : VfpuReg("v19", 0x19)
    object V1A : VfpuReg("v1a", 0x1A)
    object V1B : VfpuReg("v1b", 0x1B)
    object V1C : VfpuReg("v1c", 0x1C)
    object V1D : VfpuReg("v1d", 0x1D)
    object V1E : VfpuReg("v1e", 0x1E)
    object V1F : VfpuReg("v1f", 0x1F)
    object V20 : VfpuReg("v20", 0x20)
    object V21 : VfpuReg("v21", 0x21)
    object V22 : VfpuReg("v22", 0x22)
    object V23 : VfpuReg("v23", 0x23)
    object V24 : VfpuReg("v24", 0x24)
    object V25 : VfpuReg("v25", 0x25)
    object V26 : VfpuReg("v26", 0x26)
    object V27 : VfpuReg("v27", 0x27)
    object V28 : VfpuReg("v28", 0x28)
    object V29 : VfpuReg("v29", 0x29)
    object V2A : VfpuReg("v2a", 0x2A)
    object V2B : VfpuReg("v2b", 0x2B)
    object V2C : VfpuReg("v2c", 0x2C)
    object V2D : VfpuReg("v2d", 0x2D)
    object V2E : VfpuReg("v2e", 0x2E)
    object V2F : VfpuReg("v2f", 0x2F)
    object V30 : VfpuReg("v30", 0x30)
    object V31 : VfpuReg("v31", 0x31)
    object V32 : VfpuReg("v32", 0x32)
    object V33 : VfpuReg("v33", 0x33)
    object V34 : VfpuReg("v34", 0x34)
    object V35 : VfpuReg("v35", 0x35)
    object V36 : VfpuReg("v36", 0x36)
    object V37 : VfpuReg("v37", 0x37)
    object V38 : VfpuReg("v38", 0x38)
    object V39 : VfpuReg("v39", 0x39)
    object V3A : VfpuReg("v3a", 0x3A)
    object V3B : VfpuReg("v3b", 0x3B)
    object V3C : VfpuReg("v3c", 0x3C)
    object V3D : VfpuReg("v3d", 0x3D)
    object V3E : VfpuReg("v3e", 0x3E)
    object V3F : VfpuReg("v3f", 0x3F)
    object V40 : VfpuReg("v40", 0x40)
    object V41 : VfpuReg("v41", 0x41)
    object V42 : VfpuReg("v42", 0x42)
    object V43 : VfpuReg("v43", 0x43)
    object V44 : VfpuReg("v44", 0x44)
    object V45 : VfpuReg("v45", 0x45)
    object V46 : VfpuReg("v46", 0x46)
    object V47 : VfpuReg("v47", 0x47)
    object V48 : VfpuReg("v48", 0x48)
    object V49 : VfpuReg("v49", 0x49)
    object V4A : VfpuReg("v4a", 0x4A)
    object V4B : VfpuReg("v4b", 0x4B)
    object V4C : VfpuReg("v4c", 0x4C)
    object V4D : VfpuReg("v4d", 0x4D)
    object V4E : VfpuReg("v4e", 0x4E)
    object V4F : VfpuReg("v4f", 0x4F)
    object V50 : VfpuReg("v50", 0x50)
    object V51 : VfpuReg("v51", 0x51)
    object V52 : VfpuReg("v52", 0x52)
    object V53 : VfpuReg("v53", 0x53)
    object V54 : VfpuReg("v54", 0x54)
    object V55 : VfpuReg("v55", 0x55)
    object V56 : VfpuReg("v56", 0x56)
    object V57 : VfpuReg("v57", 0x57)
    object V58 : VfpuReg("v58", 0x58)
    object V59 : VfpuReg("v59", 0x59)
    object V5A : VfpuReg("v5a", 0x5A)
    object V5B : VfpuReg("v5b", 0x5B)
    object V5C : VfpuReg("v5c", 0x5C)
    object V5D : VfpuReg("v5d", 0x5D)
    object V5E : VfpuReg("v5e", 0x5E)
    object V5F : VfpuReg("v5f", 0x5F)
    object V60 : VfpuReg("v60", 0x60)
    object V61 : VfpuReg("v61", 0x61)
    object V62 : VfpuReg("v62", 0x62)
    object V63 : VfpuReg("v63", 0x63)
    object V64 : VfpuReg("v64", 0x64)
    object V65 : VfpuReg("v65", 0x65)
    object V66 : VfpuReg("v66", 0x66)
    object V67 : VfpuReg("v67", 0x67)
    object V68 : VfpuReg("v68", 0x68)
    object V69 : VfpuReg("v69", 0x69)
    object V6A : VfpuReg("v6a", 0x6A)
    object V6B : VfpuReg("v6b", 0x6B)
    object V6C : VfpuReg("v6c", 0x6C)
    object V6D : VfpuReg("v6d", 0x6D)
    object V6E : VfpuReg("v6e", 0x6E)
    object V6F : VfpuReg("v6f", 0x6F)
    object V70 : VfpuReg("v70", 0x70)
    object V71 : VfpuReg("v71", 0x71)
    object V72 : VfpuReg("v72", 0x72)
    object V73 : VfpuReg("v73", 0x73)
    object V74 : VfpuReg("v74", 0x74)
    object V75 : VfpuReg("v75", 0x75)
    object V76 : VfpuReg("v76", 0x76)
    object V77 : VfpuReg("v77", 0x77)
    object V78 : VfpuReg("v78", 0x78)
    object V79 : VfpuReg("v79", 0x79)
    object V7A : VfpuReg("v7a", 0x7A)
    object V7B : VfpuReg("v7b", 0x7B)
    object V7C : VfpuReg("v7c", 0x7C)
    object V7D : VfpuReg("v7d", 0x7D)
    object V7E : VfpuReg("v7e", 0x7E)
    object V7F : VfpuReg("v7f", 0x7F)

    // special purpose - directly inaccessible registers

    object Pfxs : VfpuReg("pfxs", -1)
    object Pfxt : VfpuReg("pfxt", -1)
    object Pfxd : VfpuReg("pfxd", -1)
    object Cc : VfpuReg("cc", -1)
    object Inf4 : VfpuReg("inf4", -1)
    object Rev : VfpuReg("rev", -1)
    object Rcx0 : VfpuReg("rcx0", -1)
    object Rcx1 : VfpuReg("rcx1", -1)
    object Rcx2 : VfpuReg("rcx2", -1)
    object Rcx3 : VfpuReg("rcx3", -1)
    object Rcx4 : VfpuReg("rcx4", -1)
    object Rcx5 : VfpuReg("rcx5", -1)
    object Rcx6 : VfpuReg("rcx6", -1)
    object Rcx7 : VfpuReg("rcx7", -1)
}
