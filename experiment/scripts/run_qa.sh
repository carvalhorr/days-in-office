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

# Skip connectedAndroidTest if no device is attached
if echo "$COMMAND" | grep -q "connectedAndroidTest"; then
  if ! adb devices 2>/dev/null | grep -q "device$"; then
    echo '{"exit_code":0,"passed":true,"duration_seconds":0,"output":"SKIPPED_NO_DEVICE","skipped_reason":"No ADB device detected"}'
    exit 0
  fi
fi

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
