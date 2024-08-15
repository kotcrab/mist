package mist.suite

import kio.util.child
import mist.asm.mips.GprReg
import mist.asm.mips.allegrex.AllegrexDisassembler
import mist.ghidra.GhidraClient
import mist.module.*
import mist.suite.SuiteConfig.ContextInitScope
import mist.symbolic.*
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun suiteConfig(moduleName: String, configure: SuiteConfig.() -> Unit): SuiteConfig {
  val suiteConfig = SuiteConfig(moduleName)
  suiteConfig.configure()
  return suiteConfig
}

@SuiteDslMarker
class SuiteConfig(val moduleName: String) {
  val additionalFunctionsToExecute = mutableSetOf<String>()
  var executeAllImplementationFunctions = false
    private set
  val functionsToSkipExecution = mutableSetOf<String>()
  val globals = mutableSetOf<String>()
  var functionLibraryProvider: (ModuleMemory) -> FunctionLibrary = { FunctionLibrary() }
    private set
  var commonContextConfigure: ContextInitScope.() -> Unit = { }
    private set
  val testConfigs = mutableMapOf<String, SuiteTestConfig>()
  val functionArgsIgnoredForCompare = mutableMapOf<String, List<Int>>()
  val elfFunctionNameOverrides = mutableMapOf<String, String>()

  fun alsoExecute(functionName: String) {
    additionalFunctionsToExecute.add(functionName)
  }

  fun alsoExecuteAllImplementationFunctions() {
    executeAllImplementationFunctions = true
  }

  fun skipExecuting(functionName: String) {
    functionsToSkipExecution.add(functionName)
  }

  fun global(name: String) {
    globals.add(name)
  }

  fun functionLibrary(provide: (ModuleMemory) -> FunctionLibrary) {
    functionLibraryProvider = provide
  }

  fun configureContext(configure: ContextInitScope.() -> Unit) {
    commonContextConfigure = configure
  }

  fun test(functionName: String, configure: SuiteTestConfig.() -> Unit = {}) {
    additionalFunctionsToExecute.add(functionName)
    val config = SuiteTestConfig()
    config.configure()
    testConfigs[functionName] = config
  }

  fun ignoreComparingArgsOf(functionName: String, vararg args: Int) {
    functionArgsIgnoredForCompare[functionName] = args.toList()
  }

  fun overrideElfFunctionName(actualName: String, overrideName: String) {
    elfFunctionNameOverrides[actualName] = overrideName
  }

  @SuiteDslMarker
  class ContextInitScope(
    private val module: Module,
    private val ctx: Context
  ) {
    fun writeSymbolField(path: String, value: Int) {
      writeSymbolField(path, Expr.Const.of(value))
    }

    fun writeSymbolField(path: String, value: BvExpr) {
      val (address, size) = module.lookupGlobalMember(path)
      ctx.memory.write(Expr.Const.of(address), size, value)
    }

    fun writeSymbol(name: String, value: BvExpr) {
      val (address, size) = module.lookupGlobal(name)
      ctx.memory.write(Expr.Const.of(address), size, value)
    }

    fun create(typeName: String, name: String, initByte: Int? = null): AllocatedType {
      val type = module.findTypeOrThrow(typeName)
      return AllocatedType(ctx, module.types, Expr.Const.of(ctx.memory.allocate(type, name, initByte)), type)
    }

    fun allocate(typeName: String, name: String, initByte: Int? = null): Expr.Const {
      return Expr.Const.of(ctx.memory.allocate(module.findTypeOrThrow(typeName), name, initByte))
    }

    fun allocate(size: Int = 0x1000, initByte: Int? = null): Expr.Const {
      return Expr.Const.of(ctx.memory.allocate(size, initByte))
    }

    fun assume(expr: ExprBuilder.() -> BoolExpr) {
      ctx.assume(expr(ExprBuilder(ctx)))
    }

    fun args(
      a0: BvExpr? = null,
      a1: BvExpr? = null,
      a2: BvExpr? = null,
      a3: BvExpr? = null,
      t0: BvExpr? = null,
      t1: BvExpr? = null,
      t2: BvExpr? = null,
      t3: BvExpr? = null,
    ) {
      a0?.let { ctx.writeGpr(GprReg.A0, it) }
      a1?.let { ctx.writeGpr(GprReg.A1, it) }
      a2?.let { ctx.writeGpr(GprReg.A2, it) }
      a3?.let { ctx.writeGpr(GprReg.A3, it) }
      t0?.let { ctx.writeGpr(GprReg.T0, it) }
      t1?.let { ctx.writeGpr(GprReg.T1, it) }
      t2?.let { ctx.writeGpr(GprReg.T2, it) }
      t3?.let { ctx.writeGpr(GprReg.T3, it) }
    }
  }
}

