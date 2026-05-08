#!/usr/bin/env python3
"""Parse TASKS.md to extract task info for the experiment orchestrator.

Usage:
  parse_tasks.py --list <tasks_file>
      Print all task IDs in order, one per line.

  parse_tasks.py --task <TASK-XXX> --field <body|qa_commands|scope_files|title|status> <tasks_file>
      Print the requested field for the given task.

  parse_tasks.py --verify <tasks_file>
      Verify the tasks file is parseable and print a summary.
"""

import argparse
import re
import sys
from pathlib import Path


def parse_tasks(content: str) -> dict:
    """Return a dict of task_id -> {title, status, body, qa_commands, scope_files}."""
    tasks = {}

    # Split on task headers: ### TASK-NNN: ...
    task_pattern = re.compile(r'^### (TASK-\d+): (.+)$', re.MULTILINE)
    matches = list(task_pattern.finditer(content))

    for i, match in enumerate(matches):
        task_id = match.group(1)
        title = match.group(2).strip()
        start = match.start()
        end = matches[i + 1].start() if i + 1 < len(matches) else len(content)
        body = content[start:end]

        status = _extract_status(body)
        qa_commands = _extract_qa_commands(body)
        scope_files = _extract_scope_files(body)

        tasks[task_id] = {
            "title": title,
            "status": status,
            "body": body.strip(),
            "qa_commands": qa_commands,
            "scope_files": scope_files,
        }

    return tasks


def _extract_status(body: str) -> str:
    m = re.search(r'\*\*Status:\*\*\s+(\S+)', body)
    return m.group(1) if m else "UNKNOWN"


def _extract_qa_commands(body: str) -> list:
    """Extract commands from the QA Verification Steps bash block."""
    m = re.search(r'#### QA Verification Steps\n```bash\n(.*?)```', body, re.DOTALL)
    if not m:
        return []
    raw = m.group(1).strip()
    commands = []
    for line in raw.splitlines():
        line = line.strip()
        if line and not line.startswith('#'):
            commands.append(line)
    return commands


def _extract_scope_files(body: str) -> list:
    """Extract file paths listed under Scope — Files to Create."""
    m = re.search(r'#### Scope.*?Files to Create\n(.*?)(?=\n####|\Z)', body, re.DOTALL)
    if not m:
        return []
    files = []
    for line in m.group(1).splitlines():
        line = line.strip()
        if line.startswith('- `') and line.endswith('`'):
            files.append(line[3:-1])
        elif line.startswith('- ') and not line.startswith('- `'):
            # plain path without backticks
            path = line[2:].strip()
            if '/' in path or path.endswith('.kt') or path.endswith('.xml'):
                files.append(path)
    return files


def main():
    parser = argparse.ArgumentParser(description='Parse TASKS.md')
    parser.add_argument('tasks_file', help='Path to TASKS.md')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--list', action='store_true', help='List all task IDs')
    group.add_argument('--task', metavar='TASK_ID', help='Task ID to query')
    group.add_argument('--verify', action='store_true', help='Verify and summarize')
    parser.add_argument('--field', choices=['body', 'qa_commands', 'scope_files', 'title', 'status'],
                        help='Field to print (used with --task)')
    args = parser.parse_args()

    path = Path(args.tasks_file)
    if not path.exists():
        print(f"ERROR: {args.tasks_file} not found", file=sys.stderr)
        sys.exit(1)

    content = path.read_text()
    tasks = parse_tasks(content)

    if not tasks:
        print("ERROR: No tasks found in file", file=sys.stderr)
        sys.exit(1)

    if args.list:
        for tid in sorted(tasks.keys()):
            print(tid)

    elif args.verify:
        print(f"Found {len(tasks)} tasks:")
        for tid in sorted(tasks.keys()):
            t = tasks[tid]
            qa_count = len(t['qa_commands'])
            file_count = len(t['scope_files'])
            print(f"  {tid}: {t['title'][:50]!r}  status={t['status']}  qa={qa_count}  files={file_count}")
        sys.exit(0)

    elif args.task:
        tid = args.task
        if tid not in tasks:
            print(f"ERROR: {tid} not found", file=sys.stderr)
            sys.exit(1)
        task = tasks[tid]
        if not args.field:
            print("ERROR: --field required with --task", file=sys.stderr)
            sys.exit(1)
        field = args.field
        if field == 'qa_commands':
            for cmd in task['qa_commands']:
                print(cmd)
        elif field == 'scope_files':
            for f in task['scope_files']:
                print(f)
        else:
            print(task[field])


if __name__ == '__main__':
    main()
