package mist.uofw

import mist.asm.mips.GprReg
import mist.suite.ConfigureContextScope
import mist.suite.TypedAllocation
import mist.suite.suiteConfig
import mist.symbolic.Expr
import mist.symbolic.SymbolicFunctionHandler
import java.util.concurrent.atomic.AtomicInteger

val isofsSuiteConfig = suiteConfig("isofs") {
  global("g_isofsHandlers")
  global("g_isofsUnits")
  global("g_isofsCache")
  global("g_isofsCurrentDir")
  global("g_isofsCurrentDirLbn")
  global("g_isofsMounted")
  global("g_isofsMounted2")
  global("g_isofsInitialized")
  global("g_isofsOpenDirs")
  global("g_isofsMgr")

  // Setup

  functionLibrary { moduleMemory ->
    sceSymbolicFunctionLibrary(moduleMemory).extendWith(
      listOf(
        SymbolicFunctionHandler("isofsHandlerReadDirSectors"),
        SymbolicFunctionHandler("isofsHandlerReadSectors"),
        SymbolicFunctionHandler("isofsHandlerGetCurrentLbn"),
        SymbolicFunctionHandler("isofsHandlerGetSectorSize"),
        SymbolicFunctionHandler("isofsHandlerGetTotalNumberOfSectors"),
        SymbolicFunctionHandler("isofsHandlerSeek"),
        SymbolicFunctionHandler("isofsHandler_unk1c"),
        SymbolicFunctionHandler("isofsHandlerReadPvd"),
        SymbolicFunctionHandler("isofsHandlerPrepareIntoCache"),
        SymbolicFunctionHandler("isofsHandler_unk30"),
        SymbolicFunctionHandler("isofsHandler_unk34"),
        SymbolicFunctionHandler("isofsHandler_unk38"),
      )
    )
  }

  configureContext {
    writeSymbolField("g_isofsMgr.maxUnits", 1)
    writeSymbolField("g_isofsMgr.fdwCount", 2)
    writeSymbolField("g_isofsMgr.maxFiles", 0x20)
    writeSymbol("g_isofsUnits", createUnit("g_"))
  }

  overrideElfFunctionName("module_start", "isofsInit")
  overrideElfFunctionName("module_reboot_before", "isofsRebootBefore")

  ignoreComparingArgsOf("sceKernelCreateFpl", 0)
  ignoreComparingArgsOf("sceKernelCreateSema", 0)
  ignoreComparingArgsOf("sceKernelCreateHeap", 3)
  ignoreComparingArgsOf("sceIoTerminateFd", 0)

  exclude("isofsHandlerReadSectorsArgs")
  exclude("isofsHandlerReadDirSectors")
  exclude("isofsHandlerReadSectors")
  exclude("isofsHandlerGetCurrentLbn")
  exclude("isofsHandlerGetSectorSize")
  exclude("isofsHandlerGetTotalNumberOfSectors")
  exclude("isofsHandlerSeek")
  exclude("isofsHandler_unk1c")
  exclude("isofsHandlerReadPvd")
  exclude("isofsHandlerPrepareIntoCache")
  exclude("isofsHandler_unk30")
  exclude("isofsHandler_unk34")
  exclude("isofsHandler_unk38")

  // Tests

  val readParamsFuncs = listOf(
    "isofsReadParams",
    "isofsReadParamsZero",
    "isofsReadParamsInitial",
  )
  test("isofsUmdReadSectors") {
    configureContext {
      assume { a0 eqOrNull createUnit() }
      assume { a1 eqOrNull createFile() }
      assume { a3 eqOrNull allocate() }
    }
    functionLibrary {
      it.extendWith(readParamsFuncs.map { func -> SymbolicFunctionHandler(func) })
    }
    readParamsFuncs.forEach { func ->
      ignoreComparingArgsOf(func, 4)
    }
    ignoreComparingArgsOf("sceIoDevctl", 2)
  }

  readParamsFuncs.forEach { func ->
    test(func) {
      configureContext {
        assume { a0 eqOrNull createUnit() }
        assume { a1 eqOrNull createFile() }
        assume { t0 eqOrNull allocate("LbnParams", "params") }
      }
      proveEquality()
    }
  }
  test("isofsUmdReadDirSectors") {
    ignoreComparingArgsOf("sceIoDevctl", 2)
  }
  test("isofsUmdGetSectorSize")
  test("isofsUmdGetTotalNumberOfSectors")
  test("isofsUmdGetCurrentLbn")
  test("isofsUmdSeek") {
    ignoreComparingArgsOf("sceIoDevctl", 2)
  }
  test("isofsUmd_1e180d3")
  test("isofsUmd_unkNotSupported")
  test("isofsUmdGetUnitNum")
  test("isofsUmdReadPvd") {
    configureContext {
      assume { a0 eq createUnit() }
    }
    ignoreComparingArgsOf("sceIoDevctl", 4)
    proveEquality(functionCalls = false)
  }
  listOf(
    "isofsUmd_1f100a6_1f100a7",
    "isofsUmd_1f100a8",
    "isofsUmd_1f100a9",
  ).forEach { func ->
    test(func) {
      configureContext {
        assume { a0 eq createUnit() }
        assume { a1 eq allocate() }
      }
      ignoreComparingArgsOf("sceIoDevctl", 2)
    }

  }
  test("isofsUmdPrepareIntoCache") {
    configureContext {
      assume { a0 eq createUnit() }
      assume { a1 eq create("IsofsFile", "file").address }
    }
    ignoreComparingArgsOf("sceIoDevctl", 2, 4)
    proveEquality()
  }

  test("isofsInit") {
    ignoreComparingArgsOf("sceIoAddDrv", 0)
  }
  test("isofsRebootBefore")

  test("isofsClearUnitFilesFdw") {
    configureContext {
      assume { a0 eq createUnit() }
      assume { a1 eq 2 }
    }
    proveEquality()
  }
  test("isofsClearUnit") {
    configureContext {
      assume { a0 le 0 }
    }
    proveEquality()
  }

  test("isofsDrvInit")
  test("isofsDrvExit") {
    configureContext {
      writeSymbol("g_isofsCache", allocate(initByte = 1))
    }
    proveEquality()
  }
  test("isofsDrvOpen") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      val file = create("IsofsFile", "file")
      assume { iob.fieldExpr("i_unit") le 0 }
      iob.writeField("i_private", file)

      assume { a0 eqOrNull iob.address }
      assume { a1 eqOrNull create("u8", "path", 20).address }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("isofsCheckMode"),
          SymbolicFunctionHandler("isofsCheckPath"),
          SymbolicFunctionHandler("isofsIsSceLbnPath"),
          SymbolicFunctionHandler("isofsOpen", constraints = {
            listOf(
              Expr.ZERO,
              ctx.memory.allocate(moduleTypes.findOrThrow("IsofsFile"), "openFile", 1)
            )
          }),
          SymbolicFunctionHandler("isofsOpenRaw", constraints = {
            listOf(
              Expr.ZERO,
              ctx.memory.allocate(moduleTypes.findOrThrow("IsofsFile"), "openRawFile", 1)
            )
          }),
          SymbolicFunctionHandler("isofsParseSceLbnPath"),
          SymbolicFunctionHandler("isofsReadIsoDir", constraints = {
            listOf(
              Expr.ZERO,
              ctx.memory.allocate(moduleTypes.findOrThrow("IsoDirectory"), "isoDir", 1)
            )
          }),
          SymbolicFunctionHandler("strlen"),
          SymbolicFunctionHandler("strrchr"),
        )
      )
    }
    ignoreComparingArgsOf("isofsParseSceLbnPath", 1, 2)
    ignoreComparingArgsOf("isofsReadIsoDir", 4)
    ignoreComparingArgsOf("isofsOpen", 3)
    ignoreComparingArgsOf("isofsOpenRaw", 4)
    proveEquality()
  }
  test("isofsDrvClose") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      val file = create("IsofsFile", "file")
      assume { iob.fieldExpr("i_unit") le 0 }
      iob.writeField("i_private", file)

      assume { a0 eqOrNull iob.address }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("isofsClearFile"),
        )
      )
    }
    proveEquality()
  }
  test("isofsDrvRead") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      val file = create("IsofsFile", "file")
      iob.writeField("i_unit", 0)
      iob.writeField("i_private", file)

      assume { a0 eqOrNull iob.address }
      assume { a1 eqOrNull allocate() }
    }
    ignoreComparingArgsOf("sceKernelExtendKernelStack", 1, 2)
    proveEquality()
  }
  test("isofsDrvLseek") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      val file = create("IsofsFile", "file")
      iob.writeField("i_unit", 0)
      iob.writeField("i_private", file)

      assume { a0 eqOrNull iob.address }
    }
    proveEquality()
  }
  test("isofsDrvIoctl") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      iob.writeField("i_unit", 0)

      assume { a0 eqOrNull iob.address }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("isofsIoctl"),
        )
      )
    }
    proveEquality()
  }
  test("isofsDrvDopen") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      iob.writeField("i_unit", 0)
      assume { a0 eqOrNull iob.address }
      assume { a1 eqOrNull allocate() }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("sceKernelAllocHeapMemory", constraints = {
            listOf(
              Expr.ZERO,
              ctx.memory.allocate(moduleTypes.findOrThrow("IsofsDir"), "heapDir", 1)
            )
          }),
          SymbolicFunctionHandler("isofsCheckPath"),
          SymbolicFunctionHandler("isofsFindPath", constraints = {
            listOf(
              Expr.ZERO,
              ctx.memory.allocate(moduleTypes.findOrThrow("IsofsPath"), "heapPath", 1)
            )
          }),
          SymbolicFunctionHandler("isofsGetUnit"),
          SymbolicFunctionHandler("isofsUnlinkOpenedDir"),
          SymbolicFunctionHandler("memcpy"),
          SymbolicFunctionHandler("strchr", symbolicExecutionLimit = 2, constValueV0 = 0),
          SymbolicFunctionHandler("strcmp"),
          SymbolicFunctionHandler("strlen"),
        )
      )
    }
    ignoreComparingArgsOf("memcpy", 0)
    ignoreComparingArgsOf("memset", 0)
    ignoreComparingArgsOf("strchr", 0)
    ignoreComparingArgsOf("strcmp", 1)
    ignoreComparingArgsOf("strlen", 0)
    ignoreComparingArgsOf("isofsFindPath", 1, 5)
    proveEquality()
  }
  test("isofsDrvDclose") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      val dir = create("IsofsDir", "dir")
      assume { iob.fieldExpr("i_unit") le 0 }
      iob.writeField("i_private", dir)

      assume { a0 eqOrNull iob.address }
    }
    proveEquality()
  }
  test("isofsDrvDread") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      val dir = create("IsofsDir", "dir")
      dir.writeField("fplData", allocate())
      iob.writeField("i_unit", 0)
      iob.writeField("i_private", dir)

      // hacky memory init for consts
      writeBytes(Expr.Const.of(0x88006D70.toInt()), byteArrayOf(0))
      writeBytes(Expr.Const.of(0x88006d71.toInt()), byteArrayOf(1))

      assume { a0 eqOrNull iob.address }
      assume { a1 eqOrNull create("SceIoDirent", "dirent").address }
    }
    functionLibrary {
      val counter = AtomicInteger(0)
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("isofsReadDir"),
          SymbolicFunctionHandler(
            "isofsSetDirentFromIsoDirectory", symbolicExecutionLimit = 2, constValueV0 = 0,
            constraints = {
              listOf(
                Expr.ZERO,
                ctx.memory.allocate(moduleTypes.findOrThrow("IsoDirectory"), "isoDir${counter.getAndIncrement()}", 1)
              )
            },
            preAction = { ctx -> ctx.memory.writeWord(ctx.readGpr(GprReg.A2), Expr.ZERO) }
          ),
          SymbolicFunctionHandler("memset"),
        )
      )
    }
    ignoreComparingArgsOf("sceKernelTryAllocateFpl", 1)
    ignoreComparingArgsOf("isofsSetDirentFromIsoDirectory", 2)
    proveEquality()
  }
  test("isofsDrvGetstat") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      assume { iob.fieldExpr("i_unit") le 0 }

      assume { a0 eqOrNull iob.address }
      assume { a1 eqOrNull allocateString("dir/file") }
      assume { a2 eqOrNull create("SceIoStat", "stat").address }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("isofsCheckPath"),
          SymbolicFunctionHandler("isofsCheckSceLbn"),
          SymbolicFunctionHandler("isofsReadIsoDir", constraints = {
            listOf(
              Expr.ZERO,
              ctx.memory.allocate(0x100, track = true)
            )
          }),
        )
      )
    }
    proveEquality(functionCalls = false) // TODO?
  }
  test("isofsDrvChdir")
  test("isofsDrvMount") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      assume { iob.fieldExpr("i_unit") le 0 }

      assume { a0 eqOrNull iob.address }
      assume { a1 eqOrNull allocate() }
      assume { a2 eqOrNull allocateString("umd") }
      assume { t0 eqOrNull allocate() }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("isofsClearUnitFilesFdw"),
          SymbolicFunctionHandler("isofsClearUnit"),
          SymbolicFunctionHandler("isofsSetBlockDevHandler"),
        )
      )
    }
    ignoreComparingArgsOf("sceIoDevctl", 4)
    ignoreComparingArgsOf("sceUmdManRegisterInsertEjectUMDCallBack", 1)
    proveEquality()
  }
  test("isofsDrvUmount") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      assume { iob.fieldExpr("i_unit") le 0 }

      assume { a0 eqOrNull iob.address }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("isofsClearUnitFilesFdw"),
          SymbolicFunctionHandler("isofsClearUnit"),
        )
      )
    }
    proveEquality()
  }
  test("isofsDrvDevctl") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      iob.writeField("i_unit", 0)

      assume { a0 eqOrNull iob.address }
      assume { a1 eqOrNull allocateString("dev") }
    }
    proveEquality()
  }

  test("isofsOpen") {
    configureContext {
      assume { a0 eqOrNull createUnit() }
      assume { a1 eqOrNull allocate() }
      assume { a2 eq allocate() }
      assume { a3 eq allocate("s32", "outResult") }
    }
    proveEquality()
  }
  test("isofsOpenRaw") {
    configureContext {
      assume { a0 eqOrNull createUnit() }
      assume { a1 eqOrNull allocate() }
      assume { t0 eq allocate("s32", "outResult") }
    }
    proveEquality()
  }
  test("isofsFindUnusedFile") {
    configureContext {
      assume { a0 eqOrNull createUnit() }
      assume { a1 eq allocate() }
      assume { a2 eq allocate("s32", "outResult") }
    }
    proveEquality()
  }
  test("isofsFindUnusedFileRaw") {
    configureContext {
      assume { a0 eqOrNull createUnit() }
      assume { a2 eq allocate("s32", "outResult") }
    }
    proveEquality()
  }
  test("isofsClearFile") {
    configureContext {
      assume { a0 eqOrNull allocate("IsofsFile", "file") }
    }
    proveEquality()
  }

  test("isofsFindIsoDirectory") {
    configureContext {
      val buffer = allocate(0x1000, initByte = 0)
      writeBytes(
        buffer,
        byteArrayOf(
          0x0, 0x1,
          0x0, 0x1,
          0x22, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
          0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
          0x0, 0x41,
          0x26, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
          0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
          0x0, 0x70, 0x61, 0x74, 0x68, 0x0,
        )
      )
      args(allocateString("some/path"), buffer)
      assume { a2 eqOrNull 3 }
      assume { a3 eqOrNull allocate(4) }
    }
    initContextWithModuleMemory()
  }
  test("isofsIsSceLbnPath") {
    configureContext {
      assume { a0 eqOrNull allocate(10) }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("strchr"),
          SymbolicFunctionHandler("strncmp"),
        )
      )
    }
    ignoreComparingArgsOf("strncmp", 1)
  }
  test("isofsParseSceLbnPath") {
    configureContext {
      assume { a0 eqOrNull allocate(10) }
      assume { a1 eqOrNull allocate(10) }
      assume { a2 eqOrNull allocate(10) }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("strlen"),
          SymbolicFunctionHandler("strncmp"),
          SymbolicFunctionHandler("strtol"),
          SymbolicFunctionHandler("strstr"),
        )
      )
    }
    ignoreComparingArgsOf("strncmp", 1)
    ignoreComparingArgsOf("strstr", 1)
    proveEquality()
  }

  test("isofsIoctl") {
    configureContext {
      val iob = create("SceIoIob", "iob")
      val file = create("IsofsFile", "file")
      iob.writeField("i_unit", 0)
      iob.writeField("i_private", file)

      val unit = createUnitType()
      unit.writeField("primaryVolumeDescriptor", allocate())
      writeSymbol("g_isofsUnits", unit.address)

      assume { a0 eqOrNull iob.address }
      assume { a2 eqOrNull allocate() }
      assume { (t0 eqOrNull allocate()) or (t0 eq allocate().plus(1)) }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("memcpy"),
          SymbolicFunctionHandler("isofsLseek"),
        )
      )
    }
    ignoreComparingArgsOf("sceKernelExtendKernelStack", 1, 2)
    proveEquality()
  }

  test("isofsClearCurrentDirLbn")

  test("isofsReadPvd") {
    proveEquality()
  }
  test("isofsReadDir") {
    proveEquality()
  }
  test("isofsUnitSetLbns") {
    proveEquality()
  }
  test("isofsUpdatePathsLbnSize") {
    configureContext {
      assume { a0 eqOrNull createUnit() }
    }
    proveEquality()
  }

  test("isofsReadIsoDir") {
    configureContext {
      assume { a0 eqOrNull createUnit() }
      assume { a1 eqOrNull allocateString("some/test/path") }
      assume { a2 eqOrNull allocateString("test") }
      assume { t0 eqOrNull allocate(4) }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("isofsFindIsoDirectory", symbolicExecutionLimit = 2, constValueV0 = 0x123),
          SymbolicFunctionHandler("isofsFindPath"),
          SymbolicFunctionHandler("isofsReadDir"),
          SymbolicFunctionHandler("memcmp"),
          SymbolicFunctionHandler("strchr", symbolicExecutionLimit = 2, constValueV0 = 0),
          SymbolicFunctionHandler("strcmp"),
          SymbolicFunctionHandler("strlen"),
          SymbolicFunctionHandler("memset"),
          SymbolicFunctionHandler("memcpy"),
        )
      )
    }
    ignoreComparingArgsOf("memcmp", 0)
    ignoreComparingArgsOf("memset", 0)
    ignoreComparingArgsOf("memcpy", 0)
    ignoreComparingArgsOf("strcmp", 0, 1)
    ignoreComparingArgsOf("strlen", 0)
    ignoreComparingArgsOf("isofsFindPath", 1)
    proveEquality()
  }

  test("isofsCreatePaths") {
    configureContext {
      assume { a0 eqOrNull createUnit() }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("isofsCountPathTableEntries"),
          SymbolicFunctionHandler("isofsParsePathTableEntries", constraints = {
            listOf(
              Expr.ZERO,
              ctx.memory.allocate(0x100, track = true)
            )
          }),
        )
      )
    }
    ignoreComparingArgsOf("isofsReadPathTable", 1)
    ignoreComparingArgsOf("isofsCountPathTableEntries", 2)
    ignoreComparingArgsOf("isofsParsePathTableEntries", 4)
  }

  test("isofsSetDirentFromIsoDirectory") {
    configureContext {
      val dir = create("IsofsDir", "dir")
      dir.writeField("isoDir", allocate())
      assume { a0 eqOrNull dir.address }
      assume { a1 eqOrNull create("SceIoDirent", "dirent").address }
      assume { a2 eq create("s32", "outRet").address }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("memcpy"),
        )
      )
    }
    ignoreComparingArgsOf("sceRtcGetTick", 1)
    ignoreComparingArgsOf("sceRtcTickAddTicks", 0, 1)
    ignoreComparingArgsOf("sceRtcSetTick", 0, 1)
    proveEquality()
  }

  test("isofsCountPathTableEntries") {
    configureContext {
      val paths = create("u8", "paths", 0x300)
      assume { a0 eqOrNull paths.address }
      assume { a1 le 0x40 }
      assume { a2 eq create("s32", "outRet").address }
    }
    proveEquality()
  }
  test("isofsParsePathTableEntries") {
    configureContext {
      val paths = create("u8", "paths", 0x300)
      assume { a0 eqOrNull createUnit() }
      assume { a1 eqOrNull paths.address }
      assume { a2 le 0x10 }
      assume { a3 le 5 }
      assume { t0 eq create("s32", "outRet").address }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("sceKernelAllocHeapMemory", constraints = {
            listOf(
              Expr.ZERO,
              ctx.memory.allocate(moduleTypes.findOrThrow("IsofsPath"), "heapPaths", 10)
            )
          }),
          SymbolicFunctionHandler("memset"),
          SymbolicFunctionHandler("memcpy"),
          SymbolicFunctionHandler("memcmp"),
        )
      )
    }
    ignoreComparingArgsOf("memcmp", 1)
    proveEquality()
  }

  test("isofsSetBlockDevHandler") {
    configureContext {
      val unit = createUnitType()
      unit.writeField("blockDev[4]", 0)
      assume { a0 eqOrNull unit.address }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("look_ctype_table"),
          SymbolicFunctionHandler("strncmp"),
        )
      )
    }
    ignoreComparingArgsOf("strncmp", 1)
    proveEquality()
  }

  test("isofsCheckPath") {
    configureContext {
      val name = allocate(10)
      writeBytes(name.plus(3), byteArrayOf(0))
      assume { a0 eqOrNull name }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("look_ctype_table"),
        )
      )
    }
    proveEquality()
  }

  test("isofsSplitPath") {
    configureContext {
      assume { a0 eqOrNull allocate(10) }
      assume { a1 eq create("u32", "outLen").address }
      assume { a3 eq create("u32", "outRet").address }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("strchr"),
          SymbolicFunctionHandler("strlen"),
          SymbolicFunctionHandler("strrchr"),
        )
      )
    }
  }

  test("isofsFindPath") {
    configureContext {
      val unit = createUnitType()
      unit.writeField("pathTableEntryCount", 2)
      assume { a0 eqOrNull unit.address }
      assume { a1 eqOrNull create("u8", "path", 30).address }
      assume { t1 eq create("u32", "outRet").address }
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("isofsSplitPath"),
          SymbolicFunctionHandler("memcpy"),
          SymbolicFunctionHandler("strcmp"),
        )
      )
    }
    ignoreComparingArgsOf("isofsSplitPath", 1)
    ignoreComparingArgsOf("memcpy", 0)
    ignoreComparingArgsOf("strcmp", 1)
    proveEquality()
  }

  test("isofsUmdMountedCb") {
    configureContext {
      assume { a1 eqOrNull createUnit() }
      writeSymbol("g_isofsOpenDirs", Expr.ZERO)
    }
    functionLibrary {
      it.extendWith(
        listOf(
          SymbolicFunctionHandler("isofsClearCurrentDirLbn"),
          SymbolicFunctionHandler("isofsUnitSetLbns", symbolicExecutionLimit = 3, constValueV0 = 0),
          SymbolicFunctionHandler("isofsReadPvd"),
          SymbolicFunctionHandler("isofsFreeUnitPaths"),
          SymbolicFunctionHandler("isofsCreatePaths", symbolicExecutionLimit = 3, constValueV0 = 0),
          SymbolicFunctionHandler("memcmp"),
        )
      )
    }
    proveEquality()
  }

  // Many false positives
  exclude("isofsCountPathTableEntries") // False positive on signed/unsigned read
  exclude("isofsCheckPath")

  // Long-running tests
  exclude("isofsReadIsoDir")
  exclude("isofsIoctl")
  exclude("isofsUmdReadSectors")
  exclude("isofsDrvGetstat")
  exclude("isofsParsePathTableEntries")
  exclude("isofsFindPath")
}

private fun ConfigureContextScope.createUnit(prefix: String = ""): Expr.Const {
  return createUnitType(prefix).address
}

private fun ConfigureContextScope.createUnitType(prefix: String = ""): TypedAllocation {
  val files0 = create("IsofsFile", "${prefix}files0", elements = 2)
  val files1 = create("IsofsFile", "${prefix}files1", elements = 2)

  val fdw = create("IsofsFdw", "${prefix}fdw", elements = 2)
  fdw.writeField("0|fd.filesCount", 2)
  fdw.writeField("0|fd.files", files0)
  fdw.writeField("1|fd.filesCount", 2)
  fdw.writeField("1|fd.files", files1)

  val unit = create("IsofsUnit", "${prefix}unit")
  unit.writeField("paths", allocate())
  unit.writeField("fdw", fdw)
  unit.writeField("pathTable", allocate())
  unit.writeField("handlerIndex", 0)
  assume { unit.fieldExpr("fdwCount", unsigned = true) le 2 }
  return unit
}

private fun ConfigureContextScope.createFile(): Expr.Const {
  val file = create("IsofsFile", "file")
  return file.address
}
