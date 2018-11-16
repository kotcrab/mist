/*
 * mist - interactive mips disassembler and decompiler
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

package mist.util

import kotlin.reflect.KClass

/** @author Kotcrab */

class DecompLog {
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
