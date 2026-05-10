#!/bin/sh

# Populate /dev with kernel device nodes (needed for /dev/vda, etc.)
mount -t devtmpfs devtmpfs /dev

# Mount sidecar drive
mkdir -p /mnt/sidecar
mount -t squashfs /dev/vda /mnt/sidecar

exec /mnt/sidecar/sidecar_run.sh

# reboot -f

