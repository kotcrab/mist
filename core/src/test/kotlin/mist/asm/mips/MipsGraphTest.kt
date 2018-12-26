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

import kmips.Assembler
import kmips.Label
import kmips.Reg.*
import kmips.assembleAsByteArray
import mist.asm.FunctionDef
import mist.asm.Reg
import mist.asm.isReg
import mist.asm.mips.MipsOpcode.*
import mist.asm.mips.allegrex.AllegrexDisassembler
import mist.test.util.MemBinLoader
import mist.util.DecompLog
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

/** @author Kotcrab */

class MipsGraphTest {
    private val jumpOutsideCurrentFunGraph = graphFromAsm {
        // 0x0
        nop()
        j(0x100)
        nop()
        jr(ra)
        // 0x10
        nop()
    }

    private var basicJumpLoc = 0
    private var basicJumpTargetLoc = 0
    private val basicJumpGraph = graphFromAsm {
        // 0x0
        nop()
        basicJumpLoc = virtualPc
        j(0x10) // 0x4
        nop()
        nop()
        // 0x10
        basicJumpTargetLoc = virtualPc
        add(t1, t2, t3) // jump target
        nop()
        jr(ra)
        nop()
    }

    private var basicBranchLoc = 0
    private var basicBranchTargetLoc = 0
    private val basicBranchGraph = graphFromAsm {
        val target = Label()
        // 0x0
        nop()
        basicBranchLoc = virtualPc
        beq(zero, zero, target)
        nop()
        nop()
        //0x10
        label(target)
        basicBranchTargetLoc = virtualPc
        add(t1, t2, t3) // branch target
        nop()
        jr(ra)
        nop()
    }

    private var basicCondBranchLoc = 0
    private var basicCondBranchTargetLoc = 0
    private val basicCondBranchGraph = graphFromAsm {
        val target = Label()
        // 0x0
        nop()
        basicCondBranchLoc = virtualPc
        beq(a0, a1, target)
        nop()
        nop()
        //0x10
        label(target)
        basicCondBranchTargetLoc = virtualPc
        add(t1, t2, t3) // branch target
        nop()
        jr(ra)
        nop()
    }

    private val basicSwitchA0Graph = graphFromAsm(size = 0x50) {
        val postSwtich = Label()
        val case0 = Label()
        val case1 = Label()
        val case2 = Label()
        // 0x0
        sltiu(v0, s1, 0x5) // cases count (5 total, 3 unique, 1 case is to be ignored)
        beq(v0, zero, postSwtich)
        lui(v1, 0x0)
        sll(v0, s1, 0x2)
        // 0x10
        addiu(v1, v1, 0x50) // jump target table loc
        addu(v0, v0, v1)
        lw(a0, 0x0, v0)
        jr(a0)
        // 0x20
        nop()
        label(case0)
        addi(a0, zero, 0) // case 0
        b(postSwtich)
        nop()
        label(case1)
        // 0x30
        addi(a0, zero, 1)// case 1
        b(postSwtich)
        nop()
        label(case2)
        addi(a0, zero, 2) // case 2, 3
        label(postSwtich)
        // 0x40
        nop()
        nop()
        jr(ra)
        nop()
        // 0x50 switch jump table
        if (virtualPc != 0x50) error("switch table not at expected address")
        data(0x24)
        data(0x30)
        data(0x3C)
        data(0x3C)
        data(0x4000) // ignored case
    }

    private val basicSwitchAtGraph = graphFromAsm(size = 0x50) {
        val postSwtich = Label()
        val case0 = Label()
        val case1 = Label()
        val case2 = Label()

        // 0x0
        slti(a1, a0, 0x5) // cases count (5 total, 3 unique, 1 case is to be ignored)
        beq(a1, zero, postSwtich)
        nop()
        sll(a0, a0, 0x2)
        // 0x10
        lui(at, 0x0) // jump target table loc
        addu(at, at, a0)
        lw(at, 0x50, at)
        jr(at)
        // 0x20
        nop()
        label(case0)
        addi(a0, zero, 0) // case 0
        b(postSwtich)
        nop()
        label(case1)
        // 0x30
        addi(a0, zero, 1)// case 1
        b(postSwtich)
        nop()
        label(case2)
        addi(a0, zero, 2) // case 2, 3
        label(postSwtich)
        // 0x40
        nop()
        nop()
        jr(ra)
        nop()
        // 0x50 switch jump table
        if (virtualPc != 0x50) error("switch table not at expected address")
        data(0x24)
        data(0x30)
        data(0x3C)
        data(0x3C)
        data(0x4000) // ignored case
    }

