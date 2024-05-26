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
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisLabel
import ktx.inject.Context
import mist.io.FlowNodeDto

class FlowNode(
  context: Context, nodeStage: Stage,
  val id: Int, nodeFuncName: String, val fileOffset: Int, color: Color,
  xPos: Float, yPos: Float
) : VisualNode(context, color, xPos, yPos) {
  val inEdges = mutableListOf<FlowNode>()
  val outEdges = mutableListOf<FlowNode>()
  var apiName = ""
    set(value) {
      field = value
      updateBounds()
    }
  var reversed: Boolean = false
    set(value) {
      field = value
      updateBounds()
    }

  var funcName = nodeFuncName
    set(value) {
      field = value
      updateBounds()
    }

  private val titleLabel = VisLabel(funcName, labelStyle)
  private val extraInfoLabel = VisLabel("", labelStyle)

  init {
    table.left()
    table.defaults().left()
    table.add(titleLabel).center().pad(3f).row()
    table.add(extraInfoLabel).pad(3f).row()
    titleLabel.setFontScale(0.7f)
    extraInfoLabel.setFontScale(0.6f)
    nodeStage.addActor(table)
    updateBounds()
  }

  fun updateLabels() {
    updateBounds()
  }

  override fun updateBounds() {
    if (apiName.isBlank()) {
      titleLabel.setText("${if (reversed) "(R) " else ""}$funcName")
    } else {
      titleLabel.setText("${if (reversed) "(R) " else ""}$apiName::$funcName")
    }
    updateExtraInfo()
    val width = table.prefWidth
    val height = table.prefHeight
    bounds.setSize(width, height)
    table.setPosition(bounds.x, bounds.y + bounds.height / 2)
    inConBounds.set(bounds.x, bounds.y + 5f, 6f, 12f)
    outConBounds.set(bounds.x + bounds.width - 6f, bounds.y + 5f, 6f, 12f)
  }

  private fun updateExtraInfo() {
    val externalCallsTo = inEdges.count { it.apiName != apiName }
    var extraText = if (externalCallsTo > 0) {
      "Ext. calls to this: $externalCallsTo"
    } else {
      ""
    }
    val externalCalls =
      outEdges.filter { it.apiName != apiName }.joinToString(separator = "\n", transform = { it.titleLabel.text })
    if (externalCalls.isNotBlank()) {
      if (extraText.isNotBlank()) extraText += "\n"
      extraText += "Ext. calls from this:\n"
      extraText += externalCalls
    }
    extraInfoLabel.setText(extraText)
  }

  override fun renderExtraOutline(shapeRenderer: ShapeRenderer) {
    // line under title text
    shapeRenderer.line(
      bounds.x,
      (bounds.y + bounds.height - 30),
      (bounds.x + bounds.width),
      (bounds.y + bounds.height - 30)
    )
  }

  fun addNode(otherNode: FlowNode) {
    outEdges.add(otherNode)
    otherNode.inEdges.add(this)
  }

  fun toDto(): FlowNodeDto {
    return FlowNodeDto(id, fileOffset, apiName, Color.rgba8888(color), getX(), getY(), outEdges.map { it.id })
  }
}
