package mist.suite

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
          val fwTrace = executeFunction(modelFile, suite.fwModule)
          val uofwTrace = executeFunction(modelFile, suite.uofwModule)
          CompletedCase(
            fwTrace = fwTrace,
            uofwTrace = uofwTrace,
            outName = "${functionDir.name}.${modelFile.nameWithoutExtension}.txt",
            traceComparator.compareTraces(
              suite.functionArgsIgnoredForCompare,
              suite.fwModule,
              suite.uofwModule,
              fwTrace,
              uofwTrace
            )
          )
        }
      }
      .map { it.await() }
      .filter { writeAllTraces || it.traceCompareMessages.isNotEmpty() }
  }

  private fun executeFunction(modelFile: File, module: Module): Trace {
    val function = module.getFunctionOrThrow(modelFile.parentFile.name)
    val ctx = Context()
    val testConfig = suiteConfig.testConfigs[function.name]
    val contextInitScope = SuiteConfig.ContextInitScope(module, ctx)
    suiteConfig.commonContextConfigure.invoke(contextInitScope)
    testConfig?.testContextConfigure?.invoke(contextInitScope)
    ctx.pc = function.entryPoint
    ctx.memory.ignoreIllegalAccess = true
    val moduleMemory = module.createModuleMemory()
    val modelFunctionLibrary = modelLoader.loadFromFile(modelFile, ctx, suiteConfig.functionLibraryProvider.invoke(moduleMemory))
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

    println("Writing FW results")
    completedCases.forEach {
      traceWriter.writeToFile(suite.fwModule, it.fwTrace, suiteOutDir.child(it.outName), it.traceCompareMessages)
    }
    execute("git", listOf("add", "-A"), workingDirectory = suiteOutDir)
    execute("git", listOf("commit", "-m", "init"), workingDirectory = suiteOutDir)

    println("Writing uOFW results")
    completedCases.forEach {
      traceWriter.writeToFile(suite.uofwModule, it.uofwTrace, suiteOutDir.child(it.outName), it.traceCompareMessages)
    }
  }

  private data class CompletedCase(
    val fwTrace: Trace,
    val uofwTrace: Trace,
    val outName: String,
    val traceCompareMessages: List<TraceComparator.Message>
  )
}
