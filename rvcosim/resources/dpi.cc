#include <VCosim__Dpi.h>
#include <csignal>
#include <svdpi.h>

#include "bridge.h"
#include "glog.h"
#include "utils.h"

#define DPI extern "C"
#define IN const
#define OUT

#define TRY(statement)                                                         \
  try {                                                                        \
    statement;                                                                 \
  } catch (std::runtime_error & e) {                                           \
    LOG(INFO) << fmt::format(                                                  \
        "emulator detect an exception ({}), gracefully aborting...",           \
        e.what());                                                             \
    svSetScope(svGetScopeFromName("TOP.Cosim.dpiError"));                      \
    error(e.what());                                                           \
  }

static Bridge bridge = Bridge();

DPI void instruction_fetch(IN svBitVecVal /* <32> */ *addr,
                           IN svBitVecVal /* <32> */ *data,
                           OUT svBit *resp_valid) {

  LOG(INFO) << fmt::format("[dpi]\t @{} rtl did an instruction fetch at "
                           "address 0x{:08X}, got 0x{:08X}.",
                           bridge.cycle(), (uint32_t)*addr, (uint32_t)*data);
  TRY({
    bridge.check_if_and_record_commitlog((uint32_t)*addr, (uint32_t)*data);
  });
}

DPI void load_store(IN svBitVecVal /* 32 */ *addr,
                    IN svBitVecVal /* 32 */ *store_data, IN svBit write_enable,
                    IN svBitVecVal /* 4 */ *mask_byte, OUT svBit *resp_valid,
                    OUT svBitVecVal /* 32 */ *load_data) {

  const char *action = (bool)write_enable ? "write to" : "read from";
  LOG(INFO) << fmt::format(
      "[dpi]\t @{} rtl wants to {} memory at address 0x{:08X}", bridge.cycle(),
      action, (uint32_t)*addr);

  if (write_enable)
    CHECK_S(false) << fmt::format(
        "[dpi]\t @{} mem write is not yet implemented.", bridge.cycle());
  else
    TRY({ bridge.mem_read((uint32_t)*addr, load_data); });
}

DPI void reg_file_write(IN svBit is_fp, IN svBit is_vector,
                        IN svBitVecVal /* 5 */ *addr,
                        IN svBitVecVal /* 32 */ *data) {

  const RegClass rc = to_reg_class(is_fp, is_vector);
  const int n = (uint8_t)*addr;
  const uint32_t d = (uint32_t)*data;

  LOG(INFO) << fmt::format(
      "[dpi]\t @{} rtl wants to write {}#{} with data: 0x{:08X}",
      bridge.cycle(), reg_class_name(rc), n, d);
  TRY({ bridge.reg_write(rc, n, d); });
}

DPI void init_cosim() {
  google::InitGoogleLogging("emulator");
  LOG(INFO) << fmt::format("[dpi]\t initialize dpi<->spike bridge");
  bridge.init();

  /* register dpi wave dump, wave file will be dumped at exit. */
  svSetScope(svGetScopeFromName("TOP.Cosim.dpiDumpWave"));
  dump_wave(env("COSIM_wave"));

  LOG(INFO) << fmt::format("[dpi]\t @{} dpi<->spike bridge initialized",
                           bridge.cycle());
}

DPI void timeout_check() {
  /* FIXME: proper implementation. */
  TRY({
    if (bridge.cycle() > 1000) {
      LOG(FATAL_S) << fmt::format("[dpi]\t @{} timeout, exiting...",
                                  bridge.cycle());
    }
  })
}