    private fun graphFromAsm(size: Int = -1, assemble: Assembler.() -> Unit): MipsGraph {
        val bytes = assembleAsByteArray { assemble() }
        val loader = MemBinLoader(bytes)
        val test = AllegrexDisassembler().disassemble(loader, FunctionDef("", 0, if (size == -1) bytes.size else size))
        val graph = MipsGraph(loader, test.instr, DecompLog())
        graph.generateGraph()
        return graph
    }

    @Nested
    inner class BranchCtxTest {
        @Test
        fun `fill branch ctx should ignore jump outside current function`() {
            assertThat(jumpOutsideCurrentFunGraph.jumpingTo.size).isEqualTo(0)
            assertThat(jumpOutsideCurrentFunGraph.jumpingToFrom.size).isEqualTo(0)
        }

        @Test
        fun `fill branch ctx using jump`() {
            val (jumpSrc, jumpTargets) = basicJumpGraph.jumpingTo.toList()
                .single { it.first.addr == basicJumpLoc }
            val (jumpDest, jumpSources) = basicJumpGraph.jumpingToFrom.toList()
                .single { it.first.addr == basicJumpTargetLoc }
            assertThat(jumpSrc.opcode).isEqualTo(J)
            assertThat(jumpTargets.size).isEqualTo(1)
            assertThat(jumpTargets.first()).isEqualTo(jumpDest)
            assertThat(jumpDest.opcode).isEqualTo(Add)
            assertThat(jumpSources.size).isEqualTo(1)
            assertThat(jumpSources.first()).isEqualTo(jumpSrc)
        }

        @Test
        fun `fill branch ctx using branch`() {
            val (branchSrc, branchTargets) = basicBranchGraph.jumpingTo.toList()
                .single { it.first.addr == basicBranchLoc }
            val (branchDest, branchSources) = basicBranchGraph.jumpingToFrom.toList()
                .single { it.first.addr == basicBranchTargetLoc }
            assertThat(branchSrc.opcode).isEqualTo(Beq)
            assertThat(branchTargets.size).isEqualTo(1)
            assertThat(branchTargets.first()).isEqualTo(branchDest)
            assertThat(branchDest.opcode).isEqualTo(Add)
            assertThat(branchSources.size).isEqualTo(1)
            assertThat(branchSources.first()).isEqualTo(branchSrc)
        }

        @Test
        fun `fill branch ctx using conditional branch`() {
            val (branchSrc, branchTargets) = basicCondBranchGraph.jumpingTo.toList()
                .single { it.first.addr == basicCondBranchLoc }
            val (branchDest, branchSources) = basicCondBranchGraph.jumpingToFrom.toList()
                .single { it.first.addr == basicCondBranchTargetLoc }
            assertThat(branchSrc.opcode).isEqualTo(Beq)
            assertThat(branchTargets.size).isEqualTo(1)
            assertThat(branchTargets.first()).isEqualTo(branchDest)
            assertThat(branchDest.opcode).isEqualTo(Add)
            assertThat(branchSources.size).isEqualTo(1)
            assertThat(branchSources.first()).isEqualTo(branchSrc)
        }

        @Test
        fun `fill branch ctx using switch a0 idiom`() {
            testBranchInSwitchGraph(basicSwitchA0Graph, GprReg.A0)
        }

        @Test
        fun `fill branch ctx using switch at idiom`() {
            testBranchInSwitchGraph(basicSwitchAtGraph, GprReg.At)
        }

        private fun testBranchInSwitchGraph(graph: MipsGraph, swtichReg: Reg) {
            val (branchSrc, branchTargets) = graph.jumpingTo.toList()
                .single { it.first.matchesExact(Jr, isReg(swtichReg)) }
            assertThat(branchTargets.size).isEqualTo(3) // 3 unique cases
            assertThat(graph.switchSrcInstrs).hasSize(1)
            assertThat(graph.switchSrcInstrs).containsKey(branchSrc.addr)
            branchTargets.forEachIndexed { caseIndex, caseTarget ->
                val branchSrc2 = graph.jumpingToFrom[caseTarget]!!
                assertThat(branchSrc2.size).isEqualTo(1)
                assertThat(branchSrc2.first()).isEqualTo(branchSrc)
                val switchCaseDescriptor = graph.switchCasesInstrs[caseTarget.addr]!!
                assertThat(switchCaseDescriptor.instr).isEqualTo(caseTarget)
                assertThat(switchCaseDescriptor.cases.size).isIn(1, 2)
                assertThat(switchCaseDescriptor.cases).contains(caseIndex)
            }
        }

        @Test
        fun `fail on branch outside current function`() {
            assertThatIllegalStateException().isThrownBy {
                graphFromAsm {
                    val test = Label()
                    b(test)
                    nop()
                    label(test)
                }
            }
        }
    }

