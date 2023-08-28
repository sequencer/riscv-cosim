#pragma once

#include <fmt/core.h>
#include <fstream>
#include <simif.h>

#include "glog.h"

/* A naive implementation of simif_t to make libspike happy. */
class Sim : public simif_t {
public:
  explicit Sim(size_t memsize) : memsize(memsize) { mem = new char[memsize]; }
  ~Sim() override { delete[] mem; }

  char *addr_to_mem(reg_t addr) override {
    ASSERT(addr < memsize) << fmt::format("memory out of bound ({:016X} >= {:016X})", addr,
                                          memsize);
    return &mem[addr];
  }

  bool mmio_load(reg_t addr, size_t len, uint8_t *bytes) override { LOG(FATAL_S) << "unreachable"; }

  bool mmio_store(reg_t addr, size_t len, const uint8_t *bytes) override {
    LOG(FATAL_S) << "unreachable";
  }

  [[nodiscard]] const cfg_t &get_cfg() const override { LOG(FATAL_S) << "unreachable"; }

  [[nodiscard]] const std::map<size_t, processor_t *> &get_harts() const override {
    LOG(FATAL_S) << "unreachable";
  }

  void proc_reset(unsigned id) override {}

  const char *get_symbol(uint64_t addr) override { LOG(FATAL_S) << "unreachable"; }

  char *mem;
  size_t memsize;
};
