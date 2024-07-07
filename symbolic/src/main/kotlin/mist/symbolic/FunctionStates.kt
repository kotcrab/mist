package mist.symbolic

import mist.util.Counter

class FunctionStates(
  private val counters: MutableMap<String, Counter> = mutableMapOf(),
) {
  fun get(name: String): Int {
    return getCounter(name).value
  }

  fun set(name: String, value: Int) {
    return getCounter(name).set(value)
  }

  fun getAndIncrement(name: String): Int {
    val counter = getCounter(name)
    val value = counter.value
    counter.increment()
    return value
  }

  fun getAndAdd(name: String, addValue: Int): Int {
    val counter = getCounter(name)
    val value = counter.value
    counter.value += addValue
    return value
  }

  fun copyOf(): FunctionStates {
    return FunctionStates(
      counters
        .mapValues { it.value.copy() }
        .toMutableMap()
    )
  }

  private fun getCounter(name: String): Counter {
    return counters.getOrPut(name) { Counter(0) }
  }
}
