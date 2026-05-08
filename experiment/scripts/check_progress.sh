#!/usr/bin/env bash
# check_progress.sh — Print a concise summary of all in-progress and completed runs.
# Called by the overseer between ScheduleWakeup intervals.
set -euo pipefail

ROOT_DIR="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
RESULTS_DIR="$ROOT_DIR/experiment/results"
LOCK_FILE="$ROOT_DIR/experiment/.lock"
PAUSE_FILE="$ROOT_DIR/experiment/PAUSE"
STATE_FILE="$ROOT_DIR/experiment/run_state.json"

TOOLS=(aider openhands goose)
MODELS=(gemma4-31b devstral qwen25coder-32b deepseek-r1-32b)

echo "═══════════════════════════════════════════════════════"
echo "  Days in Office Experiment — Progress Report"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "═══════════════════════════════════════════════════════"

# GPU Lock status
echo ""
echo "── GPU Lock ──────────────────────────────────────────"
if [[ -f "$LOCK_FILE" ]]; then
  LOCK_PID=$(grep "^pid=" "$LOCK_FILE" | cut -d= -f2)
  if kill -0 "$LOCK_PID" 2>/dev/null; then
    echo "  ACTIVE  $(grep -v '^pid=' "$LOCK_FILE" | tr '\n' '  ')"
    echo "  PID $LOCK_PID running since $(grep "^started=" "$LOCK_FILE" | cut -d= -f2)"
  else
    echo "  STALE LOCK (PID $LOCK_PID not running)"
  fi
else
  echo "  No active lock"
fi

# Pause flag
if [[ -f "$PAUSE_FILE" ]]; then
  echo ""
  echo "⚠ PAUSE FILE EXISTS — experiment will pause before next task"
fi

# Live run state
echo ""
echo "── Live State ────────────────────────────────────────"
if [[ -f "$STATE_FILE" ]]; then
  python3 - "$STATE_FILE" <<'PYEOF'
import json, sys
from pathlib import Path
data = json.loads(Path(sys.argv[1]).read_text())
status  = data.get("status", "?")
tool    = data.get("tool", "?")
model   = data.get("model_short", "?")
current = data.get("current_task", "?")
done    = data.get("tasks_done", 0)
failed  = data.get("tasks_failed", 0)
total   = data.get("tasks_total", 20)
updated = data.get("last_updated", "?")
current_start = data.get("current_task_start", "?")
print(f"  Status : {status}")
print(f"  Run    : {tool} × {model}")
print(f"  Task   : {current}  (done={done}, failed={failed}, total={total})")
print(f"  Updated: {updated}")
if current_start and current_start != "?":
    print(f"  Current task started: {current_start}")
PYEOF
else
  echo "  No run_state.json found"
fi

# Per-run results summary
echo ""
echo "── Run Results ───────────────────────────────────────"
printf "  %-12s %-18s %6s %6s %6s %8s\n" "Tool" "Model" "Done" "Failed" "Total" "Dur(min)"
printf "  %-12s %-18s %6s %6s %6s %8s\n" "────" "─────" "────" "──────" "─────" "────────"

ANY_RESULT=0
for tool in "${TOOLS[@]}"; do
  for model in "${MODELS[@]}"; do
    RJ="$RESULTS_DIR/$tool/$model/run.json"
    if [[ -f "$RJ" ]]; then
      ANY_RESULT=1
      python3 - "$RJ" "$tool" "$model" <<'PYEOF'
import json, sys
from pathlib import Path
data = json.loads(Path(sys.argv[1]).read_text())
tool  = sys.argv[2]
model = sys.argv[3]
done    = data.get("tasks_done", "?")
failed  = data.get("tasks_failed", "?")
total   = len(data.get("tasks", []))
secs    = data.get("total_duration_seconds")
dur_min = f"{secs//60}" if secs else ("running" if data.get("run_end_time") is None else "?")
print(f"  {tool:<12} {model:<18} {str(done):>6} {str(failed):>6} {str(total):>6} {dur_min:>8}")
PYEOF
    fi
  done
done

if [[ $ANY_RESULT -eq 0 ]]; then
  echo "  (no completed or in-progress runs yet)"
fi

echo ""
echo "═══════════════════════════════════════════════════════"

# Show intervention needed?
if [[ -f "$STATE_FILE" ]]; then
  NEEDS_INTERVENTION=$(python3 -c "
import json, sys
from pathlib import Path
data = json.loads(Path('$STATE_FILE').read_text())
if data.get('status') in ('paused', 'intervention_needed', 'error'):
    print('YES')
else:
    print('NO')
" 2>/dev/null || echo "NO")
  if [[ "$NEEDS_INTERVENTION" == "YES" ]]; then
    echo ""
    echo "⚠ INTERVENTION NEEDED — check run_state.json for details"
    STATUS=$(python3 -c "import json; print(json.loads(open('$STATE_FILE').read()).get('status','?'))" 2>/dev/null)
    MSG=$(python3 -c "import json; print(json.loads(open('$STATE_FILE').read()).get('intervention_message',''))" 2>/dev/null)
    echo "  Status: $STATUS"
    [[ -n "$MSG" ]] && echo "  Message: $MSG"
  fi
fi
