package mist.uofw

import mist.module.testSuite
import mist.simulator.ResultFunctionHandler

val audioTestSuite = testSuite("audio") {
  alsoTestFunction("audioEventHandler")
  alsoTestFunction("audioHwInit")
  alsoTestFunction("audioInput")
  alsoTestFunction("audioInputDmaCb")
  alsoTestFunction("audioInputInit")
  alsoTestFunction("audioInputSetup")
  alsoTestFunction("audioInputThread")
  alsoTestFunction("audioIntrHandler")
  alsoTestFunction("audioMixerThread")
  alsoTestFunction("audioOutput")
  alsoTestFunction("audioOutputDmaCb")
  alsoTestFunction("audioSRCOutput")
  alsoTestFunction("audioSRCOutputDmaCb")
  alsoTestFunction("dmaUpdate")
  alsoTestFunction("updateAudioBuf")

  global("g_audio")
  global("g_audioEvent")

  test("dmaUpdate") {
    beforeEach {
      args(2)
      hookWord(0xbe00000c) {
        yield(0xFF)
        yield(0x00)
      }
    }
    case { writeSymbolField("g_audio.flags", 0) }
    case { zeroedArgs() }
  }

  test("audioMixerThread") {
    beforeEach {
      hookWord(0xbe000028) {
        yieldAll(generateSequence { 0xFF })
      }
      hookFunction("sceKernelWaitEventFlag", listOf(0xFF, 0xFF, -1))
      repeat(8) { i ->
        writeSymbolField("g_audio.chans[$i].buf", allocBuf(0x100))
        writeSymbolField("g_audio.chans[$i].curSampleCnt", 10)
        writeSymbolField("g_audio.chans[$i].bytesPerSample", 2)
      }
      writeSymbolField("g_audio.dmaPtr[0]", allocBuf(0x100))
    }
  }

  test("sceAudioSRCOutputBlocking") {
    beforeEach {
      hookWord(0xbe000028) { yieldAll(generateSequence { 0xFF }) }
      hookWord(0xbe00000c) {
        while (true) {
          yield(0x00)
          yield(0xFF)
        }
      }
      hookFunction("sceKernelGetSystemTimeLow") {
        yieldAll(generateSequence(10) { it + 10 })
      }
    }
  }

  test("sceAudioInputInitEx") {
    beforeEach {
      args(allocBuf(0x30))
    }
  }

  test("sceAudioOutputBlocking") {
    beforeEach {
      hookFunction("audioOutput", listOf(0x80260002.toInt(), 0))
      hookFunction("sceKernelWaitEventFlag", listOf(0))
      writeSymbolField("g_audio.chans[3].unk10", 0)
    }
  }

  test("sceAudioOutputPannedBlocking") {
    beforeEach {
      args(0x6)
      hookFunction("audioOutput", listOf(0x80260002.toInt(), 0))
      hookFunction("sceKernelWaitEventFlag", listOf(0))
      writeSymbolField("g_audio.chans[6].unk10", 0)
    }
  }

  test("sceAudioChReserve") {
    case {
      args(-1)
    }
    case {
      args(0x3, 0)
      writeSymbolField("g_audio.chans[3].sampleCount", 0)
    }
    case {
      args(0x3, 0x100)
      writeSymbolField("g_audio.chans[3].sampleCount", 0)
    }
    case {
      args(0x3, 0x100, 0)
      writeSymbolField("g_audio.chans[3].sampleCount", 0)
    }
  }

  test("sceAudioOneshotOutput") {
    beforeEach {
      hookWord(0xbe000028) { yieldAll(generateSequence { 0xFF }) }
    }
    case {
      args(-1)
    }
    case {
      args(0x3, 0)
      writeSymbolField("g_audio.chans[3].sampleCount", 0)
    }
    case {
      args(0x3, 2)
      writeSymbolField("g_audio.chans[3].sampleCount", 0)
    }
    case {
      args(0x3, 2, 0)
      writeSymbolField("g_audio.chans[3].sampleCount", 0)
    }
    case {
      args(a3 = 0x20000)
    }
  }

  test("sceAudioChRelease") {
    case {
      args(0x3, 0)
      writeSymbolField("g_audio.chans[3].sampleCount", 0)
    }
    case {
      args(0x3, 0)
      writeSymbolField("g_audio.chans[3].unk10", 0)
    }
  }

  test("sceAudioSetChannelDataLen") {
    case {
      args(0x3, 0x1000)
      writeSymbolField("g_audio.chans[3].unk10", 0)
    }
    case {
      args(0x3, 0x1000)
      writeSymbolField("g_audio.chans[3].unk10", 0)
      writeSymbolField("g_audio.chans[3].sampleCount", 0)
    }
    case {
      args(0x3, 0x1000)
      writeSymbolField("g_audio.chans[3].unk10", 1)
    }
  }

  test("sceAudioChangeChannelConfig") {
    beforeEach {
      writeSymbolField("g_audio.chans[3].unk10", 0)
      writeSymbolField("g_audio.chans[3].curSampleCnt", 0)
    }
    case {
      args(0x3, 0)
      writeSymbolField("g_audio.chans[3].sampleCount", 10)
    }
    case {
      args(0x3, 1)
      writeSymbolField("g_audio.chans[3].sampleCount", 10)
    }
    case {
      args(0x3, 10)
      writeSymbolField("g_audio.chans[3].sampleCount", 10)
    }
    case {
      args(0x3, 10)
      writeSymbolField("g_audio.chans[3].sampleCount", 0)
    }
    case {
      args(0x3, 10)
      writeSymbolField("g_audio.chans[3].curSampleCnt", 1)
    }
    case {
      hookFunction("SysMemForKernel_0xB4F00CB5", 0x20000001)
      args(0x3, 10)
      writeSymbolField("g_audio.chans[3].curSampleCnt", allocBuf(100))
    }
  }

  test("sceAudioOutput2ChangeLength") {
    beforeEach {
      args(0x100)
    }
    case {
      writeSymbolField("g_audio.srcChFreq", 0)
    }
    case {
      writeSymbolField("g_audio.srcChFreq", 1)
    }
  }

  test("sceAudioOutput2GetRestSample") {
    case {
      writeSymbolField("g_audio.srcChFreq", 1)
      writeSymbolField("g_audio.hwBuf[18]", 1)
      writeSymbolField("g_audio.hwBuf[26]", 1)
    }
  }

  test("sceAudio_driver_0x4A0FE97D") {
    case {
      args(0xac44)
    }
    case {
      args(48000)
    }
    case {
      args(48000)
      writeSymbolField("g_audio.flags", 0)
    }
  }

  test("sceAudio_driver_0x22E4FB37") {
    beforeEach {
      hookWord(0xbe000028) {
        yieldAll(generateSequence { 0xFF })
      }
      hookFunction("sceKernelStartThread", 0)
      hookFunction("sceKernelCreateThread", 0)
      hookFunction(ResultFunctionHandler("sceKernelDmaOpAlloc") { allocBuf(0x100) })
    }
  }

  test("audioHwInit") {
    beforeEach {
      hookWord(0xbe000028) {
        yieldAll(generateSequence { 0xFF })
      }
    }
    case {
      writeSymbolField("g_audio.hwFreq", 0)
    }
    case {
      writeSymbolField("g_audio.freq", 48000)
    }
  }

  test("sceAudio_driver_0x53A4FE20") {
    beforeEach {
      hookWord(0xbe000028) {
        yieldAll(generateSequence { 0xFF })
      }
    }
    case {
      zeroedArgs()
    }
  }

  test("sceAudio_driver_0xF86DFDD6") {
    case {
      hookFunction("sceCodec_driver_0xD27707A8", 20)
    }
  }

  test("audioOutputDmaCb") {
    case {
      args(a1 = 0)
    }
  }

  test("sceAudioSRCChRelease") {
    case {
      writeSymbolField("g_audio.srcChFreq", 0)
    }
    case {
      writeSymbolField("g_audio.hwBuf[26]", 0)
      writeSymbolField("g_audio.hwBuf[18]", 0)
    }
  }

  test("sceAudioSRCOutputBlocking") {
    case {
      hookFunction("audioSRCOutput", 10)
    }
    case {
      hookFunction("audioSRCOutput", 0)
    }
    case {
      hookFunction("audioSRCOutput", 0)
      writeSymbolField("g_audio.flags", 0xFF)
    }
  }

  test("sceAudioWaitInputEnd") {
    case {
      writeSymbolField("g_audio.inputIsWaiting", 0)

    }
    case {
      writeSymbolField("g_audio.inputIsWaiting", 0)
      writeSymbolField("g_audio.inputCurSampleCnt", 0)
    }
  }

  test("sceAudioSRCChReserve") {
    beforeEach {
      args(a1 = 16000, a2 = 2)
    }
    case {
      zeroedArgs()
    }
    case {
      args(a0 = 30, a1 = 0, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.srcChFreq", 0)
      args(a0 = 30, a1 = 16000, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.srcChFreq", 0)
      args(a0 = 30, a1 = 0x3e81 - 1, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.srcChFreq", 0)
      args(a0 = 30, a1 = 0x2b12 - 1, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.flags", 0xFF)
      writeSymbolField("g_audio.srcChFreq", 0)
      args(a0 = 30, a1 = 0x2b12 - 1, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.flags", 0xFF)
      writeSymbolField("g_audio.srcChFreq", 0)
      args(a0 = 30, a1 = 0, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.flags", 0xFF)
      writeSymbolField("g_audio.srcChFreq", 0)
      args(a0 = 30, a1 = 8000, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.flags", 0xFF)
      writeSymbolField("g_audio.srcChFreq", 0)
      args(a0 = 30, a1 = 12000, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.flags", 0xFF)
      writeSymbolField("g_audio.srcChFreq", 0)
      args(a0 = 30, a1 = 24000, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.flags", 0xFF)
      writeSymbolField("g_audio.srcChFreq", 0)
      args(a0 = 30, a1 = 23999, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.flags", 0xFF)
      writeSymbolField("g_audio.srcChFreq", 0)
      args(a0 = 30, a1 = 22050, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.flags", 0xFF)
      writeSymbolField("g_audio.srcChFreq", 0)
      args(a0 = 30, a1 = 48000, a2 = 2)
    }
    case {
      writeSymbolField("g_audio.flags", 0xFF)
      writeSymbolField("g_audio.srcChFreq", 0)
      writeSymbolField("g_audio.freq", 48000)
      args(a0 = 30, a1 = 1, a2 = 2)
    }
  }

  test("audioInputInit") {
    case {
      zeroedArgs()
    }
    case {
      args(5, 5, 5, 5, 5, 5, 5, 5)
    }
    case {
      zeroedArgs()
      hookFunction("sceCodec_driver_0x6FFC0FA4", 20)
      hookFunction("sceCodec_driver_0xA88FD064", 20)
    }
  }

  test("updateAudioBuf") {
    beforeEach {
      hookWord(0xbe000028) {
        yieldAll(generateSequence { 0xFF })
      }
    }
    case {
      zeroedArgs()
    }
    case {
      hookFunction("sceKernelDmaOpAssign", 0)
      hookFunction("sceKernelDmaOpSetCallback", 0)
      hookFunction("sceKernelDmaOpSetupLink", 0)
    }
    case {
      zeroedArgs()
      hookFunction("sceKernelDmaOpAssign", 0)
      hookFunction("sceKernelDmaOpSetCallback", 0)
      hookFunction("sceKernelDmaOpSetupLink", 0)
    }
    case {
      hookFunction("sceKernelDmaOpAssign", 0)
      hookFunction("sceKernelDmaOpSetCallback", 0)
    }
    case {
      hookFunction("sceKernelDmaOpAssign", 0)
    }
    case {
      writeSymbolField("g_audio.flags", 0)
    }
  }

  test("audioInputDmaCb") {
    case {
      zeroedArgs()
    }
  }

  test("audioSRCOutputDmaCb") {
    case {
      zeroedArgs()
    }
  }

  test("audioIntrHandler") {
    case {
      hookWord(0xbe00001c) { yield(0) }
      writeSymbolField("g_audio.flags", 0)
    }
    case {
      hookWord(0xbe00001c) { yield(1) }
      writeSymbolField("g_audio.flags", 1)
      writeSymbolField("g_audio.dmaPtr[0]", allocBuf(0x100))
    }
    case {
      hookWord(0xbe00001c) { yield(2) }
      writeSymbolField("g_audio.flags", 0xFF)
      writeSymbolField("g_audio.dmaPtr[0]", allocBuf(0x100))
    }
    case {
      hookWord(0xbe00001c) { yield(4) }
      writeSymbolField("g_audio.flags", 0xFF)
      writeSymbolField("g_audio.dmaPtr[0]", allocBuf(0x100))
    }
  }

  test("audioOutput") {
    beforeEach {
      args(allocBuf(0x100))
    }
    case {
      val buf = allocBuf(0x100)
      ctx.memory.writeWord(buf, 0)
      args(buf)
      hookWord(0xbe000028) {
        yieldAll(generateSequence { 0xFF })
      }
    }
  }

  test("audioSRCOutput") {
    beforeEach {
      hookFunction("sceKernelGetSystemTimeLow") {
        yieldAll(generateSequence(10) { it + 10 })
      }
      hookWord(0xBE00000C) {
        yield(0xF0)
        yield(0)
        yield(0xFF)
        yieldAll(generateSequence { 0x0 })
      }
      hookWord(0xBE000028) {
        yieldAll(generateSequence { 0xFF })
      }
    }
    case {
      writeSymbolField("g_audio.hwBuf[18]", 0)
      writeSymbolField("g_audio.hwBuf[26]", 0)
    }
    case {
      writeSymbolField("g_audio.hwBuf[18]", 0)
      writeSymbolField("g_audio.hwBuf[26]", 0)
      writeSymbolField("g_audio.flags", 2)
    }
  }

  test("audioInputSetup") {
    beforeEach {
      hookFunction("sceKernelDmaOpAssign", 0)
      hookFunction("sceKernelDmaOpSetCallback", 0)
      hookFunction("sceKernelDmaOpSetupLink", 0)
    }
    case {
      writeSymbolField("g_audio.flags", 0)
    }
  }

  test("audioEventHandler") {
    case {
      args(0x1000)
    }
    case {
      args(0x100000)
    }
  }

  test("audioInput") {
    case {
      args(0x1000)
    }
    case {
      args(0x1000, 0x5622)
    }
    case {
      args(0x1000, 0x5622)
      writeSymbolField("g_audio.inputCurSampleCnt", 0)
    }
    case {
      args(0x1000, 0x5622, 0)
      writeSymbolField("g_audio.inputCurSampleCnt", 0)
    }
    case {
      args(0x1000, 0x2b11)
      writeSymbolField("g_audio.inputCurSampleCnt", 0)
    }
    case {
      args(0x1000, 0xac44)
      writeSymbolField("g_audio.inputCurSampleCnt", 0)
    }
    case {
      args(0x1000, 0x5622)
      hookFunction("audioInputSetup", 0xFF)
      hookFunction("sceCodec_driver_0x6FFC0FA4", 0xFF)
      writeSymbolField("g_audio.flags", 0)
      writeSymbolField("g_audio.inputCurSampleCnt", 0)
      writeSymbolField("g_audio.unkCodecArgSet", 1)
    }
    case {
      args(0x1000, 0x5622)
      hookFunction("audioInputSetup", -1)
      hookFunction("sceCodec_driver_0x6FFC0FA4", 0xFF)
      writeSymbolField("g_audio.flags", 0)
      writeSymbolField("g_audio.inputCurSampleCnt", 0)
      writeSymbolField("g_audio.unkCodecArgSet", 1)
    }
  }

  test("audioInputThread") {
    beforeEach {
      hookFunction("sceKernelWaitEventFlag", listOf(0xFF, 0xFF, -1))
      writeSymbolField("g_audio.inputBuf", allocBuf(0x100))
      writeSymbolField("g_audio.inputHwFreq", 128)
    }
    case {
      hookFunction("sceKernelWaitEventFlag", listOf(0xFF, 0xFF, -1))
      writeSymbolField("g_audio.inputBuf", allocBuf(0x100))
      writeSymbolField("g_audio.inputHwFreq", 64)
    }
    case {
      writeSymbolField("g_audio.inputCurSampleCnt", 0)
      writeSymbolField("g_audio.inputIsWaiting", 0)
      writeSymbolField("g_audio.unkCodecArgSet", 0)
    }
  }
}
