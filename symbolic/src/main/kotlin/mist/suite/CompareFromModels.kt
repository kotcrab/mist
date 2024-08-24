package mist.suite

import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.KBv32Sort
import io.ksmt.utils.cast
import kio.util.child
import kio.util.execute
import kotlinx.coroutines.*
import mist.module.Module
import mist.symbolic.*
import mist.util.cached
import java.io.File
import kotlin.time.measureTimedValue

class CompareFromModels(
  private val suite: Suite,
  private val suiteConfig: SuiteConfig,
  modelsDir: File,
  private val outDir: File,
  private val writeAllTraces: Boolean = false,
  parallelism: Int = 12,
) {
  private val modelLoader = ModelLoader()
  private val traceWriter = TraceWriter()
  private val traceComparator = TraceComparator(traceWriter)
  private val functionsDirs = modelsDir.listFiles()?.filter { it.isDirectory }
    ?.filterNot { it.name in suiteConfig.excludedFunctions }
    ?: error("Function models dir can't be read")

  @OptIn(ExperimentalCoroutinesApi::class)
  private val taskDispatcher = Dispatchers.IO.limitedParallelism(parallelism)

  fun execute() = runBlocking {
    preCheckTestedFunctions()
    val completedCases = measureTimedValue { testFunctions() }
    println("Completed tests in ${completedCases.duration}")
    writeTestResults(completedCases.value)
  }

  private fun preCheckTestedFunctions() {
    println("Pre-check all tested functions exist")
    functionsDirs.forEach { functionDir ->
      suite.fwModule.getFunctionOrThrow(functionDir.name)
      suite.uofwModule.getFunctionOrThrow(functionDir.name)
    }
  }

  private suspend fun testFunctions(): List<CompletedCase> {
    return functionsDirs.flatMapIndexed { functionIdx, functionDir ->
      testFunction(functionIdx, functionDir)
    }
  }

  private suspend fun testFunction(functionIdx: Int, functionDir: File): List<CompletedCase> = coroutineScope {
    println("Testing function ${functionIdx + 1}/${functionsDirs.size}: ${functionDir.name}")
    val modelFiles = functionDir.listFiles()
      ?.filter { it.isFile && !it.name.startsWith("_") }
      ?: emptyList()
    modelFiles
      .mapIndexed { modelIdx, modelFile ->
        async(taskDispatcher) {
          println("Running test ${modelIdx + 1}/${modelFiles.size}...")
          val functionName = modelFile.parentFile.name
          val testConfig = suiteConfig.testConfigs[functionName]
          val functionArgsIgnoredForCompare = suite.functionArgsIgnoredForCompare + (testConfig?.functionArgsIgnoredForCompare ?: emptyMap())
          val fwTrace = executeFunction(modelFile, suite.fwModule)
          val uofwTrace = executeFunction(modelFile, suite.uofwModule)
          val proveMessages = proveOutputs(modelFile, suite, fwTrace, uofwTrace, functionArgsIgnoredForCompare)

          CompletedCase(
            fwTrace = fwTrace,
            uofwTrace = uofwTrace,
            outName = "${functionDir.name}.${modelFile.nameWithoutExtension}.txt",
            traceComparator.compareTraces(
              functionArgsIgnoredForCompare,
              suite.fwModule,
              suite.uofwModule,
              fwTrace,
              uofwTrace
            ),
            proveMessages,
          )
        }
      }
      .map { it.await() }
      .filter { writeAllTraces || it.traceCompareMessages.isNotEmpty() || it.proveMessages.isNotEmpty() }
  }

  private fun proveOutputs(
    modelFile: File,
    suite: Suite,
    fwTrace: Trace,
    uofwTrace: Trace,
    functionArgsIgnoredForCompare: Map<String, Set<Int>>
  ): List<String> {
    val functionName = modelFile.parentFile.name
    val testConfig = suiteConfig.testConfigs[functionName]
      ?: return emptyList()
    val proveConfig = testConfig.prove
      ?: return emptyList()
    val fwCtx = executeSymbolicSpecific(modelFile, suite.fwModule, fwTrace)
    val uofwCtx = executeSymbolicSpecific(modelFile, suite.uofwModule, uofwTrace)
    val proveMessages = mutableListOf<String>()
    SolverContext().use { solverCtx ->
      val fwSolver = fwCtx.createDetachedSolver(solverCtx, "fw_")
      val uofwSolver = uofwCtx.createDetachedSolver(solverCtx, "uofw_")

      fun assertExpr(fwExpr: BvExpr, uofwExpr: BvExpr) {
        val fwKExpr: KExpr<KBv32Sort> = fwSolver.cachedSolverExpr(fwExpr).cast()
        val uofwKExpr: KExpr<KBv32Sort> = uofwSolver.cachedSolverExpr(uofwExpr).cast()
        solverCtx.solver.assert(solverCtx.kCtx.mkEq(fwKExpr, uofwKExpr))
      }

      val allocationNamesToProve = fwTrace.additionalAllocations.map { it.first.name }
        .toSet()
        .plus(uofwTrace.additionalAllocations.map { it.first.name })
        .filterNot { it in proveConfig.excludedAllocations }

      allocationNamesToProve.forEach { allocationName ->
        val (fwSymbol, _) = fwTrace.additionalAllocations.firstOrNull { it.first.name == allocationName }
          ?: error("No such named FW allocation: $allocationName")
        val (uofwSymbol, _) = uofwTrace.additionalAllocations.firstOrNull { it.first.name == allocationName }
          ?: error("No such named uOFW allocation: $allocationName")
        repeat(fwSymbol.length) { offset ->
          assertExpr(
            fwCtx.memory.selectByte(Expr.Const.of(fwSymbol.address.toInt()).plus(offset)),
            uofwCtx.memory.selectByte(Expr.Const.of(uofwSymbol.address.toInt()).plus(offset)),
          )
        }
      }

      if (proveConfig.functionCalls) {
        fwTrace.elements.filterIsInstance<TraceElement.FunctionCall>()
          .zip(uofwTrace.elements.filterIsInstance<TraceElement.FunctionCall>())
          .forEach { (fwCall, uofwCall) ->
            val ignoredArgIndexes = functionArgsIgnoredForCompare[fwCall.name] ?: emptyList()
            fwCall.arguments.zip(uofwCall.arguments).forEachIndexed { index, (fwArg, uofwArg) ->
              if (index !in ignoredArgIndexes) {
                assertExpr(fwArg, uofwArg)
              }
            }
          }
      }

      if (proveConfig.functionReturns) {
        fwTrace.elements.filterIsInstance<TraceElement.FunctionReturn>()
          .zip(uofwTrace.elements.filterIsInstance<TraceElement.FunctionReturn>())
          .forEach { (fwReturn, uofwReturn) ->
            if (fwReturn.returnsV0()) {
              assertExpr(fwReturn.v0, uofwReturn.v0)
            }
            if (fwReturn.returnsV1()) {
              assertExpr(fwReturn.v1, uofwReturn.v1)
            }
          }
      }

      println("Trying to prove outputs...")
      val status = solverCtx.solver.check(proveConfig.timeout)
      if (status != KSolverStatus.SAT) {
        proveMessages.add("Failed to prove outputs, result is $status")
      }
      println("Prove result: $status")
    }
    return proveMessages
  }

  private fun executeFunction(modelFile: File, module: Module): Trace {
    val function = module.getFunctionOrThrow(modelFile.parentFile.name)
    val ctx = Context()
    val testConfig = suiteConfig.testConfigs[function.name]
    val contextInitScope = SuiteConfig.ContextInitScope(module, ctx)
    val moduleMemory = module.createModuleMemory()
    if (testConfig?.initContextWithModuleMemory == true) {
      module.writeMemoryToContext(ctx)
    }
    suiteConfig.commonContextConfigure.invoke(contextInitScope)
    testConfig?.testContextConfigure?.invoke(contextInitScope)
    ctx.pc = function.entryPoint
    ctx.memory.ignoreIllegalAccess = true
    val suiteFunctionLibrary = suiteConfig.functionLibraryProvider.invoke(moduleMemory)
    val testFunctionLibrary = testConfig?.functionLibraryTransform?.invoke(suiteFunctionLibrary) ?: suiteFunctionLibrary
    val modelFunctionLibrary = modelLoader.loadFromFile(modelFile, ctx, testFunctionLibrary)
    return Engine(
      binLoader = moduleMemory.loader,
      disassembler = suite.disassembler.cached(),
      module = module,
      moduleTypes = suite.moduleTypes,
      functionLibrary = modelFunctionLibrary,
      name = function.name,
      tracing = true,
      modelsOutDir = null,
    ).executeConcrete(ctx)
  }

  private fun executeSymbolicSpecific(modelFile: File, module: Module, trace: Trace): Context {
    val functionName = modelFile.parentFile.name
    val function = module.getFunctionOrThrow(functionName)
    val ctx = Context.presetSymbolic()
    val testConfig = suiteConfig.testConfigs[functionName]
    val contextInitScope = SuiteConfig.ContextInitScope(module, ctx)
    val moduleMemory = module.createModuleMemory()
    if (testConfig?.initContextWithModuleMemory == true) {
      module.writeMemoryToContext(ctx)
    }
    suiteConfig.commonContextConfigure.invoke(contextInitScope)
    testConfig?.testContextConfigure?.invoke(contextInitScope)
    ctx.pc = function.entryPoint
    ctx.specificBranches.addAll(trace.elements.filterIsInstance<TraceElement.Branch>().map { it.taken })
    val suiteFunctionLibrary = suiteConfig.functionLibraryProvider.invoke(moduleMemory)
    val testFunctionLibrary = testConfig?.functionLibraryTransform?.invoke(suiteFunctionLibrary) ?: suiteFunctionLibrary
    Engine(
      binLoader = moduleMemory.loader,
      disassembler = suite.disassembler.cached(),
      module = module,
      moduleTypes = suite.moduleTypes,
      functionLibrary = testFunctionLibrary,
      name = function.name,
      tracing = true,
      modelsOutDir = null
    ).executeSymbolic(ctx)
    return ctx
  }

  private fun writeTestResults(completedCases: List<CompletedCase>) {
    println("\nWriting results...")
    val suiteOutDir = outDir.child(suiteConfig.moduleName).also {
      it.deleteRecursively()
      it.mkdir()
    }
    if (completedCases.isEmpty()) {
      println("Nothing to write, either all traces matched or no tests were run")
      return
    }

    execute("git", listOf("init"), workingDirectory = suiteOutDir)

    val outDirs = TraceComparator.MessageLevel.entries
      .associateWith { suiteOutDir.child(it.name.lowercase()) }
      .onEach { it.value.mkdir() }

    println("Writing FW results")
    completedCases.forEach {
      val outFile = outDirs.getValue(it.traceCompareMessages.highestMessageLevel()).child(it.outName)
      traceWriter.writeToFile(suite.fwModule, it.fwTrace, outFile, it.traceCompareMessages, it.proveMessages)
    }
    execute("git", listOf("add", "-A"), workingDirectory = suiteOutDir)
    execute("git", listOf("commit", "-m", "init"), workingDirectory = suiteOutDir)

    println("Writing uOFW results")
    completedCases.forEach {
      val outFile = outDirs.getValue(it.traceCompareMessages.highestMessageLevel()).child(it.outName)
      traceWriter.writeToFile(suite.uofwModule, it.uofwTrace, outFile, it.traceCompareMessages, it.proveMessages)
    }
  }

  private data class CompletedCase(
    val fwTrace: Trace,
    val uofwTrace: Trace,
    val outName: String,
    val traceCompareMessages: List<TraceComparator.Message>,
    val proveMessages: List<String>,
  )
}
