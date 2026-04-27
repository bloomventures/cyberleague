#!/usr/bin/env bash
set -euo pipefail

rsync -avz build.sh scaleway:vm/
rsync -avz includes/ scaleway:vm/includes/
