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

import kio.util.toHex
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import mist.asm.*
import mist.asm.mips.GprReg
import mist.asm.mips.MipsInstr
import mist.asm.mips.MipsOpcode
import mist.io.BinLoader
import java.io.File
import java.nio.charset.Charset
import java.util.*

/** @author Kotcrab */

fun main(args: Array<String>) {
    if (args.isEmpty() || File(args[0]).exists() == false) {
        println("Specify directory which contains instruction space data as first argument")
    }
    AllegrexInstructionSpaceTest(File(args[0]))
}

/**
 * This test tries to verify correctness of mist disassembler output against some other known disassembler. It is done by
 * comparing output of every possible instruction from 0x0 to 0xFFFFFFFF. This requires saved disassembly output stored
 * on disk, because it's very large (>120 GB) it can't be provided. This test is expected only to be used in development
 * to discover missing and incorrect instructions and then add standard unit tests based on the results.
 */
private class AllegrexInstructionSpaceTest(instrDataDir: File) {
    private val disasm = AllegrexDisassembler(strict = false)
    private val def = FunctionDef("Test", 0x8804000, 4)
    private val invalidInstr = "__invalid"
    private val skipIgnoredOpcodes = false
    private val ignoredOpcodes = Collections.synchronizedSet(mutableSetOf<String>())

    init {
        val threads = Runtime.getRuntime().availableProcessors()
        val ctx = newFixedThreadPoolContext(threads, "TestPool")
        val jobs = instrDataDir.listFiles()
            .filter { file -> file.extension == "txt" }
            .sortedBy { file -> file.nameWithoutExtension.toInt() }
            .map { file -> GlobalScope.launch(ctx) { processFile(file) } }
        runBlocking {
            jobs.forEach { it.join() }
        }
    }

    private fun processFile(file: File) {
        println("Processing ${file.name}...")
        file.forEachLine { asm ->
            val parts = asm
                .trimEnd()
                .replace("no instruction :(", invalidInstr)
                .replace("syscall \t", "syscall ")
                .split("; ", limit = 2)
            val encodedInstr = parts[0].toLong(16).toInt()
            val expectedInstr = parts[1]
            val instrParts = expectedInstr.split(" ", limit = 2)
            val operands = if (instrParts.size == 1) emptyList() else instrParts[1].split(",")
            val parsedOperands = operands.flatMap { op ->
                val memAccess = op.indexOf("(")
                if (memAccess == -1) {
                    return@flatMap listOf(op)
                } else {
                    // parse memory access (remove parenthesis and flip operand order)
                    return@flatMap op
                        .split("(")
                        .map { it.replace(")", "") }
                        .reversed()
                }
            }
            processAsmInstruction(
                encodedInstr,
                expectedOpcode = instrParts[0],
                expectedOperands = parsedOperands
            )
        }
    }

    private fun processAsmInstruction(encodedInstr: Int, expectedOpcode: String, expectedOperands: List<String>) {
        val expectedInstrTxt = "$expectedOpcode ${expectedOperands.joinToString()}"
        try {
            val instr = rewriteInstr(disasm.disassemble(TestBinLoader(encodedInstr), def).instr.first())
            val instrTxt = instr.toString()
            val instrInvalidCompare = """
    expected $expectedInstrTxt
    got      ${instrTxt.removeRange(0, instrTxt.indexOf(" ") + 1)}"""

            val opcode = if (instr.hasFlag(Fpu)
                && instr.opcode.mnemonic.startsWith("c.")
                && instr.opcode.mnemonic.endsWith(".s")
            ) {
                instr.opcode.mnemonic.removeSuffix(".s")
            } else {
                instr.opcode.mnemonic
            }

            if (opcode != expectedOpcode) {
                if (skipIgnoredOpcodes && expectedOpcode in ignoredOpcodes) return
                println("${encodedInstr.toHex()}: invalid opcode $instrInvalidCompare")
                if (skipIgnoredOpcodes) ignoredOpcodes.add(expectedOpcode)
                return
            }
            // no need to verify args of those
            if (expectedOpcode in arrayOf("syscall", "break", "sync")) return
            // seems to be decoded incorrectly by the emulator (code field is not read, argument order flipped)
            if (expectedOpcode in arrayOf("teq", "tge", "tgeu", "tlt", "tltu", "tne")) return
            if (expectedOpcode in arrayOf("teqi", "tgei", "tgeiu", "tlti", "tltiu", "tnei")) return
            // opcodes' operands not decoded by the emulator
            if (expectedOpcode in arrayOf("ll", "sc", "synci")) return

            if (expectedOperands.size != instr.operands.size) {
                if (skipIgnoredOpcodes && expectedOpcode in ignoredOpcodes) return
                println("${encodedInstr.toHex()}: invalid operand count $instrInvalidCompare")
                if (skipIgnoredOpcodes) ignoredOpcodes.add(expectedOpcode)
                return
            }
            instr.operands.forEachIndexed opCompare@{ idx, operand ->
                if (skipIgnoredOpcodes && expectedOpcode in ignoredOpcodes) return
                if (operand.toString() == expectedOperands[idx]) return@opCompare
                try {
                    if (operand is ImmOperand && operand.value == Integer.decode(expectedOperands[idx])) return@opCompare
                } catch (ignored: NumberFormatException) {
                }
                println("${encodedInstr.toHex()}: invalid operand at index $idx $instrInvalidCompare")
                if (skipIgnoredOpcodes) ignoredOpcodes.add(expectedOpcode)
            }
        } catch (e: DisassemblerException) {
            if (skipIgnoredOpcodes && expectedOpcode in ignoredOpcodes) return
            if (expectedOpcode == invalidInstr) return
            println("${encodedInstr.toHex()}: disassembler exception, expected '$expectedInstrTxt'")
            if (skipIgnoredOpcodes) ignoredOpcodes.add(expectedOpcode)
        }
    }

