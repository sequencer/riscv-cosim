package rvcosim

case class dpiTimeoutCheckParameter(clockRate: Int) extends DPIParameter {
  val isImport: Boolean = true
  val body: String = s"always #(${2 * clockRate + 1}) dpiTimeoutCheck();"
}

class dpiTimeoutCheck(p: dpiTimeoutCheckParameter) extends DPIModule(p)
