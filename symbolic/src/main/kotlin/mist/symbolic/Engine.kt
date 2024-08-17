package mist.symbolic

import io.ksmt.expr.KBitVec32Value
import io.ksmt.solver.KSolverStatus
import io.ksmt.utils.cast
import kio.util.child
import kio.util.toWHex
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mist.asm.*
import mist.asm.mips.BranchLikely
import mist.asm.mips.GprReg
import mist.asm.mips.MipsInstr
import mist.asm.mips.MipsOpcode
import mist.asm.mips.allegrex.AllegrexOpcode
import mist.io.BinLoader
import mist.module.Module
import mist.module.ModuleFunction
import mist.module.ModuleTypes
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class Engine(
  private val binLoader: BinLoader,
  private val disassembler: Disassembler<MipsInstr>,
  private val module: Module,
  private val moduleTypes: ModuleTypes,
  private val functionLibrary: FunctionLibrary,
  private val name: String,
  private val tracing: Boolean = false,
  private val modelsOutDir: File?,
  private val maxExecutedInstructions: Int = 10000,
  private val branchingLimit: Int = 256,
  private val globalBranchingLimit: Int = Integer.MAX_VALUE / 2,
  parallelism: Int = 16,
) {
  companion object {
    const val RETURN_TOKEN = 0x12244896
    const val INITIAL_SP = 0x09FFFE90
    const val DEAD_VALUE = 0xDEADBEEF.toInt()
    val assumedSpRange = (INITIAL_SP - 0x10000)..INITIAL_SP

    private val modelWriter = ModelWriter()
    private val extendedSolverTimeout = 10.seconds
    private val unknownFunctionArgsFallback = ModuleTypes.regsFunctionArgs
  }

  private var mode = EngineMode.SYMBOLIC_FORKING
  private val stats = EngineStats()

  @OptIn(ExperimentalCoroutinesApi::class)
  private val taskDispatcher = Dispatchers.IO.limitedParallelism(parallelism)
  private val dispatchedTasks = AtomicInteger(0)
  private val pendingCtxs = Channel<Context>(Channel.UNLIMITED)

  private val globalSymbolicBranches = ConcurrentBranchCounters()
  private val allExecutedAddresses = ConcurrentHashMap.newKeySet<Int>()

  private val nextLogAtMillis = ThreadLocal.withInitial { System.currentTimeMillis() }

  fun executeConcrete(ctx: Context): Trace {
    mode = EngineMode.CONCRETE
    ctx.memory.concrete = true
    executionLoop(ctx)
    return Trace(ctx.traceElements, allExecutedAddresses, ctx.memory.typedAllocations)
  }

  fun executeSymbolic(ctx: Context): Set<Int> {
    mode = EngineMode.SYMBOLIC_FORKING
    if (ctx.specificBranches.isNotEmpty()) {
      mode = EngineMode.SYMBOLIC_SPECIFIC
    }
    ctx.memory.concrete = false
    executionLoop(ctx)
    return allExecutedAddresses
  }

  private fun executionLoop(ctx: Context) = runBlocking {
    if (tracing) {
      check(ctx.traceElements.isEmpty()) { "Context was already traced" }
    }
    ctx.writeGpr(GprReg.Sp, Expr.Const.of(INITIAL_SP))
    ctx.writeGpr(GprReg.Ra, Expr.Const.of(RETURN_TOKEN))
    ctx.trace {
      TraceElement.ExecutionStart(
        ctx.pc,
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
    }

    pendingCtxs.send(ctx)
    coroutineScope {
      while (true) {
        val pendingCtx = pendingCtxs.tryReceive().getOrNull()
        if (dispatchedTasks.get() == 0 && pendingCtx == null) {
          println("[$name] Finished: $stats")
          break
        }
        if (pendingCtx != null) {
          dispatchedTasks.incrementAndGet()
          launch(taskDispatcher) {
            execute(pendingCtx)
            dispatchedTasks.decrementAndGet()
          }
        } else {
          yield()
        }
      }
    }
  }

  private suspend fun execute(ctx: Context) = ctx.useSolver {
    if (ctx.lastPathDecision() != null) {
      if (solveContext(ctx) == ExecuteResult.YIELD) {
        return@useSolver
      }
    }

    while (true) {
      if (ctx.executedInstrs >= maxExecutedInstructions) {
        println("WARN: Execution did not terminate within the configured limit, pc=${ctx.pc.toWHex()}")
        ctx.trace { TraceElement.DidNotTerminateWithinLimit(ctx.pc) }
        break
      }
      if (ctx.breakRaised) { // break happened in delay slot
        handleFinishedCtx(ctx, null)
        break
      }

      if (System.currentTimeMillis() > nextLogAtMillis.get()) {
        println(
          "[$name] [${Thread.currentThread().name}] dispatched=${dispatchedTasks.get()} " +
            "ctx=${ctx.hashCode().toWHex()} instrs=${ctx.executedInstrs} pc=${ctx.pc.toWHex()} $stats"
        )
        nextLogAtMillis.set(System.currentTimeMillis() + 1000)
      }

      val oldPc = ctx.pc
      ctx.pc += 4
      val result = runCatching {
        executeInstruction(ctx, oldPc, inDelaySlot = false)
      }
        .onFailure {
          stats.executionErrors.getAndIncrement()
          println("WARN: Execution error at ${oldPc.toWHex()}")
          it.printStackTrace()
        }
        .getOrElse { ExecuteResult.YIELD }
      if (result == ExecuteResult.YIELD) {
        break
      }
    }
  }

  private suspend fun executeInstruction(ctx: Context, address: Int, inDelaySlot: Boolean): ExecuteResult {
    ctx.executedInstrs++
    ctx.executedAddresses.add(address)
    val instr = disassembler.disassembleInstruction(binLoader, address)
    if (inDelaySlot && (instr.hasFlag(Jump) || instr.hasFlag(Branch))) {
      error("Tried to execute jump or branch instruction in branch delay slot")
    }

    when (instr.opcode) {
      // Arithmetic
      is MipsOpcode.Add, MipsOpcode.Addu, MipsOpcode.Addi, MipsOpcode.Addiu -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Add, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }
      is MipsOpcode.Sub, MipsOpcode.Subu -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Sub, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }

      is MipsOpcode.Mult -> {
        ctx.lo = Expr.Binary.of(BinaryOp.MultLo, instr.op0AsExpr(ctx), instr.op1AsExpr(ctx))
        ctx.hi = Expr.Binary.of(BinaryOp.MultHi, instr.op0AsExpr(ctx), instr.op1AsExpr(ctx))
      }
      is MipsOpcode.Multu -> {
        ctx.lo = Expr.Binary.of(BinaryOp.MultuLo, instr.op0AsExpr(ctx), instr.op1AsExpr(ctx))
        ctx.hi = Expr.Binary.of(BinaryOp.MultuHi, instr.op0AsExpr(ctx), instr.op1AsExpr(ctx))
      }
      is MipsOpcode.Div -> {
        ctx.lo = Expr.Binary.of(BinaryOp.Div, instr.op0AsExpr(ctx), instr.op1AsExpr(ctx))
        ctx.hi = Expr.Binary.of(BinaryOp.Mod, instr.op0AsExpr(ctx), instr.op1AsExpr(ctx))
      }
      is MipsOpcode.Divu -> {
        ctx.lo = Expr.Binary.of(BinaryOp.Divu, instr.op0AsExpr(ctx), instr.op1AsExpr(ctx))
        ctx.hi = Expr.Binary.of(BinaryOp.Modu, instr.op0AsExpr(ctx), instr.op1AsExpr(ctx))
      }
      is MipsOpcode.Mflo -> {
        ctx.writeGpr(instr.op0AsReg(), ctx.lo)
      }
      is MipsOpcode.Mfhi -> {
        ctx.writeGpr(instr.op0AsReg(), ctx.hi)
      }

      is MipsOpcode.Ins -> {
        val pos = instr.op2AsImm()
        val size = instr.op3AsImm()
        ctx.writeGpr(instr.op0AsReg(), Expr.Insert.of(instr.op0AsExpr(ctx), instr.op1AsExpr(ctx), pos, size))
      }
      is MipsOpcode.Ext -> {
        val pos = instr.op2AsImm()
        val size = instr.op3AsImm()
        ctx.writeGpr(instr.op0AsReg(), Expr.ExtractZeroExtend.of(instr.op1AsExpr(ctx), pos + size - 1, pos))
      }

      is AllegrexOpcode.Min -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Min, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }
      is AllegrexOpcode.Max -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Max, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }

      // Logic
      is MipsOpcode.And, MipsOpcode.Andi -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.And, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }
      is MipsOpcode.Or, MipsOpcode.Ori -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Or, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }
      is MipsOpcode.Xor, MipsOpcode.Xori -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Xor, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }
      is MipsOpcode.Nor -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Nor, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }

      // Shifts
      is MipsOpcode.Sll, MipsOpcode.Sllv -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Sll, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }
      is MipsOpcode.Srl, MipsOpcode.Srlv -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Srl, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }
      is MipsOpcode.Sra, MipsOpcode.Srav -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Sra, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }

      // Set less than
      is MipsOpcode.Slt, MipsOpcode.Slti -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Slt, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }
      is MipsOpcode.Sltu, MipsOpcode.Sltiu -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Sltu, instr.op1AsExpr(ctx), instr.op2AsExpr(ctx)))
      }

      // Sign extend
      is MipsOpcode.Seb -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Unary.of(UnaryOp.Seb, instr.op1AsExpr(ctx)))
      }
      is MipsOpcode.Seh -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Unary.of(UnaryOp.Seh, instr.op1AsExpr(ctx)))
      }

      // Memory loads
      is MipsOpcode.Lb -> {
        val at = instr.regPlusImm(ctx)
        val value = ctx.memory.readByte(at)
        ctx.trace { TraceElement.MemoryRead(address, at, 1, value) }
        ctx.writeGpr(instr.op0AsReg(), value)
      }
      is MipsOpcode.Lbu -> {
        val at = instr.regPlusImm(ctx)
        val value = ctx.memory.readByteUnsigned(at)
        ctx.trace { TraceElement.MemoryRead(address, at, 1, value, unsigned = true) }
        ctx.writeGpr(instr.op0AsReg(), value)
      }
      is MipsOpcode.Lh -> {
        val at = instr.regPlusImm(ctx)
        val value = ctx.memory.readHalf(at)
        ctx.trace { TraceElement.MemoryRead(address, at, 2, value) }
        ctx.writeGpr(instr.op0AsReg(), value)
      }
      is MipsOpcode.Lhu -> {
        val at = instr.regPlusImm(ctx)
        val value = ctx.memory.readHalfUnsigned(at)
        ctx.trace { TraceElement.MemoryRead(address, at, 2, value, unsigned = true) }
        ctx.writeGpr(instr.op0AsReg(), value)
      }
      is MipsOpcode.Lw -> {
        val at = instr.regPlusImm(ctx)
        val value = ctx.memory.readWord(at)
        ctx.trace { TraceElement.MemoryRead(address, at, 4, value) }
        ctx.writeGpr(instr.op0AsReg(), value)
      }
      is MipsOpcode.Lwl -> {
        val at = instr.regPlusImm(ctx)
        val shift = Expr.Binary.of(BinaryOp.And, at, Expr.Const.of(0b11))
        val effectiveAt = Expr.Binary.of(BinaryOp.Sub, at, shift)
        val prevValue = Expr.Binary.of(
          BinaryOp.And,
          instr.op0AsExpr(ctx),
          Expr.Binary.of(
            BinaryOp.Srl,
            Expr.Const.of(0xffffffff.toInt()),
            Expr.Binary.of(
              BinaryOp.Sll,
              Expr.Binary.of(BinaryOp.Add, shift, Expr.Const.of(1)),
              Expr.Const.of(3)
            )
          )
        )
        val loadValue = Expr.Binary.of(
          BinaryOp.Sll,
          ctx.memory.readWord(effectiveAt),
          Expr.Binary.of(
            BinaryOp.Sll,
            Expr.Binary.of(BinaryOp.Sub, Expr.Const.of(3), shift),
            Expr.Const.of(3)
          )
        )
        ctx.trace {
          TraceElement.MemoryRead(address, effectiveAt, 4, loadValue, unaligned = TraceElement.UnalignedMemoryAccess.Left, shift = shift)
        }
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Or, prevValue, loadValue))
      }
      is MipsOpcode.Lwr -> {
        val at = instr.regPlusImm(ctx)
        val shift = Expr.Binary.of(BinaryOp.And, at, Expr.Const.of(0b11))
        val effectiveAt = Expr.Binary.of(BinaryOp.Sub, at, shift)
        val prevValue = Expr.Binary.of(
          BinaryOp.And,
          instr.op0AsExpr(ctx),
          Expr.Binary.of(
            BinaryOp.Sll,
            Expr.Const.of(0xffffffff.toInt()),
            Expr.Binary.of(
              BinaryOp.Sll,
              Expr.Binary.of(BinaryOp.Sub, Expr.Const.of(4), shift),
              Expr.Const.of(3)
            )
          )
        )
        val loadValue = Expr.Binary.of(
          BinaryOp.Srl,
          ctx.memory.readWord(effectiveAt),
          Expr.Binary.of(BinaryOp.Sll, shift, Expr.Const.of(3))
        )
        ctx.trace {
          TraceElement.MemoryRead(address, effectiveAt, 4, loadValue, unaligned = TraceElement.UnalignedMemoryAccess.Right, shift = shift)
        }
        ctx.writeGpr(instr.op0AsReg(), Expr.Binary.of(BinaryOp.Or, prevValue, loadValue))
      }

      // Memory stores
      is MipsOpcode.Sb -> {
        val at = instr.regPlusImm(ctx)
        val value = ctx.readGpr(instr.op0AsReg())
        ctx.trace { TraceElement.MemoryWrite(address, at, 1, value) }
        ctx.memory.writeByte(at, value)
      }
      is MipsOpcode.Sh -> {
        val at = instr.regPlusImm(ctx)
        val value = ctx.readGpr(instr.op0AsReg())
        ctx.trace { TraceElement.MemoryWrite(address, at, 2, value) }
        ctx.memory.writeHalf(at, value)
      }
      is MipsOpcode.Sw -> {
        val at = instr.regPlusImm(ctx)
        val value = ctx.readGpr(instr.op0AsReg())
        ctx.trace { TraceElement.MemoryWrite(address, at, 4, value) }
        ctx.memory.writeWord(at, value)
      }
      is MipsOpcode.Swl -> {
        val at = instr.regPlusImm(ctx)
        val shift = Expr.Binary.of(BinaryOp.And, at, Expr.Const.of(0b11))
        val effectiveAt = Expr.Binary.of(BinaryOp.Sub, at, shift)
        val prevValue = Expr.Binary.of(
          BinaryOp.And,
          ctx.memory.readWord(effectiveAt),
          Expr.Binary.of(
            BinaryOp.Sll,
            Expr.Const.of(0xffffffff.toInt()),
            Expr.Binary.of(
              BinaryOp.Sll,
              Expr.Binary.of(BinaryOp.Add, shift, Expr.Const.of(1)),
              Expr.Const.of(3)
            )
          )
        )
        val storeValue = Expr.Binary.of(
          BinaryOp.Srl,
          instr.op0AsExpr(ctx),
          Expr.Binary.of(
            BinaryOp.Sll,
            Expr.Binary.of(BinaryOp.Sub, Expr.Const.of(3), shift),
            Expr.Const.of(3)
          )
        )
        ctx.trace {
          TraceElement.MemoryWrite(address, effectiveAt, 4, storeValue, unaligned = TraceElement.UnalignedMemoryAccess.Left, shift = shift)
        }
        ctx.memory.writeWord(effectiveAt, Expr.Binary.of(BinaryOp.Or, prevValue, storeValue))
      }
      is MipsOpcode.Swr -> {
        val at = instr.regPlusImm(ctx)
        val shift = Expr.Binary.of(BinaryOp.And, at, Expr.Const.of(0b11))
        val effectiveAt = Expr.Binary.of(BinaryOp.Sub, at, shift)
        val prevValue = Expr.Binary.of(
          BinaryOp.And,
          ctx.memory.readWord(effectiveAt),
          Expr.Binary.of(
            BinaryOp.Srl,
            Expr.Const.of(0xffffffff.toInt()),
            Expr.Binary.of(
              BinaryOp.Sll,
              Expr.Binary.of(BinaryOp.Sub, Expr.Const.of(4), shift),
              Expr.Const.of(3)
            )
          )
        )
        val storeValue = Expr.Binary.of(
          BinaryOp.Sll,
          ctx.memory.readWord(effectiveAt),
          Expr.Binary.of(BinaryOp.Sll, shift, Expr.Const.of(3))
        )
        ctx.trace {
          TraceElement.MemoryWrite(address, effectiveAt, 4, storeValue, unaligned = TraceElement.UnalignedMemoryAccess.Right, shift = shift)
        }
        ctx.memory.writeWord(effectiveAt, Expr.Binary.of(BinaryOp.Or, prevValue, storeValue))
      }

      // Branches
      is MipsOpcode.Beq, MipsOpcode.Beql -> {
        return handleBranch(ctx, address, instr, Expr.Condition.of(ConditionOp.Eq, instr.op0AsExpr(ctx), instr.op1AsExpr(ctx)))
      }
      is MipsOpcode.Bne, MipsOpcode.Bnel -> {
        return handleBranch(ctx, address, instr, Expr.Condition.of(ConditionOp.Neq, instr.op0AsExpr(ctx), instr.op1AsExpr(ctx)))
      }
      is MipsOpcode.Bgez, MipsOpcode.Bgezl -> {
        return handleBranch(ctx, address, instr, Expr.Condition.of(ConditionOp.Ge, instr.op0AsExpr(ctx), Expr.ZERO))
      }
      is MipsOpcode.Bgtz, MipsOpcode.Bgtzl -> {
        return handleBranch(ctx, address, instr, Expr.Condition.of(ConditionOp.Gt, instr.op0AsExpr(ctx), Expr.ZERO))
      }
      is MipsOpcode.Blez, MipsOpcode.Blezl -> {
        return handleBranch(ctx, address, instr, Expr.Condition.of(ConditionOp.Le, instr.op0AsExpr(ctx), Expr.ZERO))
      }
      is MipsOpcode.Bltz, MipsOpcode.Bltzl -> {
        return handleBranch(ctx, address, instr, Expr.Condition.of(ConditionOp.Lt, instr.op0AsExpr(ctx), Expr.ZERO))
      }

      // Jumps
      is MipsOpcode.J -> {
        handleJump(ctx, address, instr)
      }
      is MipsOpcode.Jal -> {
        handleJumpAndLink(ctx, address, instr)
      }
      is MipsOpcode.Jr -> {
        return handleJumpRegister(ctx, address, instr)
      }

      // Misc
      is MipsOpcode.Movz -> {
        ctx.writeGpr(
          instr.op0AsReg(),
          Expr.ValueIf.of(
            Expr.Condition.of(ConditionOp.Eq, instr.op2AsExpr(ctx), Expr.ZERO),
            instr.op1AsExpr(ctx),
            ctx.readGpr(instr.op0AsReg())
          )
        )
      }
      is MipsOpcode.Movn -> {
        ctx.writeGpr(
          instr.op0AsReg(),
          Expr.ValueIf.of(
            Expr.Condition.of(ConditionOp.Neq, instr.op2AsExpr(ctx), Expr.ZERO),
            instr.op1AsExpr(ctx),
            ctx.readGpr(instr.op0AsReg())
          )
        )
      }
      is MipsOpcode.Lui -> {
        ctx.writeGpr(instr.op0AsReg(), Expr.Const.of(instr.op1AsImm() shl 16))
      }
      is MipsOpcode.Nop -> {
      }
      is MipsOpcode.Sync -> {
        ctx.trace { TraceElement.Sync(address, instr.op0AsImm()) }
      }
      is MipsOpcode.Break -> {
        ctx.trace { TraceElement.Break(address, instr.op0AsImm()) }
        ctx.breakRaised = true
      }

      else -> error("Unimplemented opcode: $instr")
    }

    if (tracing) {
      if (instr.getModifiedRegisters().contains(GprReg.K1)) {
        ctx.trace { TraceElement.ModifyK1(address, ctx.readGpr(GprReg.K1)) }
      } else if (instr.getUsedRegisters().contains(GprReg.K1)) {
        ctx.trace { TraceElement.UseK1(address, ctx.readGpr(GprReg.K1)) }
      }
    }

    return ExecuteResult.CONTINUE
  }

  private suspend fun handleBranch(ctx: Context, address: Int, instr: MipsInstr, condition: BoolExpr): ExecuteResult {
    val branchTakenPc = if (instr.operands[1] is ImmOperand) instr.op1AsImm() else instr.op2AsImm()
    val expectedBranchTaken = if (mode == EngineMode.SYMBOLIC_SPECIFIC) ctx.specificBranches.removeAt(0) else null

    if (condition is Expr.Bool) {
      if (expectedBranchTaken != null && condition.value != expectedBranchTaken) {
        error("Branch evaluated to ${condition.value} but context specific branch expected $expectedBranchTaken")
      }
      return handleConcreteBranch(ctx, instr, address, condition, branchTakenPc)
    }

    if (expectedBranchTaken != null) {
      return if (expectedBranchTaken) {
        handleSymbolicBranchTaken(ctx, address, condition, branchTakenPc, pendingBranch = false)
      } else {
        handleSymbolicBranchNotTaken(ctx, instr, address, condition, pendingBranch = false)
      }
    }

    // symbolic forking
    handleSymbolicBranchTaken(ctx.copyOf(), address, condition, branchTakenPc, pendingBranch = true)
    return handleSymbolicBranchNotTaken(ctx, instr, address, condition, pendingBranch = false)
  }

  private suspend fun handleSymbolicBranchTaken(
    ctx: Context,
    address: Int,
    condition: BoolExpr,
    branchTakenPc: Int,
    pendingBranch: Boolean
  ): ExecuteResult {
    executeInstruction(ctx, address + 4, inDelaySlot = true)
    return handleSymbolicBranch(ctx, address, condition, branchTakenPc, pendingBranch)
  }

  private suspend fun handleSymbolicBranchNotTaken(
    ctx: Context,
    instr: MipsInstr,
    address: Int,
    condition: BoolExpr,
    pendingBranch: Boolean
  ): ExecuteResult {
    if (!instr.hasFlag(BranchLikely)) {
      executeInstruction(ctx, address + 4, inDelaySlot = true)
    }
    return handleSymbolicBranch(ctx, address, Expr.Not.of(condition), address + 8, pendingBranch)
  }

  private suspend fun handleConcreteBranch(
    ctx: Context,
    instr: MipsInstr,
    address: Int,
    condition: Expr.Bool,
    branchTakenPc: Int
  ): ExecuteResult {
    if (condition.value) {
      ctx.trace { TraceElement.Branch(address, true) }
      executeInstruction(ctx, address + 4, inDelaySlot = true)
      ctx.pc = branchTakenPc
    } else {
      ctx.trace { TraceElement.Branch(address, false) }
      if (!instr.hasFlag(BranchLikely)) {
        executeInstruction(ctx, address + 4, inDelaySlot = true)
      }
      ctx.pc = address + 8
    }
    return ExecuteResult.CONTINUE
  }

  private suspend fun handleSymbolicBranch(
    ctx: Context,
    address: Int,
    condition: BoolExpr,
    toAddress: Int,
    pendingBranch: Boolean
  ): ExecuteResult {
    ctx.addPathDecision(PathDecision(address, toAddress, condition))
    ctx.pc = toAddress

    val branchedCounter = ctx.symbolicBranches.getCounter(address, toAddress)
    val globalBranchedCounter = globalSymbolicBranches.getCounter(address, toAddress)

    globalBranchedCounter.get().let {
      if (it >= globalBranchingLimit) {
        if (it == globalBranchingLimit) {
          println("BLOCKING: Too many repeated branches: ${address.toWHex()} -> ${toAddress.toWHex()}, blocking all paths")
          globalBranchedCounter.getAndIncrement()
        }
        stats.droppedPaths.getAndIncrement()
        return ExecuteResult.YIELD
      }
    }

    if (branchedCounter.value >= branchingLimit) {
      if (branchedCounter.value == branchingLimit) {
        println("BLOCKING: Too many repeated context branches: ${address.toWHex()} -> ${toAddress.toWHex()}, blocking context path")
        branchedCounter.increment()
      }
      stats.droppedPaths.getAndIncrement()
      return ExecuteResult.YIELD
    }

    branchedCounter.increment()
    globalBranchedCounter.getAndIncrement()

    if (pendingBranch) {
      if (mode != EngineMode.SYMBOLIC_FORKING) {
        error("Context fork in $mode mode should not happen")
      }
      pendingCtxs.send(ctx)
      return ExecuteResult.YIELD
    }

    return solveContext(ctx)
  }

  private fun solveContext(ctx: Context): ExecuteResult {
    val status = ctx.checkSolver()
    when (status) {
      KSolverStatus.SAT -> {
        stats.satSolverResults.getAndIncrement()
        val lastDecision = ctx.lastPathDecision()
        if (lastDecision != null
          && lastDecision.condition == ctx.lastPathDecision(1)?.condition
          && ctx.memory.writesSinceLastBranch == 0
          && ctx.memory.hwWordsReadsSinceLastBranch == 1
        ) {
          // TODO test if this works
          println("WARN: potential HW spin wait on ${lastDecision.fromAddress.toWHex()} -> ${lastDecision.toAddress.toWHex()}, blocking path")
          globalSymbolicBranches.getCounter(lastDecision.fromAddress, lastDecision.toAddress).set(globalBranchingLimit + 1)
          stats.droppedPaths.getAndIncrement()
          return ExecuteResult.YIELD
        }
        ctx.memory.resetBranchAccessTracking()
        return ExecuteResult.CONTINUE
      }
      KSolverStatus.UNSAT -> {
        stats.unsatSolverResults.getAndIncrement()
        return ExecuteResult.YIELD
      }
      KSolverStatus.UNKNOWN -> {
        stats.unknownSolverResults.getAndIncrement()
        return ExecuteResult.YIELD
      }
    }
  }

  private suspend fun handleJump(ctx: Context, address: Int, instr: MipsInstr) {
    executeInstruction(ctx, address + 4, inDelaySlot = true)
    val toAddress = instr.op0AsImm()
    if (tracing) {
      if (module.getFunctionByAddress(address) != module.getFunctionByAddress(toAddress)) {
        ctx.trace { TraceElement.JumpOutOfFunctionBody(pc = address, toAddress = toAddress, sourceReg = null) }
      }
    }
    ctx.pc = toAddress
  }

  private suspend fun handleJumpAndLink(ctx: Context, address: Int, instr: MipsInstr) {
    executeInstruction(ctx, address + 4, inDelaySlot = true)
    ctx.writeGpr(GprReg.Ra, Expr.Const.of(address + 8))

    val toAddress = instr.op0AsImm()
    val function = module.getFunctionByAddress(toAddress)
    ctx.trace {
      val functionArgs = function?.let { moduleTypes.getFunctionArgs(it.name) }
      val arguments = (functionArgs ?: unknownFunctionArgsFallback).map { it.read(ctx) }
      TraceElement.FunctionCall(
        address,
        address = toAddress,
        name = function?.name ?: "<unknown>",
        known = functionArgs != null,
        arguments = arguments
      )
    }

    if (function?.type == ModuleFunction.Type.IMPORT ||
      (function?.type == ModuleFunction.Type.IMPLEMENTATION && functionLibrary.supports(function.name))
    ) {
      val providesImplementation = functionLibrary.getOrThrow(function.name).handle(ctx)
      if (!providesImplementation) {
        ctx.pc = address + 8
        ctx.trace {
          TraceElement.FunctionReturn(
            address,
            function.name,
            moduleTypes.getFunctionReturnSize(function.name),
            ctx.readGpr(GprReg.V0),
            ctx.readGpr(GprReg.V1)
          )
        }
      }
    } else {
      ctx.pc = toAddress
    }
  }

  private suspend fun handleJumpRegister(ctx: Context, address: Int, instr: MipsInstr): ExecuteResult {
    executeInstruction(ctx, address + 4, inDelaySlot = true)
    val reg = instr.op0AsReg()
    val regExpr = ctx.readGpr(reg)

    if (regExpr is Expr.Const) {
      ctx.pc = regExpr.value
    } else {
      val status = ctx.checkSolver(extendedSolverTimeout)
      if (status != KSolverStatus.SAT) {
        stats.failedJumpResolution.getAndIncrement()
        println("WARN: Solver failed for jump instruction: $instr ($reg=${regExpr} status=$status), dropping path")
        return ExecuteResult.YIELD
      }
      val evalPc = ctx.solverEval(regExpr, false)
      if (evalPc is KBitVec32Value) {
        ctx.pc = evalPc.intValue
      } else {
        println("WARN: return address evaluated with is complete = true")
        val evalPcComplete: KBitVec32Value = ctx.solverEval(regExpr, true).cast()
        ctx.pc = evalPcComplete.intValue
      }
    }

    if (tracing) {
      if (reg != GprReg.Ra) {
        if (module.getFunctionByAddress(address) != module.getFunctionByAddress(ctx.pc)) {
          ctx.trace { TraceElement.JumpOutOfFunctionBody(pc = address, toAddress = ctx.pc, sourceReg = reg) }
        }
      } else {
        ctx.trace {
          val functionName = module.getFunctionByAddress(address)?.name
            ?: ctx.traceElements.filterIsInstance<TraceElement.FunctionCall>().last().name
          TraceElement.FunctionReturn(
            address,
            functionName,
            functionName.let { moduleTypes.getFunctionReturnSize(it) },
            ctx.readGpr(GprReg.V0),
            ctx.readGpr(GprReg.V1)
          )
        }
      }
    }

    if (ctx.pc == RETURN_TOKEN && reg == GprReg.Ra) {
      handleFinishedCtx(ctx, instr)
      return ExecuteResult.YIELD
    }
    return ExecuteResult.CONTINUE
  }

  private fun handleFinishedCtx(ctx: Context, instr: MipsInstr?) {
    if (mode != EngineMode.CONCRETE) {
      // Solve in case it was for example concrete jump during symbolic execution
      val status = ctx.checkSolver(extendedSolverTimeout)
      if (status != KSolverStatus.SAT) {
        stats.failedFinishedCtxSolve.getAndIncrement()
        println("WARN: Solver failed for finished context: $instr (status=$status), dropping path")
        return
      }
    }
    val pathId = stats.finishedPaths.getAndIncrement()
    allExecutedAddresses.addAll(ctx.executedAddresses)
    if (modelsOutDir != null) {
      modelWriter.writeToFile(ctx.solverModel().detach(), ctx.functionStates, modelsOutDir.child("%06d.txt".format(pathId)))
    }
  }

  private inline fun Context.trace(block: () -> TraceElement) {
    if (tracing) {
      traceElements.add(block())
    }
  }

  private fun MipsInstr.regPlusImm(ctx: Context) = Expr.Binary.of(BinaryOp.Add, ctx.readGpr(op1AsReg()), Expr.Const.of(op2AsImm()))

  private fun MipsInstr.op0AsExpr(ctx: Context) = operands[0].asExpr(ctx)

  private fun MipsInstr.op1AsExpr(ctx: Context) = operands[1].asExpr(ctx)

  private fun MipsInstr.op2AsExpr(ctx: Context) = operands[2].asExpr(ctx)

  private fun Operand.asExpr(ctx: Context): BvExpr {
    return when (this) {
      is RegOperand -> ctx.readGpr(reg)
      is ImmOperand -> Expr.Const.of(value)
      else -> error("Unknown operand: $this")
    }
  }

  private enum class ExecuteResult {
    YIELD,
    CONTINUE,
  }

  private enum class EngineMode {
    SYMBOLIC_FORKING,
    SYMBOLIC_SPECIFIC,
    CONCRETE
  }
}
