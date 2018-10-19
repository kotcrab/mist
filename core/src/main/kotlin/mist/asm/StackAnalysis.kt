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

import kio.util.toSignedHex
import kio.util.toWHex
import mist.asm.Reg.*
import mist.util.DecompLog
import mist.util.logTag

/** @author Kotcrab */

class StackAnalysis(private val graph: Graph, private val log: DecompLog) {
    private val tag = logTag()

    var frameSize = 0
        private set
    var returnCount = 0
        private set
    var possibleFalsePositives = false
        private set
    private val accessMap = mutableMapOf<Int, StackAccess>()
    private val functionReturns = mutableMapOf<Int, Instr>()
    val framePreserve = mutableMapOf<Int, Instr>()

    fun analyze() {
        countReturnPoints()
        determinateFrameSize()
        createAccessMap()
        markFramePreserveInstructions()
    }

    private fun countReturnPoints() {
        graph.bfs { node ->
            node.instrs.forEach {
                if (it.matches(Opcode.Jr, isReg(ra))) {
                    returnCount++
                    functionReturns[it.addr] = it
                }
            }
            BfsVisitorAction.Continue
        }
    }

    private fun determinateFrameSize() {
        graph.bfs { node ->
            val instrs = node.instrs
            instrs.forEach { instr ->
                if (instr.matchesExact(Opcode.Addiu, isReg(sp), isReg(sp), isImm())) {
                    val imm = instr.op3AsImm()
                    if (imm < 0) {
                        frameSize = Math.abs(imm)
                        framePreserve[instr.addr] = instr
                    } else {
                        if (frameSize != imm) {
                            log.panic(tag, "stack frame size mismatch, expected ${frameSize.toSignedHex()}, " +
                                    "got ${imm.toSignedHex()}")
                        }
                        framePreserve[instr.addr] = instr
                    }
                } else if (instr.getModifiedRegisters().contains(sp)) {
                    log.panic(tag, "unusual sp operation on instruction: $instr")
                }
            }
            BfsVisitorAction.Continue
        }
    }

    private fun createAccessMap() {
        graph.bfs { node ->
            val instrs = node.instrs
            instrs.forEach { instr ->
                if (instr.matches(op2 = isReg(sp), op3 = isImm())) {
                    val imm = instr.op3AsImm()
                    if (instr.isMemoryRead()) {
                        accessMap.getOrPut(imm, { StackAccess() }).apply {
                            readCount++
                            relatedInstrs.add(instr)
                        }
                    }
                    if (instr.isMemoryWrite()) {
                        accessMap.getOrPut(imm, { StackAccess() }).apply {
                            writeCount++
                            relatedInstrs.add(instr)
                        }
                    }
                }
            }
            BfsVisitorAction.Continue
        }
    }

    private fun markFramePreserveInstructions() {
        val frameRegs = arrayOf(s0, s1, s2, s3, s4, s5, s6, s7, gp, sp, fp, ra)
        val alreadyPreserved = frameRegs.associateBy({ it }, { false }).toMutableMap()

        accessMap.forEach { addr, access ->
            if (access.writeCount != 1 || access.readCount != returnCount) return@forEach
            if (access.relatedInstrs.all { it.op1 is Operand.Reg } == false) return@forEach
            // check if all related instructions work on frame register and
            // check if all instructions work on the same register
            val accessRegisterSet = access.relatedInstrs.map { it.op1AsReg() }.distinctBy { it }
            if (access.relatedInstrs.all { instr -> instr.matches(op1 = anyReg(*frameRegs)) } == false
                    || accessRegisterSet.size != 1) {
                return@forEach
            }
            val accessRegister = accessRegisterSet.first()
            // if register already preserved, ignore it
            if (alreadyPreserved[accessRegister] == true) return@forEach

            var framePreserveSetValid = true

            access.relatedInstrs.forEach { relInstr ->
                var valid = false
                var cause = "unknown"

                graph.bfsFromInstr(relInstr) {
                    for (instr in it) {
                        // returning from function or frame register was overwritten by some other instruction
                        // in that case frame preserve is very likely
                        if (functionReturns.contains(instr.addr)
                                || instr.getModifiedRegisters().contains(relInstr.op1AsReg())) {
                            valid = true
                            return@bfsFromInstr BfsVisitorAction.Stop
                        }

                        // case when jal occurs, the current function must have preserved ra
                        if (accessRegister == ra && instr.matches(Opcode.Jal)) {
                            valid = true
                            return@bfsFromInstr BfsVisitorAction.Stop
                        }

                        // when currently checked register was used as source register for some other instruction it's unlikely
                        // to be frame preserve
                        if (instr.getSourceRegisters().contains(relInstr.op1AsReg())) {
                            cause = "register was used as source register by other instruction"
                            return@bfsFromInstr BfsVisitorAction.Stop
                        }
                    }
                    return@bfsFromInstr BfsVisitorAction.Continue
                }

                if (valid == false) {
                    framePreserveSetValid = false
                    log.warn(tag, "ignored possible false positive frame preserve $relInstr, cause: $cause")
                }
            }

            if (framePreserveSetValid) {
                access.relatedInstrs.forEach {
                    framePreserve[it.addr] = it
                }
                alreadyPreserved[accessRegister] = true
            } else {
                log.warn(tag, "determined access to ${addr.toWHex()} is possible false positive")
                possibleFalsePositives = true
            }
        }
    }
}

class StackAccess(var readCount: Int = 0, var writeCount: Int = 0,
                  val relatedInstrs: MutableList<Instr> = mutableListOf())
