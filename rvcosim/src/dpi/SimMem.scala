package rvcosim.dpi

import chisel3._

case class SimMemParameter(addressWidth: Int, dataWidth: Int)

class SimMem(p: SimMemParameter) extends DPIModule {
  val isImport: Boolean = true
  val clock = dpiTrigger("clock", Input(Bool()))
  val reset = dpiTrigger("reset", Input(Bool()))
  val requestValid = dpiTrigger("valid", Input(Bool()))
  val address = dpiIn("address", Input(UInt(p.addressWidth.W)))
  val writeData = dpiIn("writeData", Input(UInt(p.dataWidth.W)))
  val writeEnable = dpiIn("writeEnable", Input(Bool()))
  val maskByte = dpiIn("maskByte", Input(UInt((p.dataWidth/8).W)))

  val responseValid = dpiOut("responseValid", Output(Bool()))
  val readData = dpiOut("readData", Output(UInt(p.dataWidth.W)))
  override val trigger = s"always @(negedge ${clock.name})"
  override val guard: String = s"""${requestValid.name} && !${reset.name}"""
}
