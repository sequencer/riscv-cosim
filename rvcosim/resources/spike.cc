#include <decode_macros.h>
#include <disasm.h>

#include "elfloader.h"
#include "spike.h"
#include "utils.h"

/* FIXME: make this configurable at runtime (?) */
const uint32_t reset_vector_addr = 0;

Spike::Spike()
    : sim(1 << (10 + 10 + 6)), /* 64M memory should be enough for now. */
      isa_parser(env("COSIM_isa"), "M"),
      cfg(std::make_pair((reg_t)0, (reg_t)0), /* default_initrd_bounds */
          nullptr,                            /* default_bootargs */
          DEFAULT_ISA,                        /* default_isa */
          DEFAULT_PRIV,                       /* default_priv */
          DEFAULT_VARCH,                      /* default_varch */
          false,                              /* default_misaligned */
          endianness_little,                  /* default_endianness */
          16,                                 /* default_pmpregions */
          std::vector<mem_cfg_t>(),           /* default_mem_layout */
          std::vector<size_t>(),              /* default_hartids */
          false,                              /* default_real_time_clint */
          4                                   /* default_trigger_count */
          ),
      processor(&isa_parser, /* isa */
                &cfg,        /* cfg */
                &sim,        /* sim */
                0,           /* id */
                true,        /* halt_on_reset */
                nullptr,     /* log_file_t */
                std::cerr    /* sout */
                ),
      custom() {
  /* initialize processor state. */
  LOG(INFO) << fmt::format("[spike]\t spike read isa string: {}",
                           env("COSIM_isa"));
  auto &csrmap = processor.get_state()->csrmap;
  processor.register_extension(&custom);
  processor.enable_log_commits();

  processor.reset();

  /* read elf into sim */
  uint64_t entry;
  auto elfpath = std::string(env("COSIM_elf"));

  if (std::string_view(env("COSIM_isa"), 4) == "rv32") {
    entry = load_elf<true>(elfpath, sim.mem, sim.memsize).entry;
  } else {
    entry = load_elf<false>(elfpath, sim.mem, sim.memsize).entry;

    CHECK_S(false) << fmt::format("rv64 support is not yet implemented.");
  }

  /* setup entrypoint */
  processor.get_state()->pc = reset_vector_addr;
  LOG(INFO) << fmt::format(
      "[spike]\t spike loaded elf file {}, entrypoint is 0x{:08X}.", elfpath,
      entry);

  /* setup reset vector: `J <entrypoint>` */

  /* format of the J-type instruction:
   * |   31    |30       21|   20    |19        12|11   7|6      0|
   * | imm[20] | imm[10:1] | imm[11] | imm[19:12] |  rd  | opcode |
   *  ^------------------------------------------^
   */
  *(uint32_t *)sim.addr_to_mem(reset_vector_addr) =
      0x6f | (((entry >> 20) & 1) << 31) | (((entry >> 1) & 0x3ff) << 21) |
      (((entry >> 11) & 1) << 20) | (((entry >> 12) & 0xff) << 12);
  LOG(INFO) << fmt::format("[spike]\t spike added reset vector at mem@{}.",
                           reset_vector_addr);
}

void Spike::mem_read(uint32_t addr, uint32_t *out) {
  *out = *(uint32_t *)sim.addr_to_mem(addr);
  LOG(INFO) << fmt::format("[spike]\t spike read memory at 0x{:08X}, responsed "
                           "with data 0x{:08X}.",
                           addr, *out);
}

void Spike::instruction_fetch(uint32_t pc, uint32_t *data) {
  auto spike_state = processor.get_state();
  reg_t spike_pc = spike_state->pc;
  auto spike_fetch = processor.get_mmu()->load_insn(spike_pc);
  uint32_t spike_raw_insn = spike_fetch.insn.bits();

//  CHECK_S(pc == spike_pc) << fmt::format(
//      "spike is fetching instruction from 0x{:08X} while rtl is "
//      "fetching from 0x{:08X}.",
//      spike_pc, pc);

  LOG(INFO) << fmt::format(
      "[spike]\t spike fetched 0x{:08X} ({}) from address 0x{:08X} and "
      "responsed to rtl.",
      spike_raw_insn,
      processor.get_disassembler()->disassemble(spike_fetch.insn), spike_pc);
  *data = spike_raw_insn;

  step(spike_fetch);
  LOG(INFO) << fmt::format(
      "[spike]\t spike executed it and logged uarch changes.");
}

static void commit_log_reset(processor_t *p) {
  p->get_state()->log_reg_write.clear();
  p->get_state()->log_mem_read.clear();
  p->get_state()->log_mem_write.clear();
}

void Spike::step(insn_fetch_t fetch) {
  auto state = processor.get_state();
  reg_t pc;

  do {
    commit_log_reset(&processor);
    pc = fetch.func(&processor, fetch.insn, state->pc);
    if (pc & 1) {
      // some weird Spike mechanics that we need to bypass.
      switch (pc) {
      case PC_SERIALIZE_BEFORE:
        state->serialized = true;
        break;
      case PC_SERIALIZE_AFTER:
        break;
      default:
        CHECK_S(false) << fmt::format("invalid pc (0x{:08X}).", pc);
      }
    } else
      break;
  } while (true);
  state->pc = pc;

  /* record all uarch changes. */
  log_reg_write_queue.push_back(state->log_reg_write);
  log_mem_read_queue.push_back(state->log_mem_read);
  log_mem_write_queue.push_back(state->log_mem_write);
}
