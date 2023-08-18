run: build smoke
	GLOG_logtostderr=1 COSIM_isa=rv32gc COSIM_elf=run/smoke.elf ./run/emulator/build/emulator

build:
	nix develop -c mill -i darkriscv

smoke:
	nix develop -c clang-rv32 -mabi=ilp32f -march=rv32gc -mno-relax -static -mcmodel=medany -fvisibility=hidden -nostdlib -Wl,--entry=start -fno-PIC tests/asm/smoke.S -o run/smoke.elf

.PHONY: build run smoke
