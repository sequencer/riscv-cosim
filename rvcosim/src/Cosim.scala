package rvcosim

import chisel3._
import chisel3.probe._
import chisel3.util.experimental.BoringUtils.tapAndRead
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
  val dpiInstructionROM = Module(new InstructionRom(InstructionRomParameter(dutInstance.parameter.ifAddressWidth, dutInstance.parameter.ifDataWidth)))
  val dpiLoadStore = Module(new LoadStore(LoadStoreParameter(dutInstance.parameter.ifAddressWidth, dutInstance.parameter.ifDataWidth)))
  val dpiRegFileWrite = Module(new RegFileWrite)
  val dpiDumpWave = Module(new DumpWave)
  val dpiFinish = Module(new Finish)
  val dpiError = Module(new Error)

  val clock = read(clockGen.clock)
  val reset = read(clockGen.reset)
  dutInstance.clock := read(clockGen.clock).asClock
  dutInstance.reset := read(clockGen.reset)

  dpiInstructionROM.address.ref := tapAndRead(dutInstance.instructionFetch.request.bits.address)
  dutInstance.instructionFetch.response.data := dpiInstructionROM.data.ref

  dpiLoadStore.clock.ref := clock
  dpiLoadStore.requestValid.ref := tapAndRead(dutInstance.loadStore.request.valid) && !reset
  dpiLoadStore.address.ref := tapAndRead(dutInstance.loadStore.request.bits.address)
  dpiLoadStore.writeEnable.ref := tapAndRead(dutInstance.loadStore.request.bits.writeEnable)
  dpiLoadStore.maskByte.ref := tapAndRead(dutInstance.loadStore.request.bits.maskByte)
  dpiLoadStore.storeData.ref := tapAndRead(dutInstance.loadStore.request.bits.data)
  dutInstance.loadStore.response.valid := dpiLoadStore.responseValid.ref
  dutInstance.loadStore.response.bits.data := dpiLoadStore.loadData.ref

  dpiRegFileWrite.clock.ref := clock
  dpiRegFileWrite.writeValid.ref := read(dutInstance.rfWriteValid) && !reset
  dpiRegFileWrite.writeValid.ref := read(dutInstance.rfWriteValid) && !reset
  dpiRegFileWrite.isFp.ref := read(dutInstance.rfWriteFp)
  dpiRegFileWrite.isVector.ref := read(dutInstance.rfWriteVector)
  dpiRegFileWrite.data.ref := read(dutInstance.rfWriteData)
  dpiRegFileWrite.address.ref := read(dutInstance.rfWriteAddress)
}