    @Nested
    inner class NodeConnectionsTest {
        @Test
        fun `ignore jump outside current function`() {
            val nodes = jumpOutsideCurrentFunGraph.nodes
            assertThat(nodes.size).isEqualTo(1)
            assertThat(nodes.first().inEdges.size).isEqualTo(0)
            assertThat(nodes.first().outEdges.size).isEqualTo(0)
        }

        @Test
        fun `ignore jump outside current function on block edge`() {
            val nodes = graphFromAsm {
                j(0x100)
                nop()
            }.nodes
            assertThat(nodes.size).isEqualTo(1)
            assertThat(nodes.first().inEdges.size).isEqualTo(0)
            assertThat(nodes.first().outEdges.size).isEqualTo(0)
        }

        @Test
        fun `fail on jal to current function`() {
            assertThatIllegalStateException().isThrownBy {
                graphFromAsm {
                    // 0x0
                    nop()
                    nop()
                    nop()
                    nop()
                    jal(0x8)
                    nop()
                }
            }
        }

        @Test
        fun `don't connect function return nodes`() {
            val graph = graphFromAsm {
                val exit2 = Label()
                // 0x0
                beq(a0, a1, exit2)
                nop()
                jr(ra)
                nop()
                // 0x10
                label(exit2)
                jr(ra)
                nop()
            }
            val nodes = graph.nodes
            assertThat(nodes.size).isEqualTo(3)
            assertThat(nodes.first().inEdges.size).isEqualTo(0)
            assertThat(nodes.first().outEdges.size).isEqualTo(2)
            val jumpTaken = nodes.first().outEdges.single { it.type == EdgeType.JumpTaken }.node
            val fallthrough = nodes.first().outEdges.single { it.type == EdgeType.Fallthrough }.node
            assertThat(jumpTaken.inEdges.size).isEqualTo(1)
            assertThat(jumpTaken.outEdges.size).isEqualTo(0)
            assertThat(jumpTaken.inEdges.first().node).isEqualTo(nodes.first())
            assertThat(fallthrough.inEdges.size).isEqualTo(1)
            assertThat(fallthrough.outEdges.size).isEqualTo(0)
            assertThat(fallthrough.inEdges.first().node).isEqualTo(nodes.first())
        }

        @Test
        fun `connect nodes on basic jump graph`() {
            testUnconditionalJumpGraph(basicJumpGraph)
        }

        @Test
        fun `connect nodes on basic branch graph`() {
            testUnconditionalJumpGraph(basicBranchGraph)
        }

        private fun testUnconditionalJumpGraph(graph: MipsGraph) {
            val nodes = graph.nodes
            assertThat(nodes.size).isEqualTo(3)
            assertThat(nodes.first().inEdges.size).isEqualTo(0)
            assertThat(nodes.first().outEdges.size).isEqualTo(1)
            val jumpTakenEdge = nodes.first().outEdges.first()
            assertThat(jumpTakenEdge.type).isEqualTo(EdgeType.JumpTaken)
            val jumpTakenNode = jumpTakenEdge.node
            assertThat(jumpTakenNode.inEdges.size).isEqualTo(2)
            assertThat(jumpTakenNode.outEdges.size).isEqualTo(0)
            val startNode = jumpTakenNode.inEdges.single { it.type == EdgeType.JumpTaken }.node
            val deadCodeNode = jumpTakenNode.inEdges.single { it.type == EdgeType.Fallthrough }.node
            assertThat(startNode).isEqualTo(nodes.first())
            assertThat(deadCodeNode.inEdges.size).isEqualTo(0)
            assertThat(deadCodeNode.outEdges.size).isEqualTo(1)
            assertThat(deadCodeNode.outEdges.first().node).isEqualTo(jumpTakenNode)
        }

        @Test
        fun `connect nodes on basic conditional branch graph`() {
            val nodes = basicCondBranchGraph.nodes
            assertThat(nodes.size).isEqualTo(3)
            assertThat(nodes.first().inEdges.size).isEqualTo(0)
            assertThat(nodes.first().outEdges.size).isEqualTo(2)
            val jumpTakenNode = nodes.first().outEdges.single { it.type == EdgeType.JumpTaken }.node
            val fallthroughNode = nodes.first().outEdges.single { it.type == EdgeType.Fallthrough }.node
            assertThat(jumpTakenNode.inEdges.size).isEqualTo(2)
            assertThat(jumpTakenNode.outEdges.size).isEqualTo(0)
            assertThat(jumpTakenNode.inEdges.single { it.type == EdgeType.JumpTaken }.node).isEqualTo(nodes.first())
            assertThat(jumpTakenNode.inEdges.single { it.type == EdgeType.Fallthrough }.node).isEqualTo(fallthroughNode)
            assertThat(fallthroughNode.inEdges.size).isEqualTo(1)
            assertThat(fallthroughNode.outEdges.size).isEqualTo(1)
            assertThat(fallthroughNode.inEdges.first().node).isEqualTo(nodes.first())
            assertThat(fallthroughNode.outEdges.first().node).isEqualTo(jumpTakenNode)
        }

        @Test
        fun `connect nodes on switch a0 idiom`() {
            testConnectionsInSwitchGraph(basicSwitchAtGraph)
        }

        @Test
        fun `connect nodes on switch at idiom`() {
            testConnectionsInSwitchGraph(basicSwitchAtGraph)
        }

        private fun testConnectionsInSwitchGraph(graph: MipsGraph) {
            assertThat(graph.nodes.size).isEqualTo(6)
            val switchNode = graph.nodes.first().outEdges.single { it.type == EdgeType.Fallthrough }.node
            val exitNode = graph.nodes.first().outEdges.single { it.type == EdgeType.JumpTaken }.node
            assertThat(exitNode.inEdges.size).isEqualTo(4) // 3 cases + exit
            assertThat(exitNode.outEdges.size).isEqualTo(0)
            assertThat(exitNode.inEdges.map { it.node }).containsAll(switchNode.outEdges.map { it.node })
            assertThat(switchNode.inEdges.size).isEqualTo(1)
            assertThat(switchNode.outEdges.size).isEqualTo(3) // 3 cases
            assertThat(switchNode.inEdges.first().node).isEqualTo(graph.nodes.first())
            switchNode.outEdges.forEach {
                assertThat(it.type).isEqualTo(EdgeType.JumpTaken)
                assertThat(it.node.inEdges.size).isEqualTo(1)
                assertThat(it.node.outEdges.size).isEqualTo(1)
                assertThat(it.node.inEdges.first().node).isEqualTo(switchNode)
                assertThat(it.node.outEdges.first().node).isEqualTo(exitNode)
            }
        }

        @Test
        fun `don't fail on jalr`() {
            val nodes = graphFromAsm {
                jalr(a0)
                nop()
                jr(ra)
                nop()
            }.nodes
            assertThat(nodes.size).isEqualTo(1)
            assertThat(nodes.first().inEdges.size).isEqualTo(0)
            assertThat(nodes.first().outEdges.size).isEqualTo(0)
        }
    }

