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

package mist.asm.mips

import mist.asm.*
import mist.asm.mips.GprReg.*

/** @author Kotcrab */

class MipsSwitchIdioms {
    val atRegIdiom = IdiomMatcher<MipsInstr, SwitchAtRegIdiomState, SwitchDescriptor>(
        maxOffset = 12,
        stateProvider = { SwitchAtRegIdiomState() },
        resultTransform = { relInstrs, state ->
            SwitchDescriptor(
                relInstrs,
                state.switchCaseCount,
                state.jumpTableLoc
            )
        },
        phases = arrayOf(
            { instr, _ ->
                instr.matches(Jr, isReg(At))
            },
            { instr, state ->
                instr.matches(Lw, isReg(At), isReg(At), anyImm()) { state.jumpTableLoc += op2AsImm() }
            },
            { instr, state ->
                instr.matches(Addu, isReg(At), isReg(At), anyReg()) { state.reg1 = op2AsReg() as GprReg }
            },
            { instr, state ->
                instr.matches(Lui, isReg(At), anyImm()) { state.jumpTableLoc += op1AsImm() shl 16 }
            },
            { instr, state ->
                instr.matches(Sll, isReg(state.reg1), isReg(state.reg1), isImm(0x2))
            },
            { instr, state ->
                (instr.opcode == Beq || instr.opcode == Beql) &&
                        instr.matches(null, anyReg(), isReg(Zero)) { state.reg2 = op0AsReg() as GprReg }
            },
            { instr, state ->
                (instr.opcode == Sltiu || instr.opcode == Slti) &&
                        instr.matches(null, isReg(state.reg2), isReg(state.reg1), anyImm()) {
                            state.switchCaseCount = op2AsImm()
                        }
            }
        )
    )

    val a0RegIdiom = IdiomMatcher<MipsInstr, SwitchA0RegIdiomState, SwitchDescriptor>(
        maxOffset = 12,
        stateProvider = { SwitchA0RegIdiomState() },
        resultTransform = { relInstrs, state ->
            SwitchDescriptor(
                relInstrs,
                state.switchCaseCount,
                state.jumpTableLoc
            )
        },
        phases = arrayOf(
            { instr, _ ->
                instr.matches(Jr, isReg(A0))
            },
            { instr, _ ->
                instr.matches(Lw, isReg(A0), isReg(V0), isImm(0x0))
            },
            { instr, _ ->
                instr.matches(Addu, isReg(V0), isReg(V0), isReg(V1))
            },
            { instr, state ->
                instr.matches(Addiu, isReg(V1), isReg(V1), anyImm()) { state.jumpTableLoc += op2AsImm() }
            },
            { instr, state ->
                instr.matches(Sll, isReg(V0), anyReg(), isImm(0x2)) { state.reg1 = op1AsReg() as GprReg }
            },
            { instr, state ->
                instr.matches(Lui, isReg(V1), anyImm()) { state.jumpTableLoc += op1AsImm() shl 16 }
            },
            { instr, _ ->
                instr.matches(Beq, isReg(V0), isReg(Zero))
            },
            { instr, state ->
                instr.matches(Sltiu, isReg(V0), isReg(state.reg1), anyImm()) { state.switchCaseCount = op2AsImm() }
            }
        )
    )

    class SwitchAtRegIdiomState {
        var reg1: GprReg = Zero
        var reg2: GprReg = Zero
        var switchCaseCount = -1
        var jumpTableLoc = 0
    }

    class SwitchA0RegIdiomState {
        var reg1: GprReg = Zero
        var switchCaseCount = -1
        var jumpTableLoc = 0
    }

    private fun MipsInstr.matches(
        opcode: Opcode? = null, op0: OperandMatcher = anyOp(), op1: OperandMatcher = anyOp(),
        op2: OperandMatcher = anyOp(), matchedCallback: MipsInstr.() -> Unit
    ): Boolean {
        val matches = matches(opcode, op0, op1, op2)
        if (matches) matchedCallback()
        return matches
    }
}

class SwitchDescriptor(val relInstrs: List<MipsInstr>, val switchCaseCount: Int, val jumpTableLoc: Int)

class SwitchCaseDescriptor(val instr: Instr, val cases: MutableList<Int> = mutableListOf())
