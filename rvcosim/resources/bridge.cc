#include "bridge.h"
#include "exceptions.h"

constexpr int64_t timeout = 10;

void Bridge::instruction_fetch(uint32_t addr, uint32_t *data) {
  bool is_exiting = spike->instruction_fetch(addr, data);
  if (is_exiting && exiting_cycle < 0) {
    exiting_cycle = cycle();
  }
}

void Bridge::reg_write(RegClass rc, int n, uint32_t data) { spike->reg_write(rc, n, data); }

void Bridge::mem_read(uint32_t addr, uint32_t *out) { spike->mem_read(addr, out); }

void Bridge::mem_write(uint32_t addr, uint32_t data) {} /* TODO */

void Bridge::timeout_check() {
  if (cycle() > exiting_cycle + timeout) {
    LOG(INFO) << fmt::format(
        "[bridge] cosim is exiting {} cycles ago, but the queue is still not empty, timed out.",
        cycle() - exiting_cycle);
    throw TimeoutException();
  }
}
