package mist.io

class FlowNodeDto(
  val id: Int,
  val fileOffset: Int,
  val apiName: String,
  val color: Int,
  val x: Float,
  val y: Float,
  val outEdges: List<Int>
)
