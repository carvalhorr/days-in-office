#!/usr/bin/env bash
# setup_tool.sh — Verify a coding tool is installed and ready.
# Usage: setup_tool.sh <tool>   (tool = aider | openhands | goose)
set -euo pipefail

TOOL="${1:?Usage: setup_tool.sh <tool>}"
OLLAMA_HOST="${OLLAMA_HOST:-http://192.168.68.74:11434}"

check_ollama() {
  echo "Checking Ollama reachability at $OLLAMA_HOST ..."
  if ! curl -sf "$OLLAMA_HOST/api/tags" > /dev/null; then
    echo "ERROR: Ollama not reachable at $OLLAMA_HOST"
    exit 1
  fi
  OLLAMA_VER=$(curl -sf "$OLLAMA_HOST/api/version" | python3 -c "import sys,json; print(json.load(sys.stdin).get('version','unknown'))" 2>/dev/null || echo "unknown")
  echo "  Ollama version: $OLLAMA_VER"
}

case "$TOOL" in
  aider)
    echo "Checking aider..."
    if ! command -v aider &>/dev/null; then
      echo "ERROR: aider not found in PATH. Install with: pip install aider-chat"
      exit 1
    fi
    AIDER_VER=$(aider --version 2>&1 | head -1)
    echo "  $AIDER_VER"
    check_ollama
    echo "aider: READY"
    ;;

  openhands)
    echo "Checking OpenHands (Docker mode)..."
    if ! command -v docker &>/dev/null; then
      echo "ERROR: docker not found in PATH (required by OpenHands)"
      exit 1
    fi
    if ! docker info &>/dev/null; then
      echo "ERROR: Docker daemon is not running"
      exit 1
    fi
    DOCKER_VER=$(docker --version 2>&1 | head -1)
    echo "  $DOCKER_VER"
    OPENHANDS_IMAGE="${OPENHANDS_IMAGE:-ghcr.io/all-hands-ai/openhands:latest}"
    echo "  Checking for image $OPENHANDS_IMAGE ..."
    if ! docker image inspect "$OPENHANDS_IMAGE" &>/dev/null; then
      echo "  Image not found locally. Pulling (this may take a while)..."
      docker pull "$OPENHANDS_IMAGE" || { echo "ERROR: failed to pull $OPENHANDS_IMAGE"; exit 1; }
    fi
    OH_VER=$(docker run --rm "$OPENHANDS_IMAGE" python -m openhands.core.main --version 2>&1 | head -1 || echo "unknown")
    echo "  OpenHands: $OH_VER"
    check_ollama
    echo "openhands: READY"
    ;;

  goose)
    echo "Checking Goose..."
    if ! command -v goose &>/dev/null; then
      echo "ERROR: goose not found in PATH. See https://github.com/block/goose"
      exit 1
    fi
    GOOSE_VER=$(goose --version 2>&1 | head -1)
    echo "  $GOOSE_VER"
    check_ollama
    echo "goose: READY"
    ;;

  *)
    echo "ERROR: unknown tool '$TOOL'. Valid: aider, openhands, goose"
    exit 1
    ;;
esac
