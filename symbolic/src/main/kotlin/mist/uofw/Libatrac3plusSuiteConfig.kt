package mist.uofw

import mist.suite.suiteConfig
import mist.symbolic.SymbolicFunctionHandler

val libatrac3plusSuiteConfig = suiteConfig("libatrac3plus") {
  // Setup

  global("g_3E74")
  global("g_3E88")
  global("g_at3PlusGUID")
  global("g_3F08")
  global("g_edramAddr")
  global("g_atracIds", init = false)
  global("g_needMemAT3", init = false)
  global("g_needMemAT3plus", init = false)
  initContextsWithGlobals()

  requireSourceData(0x88003F1Cu..0x88003F54u) // switch tables

  functionLibrary { moduleMemory ->
    sceSymbolicFunctionLibrary(moduleMemory).extendWith(
      listOf(
        SymbolicFunctionHandler("loadWaveFile"),
        SymbolicFunctionHandler("setHalfwayBuffer"),
        SymbolicFunctionHandler("setMOutHalfwayBuffer"),
        SymbolicFunctionHandler("openAA3AndGetID"),
        SymbolicFunctionHandler("parseAA3"),
        SymbolicFunctionHandler("setAtracFileInfo"),
        SymbolicFunctionHandler("decodeFrame"),
        SymbolicFunctionHandler("resetId"),
      )
    )
  }
  overrideElfFunctionName("module_start", "sceAtracStartEntry")

  proveAllFunctions()

  ignoreComparingArgsOf("loadWaveFile", 1)
  ignoreComparingArgsOf("setHalfwayBuffer", 4)
  ignoreComparingArgsOf("setMOutHalfwayBuffer", 4)
  ignoreComparingArgsOf("openAA3AndGetID", 3)

  // Long-running tests
  exclude("decodeFrame")
  exclude("loadWaveFile")

  // Tests

  test("loadWaveFile") {
    configureContext {
      assume { a0 eq 0x1000 }
      assume { a1 eq allocate() }
      assume { a2 eq allocate() }
    }
    ignoreComparingArgsOf("readWaveData", 1)
  }

  test("parseAA3") {
    configureContext {
      assume { a1 eq allocate() }
      assume { a3 eq allocate() }
    }
    ignoreComparingArgsOf("sub_3A18", 1)
    ignoreComparingArgsOf("sub_3AA0", 1)
    ignoreComparingArgsOf("sub_3B14", 1)
    ignoreComparingArgsOf("sub_3B54", 1)
  }

  test("decodeFrame") {
    configureContext {
      assume { a0 eq allocate(0x10000) }
      assume { a1 eq allocate(0x10000) }
      assume { a2 eq allocate(0x10000) }
      assume { a3 eq allocate(0x10000) }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("sceKernelMemcpy"),
        )
      )
    }
    ignoreComparingArgsOf("sceKernelMemcpy", 1)
  }

  test("setHalfwayBuffer") {
    configureContext {
      assume { a0 eq allocate() }
      assume { a1 eq allocate() }
      assume { t0 eq allocate() }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("sceKernelMemcpy"),
        )
      )
    }
  }

  test("setMOutHalfwayBuffer") {
    configureContext {
      assume { a0 eq allocate() }
      assume { a1 eq allocate() }
      assume { t0 eq allocate() }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("sceKernelMemcpy"),
        )
      )
    }
  }

  test("openAA3AndGetID") {
    configureContext {
      assume { a0 eq allocate() }
      assume { a3 eq allocate() }
    }
  }

  test("setAtracFileInfo") {
    configureContext {
      assume { a0 eq allocate() }
      assume { a1 eq allocate() }
    }
  }

  test("resetId") {
    configureContext {
      assume { a0 eq allocate() }
    }
    ignoreComparingArgsOf("decodeFrame", 3)
  }

  test("getBufferInfo") {
    configureContext {
      val info = create("SceAtracIdInfo", "info")
      info.writeField("state", 3)
      assume { a0 eq info.address }
      assume { a2 eq allocate() }
    }
  }

  test("sub_31B4") {
    configureContext {
      assume { a0 eq allocate() }
    }
  }

  test("sub_2DF8") {
    configureContext {
      assume { a0 eq allocate() }
    }
  }

  test("sub_2DB8") {
    configureContext {
      assume { a0 eq allocate() }
    }
  }

  test("isValidState") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("initAT3plusDecoder") {
    configureContext {
      assume { a0 eq allocate() }
      assume { a1 eq allocate() }
    }
  }

  test("resetPlayPos") {
    configureContext {
      assume { a0 eq allocate() }
    }
    ignoreComparingArgsOf("getBufferInfo", 2)
  }

  test("getOffFromSample") {
    configureContext {
      assume { a0 eq allocate() }
    }
  }

  test("getFrameFromSample") {
    configureContext {
      assume { a0 eq allocate() }
    }
  }

  // Exported functions

  test("sceAtracReinit") {
    configureContext {
      assume { a0 le 4 }
      assume { a1 le 4 }
    }
  }

  test("sceAtracSetHalfwayBuffer") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("sceAtracDecodeData") {
    configureContext {
      assume { a1 eq allocate() }
      assume { a2 eq allocate() }
      assume { a3 eq allocate() }
      assume { t0 eq allocate() }
    }
    functionLibrary {
      it.extendWith(listOf(SymbolicFunctionHandler("getRemainFrame")))
    }
  }

  test("sceAtracGetRemainFrame") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("sceAtracGetStreamDataInfo") {
    configureContext {
      assume { a1 eq allocate() }
      assume { a2 eq allocate() }
      assume { a3 eq allocate() }
    }
  }

  test("sceAtracGetSecondBufferInfo") {
    configureContext {
      assume { a1 eq allocate() }
      assume { a2 eq allocate() }
    }
  }

  test("sceAtracSetSecondBuffer") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("sceAtracGetNextSample") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("sceAtracGetSoundSample") {
    configureContext {
      assume { a1 eq allocate() }
      assume { a2 eq allocate() }
      assume { a3 eq allocate() }
    }
  }

  test("sceAtracGetNextDecodePosition") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("sceAtracGetLoopStatus") {
    configureContext {
      assume { a1 eq allocate() }
      assume { a2 eq allocate() }
    }
  }

  test("sceAtracResetPlayPosition") {
    ignoreComparingArgsOf("getBufferInfo", 2)
  }

  test("sceAtracGetBufferInfoForReseting") {
    configureContext {
      assume { a2 eq allocate() }
    }
  }

  test("sceAtracLowLevelInitDecoder") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("sceAtracLowLevelDecode") {
    configureContext {
      assume { a1 eq allocate() }
      assume { a2 eq allocate() }
      assume { a3 eq allocate() }
      assume { t0 eq allocate() }
    }
  }

  test("sceAtracSetData") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("sceAtracSetDataAndGetID") {
    configureContext {
      assume { a0 eq allocate() }
    }
  }

  test("sceAtracSetAA3DataAndGetID") {
    configureContext {
      assume { a0 eq allocate() }
    }
  }

  test("sceAtracGetChannel") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("sceAtracGetOutputChannel") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("sceAtracGetMaxSample") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("sceAtracGetBitrate") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }

  test("sceAtracGetInternalErrorInfo") {
    configureContext {
      assume { a1 eq allocate() }
    }
  }
}
