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

package mist.ui.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.ListView
import com.kotcrab.vis.ui.widget.PopupMenu
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import ktx.actors.onChange
import ktx.inject.Context
import ktx.vis.menuItem
import ktx.vis.popupMenu
import ktx.vis.table
import mist.io.ProjectIO
import mist.shl.ShlFunctionDef
import mist.ui.util.BaseMutableListAdapter
import java.util.*
import com.badlogic.gdx.utils.Array as GdxArray

/** @author Kotcrab */

class FuncsPanel(
    private val context: Context,
    private val listener: FuncsWindowListener
) : VisTable(true) {
    private val appStage: Stage = context.inject()
    private val projectIO: ProjectIO = context.inject()
    private val userPrefs = projectIO.getUserPrefs()

    private val funcsList: List<ShlFunctionDef> = projectIO.getFuncs()
    private val listAdapter = FilteredFunctionAdapter()

    init {
        touchable = Touchable.enabled
        background = VisUI.getSkin().getDrawable("window-bg")

        pad(3f)
        val listView = ListView(listAdapter)
        listView.scrollPane.setSmoothScrolling(false)
        add(VisLabel("Functions")).left().growX().row()
        add(listView.mainTable).grow().row()
        add(table(true) {
            label("Search")
            textField {
                it.growX().minWidth(10f)
                onChange {
                    userPrefs.lastFuncSearch = text
                    listAdapter.filterList(text)
                }
                programmaticChangeEvents = true
                text = userPrefs.lastFuncSearch
            }
        }).growX()
    }

    fun refreshList() {
        listAdapter.itemsDataChanged()
    }

    fun showEditFuncWindow(def: ShlFunctionDef) {
        appStage.addActor(FunctionEditWindow(context, this@FuncsPanel, this@FuncsPanel.listener, def).fadeIn())
    }

    inner class FilteredFunctionAdapter(
        private val filteredList: ArrayList<ShlFunctionDef>
        = funcsList.toCollection(arrayListOf())
    ) : BaseMutableListAdapter<ShlFunctionDef>(filteredList) {
        private val reversedBg = (VisUI.getSkin().getDrawable("white") as TextureRegionDrawable)
            .tint(Color(27f / 255f, 161f / 255f, 226 / 255f, 0.20f))
        private val menu: PopupMenu

        init {
            selectionMode = SelectionMode.SINGLE
            menu = popupMenu {
                menuItem("Edit") {
                    onChange {
                        showEditFuncWindow(selection.first())
                    }
                }
            }
        }

        fun filterList(filterText: String) {
            filteredList.clear()
            funcsList.filter {
                if (filterText == "") true else it.name.contains(filterText, true)
            }.toCollection(filteredList)
            itemsChanged()
        }

        override fun setListView(view: ListView<ShlFunctionDef>?, viewListener: ListView<*>.ListAdapterListener?) {
            super.setListView(view, viewListener)
            filterList("")
        }

        override fun createView(def: ShlFunctionDef): VisTable {
            return table {
                touchable = Touchable.enabled
                table {
                    it.grow()
                    left()
                    if (def.reversed) background(reversedBg)
                    label("${def.returnType} ${def.name}")
                }
                addListener(menu.defaultInputListener)
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        if (tapCount == 2) {
                            listener.onFuncDoubleClick(def)
                        }
                    }
                })
            }
        }

        override fun updateView(view: VisTable, def: ShlFunctionDef) {
            val table = view.children[0] as VisTable
            val label = table.children[0] as VisLabel
            if (def.reversed) {
                table.background = reversedBg
            } else {
                table.background = null
            }
            label.setText("${def.returnType} ${def.name}")
        }
    }
}

interface FuncsWindowListener {
    fun onFuncEditWindowClose()

    fun onFuncDoubleClick(def: ShlFunctionDef)
}
