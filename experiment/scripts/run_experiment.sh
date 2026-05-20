#!/usr/bin/env bash
# run_experiment.sh — Main orchestrator. Drives one tool+model through all 20 tasks.
# Usage: run_experiment.sh <tool> <model_short_name> <model_ollama_id>
#
# Resume semantics: re-running after a pause or partial completion resumes from
# the first task that is NOT DONE and NOT FAILED in the run dir's TASKS.md.
#
# Oversight: creates experiment/run_state.json after each task.
#            Creates experiment/PAUSE check before each task — if present, pauses.
set -euo pipefail

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(git -C "$SCRIPTS_DIR" rev-parse --show-toplevel)"
OLLAMA_HOST="${OLLAMA_HOST:-http://192.168.68.74:11434}"

# Ensure JDK 17 and Android SDK are set for Gradle builds
export JAVA_HOME="${JAVA_HOME:-/usr/local/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="/usr/local/opt/coreutils/libexec/gnubin:$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
export OPENAI_API_KEY="${OPENAI_API_KEY:-ollama}"
export OPENAI_BASE_URL="${OPENAI_BASE_URL:-http://192.168.68.74:11434/v1}"
# Timeouts — calibrated for RTX 3090 Ti at ~5-10 tok/s; override via env var before running
export TOOL_TIMEOUT="${TOOL_TIMEOUT:-7200}"   # 2h per tool invocation
export QA_TIMEOUT="${QA_TIMEOUT:-1800}"       # 30 min per QA command (covers cold Gradle builds)

TOOL="${1:?Usage: run_experiment.sh <tool> <model_short_name> <model_ollama_id>}"
MODEL="${2:?}"
MODEL_OLLAMA_ID="${3:?}"

# ── Paths ───────────────────────────────────────────────────────────────────
RUN_DIR="$ROOT_DIR/runs/$TOOL/$MODEL"
RESULTS_DIR="$RUN_DIR/experiment"
RUN_JSON="$RESULTS_DIR/run.json"
RUN_LOG="$RESULTS_DIR/run.log"
BRANCH="run/$TOOL/$MODEL"
LOCK_FILE="$ROOT_DIR/experiment/.lock"
PAUSE_FILE="$ROOT_DIR/experiment/PAUSE"
STATE_FILE="$ROOT_DIR/experiment/run_state.json"
ARCH_FILE="$ROOT_DIR/ARCHITECTURE.md"
TEMPLATES_DIR="$ROOT_DIR/experiment/templates"
TASK_PROMPT="$TEMPLATES_DIR/task_prompt.txt"
RETRY_PROMPT="$TEMPLATES_DIR/retry_prompt.txt"

MAX_ATTEMPTS=10
LINT_MILESTONES="TASK-001 TASK-005 TASK-010 TASK-015 TASK-020"

# ── Validate setup ──────────────────────────────────────────────────────────
if [[ ! -d "$RUN_DIR" ]]; then
  echo "ERROR: $RUN_DIR not found. Run setup_run.sh $TOOL $MODEL first."
  exit 1
fi
mkdir -p "$RESULTS_DIR"

# ── GPU Lock ────────────────────────────────────────────────────────────────
acquire_lock() {
  if [[ -f "$LOCK_FILE" ]]; then
    LOCK_PID=$(grep "^pid=" "$LOCK_FILE" | cut -d= -f2)
    if kill -0 "$LOCK_PID" 2>/dev/null; then
      echo "ERROR: Another run is active (PID $LOCK_PID). See $LOCK_FILE."
      cat "$LOCK_FILE"
      exit 1
    else
      echo "WARNING: Stale lock (PID $LOCK_PID). Removing."
      rm -f "$LOCK_FILE"
    fi
  fi
  printf "tool=%s\nmodel=%s\npid=%s\nstarted=%s\n" \
    "$TOOL" "$MODEL" "$$" "$(date +%Y-%m-%dT%H:%M:%S)" > "$LOCK_FILE"
}

release_lock() {
  rm -f "$LOCK_FILE"
}
trap release_lock EXIT INT TERM

