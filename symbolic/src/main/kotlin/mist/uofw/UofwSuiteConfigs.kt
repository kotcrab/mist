package mist.uofw

import mist.suite.SuiteConfig

fun uofwSuiteConfigForName(moduleName: String): SuiteConfig {
  return uofwSuiteConfigs.firstOrNull { it.moduleName == moduleName }
    ?: error("No suite for module: $moduleName")
}

val uofwSuiteConfigs = listOf(
  audioSuiteConfig,
  isofsSuiteConfig,
)
