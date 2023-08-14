package rvcosim

case class dpiInitCosimParameter() extends DPIParameter {
  val isImport: Boolean = true
  val body: String = "initial dpiInitCosim();"
}

class dpiInitCosim extends DPIModule(dpiInitCosimParameter())
