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
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter
import com.kotcrab.vis.ui.widget.PopupMenu
import kio.util.toHex
import ktx.actors.onChange
import ktx.inject.Context
import ktx.vis.menuItem
import ktx.vis.popupMenu
import ktx.vis.subMenu
import ktx.vis.table
import mist.shl.ShlDefsChangeType
import mist.shl.ShlDefsChanged
import mist.shl.ShlFunctionDef
import mist.ui.util.StageEventDelegate
import mist.util.logTag
import java.util.*

/** @author Kotcrab */

class FlowTab(context: Context, disassembleFuncCallback: (String) -> Unit, editFuncCallback: (String) -> Unit) :
    VisualNodeTab<FlowNode>(context), ShlDefsChanged {
    private val tag = logTag()
    private val apis = mutableListOf<FlowApiSet>()

    init {
        camera.zoom = 1.5f
        load()
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
        addListener(FlowInput())
    }

    private fun load() {
        nodeList.clear()
        selectedNodes.clear()
        val nodeDto: List<FlowNodeDto> = projectIO.loadFlowGraph()
        val nodeMap = mutableMapOf<Int, FlowNode>()
        nodeDto.forEach {
            val funcDef = projectIO.getFuncDefByOffset(it.fileOffset)!!
            val node = FlowNode(context, nodeStage, it.id, funcDef.name, it.fileOffset, Color(it.color), it.x, it.y)
            node.apiName = it.apiName
            node.reversed = funcDef.reversed
            nodeList.add(node)
            nodeMap[node.id] = node
        }
        nodeDto.forEach { dto ->
            val node = nodeMap[dto.id]!!
            dto.outEdges.forEach { targetId ->
                node.addNode(nodeMap[targetId]!!)
            }
        }
        nodeList.forEach { it.updateLabels() }
        apis.clear()
        apis.addAll(projectIO.loadFlowApis())
    }

    override fun save(): Boolean {
        projectIO.saveFlowGraph(nodeList.map { FlowNodeDto(it) })
        isDirty = false
        return true
    }

    override fun dispose() {
        log.info(tag, "dispose $tag")
        super.dispose()
    }

    override fun shlDefsChanged(type: ShlDefsChangeType) {
        when (type) {
            ShlDefsChangeType.FunctionDefs -> {
                updateFromProjectFuncDefs()
            }
            ShlDefsChangeType.Types -> {
                // nothing to update
            }
        }
    }

    private fun updateFromProjectFuncDefs() {
        projectIO.getFuncs().forEach { def ->
            nodeList.firstOrNull { it.fileOffset == def.offset }?.run {
                if (funcName != def.name) {
                    funcName = def.name
                }
                if (reversed != def.reversed) {
                    reversed = def.reversed
                }
            }
        }
    }

    private fun markSelectedAsApi(newApiName: String, skipCheck: Boolean = false) {
        if (skipCheck == false && selectedNodes.size > 1) {
            Dialogs.showOptionDialog(appStage, "Warning", "This will mark ${selectedNodes.size} nodes as '$newApiName'",
                Dialogs.OptionDialogType.YES_CANCEL, object : OptionDialogAdapter() {
                    override fun yes() {
                        markSelectedAsApi(newApiName, true)
                    }
                })
        } else {
            selectedNodes.forEach {
                it.apiName = newApiName
            }
            nodeList.forEach { it.updateLabels() }
            dirty()
        }
    }

    fun centerAround(def: ShlFunctionDef) {
        nodeList.firstOrNull { it.funcName == def.name }?.let { node ->
            selectedNodes.clear()
            val rect = Rectangle(node.getX(), node.getY(), node.getWidth(), node.getHeight())
            camera.position.x = rect.getX() + rect.getWidth() / 2
            camera.position.y = rect.getY() + rect.getHeight() / 2
        }
    }

    override fun getTabTitle(): String {
        return "Function Flow"
    }

    override fun renderDebugText(debugTextRenderer: DebugTextRenderer) {
        debugTextRenderer.drawLine("FPS: ${Gdx.graphics.framesPerSecond}")
        debugTextRenderer.drawLine("Nodes: ${nodeList.size} (${selectedNodes.size} selected)")
        val reversedCount = nodeList.count { it.reversed }
        val totalCount = nodeList.size
        val reversedPercentText = if (totalCount == 0) {
            "0.000%"
        } else {
            "%.03f".format(Locale.US, reversedCount * 100f / totalCount) + "%"
        }
        debugTextRenderer.drawLine("Reversed: $reversedCount ($reversedPercentText)")
    }

    override fun renderConnections(shapeRenderer: ShapeRenderer, node: FlowNode) {
        var renderedCount = 0
        node.outEdges.forEach connectorDraw@{ targetNode ->
            val x1 = node.getOutConX() + 6
            val y1 = node.getOutConY() + 6
            val x2 = targetNode.getX() + 6
            val y2 = targetNode.getY() + 6
            val startX = if (x2 > x1) x1 else x2
            val startY = if (y2 > y1) y1 else y2
            if (cameraFrustum.overlaps(Rectangle(startX, startY, Math.abs(x2 - x1), Math.abs(y2 - y1))) == false) {
                return@connectorDraw
            }
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) == false && node.apiName != targetNode.apiName) {
                return@connectorDraw
            }

            shapeRenderer.color = node.color.cpy()
            if (selectedNodes.isEmpty()) {
                if (camera.zoom < 2.5f) {
                    shapeRenderer.color.a = 0.7f
                } else {
                    shapeRenderer.color.a = 0.2f
                }
            } else {
                if (node in selectedNodes || targetNode in selectedNodes) {
                    shapeRenderer.color.a = 0.7f
                } else {
                    shapeRenderer.color.a = 0.01f
                }
            }

            shapeRenderer.line(x1, y1, x2, y2)
            renderedCount++
        }
    }

    private val nodeContextMenu = popupMenu {
        fun apiListToMenu(current: PopupMenu, apis: List<String>) {
            apis.forEach {
                current.menuItem("Make part of '$it' API") {
                    onChange {
                        markSelectedAsApi(it)
                    }
                }
            }
        }
        menuItem("Disassemble") {
            onChange {
                selectedNodes.firstOrNull()?.funcName?.let { disassembleFuncCallback(it) }
            }
        }
        menuItem("Edit") {
            onChange {
                selectedNodes.firstOrNull()?.funcName?.let { editFuncCallback(it) }
            }
        }
        menuItem("Copy address") {
            onChange {
                selectedNodes.firstOrNull()?.let {
                    Gdx.app.clipboard.contents = it.fileOffset.toHex()
                }
            }
        }
        menuItem("Copy physical address") {
            onChange {
                selectedNodes.firstOrNull()?.let {
                    Gdx.app.clipboard.contents = (0x8804000 + it.fileOffset).toHex()
                }
            }
        }
        addSeparator()
        menuItem("Make part of 'default' API") {
            onChange {
                markSelectedAsApi("")
            }
        }
        apis.forEach { apiNames ->
            menuItem(apiNames.categoryName) {
                subMenu {
                    apiListToMenu(this, apiNames.apis)
                }
            }
        }
    }

    inner class FlowInput : InputListener() {
        private var mouseDragged: Boolean = false

        override fun touchDown(event: InputEvent?, stageX: Float, stageY: Float, pointer: Int, button: Int): Boolean {
            return true
        }

        override fun touchDragged(event: InputEvent?, stageX: Float, stageY: Float, pointer: Int) {
            mouseDragged = true
        }

        override fun touchUp(event: InputEvent?, stageX: Float, stageY: Float, pointerId: Int, button: Int) {
            if (button == Input.Buttons.RIGHT && mouseDragged == false) {
                val mouseOverNode = nodeList.firstOrNull { it.contains(pointer.x, pointer.y) }
                if (mouseOverNode != null && selectedNodes.contains(mouseOverNode) == false) {
                    selectedNodes.clear()
                    selectedNodes.add(mouseOverNode)
                }
                val pos = appStage.screenToStageCoordinates(Vector2(pointer.screenX, pointer.screenY))
                nodeContextMenu.showMenu(appStage, pos.x, pos.y)
            }
            mouseDragged = false
        }

        override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
            if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                    selectedNodes.clear()
                    selectedNodes.addAll(nodeList)
                } else if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                    val newNodes = mutableListOf<FlowNode>()
                    selectedNodes.forEach { node ->
                        node.outEdges.forEach { outNode ->
                            if (selectedNodes.contains(outNode) == false && newNodes.contains(outNode) == false) {
                                newNodes.add(outNode)
                            }
                        }
                    }
                    selectedNodes.addAll(newNodes)
                } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                    val newNodes = mutableListOf<FlowNode>()
                    val acceptedApis = selectedNodes.map { it.apiName }.toSet()
                    selectedNodes.forEach { node ->
                        node.outEdges.forEach { outNode ->
                            if (selectedNodes.contains(outNode) == false && newNodes.contains(outNode) == false
                                && outNode.inEdges.map { it.apiName }.all { acceptedApis.contains(it) }
                            ) {
                                newNodes.add(outNode)
                            }
                        }
                    }
                    selectedNodes.addAll(newNodes)
                }
            }

            if (Gdx.input.isKeyPressed(Input.Keys.NUM_9)) {
                selectedNodes.clear()
                nodeList.forEach { node ->
                    if (node.outEdges.none { it.apiName == "" } &&
                        node.inEdges.none { it.apiName == "" }) {
                        selectedNodes.add(node)
                    }
                }
            }

            if (Gdx.input.isKeyPressed(Input.Keys.NUM_0)) {
                selectedNodes.clear()
                nodeList.forEach { node ->
                    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                        if (node.inEdges.size == 0 && node.outEdges.size == 0) {
                            selectedNodes.add(node)
                        }
                    } else if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                        if (node.outEdges.size == 0) {
                            selectedNodes.add(node)
                        }
                    } else {
                        if (node.inEdges.size == 0) {
                            selectedNodes.add(node)
                        }
                    }
                }
            }

            if (Gdx.input.isKeyPressed(Input.Keys.Z)) {
                selectedNodes.removeIf { it.culled }
            }

            return false
        }
    }
}

class FlowApiSet(val categoryName: String, val apis: List<String>)
