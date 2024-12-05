package mist.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import ktx.assets.toInternalFile

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
