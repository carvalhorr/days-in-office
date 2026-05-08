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

TOOL="${1:?Usage: run_experiment.sh <tool> <model_short_name> <model_ollama_id>}"
MODEL="${2:?}"
MODEL_OLLAMA_ID="${3:?}"

# ── Paths ───────────────────────────────────────────────────────────────────
RUN_DIR="$ROOT_DIR/runs/$TOOL/$MODEL"
RESULTS_DIR="$ROOT_DIR/experiment/results/$TOOL/$MODEL"
RUN_JSON="$RESULTS_DIR/run.json"
RUN_LOG="$RESULTS_DIR/run.log"
LOCK_FILE="$ROOT_DIR/experiment/.lock"
PAUSE_FILE="$ROOT_DIR/experiment/PAUSE"
STATE_FILE="$ROOT_DIR/experiment/run_state.json"
ARCH_FILE="$ROOT_DIR/ARCHITECTURE.md"
TEMPLATES_DIR="$ROOT_DIR/experiment/templates"
TASK_PROMPT="$TEMPLATES_DIR/task_prompt.txt"
RETRY_PROMPT="$TEMPLATES_DIR/retry_prompt.txt"

MAX_ATTEMPTS=3
TOOL_TIMEOUT="${TOOL_TIMEOUT:-600}"
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
    "$TOOL" "$MODEL" "$$" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$LOCK_FILE"
}

release_lock() {
  rm -f "$LOCK_FILE"
}
trap release_lock EXIT INT TERM

acquire_lock

# ── Logging ─────────────────────────────────────────────────────────────────
exec > >(tee -a "$RUN_LOG") 2>&1
echo "════════════════════════════════════════════════════════"
echo "  run_experiment.sh  —  $TOOL × $MODEL"
echo "  Model: $MODEL_OLLAMA_ID"
echo "  Started: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "════════════════════════════════════════════════════════"

# ── Tool/version capture ────────────────────────────────────────────────────
OLLAMA_VER=$(curl -sf "$OLLAMA_HOST/api/version" | python3 -c "import sys,json; print(json.load(sys.stdin).get('version','unknown'))" 2>/dev/null || echo "unknown")

case "$TOOL" in
  aider)     TOOL_VERSION=$(aider --version 2>&1 | head -1 || echo "unknown") ;;
  openhands) TOOL_VERSION=$(python3 -m openhands.core.main --version 2>&1 | head -1 || echo "unknown") ;;
  goose)     TOOL_VERSION=$(goose --version 2>&1 | head -1 || echo "unknown") ;;
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
  echo "Initialised $RUN_JSON"
else
  echo "Resuming existing run: $RUN_JSON"
fi

