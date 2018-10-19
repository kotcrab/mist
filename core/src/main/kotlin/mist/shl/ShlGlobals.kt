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

package mist.shl

import mist.util.swap

/** @author Kotcrab */

class ShlGlobals {
    private val globals = mutableListOf<ShlGlobal>()
    @Transient
    private var accessMap = mutableMapOf<Int, ShlGlobal>()

    fun addGlobal(address: Int, name: String, refTid: Int, pointer: Boolean, comment: String) {
        checkAccessMap()
        val global = ShlGlobal(address, name, refTid, pointer, comment)
        globals.add(global)
        insertGlobal(global)
    }

    fun editGlobal(oldGlobal: ShlGlobal, newAddress: Int, newName: String, newRefTid: Int, newPointer: Boolean, newComment: String) {
        val prevIdx = globals.indexOf(oldGlobal)
        delete(oldGlobal)
        val newGlobal = ShlGlobal(newAddress, newName, newRefTid, newPointer, newComment)
        globals.add(prevIdx, newGlobal)
        insertGlobal(newGlobal)
    }

    fun getGlobals(): List<ShlGlobal> {
        return globals
    }

    fun getGlobalByAddress(address: Int): ShlGlobal? {
        checkAccessMap()
        return accessMap[address]
    }

    private fun checkAccessMap() {
        if (accessMap.isNotEmpty()) return
        // perform initial init of access map after deserialization
        globals.forEach { insertGlobal(it) }
    }

    private fun insertGlobal(global: ShlGlobal) {
        accessMap[global.address] = global
    }

    fun moveUp(global: ShlGlobal) {
        val newIndex = globals.indexOf(global) - 1
        if (newIndex >= 0) {
            globals.swap(newIndex, newIndex + 1)
        }
    }

    fun moveDown(global: ShlGlobal) {
        val newIndex = globals.indexOf(global) + 1
        if (newIndex < globals.size) {
            globals.swap(newIndex, newIndex - 1)
        }
    }

    fun delete(global: ShlGlobal) {
        accessMap.remove(global.address)
        globals.remove(global)
    }
}

class ShlGlobal(val address: Int, var name: String, var refTid: Int, var pointer: Boolean, var comment: String)
