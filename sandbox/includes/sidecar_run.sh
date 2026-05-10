#!/bin/sh

# JDK lives in the sidecar; /mnt/sidecar/jdk is a relative symlink to the
# Nix store path captured at sidecar build time.
JAVA_HOME=/mnt/sidecar/jdk
export PATH="$JAVA_HOME/bin:$PATH"
# lib/server contains libjvm.so in JDK 11+
export LD_LIBRARY_PATH="$JAVA_HOME/lib:$JAVA_HOME/lib/server${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"

exec /mnt/sidecar/relay
