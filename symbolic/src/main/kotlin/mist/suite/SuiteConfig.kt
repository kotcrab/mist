package mist.suite

import mist.asm.mips.GprReg
import mist.module.GhidraModule
import mist.module.Module
import mist.module.ModuleMemory
import mist.symbolic.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun suiteConfig(moduleName: String, configure: SuiteConfig.Builder.() -> Unit): SuiteConfig {
  val builder = SuiteConfig.Builder(moduleName)
  builder.configure()
  return builder.build()
}

class SuiteConfig(
  val moduleName: String,
  val additionalFunctionsToExecute: Set<String>,
  val excludedFunctions: Set<String>,
  val onlyTestFunctions: Set<String>,
  val globals: Set<SuiteGlobal>,
  val functionLibraryProvider: (ModuleMemory) -> FunctionLibrary,
  val commonContextConfigure: ConfigureContextScope.() -> Unit,
  val testConfigs: Map<String, SuiteTestConfig>,
  val functionArgsIgnoredForCompare: Map<String, Set<Int>>,
  val elfFunctionNameOverrides: Map<String, String>,
  val initContextsWithGlobals: Boolean,
  val requiredSourceData: List<UIntRange>,
  val defaultProveConfig: ProveConfig?,
) {
  @SuiteConfigDsl
  class Builder(private val moduleName: String) {
    private val testConfigs = mutableMapOf<String, SuiteTestConfig>()
    private val additionalFunctionsToExecute = mutableSetOf<String>()
    private val excludedFunctions = mutableSetOf<String>()
    private val onlyTestFunctions = mutableSetOf<String>()
    private val globals = mutableSetOf<SuiteGlobal>()
    private var functionLibraryProvider: (ModuleMemory) -> FunctionLibrary = { FunctionLibrary() }
    private var commonContextConfigure: ConfigureContextScope.() -> Unit = { }
    private val functionArgsIgnoredForCompare = mutableMapOf<String, Set<Int>>()
    private val elfFunctionNameOverrides = mutableMapOf<String, String>()
    private var initContextsWithGlobals = false
    private val requiredSourceData = mutableListOf<UIntRange>()
    private var defaultProveConfig: ProveConfig? = null

    fun test(functionName: String, configure: (SuiteTestConfig.Builder.() -> Unit)? = null) {
      additionalFunctionsToExecute.add(functionName)
      if (configure != null) {
        val builder = SuiteTestConfig.Builder()
        builder.configure()
        testConfigs[functionName] = builder.build()
      }
    }

    fun exclude(functionName: String) {
      excludedFunctions.add(functionName)
    }

    fun onlyTest(functionName: String) {
      onlyTestFunctions.add(functionName)
    }

    fun global(name: String, init: Boolean = true) {
      globals.add(SuiteGlobal(name, init))
    }

    fun functionLibrary(provide: (ModuleMemory) -> FunctionLibrary) {
      functionLibraryProvider = provide
    }

    fun configureContext(configure: ConfigureContextScope.() -> Unit) {
      commonContextConfigure = configure
    }

    fun ignoreComparingArgsOf(functionName: String, vararg args: Int) {
      functionArgsIgnoredForCompare[functionName] = args.toSet()
    }

    fun overrideElfFunctionName(actualName: String, overrideName: String) {
      elfFunctionNameOverrides[actualName] = overrideName
    }

    fun initContextsWithGlobals() {
      initContextsWithGlobals = true
    }

    fun requireSourceData(range: UIntRange) {
      requiredSourceData.add(range)
    }

    fun proveAllFunctions() {
      defaultProveConfig = ProveConfig()
    }

    fun build(): SuiteConfig {
      return SuiteConfig(
        moduleName,
        additionalFunctionsToExecute,
        excludedFunctions,
        onlyTestFunctions,
        globals,
        functionLibraryProvider,
        commonContextConfigure,
        testConfigs,
        functionArgsIgnoredForCompare,
        elfFunctionNameOverrides,
        initContextsWithGlobals,
        requiredSourceData,
        defaultProveConfig,
      )
    }
  }

  fun initCtx(
    module: Module,
    ctx: Context,
    testConfig: SuiteTestConfig?
  ) {
    val configureContextScope = ConfigureContextScope(module, ctx)
    if (testConfig?.initContextWithModuleMemory == true) {
      module.writeMemoryToContext(ctx)
    }
    if (initContextsWithGlobals) {
      module.writeGlobalsToContext(ctx)
    }
    if (module is GhidraModule && requiredSourceData.isNotEmpty()) {
      val moduleMemory = module.createModuleMemory()
      requiredSourceData.forEach { range ->
        range.forEach { at ->
          ctx.memory.writeByte(Expr.Const.of(at.toInt()), Expr.Const.of(moduleMemory.readByte(at.toInt())))
        }
      }
    }
    commonContextConfigure.invoke(configureContextScope)
    testConfig?.testContextConfigure?.invoke(configureContextScope)
  }

  data class SuiteGlobal(val name: String, val init: Boolean)
}

