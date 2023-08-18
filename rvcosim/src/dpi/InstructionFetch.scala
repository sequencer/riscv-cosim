package rvcosim.dpi

import chisel3._

case class InstructionFetchParameter(addressWidth: Int, dataWidth: Int)

class InstructionFetch(p: InstructionFetchParameter) extends DPIModule {
  val isImport: Boolean = true
  val clock = dpiTrigger("clock", Input(Bool()))
  val requestValid = dpiTrigger("requestValid", Input(Bool()))
  val address = dpiIn("address", Input(UInt(p.addressWidth.W)))
  val data = dpiOut("data", Output(UInt(p.dataWidth.W)))
  val responseValid = dpiOut("responseValid", Output(Bool()))

  override val trigger: String = s"""always @(negedge ${clock.name}, ${requestValid.name})""".stripMargin
}
