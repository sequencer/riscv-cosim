#include "bridge.h"
#include "exceptions.h"

constexpr int64_t timeout = 10;

void Bridge::instruction_fetch(uint32_t addr, uint32_t *data) {
  bool is_exiting = spike->instruction_fetch(addr, data);
  if (is_exiting && exiting_cycle < 0) {
    exiting_cycle = cycle();
  }
}

void Bridge::reg_write(RegClass rc, int n, uint32_t data) {
  if (rc == RegClass::GPR && n == 0) {
    LOG(INFO) << fmt::format("[bridge] @{} ignore write to x0.", cycle());
    return;
  }

  CHECK_S(rc == RegClass::GPR) << fmt::format("write to fpr/vrf is NYI.");

  CHECK_S(false) << fmt::format("reg_write() is NYI.");
}

void Bridge::mem_read(uint32_t addr, uint32_t *out) { spike->mem_read(addr, out); }

void Bridge::mem_write() { CHECK_S(false) << fmt::format("mem_write() is NYI."); }

void Bridge::timeout_check() {
  if (cycle() > exiting_cycle + timeout) {
    LOG(INFO) << fmt::format(
        "[bridge] cosim is exiting {} cycles ago, but the queue is still not empty, timed out.",
        cycle() - exiting_cycle);
    throw TimeoutException();
  }
}
