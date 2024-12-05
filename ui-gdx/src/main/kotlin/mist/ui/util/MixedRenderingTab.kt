package mist.ui.util

interface MixedRenderingTab {
  fun update()

  fun render()

  fun resize(width: Int, height: Int)
}
