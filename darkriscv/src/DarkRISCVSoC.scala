package rvcosim.darkriscv

import chisel3._
import chisel3.util.Valid

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
  val response: Response = Flipped(new Response)
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

class DarkRISCVSoC extends RawModule {
  val clock: Clock = IO(Input(Clock()))
  val reset: Reset = IO(Input(Reset()))
  val instructionFetch = IO(new IFBundle(parameter))
  val loadStore = IO(new LSUBundle(parameter))

  def parameter: CoreParameter = new DarkRISCVParameter

  val darkriscv = Module(new DarkRISCV)
  // TODO: construct diplomatic SoC in the future.
  darkriscv.clock := clock
  darkriscv.reset := reset
  darkriscv.halt := false.B
  darkriscv.idata := instructionFetch.response.data
  instructionFetch.request.bits.address := darkriscv.iaddr
  instructionFetch.request.valid := true.B
  darkriscv.datai := loadStore.response.bits.data
  loadStore.request.bits.data := darkriscv.datao
  loadStore.request.bits.address := darkriscv.daddr
  loadStore.request.bits.maskByte := darkriscv.be
  loadStore.request.bits.writeEnable := darkriscv.wr
  loadStore.request.valid := darkriscv.wr || darkriscv.rd
  // dontcare darkriscv.idle
  // dontcare debug
}
