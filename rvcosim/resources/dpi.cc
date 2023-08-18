#include <csignal>
#include <svdpi.h>

#include "bridge.h"
#include "glog.h"

#define DPI extern "C"
#define IN const
#define OUT

static Bridge bridge = Bridge();
static void sigint_handler(int s) { bridge.terminate(); }

DPI void instruction_fetch(IN svBitVecVal /* <32> */ *addr,
                           IN svBitVecVal /* <32> */ *data,
                           OUT svBit *resp_valid) {

  CHECK_S(false) << fmt::format(
      "[dpi]\t @{} instruction_fetch() is not yet implemented.",
      bridge.cycle());
}

DPI void load_store(IN svBitVecVal /* 32 */ *addr,
                    IN svBitVecVal /* 32 */ *store_data, IN svBit write_enable,
                    IN svBitVecVal /* 4 */ *mask_byte, OUT svBit *resp_valid,
                    OUT svBitVecVal /* 32 */ *load_data) {

  const char *action = (bool)write_enable ? "write to" : "read from";
  LOG(INFO) << fmt::format(
      "[dpi]\t @{} rtl wants to {} memory at address 0x{:04X}", bridge.cycle(),
      action, (uint32_t)*addr);

  if (write_enable)
    CHECK_S(false) << fmt::format(
        "[dpi]\t @{} mem write is not yet implemented.", bridge.cycle());
  else
    bridge.mem_read((uint32_t)*addr, load_data);
}

DPI void reg_file_write(IN svBit is_fp, IN svBit is_vector,
                        IN svBitVecVal /* 5 */ *addr,
                        IN svBitVecVal /* 32 */ *data) {

  const RegClass rc = to_reg_class(is_fp, is_vector);
  const int n = (uint8_t)*addr;
  const uint32_t d = (uint32_t)*data;

  LOG(INFO) << fmt::format(
      "[dpi]\t @{} rtl wants to write {}#{} with data: 0x{:04X}",
      bridge.cycle(), reg_class_name(rc), n, d);
  bridge.reg_write(rc, n, d);
}

DPI void init_cosim() {
  LOG(INFO) << fmt::format("[dpi]\t initialize dpi<->spike bridge");
  std::signal(SIGINT, sigint_handler);
  google::InitGoogleLogging("emulator");
  bridge.init();

  LOG(INFO) << fmt::format("[dpi]\t @{} dpi<->spike bridge initialized",
                           bridge.cycle());
}

DPI void timeout_check() {

  CHECK_S(false) << fmt::format(
      "[dpi]\t @{} timeout_check() is not yet implemented.", bridge.cycle());
}
