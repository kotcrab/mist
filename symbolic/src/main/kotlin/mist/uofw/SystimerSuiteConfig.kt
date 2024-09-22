package mist.uofw

import mist.suite.suiteConfig

val systimerSuiteConfig = suiteConfig("systimer") {
  // Setup

  global("g_timerCB")
  global("STimerRegSave")
  initContextsWithGlobals()

  functionLibrary { moduleMemory ->
    sceSymbolicFunctionLibrary(moduleMemory)
  }

  overrideElfFunctionName("module_start", "SysTimerInit")
  overrideElfFunctionName("module_reboot_before", "SysTimerEnd")

  proveAllFunctions()

  ignoreComparingArgsOf("sceKernelRegisterIntrHandler", 2)
  ignoreComparingArgsOf("sceKernelRegisterSuspendHandler", 1)
  ignoreComparingArgsOf("sceKernelRegisterResumeHandler", 1)

  configureContext {
    allocateAt(0xBC500000.toInt(), 0x40f, name = "U_HW_TIMER")
    allocateAt(0x9C500000.toInt(), 0x40f, name = "HW_TIMER")
  }

  // Tests

  test("systimerhandler") {
    configureContext {
      assume { a1 eq allocate("SceSTimerInfo", "timer") }
    }
  }
  test("SysTimerInit")
  test("SysTimerEnd")
  test("suspendSTimer")
  test("resumeSTimer")
  test("sceSTimerGetCount") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }
}
