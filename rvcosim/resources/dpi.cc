#include <VCosim__Dpi.h>
#include <csignal>
#include <svdpi.h>

#include "bridge.h"
#include "consts.h"
#include "exceptions.h"
#include "glog.h"
#include "utils.h"

#define DPI extern "C"
#define IN const
#define OUT

#define TRY(statement)                                                                             \
  do {                                                                                             \
    try {                                                                                          \
      statement;                                                                                   \
    } catch (const ExitException &e) {                                                             \
      DUMP(INFO, dpi) << fmt::format("emulator return, gracefully aborting...");                   \
      svSetScope(svGetScopeFromName("TOP.Cosim.dpiFinish"));                                       \
      finish();                                                                                    \
    } catch (const TimeoutException &e) {                                                          \
      DUMP(INFO, dpi) << fmt::format("emulator timeout, gracefully aborting...");                  \
      svSetScope(svGetScopeFromName("TOP.Cosim.dpiError"));                                        \
      error("timeout");                                                                            \
    } catch (...) {                                                                                \
      DUMP(INFO, dpi) << fmt::format(                                                              \
          "emulator detected an unknown exception, gracefully aborting...");                       \
      svSetScope(svGetScopeFromName("TOP.Cosim.dpiError"));                                        \
      error("unknown error");                                                                      \
    }                                                                                              \
  } while (0)

static Bridge bridge = Bridge();

DPI void instruction_rom(IN svBitVecVal *, OUT svBitVecVal *)
    __attribute__((alias("instruction_fetch")));
DPI void instruction_fetch(IN svBitVecVal /* <32> */ *addr, OUT svBitVecVal /* <32> */ *data) {
  DUMP(INFO, rtl) << fmt::format("@{} fetch instruction from pc 0x{:08X}.", bridge.cycle(),
                                 (uint32_t)*addr);
  TRY({ bridge.instruction_fetch((uint32_t)*addr, (uint32_t *)data); });
}

DPI void issue(IN svBitVecVal /* <32> */ *pc) {
  DUMP(INFO, rtl) << fmt::format("@{} issue instruction from pc 0x{:08X}.", bridge.cycle(),
                                 (uint32_t)*pc);
  TRY({ bridge.issue((uint32_t)*pc); });
}

DPI void load_store(IN svBitVecVal /* 32 */ *addr, IN svBitVecVal /* 32 */ *store_data,
                    IN svBit write_enable, IN svBitVecVal /* 4 */ *mask_byte, OUT svBit *resp_valid,
                    OUT svBitVecVal /* 32 */ *load_data) {
  const char *action = (bool)write_enable ? "write to" : "read from";
  DUMP(INFO, rtl) << fmt::format("@{} {} address 0x{:08X}", bridge.cycle(), action,
                                 (uint32_t)*addr);
  if (write_enable)
    TRY({
      bridge.mem_write((uint32_t)*addr,
                       *(uint32_t *)store_data & expand_mask(*(uint8_t *)mask_byte));
    });
  else
    TRY({ bridge.mem_read((uint32_t)*addr, load_data); });

  *resp_valid = true;
}

DPI void reg_file_write(IN svBit is_fp, IN svBit is_vector, IN svBitVecVal /* 5 */ *addr,
                        IN svBitVecVal /* 32 */ *data) {
  const RegClass rc = to_reg_class(is_fp, is_vector);
  const int n = (uint8_t)*addr;
  const uint32_t d = (uint32_t)*data;

  DUMP(INFO, rtl) << fmt::format("@{} write {}#{} with data: 0x{:08X}", bridge.cycle(),
                                 reg_class_name(rc), n, d);
  TRY({ bridge.reg_write(rc, n, d); });
}

static void custom_prefix(std::ostream &s, const google::LogMessageInfo &l, void *) {
  s << std::setfill(' ') << l.severity[0] << " " << std::setw(16)
    << fmt::format("{}:{}", l.filename, l.line_number);
}

DPI void init_cosim() {
  google::InitGoogleLogging("emulator", &custom_prefix);
  bridge.init();
  /* register dpi wave dump, wave file will be dumped at exit. */
  svSetScope(svGetScopeFromName("TOP.Cosim.dpiDumpWave"));
  dump_wave(env("COSIM_wave"));

  DUMP(INFO, dpi) << fmt::format("@{} initialized", bridge.cycle());
}

DPI void timeout_check() {
  TRY({ bridge.timeout_check(); });
}
