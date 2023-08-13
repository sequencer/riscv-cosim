package cosim

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.util.HasExtModuleInline

case class CosimParameter(clockRate: Int)
class Cosim(dut: => Core) extends RawModule with ProbeModule {
  // instantiate the RISC-V Core
  val dutInstance = Module(dut)
  def parameter = CosimParameter(
    clockRate = 2
  )
  val verbatim = Module(new ExtModule with HasExtModuleInline {
    override val desiredName = "Verbatim"
    val clock = IO(Output(Clock()))
    val reset = IO(Output(Bool()))
    setInline("verbatim.sv",
      s"""module Verbatim(
         |  output clock,
         |  output reset,
         |);
         |  reg _clock = 1'b0;
         |  always #(${parameter.clockRate}) _clock = ~_clock;
         |  reg _reset = 1'b1;
         |  initial #(${2 * parameter.clockRate + 1}) _reset = 0;
         |  assign clock = _clock;
         |  assign reset = _reset;
         |endmodule
         |""".stripMargin)
  })
  val clock = read(verbatim.clock)
  val reset = read(verbatim.reset)
  dutInstance.clock := read(verbatim.clock)
  dutInstance.reset := read(verbatim.reset)

  dutInstance.instructionFetch.response.valid := true.B
  dutInstance.instructionFetch.response.bits.data := DontCare
  dutInstance.loadStore.response.valid := true.B
  dutInstance.loadStore.response.bits.data := DontCare

  // hack
  done()
}