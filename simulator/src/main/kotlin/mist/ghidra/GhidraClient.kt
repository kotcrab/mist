@file:OptIn(ExperimentalEncodingApi::class)

package mist.ghidra

import com.fasterxml.jackson.databind.ObjectMapper
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

    fun defaultAltPort(): GhidraClient {
      return GhidraClient("http://localhost:18490")
    }
  }

  fun getFunctions(): List<GhidraFunction> {
    val response = httpGet {
      url("$baseUrl/v1/functions")
    }
    require(response.code() == 200)
    return objectMapper.readValue<GhidraFunctions>(response.asStringOrThrow()).functions
  }

  fun getMemory(address: Long, length: Int): ByteArray {
    val response = httpGet {
      url("$baseUrl/v1/memory")
      param {
        "address" to address.toWHex()
        "length" to length
      }
    }
    require(response.code() == 200)
    val memoryBase64 = objectMapper.readValue<GhidraMemory>(response.asStringOrThrow()).memory
    return Base64.decode(memoryBase64)
  }

  fun getMemoryBlocks(): List<GhidraMemoryBlock> {
    val response = httpGet {
      url("$baseUrl/v1/memory-blocks")
    }
    require(response.code() == 200)
    return objectMapper.readValue<GhidraMemoryBlocks>(response.asStringOrThrow()).memoryBlocks
  }

  fun getSymbols(): List<GhidraSymbol> {
    val response = httpGet {
      url("$baseUrl/v1/symbols")
    }
    require(response.code() == 200)
    return objectMapper.readValue<GhidraSymbols>(response.asStringOrThrow()).symbols
  }

  fun getTypes(): List<GhidraType> {
    val response = httpGet {
      url("$baseUrl/v1/types")
    }
    require(response.code() == 200)
    return objectMapper.readValue<GhidraTypes>(response.asStringOrThrow()).types
  }

  private fun Response.asStringOrThrow(): String = asString() ?: error("No response body")
}
