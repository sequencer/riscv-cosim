package rvcosim

import chisel3._
import chisel3.probe._

case class dpiInstructionFetchParameter(addressWidth: Int, dataWidth: Int) extends DPIParameter {
  val isImport: Boolean = true
  val clock = bind("clock", false, false, RWProbe(Clock()))
  val valid = bind("valid", false, false, RWProbe(Bool()))
  val address = bind("address", false, true, RWProbe(UInt(addressWidth.W)))
  val data = bind("data", true, true, Probe(UInt(dataWidth.W)))
  val ready = bind("ready", true, true, Probe(Bool()))

  val body: String =
    s"""always @(negedge ${clock.name}, ${valid.name})
       |  dpiInstructionFetch(
       |    ${address.name},
       |    ${data.name},
       |    ${ready.name}
       |  );""".stripMargin
}

class dpiInstructionFetch(p: dpiInstructionFetchParameter) extends DPIModule(p)
