package rvcosim.dpi

import chisel3._
import chisel3.probe.{Probe, RWProbe}

case class LoadStoreParameter(addressWidth: Int, dataWidth: Int)

class LoadStore(p: LoadStoreParameter) extends DPIModule {
  val isImport: Boolean = true
  val clock = dpiTrigger("clock", RWProbe(Clock()))
  val requestValid = dpiTrigger("valid", RWProbe(Bool()))
  val address = dpiIn("address", RWProbe(UInt(p.addressWidth.W)))
  val storeData = dpiIn("storeData", RWProbe(UInt(p.dataWidth.W)))
  val writeEnable = dpiIn("writeEnable", RWProbe(Bool()))
  val maskByte = dpiIn("maskByte", RWProbe(UInt((p.dataWidth/8).W)))

  val responseValid = dpiOut("responseValid", RWProbe(Bool()))
  val loadData = dpiOut("loadData", RWProbe(UInt(p.dataWidth.W)))
  override val trigger = s"always @(negedge ${clock.name}, ${requestValid.name})"
}
