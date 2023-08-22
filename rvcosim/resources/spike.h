#pragma once

#include <mmu.h>
#include <processor.h>

#include "consts.h"
#include "custom.h"
#include "sim.h"

/* Our very own Spike machine. */
class Spike {
public:
  Spike();

  void reg_write(RegClass rc, int n, uint32_t data);
  void mem_read(uint32_t addr, uint32_t *out);
  bool instruction_fetch(uint32_t pc, uint32_t *data);

private:
  /* step Spike forward and log arch changes. */
  void step(insn_fetch_t fetch);

  /* We want to have maximum flexibility, so we use raw processor_t and do
   * everything else ourselves. */
  Sim sim;
  isa_parser_t isa_parser;
  cfg_t cfg;
  processor_t processor;
  cosim_extension_t custom;

  std::vector<commit_log_reg_t> log_reg_write_queue;
  std::vector<commit_log_mem_t> log_mem_read_queue;
  std::vector<commit_log_mem_t> log_mem_write_queue;
};
