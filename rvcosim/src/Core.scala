package rvcosim

import chisel3._
import chisel3.util.Valid
import chisel3.probe._

trait CoreParameter {
  val ifAddressWidth: Int
  val ifDataWidth: Int
  val lsuAddressWidth: Int
  val lsuDataWidth: Int
}

class IFBundle(val parameter: CoreParameter) extends Bundle {
  class Request extends Bundle {
    val address: UInt = UInt(parameter.ifAddressWidth.W)
  }

  class Response extends Bundle {
    val data: UInt = UInt(parameter.ifDataWidth.W)
  }

  val request: Valid[Request] = Valid(new Request)
  val response: Valid[Response] = Flipped(Valid(new Response))
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

  val request: Valid[Request] = Valid(new Request)
  val response: Valid[Response] = Flipped(Valid(new Response))
}

abstract class Core extends RawModule {
  def parameter: CoreParameter
  val clock: Clock = IO(Input(Clock()))
  val reset: Reset = IO(Input(Reset()))
  val instructionFetch = IO(new IFBundle(parameter))
  val loadStore = IO(new LSUBundle(parameter))
  val csrWriteValid = IO(RWProbe(Output(Bool())))
  val csrWriteFp = IO(RWProbe(Output(Bool())))
  val csrWriteVector = IO(RWProbe(Output(Bool())))
  val csrWriteData = IO(RWProbe(Output(UInt(32.W))))
  val csrWriteAddress = IO(RWProbe(Output(UInt(chisel3.util.log2Ceil(32).W))))
}