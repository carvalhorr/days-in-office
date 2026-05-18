#!/usr/bin/env bash
# measure_metrics.sh — Measure build/test/lint metrics after a task.
# Usage: measure_metrics.sh <work_dir> <task_id> [--build] [--tests] [--lint]
# Outputs a JSON object to stdout.
set -uo pipefail

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
WORK_DIR="${1:?Usage: measure_metrics.sh <work_dir> <task_id> [--build] [--tests] [--lint]}"
TASK_ID="${2:?}"
shift 2

DO_BUILD=0
DO_TESTS=0
DO_LINT=0

for arg in "$@"; do
  case "$arg" in
    --build) DO_BUILD=1 ;;
    --tests) DO_TESTS=1 ;;
    --lint)  DO_LINT=1  ;;
  esac
done

BUILD_SUCCESS="null"
UNIT_PASS="null"
UNIT_FAIL="null"
LINT_COUNT="null"

if [[ $DO_BUILD -eq 1 ]]; then
  if (cd "$WORK_DIR" && timeout 300 ./gradlew assembleDebug -q > /dev/null 2>&1); then
    BUILD_SUCCESS="true"
  else
    BUILD_SUCCESS="false"
  fi
fi

if [[ $DO_TESTS -eq 1 ]]; then
  (cd "$WORK_DIR" && timeout 300 ./gradlew testDebugUnitTest -q > /dev/null 2>&1) || true
  # Parse JUnit XML results
  XML_DIR="$WORK_DIR/app/build/test-results/testDebugUnitTest"
  if [[ -d "$XML_DIR" ]]; then
    UNIT_PASS=$(find "$XML_DIR" -name 'TEST-*.xml' -exec python3 -c "
import sys, xml.etree.ElementTree as ET
total_pass = 0
for f in sys.argv[1:]:
    try:
        root = ET.parse(f).getroot()
        tests = int(root.get('tests', 0))
        failures = int(root.get('failures', 0))
        errors = int(root.get('errors', 0))
        total_pass += tests - failures - errors
    except Exception:
        pass
print(total_pass)
" {} + 2>/dev/null || echo "null")
    UNIT_FAIL=$(find "$XML_DIR" -name 'TEST-*.xml' -exec python3 -c "
import sys, xml.etree.ElementTree as ET
total_fail = 0
for f in sys.argv[1:]:
    try:
        root = ET.parse(f).getroot()
        failures = int(root.get('failures', 0))
        errors = int(root.get('errors', 0))
        total_fail += failures + errors
    except Exception:
        pass
print(total_fail)
" {} + 2>/dev/null || echo "null")
  fi
fi

if [[ $DO_LINT -eq 1 ]]; then
  (cd "$WORK_DIR" && timeout 300 ./gradlew lintDebug -q > /dev/null 2>&1) || true
  LINT_XML="$WORK_DIR/app/build/reports/lint-results-debug.xml"
  if [[ -f "$LINT_XML" ]]; then
    LINT_COUNT=$(python3 "$SCRIPTS_DIR/parse_lint.py" "$LINT_XML" 2>/dev/null || echo "null")
  fi
fi

python3 -c "
import json
print(json.dumps({
  'build_success': $BUILD_SUCCESS,
  'unit_test_pass_count': $UNIT_PASS,
  'unit_test_fail_count': $UNIT_FAIL,
  'lint_error_count': $LINT_COUNT,
}))"