@SuiteDslMarker
class SuiteTestConfig {
  var testContextConfigure: ContextInitScope.() -> Unit = { }
    private set
  var functionLibraryTransform: (FunctionLibrary) -> FunctionLibrary = { it }
    private set
  val functionArgsIgnoredForCompare = mutableMapOf<String, List<Int>>()

  var proveFunctionCalls: Boolean = false
    private set
  var proveFunctionReturns: Boolean = false
    private set
  val proveAllocations: MutableList<String> = mutableListOf()
  var proveTimeout = 1.minutes
    private set

  fun configureContext(configure: ContextInitScope.() -> Unit) {
    testContextConfigure = configure
  }

  fun functionLibrary(transform: (FunctionLibrary) -> FunctionLibrary) {
    functionLibraryTransform = transform
  }

  fun ignoreComparingArgsOf(functionName: String, vararg args: Int) {
    functionArgsIgnoredForCompare[functionName] = args.toList()
  }

  fun proveEqualityOf(
    functionCalls: Boolean = true,
    functionReturns: Boolean = true,
    allocations: List<String> = emptyList(),
    timeout: Duration = 1.minutes
  ) {
    proveFunctionCalls = functionCalls
    proveFunctionReturns = functionReturns
    proveAllocations.addAll(allocations)
    proveTimeout = timeout
  }
}

class Suite(
  uofwDir: File,
  private val config: SuiteConfig
) {
  val fwModule: GhidraModule
  val uofwModule: ElfModule
  val moduleTypes: ModuleTypes
  val disassembler = AllegrexDisassembler()
  val functionArgsIgnoredForCompare = config.functionArgsIgnoredForCompare.toMap()

  init {
    println("Loading modules")
    val moduleExports = readModuleExports(uofwDir.child("src/kd/${config.moduleName}/exports.exp"))
    val ghidraClient = GhidraClient.default()
    moduleTypes = ModuleTypes(ghidraClient)
    fwModule = GhidraModule(disassembler, moduleTypes, ghidraClient, moduleExports)
    uofwModule = ElfModule(
      disassembler,
      moduleTypes,
      uofwDir.child("src/kd/${config.moduleName}/${config.moduleName}.elf"),
      uofwDir.child("map/${config.moduleName}.map"),
      moduleExports,
      config.elfFunctionNameOverrides,
    )
    println("Modules loaded")

    config.globals.forEach {
      val (_, type) = fwModule.registerGlobal(it)
      uofwModule.registerGlobal(it, type)
    }

    checkModulesFunctions()
    checkFunctionLibrary(fwModule)
  }

  private fun checkModulesFunctions() {
    println("Checking module functions")
    // TODO print untested functions
    val functionNames = fwModule.functions.values.map { it.name }.toSet() + uofwModule.functions.values.map { it.name }.toSet()
    val messages = mutableListOf<String>()
    functionNames.forEach { functionName ->
      val function1 = fwModule.getFunction(functionName)
      val function2 = uofwModule.getFunction(functionName)
      if (function1?.type != function2?.type) {
        messages.add("Type mismatch: ${functionName}: ${function1?.type} != ${function2?.type}")
      }
      val fwGhidraFunction = fwModule.getGhidraFunctionOrNull(functionName)
      val functionArgs = moduleTypes.getFunctionArgs(functionName)
      val fwArgsPathNames = fwGhidraFunction?.parameters?.map { it.dataTypePathName }
      if (functionArgs == null) {
        messages.add("Unknown args for: $functionName, assuming from fw: $fwArgsPathNames")
        if (fwArgsPathNames != null) {
          moduleTypes.addFunctionArgsOverrideFromDataTypes(functionName, fwArgsPathNames)
        }
      }
      val returnSize = moduleTypes.getFunctionReturnSize(functionName)
      val fwReturns = fwGhidraFunction?.returnTypePathName?.let { moduleTypes.get(it)?.length?.div(4) }
      if (returnSize == null) {
        messages.add("Unknown return size for: $functionName, assuming from fw: $fwReturns")
        if (fwReturns != null) {
          moduleTypes.addFunctionReturnSizeOverride(functionName, fwReturns)
        }
      }
    }
    messages.sorted()
      .forEach { println("WARN: $it") }
  }

  private fun checkFunctionLibrary(module: Module) {
    val moduleMemory = module.createModuleMemory()
    val moduleFunctionLibrary = config.functionLibraryProvider.invoke(moduleMemory)
    module.functions.values
      .filter { it.type == ModuleFunction.Type.IMPORT }
      .forEach {
        if (!moduleFunctionLibrary.supports(it.name)) {
          println("WARN: Imported function ${it.name} is not implemented by the library")
        }
      }
  }

  fun compareFromModels(modelsDir: File, outDir: File) {
    CompareFromModels(this, config, modelsDir, outDir).execute()
  }

  fun generateModels(modelsOutDir: File) {
    GenerateModels(this, config, modelsOutDir).execute()
  }
}

@DslMarker
annotation class SuiteDslMarker
