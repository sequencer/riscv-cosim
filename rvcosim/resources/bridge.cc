#include "bridge.h"

void Bridge::reg_write(RegClass rc, int n, uint32_t data) {
  if (rc == RegClass::GPR && n == 0) {
    LOG(INFO) << fmt::format("[bridge]\t @{} ignore write to x0.", cycle());
    return;
  }

  CHECK_S(rc == RegClass::GPR) << fmt::format("write to fpr/vrf is not yet implemented.");

  CHECK_S(false) << fmt::format("reg_file_write() is not yet implemented.");
}

void Bridge::mem_read(uint32_t addr, uint32_t *out) { spike->mem_read(addr, out); }

void Bridge::instruction_fetch(uint32_t addr, uint32_t *data) {
  spike->instruction_fetch(addr, data);
}
