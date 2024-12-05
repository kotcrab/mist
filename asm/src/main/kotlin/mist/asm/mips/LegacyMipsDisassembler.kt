package mist.asm.mips

import mist.asm.DisassemblerException
import mist.asm.ImmOperand
import mist.asm.RegOperand
import mist.asm.mips.MipsDisassembler.StrictCheck.*
import mist.asm.mips.MipsOpcode.*

class LegacyMipsDisassembler(private val srcProcessor: LegacyMipsProcessor, strict: Boolean = true) :
  MipsDisassembler(srcProcessor, strict) {
  override fun disasmSpecialInstr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
    val rs = RegOperand(GprReg.forId(instr ushr 21 and 0x1F))
    val rt = RegOperand(GprReg.forId(instr ushr 16 and 0x1F))
    val rd = RegOperand(GprReg.forId(instr ushr 11 and 0x1F))
    val shift = instr ushr 6 and 0x1F
    val funct = instr and 0x3F
    val ifStrict = StrictChecker()
    ifStrict.register(ZeroRs) { rs.reg.id == 0 }
    ifStrict.register(ZeroRt) { rt.reg.id == 0 }
    ifStrict.register(ZeroRd) { rd.reg.id == 0 }
    ifStrict.register(ZeroShift) { shift == 0 }
    return when {
      funct == 0b100_000 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Add, rd, rs, rt)
      funct == 0b100_001 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Addu, rd, rs, rt)
      funct == 0b100_100 && ifStrict(ZeroShift) -> MipsInstr(vAddr, And, rd, rs, rt)
      funct == 0b001_101 -> MipsInstr(vAddr, Break, ImmOperand(instr ushr 6 and 0xFFFF))
      funct == 0b011_010 && ifStrict(ZeroRd, ZeroShift) -> MipsInstr(vAddr, Div, rs, rt)
      funct == 0b011_011 && ifStrict(ZeroRd, ZeroShift) -> MipsInstr(vAddr, Divu, rs, rt)
      funct == 0b001_001 && ifStrict(ZeroRt) -> MipsInstr(vAddr, Jalr, rd, rs)
      funct == 0b001_000 && ifStrict(ZeroRt, ZeroRd) -> MipsInstr(vAddr, Jr, rs)
      funct == 0b010_000 && ifStrict(ZeroRs, ZeroRt, ZeroShift) -> MipsInstr(vAddr, Mfhi, rd)
      funct == 0b010_010 && ifStrict(ZeroRs, ZeroRt, ZeroShift) -> MipsInstr(vAddr, Mflo, rd)
      funct == 0b001_011 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Movn, rd, rs, rt)
      funct == 0b001_010 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Movz, rd, rs, rt)
      funct == 0b010_001 && ifStrict(ZeroRt, ZeroRd, ZeroShift) -> MipsInstr(vAddr, Mthi, rs)
      funct == 0b010_011 && ifStrict(ZeroRt, ZeroRd, ZeroShift) -> MipsInstr(vAddr, Mtlo, rs)
      funct == 0b011_000 && ifStrict(ZeroRd, ZeroShift) -> MipsInstr(vAddr, Mult, rs, rt)
      funct == 0b011_001 && ifStrict(ZeroRd, ZeroShift) -> MipsInstr(vAddr, Multu, rs, rt)
      funct == 0b100_111 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Nor, rd, rs, rt)
      funct == 0b100_101 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Or, rd, rs, rt)
      funct == 0b000_000 && ifStrict(ZeroRs) -> MipsInstr(vAddr, Sll, rd, rt, ImmOperand(shift))
      funct == 0b000_100 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Sllv, rd, rt, rs)
      funct == 0b101_010 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Slt, rd, rs, rt)
      funct == 0b101_011 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Sltu, rd, rs, rt)
      funct == 0b000_011 && ifStrict(ZeroRs) -> MipsInstr(vAddr, Sra, rd, rt, ImmOperand(shift))
      funct == 0b000_111 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Srav, rd, rt, rs)
      funct == 0b000_010 && ifStrict(ZeroRs) -> MipsInstr(vAddr, Srl, rd, rt, ImmOperand(shift))
      funct == 0b000_110 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Srlv, rd, rt, rs)
      funct == 0b100_010 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Sub, rd, rs, rt)
      funct == 0b100_011 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Subu, rd, rs, rt)
      funct == 0b001_111 && ifStrict(ZeroRs, ZeroRt, ZeroRd) -> {
        MipsInstr(vAddr, Sync, ImmOperand(shift))
      }
      funct == 0b001_100 -> MipsInstr(vAddr, Syscall)
      funct == 0b110_100 -> MipsInstr(vAddr, Teq, rs, rt)
      funct == 0b110_000 -> MipsInstr(vAddr, Tge, rs, rt)
      funct == 0b110_001 -> MipsInstr(vAddr, Tgeu, rs, rt)
      funct == 0b110_010 -> MipsInstr(vAddr, Tlt, rs, rt)
      funct == 0b110_011 -> MipsInstr(vAddr, Tltu, rs, rt)
      funct == 0b110_110 -> MipsInstr(vAddr, Tne, rs, rt)
      funct == 0b100_110 && ifStrict(ZeroShift) -> MipsInstr(vAddr, Xor, rd, rs, rt)
      funct == 0b000_001 && instr ushr 16 and 0b1 == 0 && ifStrict(ZeroShift) && ifStrict { instr ushr 17 and 0b1 == 0 } -> {
        MipsInstr(vAddr, FpuMovf, rd, rs, RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111)))
      }
      funct == 0b000_001 && instr ushr 16 and 0b1 == 1 && ifStrict(ZeroShift) && ifStrict { instr ushr 17 and 0b1 == 0 } -> {
        MipsInstr(vAddr, FpuMovt, rd, rs, RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111)))
      }
      else -> handleUnknownInstr(vAddr, instrCount)
    }
  }

  override fun disasmRegimmInstr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
    val rs = RegOperand(GprReg.forId(instr ushr 21 and 0x1F))
    val rt = instr ushr 16 and 0x1F
    val imm = ImmOperand((instr and 0xFFFF).toShort().toInt())
    val branchImm = ImmOperand(vAddr + 0x4 + imm.value * 0x4, hintUnsigned = true)
    return when (rt) {
      0b00001 -> MipsInstr(vAddr, Bgez, rs, branchImm)
      0b10001 -> MipsInstr(vAddr, Bgezal, rs, branchImm)
      0b10011 -> MipsInstr(vAddr, Bgezall, rs, branchImm)
      0b00011 -> MipsInstr(vAddr, Bgezl, rs, branchImm)
      0b00000 -> MipsInstr(vAddr, Bltz, rs, branchImm)
      0b10000 -> MipsInstr(vAddr, Bltzal, rs, branchImm)
      0b10010 -> MipsInstr(vAddr, Bltzall, rs, branchImm)
      0b00010 -> MipsInstr(vAddr, Bltzl, rs, branchImm)
      0b01100 -> MipsInstr(vAddr, Teqi, rs, imm)
      0b01000 -> MipsInstr(vAddr, Tgei, rs, imm)
      0b01001 -> MipsInstr(vAddr, Tgeiu, rs, imm)
      0b01010 -> MipsInstr(vAddr, Tlti, rs, imm)
      0b01011 -> MipsInstr(vAddr, Tltiu, rs, imm)
      0b01110 -> MipsInstr(vAddr, Tnei, rs, imm)
      else -> handleUnknownInstr(vAddr, instrCount)
    }
  }

  override fun disasmCop1Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
    // only for FPU R instruction
    val rt = RegOperand(GprReg.forId(instr ushr 16 and 0x1F))
    // only for FPU instruction
    val fmt = instr ushr 21 and 0x1F
    val ft = RegOperand(FpuReg.forId(instr ushr 16 and 0x1F))
    val fs = RegOperand(FpuReg.forId(instr ushr 11 and 0x1F))
    val fd = RegOperand(FpuReg.forId(instr ushr 6 and 0x1F))
    val funct = instr and 0x3F
    val ifStrict = StrictChecker()
    ifStrict.register(ZeroFt) { ft.reg.id == 0 }
    ifStrict.register(ZeroFs) { fs.reg.id == 0 }
    ifStrict.register(ZeroFd) { fd.reg.id == 0 }
    ifStrict.register(ZeroFunct) { funct == 0 }
    return when {
      fmt == 0b00010 && ifStrict(ZeroFd, ZeroFunct) -> MipsInstr(vAddr, FpuCfc1, rt, fs)
      fmt == 0b00110 && ifStrict(ZeroFd, ZeroFunct) -> MipsInstr(vAddr, FpuCtc1, rt, fs)
      fmt == 0b00000 && ifStrict(ZeroFd, ZeroFunct) -> MipsInstr(vAddr, FpuMfc1, rt, fs)
      fmt == 0b00100 && ifStrict(ZeroFd, ZeroFunct) -> MipsInstr(vAddr, FpuMtc1, rt, fs)
      fmt == 0b01000 -> {
        val branchImm = (instr and 0xFFFF).toShort().toInt()
        val branchTarget = ImmOperand(vAddr + 0x4 + branchImm * 0x4)
        val ndtf = instr ushr 16 and 0b11
        val cc = RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111))
        if (cc.reg !is FpuReg.Cc0 && srcProcessor !is MipsIVProcessor) {
          throw DisassemblerException("Only MIPS IV architecture level can use non 0 condition code (cc)")
        }
        when (srcProcessor) {
          is MipsIVProcessor -> {
            when (ndtf) {
              0b00 -> MipsInstr(vAddr, FpuBc1fCcAny, cc, branchTarget)
              0b10 -> MipsInstr(vAddr, FpuBc1flCcAny, cc, branchTarget)
              0b01 -> MipsInstr(vAddr, FpuBc1tCcAny, cc, branchTarget)
              0b11 -> MipsInstr(vAddr, FpuBc1tlCcAny, cc, branchTarget)
              else -> handleUnknownInstr(vAddr, instrCount)
            }
          }
          else -> {
            when (ndtf) {
              0b00 -> MipsInstr(vAddr, FpuBc1f, branchTarget)
              0b10 -> MipsInstr(vAddr, FpuBc1fl, branchTarget)
              0b01 -> MipsInstr(vAddr, FpuBc1t, branchTarget)
              0b11 -> MipsInstr(vAddr, FpuBc1tl, branchTarget)
              else -> handleUnknownInstr(vAddr, instrCount)
            }
          }
        }
      }
      fmt == MipsDefines.FMT_S -> {
        when {
          funct == 0b000_101 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuAbsS, fd, fs)
          funct == 0b000_000 -> MipsInstr(vAddr, FpuAddS, fd, fs, ft)
          funct and 0b110_000 == 0b110_000 && ifStrict { funct ushr 6 and 0b11 == 0 } -> {
            val cc = RegOperand(FpuReg.ccForId(instr ushr 8 and 0b111))
            val cond = funct and 0b1111
            if (cc.reg !is FpuReg.Cc0 && srcProcessor !is MipsIVProcessor) {
              throw DisassemblerException("Only MIPS IV architecture level can use non 0 condition code (cc)")
            }
            when (srcProcessor) {
              is MipsIVProcessor -> {
                when (cond) {
                  0b0000 -> MipsInstr(vAddr, FpuCFSCcAny, cc, fs, ft)
                  0b0001 -> MipsInstr(vAddr, FpuCUnSCcAny, cc, fs, ft)
                  0b0010 -> MipsInstr(vAddr, FpuCEqSCcAny, cc, fs, ft)
                  0b0011 -> MipsInstr(vAddr, FpuCUeqSCcAny, cc, fs, ft)
                  0b0100 -> MipsInstr(vAddr, FpuCOltSCcAny, cc, fs, ft)
                  0b0101 -> MipsInstr(vAddr, FpuCUltSCcAny, cc, fs, ft)
                  0b0110 -> MipsInstr(vAddr, FpuCOleSCcAny, cc, fs, ft)
                  0b0111 -> MipsInstr(vAddr, FpuCUleSCcAny, cc, fs, ft)
                  0b1000 -> MipsInstr(vAddr, FpuCSfSCcAny, cc, fs, ft)
                  0b1001 -> MipsInstr(vAddr, FpuCNgleSCcAny, cc, fs, ft)
                  0b1010 -> MipsInstr(vAddr, FpuCSeqSCcAny, cc, fs, ft)
                  0b1011 -> MipsInstr(vAddr, FpuCNglSCcAny, cc, fs, ft)
                  0b1100 -> MipsInstr(vAddr, FpuCLtSCcAny, cc, fs, ft)
                  0b1101 -> MipsInstr(vAddr, FpuCNgeSCcAny, cc, fs, ft)
                  0b1110 -> MipsInstr(vAddr, FpuCLeSCcAny, cc, fs, ft)
                  0b1111 -> MipsInstr(vAddr, FpuCNgtSCcAny, cc, fs, ft)
                  else -> handleUnknownInstr(vAddr, instrCount)
                }
              }
              else -> {
                when (cond) {
                  0b0000 -> MipsInstr(vAddr, FpuCFS, fs, ft)
                  0b0001 -> MipsInstr(vAddr, FpuCUnS, fs, ft)
                  0b0010 -> MipsInstr(vAddr, FpuCEqS, fs, ft)
                  0b0011 -> MipsInstr(vAddr, FpuCUeqS, fs, ft)
                  0b0100 -> MipsInstr(vAddr, FpuCOltS, fs, ft)
                  0b0101 -> MipsInstr(vAddr, FpuCUltS, fs, ft)
                  0b0110 -> MipsInstr(vAddr, FpuCOleS, fs, ft)
                  0b0111 -> MipsInstr(vAddr, FpuCUleS, fs, ft)
                  0b1000 -> MipsInstr(vAddr, FpuCSfS, fs, ft)
                  0b1001 -> MipsInstr(vAddr, FpuCNgleS, fs, ft)
                  0b1010 -> MipsInstr(vAddr, FpuCSeqS, fs, ft)
                  0b1011 -> MipsInstr(vAddr, FpuCNglS, fs, ft)
                  0b1100 -> MipsInstr(vAddr, FpuCLtS, fs, ft)
                  0b1101 -> MipsInstr(vAddr, FpuCNgeS, fs, ft)
                  0b1110 -> MipsInstr(vAddr, FpuCLeS, fs, ft)
                  0b1111 -> MipsInstr(vAddr, FpuCNgtS, fs, ft)
                  else -> handleUnknownInstr(vAddr, instrCount)
                }
              }
            }
          }
          funct == 0b001_010 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCeilLS, fd, fs)
          funct == 0b001_110 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCeilWS, fd, fs)
          funct == 0b100_001 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtDS, fd, fs)
          funct == 0b100_101 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtLS, fd, fs)
          funct == 0b100_100 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtWS, fd, fs)
          funct == 0b000_011 -> MipsInstr(vAddr, FpuDivS, fd, fs, ft)
          funct == 0b001_011 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuFloorLS, fd, fs)
          funct == 0b001_111 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuFloorWS, fd, fs)
          funct == 0b000_110 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuMovS, fd, fs)
          funct == 0b010_001 && ft.reg.id and 0b1 == 0 && ifStrict { ft.reg.id ushr 1 and 0b1 == 0 } -> {
            MipsInstr(vAddr, FpuMovfS, fd, fs, RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111)))
          }
          funct == 0b010_011 -> MipsInstr(vAddr, FpuMovnS, fd, fs, rt)
          funct == 0b010_001 && ft.reg.id and 0b1 == 1 && ifStrict { ft.reg.id ushr 1 and 0b1 == 0 } -> {
            MipsInstr(vAddr, FpuMovtS, fd, fs, RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111)))
          }
          funct == 0b010_010 -> MipsInstr(vAddr, FpuMovzS, fd, fs, rt)
          funct == 0b000_010 -> MipsInstr(vAddr, FpuMulS, fd, fs, ft)
          funct == 0b000_111 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuNegS, fd, fs)
          funct == 0b010_101 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuRecipS, fd, fs)
          funct == 0b001_000 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuRoundLS, fd, fs)
          funct == 0b001_100 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuRoundWS, fd, fs)
          funct == 0b010_110 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuRsqrtS, fd, fs)
          funct == 0b000_100 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuSqrtS, fd, fs)
          funct == 0b000_001 -> MipsInstr(vAddr, FpuSubS, fd, fs, ft)
          funct == 0b001_001 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuTruncLS, fd, fs)
          funct == 0b001_101 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuTruncWS, fd, fs)
          else -> handleUnknownInstr(vAddr, instrCount)
        }
      }
      fmt == MipsDefines.FMT_D -> {
        when {
          funct == 0b000_101 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuAbsD, fd, fs)
          funct == 0b000_000 -> MipsInstr(vAddr, FpuAddD, fd, fs, ft)
          funct and 0b110_000 == 0b110_000 -> {
            val cc = RegOperand(FpuReg.ccForId(instr ushr 8 and 0b111))
            val cond = funct and 0b1111
            if (cc.reg !is FpuReg.Cc0 && srcProcessor !is MipsIVProcessor) {
              throw DisassemblerException("Only MIPS IV architecture level can use non 0 condition code (cc)")
            }
            when (srcProcessor) {
              is MipsIVProcessor -> {
                when (cond) {
                  0b0000 -> MipsInstr(vAddr, FpuCFDCcAny, cc, fs, ft)
                  0b0001 -> MipsInstr(vAddr, FpuCUnDCcAny, cc, fs, ft)
                  0b0010 -> MipsInstr(vAddr, FpuCEqDCcAny, cc, fs, ft)
                  0b0011 -> MipsInstr(vAddr, FpuCUeqDCcAny, cc, fs, ft)
                  0b0100 -> MipsInstr(vAddr, FpuCOltDCcAny, cc, fs, ft)
                  0b0101 -> MipsInstr(vAddr, FpuCUltDCcAny, cc, fs, ft)
                  0b0110 -> MipsInstr(vAddr, FpuCOleDCcAny, cc, fs, ft)
                  0b0111 -> MipsInstr(vAddr, FpuCUleDCcAny, cc, fs, ft)
                  0b1000 -> MipsInstr(vAddr, FpuCSfDCcAny, cc, fs, ft)
                  0b1001 -> MipsInstr(vAddr, FpuCNgleDCcAny, cc, fs, ft)
                  0b1010 -> MipsInstr(vAddr, FpuCSeqDCcAny, cc, fs, ft)
                  0b1011 -> MipsInstr(vAddr, FpuCNglDCcAny, cc, fs, ft)
                  0b1100 -> MipsInstr(vAddr, FpuCLtDCcAny, cc, fs, ft)
                  0b1101 -> MipsInstr(vAddr, FpuCNgeDCcAny, cc, fs, ft)
                  0b1110 -> MipsInstr(vAddr, FpuCLeDCcAny, cc, fs, ft)
                  0b1111 -> MipsInstr(vAddr, FpuCNgtDCcAny, cc, fs, ft)
                  else -> handleUnknownInstr(vAddr, instrCount)
                }
              }
              else -> {
                when (cond) {
                  0b0000 -> MipsInstr(vAddr, FpuCFD, fs, ft)
                  0b0001 -> MipsInstr(vAddr, FpuCUnD, fs, ft)
                  0b0010 -> MipsInstr(vAddr, FpuCEqD, fs, ft)
                  0b0011 -> MipsInstr(vAddr, FpuCUeqD, fs, ft)
                  0b0100 -> MipsInstr(vAddr, FpuCOltD, fs, ft)
                  0b0101 -> MipsInstr(vAddr, FpuCUltD, fs, ft)
                  0b0110 -> MipsInstr(vAddr, FpuCOleD, fs, ft)
                  0b0111 -> MipsInstr(vAddr, FpuCUleD, fs, ft)
                  0b1000 -> MipsInstr(vAddr, FpuCSfD, fs, ft)
                  0b1001 -> MipsInstr(vAddr, FpuCNgleD, fs, ft)
                  0b1010 -> MipsInstr(vAddr, FpuCSeqD, fs, ft)
                  0b1011 -> MipsInstr(vAddr, FpuCNglD, fs, ft)
                  0b1100 -> MipsInstr(vAddr, FpuCLtD, fs, ft)
                  0b1101 -> MipsInstr(vAddr, FpuCNgeD, fs, ft)
                  0b1110 -> MipsInstr(vAddr, FpuCLeD, fs, ft)
                  0b1111 -> MipsInstr(vAddr, FpuCNgtD, fs, ft)
                  else -> handleUnknownInstr(vAddr, instrCount)
                }
              }
            }
          }
          funct == 0b001_010 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCeilLD, fd, fs)
          funct == 0b001_110 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCeilWD, fd, fs)
          funct == 0b100_101 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtLD, fd, fs)
          funct == 0b100_000 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtSD, fd, fs)
          funct == 0b100_100 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtWD, fd, fs)
          funct == 0b000_011 -> MipsInstr(vAddr, FpuDivD, fd, fs, ft)
          funct == 0b001_011 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuFloorLD, fd, fs)
          funct == 0b001_111 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuFloorWD, fd, fs)
          funct == 0b000_110 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuMovD, fd, fs)
          funct == 0b010_001 && ft.reg.id and 0b1 == 0 && ifStrict { ft.reg.id ushr 1 and 0b1 == 0 } -> {
            MipsInstr(vAddr, FpuMovfD, fd, fs, RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111)))
          }
          funct == 0b010_011 -> MipsInstr(vAddr, FpuMovnD, fd, fs, rt)
          funct == 0b010_001 && ft.reg.id and 0b1 == 1 && ifStrict { ft.reg.id ushr 1 and 0b1 == 0 } -> {
            MipsInstr(vAddr, FpuMovtD, fd, fs, RegOperand(FpuReg.ccForId(instr ushr 18 and 0b111)))
          }
          funct == 0b010_010 -> MipsInstr(vAddr, FpuMovzD, fd, fs, rt)
          funct == 0b000_010 -> MipsInstr(vAddr, FpuMulD, fd, fs, ft)
          funct == 0b000_111 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuNegD, fd, fs)
          funct == 0b010_101 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuRecipD, fd, fs)
          funct == 0b001_000 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuRoundLD, fd, fs)
          funct == 0b001_100 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuRoundWD, fd, fs)
          funct == 0b010_110 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuRsqrtD, fd, fs)
          funct == 0b000_100 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuSqrtD, fd, fs)
          funct == 0b000_001 -> MipsInstr(vAddr, FpuSubD, fd, fs, ft)
          funct == 0b001_001 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuTruncLD, fd, fs)
          funct == 0b001_101 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuTruncWD, fd, fs)
          else -> handleUnknownInstr(vAddr, instrCount)
        }
      }
      fmt == MipsDefines.FMT_W -> {
        when {
          funct == 0b100_001 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtDW, fd, fs)
          funct == 0b100_000 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtSW, fd, fs)
          else -> handleUnknownInstr(vAddr, instrCount)
        }
      }
      fmt == MipsDefines.FMT_L -> {
        when {
          funct == 0b100_001 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtDL, fd, fs)
          funct == 0b100_000 && ifStrict(ZeroFt) -> MipsInstr(vAddr, FpuCvtSL, fd, fs)
          else -> handleUnknownInstr(vAddr, instrCount)
        }
      }
      else -> handleUnknownInstr(vAddr, instrCount)
    }
  }

  override fun disasmCop2Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
    return MipsInstr(vAddr, Cop2, ImmOperand(instr and 0x3FFFFFF))
  }

  override fun disasmCop3Instr(vAddr: Int, instr: Int, instrCount: Int): MipsInstr {
    return when (srcProcessor) {
      is MipsIProcessor, MipsIIProcessor -> MipsInstr(vAddr, Cop3, ImmOperand(instr and 0x3FFFFFF))
      is MipsIIIProcessor -> throw DisassemblerException("COP3 is not defined on MIPS III architecture level")
      is MipsIVProcessor -> {
        // for arithmetic ops
        val fr = RegOperand(FpuReg.forId(instr ushr 21 and 0x1F))
        val ft = RegOperand(FpuReg.forId(instr ushr 16 and 0x1F))
        val fs = RegOperand(FpuReg.forId(instr ushr 11 and 0x1F))
        // for memory related ops
        val base = RegOperand(GprReg.forId(instr ushr 21 and 0x1F))
        val index = RegOperand(GprReg.forId(instr ushr 16 and 0x1F))
        val expectedZero = instr ushr 11 and 0x1F
        // for all instructions
        val fd = RegOperand(FpuReg.forId(instr ushr 6 and 0x1F))
        val funct = instr and 0x3F
        val op4 = funct ushr 3 and 0b111
        val fmt3 = funct and 0b111
        val ifStrict = StrictChecker()
        when {
          funct == 0b000_001 && ifStrict { expectedZero == 0 } -> MipsInstr(vAddr, FpuLdxc1, fd, base, index)
          funct == 0b000_000 && ifStrict { expectedZero == 0 } -> MipsInstr(vAddr, FpuLwxc1, fd, base, index)
          fmt3 == MipsDefines.FMT3_S && op4 == 0b100 -> MipsInstr(vAddr, FpuMaddS, fd, fr, fs, ft)
          fmt3 == MipsDefines.FMT3_D && op4 == 0b100 -> MipsInstr(vAddr, FpuMaddD, fd, fr, fs, ft)
          fmt3 == MipsDefines.FMT3_S && op4 == 0b101 -> MipsInstr(vAddr, FpuMsubS, fd, fr, fs, ft)
          fmt3 == MipsDefines.FMT3_D && op4 == 0b101 -> MipsInstr(vAddr, FpuMsubD, fd, fr, fs, ft)
          fmt3 == MipsDefines.FMT3_S && op4 == 0b110 -> MipsInstr(vAddr, FpuNmaddS, fd, fr, fs, ft)
          fmt3 == MipsDefines.FMT3_D && op4 == 0b110 -> MipsInstr(vAddr, FpuNmaddD, fd, fr, fs, ft)
          fmt3 == MipsDefines.FMT3_S && op4 == 0b111 -> MipsInstr(vAddr, FpuNmsubS, fd, fr, fs, ft)
          fmt3 == MipsDefines.FMT3_D && op4 == 0b111 -> MipsInstr(vAddr, FpuNmsubD, fd, fr, fs, ft)
          funct == 0b001_111 && ifStrict { expectedZero == 0 } -> MipsInstr(vAddr, FpuPrefx, fd, base, index)
          funct == 0b001_001 && ifStrict { expectedZero == 0 } -> MipsInstr(vAddr, FpuSdxc1, fd, base, index)
          funct == 0b001_000 && ifStrict { expectedZero == 0 } -> MipsInstr(vAddr, FpuSwxc1, fd, base, index)
          else -> handleUnknownInstr(vAddr, instrCount)
        }
      }
    }
  }

  override fun disasmOpcodeInstr(vAddr: Int, instr: Int, instrCount: Int, opcode: Int): MipsInstr {
    // only applies to I instruction
    val rs = RegOperand(GprReg.forId(instr ushr 21 and 0x1F))
    val rt = RegOperand(GprReg.forId(instr ushr 16 and 0x1F))
    val imm = ImmOperand((instr and 0xFFFF).toShort().toInt())
    val zeroExtendImm = ImmOperand(instr and 0xFFFF)
    val branchImm = ImmOperand(vAddr + 0x4 + imm.value * 0x4)
    val ifStrict = StrictChecker()
    ifStrict.register(ZeroRs) { rs.reg.id == 0 }
    ifStrict.register(ZeroRt) { rt.reg.id == 0 }
    ifStrict.register(ZeroImm) { imm.value == 0 }
    return when {
      opcode == 0b001_000 -> MipsInstr(vAddr, Addi, rt, rs, imm)
      opcode == 0b001_001 -> MipsInstr(vAddr, Addiu, rt, rs, imm)
      opcode == 0b001_100 -> MipsInstr(vAddr, Andi, rt, rs, zeroExtendImm)
      opcode == 0b000_100 -> MipsInstr(vAddr, Beq, rs, rt, branchImm)
      opcode == 0b010_100 -> MipsInstr(vAddr, Beql, rs, rt, branchImm)
      opcode == 0b000_111 && ifStrict(ZeroRt) -> MipsInstr(vAddr, Bgtz, rs, branchImm)
      opcode == 0b010_111 && ifStrict(ZeroRt) -> MipsInstr(vAddr, Bgtzl, rs, branchImm)
      opcode == 0b000_110 && ifStrict(ZeroRt) -> MipsInstr(vAddr, Blez, rs, branchImm)
      opcode == 0b010_110 && ifStrict(ZeroRt) -> MipsInstr(vAddr, Blezl, rs, branchImm)
      opcode == 0b000_101 -> MipsInstr(vAddr, Bne, rs, rt, branchImm)
      opcode == 0b010_101 -> MipsInstr(vAddr, Bnel, rs, rt, branchImm)
      opcode == 0b000_010 ->
        MipsInstr(vAddr, J, ImmOperand((vAddr and 0xf0000000.toInt()) + (instr and 0x3FFFFFF shl 2), hintUnsigned = true))
      opcode == 0b000_011 ->
        MipsInstr(vAddr, Jal, ImmOperand((vAddr and 0xf0000000.toInt()) + (instr and 0x3FFFFFF shl 2), hintUnsigned = true))
      opcode == 0b100_000 -> MipsInstr(vAddr, Lb, rt, rs, imm)
      opcode == 0b100_100 -> MipsInstr(vAddr, Lbu, rt, rs, imm)
      opcode == 0b110_101 -> MipsInstr(vAddr, Ldc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
      opcode == 0b110_110 -> MipsInstr(vAddr, Ldc2, RegOperand(Cop2Reg.forId(rt.reg.id)), rs, imm)
      opcode == 0b110_111 && srcProcessor in arrayOf(MipsIProcessor, MipsIIProcessor) -> {
        MipsInstr(vAddr, Ldc3, RegOperand(Cop3Reg.forId(rt.reg.id)), rs, imm)
      }
      opcode == 0b100_001 -> MipsInstr(vAddr, Lh, rt, rs, imm)
      opcode == 0b100_101 -> MipsInstr(vAddr, Lhu, rt, rs, imm)
      opcode == 0b110_000 -> MipsInstr(vAddr, Ll, rt, rs, imm)
      opcode == 0b001_111 && ifStrict(ZeroRs) -> MipsInstr(vAddr, Lui, rt, zeroExtendImm)
      opcode == 0b100_011 -> MipsInstr(vAddr, Lw, rt, rs, imm)
      opcode == 0b110_001 -> MipsInstr(vAddr, Lwc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
      opcode == 0b110_010 -> MipsInstr(vAddr, Lwc2, RegOperand(Cop2Reg.forId(rt.reg.id)), rs, imm)
      opcode == 0b110_011 && srcProcessor in arrayOf(MipsIProcessor, MipsIIProcessor) -> {
        MipsInstr(vAddr, Lwc3, RegOperand(Cop3Reg.forId(rt.reg.id)), rs, imm)
      }
      opcode == 0b100_010 -> MipsInstr(vAddr, Lwl, rt, rs, imm)
      opcode == 0b100_110 -> MipsInstr(vAddr, Lwr, rt, rs, imm)
      opcode == 0b001_101 -> MipsInstr(vAddr, Ori, rt, rs, zeroExtendImm)
      opcode == 0b110_011 -> MipsInstr(vAddr, Pref, ImmOperand(rt.reg.id), rs, imm)
      opcode == 0b101_000 -> MipsInstr(vAddr, Sb, rt, rs, imm)
      opcode == 0b111_000 -> MipsInstr(vAddr, Sc, rt, rs, imm)
      opcode == 0b111_101 -> MipsInstr(vAddr, Sdc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
      opcode == 0b111_110 -> MipsInstr(vAddr, Sdc2, RegOperand(Cop2Reg.forId(rt.reg.id)), rs, imm)
      opcode == 0b111_111 && srcProcessor in arrayOf(MipsIProcessor, MipsIIProcessor) -> {
        MipsInstr(vAddr, Sdc3, RegOperand(Cop3Reg.forId(rt.reg.id)), rs, imm)
      }
      opcode == 0b101_001 -> MipsInstr(vAddr, Sh, rt, rs, imm)
      opcode == 0b001_010 -> MipsInstr(vAddr, Slti, rt, rs, imm)
      opcode == 0b001_011 -> MipsInstr(vAddr, Sltiu, rt, rs, imm.toHintedUnsigned())
      opcode == 0b101_011 -> MipsInstr(vAddr, Sw, rt, rs, imm)
      opcode == 0b111_001 -> MipsInstr(vAddr, Swc1, RegOperand(FpuReg.forId(rt.reg.id)), rs, imm)
      opcode == 0b111_010 -> MipsInstr(vAddr, Swc2, RegOperand(Cop2Reg.forId(rt.reg.id)), rs, imm)
      opcode == 0b111_011 && srcProcessor in arrayOf(MipsIProcessor, MipsIIProcessor) -> {
        MipsInstr(vAddr, Swc3, RegOperand(Cop3Reg.forId(rt.reg.id)), rs, imm)
      }
      opcode == 0b101_010 -> MipsInstr(vAddr, Swl, rt, rs, imm)
      opcode == 0b101_110 -> MipsInstr(vAddr, Swr, rt, rs, imm)
      opcode == 0b001_110 -> MipsInstr(vAddr, Xori, rt, rs, zeroExtendImm)
      else -> handleUnknownInstr(vAddr, instrCount)
    }
  }
}
