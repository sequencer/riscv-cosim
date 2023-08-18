package rvcosim

import chisel3._
import chisel3.util.Valid
import chisel3.probe._

trait CoreParameter {
  val ifAddressWidth: Int
  val ifDataWidth: Int
  val lsuAddressWidth: Int
  val lsuDataWidth: Int
  val xlen: Int
}

class IFBundle(val parameter: CoreParameter) extends Bundle {
  class Request extends Bundle {
    val address: UInt = UInt(parameter.ifAddressWidth.W)
  }

  class Response extends Bundle {
    val data: UInt = UInt(parameter.ifDataWidth.W)
  }

  val request: Valid[Request] = Probe(Valid(new Request))
  val response: Valid[Response] = RWProbe(Valid(new Response))
}

class LSUBundle(val parameter: CoreParameter) extends Bundle {
  class Request extends Bundle {
    val address: UInt = UInt(parameter.lsuAddressWidth.W)
    val writeEnable: Bool = Bool()
    val maskByte: UInt = UInt((parameter.lsuDataWidth / 8).W)
    val data: UInt = UInt(parameter.lsuDataWidth.W)
  }

  class Response extends Bundle {
    val data = UInt(parameter.lsuDataWidth.W)
  }

  val request: Valid[Request] = RWProbe(Valid(new Request))
  val response: Valid[Response] = RWProbe((Valid(new Response)))
}

class RFBundle(val parameter: CoreParameter) extends Bundle {
  val data = UInt(parameter.xlen.W)
  val address = UInt(chisel3.util.log2Ceil(32).W)
}

abstract class Core extends RawModule {
  def parameter: CoreParameter
  val clockRef: Clock = IO(RWProbe(Clock()))
  val resetRef: Reset = IO(RWProbe(Reset()))
  val instructionFetchRef = IO(new IFBundle(parameter))
  val loadStoreRef = IO(new LSUBundle(parameter))
  val rfRef = IO(RWProbe(Valid(new RFBundle(parameter))))
}