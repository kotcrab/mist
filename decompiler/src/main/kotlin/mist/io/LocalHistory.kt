package mist.io

import kio.LERandomAccessFile
import kio.util.child
import java.io.File
import java.time.Instant
import java.util.zip.Deflater
import java.util.zip.Inflater

class LocalHistory(storeDir: File) {
  private val index = storeDir.child("localHistory.index")
  private val data = storeDir.child("localHistory.data")
  private val indexRaf: LERandomAccessFile
  private val dataRaf: LERandomAccessFile

  private var compressedOut = ByteArray(1024)
  private var indexLocked = false

  init {
    if (index.exists() == false) index.createNewFile()
    if (data.exists() == false) data.createNewFile()
    indexRaf = LERandomAccessFile(index)
    dataRaf = LERandomAccessFile(data)
  }

  fun commit(data: ByteArray) {
    if (indexLocked) error("index is locked")
    if (data.size > compressedOut.size) {
      compressedOut = ByteArray(data.size)
    }
    val compressor = Deflater()
    compressor.setInput(data)
    compressor.finish()
    val compressedSize = compressor.deflate(compressedOut)
    compressor.end()
    val offset = writeEntryData(compressedSize)
    writeEntry(LocalHistoryEntry(Instant.now().epochSecond, offset, compressedSize, data.size))
  }

  fun read(entry: LocalHistoryEntry): ByteArray {
    val compressed = ByteArray(entry.compressedSize)
    val decompressed = ByteArray(entry.decompressedSize)
    dataRaf.seek(entry.offset)
    dataRaf.readFully(compressed)
    val decompressor = Inflater()
    decompressor.setInput(compressed)
    decompressor.inflate(decompressed)
    decompressor.end()
    return decompressed
  }

  fun entryReader() = EntryReader()

  private fun writeEntryData(compressedSize: Int): Long {
    val offset = dataRaf.length()
    dataRaf.seek(offset)
    dataRaf.write(compressedOut, 0, compressedSize)
    return offset
  }

  private fun writeEntry(entry: LocalHistoryEntry) {
    indexRaf.seek(indexRaf.length())
    indexRaf.writeLong(entry.epochSecond)
    indexRaf.writeLong(entry.offset)
    indexRaf.writeInt(entry.compressedSize)
    indexRaf.writeInt(entry.decompressedSize)
  }

  fun dispose() {
    indexRaf.close()
    dataRaf.close()
  }

  inner class EntryReader : Iterator<LocalHistoryEntry> {
    private val length = indexRaf.length()
    private var currentPos = 0L

    init {
      indexRaf.seek(0)
      indexLocked = true
    }

    override fun hasNext(): Boolean {
      if (currentPos == length) {
        indexLocked = false
        return false
      }
      return true
    }

    override fun next(): LocalHistoryEntry {
      val epochSecond = indexRaf.readLong()
      val offset = indexRaf.readLong()
      val compressedSize = indexRaf.readInt()
      val decompressedSize = indexRaf.readInt()
      currentPos = indexRaf.filePointer
      return LocalHistoryEntry(epochSecond, offset, compressedSize, decompressedSize)
    }
  }
}

class LocalHistoryEntry(val epochSecond: Long, val offset: Long, val compressedSize: Int, val decompressedSize: Int)
