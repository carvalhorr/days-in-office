#!/usr/bin/env bash
# run_aider.sh — Aider tool adapter.
# Usage: run_aider.sh <prompt_file> <work_dir> <model_ollama_id> [scope_file]
# Exit 0 on success, non-zero on failure or timeout.
set -uo pipefail

PROMPT_FILE="${1:?Usage: run_aider.sh <prompt_file> <work_dir> <model_ollama_id> [scope_file]}"
WORK_DIR="${2:?}"
MODEL_ID="${3:?}"
SCOPE_FILE="${4:-}"
TOOL_TIMEOUT="${TOOL_TIMEOUT:-3600}"
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

# Build command as array so file paths with spaces are safe
CMD=(timeout "$TOOL_TIMEOUT" aider
  --model "ollama/$MODEL_ID"
  --edit-format diff
  --timeout "$TOOL_TIMEOUT"
  --no-auto-commits
  --yes-always
  --no-suggest-shell-commands
  --model-settings-file "$SETTINGS_FILE"
  --message "$(cat "$PROMPT_FILE")"
  --read ARCHITECTURE.md
  --read TASKS.md
  --read CLAUDE.md)

# Pre-seeded files are read-only references when present
[ -f settings.gradle.kts ]       && CMD+=(--read settings.gradle.kts)
[ -f build.gradle.kts ]          && CMD+=(--read build.gradle.kts)
[ -f app/build.gradle.kts ]      && CMD+=(--read app/build.gradle.kts)
[ -f gradle/libs.versions.toml ] && CMD+=(--read gradle/libs.versions.toml)
[ -f gradle.properties ]         && CMD+=(--read gradle.properties)

# Add existing in-scope files as editable context so the model sees their exact
# current content — this prevents SEARCH/REPLACE blocks from failing to match.
if [[ -n "$SCOPE_FILE" && -f "$SCOPE_FILE" ]]; then
  while IFS= read -r filepath; do
    [[ -z "$filepath" ]] && continue
    [[ -f "$filepath" ]] && CMD+=(--file "$filepath")
  done < "$SCOPE_FILE"
fi

exec "${CMD[@]}"
