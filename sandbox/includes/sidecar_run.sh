#!/bin/sh

# Resolve JRE lib path from the java symlink so LD_LIBRARY_PATH works
# regardless of which Nix store hash the current build produced
JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")
export LD_LIBRARY_PATH="$JAVA_HOME/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"

exec /mnt/sidecar/relay
