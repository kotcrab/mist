package mist.uofw

import kio.util.child
import kmipsx.elf.CompileResult
import kmipsx.elf.pspCodeCompiler
import kmipsx.util.compiledElf
import mist.module.ModuleMemory
import mist.symbolic.*
import java.io.File

private var sceCompileResult: CompileResult? = null

fun compileSceFunctionLibrary(pspSdkDir: File, srcDir: File) {
  println("Compiling native code...")
  val outDir = srcDir.child("tmp").also { it.mkdir() }
  sceCompileResult = pspCodeCompiler(pspSdkDir)
    .compile(
      patchBaseAddress = ModuleMemory.INITIAL_BUFFER_ALLOC,
      srcFiles = listOf(srcDir.child("native.c")),
      outDir = outDir,
      additionalGccFlags = listOf("-std=c99"),
    )
}

fun sceSymbolicFunctionLibrary(moduleMemory: ModuleMemory): FunctionLibrary {
  val compileResult = sceCompileResult
    ?: error("Native library code was not compiled")
  if (moduleMemory.currentBufferAlloc != ModuleMemory.INITIAL_BUFFER_ALLOC) {
    error("Library must be created before allocating module memory")
  }
  return FunctionLibrary(
    listOf(
      ProvidedFunctionHandler("__compiled_code", moduleMemory) {
        compiledElf(compileResult)
      },

      SymbolicFunctionHandler("__udivdi3"),
      SymbolicFunctionHandler("__umoddi3"),

      SymbolicFunctionHandler("sceKernelCreateThread"),
      SymbolicFunctionHandler("sceKernelStartThread"),
      ResultFunctionHandler("sceKernelExitThread"),
      ResultFunctionHandler("sceKernelDelayThread"),

      ResultFunctionHandler("sceKernelCpuSuspendIntr") { Expr.Const.of(0x87654321.toInt()) },
      ResultFunctionHandler("sceKernelCpuResumeIntr"),
      ResultFunctionHandler("sceKernelCpuResumeIntrWithSync"),

      ResultFunctionHandler("sceKernelEnableIntr"),
      ResultFunctionHandler("sceKernelDisableIntr"),
      SymbolicFunctionHandler("sceKernelIsIntrContext"),
      ResultFunctionHandler("sceKernelRegisterIntrHandler"),
      ResultFunctionHandler("sceKernelReleaseIntrHandler"),
      ResultFunctionHandler("sceKernelRegisterResumeHandler"),
      ResultFunctionHandler("sceKernelRegisterSuspendHandler"),
      ResultFunctionHandler("sceKernelRegisterSysEventHandler"),
      ResultFunctionHandler("sceKernelUnregisterSysEventHandler"),

      ResultFunctionHandler("sceKernelGetSystemTimeLow") {
        Expr.Const.of(functionStates.getAndAdd("__time", 10))
      },

      SymbolicFunctionHandler("sceKernelWaitEventFlag", symbolicExecutionLimit = 1, constValueV0 = -1),
      ResultFunctionHandler("sceKernelSetEventFlag"),
      ResultFunctionHandler("sceKernelCreateEventFlag") { Expr.Const.of(0x1234) },
      ResultFunctionHandler("sceKernelClearEventFlag"),
      ResultFunctionHandler("sceKernelDeleteEventFlag"),

      ResultFunctionHandler("sceKernelDcacheWritebackRange"),
      ResultFunctionHandler("sceKernelDcacheWritebackInvalidateRange"),

      ResultFunctionHandler("sceKernelDmaOpAlloc") { memory.allocate(0x40, 0xCD, track = true) },
      SymbolicFunctionHandler("sceKernelDmaOpAssign"),
      SymbolicFunctionHandler("sceKernelDmaOpConcatenate"),
      ResultFunctionHandler("sceKernelDmaOpDeQueue"),
      ResultFunctionHandler("sceKernelDmaOpEnQueue"),
      ResultFunctionHandler("sceKernelDmaOpFree"),
      SymbolicFunctionHandler("sceKernelDmaOpSetCallback"),
      SymbolicFunctionHandler("sceKernelDmaOpSetupLink"),
      ResultFunctionHandler("sceKernelDmaOpQuit"),

      SymbolicFunctionHandler("sceKernelCreateSema"),
      SymbolicFunctionHandler("sceKernelDeleteSema"),
      SymbolicFunctionHandler("sceKernelSignalSema"),
      SymbolicFunctionHandler("sceKernelWaitSema"),

      SymbolicFunctionHandler("sceKernelCreateHeap"),
      SymbolicFunctionHandler("sceKernelDeleteHeap"),
      SymbolicFunctionHandler("sceKernelFreeHeapMemory"),
      SymbolicFunctionHandler("sceKernelAllocHeapMemory", constraints = {
        listOf(
          Expr.ZERO,
          ctx.memory.allocate(0x1000, 0xCD, track = true)
        )
      }),

      SymbolicFunctionHandler("sceIoDevctl"),
      SymbolicFunctionHandler("sceIoAddDrv"),
      SymbolicFunctionHandler("sceIoTerminateFd"),

      SymbolicFunctionHandler("sceKernelExtendKernelStack"),

      SymbolicFunctionHandler("sceKernelCreateFpl"),
      SymbolicFunctionHandler("sceKernelDeleteFpl"),
      SymbolicFunctionHandler("sceKernelFreeFpl"),
      SymbolicFunctionHandler("sceKernelTryAllocateFpl"),

      SymbolicFunctionHandler("sceRtcTickAddTicks"),
      SymbolicFunctionHandler("sceRtcGetTick"),
      SymbolicFunctionHandler("sceRtcSetTick"),

      ResultFunctionHandler("sceDdrFlush"),

      SymbolicFunctionHandler("sceKernelGetCompiledSdkVersion"),

      ResultFunctionHandler("sceCodecOutputEnable"),
      SymbolicFunctionHandler("sceCodecSetVolumeOffset"),
      SymbolicFunctionHandler("sceCodec_driver_277DFFB6"),
      SymbolicFunctionHandler("sceCodec_driver_376399B6"),
      SymbolicFunctionHandler("sceCodec_driver_6FFC0FA4"),
      SymbolicFunctionHandler("sceCodec_driver_A88FD064"),
      SymbolicFunctionHandler("sceCodec_driver_FC355DE0"),
      SymbolicFunctionHandler("sceCodec_driver_FCA6D35B"),

      ResultFunctionHandler("sceClockgenAudioClkSetFreq"),

      ResultFunctionHandler("sceSysregAudioClkoutClkSelect"),
      ResultFunctionHandler("sceSysregAudioIoEnable"),
      ResultFunctionHandler("sceSysregAudioClkoutIoEnable"),
      ResultFunctionHandler("sceSysregAudioBusClockEnable"),
      ResultFunctionHandler("sceSysregAudioClkEnable"),
      ResultFunctionHandler("sceSysregAudioClkSelect"),
      ResultFunctionHandler("sceSysregAudioIoDisable"),
      ResultFunctionHandler("sceSysregAudioClkoutIoDisable"),

      SymbolicFunctionHandler("sceUmd_040A7090"),
      SymbolicFunctionHandler("sceUmdSetDriveStatus"),
      SymbolicFunctionHandler("sceUmdMan_driver_65E2B3E0"),
      SymbolicFunctionHandler("sceUmdManRegisterInsertEjectUMDCallBack"),
      SymbolicFunctionHandler("sceUmdManUnRegisterInsertEjectUMDCallBack"),

      SymbolicFunctionHandler("sceAudiocodecReleaseEDRAM"),
      SymbolicFunctionHandler("sceAudiocodecGetEDRAM"),
      SymbolicFunctionHandler("sceAudiocodec_3DD7EE1A"),
      SymbolicFunctionHandler("sceAudiocodecInit"),
      SymbolicFunctionHandler("sceAudiocodecDecode"),
      SymbolicFunctionHandler("sceAudiocodecCheckNeedMem"),

      ProvidedFunctionHandler("sceKernelMemcpy", moduleMemory) {
        j(compileResult.functions.getValue("memcpy"))
        nop()
      }
    ) +
      listOf(
        "memcmp",
        "memcpy",
        "memset",
        "strchr",
        "strcmp",
        "strlen",
        "strncmp",
        "strrchr",
        "strstr",
        "strtol",
        "look_ctype_table",
      ).map { func ->
        ProvidedFunctionHandler(func, moduleMemory) {
          j(compileResult.functions.getValue(func))
          nop()
        }
      }
  )
}
