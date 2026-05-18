#!/usr/bin/env bash
# check_claude_credit.sh — Verify the Claude API is reachable and not
# rate-limited / quota-exhausted before starting a run.
#
# Sends a minimal canary prompt ("reply with ok"), bounded by a tiny
# --max-budget-usd so even if something goes wrong we burn fractional cents.
#
# Exit codes:
#   0 — API reachable, "ok" response received (canary passed)
#   1 — generic error (network, auth, parse, or canary timeout)
#   2 — budget exceeded (canary cap was set too low; bug, not a real failure)
#   3 — rate-limited / quota exhausted (the case we care about)
#
# Usage:
#   check_claude_credit.sh [model_id]
#
# Env overrides:
#   CLAUDE_CANARY_PROMPT   default: "reply with ok"
#   CLAUDE_CANARY_BUDGET   default: 0.10 (USD)
#   CLAUDE_CANARY_TIMEOUT  default: 30 (seconds)
set -uo pipefail

MODEL_ID="${1:-claude-sonnet-4-6}"
CANARY_PROMPT="${CLAUDE_CANARY_PROMPT:-reply with ok}"
CANARY_BUDGET="${CLAUDE_CANARY_BUDGET:-0.10}"
CANARY_TIMEOUT="${CLAUDE_CANARY_TIMEOUT:-30}"

START=$(date +%s)
OUTPUT=$(timeout "$CANARY_TIMEOUT" claude \
  --print \
  --model "$MODEL_ID" \
  --no-session-persistence \
  --max-budget-usd "$CANARY_BUDGET" \
  "$CANARY_PROMPT" 2>&1)
RC=$?
ELAPSED=$(( $(date +%s) - START ))

# Categorise the result.
# Order matters: check budget-exceeded before rate-limit because the budget
# error message can contain words that pattern-match the rate-limit regex.
if [[ $RC -eq 124 ]]; then
  echo "  Claude canary: TIMEOUT after ${CANARY_TIMEOUT}s (api unreachable or extremely slow)" >&2
  exit 1
fi

if echo "$OUTPUT" | grep -qiE "exceeded.*budget|budget.*exceeded"; then
  echo "  Claude canary: BUDGET EXCEEDED ($CANARY_BUDGET USD canary cap was too low)" >&2
  echo "  $OUTPUT" >&2
  exit 2
fi

if echo "$OUTPUT" | grep -qiE "rate.?limit|429|quota|usage limit|usage_limit|overloaded"; then
  echo "  Claude canary: RATE LIMITED / quota exhausted" >&2
  echo "  Response from Claude:" >&2
  echo "  $OUTPUT" | head -10 | sed 's/^/    /' >&2
  exit 3
fi

# Success path: rc=0 and the response starts with (or contains) "ok"
if [[ $RC -eq 0 ]] && echo "$OUTPUT" | grep -qiE "(^|[[:space:]])ok([[:space:]]|$|\.|,)"; then
  echo "  Claude canary: OK (model=$MODEL_ID, ${ELAPSED}s)"
  exit 0
fi

# Anything else: generic error. Surface the output so the operator can diagnose.
echo "  Claude canary: ERROR (rc=$RC, ${ELAPSED}s)" >&2
echo "  Response from Claude:" >&2
echo "$OUTPUT" | head -10 | sed 's/^/    /' >&2
exit 1