    @Nested
    inner class DelaySlotTransformTest {
        @Test
        fun `swap j`() {
            val graph = graphFromAsm {
                j(0x8)
                add(a0, a1, a2)
                nop()
            }
            val instrs = graph.nodes.first().instrs
            assertThat(instrs[0].matches(Add)).isTrue()
            assertThat(instrs[1].matches(J)).isTrue()
        }

        @Test
        fun `swap jal`() {
            val graph = graphFromAsm {
                jal(0x100)
                add(a0, a1, a2)
                nop()
            }
            val instrs = graph.nodes.first().instrs
            assertThat(instrs[0].matches(Add)).isTrue()
            assertThat(instrs[1].matches(Jal)).isTrue()
        }

        @Test
        fun `swap jr`() {
            val graph = graphFromAsm {
                jr(t0)
                add(a0, a1, a2)
                nop()
            }
            val instrs = graph.nodes.first().instrs
            assertThat(instrs[0].matches(Add)).isTrue()
            assertThat(instrs[1].matches(Jr)).isTrue()
        }

        @Test
        fun `swap jalr`() {
            val graph = graphFromAsm {
                jalr(t0)
                add(a0, a1, a2)
                nop()
            }
            val instrs = graph.nodes.first().instrs
            assertThat(instrs[0].matches(Add)).isTrue()
            assertThat(instrs[1].matches(Jalr)).isTrue()
        }

        @Test
        fun `fail on unsafe jr swap`() {
            assertThatIllegalStateException().isThrownBy {
                graphFromAsm {
                    jr(a0)
                    add(a0, a1, a2)
                    nop()
                }
            }
        }

        @Test
        fun `fail on unsafe jalr swap`() {
            assertThatIllegalStateException().isThrownBy {
                graphFromAsm {
                    jalr(a0)
                    add(a0, a1, a2)
                    nop()
                }
            }
        }

        @Test
        fun `swap branch`() {
            val graph = graphFromAsm {
                val dest = Label()
                beq(t0, t1, dest)
                add(a0, a1, a2)
                nop()

                label(dest)
                nop()
            }
            val instrs = graph.nodes.first().instrs
            assertThat(instrs[0].matches(Add)).isTrue()
            assertThat(instrs[1].matches(Beq)).isTrue()
        }

        @Test
        fun `transform unsafe branch swap without conflicting target node`() {
            val graph = graphFromAsm {
                val dest = Label()
                val exit = Label()
                // 0x0
                beq(a0, a1, dest)
                add(a0, a1, a2)
                nop()
                b(exit)
                // 0x10
                nop()

                label(dest)
                nop()

                label(exit)
                nop()
            }
            val entry = graph.nodes.first()
            assertThat(entry.instrs.size).isEqualTo(1)
            assertThat(entry.instrs[0].matches(Beq)).isTrue()
            val jumpTaken = graph.nodes.first().outEdges.single { it.type == EdgeType.JumpTaken }.node
            val fallthrough = graph.nodes.first().outEdges.single { it.type == EdgeType.Fallthrough }.node
            assertThat(jumpTaken.instrs.size).isNotEqualTo(1)
            assertThat(jumpTaken.instrs[0].matches(Add)).isTrue()
            assertThat(fallthrough.instrs.size).isNotEqualTo(1)
            assertThat(fallthrough.instrs[0].matches(Add)).isTrue()
        }

        @Test
        fun `transform unsafe branch swap with conflicting target node`() {
            val graph = graphFromAsm {
                val dest = Label()
                beq(a0, a1, dest)
                add(a0, a1, a2)
                nop()
                label(dest)
                nop()
            }
            val entry = graph.nodes.first()
            assertThat(entry.instrs.size).isEqualTo(1)
            assertThat(entry.instrs[0].matches(Beq)).isTrue()
            val jumpTaken = graph.nodes.first().outEdges.single { it.type == EdgeType.JumpTaken }.node
            val fallthrough = graph.nodes.first().outEdges.single { it.type == EdgeType.Fallthrough }.node
            assertThat(jumpTaken.instrs.size).isEqualTo(1)
            assertThat(jumpTaken.instrs[0].matches(Add)).isTrue()
            assertThat(jumpTaken.outEdges.size).isEqualTo(1)
            assertThat(fallthrough.instrs.size).isNotEqualTo(1)
            assertThat(fallthrough.instrs[0].matches(Add)).isTrue()
            assertThat(fallthrough.outEdges.size).isEqualTo(1)
            assertThat(jumpTaken.outEdges[0].node).isEqualTo(fallthrough.outEdges[0].node)
        }

        @Test
        fun `transform branch likely without conflicting target node`() {
            val graph = graphFromAsm {
                val dest = Label()
                val exit = Label()
                // 0x0
                beql(a0, a1, dest)
                add(a0, a1, a2)
                nop()
                b(exit)
                // 0x10
                nop()

                label(dest)
                nop()

                label(exit)
                nop()
            }
            val entry = graph.nodes.first()
            assertThat(entry.instrs.size).isEqualTo(1)
            assertThat(entry.instrs[0].matches(Beql)).isTrue()
            val jumpTaken = graph.nodes.first().outEdges.single { it.type == EdgeType.JumpTaken }.node
            val fallthrough = graph.nodes.first().outEdges.single { it.type == EdgeType.Fallthrough }.node
            assertThat(jumpTaken.instrs.size).isNotEqualTo(1)
            assertThat(jumpTaken.instrs[0].matches(Add)).isTrue()
            assertThat(fallthrough.instrs.size).isNotEqualTo(1)
            assertThat(fallthrough.instrs[0].matches(Add)).isFalse()
        }

        @Test
        fun `transform branch likely with conflicting target node`() {
            val graph = graphFromAsm {
                val dest = Label()
                beql(a0, a1, dest)
                add(a0, a1, a2)
                nop()
                label(dest)
                nop()
            }
            val entry = graph.nodes.first()
            assertThat(entry.instrs.size).isEqualTo(1)
            assertThat(entry.instrs[0].matches(Beql)).isTrue()
            val jumpTaken = graph.nodes.first().outEdges.single { it.type == EdgeType.JumpTaken }.node
            val fallthrough = graph.nodes.first().outEdges.single { it.type == EdgeType.Fallthrough }.node
            assertThat(jumpTaken.instrs.size).isEqualTo(1)
            assertThat(jumpTaken.instrs[0].matches(Add)).isTrue()
            assertThat(jumpTaken.outEdges.size).isEqualTo(1)
            assertThat(fallthrough.instrs.size).isEqualTo(1)
            assertThat(fallthrough.instrs[0].matches(Add)).isFalse()
            assertThat(fallthrough.outEdges.size).isEqualTo(1)
            assertThat(jumpTaken.outEdges[0].node).isEqualTo(fallthrough.outEdges[0].node)
        }
    }