# wait_for_claude_credit: poll the canary until it returns OK (0) or we hit
# the max-wait cap. Returns 0 on credit restored, non-zero on cap reached.
# Used by the per-attempt rate-limit handler in the task loop so that a
# rate-limit window doesn't burn the orchestrator's retry budget.
wait_for_claude_credit() {
  local interval="${CLAUDE_RATELIMIT_POLL_INTERVAL:-300}"   # 5 min between polls
  local max_wait="${CLAUDE_RATELIMIT_MAX_WAIT:-43200}"      # 12 h cap
  local start now waited canary_rc
  start=$(date +%s)
  while true; do
    sleep "$interval"
    now=$(date +%s)
    waited=$((now - start))
    if (( waited >= max_wait )); then
      echo "  ERROR: still rate-limited after ${max_wait}s (${max_wait}/3600 = $((max_wait/3600))h). Giving up." >&2
      return 1
    fi
    canary_rc=0
    bash "$SCRIPTS_DIR/check_claude_credit.sh" "$MODEL_OLLAMA_ID" >/dev/null 2>&1 || canary_rc=$?
    if [[ $canary_rc -eq 0 ]]; then
      echo "  Credit restored after $((waited/60))m $((waited%60))s."
      return 0
    fi
    if [[ $canary_rc -ne 3 ]]; then
      # Non-rate-limit canary failure (network, auth, etc.) — surface it.
      echo "  WARN: canary returned non-rate-limit error (rc=$canary_rc) during wait. Treating as transient and continuing to poll." >&2
    fi
    echo "  Still rate-limited at $(date '+%H:%M:%S') (waited $((waited/60))m, next check in ${interval}s)"
  done
}

acquire_lock

# Pre-flight: for cloud tools, verify the API is reachable and not rate-limited
# before launching the task loop. Twice in this experiment a run has burned ~80
# seconds of TOOL_ERROR retries on the first task because Claude's rate window
# was exhausted; the canary catches that in ~5 s with a clean error.
if [[ "$TOOL" == "claude" ]]; then
  echo "  Pre-flight: Claude credit canary ..."
  if ! bash "$SCRIPTS_DIR/check_claude_credit.sh" "$MODEL_OLLAMA_ID"; then
    CANARY_RC=$?
    echo "" >&2
    echo "ERROR: Claude credit canary failed (rc=$CANARY_RC)." >&2
    case $CANARY_RC in
      3) echo "  Rate-limited or quota exhausted. Wait for the rate window to reset, then re-run." >&2 ;;
      2) echo "  Canary budget too low — bug, raise CLAUDE_CANARY_BUDGET and re-run." >&2 ;;
      *) echo "  Generic canary failure (network / auth / unexpected). See output above." >&2 ;;
    esac
    echo "" >&2
    echo "Aborting before any tasks are attempted to avoid wasting retry budget." >&2
    exit $CANARY_RC
  fi
fi


# ── Logging ─────────────────────────────────────────────────────────────────
exec > >(tee -a "$RUN_LOG") 2>&1
echo "════════════════════════════════════════════════════════"
echo "  run_experiment.sh  —  $TOOL × $MODEL"
echo "  Model: $MODEL_OLLAMA_ID"
echo "  Started: $(date +%Y-%m-%dT%H:%M:%S)"
echo "════════════════════════════════════════════════════════"

# ── Tool/version capture ────────────────────────────────────────────────────
OLLAMA_VER=$(curl -sf "$OLLAMA_HOST/api/version" | python3 -c "import sys,json; print(json.load(sys.stdin).get('version','unknown'))" 2>/dev/null || echo "unknown")

case "$TOOL" in
  aider)     TOOL_VERSION=$(aider --version 2>&1 | head -1 || echo "unknown") ;;
  openhands) TOOL_VERSION=$(python3 -m openhands.core.main --version 2>&1 | head -1 || echo "unknown") ;;
  goose)     TOOL_VERSION=$(goose --version 2>&1 | head -1 || echo "unknown") ;;
  claude)    TOOL_VERSION=$(claude --version 2>&1 | head -1 || echo "unknown") ;;
  *)         TOOL_VERSION="unknown" ;;
esac
echo "Tool version: $TOOL_VERSION"
echo "Ollama version: $OLLAMA_VER"

