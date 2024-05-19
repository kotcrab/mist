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

import kio.util.toWHex
import mist.asm.Disassembler
import mist.asm.DisassemblerException
import mist.asm.Disassembly
import mist.asm.FunctionDef
import mist.asm.mips.MipsOpcode.Nop
import mist.io.BinLoader

/** @author Kotcrab */

abstract class MipsDisassembler(private val srcProcessor: MipsProcessor, protected val strict: Boolean = true) :
    Disassembler<MipsInstr> {
    final override fun disassemble(loader: BinLoader, funcDef: FunctionDef): Disassembly<MipsInstr> {
        if (funcDef.offset % 4 != 0) throw DisassemblerException("offset must be a multiply of 4")
        if (funcDef.len % 4 != 0) throw DisassemblerException("length must be a multiply of 4")
        val decoded = mutableListOf<MipsInstr>()
        repeat(funcDef.len / 4) { instrCount ->
            val vAddr = funcDef.offset + instrCount * 4
            decoded.add(disassembleInstruction(loader, vAddr, instrCount))
        }
        val illegalOpcodes = decoded.filterNot { it.hasProcessor(srcProcessor) }
        if (illegalOpcodes.isNotEmpty()) {
            val illegalMnemonics = illegalOpcodes.joinToString(transform = { it.opcode.mnemonic })
            throw DisassemblerException("generated disassembly uses opcodes not supported by specified processor: $illegalMnemonics")
        }
        return Disassembly(funcDef, decoded)
    }

    final override fun disassembleInstruction(loader: BinLoader, at: Int): MipsInstr {
        return disassembleInstruction(loader, at, 1)
    }

    private fun disassembleInstruction(loader: BinLoader, vAddr: Int, instrCount: Int): MipsInstr {
        val instr = loader.readInt(vAddr)
        val opcode = instr ushr 26
        return when {
            instr == 0 -> MipsInstr(vAddr, Nop)
            opcode == MipsDefines.SPECIAL -> disasmSpecialInstr(vAddr, instr, instrCount)
            opcode == MipsDefines.SPECIAL2 -> disasmSpecial2Instr(vAddr, instr, instrCount)
            opcode == MipsDefines.SPECIAL3 -> disasmSpecial3Instr(vAddr, instr, instrCount)
            opcode == MipsDefines.REGIMM -> disasmRegimmInstr(vAddr, instr, instrCount)
            opcode == MipsDefines.COP0 -> disasmCop0Instr(vAddr, instr, instrCount)
            opcode == MipsDefines.COP1 -> disasmCop1Instr(vAddr, instr, instrCount)
            opcode == MipsDefines.COP2 -> disasmCop2Instr(vAddr, instr, instrCount)
            opcode == MipsDefines.COP3_COP1X -> disasmCop3Instr(vAddr, instr, instrCount)
            else -> disasmOpcodeInstr(vAddr, instr, instrCount, opcode)
        }
    }

    protected open fun disasmSpecialInstr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr =
        handleUnknownInstr(vAddr, instrCount)

    protected open fun disasmSpecial2Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr =
        handleUnknownInstr(vAddr, instrCount)

    protected open fun disasmSpecial3Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr =
        handleUnknownInstr(vAddr, instrCount)

    protected open fun disasmRegimmInstr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr =
        handleUnknownInstr(vAddr, instrCount)

    protected open fun disasmCop0Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr =
        handleUnknownInstr(vAddr, instrCount)

    protected open fun disasmCop1Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr =
        handleUnknownInstr(vAddr, instrCount)

    protected open fun disasmCop2Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr =
        handleUnknownInstr(vAddr, instrCount)

    protected open fun disasmCop3Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr =
        handleUnknownInstr(vAddr, instrCount)

    protected open fun disasmOpcodeInstr(vAddr: Int, instr: Int, instrCount: Int, opcode: Int): MipsInstr =
        handleUnknownInstr(vAddr, instrCount)

    protected fun handleUnknownInstr(vAddr: Int, instrCount: Int): Nothing {
        throw DisassemblerException("unknown instruction at offset ${(instrCount * 4).toWHex()}, address ${vAddr.toWHex()}")
    }

    protected inner class StrictChecker {
        private val tests = mutableMapOf<StrictCheck, () -> Boolean>()

        fun register(check: StrictCheck, test: () -> Boolean) {
            if (tests.contains(check)) throw DisassemblerException("strict check for this type was already registered")
            tests[check] = test
        }

        operator fun invoke(vararg checks: StrictCheck): Boolean {
            if (!strict) return true
            return checks.all { check ->
                val test = tests[check] ?: throw DisassemblerException("strict check not supported in this context")
                return@all test()
            }
        }

        operator fun invoke(test: () -> Boolean): Boolean {
            if (!strict) return true
            return test()
        }
    }

    protected enum class StrictCheck {
        ZeroImm, ZeroShift, ZeroRs, ZeroRt, ZeroRd, ZeroFs, ZeroFt, ZeroFd, ZeroFunct
    }
}
