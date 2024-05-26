package mist.module

import kio.util.child
import kio.util.execute
import mist.ghidra.GhidraClient
import mist.simulator.Trace
import mist.simulator.sumCoverages
import mist.simulator.toCoverageSummary
import java.io.File

class TestRunner(
  private val moduleExports: List<ModuleExport>,
  private val outDir: File,
  private val suite: TestSuite,
) {
  fun run() {
    val fwGhidraClient = GhidraClient.default()
    val uofwGhidraClient = GhidraClient.defaultAltPort()

    println("Loading FW module")
    val fwModule = Module(fwGhidraClient, moduleExports)
    println("Loading uOFW module")
    val uofwModule = Module(uofwGhidraClient, moduleExports)
    println("Modules loaded")

    suite.globals.forEach {
      fwModule.registerGlobal(it)
      uofwModule.registerGlobal(it)
    }

    // Functions to test
    val functionsToTest = (fwModule.functions
      .values
      .filter { it.type == ModuleFunction.Type.EXPORT }
      .map { fwFunction ->
        Pair(fwFunction, uofwModule.getFunctionOrThrow(fwFunction.name))
      } +
      suite.alsoTestFunctions.map { (fwName, uofwName) ->
        Pair(fwModule.getFunctionOrThrow(fwName), uofwModule.getFunctionOrThrow(uofwName))
      })
      .filter { if (suite.onlyTestFunctions.isEmpty()) true else it.first.name in suite.onlyTestFunctions }

    println("${functionsToTest.size} functions to test")

    val fwAllExecutedAddresses = mutableSetOf<Int>()
    val uofwAllExecutedAddresses = mutableSetOf<Int>()

    val completedTests = functionsToTest.map { (fwFunction, uofwFunction) ->
      println("\nTesting ${fwFunction.name}...")
      val test = suite.getTest(fwFunction.name)
      val fwTestExecutedAddresses = mutableSetOf<Int>()
      val uofwTestExecutedAddresses = mutableSetOf<Int>()

      val completedCases = test.cases
        .also { require(it.isNotEmpty()) { "Empty test case list" } }
        .mapIndexedNotNull { caseIndex, case ->
          var fwDone = false
          runCatching {
            println("Execute FW")
            val fwTrace = fwModule.execute(fwFunction.entryPoint) {
              test.contextInit?.invoke(ContextInitScope(fwModule, this))
              case.contextInit.invoke(ContextInitScope(fwModule, this))
            }
            fwDone = true
            println("Execute uOFW")
            val uofwTrace = uofwModule.execute(uofwFunction.entryPoint) {
              test.contextInit?.invoke(ContextInitScope(uofwModule, this))
              case.contextInit.invoke(ContextInitScope(uofwModule, this))
            }

            val fwCoverage = fwFunction.calculateCoverage(fwTrace.executedAddresses)
            val uofwCoverage = uofwFunction.calculateCoverage(uofwTrace.executedAddresses)

            fwAllExecutedAddresses.addAll(fwTrace.executedAddresses)
            fwTestExecutedAddresses.addAll(fwTrace.executedAddresses)
            uofwAllExecutedAddresses.addAll(uofwTrace.executedAddresses)
            uofwTestExecutedAddresses.addAll(uofwTrace.executedAddresses)

            println("Coverage: $fwCoverage | $uofwCoverage")
            CompletedCase(
              fwTrace = fwTrace,
              uofwTrace = uofwTrace,
              outName = "${fwFunction.name}.${caseIndex}.txt"
            )
          }
            .onFailure {
              println("Case $caseIndex FAILED: ${it.message} (fwDone=$fwDone))")
              it.printStackTrace()
            }
            .onSuccess { println("Case $caseIndex OK") }
            .getOrNull()
        }

      val fwTestCoverage = fwFunction.calculateCoverage(fwTestExecutedAddresses)
      val uofwTestCoverage = uofwFunction.calculateCoverage(uofwTestExecutedAddresses)
      println("Test coverage: $fwTestCoverage | $uofwTestCoverage")

      CompletedTest(
        fwDisassemblyWithCoverage = fwModule.disassembleWithCoverage(fwFunction, fwTestExecutedAddresses),
        fwDisassemblyOutName = "${fwFunction.name}.fw.disasm.txt",
        uofwDisassemblyWithCoverage = uofwModule.disassembleWithCoverage(
          uofwFunction,
          uofwTestExecutedAddresses
        ),
        uofwDisassemblyOutName = "${uofwFunction.name}.uofw.disasm.txt",
        completedCases = completedCases
      )
    }

    println("\nWriting results...")
    val suiteOutDir = outDir.child(suite.name).also {
      it.deleteRecursively()
      it.mkdir()
    }
    val suiteFwOutDir = suiteOutDir.child("fw").also { it.mkdir() }

    execute("git", listOf("init"), workingDirectory = suiteOutDir)

    val traceWriter = TraceWriter()
    completedTests.forEach { test ->
      suiteOutDir.child(test.fwDisassemblyOutName).writeText(test.fwDisassemblyWithCoverage)
      suiteOutDir.child(test.uofwDisassemblyOutName).writeText(test.uofwDisassemblyWithCoverage)
      test.completedCases.forEach {
        traceWriter.writeToFile(fwModule, it.fwTrace, suiteOutDir.child(it.outName))
        // traceWriter.writeToFile(fwModule, it.fwTrace, suiteFwOutDir.child(it.outName))
      }
    }

    val fwCoverages = fwModule.calculateCoverages(fwAllExecutedAddresses)
    val uofwCoverages = uofwModule.calculateCoverages(uofwAllExecutedAddresses)

    suiteOutDir.child("_coverage.fw.txt").writeText(fwCoverages.toCoverageSummary())
    suiteOutDir.child("_coverage.uofw.txt").writeText(uofwCoverages.toCoverageSummary())

    execute("git", listOf("add", "-A"), workingDirectory = suiteOutDir)
    execute("git", listOf("commit", "-m", "init"), workingDirectory = suiteOutDir)

    completedTests.forEach { test ->
      test.completedCases.forEach {
        traceWriter.writeToFile(uofwModule, it.uofwTrace, suiteOutDir.child(it.outName))
      }
    }

    println(
      "\n\nTotal coverage: ${fwCoverages.values.sumCoverages()} |" +
        " ${uofwCoverages.values.sumCoverages()}"
    )
  }

  private data class CompletedTest(
    val fwDisassemblyWithCoverage: String,
    val fwDisassemblyOutName: String,
    val uofwDisassemblyWithCoverage: String,
    val uofwDisassemblyOutName: String,
    val completedCases: List<CompletedCase>
  )

  private data class CompletedCase(
    val fwTrace: Trace,
    val uofwTrace: Trace,
    val outName: String
  )
}
