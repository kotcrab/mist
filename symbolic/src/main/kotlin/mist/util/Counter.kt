package mist.util

data class Counter(
  var value: Int
) {
  fun increment() {
    value += 1
  }

  fun decrement() {
    value -= 1
  }

  fun set(newValue: Int) {
    value = newValue
  }
}
