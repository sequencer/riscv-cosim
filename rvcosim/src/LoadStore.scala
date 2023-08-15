package rvcosim

import chisel3._
import chisel3.probe.{Probe, RWProbe}

case class LoadStoreParameter(addressWidth: Int, dataWidth: Int)

class LoadStore(p: LoadStoreParameter) extends DPIModule {
  val isImport: Boolean = true
  val clock = bind("clock", false, false, RWProbe(Clock()))
  val requestValid = bind("valid", false, false, RWProbe(Bool()))
  val address = bind("address", false, true, RWProbe(UInt(p.addressWidth.W)))
  val storeData = bind("storeData", false, true, RWProbe(UInt(p.dataWidth.W)))
  val writeEnable = bind("writeEnable", false, true, RWProbe(Bool()))
  val maskByte = bind("maskByte", false, true, RWProbe(UInt((p.dataWidth/8).W)))

  val responseValid = bind("responseValid", true, true, Probe(Bool()))
  val loadData = bind("loadData", true, true, Probe(UInt(p.dataWidth.W)))

  val body: String =
    s"""always @(negedge ${clock.name}, ${requestValid.name})
       |  $desiredName(
       |    ${address.name},
       |    ${storeData.name},
       |    ${writeEnable.name},
       |    ${maskByte.name},
       |    ${responseValid.name},
       |    ${loadData.name}
       |  );""".stripMargin
}
