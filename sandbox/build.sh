#!/usr/bin/env bash
set -euo pipefail

log() { echo "[$(date +"%H:%M:%S")] $*"; }

usage() {
  echo "Usage: $0 <step> [step...]"
  echo "Steps: all nix podman ext4 sidecar vmlinux"
  echo "  all     — full build with a fresh timestamped build dir"
  echo "  nix     — nix-build rootfs (requires: nothing)"
  echo "  podman  — load image, export rootfs.tar (requires: nix)"
  echo "  ext4    — extract tar, build rootfs.img (requires: podman)"
  echo "  sidecar — build sidecar.sqsh (requires: nothing)"
  echo "  vmlinux — fetch vmlinux kernel from firecracker CI (if not present)"
  exit 1
}

[[ $# -eq 0 ]] && usage

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INCLUDES_DIR="$SCRIPT_DIR/includes"
OUT_DIR="$SCRIPT_DIR/out"
NIX_FILE="$INCLUDES_DIR/cyberleague.nix"
RELAY_X86="$INCLUDES_DIR/relay-x86"
INIT_SH="$INCLUDES_DIR/init.sh"
SIDECAR_RUN_SH="$INCLUDES_DIR/sidecar_run.sh"

# --- preflight ---

preflight() {
  log "Checking required commands..."
  for cmd in nix-build podman mke2fs fakeroot; do
    if ! command -v "$cmd" &>/dev/null; then
      echo "Error: required command '$cmd' not found" >&2
      exit 1
    fi
    log "  $cmd: ok"
  done

  log "Sourcing nix profile..."
  . /etc/profile.d/nix.sh

  log "Checking required files..."
  for f in "$NIX_FILE" "$RELAY_X86" "$INIT_SH" "$SIDECAR_RUN_SH"; do
    if [[ ! -f "$f" ]]; then
      echo "Error: required file '$f' not found" >&2
      exit 1
    fi
    log "  $f: ok"
  done
}

# --- resolve build dir for partial runs ---

resolve_build_dir() {
  if [[ ! -L "$OUT_DIR/rootfs.img" ]]; then
    echo "Error: $OUT_DIR/rootfs.img symlink not found — run 'all' first or set up a build dir" >&2
    exit 1
  fi
  BUILD_DIR="$(readlink "$OUT_DIR/rootfs.img")"
  if [[ ! -d "$BUILD_DIR" ]]; then
    echo "Error: $OUT_DIR/rootfs.img points to '$BUILD_DIR' which does not exist" >&2
    exit 1
  fi
  log "Using build dir: $BUILD_DIR"
}

# --- steps ---

step_vmlinux() {
  local out="$OUT_DIR/vmlinux"
  if [[ -f "$out" ]]; then
    log "vmlinux already exists, skipping fetch."
    return
  fi
  mkdir -p "$OUT_DIR"
  log "Fetching vmlinux kernel from firecracker CI..."
  local ARCH release_url latest_version CI_VERSION latest_kernel_key
  ARCH="$(uname -m)"
  release_url="https://github.com/firecracker-microvm/firecracker/releases"
  latest_version=$(basename "$(curl -fsSLI -o /dev/null -w "%{url_effective}" "${release_url}/latest")")
  CI_VERSION="${latest_version%.*}"
  latest_kernel_key=$(curl "http://spec.ccfc.min.s3.amazonaws.com/?prefix=firecracker-ci/$CI_VERSION/$ARCH/vmlinux-&list-type=2" \
    | grep -oP "(?<=<Key>)(firecracker-ci/$CI_VERSION/$ARCH/vmlinux-[0-9]+\.[0-9]+\.[0-9]{1,3})(?=</Key>)" \
    | sort -V | tail -1)
  wget "https://s3.amazonaws.com/spec.ccfc.min/${latest_kernel_key}" -O "$out"
  log "vmlinux written to $out"
}

step_nix() {
  log "Building rootfs via nix-build..."
  pushd "$BUILD_DIR" > /dev/null
  nix-build "cyberleague.nix" -A rootfs -o result-rootfs
  popd > /dev/null
}

step_podman() {
  log "Cleaning podman storage at $HOME/.local/share/containers/storage..."
  podman unshare rm -rf "$HOME/.local/share/containers/storage"

  local build_id
  build_id="$(basename "$BUILD_DIR")"

  trap 'log "Cleaning up container $build_id..."; podman rm --ignore "$build_id" 2>/dev/null || true' EXIT

  log "Loading image into podman..."
  local load_out
  load_out=$(podman load < "$BUILD_DIR/result-rootfs" 2>&1)
  echo "podman load output: $load_out"

  local image
  image=$(echo "$load_out" | grep -oP "Loaded images?\([^)]*\)?:\s+\K\S+")
  if [[ -z "$image" ]]; then
    echo "Error: Could not parse image name from podman load output" >&2
    exit 1
  fi
  log "Loaded image: $image"

  log "Creating podman container $build_id from image..."
  podman create --name "$build_id" "$image" /bin/true
  mkdir -p "$BUILD_DIR/root_fs"

  log "Exporting container to tar (outside fakeroot)..."
  podman export "$build_id" > "$BUILD_DIR/rootfs.tar"
  trap - EXIT
}

step_ext4() {
  log "Extracting and building ext4 image (via fakeroot)..."
  fakeroot -- bash -c "
    set -euo pipefail
    echo '[fakeroot] Extracting $BUILD_DIR/rootfs.tar to $BUILD_DIR/root_fs...'
    tar xpf '$BUILD_DIR/rootfs.tar' -C '$BUILD_DIR/root_fs'

    echo '[fakeroot] Copying init.sh into rootfs...'
    cp '$INIT_SH' '$BUILD_DIR/root_fs/init.sh'
    chmod +x '$BUILD_DIR/root_fs/init.sh'

    ROOTFS_SIZE=\$(du -sm '$BUILD_DIR/root_fs' | cut -f1)
    echo '[fakeroot] Extracted rootfs size: '\${ROOTFS_SIZE}'MB'
    if (( ROOTFS_SIZE > 480 )); then
      echo \"Warning: rootfs is \${ROOTFS_SIZE}MB, which may not fit in a 512M image\" >&2
    fi

    echo '[fakeroot] Running mke2fs to create rootfs.img...'
    mke2fs -t ext4 -d '$BUILD_DIR/root_fs' '$BUILD_DIR/rootfs.img' 512M
    echo '[fakeroot] ext4 image created.'
  "

  rm -f "$BUILD_DIR/rootfs.tar"
}

step_sidecar() {
  log "Building sidecar squashfs..."
  local tmp_sidecar
  tmp_sidecar="$(mktemp -d)"
  cp "$RELAY_X86" "$tmp_sidecar/relay"
  chmod +x "$tmp_sidecar/relay"
  cp "$SIDECAR_RUN_SH" "$tmp_sidecar/sidecar_run.sh"
  chmod +x "$tmp_sidecar/sidecar_run.sh"
  mkdir -p "$OUT_DIR"
  mksquashfs "$tmp_sidecar" "$OUT_DIR/sidecar.sqsh" -noappend
  rm -rf "$tmp_sidecar"
}

# --- dispatch ---

STEPS=("$@")

if [[ "${STEPS[*]}" == *"all"* ]]; then
  preflight

  TS=$(date +"%Y%m%d-%H%M%S")
  BUILD_ID="rootfs-$TS"
  BUILD_DIR="$OUT_DIR/$BUILD_ID"

  log "Build ID: $BUILD_ID"
  log "Build dir: $BUILD_DIR"

  log "Cleaning up previous build dirs..."
  rm -rf "$OUT_DIR"/rootfs-*/

  log "Creating build dir and copying nix file..."
  mkdir -p "$BUILD_DIR"
  cp "$NIX_FILE" "$BUILD_DIR/"

  step_vmlinux
  step_nix
  step_podman
  step_ext4
  step_sidecar

  ln -sfn "$BUILD_DIR" "$OUT_DIR/rootfs.img"
  log "Done. Build artifacts in $BUILD_DIR (symlinked from $OUT_DIR/rootfs.img)"
  exit 0
fi

preflight

for step in "${STEPS[@]}"; do
  case "$step" in
    nix)
      resolve_build_dir
      cp "$NIX_FILE" "$BUILD_DIR/"
      step_nix
      ;;
    podman)
      resolve_build_dir
      step_podman
      ;;
    ext4)
      resolve_build_dir
      step_ext4
      ;;
    sidecar)
      step_sidecar
      ;;
    vmlinux)
      step_vmlinux
      ;;
    *)
      echo "Error: unknown step '$step'" >&2
      usage
      ;;
  esac
done

log "Done."
