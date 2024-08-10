package mist

import kio.util.child
import mist.suite.Suite
import mist.uofw.compileSceFunctionLibrary
import mist.uofw.uofwSuiteConfigForName
import mist.util.pspSdkHome
import java.io.File

fun main(args: Array<String>) {
  val testedModule = args[0]
  val uofwDir = File(args[1])
  val modelsDir = File(args[2])
  val outDir = File(args[3])
  val libDir = File(args[4])

  compileSceFunctionLibrary(pspSdkHome, libDir)
  Suite(uofwDir, uofwSuiteConfigForName(testedModule))
    .compareFromModels(modelsDir.child(testedModule), outDir)

  println("Done")
}
