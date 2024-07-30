package mist.suite

import kio.util.child
import mist.asm.mips.allegrex.AllegrexDisassembler
import mist.ghidra.GhidraClient
import mist.module.*
import mist.suite.SuiteConfig.ContextInitScope
import mist.symbolic.BvExpr
import mist.symbolic.Context
import mist.symbolic.Expr
import mist.symbolic.FunctionLibrary
import java.io.File

fun suiteConfig(moduleName: String, configure: SuiteConfig.() -> Unit): SuiteConfig {
  val suiteConfig = SuiteConfig(moduleName)
  suiteConfig.configure()
  return suiteConfig
}

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

  fun test(functionName: String, configure: SuiteTestConfig.() -> Unit) {
    val config = SuiteTestConfig()
    config.configure()
    testConfigs[functionName] = config
  }

  fun ignoreComparingArgsOf(functionName: String, vararg args: Int) {
    functionArgsIgnoredForCompare[functionName] = args.toList()
  }

  class ContextInitScope(
    private val module: Module,
    val ctx: Context
  ) {
    fun writeSymbolField(path: String, value: Int) {
      writeSymbolField(path, Expr.Const.of(value))
    }

    fun writeSymbolField(path: String, value: BvExpr) {
      val (address, size) = module.lookupGlobalMember(path)
      writeMemory(address, size, value)
    }

    fun writeSymbol(name: String, value: BvExpr) {
      val (address, size) = module.lookupGlobal(name)
      writeMemory(address, size, value)
    }

    private fun writeMemory(address: Int, size: Int, value: BvExpr) {
      when (size) {
        1 -> ctx.memory.writeByte(Expr.Const.of(address), value)
        2 -> ctx.memory.writeHalf(Expr.Const.of(address), value)
        4 -> ctx.memory.writeWord(Expr.Const.of(address), value)
        else -> error("Can't write to memory, not a standard size: $size")
      }
    }
  }
}

class SuiteTestConfig {
  var testContextConfigure: ContextInitScope.() -> Unit = { }
    private set

  fun configureContext(configure: ContextInitScope.() -> Unit) {
    testContextConfigure = configure
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
    val functionNames = fwModule.functions.values.map { it.name }.toSet() + uofwModule.functions.values.map { it.name }.toSet()
    val messages = mutableListOf<String>()
    functionNames.forEach { functionName ->
      val function1 = fwModule.getFunction(functionName)
      val function2 = uofwModule.getFunction(functionName)
      if (function1?.type != function2?.type) {
        messages.add("Type mismatch: ${functionName}: ${function1?.type} != ${function2?.type}")
      }
      val fwGhidraFunction = fwModule.getGhidraFunctionOrNull(functionName)
      val argsCount = moduleTypes.getFunctionArgsCount(functionName)
      val fwArgsCount = fwGhidraFunction?.parameters?.size
      if (argsCount == null) {
        messages.add("Unknown number of args: $functionName, assuming from fw: $fwArgsCount")
        if (fwArgsCount != null) {
          moduleTypes.addFunctionArgsCountOverride(functionName, fwArgsCount)
        }
      }
      val returnSize = moduleTypes.getFunctionReturnSize(functionName)
      val fwReturns = fwGhidraFunction?.returnTypePathName?.let { moduleTypes.get(it)?.length?.div(4) }
      if (returnSize == null) {
        messages.add("Unknown return size: $functionName, assuming from fw: $fwReturns")
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
