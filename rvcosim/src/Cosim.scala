package rvcosim

import chisel3._
import chisel3.probe._
import rvcosim.dpi._

case class CosimParameter(clockRate: Int)
class Cosim(dut: => Core) extends RawModule {
  // instantiate the RISC-V Core
  val dutInstance = Module(dut)

  // The user defined simulation parameter
  def parameter = CosimParameter(clockRate = 2)

  val clockGen = Module(new ClockGen(ClockGenParameter(parameter.clockRate)))
  val dpiInitCosim = Module(new InitCosim)
  val dpiTimeoutCheck = Module(new TimeoutCheck(TimeoutCheckParameter(parameter.clockRate)))
  val dpiInstructionFetch = Module(new InstructionFetch(InstructionFetchParameter(dutInstance.parameter.ifAddressWidth, dutInstance.parameter.ifDataWidth)))
  val dpiLoadStore = Module(new LoadStore(LoadStoreParameter(dutInstance.parameter.ifAddressWidth, dutInstance.parameter.ifDataWidth)))
  val dpiRegFileWrite = Module(new RegFileWrite)
  val dpiDumpWave = Module(new DumpWave)

  val clock = read(clockGen.clock)
  val reset = read(clockGen.reset)
  dutInstance.clock := read(clockGen.clock).asClock
  dutInstance.reset := read(clockGen.reset)

  forceInitial(dpiInstructionFetch.clock.ref, clock)
  forceInitial(dpiInstructionFetch.requestValid.ref, read(ProbeValue(dutInstance.instructionFetch.request.valid)))
  forceInitial(dpiInstructionFetch.address.ref, read(ProbeValue(dutInstance.instructionFetch.request.bits.address)))
  dutInstance.instructionFetch.response.bits.data := read(dpiInstructionFetch.data.ref)
  dutInstance.instructionFetch.response.valid := read(dpiInstructionFetch.responseValid.ref)

  forceInitial(dpiLoadStore.clock.ref, clock)
  forceInitial(dpiLoadStore.requestValid.ref, read(ProbeValue(dutInstance.loadStore.request.valid)))
  forceInitial(dpiLoadStore.address.ref, read(ProbeValue(dutInstance.loadStore.request.bits.address)))
  forceInitial(dpiLoadStore.writeEnable.ref, read(ProbeValue(dutInstance.loadStore.request.bits.writeEnable)))
  forceInitial(dpiLoadStore.maskByte.ref, read(ProbeValue(dutInstance.loadStore.request.bits.maskByte)))
  forceInitial(dpiLoadStore.storeData.ref, read(ProbeValue(dutInstance.loadStore.request.bits.data)))
  dutInstance.loadStore.response.valid := read(dpiLoadStore.responseValid.ref)
  dutInstance.loadStore.response.bits.data := read(dpiLoadStore.loadData.ref)

  forceInitial(dpiRegFileWrite.clock.ref, clock)
  forceInitial(dpiRegFileWrite.writeValid.ref, read(dutInstance.rfWriteValid))
  forceInitial(dpiRegFileWrite.isFp.ref, read(dutInstance.rfWriteFp))
  forceInitial(dpiRegFileWrite.isVector.ref, read(dutInstance.rfWriteVector))
  forceInitial(dpiRegFileWrite.data.ref, read(dutInstance.rfWriteData))
  forceInitial(dpiRegFileWrite.address.ref, read(dutInstance.rfWriteAddress))
}
