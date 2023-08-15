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
  val dpiInstructionFetch = Module(new dpiInstructionFetch(dpiInstructionFetchParameter(dutInstance.parameter.ifAddressWidth, dutInstance.parameter.ifDataWidth)))

  val clock = read(clockGen.clock)
  val reset = read(clockGen.reset)
  dutInstance.clock := read(clockGen.clock)
  dutInstance.reset := read(clockGen.reset)

  forceInitial(dpiInstructionFetch.ref("valid"), read(ProbeValue(dutInstance.instructionFetch.request.valid)))
  forceInitial(dpiInstructionFetch.ref("clock"), clock)
  forceInitial(dpiInstructionFetch.ref("address"), read(ProbeValue(dutInstance.instructionFetch.request.bits.address)))
  dutInstance.instructionFetch.response.bits.data := read(dpiInstructionFetch.ref("data"))
  dutInstance.instructionFetch.response.valid := read(dpiInstructionFetch.ref("ready"))

  dutInstance.loadStore.response.valid := true.B
  dutInstance.loadStore.response.bits.data := DontCare
}
