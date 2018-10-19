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

package mist.ui.dialog

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.util.form.FormInputValidator
import com.kotcrab.vis.ui.widget.ListView
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import ktx.actors.onChange
import ktx.vis.table
import ktx.vis.validator
import mist.shl.ShlEnumMembers
import mist.shl.ShlType.ShlEnum
import mist.ui.util.StaticMutableListAdapter
import mist.util.swap

/** @author Kotcrab */
class EnumEditPanel(val enum: ShlEnum, val onCommit: () -> Unit) : VisTable(true) {
    private val memberAdapter = ShlEnumMemberAdapter(enum.members)
    private val enumEditTable = EnumEditTable()

    init {
        defaults().pad(3f)
        add(VisLabel("Edit enum: $enum")).growX().row()
        add(ListView(memberAdapter).mainTable).grow().row()
        add(enumEditTable).growX()
    }

    inner class EnumEditTable : VisTable() {
        init {
            initElemAddition()
        }

        fun initElemAddition() {
            clear()
            val maxId = enum.members.maxBy { it.value }?.value ?: -1
            initContents(ShlEnumMembers("member${enum.members.size}", maxId + 1), false)
        }

        fun initElemEdition(members: ShlEnumMembers) {
            clear()
            initContents(members, true)
        }

        private fun initContents(members: ShlEnumMembers, editMode: Boolean) {
            val nameFieldValidator = object : FormInputValidator("Name must be unique") {
                override fun validate(input: String): Boolean {
                    if (editMode && members.name == input) return true
                    return !enum.members.map { it.name }.contains(input)
                }
            }

            val valueFieldValidator = object : FormInputValidator("Value must be unique") {
                override fun validate(input: String): Boolean {
                    if (editMode && members.value.toString() == input) return true
                    return !enum.members.map { it.value }.contains(input.toInt())
                }
            }

            add(table(true) {
                left()
                defaults().left()
                if (editMode) {
                    label("Edit element: ")
                } else {
                    label("Add element: ")
                }
                val nameField = validatableTextField(members.name).cell(preferredWidth = 250f)
                label(" = ")
                val valueField = validatableTextField(members.value.toString())
                val validator = validator {
                    notEmpty(nameField, "Name can't be empty")
                    custom(nameField, nameFieldValidator)
                    notEmpty(valueField, "Value can't be empty")
                    integerNumber(valueField, "Value must be a number")
                    custom(valueField, valueFieldValidator)
                }
                textButton("Confirm") {
                    validator.addDisableTarget(this)
                    cell(colspan = 1, align = Align.right)
                    onChange {
                        if (editMode) {
                            members.name = nameField.text
                            members.value = valueField.text.toInt()
                        } else {
                            enum.members.add(ShlEnumMembers(nameField.text, valueField.text.toInt()))
                        }
                        memberAdapter.itemsChanged()
                        initElemAddition()
                    }
                }
            }).growX()
        }
    }

    inner class ShlEnumMemberAdapter(private val members: MutableList<ShlEnumMembers>) : StaticMutableListAdapter<ShlEnumMembers>(members) {
        override fun createView(members: ShlEnumMembers): VisTable {
            return table {
                touchable = Touchable.enabled
                left()
                label(members.name).cell(preferredWidth = 200f)
                label(" = ")
                label("${members.value}").cell(grow = true)
                userObject = table { _ ->
                    defaults().padLeft(3f)
                    textButton("Up") {
                        onChange {
                            val newIndex = this@ShlEnumMemberAdapter.members.indexOf(members) - 1
                            if (newIndex >= 0) {
                                this@ShlEnumMemberAdapter.members.swap(newIndex, newIndex + 1)
                            }
                            itemsChanged()
                            selectionManager.select(members)
                        }
                    }
                    textButton("Down") {
                        onChange {
                            val newIndex = this@ShlEnumMemberAdapter.members.indexOf(members) + 1
                            if (newIndex < this@ShlEnumMemberAdapter.members.size) {
                                this@ShlEnumMemberAdapter.members.swap(newIndex, newIndex - 1)
                            }
                            itemsChanged()
                            selectionManager.select(members)
                        }
                    }
                    textButton("Edit") {
                        onChange {
                            enumEditTable.initElemEdition(members)
                        }
                    }
                    textButton("Delete") {
                        onChange {
                            val index = this@ShlEnumMemberAdapter.members.indexOf(members)
                            this@ShlEnumMemberAdapter.members.remove(members)
                            itemsChanged()
                            if (this@ShlEnumMemberAdapter.members.size > 0) {
                                selectionManager.select(this@ShlEnumMemberAdapter.members[Math.min(this@ShlEnumMemberAdapter.members.lastIndex, index)])
                            }
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
