package rvcosim.dpi

import chisel3._

case class LoadStoreParameter(addressWidth: Int, dataWidth: Int)

class LoadStore(p: LoadStoreParameter) extends DPIModule {
  val isImport: Boolean = true
  val clock = dpiTrigger("clock", Input(Bool()))
  val requestValid = dpiTrigger("valid", Input(Bool()))
  val address = dpiIn("address", Input(UInt(p.addressWidth.W)))
  val storeData = dpiIn("storeData", Input(UInt(p.dataWidth.W)))
  val writeEnable = dpiIn("writeEnable", Input(Bool()))
  val maskByte = dpiIn("maskByte", Input(UInt((p.dataWidth/8).W)))

  val responseValid = dpiOut("responseValid", Output(Bool()))
  val loadData = dpiOut("loadData", Output(UInt(p.dataWidth.W)))
  override val trigger = s"always @(negedge ${clock.name}, ${requestValid.name})"
}
