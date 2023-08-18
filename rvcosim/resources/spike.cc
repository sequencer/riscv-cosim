#include "spike.h"
#include "elfloader.h"
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
      ) {
  /* initialize processor state. */
  LOG(INFO) << fmt::format("[spike]\t spike read isa string: {}",
                           env("COSIM_isa"));
  auto &csrmap = processor.get_state()->csrmap;
  csrmap[CSR_MSIMEND] =
      std::make_shared<basic_csr_t>(&processor, CSR_MSIMEND, 0);
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

  /* Format of the J-type instruction:
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
  /* FIXME: assert on addr. */
  *out = *(uint32_t *)sim.addr_to_mem(addr);
}
