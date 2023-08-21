package rvcosim.dpi

import chisel3._

case class InstructionRomParameter(addressWidth: Int, dataWidth: Int)

class InstructionRom(p: InstructionRomParameter) extends DPIModule {
  val isImport: Boolean = true
  val address = dpiIn("address", Input(UInt(p.addressWidth.W)))
  val data = dpiOut("data", Output(UInt(p.dataWidth.W)))

  override val trigger: String = s"""always @(${address.name})""".stripMargin
}
