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
TOOL_TIMEOUT="${TOOL_TIMEOUT:-3600}"
OLLAMA_HOST="${OLLAMA_HOST:-http://192.168.68.74:11434}"
OPENHANDS_IMAGE="${OPENHANDS_IMAGE:-ghcr.io/all-hands-ai/openhands:latest}"
# Runtime image (sandbox container) is intentionally NOT pinned — openhands main
# selects a runtime image that matches its own version internally. Pinning here
# leads to "runc create failed: micromamba: no such file or directory" when the
# `:latest` runtime tag drifts out of sync with the main image.

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

# Per-invocation writable mount for OpenHands' own state (JWT secret, session
# files). Without this, OpenHands tries to write to `/.openhands/` inside the
# container (HOME unset → `~` expands to `/`) and fails with PermissionError.
STATE_DIR=$(mktemp -d -t openhands-state-XXXXXX)
trap "rm -rf '$STATE_DIR'" EXIT

# Replace localhost/127.0.0.1 with host.docker.internal so the container can
# reach the Ollama server on the Docker host network.
CONTAINER_OLLAMA_HOST="${OLLAMA_HOST//localhost/host.docker.internal}"
CONTAINER_OLLAMA_HOST="${CONTAINER_OLLAMA_HOST//127.0.0.1/host.docker.internal}"

timeout "$TOOL_TIMEOUT" docker run --rm \
  --add-host host.docker.internal:host-gateway \
  -e SANDBOX_USER_ID="$(id -u)" \
  -e WORKSPACE_MOUNT_PATH="$WORK_DIR" \
  -e LLM_MODEL="ollama/$MODEL_ID" \
  -e LLM_BASE_URL="$CONTAINER_OLLAMA_HOST/v1" \
  -e LLM_API_KEY="ollama" \
  -e LLM_TEMPERATURE="0.2" \
  -e LOG_ALL_EVENTS="true" \
  -v "$WORK_DIR:/opt/workspace_base" \
  -v "$STATE_DIR:/.openhands" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  "$OPENHANDS_IMAGE" \
  python -m openhands.core.main \
    --task "$TASK_TEXT" \
    -d /opt/workspace_base
exit $?
