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
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.FocusManager
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import ktx.inject.Context
import mist.io.ProjectIO
import mist.ui.Assets
import mist.ui.util.MixedRenderingTab
import mist.ui.util.Pointer
import mist.ui.util.calcFrustum
import mist.util.DecompLog

/** @author Kotcrab */

abstract class VisualNodeTab<T : VisualNode>(protected val context: Context, closeable: Boolean = false) :
    Tab(true, closeable), MixedRenderingTab {
    private val assets: Assets = context.inject()
    protected val appStage: Stage = context.inject()
    protected val projectIO: ProjectIO = context.inject()
    protected val log: DecompLog = context.inject()

    private val delegateTable by lazy { createDelegationTable() }

    protected val camera by lazy { OrthographicCamera(1280f, 720f) }
    protected var cameraFrustum = Rectangle()
    protected val pointer by lazy { Pointer(camera) }
    private val shapeRenderer by lazy { ShapeRenderer() }
    private val batch by lazy { SpriteBatch() }
    private val debugTextRenderer by lazy { DebugTextRenderer(context, 0.6f) }

    protected val nodeStage: Stage by lazy { Stage(ScreenViewport(camera), batch) }
    protected val nodeList = mutableListOf<T>()
    protected val selectedNodes = mutableListOf<T>()
    protected val rectangularSelection: RectangularSelection<T> by lazy {
        RectangularSelection(nodeList, pointer, {
            selectedNodes.clear()
            selectedNodes.addAll(it)
        })
    }

    init {
        camera.position.x = (1280 / 2).toFloat()
        camera.position.y = (720 / 2).toFloat()
        cameraFrustum = camera.calcFrustum()
    }

    override fun update() {
        camera.update()
        cameraFrustum = camera.calcFrustum()
        shapeRenderer.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined
        nodeStage.act()
        nodeList.forEach { it.calcIfVisible(cameraFrustum) }
    }

    override fun render() {
        if (Gdx.input.isKeyPressed(Input.Keys.F11)) {
            debugTextRenderer.begin(delegateTable, batch)
            renderDebugText(debugTextRenderer)
            debugTextRenderer.end()
        }

        val alpha = if (selectedNodes.isEmpty()) {
            1f
        } else {
            0.1f
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        nodeList.forEach { node ->
            if (node.culled == false) {
                node.renderShapes(shapeRenderer, if (node in selectedNodes) 1f else alpha)
            }
        }
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        nodeList.forEach { node ->
            if (node.culled == false) {
                node.renderOutline(shapeRenderer, Color.BLACK, false, if (node in selectedNodes) 1f else alpha)
            }
        }
        shapeRenderer.end()

        if (camera.zoom < 5f) {
            batch.shader = assets.fontDistanceFieldShader
            nodeStage.draw()
            batch.shader = null
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        if (selectedNodes.isNotEmpty()) {
            selectedNodes.forEach {
                it.renderOutline(shapeRenderer, Color.ORANGE, true, 1f)
            }
        }
        shapeRenderer.end()

        rectangularSelection.render(shapeRenderer)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        for (node in selectedNodes) {
            node.renderOutline(shapeRenderer, Color.RED, true, alpha)
        }
        shapeRenderer.end()

        shapeRenderer.color = Color.BLACK
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        for (node in this.nodeList) {
            renderConnections(shapeRenderer, node)
        }
        shapeRenderer.end()

        if (shouldRenderConnectionTriangles()) {
            shapeRenderer.color = Color.BLACK
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            Gdx.gl.glEnable(GL20.GL_BLEND)
            for (node in this.nodeList) {
                renderConnectionTriangles(shapeRenderer, node)
            }
            shapeRenderer.end()
        }
    }

    protected open fun renderDebugText(debugTextRenderer: DebugTextRenderer) {
    }

    protected open fun renderConnections(shapeRenderer: ShapeRenderer, node: T) {

    }

    protected open fun shouldRenderConnectionTriangles(): Boolean {
        return false
    }

    protected open fun renderConnectionTriangles(shapeRenderer: ShapeRenderer, node: T) {

    }

    override fun resize(width: Int, height: Int) {
        if (width == 0 && height == 0) return
        debugTextRenderer.resize(width, height)
        nodeStage.viewport.update(width, height, false)
    }

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
    }

    override fun onShow() {
        super.onShow()
        takeInputFocus()
    }

    override fun onHide() {
        super.onHide()
        FocusManager.resetFocus(null)
        if (appStage.keyboardFocus == delegateTable) {
            appStage.keyboardFocus = null
        }
        if (appStage.scrollFocus == delegateTable) {
            appStage.scrollFocus = null
        }
    }

    protected fun takeInputFocus() {
        appStage.keyboardFocus = delegateTable
        appStage.scrollFocus = delegateTable
    }

    protected abstract fun createDelegationTable(): VisTable

    override fun getContentTable(): Table {
        return delegateTable
    }

    protected inner class CameraMovement : InputListener() {
        private var lastScreenX = 0f
        private var lastScreenY = 0f
        private var cameraDragged: Boolean = false
        private var mouseDragged: Boolean = false

        override fun touchDown(event: InputEvent?, stageX: Float, stageY: Float, pointerId: Int, button: Int): Boolean {
            takeInputFocus()

            val x = pointer.x
            val y = pointer.y
            lastScreenX = pointer.screenX
            lastScreenY = pointer.screenY

            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                if (selectedNodes.any { it.contains(x, y) } == false) {
                    selectedNodes.clear()
                }
                if (selectedNodes.size == 0) {
                    nodeList.firstOrNull { it.contains(x, y) }?.run { selectedNodes.add(this) }
                }
                return true
            }
            return false
        }

        override fun touchDragged(event: InputEvent?, stageX: Float, stageY: Float, pointerId: Int) {
            mouseDragged = true

            val deltaScreenX = pointer.screenX - lastScreenX
            val deltaScreenY = pointer.screenY - lastScreenY
            lastScreenX = pointer.screenX
            lastScreenY = pointer.screenY

            if (selectedNodes.isNotEmpty() && Gdx.input.isButtonPressed(Input.Buttons.LEFT) && selectedNodes.all { it.culled == false }) {
                selectedNodes.forEach {
                    it.translate(deltaScreenX * camera.zoom, -deltaScreenY * camera.zoom)
                }
                dirty()
            } else if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                if (deltaScreenX != 0f || deltaScreenY != 0f) {
                    cameraDragged = true
                    camera.position.x -= deltaScreenX * camera.zoom
                    camera.position.y += deltaScreenY * camera.zoom
                }
            }
        }

        override fun touchUp(event: InputEvent?, stageX: Float, stageY: Float, pointerId: Int, button: Int) {
            cameraDragged = false
            mouseDragged = false
            lastScreenX = 0f
            lastScreenY = 0f
        }
    }

    protected inner class CameraScrollInput : InputListener() {
        override fun scrolled(event: InputEvent?, x: Float, y: Float, amount: Int): Boolean {
            if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                if (amount == 1) { // out
                    if (camera.zoom >= 500) return false
                    val newZoom = camera.zoom + 0.1f * camera.zoom * 2f
                    camera.position.x = pointer.x + newZoom / camera.zoom * (camera.position.x - pointer.x)
                    camera.position.y = pointer.y + newZoom / camera.zoom * (camera.position.y - pointer.y)
                    camera.zoom += 0.1f * camera.zoom * 2f
                } else if (amount == -1) { // in
                    if (camera.zoom <= 0.5f) return false
                    val newZoom = camera.zoom - 0.1f * camera.zoom * 2f
                    camera.position.x = pointer.x + newZoom / camera.zoom * (camera.position.x - pointer.x)
                    camera.position.y = pointer.y + newZoom / camera.zoom * (camera.position.y - pointer.y)
                    camera.zoom -= 0.1f * camera.zoom * 2f
                }
            } else {
                val translateAmount = 100f * camera.zoom
                camera.position.y -= translateAmount * amount
            }
            return true
        }
    }
}
