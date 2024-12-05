package mist.ui.dialog

import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.util.form.FormInputValidator
import com.kotcrab.vis.ui.widget.VisValidatableTextField
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.onChange
import ktx.vis.table
import ktx.vis.validator
import mist.shl.ShlType
import mist.shl.ShlTypes

class TypeEditWindow(val types: ShlTypes, val editMode: Boolean, val type: ShlType, val onCommit: () -> Unit) :
  VisWindow("Edit type") {
  private lateinit var nameField: VisValidatableTextField
  private lateinit var sizeField: VisValidatableTextField

  private val nameFieldValidator = object : FormInputValidator("Name must be unique") {
    override fun validate(input: String): Boolean {
      if (editMode && type.name == input) return true
      return types.getTypeByName(input) == null
    }
  }

  init {
    isModal = true
    isResizable = true
    addCloseButton()
    closeOnEscape()
    add(table(true) {
      left()
      defaults().left()

      label("Name: ")
      nameField = validatableTextField(type.name).cell(preferredWidth = 250f)
      nameField.focusField()
      row()

      if (typeHasSize()) {
        label("Size (bytes): ")
        sizeField = validatableTextField(getTypeSize().toString())
        row()
      }

      val validator = validator {
        notEmpty(nameField, "Name can't be empty")
        custom(nameField, nameFieldValidator)
        if (typeHasSize()) {
          notEmpty(sizeField, "Size can't be empty")
          integerNumber(sizeField, "Size must be a number")
          valueGreaterThan(sizeField, "Size must be greater than zero", 0f)
        }
      }

      table { _ ->
        label("") {
          validator.setMessageLabel(this)
        }.cell(growX = true)
        textButton("Save") {
          validator.addDisableTarget(this)
          cell(colspan = 1, align = Align.right)
          onChange {
            commit()
            fadeOut()
          }
        }
      }.cell(colspan = 2, growX = true)
    }).grow().pad(3f)
    pack()
    centerWindow()
  }

  private fun commit() {
    if (editMode) {
      type.name = nameField.text
      setTypeSize(sizeField.text.toInt())
      onCommit()
    } else {
      addNewType()
      onCommit()
    }
  }

  private fun addNewType() {
    when (type) {
      is ShlType.ShlPrimitive -> {
        types.addPrimitive(nameField.text, sizeField.text.toInt())
      }
      is ShlType.ShlEnum -> {
        types.addEnum(nameField.text, sizeField.text.toInt())
      }
      is ShlType.ShlStruct -> {
        types.addStruct(nameField.text)
      }
    }
  }

  private fun typeHasSize(): Boolean {
    if (type is ShlType.ShlStruct) return false
    return true
  }

  private fun setTypeSize(size: Int) {
    when (type) {
      is ShlType.ShlStruct -> {
        error("type does not support setting size")
      }
      is ShlType.ShlEnum -> {
        type.size = size
      }
      is ShlType.ShlPrimitive -> {
        type.size = size
      }
    }
  }

  private fun getTypeSize(): Int {
    return when (type) {
      is ShlType.ShlStruct -> {
        error("type does not support getting size")
      }
      is ShlType.ShlEnum -> {
        type.size
      }
      is ShlType.ShlPrimitive -> {
        type.size
      }
    }
  }
}
