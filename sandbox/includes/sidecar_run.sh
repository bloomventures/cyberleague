#!/bin/sh

# JDK lives in the sidecar; /mnt/sidecar/jdk is a relative symlink to the
# Nix store path captured at sidecar build time.
JAVA_HOME=/mnt/sidecar/jdk
export PATH="$JAVA_HOME/bin:$PATH"
# lib/server contains libjvm.so; lib/jli contains libjli.so — both needed in JDK 11+
export LD_LIBRARY_PATH="$JAVA_HOME/lib:$JAVA_HOME/lib/server:$JAVA_HOME/lib/jli${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"

# Pull in every directory under the Nix store that contains shared libraries.
while IFS= read -r _d; do
  LD_LIBRARY_PATH="$_d:$LD_LIBRARY_PATH"
done <<EOF
$(find /mnt/sidecar/nix/store -name "*.so" -o -name "*.so.*" 2>/dev/null | sed 's|/[^/]*$||' | sort -u)
EOF
export LD_LIBRARY_PATH

exec /mnt/sidecar/relay
