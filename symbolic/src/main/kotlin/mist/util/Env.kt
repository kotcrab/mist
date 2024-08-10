package mist.util

import kio.util.child
import java.io.File

private const val pspSdkHomeVarName = "PSPSDK_HOME"
val pspSdkHome by lazy {
  getBaseDirectory(pspSdkHomeVarName).child("bin")
}

private fun getBaseDirectory(varName: String): File {
  val path = System.getenv(varName)
    ?: error("$varName environment variable not defined")
  val file = File(path)
  if (!file.exists()) {
    error("$varName points to non existing directory")
  }
  return file
}
