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
  val dpiFinish = Module(new Finish)
  val dpiError = Module(new Error)
  // TODO: here is the issue:
  //       at Top, I wanna define two XMR together, one from DPI and one from DUT
  define(dutInstance.clockRef, clockGen.clock)
  define(dutInstance.resetRef, clockGen.reset)

  // define(dpiInstructionFetch.clock.ref, clockGen.clock)
  // define(dpiInstructionFetch.requestValid.ref, dutInstance.instructionFetchRef.request.valid)
  // define(dpiInstructionFetch.address.ref, dutInstance.instructionFetchRef.request.bits.address)
  // define(dutInstance.instructionFetchRef.response.bits.data, dpiInstructionFetch.data.ref)
  // define(dutInstance.instructionFetchRef.response.valid, dpiInstructionFetch.responseValid.ref)

  // define(dpiLoadStore.clock.ref, clockGen.clock)
  // define(dpiLoadStore.requestValid.ref, dutInstance.loadStoreRef.request.valid)
  // define(dpiLoadStore.address.ref, dutInstance.loadStoreRef.request.bits.address)
  // define(dpiLoadStore.writeEnable.ref, dutInstance.loadStoreRef.request.bits.writeEnable)
  // define(dpiLoadStore.maskByte.ref, dutInstance.loadStoreRef.request.bits.maskByte)
  // define(dpiLoadStore.storeData.ref, dutInstance.loadStoreRef.request.bits.data)
  // define(dutInstance.loadStoreRef.response.valid, dpiLoadStore.responseValid.ref)
  // define(dutInstance.loadStoreRef.response.bits.data, dpiLoadStore.loadData.ref)

  // define(dpiRegFileWrite.clock.ref, clockGen.clock)
  // define(dpiRegFileWrite.writeValid.ref, dutInstance.rfRef.valid)
  // define(dpiRegFileWrite.isFp.ref, RWProbeValue(WireDefault(false.B)))
  // define(dpiRegFileWrite.isVector.ref, RWProbeValue(WireDefault(false.B)))
  // define(dpiRegFileWrite.data.ref, dutInstance.rfRef.bits.data)
  // define(dpiRegFileWrite.address.ref, dutInstance.rfRef.bits.address)
}
