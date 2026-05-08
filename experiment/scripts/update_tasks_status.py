#!/usr/bin/env python3
"""In-place update of a task's Status line in TASKS.md.

Usage:
  update_tasks_status.py <tasks_file> <TASK-XXX> <NEW_STATUS>

Valid statuses: NOT_STARTED, IN_PROGRESS, IN_REVIEW, DONE, FAILED
"""

import re
import sys
from pathlib import Path


VALID_STATUSES = {'NOT_STARTED', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'FAILED'}


def update_status(content: str, task_id: str, new_status: str) -> str:
    # Find the task header and the Status line that follows it
    # Pattern: ### TASK-XXX: ... \n**Status:** OLD_STATUS
    pattern = re.compile(
        r'(### ' + re.escape(task_id) + r':.*?\n\*\*Status:\*\*\s+)(\S+)',
        re.DOTALL
    )
    m = pattern.search(content)
    if not m:
        raise ValueError(f"{task_id} not found in file")

    old_status = m.group(2)
    updated = content[:m.start(2)] + new_status + content[m.end(2):]
    return updated, old_status


def main():
    if len(sys.argv) != 4:
        print(f"Usage: {sys.argv[0]} <tasks_file> <TASK-XXX> <NEW_STATUS>", file=sys.stderr)
        sys.exit(1)

    tasks_file, task_id, new_status = sys.argv[1], sys.argv[2], sys.argv[3]

    if new_status not in VALID_STATUSES:
        print(f"ERROR: invalid status {new_status!r}. Valid: {', '.join(sorted(VALID_STATUSES))}", file=sys.stderr)
        sys.exit(1)

    path = Path(tasks_file)
    if not path.exists():
        print(f"ERROR: {tasks_file} not found", file=sys.stderr)
        sys.exit(1)

    content = path.read_text()
    try:
        updated, old_status = update_status(content, task_id, new_status)
    except ValueError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)

    path.write_text(updated)
    print(f"{task_id}: {old_status} -> {new_status}")


if __name__ == '__main__':
    main()
