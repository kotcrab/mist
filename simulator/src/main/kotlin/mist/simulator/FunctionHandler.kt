package mist.simulator

import mist.asm.mips.GprReg

interface FunctionHandler {
    fun handle(ctx: Context)
}

interface NamedFunctionHandler : FunctionHandler {
    val name: String
}

class ResultFunctionHandler(
    override val name: String,
    private val resultProvider: Context.() -> Int
) : NamedFunctionHandler {
    override fun handle(ctx: Context) {
        val result = resultProvider(ctx)
        DefaultFunctionHandler.handle(ctx)
        ctx.writeGpr(GprReg.V0, result)
    }
}

object DefaultFunctionHandler : FunctionHandler {
    private const val DEAD_VALUE = 0xDEADBEEF.toInt()

    private val nullifiedRegisters = arrayOf(
        GprReg.At, GprReg.V0, GprReg.V1,
        GprReg.A0, GprReg.A1, GprReg.A2, GprReg.A3,
        GprReg.T0, GprReg.T1, GprReg.T2, GprReg.T3, GprReg.T4,
        GprReg.T5, GprReg.T6, GprReg.T7, GprReg.T8, GprReg.T9,
    )

    override fun handle(ctx: Context) {
        nullifiedRegisters.forEach {
            ctx.writeGpr(it, DEAD_VALUE)
            ctx.lo = DEAD_VALUE
            ctx.hi = DEAD_VALUE
        }
    }
}
