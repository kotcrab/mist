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

package mist.test.util

import kio.KioInputStream
import mist.io.BinLoader
import java.nio.charset.Charset

/** @author Kotcrab */

class MemBinLoader(bytes: ByteArray) : BinLoader {
  private val input = KioInputStream(bytes)

  override fun readInt(at: Int): Int = input.readInt(at)
  override fun readString(at: Int, charset: Charset): String = input.run {
    setPos(at)
    readNullTerminatedString(charset)
  }
}
