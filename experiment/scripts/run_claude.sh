#!/usr/bin/env bash
# run_claude.sh — Claude Code tool adapter.
# Usage: run_claude.sh <prompt_file> <work_dir> <model_id> [scope_file]
#
# Uses Claude Code in non-interactive (`-p`) mode, logged in via the user's
# existing Claude Code subscription (no ANTHROPIC_API_KEY required).
#
# Side-effects (when ATTEMPT_USAGE_DIR + ATTEMPT_NUMBER are set):
#   - writes $ATTEMPT_USAGE_DIR/attempt${N}_session.json   (raw final-result JSON)
#   - writes $ATTEMPT_USAGE_DIR/attempt${N}_usage.json     (parsed cost/tokens)
#
# Exit 0 on success, non-zero on failure or timeout.
set -uo pipefail

PROMPT_FILE="${1:?Usage: run_claude.sh <prompt_file> <work_dir> <model_id> [scope_file]}"
WORK_DIR="${2:?}"
MODEL_ID="${3:?}"
SCOPE_FILE="${4:-}"   # unused — Claude Code discovers files itself
TOOL_TIMEOUT="${TOOL_TIMEOUT:-7200}"

if [[ ! -f "$PROMPT_FILE" ]]; then
  echo "ERROR: prompt file not found: $PROMPT_FILE" >&2
  exit 1
fi
if [[ ! -d "$WORK_DIR" ]]; then
  echo "ERROR: work dir not found: $WORK_DIR" >&2
  exit 1
fi

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"

cd "$WORK_DIR"

# Capture the final-result JSON to a sidecar file so cost/usage can be parsed
# after the run. --output-format json emits one JSON object with the result,
# session id, total_cost_usd, and usage breakdown.
SESSION_FILE=$(mktemp /tmp/claude_session_XXXXXX.json)
trap "rm -f '$SESSION_FILE'" EXIT

set +e
timeout "$TOOL_TIMEOUT" claude \
  --model "$MODEL_ID" \
  --output-format json \
  --dangerously-skip-permissions \
  -p "$(cat "$PROMPT_FILE")" \
  > "$SESSION_FILE"
EXIT_CODE=$?
set -e

# Always persist the session JSON + parsed usage, even on failure (helps diagnose).
if [[ -n "${ATTEMPT_USAGE_DIR:-}" && -n "${ATTEMPT_NUMBER:-}" ]]; then
  mkdir -p "$ATTEMPT_USAGE_DIR"
  cp "$SESSION_FILE" "$ATTEMPT_USAGE_DIR/attempt${ATTEMPT_NUMBER}_session.json" 2>/dev/null || true
  python3 "$SCRIPTS_DIR/parse_claude_session.py" \
    "$SESSION_FILE" \
    "$ATTEMPT_USAGE_DIR/attempt${ATTEMPT_NUMBER}_usage.json" 2>/dev/null || true
fi

exit "$EXIT_CODE"
