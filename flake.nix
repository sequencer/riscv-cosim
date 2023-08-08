{
  description = "ecore";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }@inputs:
    let
      overlay = import ./overlay.nix;
    in
    flake-utils.lib.eachDefaultSystem
      (system:
        let
          pkgs = import nixpkgs { inherit system; overlays = [ overlay ]; };
          deps = with pkgs; [
            gnused
            coreutils
            gnumake
            gnugrep
            which
            parallel
            rv32-clang
            glibc_multi
            llvmForDev.bintools
            cmake
            libargs
            glog
            fmt
            (enableDebugging libspike)
            jsoncpp.dev
            ninja
            verilator
            zlib
            sv-lang
          ];

          mkLLVMShell = pkgs.mkShell.override { stdenv = pkgs.llvmForDev.stdenv; };
        in
        {
          legacyPackages = pkgs;
          devShells = {
            default = pkgs.mkShell {
              buildInputs = deps;
            };
          };
        }
      )
    // { inherit inputs; overlays.default = overlay; };
}
