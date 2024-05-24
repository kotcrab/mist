package mist.simulator

import kio.util.toUnsignedLong
import kio.util.toWHex
import mist.asm.*
import mist.asm.mips.*
import mist.asm.mips.allegrex.AllegrexOpcode
import mist.io.BinLoader
import mist.module.Module
import mist.module.ModuleFunction
import kotlin.math.max
import kotlin.math.min

class Interpreter(
    private val binLoader: BinLoader,
    private val disassembler: MipsDisassembler,
    private val module: Module,
    private val ctx: Context,
    maxExecutedInstructions: Int = 10000
) {
    companion object {
        const val RETURN_TOKEN = 0x12244896
        const val INITIAL_SP = 0x09FFFE90
        const val DEBUG_INSTRUCTIONS = false
    }

    private val trace = Trace()
    private var remainingInstructions = maxExecutedInstructions

    fun execute(startAddress: Int): Trace {
        require(trace.isEmpty()) { "This interpreter was already used for execution" }
        ctx.writeGpr(GprReg.Sp, INITIAL_SP)
        ctx.writeGpr(GprReg.Ra, RETURN_TOKEN)
        ctx.pc = startAddress
        trace.elements.add(
            TraceElement.ExecutionStart(
                listOf(
                    ctx.readGpr(GprReg.A0),
                    ctx.readGpr(GprReg.A1),
                    ctx.readGpr(GprReg.A2),
                    ctx.readGpr(GprReg.A3),
                    ctx.readGpr(GprReg.T0),
                    ctx.readGpr(GprReg.T1),
                    ctx.readGpr(GprReg.T2),
                    ctx.readGpr(GprReg.T3),
                )
            )
        )
        runCatching {
            execute()
        }
            .onFailure { throw IllegalStateException("Execution error at ${(ctx.pc - 4).toWHex()}", it) }
            .getOrThrow()
        return trace
    }

    private fun execute() {
        while (true) {
            if (remainingInstructions-- == 0) {
                println("WARN: Interpreter did not terminate within the configured limit, pc=${ctx.pc.toWHex()}")
                trace.elements.add(TraceElement.DidNotTerminateWithinLimit(ctx.pc))
                return
            }
            val oldPc = ctx.pc
            ctx.pc += 4
            val stop = executeInstruction(oldPc, inDelaySlot = false)
            if (stop) {
                return
            }
        }
    }

    private fun executeInstruction(address: Int, inDelaySlot: Boolean): Boolean {
        trace.executedAddresses.add(address)
        val instr = disassembler.disassembleInstruction(binLoader, address)
        if (inDelaySlot && (instr.hasFlag(Jump) || instr.hasFlag(Branch))) {
            error("Tried to execute jump or branch instruction in branch delay slot")
        }
        if (DEBUG_INSTRUCTIONS) {
            println("---")
            println("$instr")
            GprReg.values().filter { it.id != -1 }.forEach {
                println("${it.name}=${ctx.readGpr(it).toWHex()}")
            }
            println("---")
        }

        fun handleBranch(condition: Boolean) {
            if (condition) {
                executeInstruction(address + 4, inDelaySlot = true)
                ctx.pc = if (instr.operands[1] is ImmOperand) instr.op1AsValue() else instr.op2AsValue()
            } else {
                if (!instr.hasFlag(BranchLikely)) {
                    executeInstruction(address + 4, inDelaySlot = true)
                }
                ctx.pc = address + 8
            }
        }

        when (instr.opcode) {
            // Arithmetic
            is MipsOpcode.Add, MipsOpcode.Addu, MipsOpcode.Addi, MipsOpcode.Addiu -> {
                ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue() + instr.op2AsValue())
            }
            is MipsOpcode.Sub, MipsOpcode.Subu -> {
                ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue() - instr.op2AsValue())
            }

            is MipsOpcode.Mult -> {
                val result = instr.op0AsValue().toLong() * instr.op1AsValue().toLong()
                ctx.hi = (result ushr 32).toInt()
                ctx.lo = result.toInt()
            }
            is MipsOpcode.Multu -> {
                val result = instr.op0AsValue().toUnsignedLong() * instr.op1AsValue().toUnsignedLong()
                ctx.hi = (result ushr 32).toInt()
                ctx.lo = result.toInt()
            }
            is MipsOpcode.Mflo -> {
                ctx.writeGpr(instr.op0AsReg(), ctx.lo)
            }
            is MipsOpcode.Mfhi -> {
                ctx.writeGpr(instr.op0AsReg(), ctx.hi)
            }
            is MipsOpcode.Ins -> {
                val rt = instr.op0AsValue()
                val rs = instr.op1AsValue()
                val pos = instr.op2AsImm()
                val size = instr.op3AsImm()
                val rsMask = -1 ushr (32 - size)
                val rtMask = (rsMask shl pos).inv()
                ctx.writeGpr(instr.op0AsReg(), (rt and rtMask) or (rs and rsMask shl pos))
            }
            is MipsOpcode.Ext -> {
                val rs = instr.op1AsValue()
                val pos = instr.op2AsImm()
                val size = instr.op3AsImm()
                val leftShift = 32 - (pos + size)
                ctx.writeGpr(instr.op0AsReg(), (rs shl leftShift) ushr (pos + leftShift))
            }

            is AllegrexOpcode.Min -> {
                ctx.writeGpr(instr.op0AsReg(), min(instr.op1AsValue(), instr.op2AsValue()))
            }
            is AllegrexOpcode.Max -> {
                ctx.writeGpr(instr.op0AsReg(), max(instr.op1AsValue(), instr.op2AsValue()))
            }

            // Logic
            is MipsOpcode.And, MipsOpcode.Andi -> {
                ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue() and instr.op2AsValue())
            }
            is MipsOpcode.Or, MipsOpcode.Ori -> {
                ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue() or instr.op2AsValue())
            }
            is MipsOpcode.Xor, MipsOpcode.Xori -> {
                ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue() xor instr.op2AsValue())
            }
            is MipsOpcode.Nor -> {
                ctx.writeGpr(instr.op0AsReg(), (instr.op1AsValue() or instr.op2AsValue()).inv())
            }

            // Shifts
            is MipsOpcode.Sll, MipsOpcode.Sllv -> {
                ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue() shl instr.op2AsValue())
            }
            is MipsOpcode.Srl, MipsOpcode.Srlv -> {
                ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue() ushr instr.op2AsValue())
            }
            is MipsOpcode.Sra, MipsOpcode.Srav -> {
                ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue() shr instr.op2AsValue())
            }

            // Set less than
            is MipsOpcode.Slt, MipsOpcode.Slti -> {
                ctx.writeGpr(instr.op0AsReg(), if (instr.op1AsValue() < instr.op2AsValue()) 1 else 0)
            }
            is MipsOpcode.Sltu, MipsOpcode.Sltiu -> {
                ctx.writeGpr(instr.op0AsReg(), if (instr.op1AsValue().toUInt() < instr.op2AsValue().toUInt()) 1 else 0)
            }

            // Sign extend
            is MipsOpcode.Seb -> {
                ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue() shl 24 shr 24)
            }
            is MipsOpcode.Seh -> {
                ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue() shl 16 shr 16)
            }

            // Memory loads
            is MipsOpcode.Lb -> {
                val at = instr.regPlusImm()
                val value = ctx.memory.readByte(at) shl 24 shr 24
                trace.elements.add(TraceElement.MemoryRead(at, 1, value, unsigned = false))
                ctx.writeGpr(instr.op0AsReg(), value)
            }
            is MipsOpcode.Lbu -> {
                val at = instr.regPlusImm()
                val value = ctx.memory.readByte(at)
                trace.elements.add(TraceElement.MemoryRead(at, 1, value, unsigned = true))
                ctx.writeGpr(instr.op0AsReg(), value)
            }
            is MipsOpcode.Lh -> {
                val at = instr.regPlusImm()
                val value = ctx.memory.readHalf(at) shl 16 shr 16
                trace.elements.add(TraceElement.MemoryRead(at, 2, value, unsigned = false))
                ctx.writeGpr(instr.op0AsReg(), value)
            }
            is MipsOpcode.Lhu -> {
                val at = instr.regPlusImm()
                val value = ctx.memory.readHalf(at)
                trace.elements.add(TraceElement.MemoryRead(at, 2, value, unsigned = true))
                ctx.writeGpr(instr.op0AsReg(), value)
            }
            is MipsOpcode.Lw -> {
                val at = instr.regPlusImm()
                val value = ctx.memory.readWord(at)
                trace.elements.add(TraceElement.MemoryRead(at, 4, value, unsigned = false))
                ctx.writeGpr(instr.op0AsReg(), value)
            }

            // Memory stores
            is MipsOpcode.Sb -> {
                val at = instr.regPlusImm()
                val value = ctx.readGpr(instr.op0AsReg())
                trace.elements.add(TraceElement.MemoryWrite(at, 1, value))
                ctx.memory.writeByte(at, value)
            }
            is MipsOpcode.Sh -> {
                val at = instr.regPlusImm()
                val value = ctx.readGpr(instr.op0AsReg())
                trace.elements.add(TraceElement.MemoryWrite(at, 2, value))
                ctx.memory.writeHalf(at, value)
            }
            is MipsOpcode.Sw -> {
                val at = instr.regPlusImm()
                val value = ctx.readGpr(instr.op0AsReg())
                trace.elements.add(TraceElement.MemoryWrite(at, 4, value))
                ctx.memory.writeWord(at, value)
            }

            // Branches
            is MipsOpcode.Beq, MipsOpcode.Beql -> {
                handleBranch(instr.op0AsValue() == instr.op1AsValue())
            }
            is MipsOpcode.Bne, MipsOpcode.Bnel -> {
                handleBranch(instr.op0AsValue() != instr.op1AsValue())
            }
            is MipsOpcode.Bgez, MipsOpcode.Bgezl -> {
                handleBranch(instr.op0AsValue() >= 0)
            }
            is MipsOpcode.Bgtz, MipsOpcode.Bgtzl -> {
                handleBranch(instr.op0AsValue() > 0)
            }
            is MipsOpcode.Blez, MipsOpcode.Blezl -> {
                handleBranch(instr.op0AsValue() <= 0)
            }
            is MipsOpcode.Bltz, MipsOpcode.Bltzl -> {
                handleBranch(instr.op0AsValue() < 0)
            }

            // Jumps
            is MipsOpcode.J, MipsOpcode.Jr -> {
                executeInstruction(address + 4, inDelaySlot = true)
                val target = instr.op0AsValue()
                if (instr.opcode == MipsOpcode.Jr) {
                    if (instr.op0AsReg() == GprReg.Ra && target == RETURN_TOKEN) {
                        // End of execution will be handled in parent
                        trace.elements.add(TraceElement.FunctionReturn(ctx.readGpr(GprReg.V0), ctx.readGpr(GprReg.V1)))
                        return true
                    } else if (instr.op0AsReg() == GprReg.Ra) {
                        trace.elements.add(TraceElement.FunctionReturn(ctx.readGpr(GprReg.V0), ctx.readGpr(GprReg.V1)))
                    } else if (module.functionForAddress(ctx.pc) != module.functionForAddress(target)) {
                        trace.elements.add(TraceElement.JumpOutOfFunctionBody(pc = ctx.pc, address = target))
                    }
                }
                ctx.pc = target
            }
            is MipsOpcode.Jal -> {
                executeInstruction(address + 4, inDelaySlot = true)
                ctx.writeGpr(GprReg.Ra, address + 8)
                val target = instr.op0AsValue()
                val targetFunction = module.functionForAddress(target)
                val argumentsCount = targetFunction?.let { module.getFunctionTypeArgumentCount(it.name) }
                val capturedArguments = (0..<(argumentsCount ?: 7)).map {
                    when (it) {
                        0 -> ctx.readGpr(GprReg.A0)
                        1 -> ctx.readGpr(GprReg.A1)
                        2 -> ctx.readGpr(GprReg.A2)
                        3 -> ctx.readGpr(GprReg.A3)
                        4 -> ctx.readGpr(GprReg.T0)
                        5 -> ctx.readGpr(GprReg.T1)
                        6 -> ctx.readGpr(GprReg.T2)
                        7 -> ctx.readGpr(GprReg.T3)
                        else -> {
                            error("Untested SP args") // TODO check this
                            // ctx.memory.readWord(ctx.readGpr(GprReg.Sp) + (it - 8) * 4)
                        }
                    }
                }
                trace.elements.add(
                    TraceElement.FunctionCall(
                        address = target,
                        name = targetFunction?.name ?: "<unknown>",
                        known = argumentsCount != null,
                        arguments = capturedArguments
                    )
                )
                if (targetFunction?.type == ModuleFunction.Type.IMPORT ||
                    (targetFunction?.type == ModuleFunction.Type.IMPLEMENTATION &&
                            ctx.functionHandlers.contains(targetFunction.name))
                ) {
                    (ctx.functionHandlers[targetFunction.name] ?: DefaultFunctionHandler).handle(ctx)
                    ctx.pc = address + 8
                    trace.elements.add(TraceElement.FunctionReturn(ctx.readGpr(GprReg.V0), ctx.readGpr(GprReg.V1)))
                } else {
                    ctx.pc = target
                }
            }

            // Misc
            is MipsOpcode.Movz -> {
                if (instr.op2AsValue() == 0) {
                    ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue())
                }
            }
            is MipsOpcode.Movn -> {
                if (instr.op2AsValue() != 0) {
                    ctx.writeGpr(instr.op0AsReg(), instr.op1AsValue())
                }
            }
            is MipsOpcode.Lui -> {
                ctx.writeGpr(instr.op0AsReg(), instr.op1AsImm() shl 16)
            }
            is MipsOpcode.Nop -> {
            }
            is MipsOpcode.Sync -> {
                trace.elements.add(TraceElement.Sync(instr.op0AsImm()))
            }

            else -> error("Unimplemented opcode: $instr")
        }

        if (instr.getModifiedRegisters().contains(GprReg.K1)) {
            trace.elements.add(TraceElement.ModifyK1(ctx.readGpr(GprReg.K1)))
        } else if (instr.getUsedRegisters().contains(GprReg.K1)) {
            trace.elements.add(TraceElement.UseK1(ctx.readGpr(GprReg.K1)))
        }

        return false
    }

    private fun MipsInstr.regPlusImm() = ctx.readGpr(op1AsReg()) + op2AsImm()

    private fun MipsInstr.op0AsValue() = operands[0].asValue()

    private fun MipsInstr.op1AsValue() = operands[1].asValue()

    private fun MipsInstr.op2AsValue() = operands[2].asValue()

    private fun Operand.asValue(): Int {
        return when (this) {
            is RegOperand -> ctx.readGpr(reg)
            is ImmOperand -> value
            else -> error("Unknown operand: $this")
        }
    }
}
