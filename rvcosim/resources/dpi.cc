#include <csignal>

#include "bridge.h"
#include "glog.h"

#define DPI extern "C"

#define dpi_init_cosim dpiInitCosim
#define dpi_timeout_check dpiTimeoutCheck

static Bridge bridge = Bridge();

void sigint_handler(int s) { bridge.terminate(); }

DPI void dpi_init_cosim() {
  std::signal(SIGINT, sigint_handler);
  google::InitGoogleLogging("emulator");
  bridge.init();

  LOG(INFO) << fmt::format("[bridge] @{} initialized", bridge.cycle());
}

DPI void dpi_timeout_check() { exit(1); }
