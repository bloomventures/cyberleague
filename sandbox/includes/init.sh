#!/bin/sh

# Mount sidecar drive
mkdir -p /mnt/sidecar
mount -t squashfs /dev/vdb /mnt/sidecar

exec /mnt/sidecar/sidecar_run.sh

# reboot -f

