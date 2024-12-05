package mist.ui.dialog

import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.util.TableUtils
import com.kotcrab.vis.ui.util.form.FormInputValidator
import ktx.actors.onChange
import ktx.vis.validator
import ktx.vis.window
import mist.shl.ShlArgumentDef
import mist.shl.ShlFunctionDef
import mist.shl.shlArgRegisters

fun createFuncArgEditWindow(
  func: ShlFunctionDef, def: ShlArgumentDef, editMode: Boolean,
  commit: (ShlArgumentDef) -> Unit
) = window("Edit argument") {
  val typeFieldValidator = object : FormInputValidator("Only one field can be vararg") {
    override fun validate(input: String): Boolean {
      if (!input.endsWith("...")) return true
      if (editMode && def.type.endsWith("...") && input.endsWith("...")) return true
      return !func.arguments.map { it.type }.any { it.endsWith("...") }
    }
  }

  val regFieldValidator = object : FormInputValidator("Must be valid and unique reg value") {
    override fun validate(input: String): Boolean {
      if (shlArgRegisters.contains(input) == false) return false
      return !func.arguments.map { it.register }.contains(input)
    }
  }

  val nameFieldValidator = object : FormInputValidator("Name must be unique") {
    override fun validate(input: String): Boolean {
      if (editMode && def.name == input) return true
      return !func.arguments.map { it.name }.contains(input)
    }
  }

  isModal = true
  TableUtils.setSpacingDefaults(this)
  addCloseButton()
  closeOnEscape()
  table(true) {
    it.grow().pad(3f)
    left()
    defaults().left()

    label("type")
    label("name")
    label(" = ")
    label("register")
    row()

    val typeField = validatableTextField(def.type)
    val nameField = validatableTextField(def.name)
    label(" = ")
    val registerField = validatableTextField(def.register)
    row()
    if (editMode) {
      registerField.isDisabled = true
    }

    val validator = validator {
      notEmpty(typeField, "Type can't be empty")
      custom(typeField, typeFieldValidator)
      notEmpty(nameField, "Name can't be empty")
      custom(nameField, nameFieldValidator)
      if (editMode == false) {
        custom(registerField, regFieldValidator)
      }
    }

    label("") {
      validator.setMessageLabel(this)
    }.cell(colspan = 3, grow = true)
    textButton("Save") {
      validator.addDisableTarget(this)
      cell(colspan = 1, align = Align.right)
      onChange {
        commit(ShlArgumentDef(typeField.text, nameField.text, registerField.text))
        this@window.fadeOut()
      }
    }
  }
  pack()
  centerWindow()
}
