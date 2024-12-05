package mist.util

import kotlin.reflect.KClass

class MistLogger {
  fun trace(tag: String, msg: String) {
    println("T[$tag]: $msg")
  }

  fun info(tag: String, msg: String) {
    println("I[$tag]: $msg")
  }

  fun warn(tag: String, msg: String) {
    println("W[$tag]: $msg")
  }

  fun fatal(tag: String, msg: String) {
    println("F[$tag]: $msg")
  }

  fun panic(tag: String, msg: String): Nothing {
    println("PN[$tag]: $msg")
    error("panic: $msg")
  }
}

fun Any.logTag(): String {
  return this.javaClass.simpleName
}

fun logTag(clazz: KClass<*>): String {
  return clazz.java.simpleName
}
