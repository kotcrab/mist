package mist

import mist.module.TestRunner
import mist.module.readModuleExports
import mist.uofw.audioTestSuite
import java.io.File

fun main(args: Array<String>) {
  val moduleExports = readModuleExports(File(args[0]))
  val outDir = File(args[1])
  val suite = audioTestSuite
  TestRunner(moduleExports, outDir, suite).run()
}
