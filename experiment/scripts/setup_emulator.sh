#!/usr/bin/env bash
# setup_emulator.sh — One-time bootstrap for the experiment Android emulator.
#
# Idempotent. Will, in order:
#   1. Verify Java is available (sdkmanager needs JDK 11+).
#   2. Detect the host CPU arch and pick the matching system-image variant.
#   3. Install Android cmdline-tools to $ANDROID_HOME/cmdline-tools/latest/
#      from dl.google.com if missing (~150 MB, one-time per machine).
#   4. Install the system image if missing (~1 GB) — reuses any matching
#      image already installed under $ANDROID_HOME/system-images/ rather
#      than downloading a duplicate.
#   5. Create the AVD if missing.
#   6. Cold-boot once to seed a Quick Boot snapshot so later runs start in
#      ~15-30 s instead of ~1-3 min.
#
# Usage:
#   setup_emulator.sh             # set up if missing; no-op if ready
#   setup_emulator.sh --rebuild   # delete the AVD and snapshot, then re-create
#
# Env overrides:
#   EMU_AVD_NAME             default: exp_avd
#   EMU_SYSTEM_IMAGE         default: best match against installed images,
#                              else system-images;android-35;google_apis;<arch>
#   EMU_DEVICE               default: pixel_6
#   EMU_COLD_BOOT_TIMEOUT    default: 300 (seconds)
#   ANDROID_HOME             default: $HOME/Library/Android/sdk
#   JAVA_HOME                default: /usr/local/opt/openjdk@17 (matches harness)
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
JAVA_HOME="${JAVA_HOME:-/usr/local/opt/openjdk@17}"
export ANDROID_HOME JAVA_HOME

# Cmdline-tools build number. Bumped occasionally by Google. Override via
# EMU_CMDLINE_BUILD if Google rotates URLs and this one 404s.
CMDLINE_BUILD="${EMU_CMDLINE_BUILD:-11076708}"

AVD_NAME="${EMU_AVD_NAME:-exp_avd}"
DEVICE="${EMU_DEVICE:-pixel_6}"
BOOT_TIMEOUT="${EMU_COLD_BOOT_TIMEOUT:-300}"
PORT=5554
SERIAL="emulator-$PORT"

REBUILD=0
for arg in "$@"; do
  case "$arg" in
    --rebuild) REBUILD=1 ;;
    -h|--help) sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown arg: $arg" >&2; exit 2 ;;
  esac
done

# --- 1. Java pre-check ------------------------------------------------------
if [[ -x "$JAVA_HOME/bin/java" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
elif ! command -v java >/dev/null 2>&1; then
  echo "ERROR: java not found and JAVA_HOME=$JAVA_HOME has no bin/java." >&2
  echo "Install JDK 17: brew install openjdk@17" >&2
  exit 1
fi

# --- 2. Arch detection ------------------------------------------------------
HOST_ARCH=$(uname -m)
case "$HOST_ARCH" in
  arm64|aarch64) IMG_ARCH="arm64-v8a" ;;
  x86_64)        IMG_ARCH="x86_64" ;;
  *) echo "ERROR: unsupported host arch: $HOST_ARCH" >&2; exit 1 ;;
esac

