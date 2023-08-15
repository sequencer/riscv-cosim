package rvcosim

import chisel3._

class InitCosim extends DPIModule {
  val isImport: Boolean = true
  val body: String = s"initial $desiredName();"
}
