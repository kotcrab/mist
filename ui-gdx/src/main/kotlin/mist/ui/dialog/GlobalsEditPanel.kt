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
import com.kotcrab.vis.ui.widget.ListView
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisValidatableTextField
import kio.util.toHex
import ktx.actors.onChange
import ktx.vis.table
import ktx.vis.validator
import mist.shl.ShlGlobal
import mist.shl.ShlGlobals
import mist.shl.ShlTypes
import mist.ui.util.StaticListAdapter

/** @author Kotcrab */
class GlobalsEditPanel(private val types: ShlTypes, private val globals: ShlGlobals, val onCommit: () -> Unit) :
    VisTable(true) {
    private val memberAdapter = ShlGlobalsAdapter(globals.getGlobals())
    private val globalEditTable = GlobalEditTable()

    init {
        defaults().pad(3f)
        add(table(true) {
            left()
            label("Edit globals:")
        }).growX().row()
        add(ListView(memberAdapter).mainTable).grow().row()
        add(globalEditTable).growX()
    }

    inner class GlobalEditTable : VisTable() {
        private lateinit var validator: FormValidator
        private lateinit var typeField: VisValidatableTextField
        private lateinit var nameField: VisValidatableTextField
        private lateinit var addressField: VisValidatableTextField
        private lateinit var pointerCheck: VisCheckBox
        private lateinit var commentField: VisValidatableTextField

        init {
            initElemAddition()
        }

        fun initElemAddition() {
            clear()
            initContents(
                ShlGlobal(
                    0x0, "global_${globals.getGlobals().size}",
                    -1, false, ""
                ), false
            )
        }

        fun initElemEdition(global: ShlGlobal) {
            clear()
            initContents(global, true)
        }

        private fun initContents(global: ShlGlobal, editMode: Boolean) {
            val typeFieldValidator = object : FormInputValidator("Type must reference valid type") {
                override fun validate(input: String): Boolean {
                    return types.getTypeByName(input) != null
                }
            }
            val nameFieldValidator = object : FormInputValidator("Name must be unique") {
                override fun validate(input: String): Boolean {
                    if (editMode && global.name == input) return true
                    return !globals.getGlobals().map { it.name }.contains(input)
                }
            }
            val addressFieldValidator = object : FormInputValidator("Address must be valid hex value") {
                override fun validate(input: String): Boolean {
                    try {
                        Integer.decode(input)
                        return true
                    } catch (e: NumberFormatException) {
                        return false
                    }
                }
            }
            val addressUniqueValidator = object : FormInputValidator("Address must be unique") {
                override fun validate(input: String): Boolean {
                    val inputAddr = Integer.decode(input)
                    if (editMode && global.address == inputAddr) return true
                    return !globals.getGlobals().map { it.address }.contains(inputAddr)
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
                        types.getType(global.refTid)?.name
                            ?: "u32"
                    ).cell(preferredWidth = 220f)
                    label("Name:")
                    nameField = validatableTextField(global.name).cell(preferredWidth = 220f)
                    label("Address:")
                    addressField = validatableTextField(global.address.toHex()).cell(preferredWidth = 150f)
                    pointerCheck = checkBox("Pointer")
                    pointerCheck.isChecked = global.pointer
                    pointerCheck.onChange { validator.validate() }
                }
                row()
                table(true) { _ ->
                    label("Comment:")
                    commentField = validatableTextField(global.comment).cell(preferredWidth = 400f)

                    validator = validator {
                        notEmpty(nameField, "Name can't be empty")
                        custom(nameField, nameFieldValidator)
                        notEmpty(typeField, "Value can't be empty")
                        custom(typeField, typeFieldValidator)
                        notEmpty(addressField, "Address can't be empty")
                        custom(addressField, addressFieldValidator)
                        custom(addressField, addressUniqueValidator)
                    }
                    textButton("Confirm") {
                        validator.addDisableTarget(this)
                        cell(colspan = 1, align = Align.right)
                        onChange {
                            val refTid = types.getTypeByName(typeField.text)!!.tid
                            if (editMode) {
                                globals.editGlobal(
                                    global, Integer.decode(addressField.text), nameField.text, refTid,
                                    pointerCheck.isChecked, commentField.text
                                )
                            } else {
                                globals.addGlobal(
                                    Integer.decode(addressField.text), nameField.text, refTid,
                                    pointerCheck.isChecked, commentField.text
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

    inner class ShlGlobalsAdapter(globals: List<ShlGlobal>) : StaticListAdapter<ShlGlobal>(globals) {
        override fun createView(global: ShlGlobal): VisTable {
            return table {
                touchable = Touchable.enabled
                left()
                label("${types.getType(global.refTid)!!.name}${if (global.pointer) "*" else ""}").cell(preferredWidth = 250f)
                label(" ")
                label(global.name).cell(preferredWidth = 250f)
                label(" = ")
                label(global.address.toHex()).cell(preferredWidth = 60f)
                if (global.comment.isNotBlank()) {
                    label(" # ${global.comment}").cell(grow = true)
                } else {
                    label("").cell(grow = true)
                }
                userObject = table { _ ->
                    defaults().padLeft(3f)
                    textButton("Up") {
                        onChange {
                            globals.moveUp(global)
                            itemsChanged()
                            selectionManager.select(global)
                        }
                    }
                    textButton("Down") {
                        onChange {
                            globals.moveDown(global)
                            itemsChanged()
                            selectionManager.select(global)
                        }
                    }
                    textButton("Edit") {
                        onChange {
                            globalEditTable.initElemEdition(global)
                        }
                    }
                    textButton("Delete") {
                        onChange {
                            globals.delete(global)
                            itemsChanged()
                        }
                    }
                    isVisible = false
                }
            }
        }

        override fun itemsChanged() {
            super.itemsChanged()
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
