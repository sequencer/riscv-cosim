package cosim

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.util.{HasExtModuleInline, HasExtModuleResource}
import firrtl.stage.FirrtlCircuitAnnotation

class DarkRISCVParameter extends CoreParameter {
  override val ifAddressWidth: Int = 32
  override val ifDataWidth: Int = 32
  override val lsuAddressWidth: Int = 32
  override val lsuDataWidth: Int = 32
}

class DarkRISCVWrapper extends Core {
  def parameter: CoreParameter = new DarkRISCVParameter
  val darkriscv = Module(new DarkRISCV)
  darkriscv.clock := clock
  darkriscv.reset := reset
  darkriscv.halt := false.B
  darkriscv.idata := instructionFetch.response.bits.data
  instructionFetch.request.bits.address := darkriscv.iaddr
  instructionFetch.request.valid := true.B
  darkriscv.datai := loadStore.response.bits.data
  loadStore.request.bits.data := darkriscv.datao
  loadStore.request.bits.address := darkriscv.daddr
  loadStore.request.bits.maskByte := darkriscv.be
  loadStore.request.bits.writeEnable := darkriscv.wr
  loadStore.request.valid := darkriscv.wr || darkriscv.rd
  // dontcare darkriscv.idle
  // dontcare debug
}

class DarkRISCV extends ExtModule
  with HasExtModuleResource
  with HasExtModuleInline {
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
  setInline(
    "config.vh",
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

object RunDarkRISCV extends App {
  import firrtl.AnnotationSeq
  import chisel3.stage.ChiselGeneratorAnnotation
  os.makeDir.all(os.pwd / "elaborate")

  var topName: String = null

  val annos: AnnotationSeq = Seq(
    new chisel3.stage.phases.Elaborate,
    new chisel3.stage.phases.Convert
  ).foldLeft(
      Seq(
        ChiselGeneratorAnnotation(() => new Cosim(new DarkRISCVWrapper))
      ): AnnotationSeq
    ) { case (annos, stage) => stage.transform(annos) }
    .flatMap {
      case FirrtlCircuitAnnotation(circuit) =>
        topName = circuit.main
        os.write.over(os.pwd / "elaborate" / s"$topName.fir", circuit.serialize)
        None
      case _: chisel3.stage.DesignAnnotation[_] => None
      case _: chisel3.stage.ChiselCircuitAnnotation => None
      case a => Some(a)
    }
  os.write.over(os.pwd / "elaborate" / s"$topName.anno.json", firrtl.annotations.JsonProtocol.serialize(annos))
  os.proc(
    "firtool",
    os.pwd / "elaborate" / s"$topName.fir", s"--annotation-file=${os.pwd / "elaborate" / s"$topName.anno.json"}",
    "-dedup",
    "-O=release",
    "--disable-all-randomization",
    "--split-verilog",
    "--preserve-values=none",
    "--preserve-aggregate=all",
    s"-o=${os.pwd / "circt"}"
  ).call()
}