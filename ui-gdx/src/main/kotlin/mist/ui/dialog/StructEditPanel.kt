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

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.util.form.FormInputValidator
import com.kotcrab.vis.ui.util.form.FormValidator
import com.kotcrab.vis.ui.widget.*
import kio.util.swap
import kio.util.toHex
import kio.util.toWHex
import ktx.actors.onChange
import ktx.vis.table
import ktx.vis.validator
import mist.shl.ShlStructField
import mist.shl.ShlType
import mist.shl.ShlTypes
import mist.ui.util.StaticMutableListAdapter

/** @author Kotcrab */
class StructEditPanel(val types: ShlTypes, val struct: ShlType.ShlStruct, val onCommit: () -> Unit) : VisTable(true) {
  private val memberAdapter = ShlStructFieldAdapter(struct.fields)
  private val structEditTable = StructEditTable()
  private lateinit var sizeLabel: VisLabel

  init {
    defaults().pad(3f)
    add(table(true) {
      left()
      label("Edit struct: $struct")
      sizeLabel = label("")
    }).growX().row()
    add(ListView(memberAdapter).mainTable).grow().row()
    add(structEditTable).growX()
    updateSizeLabel()
  }

  inner class StructEditTable : VisTable() {
    lateinit var validator: FormValidator
    lateinit var typeField: VisValidatableTextField
    lateinit var nameField: VisValidatableTextField
    lateinit var pointerCheck: VisCheckBox
    lateinit var arraySizeField: VisValidatableTextField
    lateinit var commentField: VisValidatableTextField

    init {
      initElemAddition()
    }

    fun initElemAddition() {
      clear()
      initContents(
        ShlStructField(
          -1, false, "field_${String.format("%X", types.sizeOf(struct.tid))}",
          1, ""
        ), false
      )
    }

    fun initElemEdition(field: ShlStructField) {
      clear()
      initContents(field, true)
    }

    private fun initContents(field: ShlStructField, editMode: Boolean) {
      val typeFieldValidator = object : FormInputValidator("Type must reference valid type") {
        override fun validate(input: String): Boolean {
          return if (pointerCheck.isChecked) { // pointer can't create circular reference
            types.getTypeByName(input) != null
          } else {
            types.getTypeByName(input) != null && types.checkStructFieldAdditionValid(
              struct,
              input
            ) == false
          }
        }
      }

      val nameFieldValidator = object : FormInputValidator("Name must be unique") {
        override fun validate(input: String): Boolean {
          if (editMode && field.name == input) return true
          return !struct.fields.map { it.name }.contains(input)
        }
      }

      add(table(true) {
        left()
        defaults().left()
        if (editMode) {
          label("Edit element")
        } else {
          label("Add element")
        }
        row()
        table(true) {
          label("Type:")
          typeField = validatableTextField(
            types.getType(field.refTid)?.name
              ?: "u32"
          ).cell(preferredWidth = 220f)
          label("Name:")
          nameField = validatableTextField(field.name).cell(preferredWidth = 220f)
          pointerCheck = checkBox("Pointer")
          pointerCheck.isChecked = field.pointer
          pointerCheck.onChange { validator.validate() }
        }
        row()
        table(true) { _ ->
          label("Array size:")
          arraySizeField = validatableTextField(field.arraySize.toString()).cell(preferredWidth = 30f)
          label("Comment:")
          commentField = validatableTextField(field.comment).cell(preferredWidth = 400f)

          validator = validator {
            notEmpty(nameField, "Name can't be empty")
            custom(nameField, nameFieldValidator)
            notEmpty(typeField, "Value can't be empty")
            custom(typeField, typeFieldValidator)
            integerNumber(arraySizeField, "Array size must be a number")
            valueGreaterThan(arraySizeField, "Array size must greater than 0", 0f)
          }
          textButton("Confirm") {
            validator.addDisableTarget(this)
            cell(colspan = 1, align = Align.right)
            onChange {
              val refTid = types.getTypeByName(typeField.text)!!.tid
              if (editMode) {
                field.refTid = refTid
                field.pointer = pointerCheck.isChecked
                field.name = nameField.text
                field.arraySize = arraySizeField.text.toInt()
                field.comment = commentField.text
              } else {
                struct.fields.add(
                  ShlStructField(
                    refTid, pointerCheck.isChecked, nameField.text,
                    arraySizeField.text.toInt(), commentField.text
                  )
                )
              }
              memberAdapter.itemsChanged()
              initElemAddition()
            }
          }
        }
      }).growX()
    }
  }

  private fun updateSizeLabel() {
    sizeLabel.setText("sizeOf = ${types.sizeOf(struct.tid).toHex()}")
  }

  inner class ShlStructFieldAdapter(private val fields: MutableList<ShlStructField>) :
    StaticMutableListAdapter<ShlStructField>(fields) {
    override fun createView(field: ShlStructField): VisTable {
      return table {
        touchable = Touchable.enabled
        left()
        label("0x${types.sizeOfStructUntil(struct, field).toWHex()}")
        label(" ")
        label("0x${types.sizeOfStructField(field).toWHex()}")
        label(" ")
        label("${types.getType(field.refTid)!!.name}${if (field.pointer) "*" else ""}").cell(preferredWidth = 250f)
        label(" ")
        label("${field.name}${if (field.arraySize > 1) "[${field.arraySize}]" else ""}").cell(preferredWidth = 250f)
        if (field.comment.isNotBlank()) {
          label(" # ${field.comment}").cell(grow = true)
        } else {
          label("").cell(grow = true)
        }
        userObject = table { _ ->
          defaults().padLeft(3f)
          textButton("Up") {
            onChange {
              val newIndex = fields.indexOf(field) - 1
              if (newIndex >= 0) {
                fields.swap(newIndex, newIndex + 1)
              }
              itemsChanged()
              selectionManager.select(field)
            }
          }
          textButton("Down") {
            onChange {
              val newIndex = fields.indexOf(field) + 1
              if (newIndex < fields.size) {
                fields.swap(newIndex, newIndex - 1)
              }
              itemsChanged()
              selectionManager.select(field)
            }
          }
          textButton("Edit") {
            onChange {
              structEditTable.initElemEdition(field)
            }
          }
          textButton("Delete") {
            onChange {
              val index = fields.indexOf(field)
              fields.remove(field)
              itemsChanged()
              if (fields.size > 0) {
                selectionManager.select(fields[Math.min(fields.lastIndex, index)])
              }
            }
          }
          isVisible = false
        }
      }
    }

    override fun itemsChanged() {
      super.itemsChanged()
      updateSizeLabel()
      onCommit()
    }

    override fun selectView(view: VisTable) {
      super.selectView(view)
      (view.userObject as Actor).isVisible = true
    }

    override fun deselectView(view: VisTable) {
      super.deselectView(view)
      (view.userObject as Actor).isVisible = false
    }
  }
}
