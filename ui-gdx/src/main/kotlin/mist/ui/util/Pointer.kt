package mist.ui.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3

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
