

build:
	nix develop -c mill -i darkriscv

run:
	GLOG_logtostderr=1 COSIM_isa=rv32gc COSIM_elf=todo ./run/emulator/build/emulator

.PHONY: build run
