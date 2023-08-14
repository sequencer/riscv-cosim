#pragma once

#include <svdpi.h>
#include <verilated.h>
#include <verilated_cov.h>

#include "glog.h"

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

class Bridge {
public:
  void terminate() { terminated = true; }

  void init() { ctx = Verilated::threadContextp(); }
  uint64_t cycle() { return ctx->time(); }

private:
  bool terminated = false;

  VerilatedContext *ctx = nullptr;
};
