#!/usr/bin/env python3
"""Count error-severity issues in a Gradle lint XML report.

Usage:
  parse_lint.py <lint_xml_file>

Prints a single integer (count of <issue severity="error"> elements) to stdout.
Exits 0 on success, 1 if file not found.
"""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def count_errors(xml_path: Path) -> int:
    tree = ET.parse(xml_path)
    root = tree.getroot()
    # Handle both <issues> root and nested structures
    count = 0
    for issue in root.iter('issue'):
        severity = issue.get('severity', '').lower()
        if severity == 'error':
            count += 1
    return count


def main():
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <lint_xml_file>", file=sys.stderr)
        sys.exit(1)

    path = Path(sys.argv[1])
    if not path.exists():
        print(f"ERROR: {path} not found", file=sys.stderr)
        sys.exit(1)

    try:
        print(count_errors(path))
    except ET.ParseError as e:
        print(f"ERROR: XML parse error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
