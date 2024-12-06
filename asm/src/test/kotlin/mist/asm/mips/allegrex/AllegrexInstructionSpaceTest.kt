package mist.asm.mips.allegrex

import kio.util.execute
import kio.util.nullStreamHandler
import kio.util.toHex
import kotlinx.coroutines.*
import mist.asm.*
import mist.asm.mips.GprReg
import mist.asm.mips.MipsInstr
import mist.asm.mips.MipsOpcode
import mist.io.BinLoader
import java.io.File
import java.nio.charset.Charset
import java.util.*

fun main(args: Array<String>) {
  if (args.size != 3) println("Usage: [7zipExe] [compressedInstrSpace] [tmpDir]")
  AllegrexInstructionSpaceTest(File(args[0]), File(args[1]), File(args[2]))
}

/**
 * This test tries to verify correctness of mist disassembler output against some other known disassembler. It is done by
 * comparing output of every possible instruction from 0x0 to 0xFFFFFFFF. This requires saved disassembly output stored
 * on disk, because it's very large (>120 GB) it can't be provided. This test is expected only to be used in development
 * to discover missing and incorrect instructions and then add standard unit tests based on the results.
 */
@OptIn(DelicateCoroutinesApi::class)
private class AllegrexInstructionSpaceTest(val sevenZipExe: File, val instrDataArchive: File, val tmpDir: File) {
  private val disasm = AllegrexDisassembler(strict = false)
  private val def = FunctionDef("Test", 0x8804000, 4)
  private val invalidInstr = "__invalid"
  private val singleThreaded = true
  private val ignoreRepeatedOpcodes = true
  private val ignoredOpcodes = Collections.synchronizedSet(mutableSetOf<String>())

  init {
    val threads = if (singleThreaded) 1 else Runtime.getRuntime().availableProcessors() - 1
    val ctx = newFixedThreadPoolContext(threads, "TestPool")

    val startFrom = 178
    val jobs = IntRange(startFrom, 255)
      .filter { it != 104 } // 0x68 is emuhack space, ignoring it
      .map { "$it.txt" }
      .map { GlobalScope.launch(ctx) { processFile(it) } }

    runBlocking {
      jobs.forEach { it.join() }
    }
  }

