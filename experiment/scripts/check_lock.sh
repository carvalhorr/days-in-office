#!/usr/bin/env bash
# check_lock.sh — Print the GPU lock status.
# Exit 0: no lock. Exit 1: live lock. Exit 2: stale lock.
set -euo pipefail

ROOT_DIR="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
LOCK_FILE="$ROOT_DIR/experiment/.lock"

if [[ ! -f "$LOCK_FILE" ]]; then
  echo "No run active."
  exit 0
fi

LOCK_PID=$(grep "^pid=" "$LOCK_FILE" | cut -d= -f2)

if kill -0 "$LOCK_PID" 2>/dev/null; then
  echo "ACTIVE LOCK (PID $LOCK_PID is running):"
  cat "$LOCK_FILE"
  exit 1
else
  echo "STALE LOCK (PID $LOCK_PID is not running):"
  cat "$LOCK_FILE"
  exit 2
fi
