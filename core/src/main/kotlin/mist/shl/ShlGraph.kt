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

package mist.shl

import mist.io.NodeDto
import mist.io.ProjectIO
import mist.asm.mips.*
import mist.util.DecompLog
import mist.util.logTag
import java.util.*

/** @author Kotcrab */

class ShlGraph(private val projectIO: ProjectIO, private val def: ShlFunctionDef, val log: DecompLog) {
    val tag = logTag()
    lateinit var nodes: MutableList<Node<ShlInstr>>
        private set

    private val lifter = ShlLifter(projectIO, log)

    fun generateGraph(graph: Graph, stack: StackAnalysis) {
        copyGraphStructure(graph, stack)
    }

    fun loadGraph(nodesDto: List<NodeDto>) {
        nodes = MutableList(nodesDto.size) { Node<ShlInstr>() }
        nodesDto.forEachIndexed { nodeIdx, nodeDto ->
            val node = nodes[nodeIdx]
            node.instrs.addAll(nodeDto.instr)
            nodeDto.outEdges.forEach { edgeDto ->
                node.addNode(nodes[edgeDto.node], edgeDto.type, edgeDto.kind)
            }
        }
    }

    private fun copyGraphStructure(graph: Graph, stack: StackAnalysis) {
        nodes = MutableList(graph.nodes.size) { Node<ShlInstr>() }
        graph.nodes.forEachIndexed { nodeIdx, node ->
            val shlNode = nodes[nodeIdx]
            if (node == graph.getEntryNode()) {
                addEntryArgsAssigns(def, shlNode.instrs)
            }
            shlNode.instrs.addAll(lifter.lift(node.instrs))
            // add comments with ascii conversion if possible
            shlNode.instrs.forEach {
                if (it is ShlAssignInstr && it.src is ShlExpr.ShlConst) {
                    val value = (it.src as ShlExpr.ShlConst).value
                    if (value in 0x20..0x7E) {
                        it.comment += "\"${value.toChar()}\""
                    }
                }
            }
            // apply dead code on other properties from disassembly
            shlNode.instrs.filter { graph.unreachableInstrs.contains(it.addr) || stack.framePreserve.contains(it.addr) }
                .forEach { it.deadCode = true }
            applySwitchInfo(graph, shlNode)
            // connect nodes
            node.outEdges.forEach { outNode ->
                val outNodeIndex = graph.nodes.indexOf(outNode.node)
                shlNode.addNode(nodes[outNodeIndex], outNode.type, outNode.kind)
            }
        }
    }

    private fun addEntryArgsAssigns(def: ShlFunctionDef, instrs: MutableList<ShlInstr>) {
        val freeArgs = shlArgRegisters.toMutableList()
        def.arguments.forEach { arg ->
            if (freeArgs.remove(arg.register) == false) {
                log.panic(tag, "conflicting func. arg definitions (free arg list exhausted)")
            }
            if (arg.type.endsWith("...")) { // if arg is vararg
                instrs.add(ShlAssignInstr(0x0, ShlExpr.ShlVar(arg.register), ShlExpr.ShlVar("${arg.name}[0]")))
                freeArgs.forEachIndexed { freeArgIdx, freeArg ->
                    instrs.add(
                        ShlAssignInstr(
                            0x0,
                            ShlExpr.ShlVar(freeArg),
                            ShlExpr.ShlVar("${arg.name}[${freeArgIdx + 1}]")
                        )
                    )
                }
                freeArgs.clear()
            } else {
                instrs.add(ShlAssignInstr(0x0, ShlExpr.ShlVar(arg.register), ShlExpr.ShlVar(arg.name)))
            }
        }
    }

    private fun applySwitchInfo(graph: Graph, shlNode: Node<ShlInstr>) {
        for (instr in shlNode.instrs) {
            val switchDes = graph.switchSrcInstrs[instr.addr]
            if (switchDes != null) {
                instr.comment += "switch ${switchDes.switchCaseCount} cases"
                break
            }
        }

        for (instr in shlNode.instrs) {
            val switchCases = graph.switchCasesInstrs[instr.addr]
            if (switchCases != null) {
                // prefer using first instruction for adding comment to avoid branch delay slot problems
                shlNode.instrs.first().comment += "case ${switchCases.cases.joinToString()}"
                break
            }
        }
    }

    @Deprecated("Likely to be removed") //TODO consider removing this
    fun bfsFromInstr(instr: ShlInstr, visitor: (Node<ShlInstr>, List<ShlInstr>) -> BfsVisitorAction) {
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
            if (instrToVisit.isNotEmpty()) {
                if (visitor(nodes[idx], instrToVisit) == BfsVisitorAction.Stop) return
            }

            nodes[idx].outEdges.forEach { (node, _) ->
                val nodeIdx = nodes.indexOf(node)
                if (!visited[nodeIdx]) {
                    visited[nodeIdx] = true
                    queue.add(nodeIdx)
                }
            }
        }
    }

    fun removeNode(node: Node<ShlInstr>) {
        if (node.inEdges.size != 0) log.panic(tag, "can't remove node which has input edges")
        log.info(tag, "removing node ${nodes.indexOf(node)}")
        val success = nodes.remove(node)
        if (!success) return
        node.outEdges.toTypedArray().forEach {
            node.removeNode(it)
        }
    }

    fun optimizeNodes() {
        while (true) {
            val toOptimize = getNodeToOptimize() ?: break
            val childEdge = toOptimize.outEdges[0]
            toOptimize.removeNode(childEdge)

            val prevParents = toOptimize.inEdges.toTypedArray()
            prevParents.forEach { parentEdge ->
                parentEdge.node.outEdges
                    .filter { it.node == toOptimize }
                    .forEach { parentEdge.node.removeNode(it) }
            }

            prevParents.forEach { parentEdge ->
                parentEdge.node.addNode(childEdge.node, parentEdge.type, parentEdge.kind)
            }
            removeNode(toOptimize)
        }
        nodes.forEach { node ->
            node.instrs.removeIf { it.deadCode }
            if (node.instrs.size == 0) {
                node.instrs.add(ShlNopInstr(0x0))
            }
        }
    }

    private fun getNodeToOptimize(): Node<ShlInstr>? {
        return nodes.firstOrNull { node ->
            node.instrs.all { it.deadCode }
                    && node.inEdges.all { it.kind != EdgeKind.BackEdge }
                    && node.outEdges.size == 1 && node.outEdges[0].kind != EdgeKind.BackEdge
        }
    }

    fun getEntryNode(): Node<ShlInstr> {
        return nodes.first()
    }

    fun getExitNodes(): List<Node<ShlInstr>> {
        return nodes.filter { it.outEdges.size == 0 }
            .filter { it ->
                val lastInstr = it.instrs.last()
                lastInstr is ShlJumpInstr && lastInstr.dest.compareExpr(ShlExpr.ShlVar("ra"))
            }
    }

    fun getNodeForInstr(instr: ShlInstr): Node<ShlInstr> {
        return nodes.first { it.instrs.contains(instr) }
    }

    fun getNodeIdxForInstr(instr: ShlInstr): Int {
        return nodes.indexOfFirst { it.instrs.contains(instr) }
    }

    fun getInstr(ref: InstrRef): ShlInstr {
        return nodes[ref.nodeIdx].instrs[ref.instrIdx]
    }
}

data class InstrRef(val nodeIdx: Int, val instrIdx: Int) {
    override fun toString(): String {
        return "InstrRef($nodeIdx, $instrIdx)"
    }
}
