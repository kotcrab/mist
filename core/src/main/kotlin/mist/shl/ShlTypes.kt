/*
 * mist - interactive disassembler and decompiler
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

import kio.util.runtimeTypeAdapter
import kio.util.toHex
import mist.shl.ShlType.*

/** @author Kotcrab */

class ShlTypes {
    private var nextTypeId = 0
    private val structs = mutableListOf<ShlStruct>()
    private val enums = mutableListOf<ShlEnum>()
    private val primitives = mutableListOf<ShlPrimitive>()

    @Transient
    private var typeAccess = Array<ShlType?>(0, { null })

    fun addStruct(name: String): Int {
        checkTypeAccess()
        val tid = getNextTypeId()
        val type = ShlStruct(tid, name)
        structs.add(type)
        insertType(type)
        return tid
    }

    fun addEnum(name: String, size: Int): Int {
        checkTypeAccess()
        val tid = getNextTypeId()
        val type = ShlEnum(tid, name, size)
        enums.add(type)
        insertType(type)
        return tid
    }

    fun addPrimitive(name: String, size: Int): Int {
        checkTypeAccess()
        val tid = getNextTypeId()
        val type = ShlPrimitive(tid, name, size)
        primitives.add(type)
        insertType(type)
        return tid
    }

    fun getStructs(): List<ShlStruct> {
        return structs
    }

    fun getEnums(): List<ShlEnum> {
        return enums
    }

    fun getPrimitives(): List<ShlPrimitive> {
        return primitives
    }

    fun getType(tid: Int): ShlType? {
        checkTypeAccess()
        if (tid >= typeAccess.size || tid < 0) return null
        return typeAccess[tid]
    }

    fun getTypeByName(name: String): ShlType? {
        typeAccess.forEach {
            if (it == null) return@forEach
            if (it.name == name) return it
        }
        return null
    }

    fun getPointerSize(): Int {
        val type = getType(0) as? ShlPrimitive
            ?: error("tid 0 must be primitive and must represent size of pointer")
        return type.size
    }

    fun sizeOfStructUntil(struct: ShlStruct, field: ShlStructField): Int {
        var size = 0
        struct.fields.subList(0, struct.fields.indexOf(field)).forEach {
            size += sizeOfStructField(it)
        }
        return size
    }

    fun sizeOfStructField(field: ShlStructField): Int {
        return sizeOfStructArrayField(field) * field.arraySize
    }

    fun sizeOfStructArrayField(field: ShlStructField): Int {
        return if (field.pointer) {
            getPointerSize()
        } else {
            sizeOf(field.refTid)
        }
    }

    fun sizeOf(tid: Int): Int {
        var size = 0
        val type = getType(tid)
        when (type) {
            is ShlStruct -> {
                type.fields.forEach { field ->
                    size += sizeOfStructField(field)
                }
            }
            is ShlEnum -> size += type.size
            is ShlPrimitive -> size += type.size
            null -> error("type id $tid does not exist")
        }
        return size
    }

    fun getAccessedStructField(struct: ShlStruct, offset: Int): Pair<ShlStructField?, String> {
        val fallback = Pair(null, "__virtualField0 + ${offset.toHex()}")
        if (struct.fields.size == 0) return fallback
        return CalcStructAccessedField().calc(struct, offset) ?: return fallback
    }

    private inner class CalcStructAccessedField {
        private var size = 0
        private var accessStack: MutableList<ShlStructField> = mutableListOf()
        fun calc(struct: ShlStruct, offset: Int): Pair<ShlStructField?, String>? {
            struct.fields.forEach { field ->
                repeat(field.arraySize) { arrayIdx ->
                    val refType = getType(field.refTid)
                    if (refType is ShlStruct && field.pointer == false) {
                        accessStack.add(field)
                        val result = calc(refType, offset)
                        if (result != null) return result
                        accessStack.removeAt(accessStack.lastIndex)
                    } else {
                        if (size == offset) { // exact field match
                            accessStack.add(field)
                            val joinedNames = accessStack.joinToString(separator = ".", transform = { it.name })
                            return Pair(field, if (field.arraySize != 1) "$joinedNames[$arrayIdx]" else joinedNames)
                        } else if (size > offset) {
                            return Pair(
                                null,
                                "__virtualField0 + ${offset.toHex()}"
                            ) // TODO should this try to find closest entry?
                        }
                        size += sizeOfStructArrayField(field)
                    }
                }
            }
            return null
        }
    }

    fun checkStructFieldAdditionValid(struct: ShlStruct, addedTypeName: String): Boolean {
        val addedType = getTypeByName(addedTypeName) ?: return false
        return checkStructFieldAdditionValid(struct.tid, addedType.tid)
    }

    private fun checkStructFieldAdditionValid(checkForTid: Int, checkInTid: Int): Boolean {
        val type = getType(checkInTid) as? ShlStruct ?: return false
        if (type.tid == checkForTid) return false
        type.fields.forEach {
            if (it.refTid == checkForTid && it.pointer == false) return false
            val valid = checkStructFieldAdditionValid(checkForTid, it.refTid)
            if (valid == false) return false
        }
        return true
    }

    private fun getNextTypeId(): Int {
        val tid = nextTypeId
        nextTypeId++
        return tid
    }

    private fun checkTypeAccess() {
        if (typeAccess.isNotEmpty()) return
        if (nextTypeId == 0) return
        // perform initial init of type access after deserialization
        structs.forEach { insertType(it) }
        enums.forEach { insertType(it) }
        primitives.forEach { insertType(it) }
    }

    private fun insertType(type: ShlType) {
        ensureCapacity(type.tid)
        if (typeAccess[type.tid] != null) error("duplicate type id: ${type.tid}")
        typeAccess[type.tid] = type
    }

    private fun ensureCapacity(tid: Int) {
        if (typeAccess.size > tid) return
        val oldTypeAccess = typeAccess
        val minCapacity = 1000
        typeAccess = Array(Math.max(minCapacity, (tid * 0.8).toInt()), { null })
        System.arraycopy(oldTypeAccess, 0, typeAccess, 0, oldTypeAccess.size)
    }
}

sealed class ShlType(val tid: Int, var name: String) {
    companion object {
        fun provideGsonTypeAdapter() = runtimeTypeAdapter(
            ShlType::class, arrayOf(
                ShlStruct::class,
                ShlEnum::class,
                ShlPrimitive::class
            )
        )
    }

    class ShlStruct(tid: Int, name: String) : ShlType(tid, name) {
        val fields = mutableListOf<ShlStructField>()

        override fun toString(): String {
            return "$name (tid = $tid)"
        }
    }

    class ShlEnum(tid: Int, name: String, var size: Int) : ShlType(tid, name) {
        val members = mutableListOf<ShlEnumMembers>()

        override fun toString(): String {
            return "$name (tid = $tid, size = ${size.toHex()})"
        }
    }

    class ShlPrimitive(tid: Int, name: String, var size: Int) : ShlType(tid, name) {
        override fun toString(): String {
            return "$name (tid = $tid, size = ${size.toHex()})"
        }
    }
}

class ShlStructField(var refTid: Int, var pointer: Boolean, var name: String, var arraySize: Int, var comment: String)

class ShlEnumMembers(var name: String, var value: Int)
