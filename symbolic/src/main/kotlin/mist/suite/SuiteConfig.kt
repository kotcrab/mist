package mist.suite

import mist.asm.mips.GprReg
import mist.module.Module
import mist.module.ModuleMemory
import mist.suite.SuiteConfig.ContextInitScope
import mist.symbolic.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun suiteConfig(moduleName: String, configure: SuiteConfig.() -> Unit): SuiteConfig {
  val suiteConfig = SuiteConfig(moduleName)
  suiteConfig.configure()
  return suiteConfig
}

@SuiteConfigDsl
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

  fun test(functionName: String, configure: (SuiteTestConfig.() -> Unit)? = null) {
    additionalFunctionsToExecute.add(functionName)
    if (configure != null) {
      val config = SuiteTestConfig()
      config.configure()
      testConfigs[functionName] = config
    }
  }

  fun ignoreComparingArgsOf(functionName: String, vararg args: Int) {
    functionArgsIgnoredForCompare[functionName] = args.toList()
  }

  fun overrideElfFunctionName(actualName: String, overrideName: String) {
    elfFunctionNameOverrides[actualName] = overrideName
  }

  @SuiteConfigDsl
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

    fun create(typeName: String, name: String, elements: Int = 1, initByte: Int? = null): TypedAllocation {
      val type = module.findTypeOrThrow(typeName)
      val buffer = Expr.Const.of(ctx.memory.allocate(type, name, elements, initByte))
      return TypedAllocation(ctx, module.types, buffer, type, elements)
    }

    fun allocate(typeName: String, name: String, elements: Int = 1, initByte: Int? = null): Expr.Const {
      val type = module.findTypeOrThrow(typeName)
      return Expr.Const.of(ctx.memory.allocate(type, name, elements, initByte))
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

@SuiteConfigDsl
class SuiteTestConfig {
  var testContextConfigure: ContextInitScope.() -> Unit = { }
    private set
  var functionLibraryTransform: (FunctionLibrary) -> FunctionLibrary = { it }
    private set
  val functionArgsIgnoredForCompare = mutableMapOf<String, List<Int>>()
  var prove: Prove? = null
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

  fun proveEquality(
    functionCalls: Boolean = true,
    functionReturns: Boolean = true,
    excludedAllocations: Set<String> = emptySet(),
    timeout: Duration = 1.minutes
  ) {
    prove = Prove(
      functionCalls = functionCalls,
      functionReturns = functionReturns,
      excludedAllocations = excludedAllocations,
      timeout = timeout,
    )
  }

  data class Prove(
    val functionCalls: Boolean,
    val functionReturns: Boolean,
    val excludedAllocations: Set<String>,
    val timeout: Duration
  )
}

@DslMarker
annotation class SuiteConfigDsl
