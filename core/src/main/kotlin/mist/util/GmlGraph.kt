/*
 * mist - interactive disassembler and decompiler
 * Copyright (C) 2018 Pawel Pastuszak
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package mist.util

import mist.util.GmlAtrib.*
import java.io.File
import java.io.Reader
import java.nio.charset.Charset

/** @author Kotcrab */

fun parseGml(file: File, charset: Charset = Charsets.UTF_8): GmlArray {
  return parseGml(file.reader(charset))
}

fun parseGml(reader: Reader): GmlArray {
  val floatRegex = Regex("^[-+]?[0-9]*\\.[0-9]+$")
  val intRegex = Regex("^[-+]?[0-9]+$")
  val parentStack = mutableListOf<GmlArray>()
  parentStack.add(GmlArray("root"))
  var name: String? = null
  reader.forEachLine { inLine ->
    val line = inLine.replace("\t", " ")
    var i = 0
    while (i < line.length && i != -1) {
      if (Character.isWhitespace(line[i])) {
        i++
      } else if (line[i] == '#') {
        i = line.length
      } else if (name == null) {
        val end = line.indexOf(" ", i)
        name = line.substring(i, if (end == -1) line.length else end)
        if (name!![0] == ']') {
          parentStack.removeAt(parentStack.lastIndex)
          name = null
        }
        i = end
      } else {
        val end = line.indexOf(" ", i)
        val localVal = line.substring(i, if (end == -1) line.length else end)
        when {
          localVal[0] == '[' -> {
            val newArr = GmlArray(name!!)
            parentStack.last().atribs.add(newArr)
            parentStack.add(newArr)
          }
          localVal[0] == ']' -> {
            parentStack.removeAt(parentStack.lastIndex)
          }
          else -> {
            parentStack.last().atribs.add(
              when {
                localVal.matches(floatRegex) -> GmlFloat(name!!, localVal.toFloat())
                localVal.matches(intRegex) -> GmlInt(name!!, localVal.toInt())
                else -> GmlString(name!!, localVal)
              }
            )
          }
        }
        i = end
        name = null
      }
    }
  }
  return parentStack[0]["graph", 0] as GmlArray
}

fun getGmlNodes(graph: GmlArray): List<GmlNode> {
  val nodes = mutableListOf<GmlNode>()
  graph["node"]
    .map { it as GmlArray }
    .forEach { node ->
      val graphics = node["graphics", 0]
      nodes.add(
        GmlNode(
          node.getInt("id"),
          graphics.getFloat("x"),
          graphics.getFloat("y"),
          graphics.getFloat("w"),
          graphics.getFloat("h")
        )
      )
    }
  return nodes
}

fun getGmlEdges(graph: GmlArray): List<GmlEdge> {
  val edges = mutableListOf<GmlEdge>()
  graph["edge"]
    .map { it as GmlArray }
    .forEach { edge ->
      val graphics = edge["graphics", 0]
      val line = graphics["Line"]
      val source = edge.getInt("source")
      val target = edge.getInt("target")
      if (line.isEmpty()) {
        edges.add(GmlEdge(source, target, null))
      } else {
        val points = mutableListOf<GmlPoint>()
        line[0]["point"].map { it as GmlArray }.forEach { point ->
          points.add(GmlPoint(point.getFloat("x"), point.getFloat("y")))
        }
        edges.add(GmlEdge(source, target, points))
      }
    }
  return edges
}


sealed class GmlAtrib(val name: String) {
  class GmlString(name: String, val value: String) : GmlAtrib(name) {
    override fun toString(): String {
      return "$name = $value"
    }
  }

  class GmlInt(name: String, val value: Int) : GmlAtrib(name) {
    override fun toString(): String {
      return "$name = $value"
    }
  }

  class GmlFloat(name: String, val value: Float) : GmlAtrib(name) {
    override fun toString(): String {
      return "$name = $value"
    }
  }

  class GmlArray(name: String, val atribs: MutableList<GmlAtrib> = mutableListOf()) : GmlAtrib(name)

  fun asString(): String {
    return (this as GmlString).value
  }

  fun asInt(): Int {
    return (this as GmlInt).value
  }

  fun asFloat(): Float {
    return (this as GmlFloat).value
  }

  fun asList(): List<GmlAtrib> {
    return (this as GmlArray).atribs
  }

  operator fun get(name: String): List<GmlAtrib> {
    return asList().filter { it.name == name }
  }

  operator fun get(name: String, index: Int): GmlAtrib {
    return asList().filter { it.name == name }[index]
  }

  fun getInt(name: String): Int {
    return get(name, 0).asInt()
  }

  fun getFloat(name: String): Float {
    return get(name, 0).asFloat()
  }

  fun getString(name: String): String {
    return get(name, 0).asString()
  }
}

class GmlNode(val id: Int, val x: Float, val y: Float, val w: Float, val h: Float)

class GmlEdge(val source: Int, val target: Int, val path: List<GmlPoint>?)

class GmlPoint(val x: Float, val y: Float)
