package mist.asm.mips

import kio.util.toSignedHex
import kio.util.toWHex
import mist.asm.*
import mist.asm.mips.MipsOpcode.*
import mist.util.MistLogger
import mist.util.logTag

// TODO rewrite as SHL module or write unit tests for this
// TODO this will probably only work when fp register is not used (so -fomit-frame-pointer was used or PSP binary)
class MipsStackAnalysis(private val graph: MipsGraph, private val logger: MistLogger) {
  private val tag = logTag()

  var frameSize = 0
    private set
  var returnCount = 0
    private set
  var possibleFalsePositives = false
    private set
  private val accessMap = mutableMapOf<Int, StackAccess>()
  private val functionReturns = mutableMapOf<Int, MipsInstr>()
  val framePreserve = mutableMapOf<Int, MipsInstr>()

  fun analyze() {
    countReturnPoints()
    determinateFrameSize()
    createAccessMap()
    markFramePreserveInstructions()
  }

  private fun countReturnPoints() {
    graph.bfs { node ->
      node.instrs.forEach {
        if (it.matches(Jr, isReg(GprReg.Ra))) {
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
        if (instr.matchesExact(Addiu, isReg(GprReg.Sp), isReg(GprReg.Sp), anyImm())) {
          val imm = instr.op2AsImm()
          if (imm < 0) {
            frameSize = Math.abs(imm)
            framePreserve[instr.addr] = instr
          } else {
            if (frameSize != imm) {
              logger.panic(
                tag, "stack frame size mismatch, expected ${frameSize.toSignedHex()}, " +
                  "got ${imm.toSignedHex()}"
              )
            }
            framePreserve[instr.addr] = instr
          }
        } else if (instr.getModifiedRegisters().contains(GprReg.Sp)) {
          logger.panic(tag, "unusual sp operation on instruction: $instr")
        }
      }
      BfsVisitorAction.Continue
    }
  }

  private fun createAccessMap() {
    graph.bfs { node ->
      val instrs = node.instrs
      instrs.forEach { instr ->
        if (instr.matches(op1 = isReg(GprReg.Sp), op2 = anyImm())) {
          val imm = instr.op2AsImm()
          if (instr.hasFlag(MemoryRead)) {
            accessMap.getOrPut(imm, defaultValue = { StackAccess() }).apply {
              readCount++
              relatedInstrs.add(instr)
            }
          }
          if (instr.hasFlag(MemoryWrite)) {
            accessMap.getOrPut(imm, defaultValue = { StackAccess() }).apply {
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
    val frameRegs = arrayOf(
      GprReg.S0, GprReg.S1, GprReg.S2, GprReg.S3, GprReg.S4, GprReg.S5,
      GprReg.S6, GprReg.S7, GprReg.Gp, GprReg.Sp, GprReg.Fp, GprReg.Ra
    )
    val alreadyPreserved: MutableMap<Reg, Boolean> = frameRegs.associateBy({ it }, { false }).toMutableMap()

    accessMap.forEach { addr, access ->
      if (access.writeCount != 1 || access.readCount != returnCount) return@forEach
      if (access.relatedInstrs.all { it.operands.getOrNull(0) is RegOperand } == false) return@forEach
      // check if all related instructions work on frame register and
      // check if all instructions work on the same register
      val accessRegisterSet = access.relatedInstrs.map { it.op0AsReg() }.distinctBy { it }
      if (access.relatedInstrs.all { instr -> instr.matches(op0 = isReg(*frameRegs)) } == false
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
              || instr.getModifiedRegisters().contains(relInstr.op0AsReg())
            ) {
              valid = true
              return@bfsFromInstr BfsVisitorAction.Stop
            }

            // case when jal occurs, the current function must have preserved ra
            if (accessRegister == GprReg.Ra && instr.matches(Jal)) {
              valid = true
              return@bfsFromInstr BfsVisitorAction.Stop
            }

            // when currently checked register was used as source register for some other instruction it's unlikely
            // to be frame preserve
            if (instr.getUsedRegisters().contains(relInstr.op0AsReg())) {
              cause = "register was used as source register by other instruction"
              return@bfsFromInstr BfsVisitorAction.Stop
            }
          }
          return@bfsFromInstr BfsVisitorAction.Continue
        }

        if (valid == false) {
          framePreserveSetValid = false
          logger.warn(tag, "ignored possible false positive frame preserve $relInstr, cause: $cause")
        }
      }

      if (framePreserveSetValid) {
        access.relatedInstrs.forEach {
          framePreserve[it.addr] = it
        }
        alreadyPreserved[accessRegister] = true
      } else {
        logger.warn(tag, "determined access to ${addr.toWHex()} is possible false positive")
        possibleFalsePositives = true
      }
    }
  }
}

class StackAccess(
  var readCount: Int = 0, var writeCount: Int = 0,
  val relatedInstrs: MutableList<MipsInstr> = mutableListOf()
)
