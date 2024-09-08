package mist.suite

import kio.util.child
import mist.asm.mips.allegrex.AllegrexDisassembler
import mist.ghidra.GhidraClient
import mist.module.*
import java.io.File

class Suite(
  uofwDir: File,
  private val config: SuiteConfig
) {
  // TODO more generic names
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
      uofwDir.child("debug/map/${config.moduleName}.map"),
      moduleExports,
      config.elfFunctionNameOverrides,
    )
    println("Modules loaded")

    config.globals.forEach { (name, init) ->
      val global = fwModule.registerGlobal(name, init)
      uofwModule.registerGlobal(global.symbol.name, global.symbol.length, global.type, init)
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
      val functionArgs = moduleTypes.getFunctionArgs(functionName)
      val fwArgsPathNames = fwGhidraFunction?.parameters?.map { it.dataTypePathName }
      if (functionArgs == null) {
        if (fwArgsPathNames != null) {
          messages.add("Unknown args for: $functionName, assuming from fw: $fwArgsPathNames")
          moduleTypes.addFunctionArgsOverrideFromDataTypes(functionName, fwArgsPathNames)
        } else {
          messages.add("Unknown args for: $functionName")
        }
      }
      val returnSize = moduleTypes.getFunctionReturnSize(functionName)
      val fwReturns = fwGhidraFunction?.returnTypePathName?.let { moduleTypes.get(it)?.length }
      if (returnSize == null) {
        if (fwReturns != null) {
          messages.add("Unknown return size for: $functionName, assuming from fw: $fwReturns")
          moduleTypes.addFunctionReturnSizeOverride(functionName, fwReturns)
        } else {
          messages.add("Unknown return size for: $functionName")
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
