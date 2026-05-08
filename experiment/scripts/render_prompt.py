#!/usr/bin/env python3
"""Render a prompt template by substituting {{VARIABLE}} placeholders.

Usage:
  render_prompt.py --template <file> --output <file> [--var KEY=VALUE ...] [--var KEY=@file ...]

  Values prefixed with @ are read from the named file.
  --output - means stdout.

Example:
  render_prompt.py \\
    --template templates/task_prompt.txt \\
    --output /tmp/prompt.txt \\
    --var TASK_ID=TASK-005 \\
    --var TASK_TITLE="Working Days" \\
    --var TASK_BODY=@/tmp/task_body.txt \\
    --var ARCHITECTURE_SUMMARY=@ARCHITECTURE.md \\
    --var ATTEMPT_NUMBER=1
"""

import argparse
import sys
from pathlib import Path


def render(template: str, variables: dict) -> str:
    result = template
    for key, value in variables.items():
        result = result.replace('{{' + key + '}}', value)
    return result


def main():
    parser = argparse.ArgumentParser(description='Render a prompt template')
    parser.add_argument('--template', required=True, help='Template file path')
    parser.add_argument('--output', required=True, help='Output file path (- for stdout)')
    parser.add_argument('--var', action='append', metavar='KEY=VALUE', default=[],
                        help='Variable substitution. Prefix value with @ to read from file.')
    args = parser.parse_args()

    template_path = Path(args.template)
    if not template_path.exists():
        print(f"ERROR: template {args.template} not found", file=sys.stderr)
        sys.exit(1)

    template = template_path.read_text()

    variables = {}
    for kv in args.var:
        if '=' not in kv:
            print(f"ERROR: invalid --var {kv!r}, expected KEY=VALUE", file=sys.stderr)
            sys.exit(1)
        key, value = kv.split('=', 1)
        if value.startswith('@'):
            file_path = Path(value[1:])
            if not file_path.exists():
                print(f"ERROR: file {value[1:]!r} not found for variable {key}", file=sys.stderr)
                sys.exit(1)
            value = file_path.read_text()
        variables[key] = value

    result = render(template, variables)

    # Warn about un-substituted placeholders
    import re
    remaining = re.findall(r'\{\{[A-Z_]+\}\}', result)
    if remaining:
        print(f"WARNING: unsubstituted placeholders: {remaining}", file=sys.stderr)

    if args.output == '-':
        sys.stdout.write(result)
    else:
        Path(args.output).write_text(result)


if __name__ == '__main__':
    main()
