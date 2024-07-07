package mist

import kio.util.child
import mist.suite.Suite
import mist.uofw.uofwSuiteConfigForName
import java.io.File

fun main(args: Array<String>) {
  val testedModule = args[0]
  val uofwDir = File(args[1])
  val modelsOutDir = File(args[2])

  Suite(uofwDir, uofwSuiteConfigForName(testedModule))
    .generateModels(modelsOutDir.child(testedModule))

  println("Done")
}
