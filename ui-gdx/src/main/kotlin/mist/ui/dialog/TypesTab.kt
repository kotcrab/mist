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

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.util.adapter.ListSelectionAdapter
import com.kotcrab.vis.ui.widget.ListView
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import ktx.actors.onChange
import ktx.inject.Context
import ktx.vis.table
import mist.io.ProjectIO
import mist.shl.ShlType
import mist.shl.ShlType.*
import mist.ui.util.StaticListAdapter
import mist.util.DecompLog
import mist.util.logTag

/** @author Kotcrab */
class TypesTab(context: Context, val commit: () -> Unit) : Tab(true, false) {
  private val tag = logTag()

  private val log: DecompLog = context.inject()
  private val projectIO: ProjectIO = context.inject()
  private val appStage: Stage = context.inject()
  private val types = projectIO.getTypes()
  private val globals = projectIO.getGlobals()

  private val categorySelection = TypeCategoryAdapter()
  private val content: VisTable
  private lateinit var typesPanel: VisTable
  private lateinit var typeAdapter: TypeAdapter
  private lateinit var typeView: ListView<ShlType>
  private lateinit var typeEditPanel: VisTable
  private var needsCommit = false

  init {
    content = table {
      table(true) { cell ->
        cell.growY().minWidth(250f).pad(3f)
        defaults().left()
        left().top()
        label("Category:")
        row()
        listView(categorySelection) { listCell ->
          listCell.growX()
        }
        row()
        addSeparator()
        label("Types:")
        row()
        typesPanel = table {
          it.grow()
        }
      }
      addSeparator(true)
      typeEditPanel = table(true) {
        it.grow()
      }
    }
    categorySelection.selectionManager.apply {
      listener = object : ListSelectionAdapter<TypeCategory, VisTable>() {
        override fun selected(item: TypeCategory, view: VisTable) {
          switchTypeView(item)
        }
      }
      select(TypeCategory.Structs)
    }
  }

  override fun save(): Boolean {
    projectIO.saveProject()
    isDirty = false
    return true
  }

  override fun dispose() {
    log.info(tag, "dispose $tag")
    super.dispose()
  }

  override fun getContentTable(): Table {
    return content
  }

  override fun getTabTitle(): String {
    return "Types"
  }

  private fun switchTypeView(item: TypeCategory) {
    log.trace(tag, "switch to '$item' category")
    typesPanel.clear()

    if (item == TypeCategory.Globals) {
      typesPanel.add(VisLabel("Edit globals on the right panel"))
      typeEditPanel.clear()
      typeEditPanel.add(GlobalsEditPanel(types, globals, { dirty() })).grow()
      return
    }

    switchTypeEdit(null)
    val (adapter, type) = when (item) {
      TypesTab.TypeCategory.Primitives -> {
        Pair(TypeAdapter(types.getPrimitives()), ShlPrimitive(0, "", 0x4))
      }
      TypesTab.TypeCategory.Enums -> {
        Pair(TypeAdapter(types.getEnums()), ShlEnum(0, "", 0x4))
      }
      TypesTab.TypeCategory.Structs -> {
        Pair(TypeAdapter(types.getStructs()), ShlStruct(0, ""))
      }
      TypesTab.TypeCategory.Globals -> error("this type should have been already handled")
    }
    typeAdapter = adapter
    typeView = ListView(typeAdapter)
    typesPanel.add(typeView.mainTable).grow().row()
    val addButton = VisTextButton("Add")
    addButton.onChange {
      appStage.addActor(TypeEditWindow(types, false, type, { typesChanged() }).fadeIn())
    }
    typesPanel.add(addButton).growX()
    typeAdapter.selectionManager.listener = object : ListSelectionAdapter<ShlType, VisTable>() {
      override fun selected(item: ShlType, view: VisTable) {
        switchTypeEdit(item)
      }
    }
  }

  private fun switchTypeEdit(type: ShlType?) {
    typeEditPanel.clear()
    when (type) {
      null -> {
        typeEditPanel.add(VisLabel("Select type to edit it"))
      }
      is ShlType.ShlPrimitive -> {
        typeEditPanel.add(VisLabel("Primitives do not have extra properties"))
      }
      is ShlType.ShlEnum -> {
        typeEditPanel.add(EnumEditPanel(type, { dirty() })).grow()
      }
      is ShlType.ShlStruct -> {
        typeEditPanel.add(StructEditPanel(types, type, { dirty() })).grow()
      }
    }
  }

  private fun typesChanged() {
    typeAdapter.itemsChanged()
    dirty()
  }

  override fun dirty() {
    super.dirty()
    needsCommit = true
  }

  override fun onHide() {
    super.onHide()
    if (needsCommit) {
      commit()
      needsCommit = false
    }
  }

  private class TypeAdapter(types: List<ShlType>) : StaticListAdapter<ShlType>(types) {
    override fun createView(type: ShlType): VisTable {
      return table {
        touchable = Touchable.enabled
        left()
        label(type.toString())
      }
    }
  }

  private class TypeCategoryAdapter : StaticListAdapter<TypeCategory>(TypeCategory.values().toList()) {
    override fun createView(type: TypeCategory): VisTable {
      return table {
        touchable = Touchable.enabled
        left()
        label(type.toString())
      }
    }
  }

  enum class TypeCategory {
    Primitives, Enums, Structs, Globals
  }
}
