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

import java.util.*

fun <T> MutableList<T>.swap(idx1: T, idx2: T) {
    swap(indexOf(idx1), indexOf(idx2))
}

fun <T> MutableList<T>.swap(idx1: Int, idx2: Int) {
    Collections.swap(this, idx1, idx2)
}

fun StringBuilder.appendLine(text: String = "", newLine: String = "\n") {
    append(text)
    append(newLine)
}