    @Nested
    inner class EdgeClassificationTest {
        @Test
        fun `classify forward edge`() {
            val jumpTaken = basicCondBranchGraph.nodes.first().outEdges.single { it.type == EdgeType.JumpTaken }
            assertThat(jumpTaken.kind).isEqualTo(EdgeKind.ForwardEdge)

        }

        @Test
        fun `classify tree edge`() {
            val fallthrough = basicCondBranchGraph.nodes.first().outEdges.single { it.type == EdgeType.Fallthrough }
            assertThat(fallthrough.kind).isEqualTo(EdgeKind.TreeEdge)
        }

        @Test
        fun `classify cross edge`() {
            val graph = graphFromAsm {
                val branch = Label()
                val cross = Label()
                val exit = Label()
                beq(a0, a1, branch)
                nop()
                b(exit)
                nop()

                label(cross)
                nop()
                b(exit)
                nop()

                label(branch)
                nop()
                b(cross)
                nop()

                label(exit)
                nop()
            }
            val jumpTaken = graph.nodes.first().outEdges.single { it.type == EdgeType.JumpTaken }.node
            assertThat(jumpTaken.outEdges.size).isEqualTo(1)
            val crossNode = jumpTaken.outEdges[0].node
            assertThat(crossNode.outEdges.size).isEqualTo(1)
            assertThat(crossNode.outEdges[0].kind).isEqualTo(EdgeKind.CrossEdge)
        }

        @Test
        fun `classify back edge`() {
            val graph = graphFromAsm {
                val loop = Label()
                label(loop)
                b(loop)
                nop()
            }
            assertThat(graph.nodes.size).isEqualTo(1)
            val node = graph.nodes.first()
            assertThat(node.outEdges.size).isEqualTo(1)
            assertThat(node.outEdges[0].kind).isEqualTo(EdgeKind.BackEdge)
        }
    }

