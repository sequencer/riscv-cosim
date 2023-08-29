package rvcosim.picorv32

import chisel3._
import chisel3.experimental.{ExtModule, IntParam}
import chisel3.util.{HasExtModuleInline, HasExtModuleResource}
import firrtl.stage.FirrtlCircuitAnnotation
import rvcosim._
import chisel3.probe._

class Picorv32 extends ExtModule with HasExtModuleResource with HasExtModuleDefine with HasExtModuleInline {
  override val params = Map(
    "ENABLE_COUNTERS" -> IntParam(0),
    "ENABLE_COUNTERS64" -> IntParam(0),
    "ENABLE_REGS_16_31" -> IntParam(0),
    "ENABLE_REGS_DUALPORT" -> IntParam(0),
    "LATCHED_MEM_RDATA" -> IntParam(0),
    "TWO_STAGE_SHIFT" -> IntParam(1),
    "BARREL_SHIFTER" -> IntParam(0),
    "TWO_CYCLE_COMPARE" -> IntParam(0),
    "TWO_CYCLE_ALU" -> IntParam(0),
    "COMPRESSED_ISA" -> IntParam(0),
    "CATCH_MISALIGN" -> IntParam(1),
    "CATCH_ILLINSN" -> IntParam(1),
    "ENABLE_PCPI" -> IntParam(0),
    "ENABLE_MUL" -> IntParam(0),
    "ENABLE_FAST_MUL" -> IntParam(0),
    "ENABLE_DIV" -> IntParam(0),
    "ENABLE_IRQ" -> IntParam(0),
    "ENABLE_IRQ_QREGS" -> IntParam(1),
    "ENABLE_IRQ_TIMER" -> IntParam(1),
    "ENABLE_TRACE" -> IntParam(0),
    "REGS_INIT_ZERO" -> IntParam(0),
    "MASKED_IRQ" -> IntParam(BigInt("00000000", 16)),
    "LATCHED_IRQ" -> IntParam(BigInt("ffffffff", 16)),
    "PROGADDR_RESET" -> IntParam(BigInt("00000000", 16)),
    "PROGADDR_IRQ" -> IntParam(BigInt("00000010", 16)),
    "STACKADDR" -> IntParam(BigInt("ffffffff", 16))
  )
  override def desiredName: String = "picorv32"
  addResource("picorv32.v")
  //  input clk,
  val clock = IO(Input(Clock())).suggestName("clk")
  //  input resetn,
  val resetn = IO(Input(Reset())).suggestName("resetn")
  //  output trap,
  val trap = IO(Output(Bool())).suggestName("trap")

  //  output mem_valid,
  val memoryValid = IO(Output(Bool())).suggestName("mem_valid")
  //  output mem_instr,
  val memInstruction = IO(Output(Bool())).suggestName("mem_instr")
  //  input mem_ready,
  val memReady = IO(Input(Bool())).suggestName("mem_ready")

  //  output [31:0] mem_addr,
  val memAddress = IO(Output(UInt(32.W))).suggestName("mem_addr")
  //  output [31:0] mem_wdata,
  val memWriteData = IO(Output(UInt(32.W))).suggestName("mem_wdata")
  //  output [ 3:0] mem_wstrb,
  val memWriteMask = IO(Output(UInt(4.W))).suggestName("mem_wstrb")
  //  input [31:0] mem_rdata,
  val memReadData = IO(Input(UInt(32.W))).suggestName("mem_rdata")

  // Look-Ahead Interface
  //  output mem_la_read,
  val memoryLookAheadRead = IO(Output(Bool())).suggestName("mem_la_read")
  //  output mem_la_write,
  val memoryLookAheadWrite = IO(Output(Bool())).suggestName("mem_la_write")
  //  output [31:0] mem_la_addr,
  val memoryLookAheadAddress = IO(Output(UInt(32.W))).suggestName("mem_la_addr")
  //  output [31:0] mem_la_wdata,
  val memoryLookAheadWriteData = IO(Output(UInt(32.W))).suggestName("mem_la_wdata")
  //  output [ 3:0] mem_la_wstrb,
  val memoryLookAheadWriteMask = IO(Output(UInt(4.W))).suggestName("mem_la_wstrb")

  // Pico Co-Processor Interface (PCPI)
  //  output pcpi_valid,
  val picoCoProcessorInterfaceValid = IO(Output(Bool())).suggestName("pcpi_valid")
  //  output [31:0] pcpi_insn,
  val picoCoProcessorInterfaceInstruction = IO(Output(UInt(32.W))).suggestName("pcpi_insn")
  //  output [31:0] pcpi_rs1,
  val picoCoProcessorInterfaceRs1 = IO(Output(UInt(32.W))).suggestName("pcpi_rs1")
  //  output [31:0] pcpi_rs2,
  val picoCoProcessorInterfaceRs2 = IO(Output(UInt(32.W))).suggestName("pcpi_rs2")
  //  input pcpi_wr,
  val picoCoProcessorInterfaceWrite = IO(Input(Bool())).suggestName("pcpi_wr")
  //  input [31:0] pcpi_rd,
  val picoCoProcessorInterfaceRd = IO(Input(UInt(32.W))).suggestName("pcpi_rd")
  //  input pcpi_wait,
  val picoCoProcessorInterfaceWait = IO(Input(Bool())).suggestName("pcpi_wait")
  //  input pcpi_ready,
  val picoCoProcessorInterfaceReady = IO(Input(Bool())).suggestName("pcpi_ready")

  // IRQ Interface
  //  input [31:0] irq,
  val irq = IO(Input(UInt(32.W))).suggestName("irq")
  //  output [31:0] eoi,
  val eoi = IO(Output(UInt(32.W))).suggestName("eoi")

  // Trace Interface
  //  output trace_valid,
  val traceValid = IO(Output(Bool())).suggestName("trace_valid")
  //  output [35:0] trace_data
  val traceData = IO(Output(UInt(36.W))).suggestName("trace_data")
}
