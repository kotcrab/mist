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

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.kotcrab.vis.ui.widget.PopupMenu
import ktx.actors.onChange
import ktx.async.ktxAsync
import ktx.inject.Context
import ktx.vis.menuItem
import ktx.vis.table
import mist.TabProvidesMenu
import mist.asm.EdgeKind
import mist.asm.EdgeType
import mist.io.AsmFunctionIO
import mist.io.LoadedFunc
import mist.io.LocalHistoryEntry
import mist.shl.*
import mist.ui.dialog.LocalHistoryDialog
import mist.ui.util.*
import mist.util.logTag

/** @author Kotcrab */

class GraphTab(context: Context,
               private val asmFunctionIO: AsmFunctionIO,
               val def: ShlFunctionDef,
               val onFuncDoubleClick: (ShlFunctionDef) -> Unit)
    : VisualNodeTab<GraphNode>(context, closeable = true), ShlDefsChanged, TabProvidesMenu {
    private val tag = logTag()
    private val layout = projectIO.provideLayout()
    private val remoteDebugger: RemoteDebugger = context.inject()

    private lateinit var graph: ShlGraph
    private lateinit var dataFlowAnalysis: DataFlowAnalysis
    private lateinit var exprMutator: ExprMutator

    private val codeNodeListener = object : GraphNode.Listener {
        override fun init() {
            initNodes(centerCamera = false, reInitFlow = true, markDirty = true)
        }

        override fun onHighlight(expr: String) {
            nodeList.forEach {
                it.highlightVar(expr)
            }
        }
    }

    private val remoteDbgListener = object : RemoteDebuggerListener {
        override fun handleMessage(msg: PpssppMessage) {
            when (msg) {
                is BreakpointPausedEvent -> {
                    nodeList.forEach { it.showBreakpoint(msg.addr - 0x8804000) }
                }
                is RunningSetResponse -> {
                    nodeList.forEach { it.hideBreakpoint() }
                }
            }
        }
    }

    init {
        camera.zoom = 1.7f
        loadFunction()
        remoteDebugger.addListener(remoteDbgListener)
    }

    private fun loadFunction(ignoreSaved: Boolean = false) {
        val func = asmFunctionIO.load(ignoreSaved)
        graph = func.graph
        dataFlowAnalysis = DataFlowAnalysis(graph, log)
        exprMutator = ExprMutator(graph, log)
        initNodes(centerCamera = true, reInitFlow = true, markDirty = false)
        func.layoutData?.forEachIndexed { nodeIdx, pos ->
            nodeList[nodeIdx].setPos(pos.x, pos.y)
        }
    }

    override fun createDelegationTable() = table {
        table {
            // this gives event priority to node stage
            it.grow()
            touchable = Touchable.enabled
            addListener(StageEventDelegate(nodeStage))
        }
        touchable = Touchable.enabled
        addListener(rectangularSelection)
        addListener(CameraScrollInput())
        addListener(CameraMovement())
    }

    override fun save(): Boolean {
        asmFunctionIO.save(LoadedFunc(graph, nodeList.map { Vector2(it.getX(), it.getY()) }))
        isDirty = false
        return true
    }

    override fun dispose() {
        log.info(tag, "dispose $tag")
        remoteDebugger.remoteListener(remoteDbgListener)
        super.dispose()
        asmFunctionIO.dispose()
    }

    private fun initNodes(centerCamera: Boolean, reInitFlow: Boolean, markDirty: Boolean) {
        nodeStage.clear()
        nodeList.clear()
        selectedNodes.clear()
        graph.nodes.forEachIndexed { index, node ->
            nodeList.add(GraphNode(context, nodeStage, exprMutator, 0f, 0f, node, index, codeNodeListener,
                    if (index == 0) def else null, onFuncDoubleClick))
        }
        graph.nodes.forEachIndexed { idx, node ->
            node.outEdges.forEach { edge ->
                val otherNode = nodeList[graph.nodes.indexOf(edge.node)]
                nodeList[idx].addNode(otherNode, edge.type, edge.kind)
            }
        }
        layout.layout(nodeList)
        if (reInitFlow) {
            dataFlowAnalysis.analyze()
        }
        if (centerCamera) {
            val node = nodeList[0]
            val rect = Rectangle(node.getX(), node.getY(), node.getWidth(), node.getHeight())
            camera.position.x = rect.getX() + rect.getWidth() / 2
            camera.position.y = rect.getY() + rect.getHeight() / 2
        }
        if (markDirty) {
            dirty()
        }
    }

    override fun shlDefsChanged(type: ShlDefsChangeType) {
        when (type) {
            ShlDefsChangeType.FunctionDefs -> {
                initNodes(centerCamera = false, reInitFlow = true, markDirty = false)
                pane?.updateTabTitle(this)
            }
            ShlDefsChangeType.Types -> {
                initNodes(centerCamera = false, reInitFlow = true, markDirty = false)
            }
        }
    }

    override fun fillMenu(popupMenu: PopupMenu) {
        popupMenu.menuItem("Local History") {
            onChange {
                appStage.addActor(LocalHistoryDialog(asmFunctionIO.getLocalHistoryEntries(), object : WindowResultListener<LocalHistoryEntry> {
                    override fun finished(result: LocalHistoryEntry) {
                        asmFunctionIO.revert(result)
                        loadFunction()
                        dirty()
                    }
                }).fadeIn())
            }
        }
        popupMenu.menuItem("Discard all changes") {
            onChange {
                loadFunction(ignoreSaved = true)
                dirty()
            }
        }
        popupMenu.addSeparator()
        popupMenu.menuItem("Debug this") {
            onChange {
                val client = remoteDebugger.getClient() ?: return@onChange
                ktxAsync {
                    nodeList.forEach { node ->
                        node.node.instrs.forEach { instr ->
                            client.addBreakpoint(instr.addr + 0x8804000, false)
                        }
                    }
                }
            }
        }
        popupMenu.menuItem("Stop debugging this") {
            onChange {
                val client = remoteDebugger.getClient() ?: return@onChange
                ktxAsync {
                    nodeList.forEach { node ->
                        node.node.instrs.forEach { instr ->
                            client.removeBreakpoint(instr.addr + 0x8804000)
                        }
                    }
                }
            }
        }
        popupMenu.menuItem("Step") {
            onChange {
                ktxAsync {
                    remoteDebugger.getClient()?.setRunning(true)
                }
            }
        }
    }

    override fun getTabTitle(): String {
        return "Graph - \"${def.name}\""
    }

    override fun renderDebugText(debugTextRenderer: DebugTextRenderer) {
        debugTextRenderer.drawLine("FPS: ${Gdx.graphics.framesPerSecond}")
        debugTextRenderer.drawLine("Nodes: ${this.nodeList.size}")
        debugTextRenderer.drawLine("Selected nodes: ${selectedNodes.size}")
    }

    override fun renderConnections(shapeRenderer: ShapeRenderer, node: GraphNode) {
        node.getOutEdges().forEach edgeDraw@{ edge ->
            val x1 = node.getOutConX() + 3
            val y1 = node.getOutConY() + 3
            val x2 = edge.node.getInConX() + 3
            val y2 = edge.node.getInConY() + 3 + 8
            val startX = if (x2 > x1) x1 else x2
            val startY = if (y2 > y1) y1 else y2
            if (cameraFrustum.overlaps(Rectangle(startX, startY, Math.abs(x2 - x1), Math.abs(y2 - y1))) == false) {
                return@edgeDraw
            }
            shapeRenderer.color = getEdgeColor(node, edge)

            var points = edge.points
            if (node == edge.node && points == null) {
                val offset = 20
                points = listOf(
                        Vector2(0f, 0f),
                        Vector2(x1, y1 - offset),
                        Vector2(x1 - node.getWidth() / 2 - offset, y1 - offset),
                        Vector2(x1 - node.getWidth() / 2 - offset, y2 + offset),
                        Vector2(x1, y2 + offset),
                        Vector2(0f, 0f)
                )
            }

            if (points == null || points.size < 3) {
                shapeRenderer.line(x1, y1, x2, y2)
            } else {
                shapeRenderer.line(x1, y1, points[1].x, points[1].y)
                for (i in 2..points.size - 2) {
                    shapeRenderer.line(points[i - 1].x, points[i - 1].y, points[i].x, points[i].y)
                }
                shapeRenderer.line(points[points.lastIndex - 1].x, points[points.lastIndex - 1].y, x2, y2)
            }
        }
    }

    override fun shouldRenderConnectionTriangles(): Boolean {
        return true
    }

    override fun renderConnectionTriangles(shapeRenderer: ShapeRenderer, node: GraphNode) {
        node.getOutEdges().forEach triangDraw@{ edge ->
            val x2 = edge.node.getInConX() + 3
            val y2 = edge.node.getInConY() + 3
            if (cameraFrustum.overlaps(Rectangle(x2 - 8, y2 + 8, 16f, 16f)) == false) {
                return@triangDraw
            }
            shapeRenderer.color = getEdgeColor(node, edge)
            shapeRenderer.triangle(x2 - 8, y2 + 8, x2, y2, x2 + 8, y2 + 8)
        }
    }

    private fun getEdgeColor(node: GraphNode, edge: CodeNodeEdge): Color = when (edge.edgeType) {
        EdgeType.JumpTaken -> {
            when (edge.edgeKind) {
                EdgeKind.BackEdge -> Color.GOLD.cpy()
                else -> Color.GREEN.cpy()
            }
        }
        EdgeType.Fallthrough -> {
            when (node.getOutEdges().size) {
                2 -> {
                    when (edge.edgeKind) {
                        EdgeKind.BackEdge -> Color.MAGENTA.cpy()
                        else -> Color.RED.cpy()
                    }
                }
                else -> {
                    when (edge.edgeKind) {
                        EdgeKind.BackEdge -> Color.GRAY.cpy()
                        else -> Color.WHITE.cpy()
                    }
                }
            }
        }
    }

}
