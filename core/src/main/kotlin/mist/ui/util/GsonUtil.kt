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

package mist.ui.util

import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import kotlin.reflect.KClass

/** @author Kotcrab */

fun <C : Any, S : C> runtimeTypeAdapter(base: KClass<C>,
                                        subTypes: Array<KClass<out S>>,
                                        legacySubTypes: Array<out Pair<String, KClass<out S>>> = emptyArray()): RuntimeTypeAdapterFactory<C> {
    val adapter = RuntimeTypeAdapterFactory.of(base.java, "_type")
    subTypes.forEach { subClass ->
        adapter.registerSubtype(subClass.java)
    }
    legacySubTypes.forEach { subClass ->
        adapter.registerLegacySubtype(subClass.second.java, subClass.first)
    }
    return adapter
}
