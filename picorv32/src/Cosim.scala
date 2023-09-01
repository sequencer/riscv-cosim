package rvcosim.picorv32

import chisel3._
import chisel3.probe._
import chisel3.util.experimental.BoringUtils.{bore, tap, tapAndRead}
import rvcosim._
import rvcosim.dpi._

case class CosimParameter(clockRate: Int)
class Cosim extends RawModule {
  // The user defined simulation parameter
  def parameter = CosimParameter(clockRate = 2)

  val clockGen = Module(new ClockGen(ClockGenParameter(parameter.clockRate)))
  val dpiInitCosim = Module(new InitCosim)
  val dpiTimeoutCheck = Module(new TimeoutCheck(TimeoutCheckParameter(parameter.clockRate)))
  val dpiInstructionFetch = Module(new InstructionFetch(InstructionFetchParameter(32, 32)))
  val dpiLoadStore = Module(new LoadStore(LoadStoreParameter(32, 32)))
  val dpiRegFileWrite = Module(new RegFileWrite)
  val dpiIssue = Module(new Issue)
  val dpiRetire = Module(new Retire)
  val dpiDumpWave = Module(new DumpWave)
  val dpiFinish = Module(new Finish)
  val dpiError = Module(new Error)

  val picorv32 = Module(new Picorv32)

  val clock = read(clockGen.clock)
  val reset = read(clockGen.reset)

  // Input
  picorv32.clock := clock.asClock
  picorv32.resetn := !reset.asBool

  // picorv32.trap

  // always ready?
  picorv32.memReady := true.B
  picorv32.memReadData := Mux(picorv32.memInstruction, dpiInstructionFetch.data.ref, dpiLoadStore.loadData.ref)

  dpiLoadStore.clock.ref := clock
  dpiLoadStore.requestValid.ref := picorv32.memoryValid && !picorv32.memInstruction
  dpiLoadStore.address.ref := picorv32.memAddress
  dpiLoadStore.storeData.ref := picorv32.memWriteData
  dpiLoadStore.writeEnable.ref := picorv32.memWriteMask.orR
  dpiLoadStore.maskByte.ref := picorv32.memWriteMask

  dpiInstructionFetch.clock.ref := clock
  dpiInstructionFetch.requestValid.ref := picorv32.memoryValid && picorv32.memInstruction
  dpiInstructionFetch.address.ref := picorv32.memAddress

  dpiRegFileWrite.clock.ref := clock
  dpiRegFileWrite.writeValid.ref := read(bore(picorv32.cpuRegsWrite))
  dpiRegFileWrite.isFp.ref := false.B
  dpiRegFileWrite.isVector.ref := false.B
  dpiRegFileWrite.address.ref := read(bore(picorv32.latchedRd))
  dpiRegFileWrite.data.ref := read(bore(picorv32.cpuRegsWriteData))
  dpiIssue.clock.ref := clock
  dpiIssue.valid.ref := read(bore(picorv32.launchNextInsn))
  dpiIssue.pc.ref := read(bore(picorv32.nextPc))

  dpiRetire.clock.ref := clock
  //
  dpiRetire.valid.ref :=
    // line 1800
    (
      (read(bore(picorv32.cpuState)) === "b00001000".U) &&
        (
          (read(bore(picorv32.memDone)) && read(bore(picorv32.isBranch))) ||
          !read(bore(picorv32.isBranch))
          )
      ) ||
  // 1824
      (
        (read(bore(picorv32.cpuState)) === "b00000100".U) &&
          (read(bore(picorv32.regSh)) === 0.U)
        ) ||
  // 1849
      (
        (read(bore(picorv32.cpuState)) === "b00000010".U) &&
          (read(bore(picorv32.memDone))) && ! (read(bore(picorv32.memDoPrefetch)))
        ) ||
  // 1875
      (
        (read(bore(picorv32.cpuState)) === "b00000001".U) &&
          (read(bore(picorv32.memDone))) && !(read(bore(picorv32.memDoPrefetch)))
        )

  dpiRetire.pc.ref := read(bore(picorv32.currentPc))


  // DontCare
  // picorv32.memoryLookAheadRead
  // picorv32.memoryLookAheadWrite
  // picorv32.memoryLookAheadAddress
  // picorv32.memoryLookAheadWriteData
  // picorv32.memoryLookAheadWriteMask
  // picorv32.picoCoProcessorInterfaceValid
  // picorv32.picoCoProcessorInterfaceInstruction
  // picorv32.picoCoProcessorInterfaceRs1
  // picorv32.picoCoProcessorInterfaceRs2
  picorv32.picoCoProcessorInterfaceWrite := false.B
  picorv32.picoCoProcessorInterfaceRd := 0.U
  picorv32.picoCoProcessorInterfaceWait := false.B
  picorv32.picoCoProcessorInterfaceReady := false.B

  picorv32.irq := 0.U
  // picorv32.eoi
  // picorv32.traceValid
  // picorv32.traceData

}
