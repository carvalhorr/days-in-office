#!/usr/bin/env bash
# pull_model.sh — Pull a model on the Ollama server, streaming progress.
# Usage: pull_model.sh <model_ollama_id>
set -euo pipefail

OLLAMA_HOST="${OLLAMA_HOST:-http://192.168.68.74:11434}"
MODEL_ID="${1:?Usage: pull_model.sh <model_ollama_id>}"

echo "Pulling model $MODEL_ID from $OLLAMA_HOST ..."

# Stream the NDJSON progress lines from the Ollama pull API
curl -s -f -X POST "$OLLAMA_HOST/api/pull" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"$MODEL_ID\"}" | while IFS= read -r line; do
    status=$(echo "$line" | python3 -c "import sys,json; d=json.loads(sys.stdin.read()); print(d.get('status',''))" 2>/dev/null || true)
    total=$(echo "$line"  | python3 -c "import sys,json; d=json.loads(sys.stdin.read()); print(d.get('total',''))"  2>/dev/null || true)
    completed=$(echo "$line" | python3 -c "import sys,json; d=json.loads(sys.stdin.read()); print(d.get('completed',''))" 2>/dev/null || true)
    if [[ -n "$total" && "$total" != "None" && "$total" -gt 0 ]]; then
      pct=$(( completed * 100 / total ))
      printf "\r  %s  %d%%" "$status" "$pct"
    elif [[ -n "$status" ]]; then
      echo "  $status"
    fi
  done
echo ""
echo "Pull complete: $MODEL_ID"
