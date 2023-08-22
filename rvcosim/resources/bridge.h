#pragma once

#include <svdpi.h>
#include <verilated.h>
#include <verilated_cov.h>

#include "glog.h"
#include "spike.h"

/* A bridge between Spike and DPI. */
class Bridge {
public:
  Bridge() {}
  ~Bridge() { delete spike; }

  void init() {
    LOG(INFO) << fmt::format("[bridge] initializing");
    ctx = Verilated::threadContextp();
    spike = new Spike();
  }
  uint64_t cycle() { return ctx->time(); }

  /* --- bridges --- */
  void instruction_fetch(uint32_t addr, uint32_t *data);
  void reg_write(RegClass rc, int n, uint32_t data);
  void mem_read(uint32_t addr, uint32_t *out);
  void mem_write();

  void timeout_check();

private:
  VerilatedContext *ctx = nullptr;
  Spike *spike = nullptr;
  int64_t exiting_cycle = std::numeric_limits<int64_t>::min();
};
