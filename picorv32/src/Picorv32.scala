package rvcosim.picorv32

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.util.{HasExtModuleInline, HasExtModuleResource}
import firrtl.stage.FirrtlCircuitAnnotation
import rvcosim._
import chisel3.probe._

class Picorv32Parameter extends CoreParameter {
  override val ifAddressWidth: Int = 32
  override val ifDataWidth: Int = 32
  override val lsuAddressWidth: Int = 32
  override val lsuDataWidth: Int = 32
}

class Picorv32 extends ExtModule
  with HasExtModuleResource
  with HasExtModuleDefine
  with HasExtModuleInline {
  override def desiredName: String = "picorv32"
}
