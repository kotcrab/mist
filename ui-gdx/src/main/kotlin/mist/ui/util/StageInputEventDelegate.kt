package mist.ui.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage

class StageEventDelegate(private val targetStage: Stage) : InputListener() {
  override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
    val consumed = targetStage.touchDown(Gdx.input.x, Gdx.input.y, pointer, button)
    if (consumed) event?.stop()
    return consumed
  }

  override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
    val consumed = targetStage.touchDragged(Gdx.input.x, Gdx.input.y, pointer)
    if (consumed) event?.stop()
  }

  override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
    val consumed = targetStage.touchUp(Gdx.input.x, Gdx.input.y, pointer, button)
    if (consumed) event?.stop()
  }

  override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
    val consumed = targetStage.mouseMoved(Gdx.input.x, Gdx.input.y)
    if (consumed) event?.stop()
    return consumed
  }

  override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
    val consumed = targetStage.keyDown(keycode)
    if (consumed) event?.stop()
    return consumed
  }

  override fun keyUp(event: InputEvent?, keycode: Int): Boolean {
    val consumed = targetStage.keyUp(keycode)
    if (consumed) event?.stop()
    return consumed
  }

  override fun keyTyped(event: InputEvent?, character: Char): Boolean {
    val consumed = targetStage.keyTyped(character)
    if (consumed) event?.stop()
    return consumed
  }

  override fun scrolled(event: InputEvent?, x: Float, y: Float, amount: Int): Boolean {
    val consumed = targetStage.scrolled(amount)
    if (consumed) event?.stop()
    return consumed
  }
}
