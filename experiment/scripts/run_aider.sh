#!/usr/bin/env bash
# run_aider.sh — Aider tool adapter.
# Usage: run_aider.sh <prompt_file> <work_dir> <model_ollama_id>
# Exit 0 on success, non-zero on failure or timeout.
set -uo pipefail

PROMPT_FILE="${1:?Usage: run_aider.sh <prompt_file> <work_dir> <model_ollama_id>}"
WORK_DIR="${2:?}"
MODEL_ID="${3:?}"
TOOL_TIMEOUT="${TOOL_TIMEOUT:-600}"
OLLAMA_HOST="${OLLAMA_HOST:-http://192.168.68.74:11434}"

if [[ ! -f "$PROMPT_FILE" ]]; then
  echo "ERROR: prompt file not found: $PROMPT_FILE" >&2
  exit 1
fi
if [[ ! -d "$WORK_DIR" ]]; then
  echo "ERROR: work dir not found: $WORK_DIR" >&2
  exit 1
fi

cd "$WORK_DIR"

exec timeout "$TOOL_TIMEOUT" aider \
  --model "ollama/$MODEL_ID" \
  --openai-api-base "$OLLAMA_HOST" \
  --no-auto-commits \
  --yes-always \
  --no-suggest-shell-commands \
  --set-env "AIDER_MODEL_SETTINGS_temperature=0.2" \
  --message "$(cat "$PROMPT_FILE")" \
  --read ARCHITECTURE.md \
  --read TASKS.md \
  --read CLAUDE.md
