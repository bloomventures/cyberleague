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
        # node
        #pkgsLinux.nodejs
        # wasm
        #pkgsLinux.wasmtime
        # python
        #pkgsLinux.python3Minimal
      ];
    };
  };

  # JDK lives in the sidecar squashfs (not the initramfs) so it is
  # demand-paged from disk rather than loaded into RAM at boot.
  jdk = pkgsLinux.jdk_headless;
in
{ inherit rootfs jdk; }
