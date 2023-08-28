#pragma once

#include <fstream>
#include <linux/elf.h>

#include "elfloader.h"
#include "glog.h"

struct ElfResult {
  uint64_t entry;
};

template <bool S> static inline int elfclass();
template <> inline int elfclass<true>() { return ELFCLASS32; }
template <> inline int elfclass<false>() { return ELFCLASS64; }

/* A naive elf loader. */
template <bool S> ElfResult load_elf(const std::string &filename, char *mem, uint64_t memsize) {
  typedef typename std::conditional<S, Elf32_Ehdr, Elf64_Ehdr>::type elf_ehdr_t;
  typedef typename std::conditional<S, Elf32_Phdr, Elf64_Phdr>::type elf_phdr_t;
  std::ifstream fs(filename, std::ios::binary);
  fs.exceptions(std::ios::failbit);

  elf_ehdr_t ehdr;
  fs.read(reinterpret_cast<char *>(&ehdr), sizeof(ehdr));
  ASSERT(ehdr.e_machine == EM_RISCV && ehdr.e_type == ET_EXEC &&
         ehdr.e_ident[EI_CLASS] == elfclass<S>());
  ASSERT(ehdr.e_phentsize == sizeof(elf_phdr_t));

  for (size_t i = 0; i < ehdr.e_phnum; i++) {
    auto phdr_offset = ehdr.e_phoff + i * ehdr.e_phentsize;
    elf_phdr_t phdr;
    fs.seekg((long)phdr_offset).read(reinterpret_cast<char *>(&phdr), sizeof(phdr));
    if (phdr.p_type == PT_LOAD) {
      ASSERT(phdr.p_paddr + phdr.p_filesz < memsize);
      fs.seekg((long)phdr.p_offset)
          .read(reinterpret_cast<char *>(&mem[phdr.p_paddr]), phdr.p_filesz);
    }
  }
  return {.entry = ehdr.e_entry};
}
