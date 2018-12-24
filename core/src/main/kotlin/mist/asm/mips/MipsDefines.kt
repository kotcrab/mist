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

/** @author Kotcrab */

object MipsDefines {
    const val SPECIAL = 0b000_000
    const val SPECIAL2 = 0b011_100
    const val SPECIAL3 = 0b011_111
    const val REGIMM = 1
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
