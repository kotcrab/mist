package mist

import kio.util.child
import mist.suite.Suite
import mist.uofw.uofwSuiteConfigForName
import java.io.File

fun main(args: Array<String>) {
  val testedModule = args[0]
  val uofwDir = File(args[1])
  val modelsDir = File(args[2])
  val outDir = File(args[3])

  Suite(uofwDir, uofwSuiteConfigForName(testedModule))
    .compareFromModels(modelsDir.child(testedModule), outDir)

  println("Done")
}
