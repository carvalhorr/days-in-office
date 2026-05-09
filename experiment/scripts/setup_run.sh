#!/usr/bin/env bash
# setup_run.sh — Create and initialise a fresh run directory.
# Usage: setup_run.sh <tool> <model_short_name>
set -euo pipefail

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(git -C "$SCRIPTS_DIR" rev-parse --show-toplevel)"
OLLAMA_HOST="${OLLAMA_HOST:-http://192.168.68.74:11434}"

export JAVA_HOME="${JAVA_HOME:-/usr/local/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="/usr/local/opt/coreutils/libexec/gnubin:$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

TOOL="${1:?Usage: setup_run.sh <tool> <model_short_name>}"
MODEL="${2:?}"

VALID_TOOLS=(aider openhands goose)
VALID_MODELS=(gemma4-31b devstral qwen25coder-32b deepseek-r1-32b)

if ! printf '%s\n' "${VALID_TOOLS[@]}" | grep -qx "$TOOL"; then
  echo "ERROR: unknown tool '$TOOL'. Valid: ${VALID_TOOLS[*]}"
  exit 1
fi
if ! printf '%s\n' "${VALID_MODELS[@]}" | grep -qx "$MODEL"; then
  echo "ERROR: unknown model '$MODEL'. Valid: ${VALID_MODELS[*]}"
  exit 1
fi

RUN_DIR="$ROOT_DIR/runs/$TOOL/$MODEL"
WRAPPER_DIR="$ROOT_DIR/experiment/skeleton"
ENV_SNAPSHOT="$ROOT_DIR/experiment/env_snapshot.json"
BRANCH="run/$TOOL/$MODEL"

if [[ -d "$RUN_DIR" ]]; then
  echo "ERROR: $RUN_DIR already exists."
  echo "Use reset_run.sh --confirm $TOOL $MODEL to delete it first."
  exit 1
fi

echo "Setting up run: $TOOL × $MODEL"
echo "  Run dir: $RUN_DIR"

# Copy Gradle wrapper (pre-seeded binary so agents never need to generate it)
mkdir -p "$RUN_DIR/gradle/wrapper"
cp "$WRAPPER_DIR/gradlew" "$RUN_DIR/gradlew"
chmod +x "$RUN_DIR/gradlew"
cp "$WRAPPER_DIR/gradle/wrapper/gradle-wrapper.jar"        "$RUN_DIR/gradle/wrapper/"
cp "$WRAPPER_DIR/gradle/wrapper/gradle-wrapper.properties" "$RUN_DIR/gradle/wrapper/"
echo "  Copied Gradle wrapper (Gradle $(grep distributionUrl "$RUN_DIR/gradle/wrapper/gradle-wrapper.properties" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+'))"

# Copy reference docs
cp "$ROOT_DIR/ARCHITECTURE.md" "$RUN_DIR/ARCHITECTURE.md"
cp "$ROOT_DIR/TASKS.md"        "$RUN_DIR/TASKS.md"
cp "$ROOT_DIR/CLAUDE.md"       "$RUN_DIR/CLAUDE.md"
echo "  Copied ARCHITECTURE.md, TASKS.md, CLAUDE.md"

# Initialise independent git repo in run dir
git -C "$RUN_DIR" init -q
git -C "$RUN_DIR" add -A
git -C "$RUN_DIR" commit -q -m "chore: init run $TOOL/$MODEL — skeleton + docs"
echo "  git init and initial commit done"

# Register branch in main repo so cross-run git diff works
git -C "$ROOT_DIR" fetch "$RUN_DIR" HEAD:"$BRANCH" 2>/dev/null \
  && echo "  Branch $BRANCH registered in main repo" \
  || echo "  WARNING: could not register branch in main repo (non-fatal)"

# Gradle pre-flight — just verify the wrapper binary is present and executable
echo "  Checking Gradle wrapper..."
if [[ ! -x "$RUN_DIR/gradlew" ]]; then
  echo "ERROR: $RUN_DIR/gradlew not executable"
  exit 1
fi
if [[ ! -f "$RUN_DIR/gradle/wrapper/gradle-wrapper.jar" ]]; then
  echo "ERROR: gradle-wrapper.jar missing"
  exit 1
fi
GRADLE_VER=$(grep distributionUrl "$RUN_DIR/gradle/wrapper/gradle-wrapper.properties" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)
echo "    Gradle wrapper $GRADLE_VER OK"

# Verify Java
JAVA_VER=$(java -version 2>&1 | head -1)
echo "  Java: $JAVA_VER"

# Verify Android SDK
[[ -z "${ANDROID_HOME:-}" ]] && echo "WARNING: ANDROID_HOME not set"

# Verify Ollama reachable
echo "  Checking Ollama at $OLLAMA_HOST ..."
if ! curl -sf "$OLLAMA_HOST/api/tags" > /dev/null; then
  echo "ERROR: Ollama not reachable at $OLLAMA_HOST"
  exit 1
fi
OLLAMA_VER=$(curl -sf "$OLLAMA_HOST/api/version" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('version','unknown'))" 2>/dev/null \
  || echo "unknown")
echo "    Ollama $OLLAMA_VER"

# Verify tool is installed
bash "$SCRIPTS_DIR/setup_tool.sh" "$TOOL"

# Write or verify env_snapshot.json
python3 - <<PYEOF
import json, os, subprocess
from pathlib import Path
from datetime import datetime

snapshot_path = Path("$ENV_SNAPSHOT")
now = datetime.now().strftime('%Y-%m-%dT%H:%M:%S')

def capture_version(cmd):
    try:
        return subprocess.check_output(cmd, stderr=subprocess.STDOUT, text=True).strip().splitlines()[0]
    except Exception:
        return "unknown"

current = {
    "captured_at": now,
    "java_version": capture_version(["java", "-version"]),
    "python_version": capture_version(["python3", "--version"]),
    "android_home": os.environ.get("ANDROID_HOME", ""),
    "java_home":    os.environ.get("JAVA_HOME", ""),
    "openai_base_url": os.environ.get("OPENAI_BASE_URL", "http://192.168.68.74:11434/v1"),
    "ollama_host": "$OLLAMA_HOST",
    "ollama_version": "$OLLAMA_VER",
}

if snapshot_path.exists():
    existing = json.loads(snapshot_path.read_text())
    mismatch = [f"  {k}: was {existing.get(k)!r}, now {current.get(k)!r}"
                for k in ("java_version", "android_home", "java_home")
                if existing.get(k) != current.get(k)]
    if mismatch:
        print("WARNING: environment differs from initial snapshot:")
        print('\n'.join(mismatch))
    else:
        print("  env_snapshot.json: consistent with previous runs")
else:
    snapshot_path.write_text(json.dumps(current, indent=2) + '\n')
    print("  env_snapshot.json written")
PYEOF

echo ""
echo "Setup complete: $TOOL × $MODEL"
echo "Run with: bash experiment/scripts/run_experiment.sh $TOOL $MODEL <ollama_model_id>"
