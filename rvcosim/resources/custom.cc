#include "custom.h"
#include "csr.h"
#include "glog.h"

/*
 * # rv_cosim custom extension
 * cosim.exit imm20 11..7=0 6..2=0x1F 1..0=3
 */

static reg_t exit_insn(processor_t *p, insn_t insn, reg_t pc) {
  LOG(INFO) << fmt::format("[spike]\t custom exit instruction at pc 0x{:08X} "
                           "with code {}, switched to exiting mode.",
                           pc, insn.u_imm());
  p->put_csr(CSR_MSIMEND, 1);
  return pc + 4;
}

struct : public arg_t {
  std::string to_string(insn_t insn) const {
    return std::to_string(insn.u_imm());
  }
} exit_code;

std::vector<insn_desc_t> cosim_extension_t::get_instructions() {
  std::vector<insn_desc_t> insns;
  insns.push_back((insn_desc_t){0x7F, 0xfff, exit_insn, exit_insn, exit_insn,
                                exit_insn, exit_insn, exit_insn, exit_insn,
                                exit_insn});
  return insns;
}

std::vector<disasm_insn_t *> cosim_extension_t::get_disasms() {
  std::vector<disasm_insn_t *> insns;
  insns.push_back(new disasm_insn_t("cosim.exit", 0x7F, 0xFFF, {&exit_code}));
  return insns;
}
