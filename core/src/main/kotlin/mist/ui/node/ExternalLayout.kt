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

package mist.ui.node

import com.badlogic.gdx.math.Vector2
import kio.util.appendLine
import kio.util.child
import kio.util.execute
import kio.util.nullStreamHandler
import mist.util.*
import java.io.File

/** @author Kotcrab */

class ExternalLayout(private val layoutExe: File, private val tmpDir: File, private val log: DecompLog) {
    val tag = logTag()

    private val inFile = tmpDir.child("layout-wip.gml")
    private val outFile = tmpDir.child("layout-wip-layout.gml")

    fun layout(nodes: List<GraphNode>) {
        if (layoutExe.exists() == false) error("layout.exe is missing")
        tmpDir.mkdir()
        log.trace(tag, "writing GML")
        writeGml(nodes)
        log.trace(tag, "running external layout")
        runLayout()
        log.trace(tag, "reimporting GML")
        reimportGml(nodes)
    }

    private fun writeGml(nodes: List<GraphNode>) {
        val gml = StringBuilder()
        gml.appendLine("graph [")
        gml.appendLine("\tdirected 1")
        nodes.forEachIndexed { idx, node ->
            gml.appendLine("\tnode [")
            gml.appendLine("\t\tid $idx")
            gml.appendLine("\t\tgraphics [")
            gml.appendLine("\t\t\tx ${node.getX()}")
            gml.appendLine("\t\t\ty ${node.getY()}")
            gml.appendLine("\t\t\tw ${node.getWidth()}")
            gml.appendLine("\t\t\th ${node.getHeight()}")
            gml.appendLine("\t\t]")
            gml.appendLine("\t]")
        }
        nodes.forEach { node ->
            node.getOutEdges().forEach { childNode ->
                gml.appendLine("\tedge [")
                gml.appendLine("\t\tsource ${nodes.indexOf(node)}")
                gml.appendLine("\t\ttarget ${nodes.indexOf(childNode.node)}")
                gml.appendLine("\t]")
            }
        }
        gml.appendLine("]")
        inFile.writeText(gml.toString())
    }

    private fun runLayout() {
        execute(layoutExe, args = arrayOf(inFile, outFile), workingDirectory = tmpDir, streamHandler = nullStreamHandler())
    }

    private fun reimportGml(nodes: List<GraphNode>) {
        val graph = parseGml(outFile)
        val gNodes = getGmlNodes(graph)
        val gEdges = getGmlEdges(graph)
        gNodes.forEach { gNode ->
            val node = nodes[gNode.id]
            node.setPos(gNode.x - gNode.w / 2, -gNode.y - gNode.h / 2)
            gEdges.filter { it.source == gNode.id }.forEach nextEdge@{ gEdge ->
                if (gEdge.path == null) return@nextEdge
                val target = nodes[gEdge.target]
                node.getOutEdges().filter { it.node == target }.forEach { edge ->
                    edge.points = gEdge.path.map { point -> Vector2(point.x, -point.y) }
                }
            }
        }
    }
}