    private fun rewriteInstr(instr: MipsInstr): MipsInstr {
        // this should give an general idea which asm idioms needs to be supported
        if (instr.matches(MipsOpcode.Addu, anyReg(), isReg(GprReg.Zero), isReg(GprReg.Zero))) {
            return MipsInstr(instr.addr, IdiomLi, instr.operands[0], ImmOperand(0))
        }
        if (instr.matches(MipsOpcode.Or, anyReg(), isReg(GprReg.Zero), isReg(GprReg.Zero))) {
            return MipsInstr(instr.addr, IdiomLi, instr.operands[0], ImmOperand(0))
        }
        if (instr.matches(MipsOpcode.Jalr, isReg(GprReg.Ra), anyReg())) {
            return MipsInstr(instr.addr, MipsOpcode.Jalr, instr.operands[1])
        }
        if (instr.matches(MipsOpcode.Addu, anyReg(), isReg(GprReg.Zero), anyReg())) {
            return MipsInstr(instr.addr, IdiomMove, instr.operands[0], instr.operands[2])
        }
        if (instr.matches(MipsOpcode.Or, anyReg(), isReg(GprReg.Zero), anyReg())) {
            return MipsInstr(instr.addr, IdiomMove, instr.operands[0], instr.operands[2])
        }
        if (instr.matches(MipsOpcode.Addu, anyReg(), anyReg(), isReg(GprReg.Zero))) {
            return MipsInstr(instr.addr, IdiomMove, instr.operands[0], instr.operands[1])
        }
        if (instr.matches(MipsOpcode.Or, anyReg(), anyReg(), isReg(GprReg.Zero))) {
            return MipsInstr(instr.addr, IdiomMove, instr.operands[0], instr.operands[1])
        }
        if (instr.matches(MipsOpcode.Beq, anyReg(), anyReg(), anyImm()) && instr.op0AsReg() == instr.op1AsReg()) {
            return MipsInstr(instr.addr, IdiomB, instr.operands[2])
        }
        if (instr.matches(MipsOpcode.Addi, anyReg(), isReg(GprReg.Zero), anyImm())) {
            return MipsInstr(instr.addr, IdiomLi, instr.operands[0], instr.operands[2])
        }
        if (instr.matches(MipsOpcode.Addiu, anyReg(), isReg(GprReg.Zero), anyImm())) {
            return MipsInstr(instr.addr, IdiomLi, instr.operands[0], instr.operands[2])
        }
        if (instr.matches(MipsOpcode.Ori, anyReg(), isReg(GprReg.Zero), anyImm())) {
            return MipsInstr(instr.addr, IdiomLi, instr.operands[0], instr.operands[2])
        }
        if (instr.matches(MipsOpcode.Beql, anyReg(), anyReg(), anyImm()) && instr.op0AsReg() == instr.op1AsReg()) {
            return MipsInstr(instr.addr, IdiomBl, instr.operands[2])
        }
        return instr
    }

    private object IdiomLi : MipsOpcode("li")
    private object IdiomMove : MipsOpcode("move")
    private object IdiomB : MipsOpcode("b")
    private object IdiomBl : MipsOpcode("bl")

    private class TestBinLoader(val instr: Int) : BinLoader {
        override fun readInt(at: Int): Int {
            return instr
        }

        override fun readString(at: Int, charset: Charset): String {
            error("not supported")
        }
    }
}
