#!/usr/bin/env bash
# install_deps.sh — Install all experiment dependencies on macOS.
# Run once before starting the experiment.
# Usage: bash experiment/scripts/install_deps.sh
set -euo pipefail

OLLAMA_HOST="${OLLAMA_HOST:-http://192.168.68.74:11434}"
OPENHANDS_IMAGE="ghcr.io/all-hands-ai/openhands:latest"
OPENHANDS_RUNTIME_IMAGE="ghcr.io/all-hands-ai/runtime:latest"

ok()   { echo "  ✓ $*"; }
info() { echo "  → $*"; }
fail() { echo "  ✗ $*" >&2; exit 1; }
header() { echo ""; echo "── $* ──────────────────────────────────────────"; }

echo "════════════════════════════════════════════════════"
echo "  Days in Office — Experiment Dependency Installer"
echo "════════════════════════════════════════════════════"

# ── Prerequisites ────────────────────────────────────────────
header "Checking prerequisites"

command -v brew  &>/dev/null || fail "Homebrew not found. Install from https://brew.sh"
ok "Homebrew $(brew --version | head -1 | awk '{print $2}')"

command -v docker &>/dev/null || fail "Docker not found. Install Docker Desktop from https://docker.com"
docker info &>/dev/null      || fail "Docker daemon not running. Start Docker Desktop."
ok "Docker $(docker --version | awk '{print $3}' | tr -d ',')"

# ── GNU coreutils (provides timeout) ────────────────────────
header "GNU coreutils (timeout)"
if [[ -f "/usr/local/opt/coreutils/libexec/gnubin/timeout" ]]; then
  ok "coreutils already installed"
else
  info "Installing coreutils via Homebrew..."
  brew install coreutils
  ok "coreutils installed"
fi

# ── JDK 17 ──────────────────────────────────────────────────
header "JDK 17"
if /usr/local/opt/openjdk@17/bin/java -version &>/dev/null 2>&1; then
  ok "JDK 17 already installed"
else
  info "Installing openjdk@17 via Homebrew..."
  brew install openjdk@17
  ok "JDK 17 installed"
fi
JAVA17_HOME="/usr/local/opt/openjdk@17"
ok "JAVA_HOME will be: $JAVA17_HOME"

# ── Android SDK ──────────────────────────────────────────────
header "Android SDK"
ANDROID_HOME_PATH="$HOME/Library/Android/sdk"
if [[ -d "$ANDROID_HOME_PATH/platform-tools" ]]; then
  ok "Android SDK found at $ANDROID_HOME_PATH"
else
  echo ""
  echo "  Android SDK not found at $ANDROID_HOME_PATH."
  echo "  Install Android Studio from https://developer.android.com/studio"
  echo "  then open SDK Manager and install SDK Platform 35."
  echo "  Skipping — the experiment will fail to build until this is done."
fi

# ── Python 3.11 ──────────────────────────────────────────────
header "Python 3.11 (for aider)"
if /usr/local/opt/python@3.11/bin/python3.11 --version &>/dev/null 2>&1; then
  ok "Python 3.11 already installed"
else
  info "Installing python@3.11 via Homebrew..."
  brew install python@3.11
  ok "Python 3.11 installed"
fi

# ── aider ────────────────────────────────────────────────────
header "aider"
if command -v aider &>/dev/null; then
  ok "aider $(aider --version 2>&1)"
else
  info "Installing aider-chat via pip3.11..."
  /usr/local/opt/python@3.11/bin/python3.11 -m pip install aider-chat
  ok "aider $(aider --version 2>&1)"
fi

# ── Goose ────────────────────────────────────────────────────
header "Goose (block-goose-cli)"
if command -v goose &>/dev/null; then
  ok "goose $(goose --version 2>&1 | head -1)"
else
  info "Installing block-goose-cli via Homebrew..."
  brew install block-goose-cli
  ok "goose $(goose --version 2>&1 | head -1)"
fi

# ── OpenHands Docker images ──────────────────────────────────
header "OpenHands Docker images"
for IMAGE in "$OPENHANDS_IMAGE" "$OPENHANDS_RUNTIME_IMAGE"; do
  if docker image inspect "$IMAGE" &>/dev/null; then
    SIZE=$(docker images --format "{{.Size}}" "$IMAGE" 2>/dev/null || echo "?")
    ok "$IMAGE ($SIZE) — already present"
  else
    info "Pulling $IMAGE (this may take several minutes)..."
    docker pull "$IMAGE"
    ok "$IMAGE pulled"
  fi
done

# ── Ollama connectivity ──────────────────────────────────────
header "Ollama server ($OLLAMA_HOST)"
if curl -sf "$OLLAMA_HOST/api/tags" > /dev/null; then
  OLLAMA_VER=$(curl -sf "$OLLAMA_HOST/api/version" | python3 -c "import sys,json; print(json.load(sys.stdin).get('version','?'))" 2>/dev/null || echo "?")
  ok "Ollama $OLLAMA_VER reachable"
  MODELS=$(curl -sf "$OLLAMA_HOST/api/tags" | python3 -c "import sys,json; [print('    '+m['name']) for m in json.load(sys.stdin)['models']]" 2>/dev/null || echo "    (none)")
  echo "  Available models:"
  echo "$MODELS"
else
  echo "  ✗ Ollama not reachable at $OLLAMA_HOST"
  echo "    Start Ollama or check the server address."
fi

# ── Shell environment hint ───────────────────────────────────
header "Environment variables"
echo ""
echo "  Add these to your ~/.zshrc (or ~/.bash_profile) if not already set:"
echo ""
echo "    export JAVA_HOME=/usr/local/opt/openjdk@17"
echo "    export ANDROID_HOME=\$HOME/Library/Android/sdk"
echo "    export PATH=\"\$JAVA_HOME/bin:\$ANDROID_HOME/platform-tools:\$PATH\""
echo "    export OPENAI_API_KEY=ollama"
echo "    export OPENAI_BASE_URL=http://192.168.68.74:11434/v1"
echo ""
echo "  (The experiment scripts set these automatically, so this is optional.)"

# ── Summary ──────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════════════"
echo "  Installation complete."
echo "  Run the pre-flight check:"
echo "    bash experiment/scripts/setup_tool.sh aider"
echo "    bash experiment/scripts/setup_tool.sh openhands"
echo "    bash experiment/scripts/setup_tool.sh goose"
echo "════════════════════════════════════════════════════"
