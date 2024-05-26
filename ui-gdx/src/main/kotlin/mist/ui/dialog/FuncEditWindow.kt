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
import com.kotcrab.vis.ui.util.TableUtils
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter
import com.kotcrab.vis.ui.widget.*
import kio.util.toWHex
import ktx.actors.onChange
import ktx.inject.Context
import ktx.vis.menuItem
import ktx.vis.popupMenu
import ktx.vis.table
import mist.io.ProjectIO
import mist.shl.ShlArgumentDef
import mist.shl.ShlFunctionDef
import mist.ui.util.StaticMutableListAdapter

/** @author Kotcrab */

class FunctionEditWindow(
  context: Context,
  private val funcsPanel: FuncsPanel,
  private val listener: FuncsWindowListener,
  private val func: ShlFunctionDef
) : VisWindow("Function Edit") {
  private val projectIO: ProjectIO = context.inject()
  private val appStage: Stage = context.inject()
  private val argAdapter = ArgumentAdapter(appStage, func, func.arguments)

  private lateinit var nameTextField: VisTextField
  private lateinit var returnTypeField: VisTextField
  private lateinit var reversedCheck: VisCheckBox

  init {
    isModal = true
    TableUtils.setSpacingDefaults(this)
    addCloseButton()
    closeOnEscape()
    add(table {
      left()
      defaults().left()
      table {
        label("Function: ")
        nameTextField = textField(func.name).cell(growX = true)
      }.cell(growX = true, row = true)
      label("Offset: ${func.offset.toWHex()}, length: ${func.len.toWHex()}").cell(row = true)
      horizontalGroup {
        label("Return type: ")
        returnTypeField = textField(func.returnType)
      }.cell(row = true)
      reversedCheck = checkBox("Reversed").cell(row = true)
      reversedCheck.isChecked = func.reversed
      label("Arguments:").cell(row = true)
      listView(argAdapter) {
        it.grow().row()
      }
      textButton("Add argument") {
        onChange { showArgArgumentDialog() }
      }
    }).grow().pad(3f)
    setSize(500f, 300f)
    centerWindow()
  }

  private fun showArgArgumentDialog() {
    val defaultArg = ShlArgumentDef("void*", "arg", "a0")
    stage.addActor(createFuncArgEditWindow(func, defaultArg, false, { newArg ->
      func.arguments.add(newArg)
      argAdapter.itemsChanged()
    }).fadeIn())
  }

  override fun close() {
    if (returnTypeField.isEmpty) {
      showErrorDialog("Return type can't be empty")
      return
    }
    if (nameTextField.isEmpty) {
      showErrorDialog("Function name can't be empty")
      return
    }
    if (func.name != nameTextField.text && projectIO.getFuncDefByName(nameTextField.text) != null) {
      showErrorDialog("Function name must be unique")
      return
    }
    func.name = nameTextField.text
    func.returnType = returnTypeField.text
    func.reversed = reversedCheck.isChecked
    closeDialog()
  }

  private fun showErrorDialog(msg: String) {
    val dialog = Dialogs.showOptionDialog(
      appStage,
      "Error",
      msg,
      Dialogs.OptionDialogType.YES_CANCEL,
      object : OptionDialogAdapter() {
        override fun yes() {
          closeDialog()
        }
      })
    dialog.setYesButtonText("Discard edits")
    dialog.setCancelButtonText("Continue editing")
  }

  private fun closeDialog() {
    super.close()
    listener.onFuncEditWindowClose()
    funcsPanel.refreshList()
  }
}

private class ArgumentAdapter(appStage: Stage, func: ShlFunctionDef, val list: MutableList<ShlArgumentDef>) :
  StaticMutableListAdapter<ShlArgumentDef>(list) {

  private val menu: PopupMenu

  init {
    selectionMode = SelectionMode.SINGLE
    menu = popupMenu {
      menuItem("Edit") {
        onChange {
          val currentArg = selection.first()
          appStage.addActor(createFuncArgEditWindow(func, currentArg, true, { newArg ->
            list.remove(currentArg)
            list.add(newArg)
            itemsChanged()
          }).fadeIn())
        }
      }
      menuItem("Delete") {
        onChange {
          list.remove(selection.first())
          itemsChanged()
        }
      }
    }
  }

  override fun itemsChanged() {
    sort { o1, o2 -> o1.register.compareTo(o2.register) }
    super.itemsChanged()
  }

  override fun createView(def: ShlArgumentDef): VisTable {
    return table {
      touchable = Touchable.enabled
      left()
      label("${def.type} ${def.name} = ${def.register}")
      addListener(menu.defaultInputListener)
    }
  }
}