# Prefer reusing an installed image matching the host arch over downloading
# a different variant. Caller can still override via EMU_SYSTEM_IMAGE.
detect_installed_image() {
  local arch="$1"
  local base="$ANDROID_HOME/system-images"
  [[ -d "$base" ]] || return 1
  local api_dir variant_dir
  for api_dir in "$base"/android-*; do
    [[ -d "$api_dir" ]] || continue
    for variant_dir in "$api_dir"/*; do
      [[ -d "$variant_dir/$arch" ]] || continue
      [[ -f "$variant_dir/$arch/source.properties" ]] || continue
      echo "system-images;$(basename "$api_dir");$(basename "$variant_dir");$arch"
      return 0
    done
  done
  return 1
}

if [[ -n "${EMU_SYSTEM_IMAGE:-}" ]]; then
  SYSTEM_IMAGE="$EMU_SYSTEM_IMAGE"
elif EXISTING=$(detect_installed_image "$IMG_ARCH"); then
  SYSTEM_IMAGE="$EXISTING"
  echo "Reusing installed system image: $SYSTEM_IMAGE"
else
  SYSTEM_IMAGE="system-images;android-35;google_apis;$IMG_ARCH"
fi

# --- 3. cmdline-tools bootstrap --------------------------------------------
CMDLINE_DIR="$ANDROID_HOME/cmdline-tools/latest"
if [[ ! -x "$CMDLINE_DIR/bin/sdkmanager" ]]; then
  case "$(uname -s)" in
    Darwin) ZIP_NAME="commandlinetools-mac-${CMDLINE_BUILD}_latest.zip" ;;
    Linux)  ZIP_NAME="commandlinetools-linux-${CMDLINE_BUILD}_latest.zip" ;;
    *) echo "ERROR: unsupported OS for cmdline-tools auto-install: $(uname -s)" >&2; exit 1 ;;
  esac
  URL="https://dl.google.com/android/repository/$ZIP_NAME"
  echo "Installing Android cmdline-tools (build $CMDLINE_BUILD) from $URL ..."
  TMPDIR=$(mktemp -d)
  trap 'rm -rf "$TMPDIR"' EXIT
  curl -fL --progress-bar -o "$TMPDIR/cmdline-tools.zip" "$URL"
  unzip -q "$TMPDIR/cmdline-tools.zip" -d "$TMPDIR/"
  # Zip extracts as TMPDIR/cmdline-tools/ — Google expects it under .../latest/
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  mv "$TMPDIR/cmdline-tools" "$CMDLINE_DIR"
  rm -rf "$TMPDIR"
  trap - EXIT
  echo "  Installed to $CMDLINE_DIR"
fi

export PATH="$CMDLINE_DIR/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

require() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: $1 still not on PATH after bootstrap. PATH=$PATH" >&2
    exit 1
  }
}
require sdkmanager
require avdmanager
require emulator
require adb

# --- 4. System image --------------------------------------------------------
if ! sdkmanager --list_installed 2>/dev/null | grep -q "$SYSTEM_IMAGE"; then
  echo "Installing $SYSTEM_IMAGE ..."
  yes | sdkmanager --licenses >/dev/null 2>&1 || true
  sdkmanager "$SYSTEM_IMAGE"
else
  echo "System image $SYSTEM_IMAGE: present."
fi

# --- 5. AVD -----------------------------------------------------------------
AVD_DIR="$HOME/.android/avd/${AVD_NAME}.avd"
AVD_INI="$HOME/.android/avd/${AVD_NAME}.ini"
if [[ $REBUILD -eq 1 && -e "$AVD_DIR" ]]; then
  echo "Removing existing AVD $AVD_NAME ..."
  avdmanager delete avd -n "$AVD_NAME" >/dev/null 2>&1 || true
  rm -rf "$AVD_DIR" "$AVD_INI"
fi
if [[ ! -d "$AVD_DIR" ]]; then
  echo "Creating AVD $AVD_NAME (image: $SYSTEM_IMAGE, device: $DEVICE) ..."
  echo "no" | avdmanager create avd -n "$AVD_NAME" \
    -k "$SYSTEM_IMAGE" --device "$DEVICE" --force
else
  echo "AVD $AVD_NAME: present."
fi

# --- 6. Snapshot — cold boot once, settle, graceful kill saves Quick Boot.
SNAPSHOT_DIR="$AVD_DIR/snapshots/default_boot"
if [[ $REBUILD -eq 1 || ! -d "$SNAPSHOT_DIR" ]]; then
  echo "Seeding Quick Boot snapshot (cold boot, timeout ${BOOT_TIMEOUT}s)..."
  emulator -avd "$AVD_NAME" -port "$PORT" \
    -no-window -no-audio -no-boot-anim \
    -gpu swiftshader_indirect >/dev/null 2>&1 &
  EMU_PID=$!
  trap 'kill "$EMU_PID" 2>/dev/null || true' EXIT INT TERM

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
  echo "  Boot complete; letting system services settle (15s)..."
  sleep 15

  # Trigger snapshot save; saving a multi-GB RAM dump can take ~30s.
  # Wait up to 300s for the emulator to exit on its own, then SIGKILL.
  echo "  Saving snapshot (graceful shutdown, up to 300s)..."
  adb -s "$SERIAL" emu kill >/dev/null 2>&1 || true
  SHUTDOWN_END=$((SECONDS + 300))
  while kill -0 "$EMU_PID" 2>/dev/null; do
    if (( SECONDS >= SHUTDOWN_END )); then
      echo "  WARN: emulator didn't exit in 300s — forcing SIGKILL." >&2
      kill -9 "$EMU_PID" 2>/dev/null || true
      break
    fi
    sleep 2
  done
  wait "$EMU_PID" 2>/dev/null || true
  trap - EXIT INT TERM
  echo "  Snapshot saved: $SNAPSHOT_DIR"
else
  echo "Quick Boot snapshot: present."
fi

echo ""
echo "Emulator ready. Use:"
echo "  experiment/scripts/with_emulator.sh <command...>"
