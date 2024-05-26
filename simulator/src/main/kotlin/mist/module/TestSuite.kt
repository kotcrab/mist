package mist.module

import mist.asm.mips.GprReg
import mist.simulator.Context
import mist.simulator.NamedFunctionHandler
import mist.simulator.ResultFunctionHandler

fun testSuite(name: String, configure: TestSuite.() -> Unit): TestSuite {
  return TestSuite(name).apply(configure)
}

class TestSuite(
  val name: String
) {
  val onlyTestFunctions = mutableListOf<String>()
  val alsoTestFunctions = mutableListOf<Pair<String, String>>()
  val globals = mutableListOf<String>()

  private val tests = mutableMapOf<String, Test>()

  fun onlyTestFunction(functionName: String) {
    onlyTestFunctions.add(functionName)
  }

  fun alsoTestFunction(functionName: String, functionName2: String = functionName) {
    alsoTestFunctions.add(functionName to functionName2)
  }

  fun global(name: String) {
    globals.add(name)
  }

  fun test(functionName: String, noDefaultCase: Boolean = false, configure: Test.() -> Unit) {
    tests[functionName] = Test(noDefaultCase).apply(configure)
  }

  fun getTest(functionName: String): Test {
    return tests[functionName] ?: Test()
  }
}

class Test(noDefaultCase: Boolean = false) {
  var contextInit: ContextInit? = null
    private set

  val cases = mutableListOf<Case>()

  init {
    if (!noDefaultCase) {
      case { }
    }
  }

  fun beforeEach(init: ContextInit) {
    if (contextInit != null) {
      error("Init already configured")
    }
    contextInit = init
  }

  fun case(init: ContextInit) {
    cases.add(Case(init))
  }
}

class Case(val contextInit: ContextInit)

typealias ContextInit = ContextInitScope.() -> Unit

class ContextInitScope(
  private val module: Module,
  val ctx: Context
) {
  fun writeSymbolField(path: String, value: Int) {
    val (address, size) = module.lookupSymbolFieldAddress(path)
    when (size) {
      1 -> ctx.memory.writeByte(address, value)
      2 -> ctx.memory.writeHalf(address, value)
      4 -> ctx.memory.writeWord(address, value)
      else -> error("Can't write field, not a standard size: $size")
    }
  }

  fun writeSymbolWord(name: String, offset: Int, value: Int) {
    ctx.memory.writeWord(module.lookupSymbolAddress(name) + offset, value)
  }

  fun writeSymbolHalf(name: String, offset: Int, value: Int) {
    ctx.memory.writeHalf(module.lookupSymbolAddress(name) + offset, value)
  }

  fun writeSymbolByte(name: String, offset: Int, value: Int) {
    ctx.memory.writeByte(module.lookupSymbolAddress(name) + offset, value)
  }

  fun hookWord(at: Long, block: suspend SequenceScope<Int>.() -> Unit) {
    hookWord(at.toInt(), block)
  }

  fun hookWord(at: Int, block: suspend SequenceScope<Int>.() -> Unit) {
    ctx.memory.onReadWordHooks[at] = sequence(block).iterator()::next
  }

  fun hookHalf(at: Long, block: suspend SequenceScope<Int>.() -> Unit) {
    hookHalf(at.toInt(), block)
  }

  fun hookHalf(at: Int, block: suspend SequenceScope<Int>.() -> Unit) {
    ctx.memory.onReadHalfHooks[at] = sequence(block).iterator()::next
  }

  fun hookByte(at: Long, block: suspend SequenceScope<Int>.() -> Unit) {
    hookByte(at.toInt(), block)
  }

  fun hookByte(at: Int, block: suspend SequenceScope<Int>.() -> Unit) {
    ctx.memory.onReadByteHooks[at] = sequence(block).iterator()::next
  }

  fun hookFunction(name: String, constResult: Int) {
    hookFunction(ResultFunctionHandler(name) { constResult })
  }

  fun hookFunction(name: String, iterable: Iterable<Int>) {
    val iterator = iterable.iterator()
    hookFunction(ResultFunctionHandler(name) { iterator.next() })
  }

  fun hookFunction(name: String, block: suspend SequenceScope<Int>.() -> Unit) {
    val iterator = sequence(block).iterator()
    hookFunction(ResultFunctionHandler(name) { iterator.next() })
  }

  fun hookFunction(handler: NamedFunctionHandler) {
    ctx.registerFunctionHandler(handler)
  }

  fun zeroedArgs() {
    args(0, 0, 0, 0, 0, 0, 0, 0)
  }

  fun args(
    a0: Int? = null,
    a1: Int? = null,
    a2: Int? = null,
    a3: Int? = null,
    t0: Int? = null,
    t1: Int? = null,
    t2: Int? = null,
    t3: Int? = null,
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

  fun allocBuf(size: Int, initByte: Int = 0xCD): Int {
    return ctx.memory.allocBuf(size, initByte)
  }
}

