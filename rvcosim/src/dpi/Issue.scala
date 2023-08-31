package rvcosim.dpi

import chisel3._

class Issue extends DPIModule {
  val isImport: Boolean = true
  val clock = dpiTrigger("clock", Input(Bool()))
  val valid = dpiTrigger("valid", Input(Bool()))
  val pc = dpiIn("pc", Input(UInt(32.W)))

  override val trigger: String = s"""always @(negedge ${clock.name})""".stripMargin
  override val guard: String = s"""${valid.name}"""
}
