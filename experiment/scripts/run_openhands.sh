#!/usr/bin/env bash
# run_openhands.sh — OpenHands tool adapter (Docker headless mode).
# Usage: run_openhands.sh <prompt_file> <work_dir> <model_ollama_id>
# Exit 0 on success, non-zero on failure or timeout.
#
# Uses Docker because the host Python (3.8) is too old for the openhands pip package.
# The workspace is mounted into the container. Ollama is reached via host.docker.internal.
set -uo pipefail

PROMPT_FILE="${1:?Usage: run_openhands.sh <prompt_file> <work_dir> <model_ollama_id>}"
WORK_DIR="${2:?}"
MODEL_ID="${3:?}"
TOOL_TIMEOUT="${TOOL_TIMEOUT:-1800}"
OLLAMA_HOST="${OLLAMA_HOST:-http://192.168.68.74:11434}"
OPENHANDS_IMAGE="${OPENHANDS_IMAGE:-ghcr.io/all-hands-ai/openhands:latest}"
RUNTIME_IMAGE="${OPENHANDS_RUNTIME_IMAGE:-ghcr.io/all-hands-ai/runtime:latest}"

if [[ ! -f "$PROMPT_FILE" ]]; then
  echo "ERROR: prompt file not found: $PROMPT_FILE" >&2
  exit 1
fi
if [[ ! -d "$WORK_DIR" ]]; then
  echo "ERROR: work dir not found: $WORK_DIR" >&2
  exit 1
fi

# Resolve absolute path (Docker needs it)
WORK_DIR="$(cd "$WORK_DIR" && pwd)"
TASK_TEXT="$(cat "$PROMPT_FILE")"

# Replace localhost/127.0.0.1 with host.docker.internal so the container can
# reach the Ollama server on the Docker host network.
CONTAINER_OLLAMA_HOST="${OLLAMA_HOST//localhost/host.docker.internal}"
CONTAINER_OLLAMA_HOST="${CONTAINER_OLLAMA_HOST//127.0.0.1/host.docker.internal}"

exec timeout "$TOOL_TIMEOUT" docker run --rm \
  --add-host host.docker.internal:host-gateway \
  -e SANDBOX_RUNTIME_CONTAINER_IMAGE="$RUNTIME_IMAGE" \
  -e SANDBOX_USER_ID="$(id -u)" \
  -e WORKSPACE_MOUNT_PATH="$WORK_DIR" \
  -e LLM_MODEL="ollama/$MODEL_ID" \
  -e LLM_BASE_URL="$CONTAINER_OLLAMA_HOST/v1" \
  -e LLM_API_KEY="ollama" \
  -e LLM_TEMPERATURE="0.2" \
  -e LOG_ALL_EVENTS="true" \
  -v "$WORK_DIR:/opt/workspace_base" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  "$OPENHANDS_IMAGE" \
  python -m openhands.core.main \
    --task "$TASK_TEXT" \
    --workspace-dir /opt/workspace_base \
    --no-browser-actions \
    --headless
