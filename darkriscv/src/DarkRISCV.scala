package rvcosim.darkriscv

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.util.{HasExtModuleInline, HasExtModuleResource}
import firrtl.stage.FirrtlCircuitAnnotation
import rvcosim._
import chisel3.probe._

class DarkRISCVParameter extends CoreParameter {
  override val ifAddressWidth: Int = 32
  override val ifDataWidth: Int = 32
  override val lsuAddressWidth: Int = 32
  override val lsuDataWidth: Int = 32
}

class DarkRISCV extends ExtModule
  with HasExtModuleResource
  with HasExtModuleDefine
  with HasExtModuleInline {
  override def desiredName: String = "darkriscv"
  /* input             CLK,   // clock */
  val clock = IO(Input(Clock())).suggestName("CLK")
  /* input             RES,   // reset */
  val reset = IO(Input(Reset())).suggestName("RES")
  /* input             HLT,   // halt */
  val halt = IO(Input(Bool())).suggestName("HLT")
  /* input      [31:0] IDATA, // instruction data bus */
  val idata = IO(Input(UInt(32.W))).suggestName("IDATA")
  /* output     [31:0] IADDR, // instruction addr bus */
  val iaddr = IO(Output(UInt(32.W))).suggestName("IADDR")
  /* input      [31:0] DATAI, // data bus (input) */
  val datai = IO(Input(UInt(32.W))).suggestName("DATAI")
  /* output     [31:0] DATAO, // data bus (output) */
  val datao = IO(Output(UInt(32.W))).suggestName("DATAO")
  /* output     [31:0] DADDR, // addr bus */
  val daddr = IO(Output(UInt(32.W))).suggestName("DADDR")
  /* output     [ 3:0] BE,   // byte enable */
  val be = IO(Output(UInt(4.W))).suggestName("BE")
  /* output            WR,    // write enable */
  val wr = IO(Output(Bool())).suggestName("WR")
  /* output            RD,    // read enable */
  val rd = IO(Output(Bool())).suggestName("RD")
  /* output            IDLE,   // idle output */
  val idle = IO(Output(Bool())).suggestName("IDLE")
  /* output [3:0]  DEBUG       // old-school osciloscope based debug! :) */
  val debug = IO(Output(UInt(4.W))).suggestName("DEBUG")

  // XMR
  val dptr = define(Probe(UInt(chisel3.util.log2Ceil(32).W)), Seq("darkriscv", "darkriscv", "DPTR"))
  val regs = defineVec(Probe(Vec(32, UInt(32.W))), Seq("darkriscv", "darkriscv", "REGS"))
  val lcc = define(Probe(Bool()), Seq("darkriscv", "darkriscv", "LCC"))
  val auipc = define(Probe(Bool()), Seq("darkriscv", "darkriscv", "AUIPC"))
  val jal = define(Probe(Bool()), Seq("darkriscv", "darkriscv", "JAL"))
  val jalr = define(Probe(Bool()), Seq("darkriscv", "darkriscv", "JALR"))
  val lui = define(Probe(Bool()), Seq("darkriscv", "darkriscv", "LUI"))
  val mcc = define(Probe(Bool()), Seq("darkriscv", "darkriscv", "MCC"))
  val rcc = define(Probe(Bool()), Seq("darkriscv", "darkriscv", "RCC"))

  setInline(
    "config.v",
    """//`define __3STAGE__
      |//`define __RV32E__
      |//`define __THREADS__ 3
      |//`define __MAC16X16__
      |//`define __FLEXBUZZ__
      |//`define __INTERRUPT__
      |`define __RESETPC__ 32'd0
      |//`define __INTERACTIVE__
      |//`define __PERFMETER__
      |//`define __REGDUMP__
      |//`define __HARVARD__
      |`ifdef __HARVARD__
      |    `define MLEN 13 // MEM[12:0] ->  8KBytes LENGTH = 0x2000
      |`else
      |    `define MLEN 12 // MEM[12:0] -> 4KBytes LENGTH = 0x1000
      |    //`define MLEN 15 // MEM[12:0] -> 16KBytes LENGTH = 0x8000 for coremark!
      |`endif
      |//`define __RMW_CYCLE__
      |""".stripMargin)
  addResource("darkriscv.v")
}
