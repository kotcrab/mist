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

package mist.asm

import mist.asm.Reg.*

/** @author Kotcrab */

class SwitchAtRegIdiom : IdiomMatcher<SwitchDescriptor>(maxOffset = 12) {
    private var reg1 = zero
    private var reg2 = zero
    private var switchCaseCount = -1
    private var jumpTableLoc = 0

    override fun reset() {
        reg1 = zero
        reg2 = zero
        switchCaseCount = -1
        jumpTableLoc = 0
    }

    override fun getPhases(): Array<(Instr) -> Boolean> = arrayOf(
            { instr -> instr.matches(Opcode.Jr, isReg(at)) },
            { instr -> instr.matches(Opcode.Lw, isReg(at), isReg(at), anyImm(), matchedCallback = { jumpTableLoc += op3AsImm() }) },
            { instr -> instr.matches(Opcode.Addu, isReg(at), isReg(at), anyReg(), matchedCallback = { reg1 = op3AsReg() }) },
            { instr -> instr.matches(Opcode.Lui, isReg(at), anyImm(), matchedCallback = { jumpTableLoc += op2AsImm() shl 16 }) },
            { instr -> instr.matches(Opcode.Sll, isReg(reg1), isReg(reg1), isImm(0x2)) },
            { instr ->
                (instr.opcode == Opcode.Beq || instr.opcode == Opcode.Beql) &&
                        instr.matches(null, anyReg(), isReg(zero), matchedCallback = { reg2 = op1AsReg() })
            },
            { instr ->
                (instr.opcode == Opcode.Sltiu || instr.opcode == Opcode.Slti) &&
                        instr.matches(null, isReg(reg2), isReg(reg1), anyImm(), matchedCallback = { switchCaseCount = op3AsImm() })
            })

    override fun matchedResult(relInstrs: List<Instr>): SwitchDescriptor {
        return SwitchDescriptor(relInstrs, switchCaseCount, jumpTableLoc)
    }
}

class SwitchA0RegIdiom : IdiomMatcher<SwitchDescriptor>(maxOffset = 12) {
    private var reg1 = zero
    private var switchCaseCount = -1
    private var jumpTableLoc = 0

    override fun reset() {
        reg1 = zero
        switchCaseCount = -1
        jumpTableLoc = 0
    }

    override fun getPhases(): Array<(Instr) -> Boolean> = arrayOf(
            { instr -> instr.matches(Opcode.Jr, isReg(a0)) },
            { instr -> instr.matches(Opcode.Lw, isReg(a0), isReg(v0), isImm(0x0)) },
            { instr -> instr.matches(Opcode.Addu, isReg(v0), isReg(v0), isReg(v1)) },
            { instr -> instr.matches(Opcode.Addiu, isReg(v1), isReg(v1), anyImm(), matchedCallback = { jumpTableLoc += op3AsImm() }) },
            { instr -> instr.matches(Opcode.Sll, isReg(v0), anyReg(), isImm(0x2), matchedCallback = { reg1 = op2AsReg() }) },
            { instr -> instr.matches(Opcode.Lui, isReg(v1), anyImm(), matchedCallback = { jumpTableLoc += op2AsImm() shl 16 }) },
            { instr -> instr.matches(Opcode.Beq, isReg(v0), isReg(zero)) },
            { instr -> instr.matches(Opcode.Sltiu, isReg(v0), isReg(reg1), anyImm(), matchedCallback = { switchCaseCount = op3AsImm() }) })

    override fun matchedResult(relInstrs: List<Instr>): SwitchDescriptor {
        return SwitchDescriptor(relInstrs, switchCaseCount, jumpTableLoc)
    }
}

class SwitchDescriptor(val relInstrs: List<Instr>, val switchCaseCount: Int, val jumpTableLoc: Int)

class SwitchCaseDescriptor(val instr: Instr, val cases: MutableList<Int> = mutableListOf())
