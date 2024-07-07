package mist.symbolic

enum class UnaryOp {
  ZebMem,
  SebMem,
  ZehMem,
  SehMem,
  Seb,
  Seh,
}

enum class BinaryOp {
  Add,
  Sub,
  Max,
  Min,
  Slt,
  Sltu,
  And,
  Or,
  Xor,
  Nor,
  Sll,
  Srl,
  Sra,
  MultLo,
  MultHi,
  MultuLo,
  MultuHi,
  Div,
  Divu,
  Mod,
  Modu,
}

enum class ConditionOp {
  Eq,
  Neq,
  Ge,
  Gt,
  Le,
  Lt,
}
