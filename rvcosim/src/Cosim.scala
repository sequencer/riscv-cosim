package rvcosim

import chisel3._
import chisel3.probe._

case class CosimParameter(clockRate: Int)
class Cosim(dut: => Core) extends RawModule {
  // instantiate the RISC-V Core
  val dutInstance = Module(dut)
  def parameter = CosimParameter(clockRate = 2)

  val clockGen = Module(new ClockGen(ClockGenParameter(parameter.clockRate)))
  val dpiInitCosim = Module(new dpiInitCosim)
  val dpiTimeoutCheck = Module(new dpiTimeoutCheck(dpiTimeoutCheckParameter(parameter.clockRate)))

  val clock = read(clockGen.clock)
  val reset = read(clockGen.reset)
  dutInstance.clock := read(clockGen.clock)
  dutInstance.reset := read(clockGen.reset)

  dutInstance.instructionFetch.response.valid := true.B
  dutInstance.instructionFetch.response.bits.data := DontCare
  dutInstance.loadStore.response.valid := true.B
  dutInstance.loadStore.response.bits.data := DontCare
}
