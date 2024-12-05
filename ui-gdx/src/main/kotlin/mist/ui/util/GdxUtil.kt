package mist.ui.util

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.vis.KVisTable
import ktx.vis.table
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

fun ShapeRenderer.rect(rect: Rectangle) {
  rect(rect.x, rect.y, rect.width, rect.height)
}

fun OrthographicCamera.calcFrustum(): Rectangle {
  val cameraWidth = viewportWidth * zoom
  val cameraHeight = viewportHeight * zoom
  val cameraX = position.x - cameraWidth / 2
  val cameraY = position.y - cameraHeight / 2
  return Rectangle(cameraX, cameraY, cameraWidth, cameraHeight)
}

abstract class VisFragment {
  open val ui: Actor = Actor()
}

inline fun Actor.onChange(crossinline listener: () -> Any): ChangeListener {
  val changeListener = object : ChangeListener() {
    override fun changed(event: ChangeEvent, actor: Actor) {
      listener()
    }
  }
  this.addListener(changeListener)
  return changeListener
}

fun Table.ui(setVisDefaults: Boolean = false, init: KVisTable.() -> Unit) {
  clearChildren()
  add(table(setVisDefaults, init)).grow()
}

suspend fun <T> Stage.startForResult(window: (Continuation<T>) -> VisWindow): T = suspendCoroutine { continuation ->
  addActor(window(continuation).fadeIn())
}

suspend fun <T, Arg0> Stage.startForResult(window: (Arg0, Continuation<T>) -> VisWindow, arg0: Arg0): T {
  return suspendCoroutine { continuation ->
    addActor(window(arg0, continuation).fadeIn())
  }
}

suspend fun <T, Arg0, Arg1> Stage.startForResult(
  window: (Arg0, Arg1, Continuation<T>) -> VisWindow, arg0: Arg0, arg1: Arg1
): T {
  return suspendCoroutine { continuation ->
    addActor(window(arg0, arg1, continuation).fadeIn())
  }
}

suspend fun <T, Arg0, Arg1, Arg2> Stage.startForResult(
  window: (Arg0, Arg1, Arg2, Continuation<T>) -> VisWindow, arg0: Arg0, arg1: Arg1, arg2: Arg2
): T {
  return suspendCoroutine { continuation ->
    addActor(window(arg0, arg1, arg2, continuation).fadeIn())
  }
}
