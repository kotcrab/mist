package mist.ghidra.model

data class GhidraMemoryBlocks(
  val memoryBlocks: List<GhidraMemoryBlock>
)

data class GhidraMemoryBlock(
  val name: String,
  val comment: String,
  val sourceName: String,
  val addressSpaceName: String,
  val start: Long,
  val end: Long,
  val size: Long,
  val read: Boolean,
  val write: Boolean,
  val execute: Boolean,
  val volatile: Boolean,
  val overlay: Boolean,
  val initialized: Boolean,
  val mapped: Boolean,
  val external: Boolean,
  val loaded: Boolean,
)
