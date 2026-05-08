#!/usr/bin/env python3
"""Atomically update run.json with task results or run-level metadata.

Usage:
  # Initialise a new run.json (call once at run start):
  update_run_json.py --init <run_json> --tool <t> --model-id <id> --model-short <s>
                     --tool-version <v> --ollama-version <v> --template-hashes <json>

  # Append a completed task record:
  update_run_json.py --append-task <run_json> --task-json <file_or_json_string>

  # Finalise the run (call once at run end):
  update_run_json.py --finalise <run_json>

All writes use write-to-tmp-then-rename for atomicity.
"""

import argparse
import json
import os
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path


def now_iso() -> str:
    return datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')


def atomic_write(path: Path, data: dict):
    tmp_fd, tmp_path = tempfile.mkstemp(dir=path.parent, prefix='.run_json_tmp')
    try:
        with os.fdopen(tmp_fd, 'w') as f:
            json.dump(data, f, indent=2)
            f.write('\n')
        os.replace(tmp_path, path)
    except Exception:
        try:
            os.unlink(tmp_path)
        except OSError:
            pass
        raise


def cmd_init(args):
    path = Path(args.init)
    if path.exists():
        print(f"ERROR: {path} already exists. Delete it first or use reset_run.sh.", file=sys.stderr)
        sys.exit(1)

    template_hashes = json.loads(args.template_hashes) if args.template_hashes else {}

    data = {
        "tool": args.tool,
        "model_id": args.model_id,
        "model_short_name": args.model_short,
        "run_start_time": now_iso(),
        "run_end_time": None,
        "total_duration_seconds": None,
        "tool_version": args.tool_version,
        "ollama_version": args.ollama_version,
        "tasks_done": 0,
        "tasks_failed": 0,
        "tasks_skipped": 0,
        "total_attempts": 0,
        "total_build_successes": 0,
        "total_unit_test_passes": 0,
        "total_unit_test_failures": 0,
        "average_task_duration_seconds": None,
        "phase_completion": {},
        "prompt_template_hashes": template_hashes,
        "tasks": [],
    }
    atomic_write(path, data)
    print(f"Initialised {path}")


def cmd_append_task(args):
    path = Path(args.append_task)
    if not path.exists():
        print(f"ERROR: {path} not found", file=sys.stderr)
        sys.exit(1)

    task_json_input = args.task_json
    if task_json_input.startswith('@'):
        task_data = json.loads(Path(task_json_input[1:]).read_text())
    else:
        task_data = json.loads(task_json_input)

    data = json.loads(path.read_text())
    data['tasks'].append(task_data)
    atomic_write(path, data)


def cmd_finalise(args):
    path = Path(args.finalise)
    if not path.exists():
        print(f"ERROR: {path} not found", file=sys.stderr)
        sys.exit(1)

    data = json.loads(path.read_text())
    end_time = now_iso()
    data['run_end_time'] = end_time

    # Parse start/end for duration
    try:
        start = datetime.strptime(data['run_start_time'], '%Y-%m-%dT%H:%M:%SZ').replace(tzinfo=timezone.utc)
        end = datetime.strptime(end_time, '%Y-%m-%dT%H:%M:%SZ').replace(tzinfo=timezone.utc)
        data['total_duration_seconds'] = int((end - start).total_seconds())
    except Exception:
        pass

    tasks = data.get('tasks', [])
    done = sum(1 for t in tasks if t.get('status') == 'DONE')
    failed = sum(1 for t in tasks if t.get('status') == 'FAILED')
    skipped = sum(1 for t in tasks if t.get('status') == 'SKIPPED')
    total_attempts = sum(t.get('attempts', 0) for t in tasks)
    build_successes = sum(1 for t in tasks if t.get('build_success') is True)
    test_passes = sum(t.get('unit_test_pass_count') or 0 for t in tasks)
    test_fails = sum(t.get('unit_test_fail_count') or 0 for t in tasks)

    data['tasks_done'] = done
    data['tasks_failed'] = failed
    data['tasks_skipped'] = skipped
    data['total_attempts'] = total_attempts
    data['total_build_successes'] = build_successes
    data['total_unit_test_passes'] = test_passes
    data['total_unit_test_failures'] = test_fails

    durations = [t.get('duration_seconds') for t in tasks if t.get('duration_seconds') is not None]
    data['average_task_duration_seconds'] = round(sum(durations) / len(durations), 1) if durations else None

    # Phase completion
    phase_map = {
        "phase_1_foundation":            ["TASK-001", "TASK-002", "TASK-003", "TASK-004"],
        "phase_2_business_logic":        ["TASK-005", "TASK-006"],
        "phase_3_detection":             ["TASK-007", "TASK-008", "TASK-009", "TASK-010"],
        "phase_4_navigation_onboarding": ["TASK-011", "TASK-012"],
        "phase_5_ui":                    ["TASK-013", "TASK-014", "TASK-015"],
        "phase_6_notifications_widget":  ["TASK-016", "TASK-017"],
        "phase_7_quality":               ["TASK-018", "TASK-019", "TASK-020"],
    }
    status_by_id = {t['task_id']: t['status'] for t in tasks}
    phase_completion = {}
    for phase, task_ids in phase_map.items():
        done_in_phase = sum(1 for tid in task_ids if status_by_id.get(tid) == 'DONE')
        phase_completion[phase] = round(done_in_phase / len(task_ids), 3)
    data['phase_completion'] = phase_completion

    atomic_write(path, data)
    print(f"Finalised {path}: {done} done, {failed} failed, {skipped} skipped")


def main():
    parser = argparse.ArgumentParser(description='Update run.json')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--init', metavar='RUN_JSON')
    group.add_argument('--append-task', metavar='RUN_JSON')
    group.add_argument('--finalise', metavar='RUN_JSON')

    parser.add_argument('--tool')
    parser.add_argument('--model-id')
    parser.add_argument('--model-short')
    parser.add_argument('--tool-version')
    parser.add_argument('--ollama-version')
    parser.add_argument('--template-hashes')
    parser.add_argument('--task-json', metavar='JSON_OR_@FILE')

    args = parser.parse_args()

    if args.init:
        for field in ('tool', 'model_id', 'model_short', 'tool_version', 'ollama_version'):
            if not getattr(args, field.replace('-', '_')):
                print(f"ERROR: --{field} required with --init", file=sys.stderr)
                sys.exit(1)
        cmd_init(args)
    elif args.append_task:
        if not args.task_json:
            print("ERROR: --task-json required with --append-task", file=sys.stderr)
            sys.exit(1)
        cmd_append_task(args)
    elif args.finalise:
        cmd_finalise(args)


if __name__ == '__main__':
    main()