    @Nested
    inner class UnreachableCodeTest {
        @Test
        fun `mark directly unreachable code`() {
            val deadNode = basicJumpGraph.nodes.single { it.inEdges.size == 0 && it != basicJumpGraph.getEntryNode() }
            assertThat(basicJumpGraph.unreachableInstrs.values.toList()).containsAll(deadNode.instrs)
        }

        @Test
        fun `mark unreachable code after return`() {
            val graph = graphFromAsm {
                jr(ra)
                nop()
                add(a0, a1, a2)
            }
            assertThat(graph.unreachableInstrs.size).isEqualTo(1)
            assertThat(graph.unreachableInstrs.values.first().matches(Add)).isTrue()
        }
    }

    @Nested
    inner class BfsTest {
        @Test
        fun `bfs in simple graph`() {
            val visitedNodes = mutableListOf<Int>()
            basicJumpGraph.bfs {
                visitedNodes.add(basicJumpGraph.nodes.indexOf(it))
                BfsVisitorAction.Continue
            }
            assertThat(visitedNodes).containsExactly(0, 2)
        }

        @Test
        fun `stop bfs in simple graph`() {
            val visitedNodes = mutableListOf<Int>()
            basicJumpGraph.bfs {
                visitedNodes.add(basicJumpGraph.nodes.indexOf(it))
                BfsVisitorAction.Stop
            }
            assertThat(visitedNodes).containsExactly(0)
        }

        @Test
        fun `bfs in simple switch graph`() {
            val visitedNodes = mutableListOf<Int>()
            basicSwitchA0Graph.bfs {
                visitedNodes.add(basicSwitchA0Graph.nodes.indexOf(it))
                BfsVisitorAction.Continue
            }
            assertThat(visitedNodes).containsExactly(0, 1, 5, 2, 3, 4)
        }

        @Test
        fun `bfs from instr in simple graph`() {
            val visitedNodes = mutableListOf<Int>()
            basicJumpGraph.bfsFromInstr(basicJumpGraph.getEntryNode().instrs[1]) { instr ->
                visitedNodes.add(basicJumpGraph.nodes.indexOfFirst { it.instrs.contains(instr.first()) })
                BfsVisitorAction.Continue
            }
            assertThat(visitedNodes).containsExactly(0, 2)
        }
    }