@SuiteConfigDsl
class SuiteTestConfig(
  val testContextConfigure: ConfigureContextScope.() -> Unit,
  val functionLibraryTransform: (FunctionLibrary) -> FunctionLibrary,
  val functionArgsIgnoredForCompare: Map<String, Set<Int>>,
  val initContextWithModuleMemory: Boolean, // TODO replace uses with initContextsWithGlobals
  val proveConfig: ProveConfig?,
  val maxFinishedPaths: Int?,
) {
  class Builder {
    private var testContextConfigure: ConfigureContextScope.() -> Unit = { }
    private var functionLibraryTransform: (FunctionLibrary) -> FunctionLibrary = { it }
    private val functionArgsIgnoredForCompare = mutableMapOf<String, Set<Int>>()
    private var initContextWithModuleMemory: Boolean = false
    private var proveConfig: ProveConfig? = null
    private var maxFinishedPaths: Int? = null

    fun configureContext(configure: ConfigureContextScope.() -> Unit) {
      testContextConfigure = configure
    }

    fun functionLibrary(transform: (FunctionLibrary) -> FunctionLibrary) {
      functionLibraryTransform = transform
    }

    fun ignoreComparingArgsOf(functionName: String, vararg args: Int) {
      functionArgsIgnoredForCompare[functionName] = args.toSet()
    }

    fun initContextWithModuleMemory() {
      initContextWithModuleMemory = true
    }

    fun proveEquality(
      functionCalls: Boolean = true,
      functionReturns: Boolean = true,
      excludedGlobals: Set<String> = emptySet(),
      excludedAllocations: Set<String> = emptySet(),
      timeout: Duration = 1.minutes
    ) {
      proveConfig = ProveConfig(
        functionCalls = functionCalls,
        functionReturns = functionReturns,
        excludedGlobals = excludedGlobals,
        excludedAllocations = excludedAllocations,
        timeout = timeout,
      )
    }

    fun maxFinishedPaths(value: Int) {
      maxFinishedPaths = value
    }

    fun build(): SuiteTestConfig {
      return SuiteTestConfig(
        testContextConfigure,
        functionLibraryTransform,
        functionArgsIgnoredForCompare,
        initContextWithModuleMemory,
        proveConfig,
        maxFinishedPaths,
      )
    }
  }
}

data class ProveConfig(
  val functionCalls: Boolean = true,
  val functionReturns: Boolean = true,
  val excludedGlobals: Set<String> = emptySet(),
  val excludedAllocations: Set<String> = emptySet(),
  val timeout: Duration = 1.minutes,
)

@SuiteConfigDsl
class ConfigureContextScope(
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
    val buffer = ctx.memory.allocate(type, name, elements, initByte)
    return TypedAllocation(ctx, module.types, buffer, type, elements)
  }

  fun allocate(typeName: String, name: String, elements: Int = 1, initByte: Int? = null): Expr.Const {
    val type = module.findTypeOrThrow(typeName)
    return ctx.memory.allocate(type, name, elements, initByte)
  }

  fun allocate(size: Int = 0x1000, initByte: Int? = null): Expr.Const {
    return ctx.memory.allocate(size, initByte, track = true)
  }

  fun allocateString(text: String): Expr.Const {
    val bytes = text.toByteArray()
    val buffer = ctx.memory.allocate(bytes.size + 1, initByte = 0, track = false)
    writeBytes(buffer, bytes)
    return buffer
  }

  fun allocateAt(at: Int, size: Int, initByte: Int? = null, name: String): Expr.Const {
    return ctx.memory.allocateAt(at, size, initByte, track = true, name)
  }

  fun writeBytes(buffer: Expr.Const, bytes: ByteArray) {
    bytes.forEachIndexed { index, byte ->
      ctx.memory.writeByte(buffer.plus(index), Expr.Const.of(byte.toInt()))
    }
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

@DslMarker
private annotation class SuiteConfigDsl
