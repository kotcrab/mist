package mist.asm.mips

import kio.util.swap
import kio.util.toWHex
import mist.asm.*
import mist.asm.mips.MipsOpcode.*
import mist.io.BinLoader
import mist.util.MistLogger
import mist.util.logTag
import java.util.*

class MipsGraph(
  private val loader: BinLoader,
  private val instrs: List<MipsInstr>,
  private val logger: MistLogger
) {
  private val tag = logTag()

  private val switchIdioms = MipsSwitchIdioms()

  val nodes = mutableListOf<Node<MipsInstr>>()

  // maps jump cause to instruction that it is jumping to
  val jumpingTo = mutableMapOf<MipsInstr, MutableSet<MipsInstr>>()

  // maps jump target to instruction that jumps to it
  val jumpingToFrom = mutableMapOf<MipsInstr, MutableSet<MipsInstr>>()

  // maps unreachable instruction address to instruction at that address
  val unreachableInstrs = mutableMapOf<Int, MipsInstr>()

  // maps `jt at` and `jr a0` instructions to their respective switch metadata
  val switchSrcInstrs = mutableMapOf<Int, SwitchDescriptor>()

  // maps instruction at specified address to descriptor with switch cases list
  val switchCasesInstrs = mutableMapOf<Int, SwitchCaseDescriptor>()

  // list of instructions that are in branch delay slots
  val branchDelaySlotInstrs = mutableMapOf<Int, MipsInstr>()

  fun generateGraph() {
    check(nodes.isEmpty()) { "Graph already generated" }
    fillControlFlowCtx()
    createNodes()
    connectNodes()
    transformBranchDelaySlot()
    classifyEdges()
    markDeadCode()
    postCheck()
  }

  private fun fillControlFlowCtx() {
    instrs.forEachIndexed { srcInstrIdx, srcInstr ->
      when {
        srcInstr.hasFlag(Branch) -> {
          val imm = srcInstr.operands.last { it is ImmOperand } as ImmOperand
          val destInstr = instrs.firstOrNull { it.addr == imm.value }
            ?: logger.panic(tag, "branch to instruction outside current function")
          jumpingTo.getOrPut(srcInstr, defaultValue = { mutableSetOf() }).add(destInstr)
          jumpingToFrom.getOrPut(destInstr, defaultValue = { mutableSetOf() }).add(srcInstr)
        }
        srcInstr.matches(J) -> {
          val destInstr = instrs.firstOrNull { it.addr == srcInstr.op0AsImm() }
          if (destInstr != null) { // if jump within function
            jumpingTo.getOrPut(srcInstr, defaultValue = { mutableSetOf() }).add(destInstr)
            jumpingToFrom.getOrPut(destInstr, defaultValue = { mutableSetOf() }).add(srcInstr)
          }
        }
        // switch case idiom detection
        srcInstr.matches(Jr, op0 = isReg(GprReg.At)) -> {
          switchIdioms.atRegIdiom.matches(instrs, srcInstrIdx)?.let { handleSwitch(it) }
        }
        srcInstr.matches(Jr, op0 = isReg(GprReg.A0)) -> {
          switchIdioms.a0RegIdiom.matches(instrs, srcInstrIdx)?.let { handleSwitch(it) }
        }
      }
    }
  }

  private fun handleSwitch(result: SwitchDescriptor) {
    logger.trace(tag, "switch ${result.switchCaseCount} cases, jump table at ${result.jumpTableLoc.toWHex()}")
    val srcInstr = result.relInstrs.first()
    switchSrcInstrs[srcInstr.addr] = result
    repeat(result.switchCaseCount) { case ->
      val caseTargetAddr = loader.readInt(result.jumpTableLoc + case * 0x4)
      val destInstr = instrs.firstOrNull { it.addr == caseTargetAddr }
      if (destInstr == null) {
        logger.warn(
          tag, "switch case $case jumps to instruction outside current function and will be ignored, " +
            "jump target: ${caseTargetAddr.toWHex()}"
        )
        return@repeat
      }
      jumpingTo.getOrPut(srcInstr, defaultValue = { mutableSetOf() }).add(destInstr)
      jumpingToFrom.getOrPut(destInstr, defaultValue = { mutableSetOf() }).add(srcInstr)
      switchCasesInstrs.getOrPut(destInstr.addr, defaultValue = { SwitchCaseDescriptor(destInstr) }).cases
        .add(case)
    }
  }

  private fun createNodes() {
    nodes.add(Node())
    var splitOnNext = false
    instrs.forEach { instr ->
      if (jumpingToFrom.contains(instr) && jumpingToFrom[instr]!!.size != 0) {
        if (nodes.last().instrs.size != 0) nodes.add(Node())
      }
      nodes.last().instrs.add(instr)
      if (splitOnNext) {
        if (nodes.last().instrs.size != 0) nodes.add(Node())
        splitOnNext = false
      }
      if (jumpingTo.contains(instr) && jumpingTo[instr]!!.size != 0) {
        splitOnNext = true
      }
    }
    if (nodes.last().instrs.isEmpty()) nodes.remove(nodes.last())
  }

  private fun connectNodes() {
    nodes.forEach { node ->
      if (node.instrs.size >= 2) {
        val jumpCause = node.instrs[node.instrs.lastIndex - 1]
        when {
          jumpCause.opcode == J -> { // unconditional jump
            val jumpTargetInstr = instrs.firstOrNull { it.addr == jumpCause.op0AsImm() }
            if (jumpTargetInstr != null) {
              linkJumpToNode(node, jumpTargetInstr)
              return@forEach
            } else {
              // jumping outside context of current function, ignored
              // important: connecting this node with fallthrough edge in handled bellow
              logger.warn(tag, "unconditional jump outside of current context will be ignored")
            }
          }
          jumpCause.opcode == Jal -> { // most likely call to other function
            val jumpingToInstr = instrs.firstOrNull { it.addr == jumpCause.op0AsImm() }
            if (jumpingToInstr != null) {
              logger.panic(tag, "jal (jump and link) to instruction inside current function")
            }
          }
          jumpCause.matches(Jr, op0 = isReg(GprReg.Ra)) -> { // function exit
            return@forEach
          }
          switchSrcInstrs.contains(jumpCause.addr) -> {
            jumpingTo[jumpCause]?.forEach { jumpTargetInstr ->
              linkJumpToNode(node, jumpTargetInstr)
            }
            return@forEach
          }
          jumpCause.opcode == Jalr -> {
            logger.warn(tag, "function uses jalr (if jumping within current function graph will be invalid)")
          }
          jumpCause.hasFlag(Jump) -> logger.panic(tag, "unhandled jump type instruction: $jumpCause")
        }
      }

      // this will handle "b label" MIPS instruction alias
      // and in general `beq reg1, reg2, label` where reg1 == reg2
      // it should be enough for compiled code
      fun MipsInstr.isUnconditionalBranch() = (opcode == Beq
        && operands.getOrNull(0) is RegOperand && operands.getOrNull(1) is RegOperand && op0AsReg() == op1AsReg())

      val branchCause = node.getBranchCauseInstr()

      // handle fall node
      if (branchCause == null || branchCause.isUnconditionalBranch() == false) {
        val fallToInstr = instrs.indexOf(node.instrs.last()) + 1
        if (fallToInstr < instrs.size) {
          val otherNode = getNodeForInstr(instrs[fallToInstr])
          node.addNode(otherNode, EdgeType.Fallthrough)
        } else {
          logger.warn(tag, "unexpected end of function")
        }
      }

      // handle jumps to other node
      if (branchCause != null) {
        val branchTargets = jumpingTo[branchCause]
        if (branchTargets != null) {
          branchTargets.forEach { branchDst ->
            val otherNode = getNodeForInstr(branchDst)
            node.addNode(otherNode, EdgeType.JumpTaken)
          }
        } else {
          logger.panic(
            tag, "branch instruction '$branchCause' does not define any branch target, check control" +
              " flow context generation for mistakes"
          )
        }
      }
    }
  }

  private fun linkJumpToNode(srcNode: Node<MipsInstr>, jumpTargetInstr: MipsInstr) {
    val destNode = nodes.first { it.instrs.contains(jumpTargetInstr) }
    val destInNodeIdx = destNode.instrs.indexOf(jumpTargetInstr)
    if (destInNodeIdx != 0) {
      logger.panic(
        tag, "jump to instruction that is not first in the node, check control flow context " +
          "generation for mistakes"
      )
    }
    srcNode.addNode(destNode, EdgeType.JumpTaken)
  }

  private fun transformBranchDelaySlot() {
    nodes.toList().forEach { node ->
      node.instrs.toMutableList().forEachIndexed { index, instr ->
        if (!instr.hasFlag(Jump) && !instr.hasFlag(Branch)) {
          return@forEachIndexed
        }
        when {
          instr.opcode in arrayOf(J, Jal) -> {
            // jal and j should be always safe to swap
            // they don't use register so there is no risk of
            // swapped instruction modifying some source register
            val delaySlotInstr = node.instrs[index + 1]
            branchDelaySlotInstrs[delaySlotInstr.addr] = delaySlotInstr
            node.instrs.swap(index, index + 1)
          }
          instr.opcode in arrayOf(Jr, Jalr) -> {
            val delaySlotInstr = node.instrs[index + 1]
            branchDelaySlotInstrs[delaySlotInstr.addr] = delaySlotInstr
            if (delaySlotInstr.getModifiedRegisters().any { instr.getUsedRegisters().contains(it) }) {
              logger.panic(
                tag, "$instr instruction not safe to swap, delay slot instruction: $delaySlotInstr"
              )
            }
            node.instrs.swap(index, index + 1)
          }
          instr.hasFlag(BranchLikely) -> {
            val delaySlotInstr = node.instrs[index + 1]
            branchDelaySlotInstrs[delaySlotInstr.addr] = delaySlotInstr
            propagateBranchDelaySlot(node, instr, delaySlotInstr)
          }
          instr.hasFlag(Branch) -> {
            val delaySlotInstr = node.instrs[index + 1]
            branchDelaySlotInstrs[delaySlotInstr.addr] = delaySlotInstr
            if (delaySlotInstr.getModifiedRegisters().any { instr.getUsedRegisters().contains(it) }) {
              // fallback to propagation if swap not safe
              propagateBranchDelaySlot(node, instr, delaySlotInstr)
            } else {
              node.instrs.swap(index, index + 1)
            }
          }
          else -> logger.panic(tag, "unhandled case when removing branch delay slot: $instr")
        }
      }
    }
  }

  private fun propagateBranchDelaySlot(node: Node<MipsInstr>, branchCause: MipsInstr, delaySlotInstr: MipsInstr) {
    node.instrs.remove(delaySlotInstr)
    node.outEdges.toList()
      .filter {
        if (branchCause.hasFlag(BranchLikely)) {
          // if branch cause is branch likely type then only propagate to nodes with jump taken edge type
          it.type == EdgeType.JumpTaken
        } else {
          // if this is normal branch then propagate to every child node
          true
        }
      }
      .forEach { edge ->
        if (edge.node.inEdges.size == 1) {
          edge.node.instrs.add(0, delaySlotInstr)
        } else {
          // case when child node has other input connections than origin of branch cause
          // insert new node into graph with instruction
          val newNode = Node<MipsInstr>()
          newNode.instrs.add(delaySlotInstr)
          nodes.add(newNode)
          // remove old connections and connect newNode with rest of nodes
          node.removeNode(edge)
          node.addNode(newNode, edge.type)
          newNode.addNode(edge.node, EdgeType.Fallthrough)
        }
      }
  }

  private fun classifyEdges() {
    var time = 0
    val parents = mutableMapOf<Node<MipsInstr>, Node<MipsInstr>?>()
    val startTimes = mutableMapOf<Node<MipsInstr>, Int>()
    val endTimes = mutableMapOf<Node<MipsInstr>, Int>()

    fun visit(v: Node<MipsInstr>, parent: Node<MipsInstr>? = null) {
      parents[v] = parent
      time++
      startTimes[v] = time
      parent?.setEdgeKind(v, EdgeKind.TreeEdge)
      v.outEdges.map { it.node }.forEach { n ->
        when {
          n !in parents -> visit(n, v)
          n !in endTimes -> v.setEdgeKind(n, EdgeKind.BackEdge)
          startTimes[v]!! < startTimes[n]!! -> v.setEdgeKind(n, EdgeKind.ForwardEdge)
          else -> v.setEdgeKind(n, EdgeKind.CrossEdge)
        }
      }
      time++
      endTimes[v] = time
    }

    nodes.forEach { v ->
      if (v !in parents) {
        visit(v)
      }
    }
  }

  private fun markDeadCode() {
    markDirectlyUnreachableCode()
    markUnreachableCodeAfterReturn()
  }

  private fun markDirectlyUnreachableCode() {
    nodes.forEachIndexed { index, node ->
      if (index != 0 && node.inEdges.size == 0) {
        node.instrs.forEach {
          unreachableInstrs[it.addr] = it
        }
      }
    }
  }

  private fun markUnreachableCodeAfterReturn() {
    nodes.forEach nodeLoop@{ node ->
      val returnPoint = node.instrs.indexOfFirst { it.matches(Jr, op0 = isReg(GprReg.Ra)) }
      if (returnPoint == -1) return@nodeLoop
      for (i in returnPoint + 1 until node.instrs.size) {
        val instr = node.instrs[i]
        unreachableInstrs[instr.addr] = instr
      }
    }
  }

  private fun postCheck() {
    if (nodes.any { it.instrs.size == 0 }) {
      logger.panic(tag, "graph has node with no instructions in it")
    }
  }

  fun bfsFromInstr(instr: MipsInstr, visitor: (List<MipsInstr>) -> BfsVisitorAction) {
    val startIdx = getNodeIdxForInstr(instr)
    var idx = startIdx
    val visited = BooleanArray(nodes.count())
    val queue = LinkedList<Int>()
    visited[idx] = true
    queue.add(idx)

    while (queue.size != 0) {
      idx = queue.poll()!!
      val instrToVisit = if (idx == startIdx) {
        val nodeInstrs = nodes[idx].instrs
        nodeInstrs.subList(nodeInstrs.indexOf(instr) + 1, nodeInstrs.size)
      } else {
        nodes[idx].instrs
      }
      if (visitor(instrToVisit) == BfsVisitorAction.Stop) return

      nodes[idx].outEdges.forEach { (node, _) ->
        val nodeIdx = nodes.indexOf(node)
        if (!visited[nodeIdx]) {
          visited[nodeIdx] = true
          queue.add(nodeIdx)
        }
      }
    }
  }

  fun bfs(startIdx: Int = 0, visitor: (Node<MipsInstr>) -> BfsVisitorAction) {
    var idx = startIdx
    val visited = BooleanArray(nodes.count())
    val queue = LinkedList<Int>()
    visited[idx] = true
    queue.add(idx)

    while (queue.size != 0) {
      idx = queue.poll()!!
      if (visitor(nodes[idx]) == BfsVisitorAction.Stop) return
      nodes[idx].outEdges.forEach { (node, _) ->
        val nodeIdx = nodes.indexOf(node)
        if (!visited[nodeIdx]) {
          visited[nodeIdx] = true
          queue.add(nodeIdx)
        }
      }
    }
  }

  fun getEntryNode(): Node<MipsInstr> {
    return nodes.first()
  }

  private fun getNodeForInstr(instr: MipsInstr): Node<MipsInstr> {
    return nodes.first { it.instrs.contains(instr) }
  }

  private fun getNodeIdxForInstr(instr: MipsInstr): Int {
    return nodes.indexOfFirst { it.instrs.contains(instr) }
  }

  private fun Node<MipsInstr>.getBranchCauseInstr(): MipsInstr? {
    return instrs.lastOrNull { it.hasFlag(Branch) }
  }
}

