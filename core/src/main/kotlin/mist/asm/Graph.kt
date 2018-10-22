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

import kio.util.toWHex
import mist.io.BinLoader
import mist.util.DecompLog
import mist.util.logTag
import mist.util.swap
import java.util.*

/** @author Kotcrab */

class Graph(private val loader: BinLoader,
            private val instrs: List<Instr>,
            private val log: DecompLog) {
    private val tag = logTag()

    private val switchAtRegIdiom = SwitchAtRegIdiom()
    private val switchA0RegIdiom = SwitchA0RegIdiom()

    val nodes = mutableListOf<Node<Instr>>()

    // maps jump cause to instruction that it is jumping to
    val jumpingTo = mutableMapOf<Instr, MutableSet<Instr>>()
    // maps jump target to instruction that jumps to it
    val jumpingToFrom = mutableMapOf<Instr, MutableSet<Instr>>()

    // maps unreachable instruction address to instruction at that address
    val unreachableInstrs = mutableMapOf<Int, Instr>()
    // maps `jt at` and `jr a0` instructions to their respective switch metadata
    val switchSrcInstrs = mutableMapOf<Int, SwitchDescriptor>()
    // maps instruction at specified address to descriptor with switch cases list
    val switchCasesInstrs = mutableMapOf<Int, SwitchCaseDescriptor>()
    // list of instructions that are in branch delay slots
    val branchDelaySlotInstrs = mutableMapOf<Int, Instr>()

    fun generateGraph() {
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
                srcInstr.isBranch() -> {
                    val destAddr = srcInstr.addr + 0x4 + srcInstr.getLastImm() * 0x4
                    val destInstr = instrs.firstOrNull { it.addr == destAddr }
                            ?: log.panic(tag, "branch to instruction outside current function")
                    jumpingTo.getOrPut(srcInstr, defaultValue = { mutableSetOf() }).add(destInstr)
                    jumpingToFrom.getOrPut(destInstr, defaultValue = { mutableSetOf() }).add(srcInstr)
                }
                srcInstr.matches(Opcode.J) -> {
                    val destInstr = instrs.firstOrNull { it.addr == srcInstr.op1AsImm() }
                    if (destInstr != null) { // if jump within function
                        jumpingTo.getOrPut(srcInstr, defaultValue = { mutableSetOf() }).add(destInstr)
                        jumpingToFrom.getOrPut(destInstr, defaultValue = { mutableSetOf() }).add(srcInstr)
                    }
                }
                // switch case idiom detection
                srcInstr.matches(Opcode.Jr, op1 = isReg(Reg.at)) -> {
                    switchAtRegIdiom.matches(instrs, srcInstrIdx)?.let { handleSwitch(it) }
                }
                srcInstr.matches(Opcode.Jr, op1 = isReg(Reg.a0)) -> {
                    switchA0RegIdiom.matches(instrs, srcInstrIdx)?.let { handleSwitch(it) }
                }
            }
        }
    }

    private fun handleSwitch(result: SwitchDescriptor) {
        log.trace(tag, "switch ${result.switchCaseCount} cases, jump table at ${result.jumpTableLoc.toWHex()}")
        val srcInstr = result.relInstrs.first()
        switchSrcInstrs[srcInstr.addr] = result
        repeat(result.switchCaseCount) { case ->
            val caseTargetAddr = loader.readInt(result.jumpTableLoc + case * 0x4)
            val destInstr = instrs.firstOrNull { it.addr == caseTargetAddr }
            if (destInstr == null) {
                log.warn(tag, "switch case $case jumps to instruction outside current function and will be ignored, " +
                        "jump target: ${caseTargetAddr.toWHex()}")
                return@repeat
            }
            jumpingTo.getOrPut(srcInstr, defaultValue = { mutableSetOf() }).add(destInstr)
            jumpingToFrom.getOrPut(destInstr, defaultValue = { mutableSetOf() }).add(srcInstr)
            switchCasesInstrs.getOrPut(destInstr.addr, defaultValue = { SwitchCaseDescriptor(destInstr) }).cases.add(case)
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
                    jumpCause.opcode == Opcode.J -> { // unconditional jump
                        val jumpTargetInstr = instrs.firstOrNull { it.addr == jumpCause.op1AsImm() }
                        if (jumpTargetInstr != null) {
                            linkJumpToNode(node, jumpTargetInstr)
                            return@forEach
                        } else {
                            // jumping outside context of current function, ignored
                            // important: connecting this node with fallthrough edge in handled bellow
                            log.warn(tag, "unconditional jump outside of current context will be ignored")
                        }
                    }
                    jumpCause.opcode == Opcode.Jal -> { // most likely call to other function
                        val jumpingToInstr = instrs.firstOrNull { it.addr == jumpCause.op1AsImm() }
                        if (jumpingToInstr != null) {
                            log.panic(tag, "jal (jump and link) to instruction inside current function")
                        }
                    }
                    jumpCause.matches(Opcode.Jr, isReg(Reg.ra)) -> { // function exit
                        return@forEach
                    }
                    switchSrcInstrs.contains(jumpCause.addr) -> {
                        jumpingTo[jumpCause]?.forEach { jumpTargetInstr ->
                            linkJumpToNode(node, jumpTargetInstr)
                        }
                        return@forEach
                    }
                    jumpCause.opcode == Opcode.Jalr -> {
                        log.warn(tag, "function uses jalr (if jumping within current function graph will be invalid)")
                    }
                    jumpCause.isJump() -> log.panic(tag, "unhandled jump type instruction: $jumpCause")
                }
            }

            // this will handle "b label" MIPS instruction alias
            // and in general `beq reg1, reg2 label` where reg1 == reg2
            // it should be enough for compiled code
            fun Instr.isUnconditionalBranch() = (opcode == Opcode.Beq
                    && op1 is Operand.Reg && op2 is Operand.Reg && op1.reg == op2.reg)

            val branchCause = node.getBranchCauseInstr()

            // handle fall node
            if (branchCause == null || branchCause.isUnconditionalBranch() == false) {
                val fallToInstr = instrs.indexOf(node.instrs.last()) + 1
                if (fallToInstr < instrs.size) {
                    val otherNode = getNodeForInstr(instrs[fallToInstr])
                    node.addNode(otherNode, EdgeType.Fallthrough)
                } else {
                    log.warn(tag, "unexpected end of function")
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
                    log.panic(tag, "branch instruction '$branchCause' does not define any branch target, check control flow context " +
                            "generation for mistakes")
                }
            }
        }
    }

    private fun linkJumpToNode(srcNode: Node<Instr>, jumpTargetInstr: Instr) {
        val destNode = nodes.first { it.instrs.contains(jumpTargetInstr) }
        val destInNodeIdx = destNode.instrs.indexOf(jumpTargetInstr)
        if (destInNodeIdx != 0) {
            log.panic(tag, "jump to instruction that is not first in the node, check control flow context " +
                    "generation for mistakes")
        }
        srcNode.addNode(destNode, EdgeType.JumpTaken)
    }

    private fun transformBranchDelaySlot() {
        nodes.toList().forEach { node ->
            node.instrs.toMutableList().forEachIndexed { index, instr ->
                if (instr.opcode.type != Opcode.Type.ControlFlow) return@forEachIndexed
                when {
                    instr.opcode in arrayOf(Opcode.J, Opcode.Jal) -> {
                        // jal and j should be always safe to swap
                        // they don't use register so there is no risk of
                        // swapped instruction modifying some source register
                        val delaySlotInstr = node.instrs[index + 1]
                        branchDelaySlotInstrs[delaySlotInstr.addr] = delaySlotInstr
                        node.instrs.swap(index, index + 1)
                    }
                    instr.opcode in arrayOf(Opcode.Jr, Opcode.Jalr) -> {
                        val delaySlotInstr = node.instrs[index + 1]
                        branchDelaySlotInstrs[delaySlotInstr.addr] = delaySlotInstr
                        if (delaySlotInstr.getModifiedRegisters().any { instr.getSourceRegisters().contains(it) }) {
                            log.panic(tag, "${instr.opcode.toString().toLowerCase()} instruction not safe to swap, " +
                                    "instruction: $delaySlotInstr")
                        }
                        node.instrs.swap(index, index + 1)
                    }
                    instr.isBranchLikely() -> {
                        val delaySlotInstr = node.instrs[index + 1]
                        branchDelaySlotInstrs[delaySlotInstr.addr] = delaySlotInstr
                        propagateBranchDelaySlot(node, instr, delaySlotInstr)
                    }
                    instr.isBranch() -> {
                        val delaySlotInstr = node.instrs[index + 1]
                        branchDelaySlotInstrs[delaySlotInstr.addr] = delaySlotInstr
                        if (delaySlotInstr.getModifiedRegisters().any { instr.getSourceRegisters().contains(it) }) {
                            // fallback to propagation if swap not safe
                            propagateBranchDelaySlot(node, instr, delaySlotInstr)
                        } else {
                            node.instrs.swap(index, index + 1)
                        }
                    }
                    else -> log.panic(tag, "unhandled case when removing branch delay slot: $instr")
                }
            }
        }
    }

    private fun propagateBranchDelaySlot(node: Node<Instr>, branchCause: Instr, delaySlotInstr: Instr) {
        node.instrs.remove(delaySlotInstr)
        node.outEdges.toList()
                .filter {
                    if (branchCause.isBranchLikely()) {
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
                        val newNode = Node<Instr>()
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
        val parents = mutableMapOf<Node<Instr>, Node<Instr>?>()
        val startTimes = mutableMapOf<Node<Instr>, Int>()
        val endTimes = mutableMapOf<Node<Instr>, Int>()

        fun visit(v: Node<Instr>, parent: Node<Instr>? = null) {
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
            val returnPoint = node.instrs.indexOfFirst { it.matches(Opcode.Jr, isReg(Reg.ra)) }
            if (returnPoint == -1) return@nodeLoop
            for (i in returnPoint + 1 until node.instrs.size) {
                val instr = node.instrs[i]
                unreachableInstrs[instr.addr] = instr
            }
        }
    }

    private fun postCheck() {
        if (nodes.any { it.instrs.size == 0 }) {
            log.panic(tag, "graph has node with no instructions in it")
        }
    }

    fun bfsFromInstr(instr: Instr, visitor: (List<Instr>) -> BfsVisitorAction) {
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

    fun bfs(startIdx: Int = 0, visitor: (Node<Instr>) -> BfsVisitorAction) {
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

    fun getEntryNode(): Node<Instr> {
        return nodes.first()
    }

    private fun getNodeForInstr(instr: Instr): Node<Instr> {
        return nodes.first { it.instrs.contains(instr) }
    }

    private fun getNodeIdxForInstr(instr: Instr): Int {
        return nodes.indexOfFirst { it.instrs.contains(instr) }
    }

    private fun Node<Instr>.getBranchCauseInstr(): Instr? {
        return instrs.lastOrNull { it.isBranch() }
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