  private fun processFile(fileName: String) {
    println("Processing $fileName...")
    val file = File(tmpDir, fileName)
    file.delete()
    execute(
      sevenZipExe,
      args = listOf("e", instrDataArchive, "-o${tmpDir.absolutePath}", fileName),
      streamHandler = nullStreamHandler()
    )
    file.forEachLine { asm ->
      val parts = asm
        .trimEnd()
        .replace("no instruction :(", invalidInstr)
        .replace("?? ---unknown---", invalidInstr)
        .replace("* jitblock: (invalid emuhack)", invalidInstr)
        .replace("* (invalid): (invalid emuhack)", invalidInstr)
        .replace("* replacement: (invalid emuhack)", invalidInstr)
        .replace("vcrs ERROR", invalidInstr)
        .replace("BADVTFM", invalidInstr)
        .replace("syscall \t", "syscall ")
        .split("; ", limit = 2)

      val encodedInstr = parts[0].toLong(16).toInt()
      val expectedInstr = parts[1]
      val instrParts = expectedInstr.split(" ", limit = 2)
      val expectedOpcode = instrParts[0]
      val operands = when {
        instrParts.size == 1 -> emptyList()
        expectedOpcode in arrayOf("vpfxs", "vpfxt", "vpfxd") -> listOf(instrParts[1].replace(",", ", "))
        expectedOpcode.startsWith("vrot") -> {
          val p = instrParts[1].split(",", limit = 3).toMutableList()
          p[2] = p[2].replace(",", ", ")
          p
        }
        else -> instrParts[1].split(",")
      }
      val parsedOperands = operands
        .map { op ->
          if (op == "(interlock)") "S733" else op
        }
        .flatMap { op ->
          val memAccess = op.indexOf("(")
          if (memAccess == -1 || expectedOpcode.startsWith("vcst.")) {
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
        expectedOpcode,
        expectedOperands = parsedOperands
      )
    }
    println("Completed $fileName...")
    file.delete()
  }

  private fun processAsmInstruction(encodedInstr: Int, expectedOpcode: String, expectedOperands: List<String>) {
    // no idea about those, not present in any available docs, ignored
    // mfmc0 is actually implemented as EI / DI
    if (expectedOpcode in arrayOf("iack", "dis.int", "mfmc0")) return
    // VFPU related but unknown encoding and implementation
    if (expectedOpcode in arrayOf("mfc2", "cfc2", "mtc2", "ctc2", "???.s", "???.p")) return
    if (expectedOpcode in arrayOf("vf2h.s", "vf2h.t", "vh2f.t", "vh2f.q", "vi2c.s", "vi2c.t")) return
    if (expectedOpcode in arrayOf("vi2s.s", "vi2s.t", "vi2us.s", "vi2us.t", "vcmov")) return
    if (expectedOpcode in arrayOf("vt4444.s", "vt4444.t", "vt5551.s", "vt5551.t", "vt5650.s", "vt5650.t")) return
    // don't know why this is using different encoding beyond CC 0..5
    if (expectedOpcode.startsWith("vcmov") && expectedOperands.contains("CC[...]")) return

    val expectedInstrTxt = "$expectedOpcode ${expectedOperands.joinToString()}"
    try {
      val instr = rewriteInstr(disasm.disassemble(TestBinLoader(encodedInstr), def).instrs.first())
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
        if (ignoreRepeatedOpcodes && expectedOpcode in ignoredOpcodes) return
        println("${encodedInstr.toHex()}: invalid opcode $instrInvalidCompare")
        if (ignoreRepeatedOpcodes) ignoredOpcodes.add(expectedOpcode)
        return
      }
      // no need to verify args of those
      if (expectedOpcode in arrayOf("syscall", "break", "sync", "cache")) return
      // seems to be decoded incorrectly by the emulator (code field is not read / argument order flipped)
      if (expectedOpcode in arrayOf("teq", "tge", "tgeu", "tlt", "tltu", "tne")) return
      if (expectedOpcode in arrayOf("teqi", "tgei", "tgeiu", "tlti", "tltiu", "tnei")) return
      // opcodes' operands not decoded by the emulator and mist decoding is not 100% precise
      if (expectedOpcode in arrayOf("mfc0", "mtc0", "mfic", "mtic", "rdhwr", "rdpgpr", "wrpgpr")) return
      // opcodes' operands not decoded by the emulator
      if (expectedOpcode in arrayOf("ll", "sc", "synci", "halt")) return
      if (expectedOpcode in arrayOf("tlbp", "tlbr", "tlbwi", "tlbwr", "eret", "deret", "wait")) return
      if (expectedOpcode in arrayOf("vsbz", "vlgb")) return
      // first operand is probably wrong (https://github.com/hrydgard/ppsspp/blob/7acb051cae389eec51299d7d50c61fe6f8b44b70/Core/MIPS/MIPSDisVFPU.cpp#L348)
      if (expectedOpcode in arrayOf("vcrs.t")) return

      if (expectedOperands.size != instr.operands.size) {
        if (ignoreRepeatedOpcodes && expectedOpcode in ignoredOpcodes) return
        println("${encodedInstr.toHex()}: invalid operand count $instrInvalidCompare")
        if (ignoreRepeatedOpcodes) ignoredOpcodes.add(expectedOpcode)
        return
      }
      instr.operands.forEachIndexed opCompare@{ idx, operand ->
        if (ignoreRepeatedOpcodes && expectedOpcode in ignoredOpcodes) return
        if (operand.toString() == expectedOperands[idx]) return@opCompare
        try {
          val expectedOp = expectedOperands[idx].replace("\t", "")
          if (operand is ImmOperand && operand.value == Integer.decode(expectedOp)) return@opCompare
          if (operand is FloatOperand) {
            if (operand.value.isNaN() && expectedOp == "nan") return@opCompare
            if (operand.value.isInfinite() && expectedOp == "inf") return@opCompare
            if (Math.abs(operand.value - expectedOp.toFloat()) < 1e-3) return@opCompare
          }
        } catch (ignored: NumberFormatException) {
        }
        println("${encodedInstr.toHex()}: invalid operand at index $idx $instrInvalidCompare")
        if (ignoreRepeatedOpcodes) ignoredOpcodes.add(expectedOpcode)
        return
      }
    } catch (e: DisassemblerException) {
      if (ignoreRepeatedOpcodes && expectedOpcode in ignoredOpcodes) return
      if (expectedOpcode == invalidInstr) return
      println("${encodedInstr.toHex()}: disassembler exception, expected '$expectedInstrTxt'")
      if (ignoreRepeatedOpcodes) ignoredOpcodes.add(expectedOpcode)
    }
  }

  private fun rewriteInstr(instr: MipsInstr): MipsInstr {
    // this should give an general idea which asm idioms needs to be supported
    // not shown here: la (load address) idiom, uses 2 instructions
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
