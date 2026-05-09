#!/usr/bin/env bash
# setup_run.sh — Create and initialise a fresh run directory.
# Usage: setup_run.sh <tool> <model_short_name>
set -euo pipefail

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(git -C "$SCRIPTS_DIR" rev-parse --show-toplevel)"
OLLAMA_HOST="${OLLAMA_HOST:-http://192.168.68.74:11434}"

# Ensure JDK 17 and Android SDK are on PATH for this session
export JAVA_HOME="${JAVA_HOME:-/usr/local/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="/usr/local/opt/coreutils/libexec/gnubin:$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

TOOL="${1:?Usage: setup_run.sh <tool> <model_short_name>}"
MODEL="${2:?}"

VALID_TOOLS=(aider openhands goose)
VALID_MODELS=(gemma4-31b devstral qwen25coder-32b deepseek-r1-32b)

# Validate inputs
if ! printf '%s\n' "${VALID_TOOLS[@]}" | grep -qx "$TOOL"; then
  echo "ERROR: unknown tool '$TOOL'. Valid: ${VALID_TOOLS[*]}"
  exit 1
fi
if ! printf '%s\n' "${VALID_MODELS[@]}" | grep -qx "$MODEL"; then
  echo "ERROR: unknown model '$MODEL'. Valid: ${VALID_MODELS[*]}"
  exit 1
fi

RUN_DIR="$ROOT_DIR/runs/$TOOL/$MODEL"
RESULTS_DIR="$ROOT_DIR/experiment/results/$TOOL/$MODEL"
ENV_SNAPSHOT="$ROOT_DIR/experiment/env_snapshot.json"

# Fail if run dir already exists
if [[ -d "$RUN_DIR" ]]; then
  echo "ERROR: $RUN_DIR already exists."
  echo "Use reset_run.sh --confirm $TOOL $MODEL to delete it first."
  exit 1
fi

echo "Setting up run: $TOOL × $MODEL"
echo "  Run dir:     $RUN_DIR"
echo "  Results dir: $RESULTS_DIR"

# Create directories
mkdir -p "$RUN_DIR"
mkdir -p "$RESULTS_DIR"

# Copy reference docs
cp "$ROOT_DIR/ARCHITECTURE.md" "$RUN_DIR/ARCHITECTURE.md"
cp "$ROOT_DIR/TASKS.md"        "$RUN_DIR/TASKS.md"
cp "$ROOT_DIR/CLAUDE.md"       "$RUN_DIR/CLAUDE.md"
echo "  Copied ARCHITECTURE.md, TASKS.md, CLAUDE.md"

# Initialise git repo in run dir
cd "$RUN_DIR"
git init -q
git add ARCHITECTURE.md TASKS.md CLAUDE.md
git commit -q -m "chore: init run $TOOL/$MODEL"
echo "  git init and initial commit done"

# Verify Java
echo "  Checking Java..."
if ! command -v java &>/dev/null; then
  echo "ERROR: java not found in PATH"
  exit 1
fi
JAVA_VER=$(java -version 2>&1 | head -1)
echo "    $JAVA_VER"

# Verify Android SDK
if [[ -z "${ANDROID_HOME:-}" ]]; then
  echo "WARNING: ANDROID_HOME not set"
fi

# Verify Ollama reachable
echo "  Checking Ollama at $OLLAMA_HOST ..."
if ! curl -sf "$OLLAMA_HOST/api/tags" > /dev/null; then
  echo "ERROR: Ollama not reachable at $OLLAMA_HOST"
  exit 1
fi
OLLAMA_VER=$(curl -sf "$OLLAMA_HOST/api/version" | python3 -c "import sys,json; print(json.load(sys.stdin).get('version','unknown'))" 2>/dev/null || echo "unknown")
echo "    Ollama $OLLAMA_VER"

# Verify tool is installed
bash "$SCRIPTS_DIR/setup_tool.sh" "$TOOL"

# Write or verify env_snapshot.json
python3 - <<PYEOF
import json, os, subprocess, sys
from pathlib import Path
from datetime import datetime, timezone

snapshot_path = Path("$ENV_SNAPSHOT")
now = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')

def capture_version(cmd):
    try:
        return subprocess.check_output(cmd, stderr=subprocess.STDOUT, text=True).strip().splitlines()[0]
    except Exception:
        return "unknown"

java_ver = capture_version(["java", "-version"])
python_ver = capture_version(["python3", "--version"])

current = {
    "captured_at": now,
    "java_version": java_ver,
    "python_version": python_ver,
    "android_home": os.environ.get("ANDROID_HOME", ""),
    "java_home":    os.environ.get("JAVA_HOME", ""),
    "openai_base_url": os.environ.get("OPENAI_BASE_URL", "http://192.168.68.74:11434/v1"),
    "ollama_host": "$OLLAMA_HOST",
    "ollama_version": "$OLLAMA_VER",
}

if snapshot_path.exists():
    existing = json.loads(snapshot_path.read_text())
    # Compare fields (ignore captured_at)
    mismatch = []
    for k in ("java_version", "android_home", "java_home"):
        if existing.get(k) != current.get(k):
            mismatch.append(f"  {k}: was {existing.get(k)!r}, now {current.get(k)!r}")
    if mismatch:
        print("WARNING: environment differs from initial snapshot:")
        print('\n'.join(mismatch))
    else:
        print("  env_snapshot.json: consistent with previous runs")
else:
    snapshot_path.write_text(json.dumps(current, indent=2) + '\n')
    print(f"  env_snapshot.json written")
PYEOF

echo ""
echo "Setup complete: $TOOL × $MODEL"
echo "Run with: bash experiment/scripts/run_experiment.sh $TOOL $MODEL <ollama_model_id>"
