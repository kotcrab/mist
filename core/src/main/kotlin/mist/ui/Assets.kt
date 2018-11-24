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

package mist.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import ktx.assets.toInternalFile

/** @author Kotcrab */

class Assets {
    val fontDistanceFieldShader = ShaderProgram("font.vert".toInternalFile(), "font.frag".toInternalFile())
    private val fontTexture: Texture
    val consolasFont: BitmapFont
    val consolasSmallFont: BitmapFont

    init {
        if (fontDistanceFieldShader.isCompiled == false) {
            Gdx.app.error("fontShader", "compilation failed:\n" + fontDistanceFieldShader.log)
        }
        fontTexture = Texture("inconsolata.png".toInternalFile(), true)
        fontTexture.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear)
        consolasFont = BitmapFont("inconsolata.fnt".toInternalFile(), TextureRegion(fontTexture), false)
        consolasSmallFont =
                BitmapFont("inconsolata-small.fnt".toInternalFile(), "inconsolata-small.png".toInternalFile(), false)
    }

    fun dispose() {
        fontDistanceFieldShader.dispose()
        fontTexture.dispose()
        consolasFont.dispose()
        consolasSmallFont.dispose()
    }
}
