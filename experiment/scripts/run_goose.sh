#!/usr/bin/env bash
# run_goose.sh — Goose tool adapter.
# Usage: run_goose.sh <prompt_file> <work_dir> <model_ollama_id>
# Exit 0 on success, non-zero on failure or timeout.
set -uo pipefail

PROMPT_FILE="${1:?Usage: run_goose.sh <prompt_file> <work_dir> <model_ollama_id>}"
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

# Goose uses the OpenAI-compatible Ollama endpoint
export OPENAI_BASE_URL="$OLLAMA_HOST/v1"
export OPENAI_API_KEY="${OPENAI_API_KEY:-ollama}"

cd "$WORK_DIR"

exec timeout "$TOOL_TIMEOUT" goose run \
  --provider openai \
  --model "$MODEL_ID" \
  --with-extension computercontroller \
  -- "$(cat "$PROMPT_FILE")"
