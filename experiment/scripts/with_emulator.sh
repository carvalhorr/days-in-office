#!/usr/bin/env bash
# with_emulator.sh — Run a command with the experiment emulator booted.
#
# Boots the AVD created by setup_emulator.sh from its Quick Boot snapshot,
# exports ANDROID_SERIAL so any `adb` / `./gradlew connectedAndroidTest`
# invocation in the wrapped command targets it, then guarantees shutdown via
# trap (even on crash, SIGTERM, or QA timeout). Snapshot is preserved
# (`-no-snapshot-save`) so the AVD stays in its known-good post-boot state.
#
# Usage:
#   with_emulator.sh <command...>
#
# Concurrency: serialises across concurrent experiment runs via a directory
# lock — the AVD binds console ports 5554/5555 by default and cannot be shared.
# For true parallelism, run setup_emulator.sh per port and pass EMU_PORT.
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export ANDROID_HOME
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

AVD_NAME="${EMU_AVD_NAME:-exp_avd}"
PORT="${EMU_PORT:-5554}"
SERIAL="emulator-$PORT"
BOOT_TIMEOUT="${EMU_BOOT_TIMEOUT:-120}"
LOCKDIR="${EMU_LOCKDIR:-/tmp/days_in_office_emulator_${PORT}.lock.d}"

[[ $# -ge 1 ]] || { echo "Usage: with_emulator.sh <command...>" >&2; exit 2; }

# --- Lock (mkdir is atomic and portable — no flock dependency on macOS) ----
LOCK_WAIT_TIMEOUT="${EMU_LOCK_TIMEOUT:-1800}"
LOCK_START=$SECONDS
while ! mkdir "$LOCKDIR" 2>/dev/null; do
  # Detect stale lock: PID file present but the holder is gone.
  if [[ -f "$LOCKDIR/pid" ]]; then
    HOLDER=$(cat "$LOCKDIR/pid" 2>/dev/null || true)
    if [[ -n "$HOLDER" ]] && ! kill -0 "$HOLDER" 2>/dev/null; then
      echo "Clearing stale emulator lock from PID $HOLDER ..." >&2
      rm -rf "$LOCKDIR"
      continue
    fi
  fi
  if (( SECONDS - LOCK_START >= LOCK_WAIT_TIMEOUT )); then
    echo "ERROR: timed out waiting for emulator lock at $LOCKDIR" >&2
    exit 1
  fi
  sleep 5
done
echo $$ > "$LOCKDIR/pid"

# --- Cleanup (lock + emulator) ---------------------------------------------
EMU_PID=""
cleanup() {
  local rc=$?
  if [[ -n "$EMU_PID" ]] && kill -0 "$EMU_PID" 2>/dev/null; then
    adb -s "$SERIAL" emu kill >/dev/null 2>&1 || true
    # Give it 10s to shut down, then SIGKILL.
    for _ in 1 2 3 4 5 6 7 8 9 10; do
      kill -0 "$EMU_PID" 2>/dev/null || break
      sleep 1
    done
    kill -9 "$EMU_PID" 2>/dev/null || true
    wait "$EMU_PID" 2>/dev/null || true
  fi
  rm -rf "$LOCKDIR"
  exit $rc
}
trap cleanup EXIT INT TERM

# --- Pre-flight: AVD exists? ------------------------------------------------
if [[ ! -d "$HOME/.android/avd/${AVD_NAME}.avd" ]]; then
  echo "ERROR: AVD '$AVD_NAME' not found. Run experiment/scripts/setup_emulator.sh first." >&2
  exit 1
fi

# --- Boot -------------------------------------------------------------------
echo "[with_emulator] booting $AVD_NAME on port $PORT ..."
emulator -avd "$AVD_NAME" -port "$PORT" \
  -no-window -no-audio -no-boot-anim -no-snapshot-save \
  -gpu swiftshader_indirect >/dev/null 2>&1 &
EMU_PID=$!

adb start-server >/dev/null 2>&1 || true
adb -s "$SERIAL" wait-for-device
END=$((SECONDS + BOOT_TIMEOUT))
until [[ "$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]] \
   && adb -s "$SERIAL" shell pm path android >/dev/null 2>&1; do
  if (( SECONDS >= END )); then
    echo "ERROR: emulator failed to boot within ${BOOT_TIMEOUT}s" >&2
    exit 1
  fi
  sleep 2
done
echo "[with_emulator] booted, running wrapped command ..."

export ANDROID_SERIAL="$SERIAL"
"$@"
