#pragma once

#include <mmu.h>
#include <processor.h>

#include "sim.h"

/* Our very own Spike machine. */
class Spike {
public:
  Spike();

private:
  /* We want to have maximum flexibility, so we use raw processor_t and do
   * everything else ourselves. */
  Sim sim;
  isa_parser_t isa_parser;
  cfg_t cfg;
  processor_t processor;
};

// Write this CSR to end simulation.
constexpr uint32_t CSR_MSIMEND = 0x7cc;
