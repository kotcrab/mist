@file:OptIn(ExperimentalEncodingApi::class)

package mist.ghidra

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.rybalkinsd.kohttp.dsl.httpGet
import io.github.rybalkinsd.kohttp.ext.asString
import io.github.rybalkinsd.kohttp.ext.url
import kio.util.toWHex
import mist.ghidra.model.*
import mist.util.commonObjectMapper
import okhttp3.Response
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class GhidraClient(
  private val baseUrl: String,
  private val objectMapper: ObjectMapper = commonObjectMapper,
) {
  companion object {
    fun default(): GhidraClient {
      return GhidraClient("http://localhost:18489")
    }
  }

  fun getFunctions(): List<GhidraFunction> {
    val response = httpGet {
      url("$baseUrl/v1/functions")
    }
    check(response.code() == 200)
    return objectMapper.readValue<GhidraFunctions>(response.asStringOrThrow()).functions
  }

  fun getMemory(address: Long, length: Int): ByteArray {
    return getMemory(address.toWHex(), length)
  }

  fun getMemory(address: String, length: Int): ByteArray {
    val response = httpGet {
      url("$baseUrl/v1/memory")
      param {
        "address" to address
        "length" to length
      }
    }
    check(response.code() == 200)
    val memoryBase64 = objectMapper.readValue<GhidraMemory>(response.asStringOrThrow()).memory
    return Base64.decode(memoryBase64)
  }

  fun getMemoryBlocks(): List<GhidraMemoryBlock> {
    val response = httpGet {
      url("$baseUrl/v1/memory-blocks")
    }
    check(response.code() == 200)
    return objectMapper.readValue<GhidraMemoryBlocks>(response.asStringOrThrow()).memoryBlocks
  }

  fun getSymbols(): List<GhidraSymbol> {
    val response = httpGet {
      url("$baseUrl/v1/symbols")
    }
    check(response.code() == 200)
    return objectMapper.readValue<GhidraSymbols>(response.asStringOrThrow()).symbols
  }

  fun getRelocations(): List<GhidraRelocation> {
    val response = httpGet {
      url("$baseUrl/v1/relocations")
    }
    check(response.code() == 200)
    return objectMapper.readValue<GhidraRelocations>(response.asStringOrThrow()).relocations
  }

  fun getTypes(): List<GhidraType> {
    val response = httpGet {
      url("$baseUrl/v1/types")
    }
    check(response.code() == 200)
    return objectMapper.readValue<GhidraTypes>(response.asStringOrThrow()).types.map {
      it.copy(
        properties = when (it.kind) {
          GhidraType.Kind.ENUM -> objectMapper.convertValue<GhidraType.EnumProperties>(it.rawProperties)
          GhidraType.Kind.TYPEDEF -> objectMapper.convertValue<GhidraType.TypedefProperties>(it.rawProperties)
          GhidraType.Kind.POINTER -> objectMapper.convertValue<GhidraType.PointerProperties>(it.rawProperties)
          GhidraType.Kind.ARRAY -> objectMapper.convertValue<GhidraType.ArrayProperties>(it.rawProperties)
          GhidraType.Kind.STRUCTURE -> objectMapper.convertValue<GhidraType.CompositeProperties>(it.rawProperties)
          GhidraType.Kind.UNION -> objectMapper.convertValue<GhidraType.CompositeProperties>(it.rawProperties)
          GhidraType.Kind.FUNCTION_DEFINITION -> objectMapper.convertValue<GhidraType.FunctionDefinitionProperties>(it.rawProperties)
          GhidraType.Kind.BUILT_IN -> objectMapper.convertValue<GhidraType.BuiltInProperties>(it.rawProperties)
          GhidraType.Kind.UNKNOWN -> null
        }
      )
    }
  }

  private fun Response.asStringOrThrow(): String = asString() ?: error("No response body")
}
