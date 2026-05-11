#!/usr/bin/env bash
set -euo pipefail

rsync -avz build.sh cyberleague-eval-server:vm/
rsync -avz includes/ cyberleague-eval-server:vm/includes/
