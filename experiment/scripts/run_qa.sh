#!/usr/bin/env bash
# run_qa.sh — Run a single QA verification command in a work directory.
# Usage: run_qa.sh <work_dir> <command>
# Outputs a JSON line: {"exit_code":N, "passed":true/false, "duration_seconds":N, "output":"..."}
# Exit code of THIS script is always 0; pass/fail is in the JSON.
set -uo pipefail

WORK_DIR="${1:?Usage: run_qa.sh <work_dir> <command>}"
shift
COMMAND="$*"
QA_TIMEOUT="${QA_TIMEOUT:-900}"

# Note: connectedAndroidTest is never invoked bare by any task QA command —
# tasks needing an emulator wrap their command in experiment/scripts/with_emulator.sh,
# which boots the AVD before the test runs. A missing device here is a real
# failure, not something to silently skip.

START=$(date +%s)
TMPOUT=$(mktemp)

(
  cd "$WORK_DIR"
  eval "timeout $QA_TIMEOUT $COMMAND"
) > "$TMPOUT" 2>&1
EXIT_CODE=$?

END=$(date +%s)
DURATION=$(( END - START ))

# Truncate output to last 4000 chars for the JSON field
OUTPUT=$(tail -c 4000 "$TMPOUT")
rm -f "$TMPOUT"

# Determine passed: exit 0 and not timed out
if [[ $EXIT_CODE -eq 0 ]]; then
  PASSED="True"
elif [[ $EXIT_CODE -eq 124 ]]; then
  PASSED="False"
  OUTPUT="TIMEOUT after ${QA_TIMEOUT}s\n$OUTPUT"
else
  PASSED="False"
fi

# Emit JSON (using python for safe quoting)
python3 -c "
import json, sys
print(json.dumps({
  'exit_code': $EXIT_CODE,
  'passed': $PASSED,
  'duration_seconds': $DURATION,
  'output': sys.argv[1],
  'command': sys.argv[2],
}))" "$OUTPUT" "$COMMAND"
