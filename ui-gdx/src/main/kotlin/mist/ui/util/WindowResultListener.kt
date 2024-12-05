package mist.ui.util

interface WindowResultListener<T> {
  fun finished(result: T)

  fun canceled() {

  }
}
