package mist.symbolic

class FunctionLibrary(
  private val functionHandlers: Map<String, FunctionHandler> = emptyMap(),
) {
  constructor(functionHandlers: List<NamedFunctionHandler>) : this(functionHandlers.asFunctionMap())

  fun extendWith(newFunctionHandlers: List<NamedFunctionHandler>): FunctionLibrary {
    return FunctionLibrary(functionHandlers + newFunctionHandlers.asFunctionMap())
  }

  fun transform(mapper: (String, FunctionHandler) -> FunctionHandler): FunctionLibrary {
    return FunctionLibrary(functionHandlers.mapValues { mapper(it.key, it.value) })
  }

  fun supports(name: String): Boolean {
    return name in functionHandlers
  }

  fun getOrThrow(name: String): FunctionHandler {
    return functionHandlers[name]
      ?: error("Missing function handler for $name")
  }

  fun get(name: String): FunctionHandler {
    return functionHandlers.getOrElse(name) {
      println("WARN: Using default handler for function: $name")
      DefaultFunctionHandler
    }
  }
}

private fun List<NamedFunctionHandler>.asFunctionMap() = associateBy { it.name }
