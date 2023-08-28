package rvcosim.picorv32

import chisel3._
import chisel3.probe._
import chisel3.util.experimental.BoringUtils.{bore, tap, tapAndRead}
import rvcosim._
import rvcosim.dpi._

case class CosimParameter(clockRate: Int)
class Cosim extends RawModule {
  // instantiate the RISC-V Core
  val dutInstance = Module(new Picorv32SoC)

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
}
