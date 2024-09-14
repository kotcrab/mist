package mist

import mist.suite.Suite
import mist.uofw.uofwSuiteConfigForName
import java.io.File

fun main(args: Array<String>) {
  val testedModule = args[0]
  val uofwDir = File(args[1])
  val outDir = File(args[2])

  Suite(uofwDir, uofwSuiteConfigForName(testedModule), checkFunctionLibrary = false)
    .delink(outDir)

  println("Done")
}
