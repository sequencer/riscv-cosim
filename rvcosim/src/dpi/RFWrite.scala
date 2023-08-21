package rvcosim.dpi

import chisel3._

class RegFileWrite extends DPIModule {
  val isImport: Boolean = true
  val clock = dpiTrigger("clock", Input(Bool()))
  val writeValid = dpiTrigger("writeValid", Input(Bool()))
  val isFp = dpiIn("isFp", Input(Bool()))
  val isVector = dpiIn("isVector", Input(Bool()))
  val address = dpiIn("address", Input(UInt(chisel3.util.log2Ceil(32).W)))
  val data = dpiIn("data", Input(UInt(32.W)))

  override val trigger: String = s"""always @(negedge ${clock.name})""".stripMargin
  override val guard: String = s"""${writeValid.name}"""
}
