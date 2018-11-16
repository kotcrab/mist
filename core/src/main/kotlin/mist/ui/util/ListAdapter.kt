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

import com.badlogic.gdx.scenes.scene2d.Actor
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.util.adapter.AbstractListAdapter
import com.kotcrab.vis.ui.widget.VisTable
import java.util.*

/** @author Kotcrab */

abstract class StaticListAdapter<T>(list: List<T>) : BaseListAdapter<T>(list) {
    override fun updateView(view: VisTable?, item: T) {
        error("update list not supported, perform full rebuild instead")
    }
}

abstract class BaseListAdapter<T>(list: List<T>) : ListAdapter<T, VisTable>(list) {
    private val bg = VisUI.getSkin().getDrawable("window-bg")
    private val selection = VisUI.getSkin().getDrawable("list-selection")

    init {
        selectionMode = SelectionMode.SINGLE
    }

    override fun selectView(view: VisTable) {
        view.background = selection
    }

    override fun deselectView(view: VisTable) {
        view.background = bg
    }
}

abstract class ListAdapter<ItemT, ViewT : Actor>(private val list: List<ItemT>) : AbstractListAdapter<ItemT, ViewT>() {
    private companion object {
        const val immutableError = "not supported by immutable list"
    }

    override fun iterable(): Iterable<ItemT> {
        return list
    }

    override fun size(): Int {
        return list.size
    }

    override fun indexOf(item: ItemT): Int {
        return list.indexOf(item)
    }

    override fun add(element: ItemT) {
        error(immutableError)
    }

    override fun get(index: Int): ItemT {
        return list[index]
    }

    override fun sort(comparator: Comparator<ItemT>) {
        error(immutableError)
    }

    // Delegates

    operator fun set(index: Int, element: ItemT): ItemT {
        error(immutableError)
    }

    fun add(index: Int, element: ItemT) {
        error(immutableError)
    }

    fun remove(index: Int): ItemT? {
        error(immutableError)
    }

    fun remove(item: ItemT): Boolean {
        error(immutableError)
    }

    fun clear() {
        error(immutableError)
    }

    fun addAll(c: Collection<ItemT>): Boolean {
        error(immutableError)
    }

    fun addAll(index: Int, c: Collection<ItemT>): Boolean {
        error(immutableError)
    }

    fun removeAll(c: Collection<*>): Boolean {
        error(immutableError)
    }
}

abstract class StaticMutableListAdapter<T>(list: MutableList<T>) : BaseMutableListAdapter<T>(list) {
    override fun updateView(view: VisTable?, item: T) {
        error("update list not supported, perform full rebuild instead")
    }
}

abstract class BaseMutableListAdapter<T>(list: MutableList<T>) : MutableListAdapter<T, VisTable>(list) {
    private val bg = VisUI.getSkin().getDrawable("window-bg")
    private val selection = VisUI.getSkin().getDrawable("list-selection")

    init {
        selectionMode = SelectionMode.SINGLE
    }

    override fun selectView(view: VisTable) {
        view.background = selection
    }

    override fun deselectView(view: VisTable) {
        view.background = bg
    }
}

abstract class MutableListAdapter<ItemT, ViewT : Actor>(private val list: MutableList<ItemT>) :
    AbstractListAdapter<ItemT, ViewT>() {

    override fun iterable(): Iterable<ItemT> {
        return list
    }

    override fun size(): Int {
        return list.size
    }

    override fun indexOf(item: ItemT): Int {
        return list.indexOf(item)
    }

    override fun add(element: ItemT) {
        list.add(element)
        itemAdded(element)
    }

    override fun get(index: Int): ItemT {
        return list[index]
    }

    override fun sort(comparator: Comparator<ItemT>) {
        Collections.sort(list, comparator)
    }

    // Delegates

    operator fun set(index: Int, element: ItemT): ItemT {
        val res = list.set(index, element)
        itemsChanged()
        return res
    }

    fun add(index: Int, element: ItemT) {
        list.add(index, element)
        itemAdded(element)
    }

    fun remove(index: Int): ItemT? {
        val res = list.removeAt(index)
        if (res != null) itemRemoved(res)
        return res
    }

    fun remove(item: ItemT): Boolean {
        val res = list.remove(item)
        if (res) itemRemoved(item)
        return res
    }

    fun clear() {
        list.clear()
        itemsChanged()
    }

    fun addAll(c: Collection<ItemT>): Boolean {
        val res = list.addAll(c)
        itemsChanged()
        return res
    }

    fun addAll(index: Int, c: Collection<ItemT>): Boolean {
        val res = list.addAll(index, c)
        itemsChanged()
        return res
    }

    fun removeAll(c: Collection<*>): Boolean {
        val res = list.removeAll(c)
        itemsChanged()
        return res
    }
}
