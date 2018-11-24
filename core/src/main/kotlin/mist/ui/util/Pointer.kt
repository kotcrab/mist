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

package mist.ui.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3

/** @author Kotcrab */

class Pointer(private val camera: OrthographicCamera) {
    private val calcVector: Vector3 = Vector3(0f, 0f, 0f)

    val x: Float
        get() {
            calcVector.x = screenX
            camera.unproject(calcVector)
            return calcVector.x
        }

    val y: Float
        get() {
            calcVector.y = screenY
            camera.unproject(calcVector)
            return calcVector.y
        }

    val screenX
        get() = Gdx.input.x.toFloat()

    val screenY
        get() = Gdx.input.y.toFloat()

    fun calcX(x: Float): Float {
        calcVector.x = x
        camera.unproject(calcVector)
        return calcVector.x
    }

    fun calcY(y: Float): Float {
        calcVector.y = y
        camera.unproject(calcVector)
        return calcVector.y
    }
}
