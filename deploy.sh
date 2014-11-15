#!/bin/bash

set -euo pipefail

set +e
if ! git diff-files --quiet --ignore-submodules ; then
  echo "Uncommited changes; stash or commit before deploying"
  exit 1
fi
if ! git diff-index --cached --quiet HEAD --ignore-submodules ; then
  echo "Staged but uncommited changes; commit before deploying"
  exit 2
fi
set -e

DATE=$(date +"%Y-%m-%d_%H%M%S")
PROJECT_NAME="cyberleague"
lein uberjar
scp target/${PROJECT_NAME}-standalone.jar cyberleague:/www/${PROJECT_NAME}/${PROJECT_NAME}-${DATE}.jar
ssh cyberleague "cd /www/${PROJECT_NAME}/ && ./run.sh '${PROJECT_NAME}-${DATE}.jar'"
git tag "${DATE}"
