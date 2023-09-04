package rvcosim.dpi

import chisel3._

case class IssueParameter(addressWidth: Int)

class Issue(p: IssueParameter) extends DPIModule {
  val isImport: Boolean = true
  val clock = dpiTrigger("clock", Input(Bool()))
  val valid = dpiTrigger("valid", Input(Bool()))
  val address = dpiIn("address", Input(UInt(p.addressWidth.W)))

  override val trigger: String = s"""always @(negedge ${clock.name})""".stripMargin
  override val guard: String = s"""${valid.name}"""
}