class Node<T> {
  val instrs = mutableListOf<T>()
  val inEdges = mutableListOf<Edge<T>>()
  val outEdges = mutableListOf<Edge<T>>()

  fun addNode(otherNode: Node<T>, type: EdgeType, kind: EdgeKind = EdgeKind.Unknown) {
    outEdges.add(Edge(otherNode, type, kind))
    otherNode.inEdges.add(Edge(this, type, kind))
  }

  fun removeNode(outEdge: Edge<T>) {
    val removed = outEdges.remove(outEdge)
    if (removed == false) return
    outEdge.node.inEdges.removeAll { it.node == this }
  }

  fun setEdgeKind(otherNode: Node<T>, newKind: EdgeKind) {
    outEdges.find { it.node == otherNode }?.also { outEdge ->
      outEdge.kind = newKind
      outEdge.node.inEdges.find { it.node == this }?.also { inEdge ->
        inEdge.kind = newKind
      }
    }
  }
}

class Edge<T>(val node: Node<T>, val type: EdgeType, var kind: EdgeKind = EdgeKind.Unknown) {
  operator fun component1(): Node<T> {
    return node
  }

  operator fun component2(): EdgeType {
    return type
  }

  operator fun component3(): EdgeKind {
    return kind
  }
}

enum class EdgeType {
  JumpTaken, Fallthrough
}

enum class EdgeKind {
  Unknown, TreeEdge, BackEdge, ForwardEdge, CrossEdge
}

enum class BfsVisitorAction {
  Continue, Stop
}