# ── State helper ─────────────────────────────────────────────────────────────
write_state() {
  local status="$1" current_task="${2:-}" msg="${3:-}"
  DONE_COUNT=$(python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --list | \
    xargs -I{} python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --task {} --field status 2>/dev/null | \
    grep -c "^DONE$" || echo "0")
  FAILED_COUNT=$(python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --list | \
    xargs -I{} python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --task {} --field status 2>/dev/null | \
    grep -c "^FAILED$" || echo "0")
  python3 -c "
import json, os
from pathlib import Path
from datetime import datetime, timezone
data = {
  'status': '$status',
  'tool': '$TOOL',
  'model_short': '$MODEL',
  'model_ollama_id': '$MODEL_OLLAMA_ID',
  'current_task': '$current_task',
  'current_task_start': os.environ.get('TASK_START_TIME', ''),
  'tasks_done': $DONE_COUNT,
  'tasks_failed': $FAILED_COUNT,
  'tasks_total': 20,
  'last_updated': datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ'),
  'intervention_message': '$msg',
  'pause_requested': Path('$PAUSE_FILE').exists(),
}
Path('$STATE_FILE').write_text(json.dumps(data, indent=2) + '\n')
"
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

  # Check pause file before each task
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

  # Mark IN_PROGRESS
  python3 "$SCRIPTS_DIR/update_tasks_status.py" "$RUN_DIR/TASKS.md" "$TASK_ID" "IN_PROGRESS"
  export TASK_START_TIME
  TASK_START_TIME=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  write_state "running" "$TASK_ID" ""

  TASK_START_SECS=$(date +%s)

  TASK_STATUS_FINAL="FAILED"
  ATTEMPTS=0
  FAILURE_REASONS=()
  QA_RESULTS="[]"
  COMMIT_HASH=""

  for ATTEMPT in $(seq 1 $MAX_ATTEMPTS); do
    ATTEMPTS=$ATTEMPT
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
        --var "FAILURE_OUTPUT=$LAST_FAILURE_OUTPUT"
    fi

    # Invoke tool adapter
    echo "  Invoking $TOOL with model $MODEL_OLLAMA_ID ..."
    TOOL_EXIT=0
    bash "$SCRIPTS_DIR/run_${TOOL}.sh" "$PROMPT_FILE" "$RUN_DIR" "$MODEL_OLLAMA_ID" || TOOL_EXIT=$?
    rm -f "$PROMPT_FILE"

    if [[ $TOOL_EXIT -ne 0 ]]; then
      if [[ $TOOL_EXIT -eq 124 ]]; then
        echo "  TIMEOUT after ${TOOL_TIMEOUT}s"
        FAILURE_REASONS+=("TIMEOUT")
      else
        echo "  TOOL_ERROR (exit $TOOL_EXIT)"
        FAILURE_REASONS+=("TOOL_ERROR")
      fi
      LAST_FAILURE_OUTPUT="Tool exited with code $TOOL_EXIT"
      continue
    fi

    # Run QA verification
    echo "  Running QA verification..."
    QA_COMMANDS=$(python3 "$SCRIPTS_DIR/parse_tasks.py" "$RUN_DIR/TASKS.md" --task "$TASK_ID" --field qa_commands)
    ALL_PASSED=1
    QA_RESULTS_ARR=()
    LAST_FAILURE_OUTPUT=""

    while IFS= read -r CMD; do
      [[ -z "$CMD" ]] && continue
      echo "    QA: $CMD"
      QA_JSON=$(bash "$SCRIPTS_DIR/run_qa.sh" "$RUN_DIR" "$CMD")
      QA_RESULTS_ARR+=("$QA_JSON")
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

    # Build QA results JSON array
    QA_RESULTS="[$(IFS=','; echo "${QA_RESULTS_ARR[*]}")]"

    if [[ $ALL_PASSED -eq 1 ]]; then
      echo "  ✓ All QA checks passed on attempt $ATTEMPT"
      TASK_STATUS_FINAL="DONE"
      # Capture commit hash
      COMMIT_HASH=$(git -C "$RUN_DIR" rev-parse HEAD 2>/dev/null || echo "")
      python3 "$SCRIPTS_DIR/update_tasks_status.py" "$RUN_DIR/TASKS.md" "$TASK_ID" "DONE"
      break
    else
      echo "  ✗ QA failed on attempt $ATTEMPT"
      FAILURE_REASONS+=("QA_FAIL")
      # Truncate failure context to 4000 chars
      LAST_FAILURE_OUTPUT=$(echo -e "$LAST_FAILURE_OUTPUT" | tail -c 4000)
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
record = {
  'task_id': '$TASK_ID',
  'status': '$TASK_STATUS_FINAL',
  'start_time': '$TASK_START_TIME',
  'end_time': '$(date -u +%Y-%m-%dT%H:%M:%SZ)',
  'duration_seconds': $TASK_DURATION,
  'attempts': $ATTEMPTS,
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
" "$METRICS_JSON" "$QA_RESULTS" "$FILES_CREATED" "$FAILURE_REASONS_JSON")

  python3 "$SCRIPTS_DIR/update_run_json.py" --append-task "$RUN_JSON" --task-json "$TASK_RECORD"

  STATUS_ICON="✓"
  [[ "$TASK_STATUS_FINAL" == "FAILED" ]] && STATUS_ICON="✗"
  echo "$STATUS_ICON $TASK_ID: $TASK_STATUS_FINAL (${TASK_DURATION}s, $ATTEMPTS attempt(s))"

  write_state "running" "$TASK_ID" ""
done  # tasks

# ── Finalise ─────────────────────────────────────────────────────────────────
python3 "$SCRIPTS_DIR/update_run_json.py" --finalise "$RUN_JSON"
write_state "complete" "" ""

echo ""
echo "════════════════════════════════════════════════════════"
echo "  Run complete: $TOOL × $MODEL"
echo "  Results: $RUN_JSON"
echo "  Log:     $RUN_LOG"
echo "════════════════════════════════════════════════════════"