# ── Template hashes ──────────────────────────────────────────────────────────
TEMPLATE_HASHES=$(python3 -c "
import json, hashlib
from pathlib import Path
hashes = {}
for f in ['$TASK_PROMPT', '$RETRY_PROMPT']:
    p = Path(f)
    hashes[p.name] = hashlib.sha256(p.read_bytes()).hexdigest() if p.exists() else None
print(json.dumps(hashes))
")

# ── Initialise run.json (skip if resuming) ───────────────────────────────────
if [[ ! -f "$RUN_JSON" ]]; then
  python3 "$SCRIPTS_DIR/update_run_json.py" --init "$RUN_JSON" \
    --tool "$TOOL" \
    --model-id "$MODEL_OLLAMA_ID" \
    --model-short "$MODEL" \
    --tool-version "$TOOL_VERSION" \
    --ollama-version "$OLLAMA_VER" \
    --template-hashes "$TEMPLATE_HASHES"
else
  echo "Resuming existing run: $RUN_JSON"
fi

# ── State helper ─────────────────────────────────────────────────────────────
write_state() {
  local status="$1" current_task="${2:-}" msg="${3:-}"
  # Count statuses entirely in Python to avoid grep -c + || echo "0" double-output bug
  python3 - <<PYEOF
import json, re, sys
from pathlib import Path
from datetime import datetime

content = Path("$RUN_DIR/TASKS.md").read_text()
done   = len(re.findall(r'\*\*Status:\*\*\s+DONE',   content))
failed = len(re.findall(r'\*\*Status:\*\*\s+FAILED', content))
total  = len(re.findall(r'\*\*Status:\*\*',          content))

data = {
  "status": "$status",
  "tool": "$TOOL",
  "model_short": "$MODEL",
  "model_ollama_id": "$MODEL_OLLAMA_ID",
  "current_task": "$current_task",
  "current_task_start": "${TASK_START_TIME:-}",
  "tasks_done": done,
  "tasks_failed": failed,
  "tasks_total": total,
  "last_updated": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
  "intervention_message": "$msg",
  "pause_requested": Path("$PAUSE_FILE").exists(),
}
Path("$STATE_FILE").write_text(json.dumps(data, indent=2) + "\n")
PYEOF
}

# ── Get task IDs ────────────────────────────────────────────────────────────
TASK_IDS=$(python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --list)
echo "Tasks: $(echo "$TASK_IDS" | wc -l | tr -d ' ') found"

write_state "running" "" ""

# ── Main loop ────────────────────────────────────────────────────────────────
for TASK_ID in $TASK_IDS; do
  # Skip already-resolved tasks (support for resume)
  CURRENT_STATUS=$(python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --task "$TASK_ID" --field status)
  if [[ "$CURRENT_STATUS" == "DONE" || "$CURRENT_STATUS" == "FAILED" ]]; then
    echo "── $TASK_ID: already $CURRENT_STATUS, skipping ──"
    continue
  fi

  # Skip task if any dependency is not DONE
  TASK_DEPS=$(python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --task "$TASK_ID" --field dependencies 2>/dev/null || true)
  DEP_BLOCKED=0
  for DEP_ID in $TASK_DEPS; do
    DEP_STATUS=$(python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --task "$DEP_ID" --field status 2>/dev/null || echo "UNKNOWN")
    if [[ "$DEP_STATUS" != "DONE" ]]; then
      echo "── $TASK_ID: skipping — dependency $DEP_ID is $DEP_STATUS ──"
      DEP_BLOCKED=1
      break
    fi
  done
  [[ $DEP_BLOCKED -eq 1 ]] && continue

  # Check pause file before each task
  # TODO: also check inside the attempt loop so PAUSE can halt within minutes
  # instead of waiting up to 10 attempts of the current task — see RUNS_LOG.md.
  if [[ -f "$PAUSE_FILE" ]]; then
    echo ""
    echo "PAUSE FILE detected. Pausing before $TASK_ID."
    write_state "paused" "$TASK_ID" "Pause file found at experiment/PAUSE. Delete it and re-run to resume."
    echo "To resume: rm $PAUSE_FILE && bash $0 $TOOL $MODEL $MODEL_OLLAMA_ID"
    exit 0
  fi

  TASK_TITLE=$(python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --task "$TASK_ID" --field title)
  echo ""
  echo "════════ $TASK_ID: $TASK_TITLE ════════"

  # Build scope file list once per task — used by tool adapter and retry prompt
  SCOPE_FILE_LIST=$(mktemp /tmp/scope_XXXXXX.txt)
  python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --task "$TASK_ID" --field scope_files > "$SCOPE_FILE_LIST"
  SCOPE_FILES_TEXT=$(cat "$SCOPE_FILE_LIST")

  # Mark IN_PROGRESS
  python3 "$SCRIPTS_DIR/update_tasks_status.py" "$RUN_DIR/TASKS.md" "$TASK_ID" "IN_PROGRESS"
  export TASK_START_TIME
  TASK_START_TIME=$(date +%Y-%m-%dT%H:%M:%S)
  write_state "running" "$TASK_ID" ""

  TASK_START_SECS=$(date +%s)

  TASK_STATUS_FINAL="FAILED"
  ATTEMPTS=0
  FAILURE_REASONS=()
  ATTEMPT_LOG_JSON="[]"
  QA_RESULTS="[]"
  COMMIT_HASH=""
  ATTEMPT_HISTORY_TEXT=""

  for ATTEMPT in $(seq 1 $MAX_ATTEMPTS); do
    ATTEMPTS=$ATTEMPT
    ATTEMPT_START_TIME=$(date +%Y-%m-%dT%H:%M:%S)
    ATTEMPT_START_SECS=$(date +%s)
    echo "── Attempt $ATTEMPT of $MAX_ATTEMPTS ──"

    # Build prompt
    PROMPT_FILE=$(mktemp /tmp/prompt_XXXXXX.txt)
    if [[ $ATTEMPT -eq 1 ]]; then
      python3 "$SCRIPTS_DIR/render_prompt.py" \
        --template "$TASK_PROMPT" \
        --output "$PROMPT_FILE" \
        --var "TASK_ID=$TASK_ID" \
        --var "TASK_TITLE=$TASK_TITLE" \
        --var "TASK_BODY=$(python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --task "$TASK_ID" --field body)" \
        --var "ARCHITECTURE_SUMMARY=@$ARCH_FILE" \
        --var "ATTEMPT_NUMBER=$ATTEMPT"
    else
      python3 "$SCRIPTS_DIR/render_prompt.py" \
        --template "$RETRY_PROMPT" \
        --output "$PROMPT_FILE" \
        --var "TASK_ID=$TASK_ID" \
        --var "TASK_TITLE=$TASK_TITLE" \
        --var "ATTEMPT_NUMBER=$ATTEMPT" \
        --var "PREV_ATTEMPT_NUMBER=$((ATTEMPT - 1))" \
        --var "ATTEMPT_HISTORY=$ATTEMPT_HISTORY_TEXT" \
        --var "SCOPE_FILES=$SCOPE_FILES_TEXT" \
        --var "FAILURE_OUTPUT=$LAST_FAILURE_OUTPUT"
    fi

    # Save prompt for post-mortem review
    mkdir -p "$RESULTS_DIR/$TASK_ID"
    cp "$PROMPT_FILE" "$RESULTS_DIR/$TASK_ID/attempt${ATTEMPT}_prompt.txt"

    # Invoke tool adapter
    # Tool adapters may write attempt${N}_usage.json into $ATTEMPT_USAGE_DIR
    # (cost/token telemetry); merged into the attempt record below if present.
    export ATTEMPT_USAGE_DIR="$RESULTS_DIR/$TASK_ID"
    export ATTEMPT_NUMBER="$ATTEMPT"
    # Tool invocation wrapped in a credit-aware retry: if the tool errors AND
    # the canary confirms rate-limit exhaustion, wait for the rate window to
    # reset and re-invoke without counting this as a real failed attempt.
    # This handles the mid-run rate-limit-cascade pattern we saw twice in
    # earlier experiment runs (10 attempts × ~85s of TOOL_ERROR wasted before
    # the orchestrator gave up on a task it could have completed an hour later).
    while true; do
      echo "  Invoking $TOOL with model $MODEL_OLLAMA_ID ..."
      TOOL_EXIT=0
      bash "$SCRIPTS_DIR/run_${TOOL}.sh" "$PROMPT_FILE" "$RUN_DIR" "$MODEL_OLLAMA_ID" "$SCOPE_FILE_LIST" || TOOL_EXIT=$?
      # Only do the rate-limit check for claude, and only on non-zero non-timeout exits.
      if [[ "$TOOL" == "claude" && $TOOL_EXIT -ne 0 && $TOOL_EXIT -ne 124 ]]; then
        echo "  Tool errored (exit $TOOL_EXIT) — checking if API is rate-limited ..."
        CANARY_RC=0
        bash "$SCRIPTS_DIR/check_claude_credit.sh" "$MODEL_OLLAMA_ID" >/dev/null 2>&1 || CANARY_RC=$?
        if [[ $CANARY_RC -eq 3 ]]; then
          echo "  ⏸  Rate-limit detected. Pausing this attempt; will retry when credit returns."
          if wait_for_claude_credit; then
            echo "  ▶  Resuming attempt $ATTEMPT."
            continue   # re-invoke tool with same prompt; same ATTEMPT number
          else
            echo "  WARN: credit-wait exceeded the cap. Treating as a regular TOOL_ERROR." >&2
            break
          fi
        fi
      fi
      break   # success or non-rate-limit failure — fall through to normal handling
    done
    rm -f "$PROMPT_FILE"

    # Pick up tool-specific usage telemetry (Claude writes cost/tokens here).
    USAGE_FILE="$RESULTS_DIR/$TASK_ID/attempt${ATTEMPT}_usage.json"
    ATTEMPT_USAGE_JSON="{}"
    [[ -f "$USAGE_FILE" ]] && ATTEMPT_USAGE_JSON=$(cat "$USAGE_FILE")

    if [[ $TOOL_EXIT -ne 0 ]]; then
      if [[ $TOOL_EXIT -eq 124 ]]; then
        echo "  TIMEOUT after ${TOOL_TIMEOUT}s"
        CURRENT_OUTCOME="TIMEOUT"
        LAST_FAILURE_OUTPUT="Tool timed out after ${TOOL_TIMEOUT}s"
      else
        echo "  TOOL_ERROR (exit $TOOL_EXIT)"
        CURRENT_OUTCOME="TOOL_ERROR"
        LAST_FAILURE_OUTPUT="Tool exited with code $TOOL_EXIT"
      fi
      FAILURE_REASONS+=("$CURRENT_OUTCOME")
      echo "$LAST_FAILURE_OUTPUT" > "$RESULTS_DIR/$TASK_ID/attempt${ATTEMPT}_failure.txt"
      ATTEMPT_HISTORY_TEXT=$(python3 -c "
import sys
hist, attempt, outcome, brief = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4].replace('\n',' ')[:300]
print(hist + 'Attempt ' + attempt + ' (' + outcome + '): ' + brief + '\n', end='')
" "$ATTEMPT_HISTORY_TEXT" "$ATTEMPT" "$CURRENT_OUTCOME" "$LAST_FAILURE_OUTPUT")
      git -C "$RUN_DIR" add -A 2>/dev/null || true
      git -C "$RUN_DIR" diff --cached --quiet 2>/dev/null || \
        git -C "$RUN_DIR" commit -q -m "fix: retry $ATTEMPT for $TASK_ID — tool error (exit $TOOL_EXIT) (Started: $ATTEMPT_START_TIME, Ended: $(date +%Y-%m-%dT%H:%M:%S))"
      git -C "$ROOT_DIR" fetch "$RUN_DIR" HEAD:"$BRANCH" 2>/dev/null || true
      ATTEMPT_END_SECS=$(date +%s)
      ATTEMPT_LOG_JSON=$(python3 -c "
import json, sys
arr = json.loads(sys.argv[1])
rec = {'attempt':$ATTEMPT,'start_time':sys.argv[2],'end_time':sys.argv[3],'duration_seconds':$((ATTEMPT_END_SECS - ATTEMPT_START_SECS)),'outcome':sys.argv[4]}
usage = json.loads(sys.argv[5])
if usage: rec['usage'] = usage
arr.append(rec)
print(json.dumps(arr))" "$ATTEMPT_LOG_JSON" "$ATTEMPT_START_TIME" "$(date +%Y-%m-%dT%H:%M:%S)" "$CURRENT_OUTCOME" "$ATTEMPT_USAGE_JSON")
      continue
    fi

    # Run QA verification
    echo "  Running QA verification..."
    QA_COMMANDS=$(python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --task "$TASK_ID" --field qa_commands)
    ALL_PASSED=1
    QA_RESULTS="[]"
    LAST_FAILURE_OUTPUT=""

    while IFS= read -r CMD; do
      [[ -z "$CMD" ]] && continue
      echo "    QA: $CMD"
      QA_JSON=$(bash "$SCRIPTS_DIR/run_qa.sh" "$RUN_DIR" "$CMD")
      QA_RESULTS=$(echo "$QA_JSON" | python3 -c "
import json, sys
arr = json.loads(sys.argv[1])
arr.append(json.load(sys.stdin))
print(json.dumps(arr))" "$QA_RESULTS" 2>/dev/null || echo "$QA_RESULTS")
      PASSED=$(echo "$QA_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['passed'])")
      if [[ "$PASSED" != "True" ]]; then
        ALL_PASSED=0
        OUTPUT=$(echo "$QA_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['output'])")
        LAST_FAILURE_OUTPUT="${LAST_FAILURE_OUTPUT}CMD: $CMD\n$OUTPUT\n\n"
        echo "    ✗ FAILED"
      else
        echo "    ✓ passed"
      fi
    done <<< "$QA_COMMANDS"

    if [[ $ALL_PASSED -eq 1 ]]; then
      echo "  ✓ All QA checks passed on attempt $ATTEMPT"
      TASK_STATUS_FINAL="DONE"
      python3 "$SCRIPTS_DIR/update_tasks_status.py" "$RUN_DIR/TASKS.md" "$TASK_ID" "DONE"
      git -C "$RUN_DIR" add -A 2>/dev/null || true
      git -C "$RUN_DIR" diff --cached --quiet 2>/dev/null || \
        git -C "$RUN_DIR" commit -q -m "feat: complete $TASK_ID — $TASK_TITLE (Started: $ATTEMPT_START_TIME, Ended: $(date +%Y-%m-%dT%H:%M:%S))"
      git -C "$ROOT_DIR" fetch "$RUN_DIR" HEAD:"$BRANCH" 2>/dev/null || true
      COMMIT_HASH=$(git -C "$RUN_DIR" rev-parse HEAD 2>/dev/null || echo "")
      ATTEMPT_END_SECS=$(date +%s)
      ATTEMPT_LOG_JSON=$(python3 -c "
import json, sys
arr = json.loads(sys.argv[1])
rec = {'attempt':$ATTEMPT,'start_time':sys.argv[2],'end_time':sys.argv[3],'duration_seconds':$((ATTEMPT_END_SECS - ATTEMPT_START_SECS)),'outcome':'DONE'}
usage = json.loads(sys.argv[4])
if usage: rec['usage'] = usage
arr.append(rec)
print(json.dumps(arr))" "$ATTEMPT_LOG_JSON" "$ATTEMPT_START_TIME" "$(date +%Y-%m-%dT%H:%M:%S)" "$ATTEMPT_USAGE_JSON")
      break
    else
      echo "  ✗ QA failed on attempt $ATTEMPT"
      FAILURE_REASONS+=("QA_FAIL")
      LAST_FAILURE_OUTPUT=$(echo -e "$LAST_FAILURE_OUTPUT" | tail -c 4000)
      printf '%s' "$LAST_FAILURE_OUTPUT" > "$RESULTS_DIR/$TASK_ID/attempt${ATTEMPT}_failure.txt"
      ATTEMPT_HISTORY_TEXT=$(python3 -c "
import sys
hist, attempt, outcome, brief = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4].replace('\n',' ')[:300]
print(hist + 'Attempt ' + attempt + ' (' + outcome + '): ' + brief + '\n', end='')
" "$ATTEMPT_HISTORY_TEXT" "$ATTEMPT" "QA_FAIL" "$LAST_FAILURE_OUTPUT")
      git -C "$RUN_DIR" add -A 2>/dev/null || true
      git -C "$RUN_DIR" diff --cached --quiet 2>/dev/null || \
        git -C "$RUN_DIR" commit -q -m "fix: retry $ATTEMPT for $TASK_ID — QA failed (Started: $ATTEMPT_START_TIME, Ended: $(date +%Y-%m-%dT%H:%M:%S))"
      git -C "$ROOT_DIR" fetch "$RUN_DIR" HEAD:"$BRANCH" 2>/dev/null || true
      ATTEMPT_END_SECS=$(date +%s)
      ATTEMPT_LOG_JSON=$(python3 -c "
import json, sys
arr = json.loads(sys.argv[1])
rec = {'attempt':$ATTEMPT,'start_time':sys.argv[2],'end_time':sys.argv[3],'duration_seconds':$((ATTEMPT_END_SECS - ATTEMPT_START_SECS)),'outcome':'QA_FAIL'}
usage = json.loads(sys.argv[4])
if usage: rec['usage'] = usage
arr.append(rec)
print(json.dumps(arr))" "$ATTEMPT_LOG_JSON" "$ATTEMPT_START_TIME" "$(date +%Y-%m-%dT%H:%M:%S)" "$ATTEMPT_USAGE_JSON")
    fi
  done  # attempts

  if [[ "$TASK_STATUS_FINAL" == "FAILED" ]]; then
    python3 "$SCRIPTS_DIR/update_tasks_status.py" "$RUN_DIR/TASKS.md" "$TASK_ID" "FAILED"
  fi

  TASK_END_SECS=$(date +%s)
  TASK_DURATION=$(( TASK_END_SECS - TASK_START_SECS ))

  # Measure post-task metrics
  MEASURE_FLAGS="--build --tests"
  if echo "$LINT_MILESTONES" | grep -qw "$TASK_ID"; then
    MEASURE_FLAGS="$MEASURE_FLAGS --lint"
  fi
  METRICS_JSON=$(bash "$SCRIPTS_DIR/measure_metrics.sh" "$RUN_DIR" "$TASK_ID" $MEASURE_FLAGS 2>/dev/null \
    || echo '{"build_success":null,"unit_test_pass_count":null,"unit_test_fail_count":null,"lint_error_count":null}')

  # Files created by this task (from git)
  FILES_CREATED=$(git -C "$RUN_DIR" diff --name-only HEAD~1 HEAD 2>/dev/null | python3 -c "
import sys, json
files = [l.strip() for l in sys.stdin if l.strip()]
print(json.dumps(files))" || echo "[]")

  # Build task record JSON
  FAILURE_REASONS_JSON=$(python3 -c "import json, sys; print(json.dumps(sys.argv[1:]))" "${FAILURE_REASONS[@]+"${FAILURE_REASONS[@]}"}")
  TASK_RECORD=$(python3 -c "
import json, sys
metrics = json.loads(sys.argv[1])
qa = json.loads(sys.argv[2])
files = json.loads(sys.argv[3])
reasons = json.loads(sys.argv[4])
attempt_log = json.loads(sys.argv[5])
record = {
  'task_id': '$TASK_ID',
  'status': '$TASK_STATUS_FINAL',
  'start_time': '$TASK_START_TIME',
  'end_time': '$(date +%Y-%m-%dT%H:%M:%S)',
  'duration_seconds': $TASK_DURATION,
  'attempts': $ATTEMPTS,
  'attempt_log': attempt_log,
  'failure_reasons': reasons,
  'qa_results': qa,
  'build_success': metrics.get('build_success'),
  'lint_error_count': metrics.get('lint_error_count'),
  'unit_test_pass_count': metrics.get('unit_test_pass_count'),
  'unit_test_fail_count': metrics.get('unit_test_fail_count'),
  'files_created': files,
  'commit_hash': '$COMMIT_HASH' or None,
}
print(json.dumps(record))
" "$METRICS_JSON" "$QA_RESULTS" "$FILES_CREATED" "$FAILURE_REASONS_JSON" "$ATTEMPT_LOG_JSON")

  python3 "$SCRIPTS_DIR/update_run_json.py" --append-task "$RUN_JSON" --task-json "$TASK_RECORD"

  # Commit results artifacts to run repo and sync branch to main repo
  git -C "$RUN_DIR" add -A 2>/dev/null || true
  git -C "$RUN_DIR" diff --cached --quiet 2>/dev/null || \
    git -C "$RUN_DIR" commit -q -m "chore: results $TASK_ID — $TASK_STATUS_FINAL ($ATTEMPTS attempt(s))"
  git -C "$ROOT_DIR" fetch "$RUN_DIR" HEAD:"$BRANCH" 2>/dev/null || true

  rm -f "$SCOPE_FILE_LIST"

  STATUS_ICON="✓"
  [[ "$TASK_STATUS_FINAL" == "FAILED" ]] && STATUS_ICON="✗"
  echo "$STATUS_ICON $TASK_ID: $TASK_STATUS_FINAL (${TASK_DURATION}s, $ATTEMPTS attempt(s))"

  # ── Post-task UI smoke (REPORT-ONLY) ────────────────────────────────────
  # Runs after every DONE task regardless of whether the task's own qa_commands
  # invoked it. Outcome is appended to SMOKE_RESULTS.md and a banner is printed
  # to run.log if the failure set changed. Smoke results never gate task DONE.
  # Requires the AVD to be already booted (orchestrator does not boot one).
  if [[ "$TASK_STATUS_FINAL" == "DONE" ]]; then
    SMOKE_CMD="./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.carvalhorr.daysInOffice.smoke.ui.MainFlowSmokeTest"
    SMOKE_LOG_MD="$RUN_DIR/SMOKE_RESULTS.md"
    echo "  Running UI smoke (report-only)..."
    SMOKE_JSON=$(bash "$SCRIPTS_DIR/run_qa.sh" "$RUN_DIR" "$SMOKE_CMD" 2>/dev/null || echo '{"passed":false,"output":"smoke runner errored"}')
    SMOKE_PASSED=$(echo "$SMOKE_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin).get('passed', False))" 2>/dev/null || echo "False")
    SMOKE_OUTPUT=$(echo "$SMOKE_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin).get('output', ''))" 2>/dev/null || echo "")
    SMOKE_FAILS=$(printf '%s' "$SMOKE_OUTPUT" | grep -oE 'MainFlowSmokeTest > [A-Za-z0-9_]+' | awk '{print $NF}' | sort -u | paste -sd, - 2>/dev/null || echo "")
    SMOKE_TS=$(date +%Y-%m-%dT%H:%M:%S)
    if [[ "$SMOKE_PASSED" == "True" ]]; then
      echo "    ✓ Smoke: all passing"
      SMOKE_VERDICT="PASS"
      SMOKE_FAILS_DISPLAY="(none)"
    else
      SMOKE_VERDICT="FAIL"
      SMOKE_FAILS_DISPLAY="${SMOKE_FAILS:-(could not parse failures)}"
      echo "    ⚠ Smoke: $SMOKE_FAILS_DISPLAY"
      echo "  ════════════════════════════════════════════════════════"
      echo "  ⚠ POST-TASK SMOKE REGRESSION ALERT — $TASK_ID"
      echo "    Failing tests: $SMOKE_FAILS_DISPLAY"
      echo "    Cross-check against Known-Failing UI Smoke Tests in CLAUDE.md."
      echo "    Failures outside that list are NEW regressions caused by this task."
      echo "  ════════════════════════════════════════════════════════"
    fi
    # Append to SMOKE_RESULTS.md (create with header on first write)
    if [[ ! -f "$SMOKE_LOG_MD" ]]; then
      cat > "$SMOKE_LOG_MD" <<'MDHEADER'
# Post-Task UI Smoke Results

Recorded by `run_experiment.sh` after every task that reaches DONE.
**Report-only:** does not block task completion. Use it to spot regressions —
any failing test outside `Known-Failing UI Smoke Tests` (CLAUDE.md) is new.

| Task | Time (UTC-local) | Verdict | Failing tests |
|---|---|---|---|
MDHEADER
    fi
    echo "| $TASK_ID | $SMOKE_TS | $SMOKE_VERDICT | $SMOKE_FAILS_DISPLAY |" >> "$SMOKE_LOG_MD"
    # Commit the smoke log update so it lands in the run-repo's history.
    git -C "$RUN_DIR" add SMOKE_RESULTS.md 2>/dev/null || true
    git -C "$RUN_DIR" diff --cached --quiet 2>/dev/null || \
      git -C "$RUN_DIR" commit -q -m "chore: smoke $TASK_ID — $SMOKE_VERDICT" 2>/dev/null || true
    git -C "$ROOT_DIR" fetch "$RUN_DIR" HEAD:"$BRANCH" 2>/dev/null || true
  fi

  write_state "running" "$TASK_ID" ""
done  # tasks

# ── Finalise ─────────────────────────────────────────────────────────────────
python3 "$SCRIPTS_DIR/update_run_json.py" --finalise "$RUN_JSON"
write_state "complete" "" ""

git -C "$RUN_DIR" add -A 2>/dev/null || true
git -C "$RUN_DIR" diff --cached --quiet 2>/dev/null || \
  git -C "$RUN_DIR" commit -q -m "chore: finalise run $TOOL/$MODEL"
git -C "$ROOT_DIR" fetch "$RUN_DIR" HEAD:"$BRANCH" 2>/dev/null || true

echo ""
echo "════════════════════════════════════════════════════════"
echo "  Run complete: $TOOL × $MODEL"
echo "  Results: $RUN_JSON"
echo "  Log:     $RUN_LOG"
echo "════════════════════════════════════════════════════════"
