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

package mist.ui.node

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.kotcrab.vis.ui.widget.VisTable
import ktx.inject.Context
import mist.ui.Assets
import mist.ui.util.rect

/** @author Kotcrab */

abstract class VisualNode(context: Context, val color: Color, xPos: Float, yPos: Float) {
  private val assets: Assets = context.inject()

  val bounds = Rectangle(xPos, yPos, 0f, 0f)

  protected val inConBounds = Rectangle()
  protected val outConBounds = Rectangle()

  protected val table = VisTable()
  protected val labelStyle = Label.LabelStyle(assets.consolasFont, Color.WHITE.cpy())

  var culled: Boolean = false
    private set

  fun getX() = bounds.x
  fun getY() = bounds.y
  fun setX(x: Float) {
    bounds.x = x
    updateBounds()
  }

  fun setY(y: Float) {
    bounds.y = y
    updateBounds()
  }

  fun setPos(x: Float, y: Float) {
    bounds.x = x
    bounds.y = y
    updateBounds()
  }

  fun translate(dx: Float, dy: Float) {
    bounds.x += dx
    bounds.y += dy
    updateBounds()
  }

  protected abstract fun updateBounds()

  fun contains(px: Float, py: Float): Boolean {
    return bounds.contains(px, py)
  }

  fun getWidth() = bounds.width
  fun getHeight() = bounds.height
  fun getOutConX() = outConBounds.x
  fun getOutConY() = outConBounds.y
  fun getInConX() = inConBounds.x
  fun getInConY() = inConBounds.y

  fun calcIfVisible(cameraRect: Rectangle) {
    culled = !cameraRect.overlaps(bounds)
    table.isVisible = !culled
  }

  fun renderShapes(shapeRenderer: ShapeRenderer, alpha: Float) {
    shapeRenderer.color = Color(0.35f, 0.35f, 0.35f, alpha)
    shapeRenderer.rect(bounds)

    shapeRenderer.color = Color.BLACK
    shapeRenderer.rect(inConBounds)
    shapeRenderer.rect(outConBounds)
  }

  protected open fun renderExtraOutline(shapeRenderer: ShapeRenderer) {
  }

  fun renderOutline(shapeRenderer: ShapeRenderer, selectedColor: Color, selected: Boolean, alpha: Float) {
    if (selected) {
      shapeRenderer.color = selectedColor.cpy()
    } else {
      shapeRenderer.color = color.cpy()
    }
    shapeRenderer.color.a = alpha
    shapeRenderer.rect(bounds)
    renderExtraOutline(shapeRenderer)
  }
}
