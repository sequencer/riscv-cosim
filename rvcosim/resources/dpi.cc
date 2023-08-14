#include <csignal>

#include "bridge.h"
#include "glog.h"

#define BRIDGE_API extern "C"

static Bridge bridge = Bridge();

void sigint_handler(int s) { bridge.terminate(); }

BRIDGE_API void dpiInitCosim() {
  LOG(INFO) << fmt::format("dpi_init_cosim");

  std::signal(SIGINT, sigint_handler);
  google::InitGoogleLogging("emulator");
  bridge.init();
}

BRIDGE_API void dpiTimeoutCheck() { exit(1); }
