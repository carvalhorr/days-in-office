#!/usr/bin/env bash
# reset_run.sh — Delete a run directory and its results. Requires --confirm.
# Usage: reset_run.sh --confirm <tool> <model_short_name>
set -euo pipefail

ROOT_DIR="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"

if [[ "${1:-}" != "--confirm" ]]; then
  echo "Usage: reset_run.sh --confirm <tool> <model_short_name>"
  echo "This deletes runs/<tool>/<model>/ and experiment/results/<tool>/<model>/"
  exit 1
fi

TOOL="${2:?Missing tool argument}"
MODEL="${3:?Missing model_short_name argument}"

RUN_DIR="$ROOT_DIR/runs/$TOOL/$MODEL"
RESULTS_DIR="$ROOT_DIR/experiment/results/$TOOL/$MODEL"

# Check for active lock on this run
LOCK_FILE="$ROOT_DIR/experiment/.lock"
if [[ -f "$LOCK_FILE" ]]; then
  LOCK_TOOL=$(grep "^tool=" "$LOCK_FILE" | cut -d= -f2 || echo "")
  LOCK_MODEL=$(grep "^model=" "$LOCK_FILE" | cut -d= -f2 || echo "")
  LOCK_PID=$(grep "^pid=" "$LOCK_FILE" | cut -d= -f2 || echo "")
  if [[ "$LOCK_TOOL" == "$TOOL" && "$LOCK_MODEL" == "$MODEL" ]] && kill -0 "$LOCK_PID" 2>/dev/null; then
    echo "ERROR: run $TOOL/$MODEL is currently active (PID $LOCK_PID). Stop it first."
    exit 1
  fi
fi

DELETED=0
if [[ -d "$RUN_DIR" ]]; then
  rm -rf "$RUN_DIR"
  echo "Deleted $RUN_DIR"
  DELETED=1
else
  echo "  (no run directory at $RUN_DIR)"
fi

if [[ -d "$RESULTS_DIR" ]]; then
  rm -rf "$RESULTS_DIR"
  echo "Deleted $RESULTS_DIR"
  DELETED=1
else
  echo "  (no results directory at $RESULTS_DIR)"
fi

if [[ $DELETED -eq 0 ]]; then
  echo "Nothing to delete for $TOOL/$MODEL."
else
  echo "Reset complete. Re-run setup_run.sh $TOOL $MODEL to start fresh."
fi
