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

/* A bridge between Spike and DPI. */
class Bridge {
public:
  Bridge() {}
  ~Bridge() { delete spike; }
  void terminate() { terminated = true; }

  void init() {
    ctx = Verilated::threadContextp();
    spike = new Spike();
  }
  uint64_t cycle() { return ctx->time(); }

private:
  bool terminated = false;
  VerilatedContext *ctx = nullptr;
  Spike *spike = nullptr;
};
