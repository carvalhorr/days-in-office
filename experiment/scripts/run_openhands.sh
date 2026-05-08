#!/usr/bin/env bash
# run_openhands.sh — OpenHands tool adapter (headless mode).
# Usage: run_openhands.sh <prompt_file> <work_dir> <model_ollama_id>
# Exit 0 on success, non-zero on failure or timeout.
set -uo pipefail

PROMPT_FILE="${1:?Usage: run_openhands.sh <prompt_file> <work_dir> <model_ollama_id>}"
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

export LLM_BASE_URL="$OLLAMA_HOST/v1"
export LLM_MODEL="ollama/$MODEL_ID"
export LLM_TEMPERATURE="0.2"

exec timeout "$TOOL_TIMEOUT" python3 -m openhands.core.main \
  --task "$(cat "$PROMPT_FILE")" \
  --workspace-dir "$WORK_DIR" \
  -l "ollama/$MODEL_ID" \
  --llm-base-url "$OLLAMA_HOST/v1" \
  --no-browser-actions \
  --headless
