#pragma once

#include <svdpi.h>
#include <verilated.h>
#include <verilated_cov.h>

#include "glog.h"
#include "spike.h"

enum RegClass { GPR = 0, FPR = 1, VRF = 2 };

inline RegClass to_reg_class(bool fp, bool vector) { return (RegClass)(fp + (vector << 1)); }

inline const char *reg_class_name(RegClass rc) {
  switch (rc) {
  case GPR:
    return "gpr";
  case FPR:
    return "fpr";
  case VRF:
    return "vrf";
  default:
    CHECK_S(false) << fmt::format("unreachable");
  }
}

/* A bridge between Spike and DPI. */
class Bridge {
public:
  Bridge() {}
  ~Bridge() { delete spike; }

  void init() {
    LOG(INFO) << fmt::format("[bridge]\t bridge initializing");
    ctx = Verilated::threadContextp();
    spike = new Spike();
  }
  uint64_t cycle() { return ctx->time(); }

  /* --- bridges --- */
  void reg_write(RegClass rc, int n, uint32_t data);
  void mem_read(uint32_t addr, uint32_t *out);
  void instruction_fetch(uint32_t addr, uint32_t *data);

private:
  VerilatedContext *ctx = nullptr;
  Spike *spike = nullptr;
};
