package rvcosim.dpi

import chisel3._
import chisel3.probe._

class RFWrite extends DPIModule {
  val isImport: Boolean = true
  val clock = dpiTrigger("clock", RWProbe(Bool()))
  val writeValid = dpiTrigger("writeValid", RWProbe(Bool()))
  val isFp = dpiIn("isFp", RWProbe(Bool()))
  val isVector = dpiIn("isVector", RWProbe(Bool()))
  val address = dpiIn("address", RWProbe(UInt(chisel3.util.log2Ceil(32).W)))
  val data = dpiIn("data", RWProbe(UInt(32.W)))

  override val trigger: String = s"""always @(negedge ${clock.name}, ${writeValid.name})""".stripMargin
}
