#!/usr/bin/env python3
"""Parse a Claude Code `-p --output-format json` session file into a small
usage record (tokens + USD cost) for inclusion in run.json attempt records.

Usage: parse_claude_session.py <session_json> <out_usage_json>

Output JSON shape (all numeric fields may be null if unavailable):
{
  "cost_usd": <float>,
  "input_tokens": <int>,
  "output_tokens": <int>,
  "cache_read_tokens": <int>,
  "cache_creation_tokens": <int>,
  "num_turns": <int>,
  "duration_ms": <int>,
  "session_id": <str>,
  "is_error": <bool>
}
"""
import json
import sys
from pathlib import Path


def parse(session_path: Path) -> dict:
    raw = session_path.read_text().strip()
    if not raw:
        return {}

    # --output-format json gives a single JSON object.
    # If --output-format stream-json was used (multiple JSONL lines), pick the
    # final "result"-typed event.
    data = None
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        for line in reversed(raw.splitlines()):
            line = line.strip()
            if not line:
                continue
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                continue
            if obj.get("type") == "result":
                data = obj
                break
        if data is None:
            return {}

    usage = data.get("usage") or {}
    return {
        "cost_usd": data.get("total_cost_usd"),
        "input_tokens": usage.get("input_tokens"),
        "output_tokens": usage.get("output_tokens"),
        "cache_read_tokens": usage.get("cache_read_input_tokens"),
        "cache_creation_tokens": usage.get("cache_creation_input_tokens"),
        "num_turns": data.get("num_turns"),
        "duration_ms": data.get("duration_ms"),
        "session_id": data.get("session_id"),
        "is_error": data.get("is_error"),
    }


def main():
    if len(sys.argv) != 3:
        print("Usage: parse_claude_session.py <session_json> <out_usage_json>", file=sys.stderr)
        sys.exit(1)
    session_path = Path(sys.argv[1])
    out_path = Path(sys.argv[2])
    if not session_path.exists():
        print(f"ERROR: session file not found: {session_path}", file=sys.stderr)
        sys.exit(1)
    record = parse(session_path)
    out_path.write_text(json.dumps(record, indent=2) + "\n")


if __name__ == "__main__":
    main()
