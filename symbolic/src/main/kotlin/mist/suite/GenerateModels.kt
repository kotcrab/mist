package mist.suite

import kio.util.child
import kio.util.readJson
import kio.util.writeJson
import mist.module.ModuleFunction
import mist.module.toCoverageSummary
import mist.symbolic.Context
import mist.symbolic.Engine
import mist.util.cached
import java.io.File

class GenerateModels(
  private val suite: Suite,
  private val suiteConfig: SuiteConfig,
  private val modelsOutDir: File
) {
  private val module = suite.fwModule
  private val executedAddresses = mutableSetOf<Int>()

  private val functionsToExecute = module.functions.values
    .filter {
      it.type == ModuleFunction.Type.EXPORT ||
        (it.type == ModuleFunction.Type.IMPLEMENTATION && suiteConfig.executeAllImplementationFunctions) ||
        it.name in suiteConfig.additionalFunctionsToExecute
    }
    .filterNot { it.name in suiteConfig.excludedFunctions }

  fun execute() {
    modelsOutDir.mkdir()
    executeFunctions()
    printCoverages()
  }

  private fun executeFunctions() {
    functionsToExecute.onEachIndexed { index, function ->
      println("\n\nRunning ${index + 1}/${functionsToExecute.size} ${function.name}...")
      executedAddresses.addAll(
        executeEngine(function)
      )
    }
  }

  private fun printCoverages() {
    println("----- Coverages -----")
    functionsToExecute.forEach {
      println("\n--- ${it.name} ---")
      println(module.disassembleWithCoverage(it, executedAddresses))
      println("------------------\n")
    }
    println("---")
    println(module.calculateCoverages(executedAddresses, suiteConfig.excludedFunctions).toCoverageSummary())
    println("---")
  }

  private fun executeEngine(function: ModuleFunction): Set<Int> {
    val ctx = Context.presetSymbolic()
    val testConfig = suiteConfig.testConfigs[function.name]
    val contextInitScope = SuiteConfig.ContextInitScope(module, ctx)
    val moduleMemory = module.createModuleMemory()
    if (testConfig?.initContextWithModuleMemory == true) {
      module.writeMemoryToContext(ctx)
    }
    suiteConfig.commonContextConfigure.invoke(contextInitScope)
    testConfig?.testContextConfigure?.invoke(contextInitScope)
    ctx.pc = function.entryPoint
    val suiteFunctionLibrary = suiteConfig.functionLibraryProvider.invoke(moduleMemory)
    val testFunctionLibrary = testConfig?.functionLibraryTransform?.invoke(suiteFunctionLibrary) ?: suiteFunctionLibrary
    val functionModelsOutDir = modelsOutDir.child(function.name)
    val executedAddressesFile = functionModelsOutDir.child("_executedAddresses.txt")
    if (executedAddressesFile.exists()) {
      println("Function already processed, skipping")
      return executedAddressesFile.readJson()
    }
    if (functionModelsOutDir.exists()) {
      println("Deleting incomplete results...")
      functionModelsOutDir.deleteRecursively()
    }
    functionModelsOutDir.mkdir()
    val executedAddresses = Engine(
      binLoader = moduleMemory.loader,
      disassembler = suite.disassembler.cached(),
      module = module,
      moduleTypes = suite.moduleTypes,
      functionLibrary = testFunctionLibrary,
      name = function.name,
      tracing = false,
      modelsOutDir = functionModelsOutDir
    ).executeSymbolic(ctx)
    functionModelsOutDir.child("_coverage.txt").writeText(module.disassembleWithCoverage(function, executedAddresses))
    executedAddressesFile.writeJson(executedAddresses)
    return executedAddresses
  }
}
