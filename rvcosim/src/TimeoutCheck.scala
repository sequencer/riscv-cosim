package rvcosim

import chisel3._

case class TimeoutCheckParameter(clockRate: Int)

class TimeoutCheck(p: TimeoutCheckParameter) extends DPIModule{
  val isImport: Boolean = true
  val body: String = s"always #(${2 * p.clockRate + 1}) $desiredName();"
}
