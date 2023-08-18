#pragma once

#include <svdpi.h>
#include <verilated.h>
#include <verilated_cov.h>

#include "glog.h"
#include "spike.h"

#define TRY(statement)                                                         \
  try {                                                                        \
    if (!terminated)                                                           \
      statement;                                                               \
  } catch (std::runtime_error & e) {                                           \
    terminate();                                                               \
    LOG(ERROR) << fmt::format(                                                 \
        "emulator detect an exception ({}), gracefully aborting...",           \
        e.what());                                                             \
  }

enum RegClass { GPR = 0, FPR = 1, VRF = 2 };

inline RegClass to_reg_class(bool fp, bool vector) {
  return (RegClass)(fp + (vector << 1));
}

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
  void terminate() { terminated = true; }

  void init() {
    LOG(INFO) << fmt::format("[bridge]\t bridge initializing");
    ctx = Verilated::threadContextp();
    spike = new Spike();
  }
  uint64_t cycle() { return ctx->time(); }

  /* --- bridges --- */
  void reg_write(RegClass rc, int n, uint32_t data);
  void mem_read(uint32_t addr, uint32_t *out);

private:
  bool terminated = false;
  VerilatedContext *ctx = nullptr;
  Spike *spike = nullptr;
};
