#include "bridge.h"
#include "exceptions.h"

constexpr int64_t timeout = 10;

void Bridge::instruction_fetch(uint32_t addr, uint32_t *data) {
  spike->instruction_fetch(addr, data);
}

void Bridge::issue(uint32_t pc) {
  bool is_exiting = spike->issue(pc);
  if (is_exiting && exiting_cycle < 0)
    exiting_cycle = cycle();
}

void Bridge::reg_write(RegClass rc, int n, uint32_t data) { spike->reg_write(rc, n, data); }

void Bridge::mem_read(uint32_t addr, uint32_t *out) { spike->mem_read(addr, out); }

void Bridge::mem_write(uint32_t addr, uint32_t data) {} /* TODO */

void Bridge::retire(uint32_t pc) { spike->retire(pc); }

void Bridge::timeout_check() {
  if (cycle() > exiting_cycle + timeout) {
    DUMP(INFO, bridge) << fmt::format(
        "cosim is exiting {} cycles ago, but the queue is still not empty, timed out.",
        cycle() - exiting_cycle);
    throw TimeoutException();
  }
}
