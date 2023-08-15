package rvcosim

import chisel3._
import chisel3.probe._

case class InstructionFetchParameter(addressWidth: Int, dataWidth: Int)

class InstructionFetch(p: InstructionFetchParameter) extends DPIModule {
  val isImport: Boolean = true
  val clock = bind("clock", false, false, RWProbe(Clock()))
  val requestValid = bind("requestValid", false, false, RWProbe(Bool()))
  val address = bind("address", false, true, RWProbe(UInt(p.addressWidth.W)))
  val data = bind("data", true, true, Probe(UInt(p.dataWidth.W)))
  val responseValid = bind("responseValid", true, true, Probe(Bool()))

  val body: String =
    s"""always @(negedge ${clock.name}, ${requestValid.name})
       |  $desiredName(
       |    ${address.name},
       |    ${data.name},
       |    ${responseValid.name}
       |  );""".stripMargin
}
