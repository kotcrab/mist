package mist.uofw

import mist.suite.suiteConfig
import mist.symbolic.Expr
import mist.symbolic.ResultFunctionHandler

val audioSuiteConfig = suiteConfig("audio") {
  global("g_audio")
  global("g_audioEvent")

  test("module_start")
  test("updateAudioBuf")
  test("audioMixerThread")
  test("audioInputThread")
  test("audioIntrHandler")
  test("audioEventHandler")
  test("audioOutputDmaCb")
  test("audioSRCOutputDmaCb")
  test("audioInputDmaCb")

  functionLibrary { moduleMemory ->
    sceSymbolicFunctionLibrary(moduleMemory).extendWith(
      listOf(
        ResultFunctionHandler("updateAudioBuf") { Expr.ZERO }
      )
    )
  }

  configureContext {
    repeat(8) {
      writeSymbolField("g_audio.chans[$it].curSampleCnt", 5)
    }
  }

  ignoreComparingArgsOf("memset", 0)
  ignoreComparingArgsOf("sceKernelDmaOpSetCallback", 1)
  ignoreComparingArgsOf("sceKernelCreateEventFlag", 0)
  ignoreComparingArgsOf("sceKernelCreateThread", 0, 1)
  ignoreComparingArgsOf("sceKernelRegisterIntrHandler", 2)
}
