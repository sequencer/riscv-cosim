package rvcosim.dpi

import chisel3._
import chisel3.probe._

case class InstructionFetchParameter(addressWidth: Int, dataWidth: Int)

class InstructionFetch(p: InstructionFetchParameter) extends DPIModule {
  val isImport: Boolean = true
  val clock = dpiTrigger("clock", RWProbe(Clock()))
  val requestValid = dpiTrigger("requestValid", RWProbe(Bool()))
  val address = dpiIn("address", RWProbe(UInt(p.addressWidth.W)))
  val data = dpiIn("data", Probe(UInt(p.dataWidth.W)))
  val responseValid = dpiOut("responseValid", Probe(Bool()))

  override val trigger: String = s"""always @(negedge ${clock.name}, ${requestValid.name})""".stripMargin
}
