#!/usr/bin/env bash
# flush_model.sh — Evict a model from Ollama GPU memory.
# Usage: flush_model.sh <model_ollama_id>
set -euo pipefail

OLLAMA_HOST="${OLLAMA_HOST:-http://192.168.68.74:11434}"
MODEL_ID="${1:?Usage: flush_model.sh <model_ollama_id>}"

echo "Flushing model $MODEL_ID from GPU memory..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$OLLAMA_HOST/api/generate" \
  -H "Content-Type: application/json" \
  -d "{\"model\": \"$MODEL_ID\", \"keep_alive\": 0}")

if [[ "$HTTP_CODE" == "200" ]]; then
  echo "Model $MODEL_ID flushed (HTTP 200)."
else
  echo "WARNING: flush returned HTTP $HTTP_CODE for model $MODEL_ID"
  exit 1
fi
