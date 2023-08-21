run: build smoke
	GLOG_logtostderr=1 COSIM_isa=rv32i COSIM_elf=run/smoke.elf COSIM_wave=run/wave.fst ./run/emulator/build/emulator

build: init
	nix develop -c mill -i darkriscv

smoke:
	nix develop -c clang-rv32 -mabi=ilp32 -march=rv32i -mno-relax -static -mcmodel=medany -fvisibility=hidden -nostdlib -Wl,--entry=start -fno-PIC tests/asm/smoke.S -o run/smoke.elf

init:
	git submodule update --init

.PHONY: build run smoke init
