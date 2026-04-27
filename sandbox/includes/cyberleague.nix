{ pkgs ? import <nixpkgs> { }
  , pkgsLinux ? import <nixpkgs> { system = "x86_64-linux"; }
}:

let
  rootfs = pkgsLinux.dockerTools.buildImage {
    name = "cyberleague";
    tag = "latest";

    extraCommands = ''
      mkdir -p dev proc sys tmp
    '';

    copyToRoot = pkgsLinux.buildEnv {
      name = "root";
      paths = [
        pkgsLinux.busybox
        #pkgsLinux.coreutils
        # rust
        #pkgsLinux.stdenv.cc.cc.lib
        #pkgsLinux.musl
        # java, clojure(uberjar) — java binary added via extraCommands wrapper
        pkgsLinux.jre_minimal
        # node
        #pkgsLinux.nodejs
        # wasm
        #pkgsLinux.wasmtime
        # python
        #pkgsLinux.python3Minimal
      ];
    };
  };
in
{ inherit rootfs; }