    @Test
    fun `get entry node test`() {
        assertThat(basicJumpGraph.getEntryNode()).isEqualTo(basicJumpGraph.nodes.first())
        assertThat(basicBranchGraph.getEntryNode()).isEqualTo(basicBranchGraph.nodes.first())
        assertThat(basicSwitchA0Graph.getEntryNode()).isEqualTo(basicSwitchA0Graph.nodes.first())
        assertThat(basicSwitchAtGraph.getEntryNode()).isEqualTo(basicSwitchAtGraph.nodes.first())
    }

    @Nested
    inner class NodeTest {
        @Test
        fun `add node`() {
            val node = Node<Int>()
            val node2 = Node<Int>()
            node.addNode(node2, type = EdgeType.Fallthrough, kind = EdgeKind.ForwardEdge)
            assertThat(node.outEdges.first().node).isEqualTo(node2)
            assertThat(node.outEdges.first().type).isEqualTo(EdgeType.Fallthrough)
            assertThat(node.outEdges.first().kind).isEqualTo(EdgeKind.ForwardEdge)
            assertThat(node.inEdges).isEmpty()
            assertThat(node2.inEdges.first().node).isEqualTo(node)
            assertThat(node2.inEdges.first().type).isEqualTo(EdgeType.Fallthrough)
            assertThat(node2.inEdges.first().kind).isEqualTo(EdgeKind.ForwardEdge)
            assertThat(node2.outEdges).isEmpty()
        }

        @Test
        fun `remove node`() {
            val node = Node<Int>()
            val node2 = Node<Int>()
            val node3 = Node<Int>()
            node.addNode(node2, type = EdgeType.Fallthrough, kind = EdgeKind.ForwardEdge)
            node.addNode(node3, type = EdgeType.Fallthrough, kind = EdgeKind.ForwardEdge)
            node.removeNode(node.outEdges.first { it.node == node2 })
            assertThat(node.outEdges.filter { it.node == node2 }).isEmpty()
            assertThat(node.inEdges).isEmpty()
            assertThat(node2.inEdges).isEmpty()
            assertThat(node2.outEdges).isEmpty()
        }

        @Test
        fun `set edge kind`() {
            val node = Node<Int>()
            val node2 = Node<Int>()
            node.addNode(node2, type = EdgeType.Fallthrough, kind = EdgeKind.ForwardEdge)
            assertThat(node.outEdges.first().kind).isEqualTo(EdgeKind.ForwardEdge)
            assertThat(node2.inEdges.first().kind).isEqualTo(EdgeKind.ForwardEdge)
            node.setEdgeKind(node2, newKind = EdgeKind.BackEdge)
            assertThat(node.outEdges.first().kind).isEqualTo(EdgeKind.BackEdge)
            assertThat(node2.inEdges.first().kind).isEqualTo(EdgeKind.BackEdge)
        }

        @Test
        fun `destruct edge`() {
            val node = Node<Int>()
            val edge = Edge(node, EdgeType.Fallthrough, EdgeKind.ForwardEdge)
            val (node2, type, kind) = edge
            assertThat(node2).isEqualTo(node)
            assertThat(type).isEqualTo(EdgeType.Fallthrough)
            assertThat(kind).isEqualTo(EdgeKind.ForwardEdge)
        }
    }
}
