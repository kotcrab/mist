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

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.kotcrab.vis.ui.widget.VisTable
import ktx.inject.Context
import mist.ui.Assets

/** @author Kotcrab */

class DebugTextRenderer(context: Context, private val scale: Float) {
    private val assets: Assets = context.inject()

    private val projectionMatrix = Matrix4()
    private val offset = 10f

    private var batch: SpriteBatch? = null
    private var lineIdx = 0
    private var lineHeight = 0f
    private var baseX = 0f
    private var baseY = 0f

    fun resize(width: Int, height: Int) {
        projectionMatrix.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
        projectionMatrix.scl(scale)
    }

    fun begin(destTable: VisTable, batch: SpriteBatch) {
        this.batch = batch
        batch.projectionMatrix = projectionMatrix
        batch.shader = assets.fontDistanceFieldShader
        batch.begin()
        lineIdx = 0
        lineHeight = 25f
        val pos = destTable.localToStageCoordinates(Vector2.Zero.cpy())
        baseX = pos.x / scale + offset
        baseY = pos.y / scale + destTable.height / scale - offset
    }

    fun drawLine(text: String) {
        if (batch == null) error("call begin() first")
        assets.consolasFont.draw(batch, text, baseX, baseY - lineHeight * lineIdx)
        lineIdx++
    }

    fun end() {
        if (batch == null) error("call begin() first")
        batch!!.end()
        batch!!.shader = null
    }
}
