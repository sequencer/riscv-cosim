#pragma once

#include <riscv/extension.h>

#include "exceptions.h"

class cosim_extension_t : public extension_t {
  std::vector<insn_desc_t> get_instructions() override;
  std::vector<disasm_insn_t *> get_disasms() override;
  const char *name() override { return "cosim"; }
};
