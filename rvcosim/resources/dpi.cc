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
      "[dpi] @{} instruction_fetch() is not implemented yet.", bridge.cycle());
}

DPI void load_store(IN svBitVecVal /* 32 */ addr,
                    IN svBitVecVal /* 32 */ *store_data, IN svBit write_enable,
                    IN svBitVecVal /* 4 */ *mask_byte, OUT svBit *resp_valid,
                    OUT svBitVecVal /* 32 */ *load_data) {

  CHECK_S(false) << fmt::format(
      "[dpi] @{} load_store() is not implemented yet.", bridge.cycle());
}

DPI void reg_file_write(IN svBit is_fp, IN svBit is_vector,
                        IN svBitVecVal /* 5 */ *addr,
                        IN svBitVecVal /* 32 */ *data) {

  const char *reg_class =
      (bool)is_fp ? "fpr" : ((bool)is_vector ? "vrf" : "gpr");
  LOG(INFO) << fmt::format(
      "[dpi] @{} rtl wants to write {}#{} with data: 0x{:04X}", bridge.cycle(),
      reg_class, (uint8_t)*addr, (uint32_t)*data);

  CHECK_S(false) << fmt::format(
      "[dpi] @{} reg_file_write() is not implemented yet.", bridge.cycle());
}

DPI void init_cosim() {
  std::signal(SIGINT, sigint_handler);
  google::InitGoogleLogging("emulator");
  bridge.init();

  LOG(INFO) << fmt::format("[dpi] @{} dpi-spike bridge initialized",
                           bridge.cycle());
}

DPI void timeout_check() {

  CHECK_S(false) << fmt::format(
      "[dpi] @{} timeout_check() is not implemented yet.", bridge.cycle());
}
