package mist.module

import java.io.File

private const val EXPORT_PREFIX = "PSP_EXPORT_FUNC_NID("

fun readModuleExports(file: File): List<ModuleExport> {
  return file.readLines()
    .filter { it.startsWith(EXPORT_PREFIX) }
    .map {
      ModuleExport(
        name = it.substringAfter(EXPORT_PREFIX).substringBefore(",").trim(),
        nid = it.substringAfter(",").substringBeforeLast(")").trim(),
      )
    }
}

data class ModuleExport(
  val name: String,
  val nid: String
)
