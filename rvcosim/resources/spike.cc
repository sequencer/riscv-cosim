#include "spike.h"
#include "elfloader.h"
#include "utils.h"

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
  /* Initialize processor state, read elf into sim, setup entrypoint. */
  LOG(INFO) << fmt::format("[spike] spike read isa string: {}",
                           env("COSIM_isa"));
  auto &csrmap = processor.get_state()->csrmap;
  csrmap[CSR_MSIMEND] =
      std::make_shared<basic_csr_t>(&processor, CSR_MSIMEND, 0);
  processor.enable_log_commits();

  processor.reset();
  uint64_t entry;
  auto elfpath = std::string(env("COSIM_elf"));
  LOG(INFO) << fmt::format("[spike] spike is loading elf file: {}", elfpath);

  if (std::string_view(env("COSIM_isa"), 4) == "rv32") {
    entry = load_elf<true>(elfpath, sim.mem, sim.memsize).entry;
  } else {
    entry = load_elf<false>(elfpath, sim.mem, sim.memsize).entry;
  }
  processor.get_state()->pc = entry;

  LOG(INFO) << fmt::format(
      "[spike] spike loaded elf file, entrypoint is 0x{:08X}", entry);
}
