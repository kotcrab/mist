package mist.uofw

import kmips.Label
import kmips.Reg.*
import mist.module.ModuleMemory
import mist.symbolic.*

fun sceSymbolicFunctionLibrary(moduleMemory: ModuleMemory): FunctionLibrary {
  return FunctionLibrary(
    listOf(
      ProvidedFunctionHandler("memset", moduleMemory) {
        // a0 ptr, a1 value, a2 num
        val end = Label()

        beq(a2, zero, end)
        move(v0, a0)

        addu(a2, a0, a2)
        val loop = label()
        sb(a1, 0, a0)
        addiu(a0, a0, 1)
        bne(a2, a0, loop)
        nop()

        label(end)
        jr(ra)
        nop()
      },

      SymbolicFunctionHandler("sceKernelCreateThread"),
      SymbolicFunctionHandler("sceKernelStartThread"),
      ResultFunctionHandler("sceKernelExitThread"),
      ResultFunctionHandler("sceKernelDelayThread"),

      ResultFunctionHandler("sceKernelCpuSuspendIntr") { Expr.Const.of(0x87654321.toInt()) },
      ResultFunctionHandler("sceKernelCpuResumeIntr"),
      ResultFunctionHandler("sceKernelCpuResumeIntrWithSync"),

      ResultFunctionHandler("sceKernelEnableIntr"),
      ResultFunctionHandler("sceKernelRegisterIntrHandler"),
      ResultFunctionHandler("sceKernelReleaseIntrHandler"),
      ResultFunctionHandler("sceKernelRegisterSysEventHandler"),
      ResultFunctionHandler("sceKernelUnregisterSysEventHandler"),

      ResultFunctionHandler("sceKernelGetSystemTimeLow") {
        Expr.Const.of(functionStates.getAndAdd("__time", 10))
      },

      SymbolicFunctionHandler("sceKernelWaitEventFlag", symbolicExecutionLimit = 1, constValue = -1),
      ResultFunctionHandler("sceKernelSetEventFlag"),
      ResultFunctionHandler("sceKernelCreateEventFlag") { Expr.Const.of(0x1234) },
      ResultFunctionHandler("sceKernelClearEventFlag"),
      ResultFunctionHandler("sceKernelDeleteEventFlag"),

      ResultFunctionHandler("sceKernelDcacheWritebackRange"),
      ResultFunctionHandler("sceKernelDcacheWritebackInvalidateRange"),

      ResultFunctionHandler("sceKernelDmaOpAlloc") { Expr.Const.of(memory.allocate(0x40)) },
      SymbolicFunctionHandler("sceKernelDmaOpAssign"),
      SymbolicFunctionHandler("sceKernelDmaOpConcatenate"),
      ResultFunctionHandler("sceKernelDmaOpDeQueue"),
      ResultFunctionHandler("sceKernelDmaOpEnQueue"),
      ResultFunctionHandler("sceKernelDmaOpFree"),
      SymbolicFunctionHandler("sceKernelDmaOpSetCallback"),
      SymbolicFunctionHandler("sceKernelDmaOpSetupLink"),
      ResultFunctionHandler("sceKernelDmaOpQuit"),

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
    )
  )
}
