package rvcosim.dpi

import chisel3._

class Error extends DPIModule {
  val isImport: Boolean = false

  // TODO: think about `chisel3.properties.Property`?
  override val exportBody = s"""
     |function $desiredName(input string what);
     |   $$error(what);
     |endfunction;
     |""".stripMargin
}
