#!/usr/bin/env bash
# run_aider.sh — Aider tool adapter.
# Usage: run_aider.sh <prompt_file> <work_dir> <model_ollama_id>
# Exit 0 on success, non-zero on failure or timeout.
set -uo pipefail

PROMPT_FILE="${1:?Usage: run_aider.sh <prompt_file> <work_dir> <model_ollama_id>}"
WORK_DIR="${2:?}"
MODEL_ID="${3:?}"
TOOL_TIMEOUT="${TOOL_TIMEOUT:-1800}"
OLLAMA_HOST="${OLLAMA_HOST:-http://192.168.68.74:11434}"

if [[ ! -f "$PROMPT_FILE" ]]; then
  echo "ERROR: prompt file not found: $PROMPT_FILE" >&2
  exit 1
fi
if [[ ! -d "$WORK_DIR" ]]; then
  echo "ERROR: work dir not found: $WORK_DIR" >&2
  exit 1
fi

# Write a per-invocation model settings file to set temperature=0.2
SETTINGS_FILE=$(mktemp /tmp/aider_settings_XXXXXX.yml)
cat > "$SETTINGS_FILE" <<YAML
- name: "ollama/$MODEL_ID"
  extra_params:
    temperature: 0.2
YAML
trap "rm -f '$SETTINGS_FILE'" EXIT

cd "$WORK_DIR"

# aider uses OLLAMA_API_BASE (not --openai-api-base) for native Ollama models
export OLLAMA_API_BASE="$OLLAMA_HOST"

exec timeout "$TOOL_TIMEOUT" aider \
  --model "ollama/$MODEL_ID" \
  --no-auto-commits \
  --yes-always \
  --no-suggest-shell-commands \
  --model-settings-file "$SETTINGS_FILE" \
  --message "$(cat "$PROMPT_FILE")" \
  --read ARCHITECTURE.md \
  --read TASKS.md \
  --read CLAUDE.md
