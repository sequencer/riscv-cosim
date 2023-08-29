package rvcosim.dpi

import chisel3._


/* TODO:
 *   isa -> config
 *   cfg ->
 *     initrd_bounds
 *     bootargs
 *     isa
 *     priv
 *     varch
 *     misaligned
 *     endianness
 *     pmpregions
 *     mem_layout
 *     hartids
 *     real_time_clint
 *     trigger_count
 *   sim
 *   id
 *   halt_on_reset
 *   log_file_t
 *   sout
 */
case class SpikeParameter(ifDataWidth: Int = 32, isa: String) {
  val ifAddressWidth: Int = 64
  val loadAddressWidth: Int = 64
  val loadDataWidth: Int = 64
  val storeAddressWidth: Int = 64
  val storeDataWidth: Int = 64
}

class Spike(p: SpikeParameter) extends DPIModule {
  val isImport: Boolean = true
  val clock = dpiTrigger("clock", Input(Bool()))
  val reset = dpiTrigger("reset", Input(Bool()))

  val resetVector = dpiIn("resetVector", Input(UInt(64.W)))
  val hartid = dpiIn("hartid", Input(UInt(64.W)))

  val debug = dpiIn("debug", Input(Bool()))
  val mtip = dpiIn("mtip", Input(Bool()))
  val msip = dpiIn("msip", Input(Bool()))
  val meip = dpiIn("meip", Input(Bool()))
  val seip = dpiIn("seip", Input(Bool()))

  val ifValid = dpiOut("ifValid", Output(Bool()))
  val ifReady = dpiIn("ifReady", Input(Bool()))
  val ifAddress = dpiOut("ifAddress", Input(UInt(p.ifAddressWidth.W)))
  val ifData = dpiOut("ifData", Output(UInt(p.ifDataWidth.W)))

  val loadValid = dpiOut("loadValid", Output(Bool()))
  val loadReady = dpiIn("loadReady", Input(Bool()))
  val loadAddress = dpiOut("loadAddress", Input(UInt(p.loadAddressWidth.W)))
  val loadData = dpiIn("loadData", Output(UInt(p.loadDataWidth.W)))
  val loadMask = dpiIn("loadMask", Output(UInt((p.loadDataWidth/8).W)))

  val storeValid = dpiOut("storeValid", Output(Bool()))
  val storeReady = dpiIn("storeReady", Input(Bool()))
  val storeAddress = dpiOut("storeAddress", Input(UInt(p.storeAddressWidth.W)))
  val storeData = dpiIn("storeData", Output(UInt(p.storeDataWidth.W)))
  val storeMask = dpiIn("storeMask", Output(UInt((p.storeDataWidth / 8).W)))

  override val trigger = s"always @(negedge ${clock.name}, ${reset.name})"
  override val initial = Map(
    "input string isa" -> p.isa,
    "input int hartid" -> hartid.name,
  )
  override val guard: String = s"""!${reset.name} || ${mtip.name} || ${msip.name} || ${meip.name} || ${seip.name} || ${ifReady.name} || ${loadReady.name} || ${storeReady.name} """
}
