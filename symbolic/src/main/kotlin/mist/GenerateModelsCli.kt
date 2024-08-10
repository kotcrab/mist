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
  val modelsOutDir = File(args[2])
  val libDir = File(args[3])

  compileSceFunctionLibrary(pspSdkHome, libDir)
  Suite(uofwDir, uofwSuiteConfigForName(testedModule))
    .generateModels(modelsOutDir.child(testedModule))

  println("Done")
}
