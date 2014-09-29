#!/bin/bash

sudo ipfw flush

PORT="$1"
if [[ -n "${PORT}" ]]; then
  echo "Forwarding :80 to :${PORT}"
  sudo ipfw add 100 fwd 127.0.0.1,${PORT} tcp from any to any 80 in
fi

