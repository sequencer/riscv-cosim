picorv32: init
	nix develop -c mill -i picorv32

init:
	git submodule update --init

.PHONY: picorv32 init
