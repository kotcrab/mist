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
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import mist.ui.util.Pointer

/** @author Kotcrab */

class RectangularSelection<T : VisualNode>(
    private val nodes: MutableList<T>,
    private val pointer: Pointer,
    private val finishedDrawing: (containedNodes: List<T>) -> Unit
) : InputListener() {
    companion object {
        private const val DRAW_BUTTON = Buttons.RIGHT
    }

    private var isDrawing = false
    private var dragged = false
    private val currentRect = Rectangle()
    private val rectToDraw = Rectangle()
    private val previousRectDrawn = Rectangle()
    private var drawingPointer = -1

    fun render(shapeRenderer: ShapeRenderer) {
        if (isDrawing) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            shapeRenderer.color = Color.RED
            shapeRenderer.begin(ShapeType.Line)
            shapeRenderer.rect(rectToDraw.x, rectToDraw.y, rectToDraw.width, rectToDraw.height)
            shapeRenderer.end()
            shapeRenderer.setColor(0.7f, 0f, 0f, 0.3f)
            shapeRenderer.begin(ShapeType.Filled)
            shapeRenderer.rect(rectToDraw.x, rectToDraw.y, rectToDraw.width, rectToDraw.height)
            shapeRenderer.end()
        }
    }

    override fun touchDown(event: InputEvent?, stageX: Float, stageY: Float, pointerId: Int, button: Int): Boolean {
        if (button == DRAW_BUTTON) {
            val x = pointer.x
            val y = pointer.y
            drawingPointer = pointerId
            currentRect.set(x, y, 0f, 0f)
            updateDrawableRect()
            return true
        }
        return false
    }

    override fun touchDragged(event: InputEvent?, stageX: Float, stageY: Float, pointerId: Int) {
        if (drawingPointer == pointerId && Gdx.input.isButtonPressed(DRAW_BUTTON)) {
            val x = pointer.x
            val y = pointer.y
            dragged = true
            currentRect.setSize(x - currentRect.x, y - currentRect.y)
            updateDrawableRect()
        }
    }

    override fun touchUp(event: InputEvent?, stageX: Float, stageY: Float, pointerId: Int, button: Int) {
        if (drawingPointer == pointerId && Gdx.input.isButtonPressed(DRAW_BUTTON) == false && dragged) {
            finishedDrawing(nodes.filter { rectToDraw.contains(it.bounds) })
            isDrawing = false
            dragged = false
            drawingPointer = -1
        }
    }

    private fun updateDrawableRect() {
        var x = currentRect.x
        var y = currentRect.y
        var width = currentRect.width
        var height = currentRect.height

        // Make the width and height positive, if necessary
        if (width < 0) {
            width = 0 - width
            x = x - width + 1
        }

        if (height < 0) {
            height = 0 - height
            y = y - height + 1
        }

        // Update rectToDraw after saving old value
        if (isDrawing) {
            previousRectDrawn.set(rectToDraw.x, rectToDraw.y, rectToDraw.width, rectToDraw.height)
        } else {
            isDrawing = true
        }
        rectToDraw.set(x, y, width, height)
    }
}
