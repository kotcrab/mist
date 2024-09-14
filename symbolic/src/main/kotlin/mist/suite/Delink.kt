package mist.suite

import kio.util.child
import java.io.File

class Delink(
  private val suite: Suite,
  private val suiteConfig: SuiteConfig,
  private val outDir: File
) {
  fun execute() {
    suite.fwModule.delink(outDir.child("${suiteConfig.moduleName}_fw.o"), suiteConfig.globals.map { it.name }.toSet())
  }
}
