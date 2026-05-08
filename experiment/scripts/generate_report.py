#!/usr/bin/env python3
"""Generate comparison.md from all run.json result files.

Usage:
  generate_report.py --results-dir experiment/results/ --output experiment/results/report/comparison.md
"""

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path


TOOLS = ['aider', 'openhands', 'goose']
MODELS = ['gemma4-31b', 'devstral', 'qwen25coder-32b', 'deepseek-r1-32b']
MODEL_DISPLAY = {
    'gemma4-31b': 'gemma4-31b',
    'devstral': 'devstral',
    'qwen25coder-32b': 'qwen2.5-coder',
    'deepseek-r1-32b': 'deepseek-r1',
}
ALL_TASKS = [f"TASK-{i:03d}" for i in range(1, 21)]
LINT_MILESTONES = {"TASK-001", "TASK-005", "TASK-010", "TASK-015", "TASK-020"}
PHASE_MAP = {
    "Phase 1 Foundation":            ["TASK-001", "TASK-002", "TASK-003", "TASK-004"],
    "Phase 2 Business Logic":        ["TASK-005", "TASK-006"],
    "Phase 3 Detection":             ["TASK-007", "TASK-008", "TASK-009", "TASK-010"],
    "Phase 4 Navigation/Onboarding": ["TASK-011", "TASK-012"],
    "Phase 5 UI":                    ["TASK-013", "TASK-014", "TASK-015"],
    "Phase 6 Notifications/Widget":  ["TASK-016", "TASK-017"],
    "Phase 7 Quality":               ["TASK-018", "TASK-019", "TASK-020"],
}


def load_runs(results_dir: Path) -> dict:
    """Return {(tool, model): run_data} for all found run.json files."""
    runs = {}
    for tool in TOOLS:
        for model in MODELS:
            path = results_dir / tool / model / 'run.json'
            if path.exists():
                try:
                    runs[(tool, model)] = json.loads(path.read_text())
                except Exception as e:
                    print(f"WARNING: could not parse {path}: {e}", file=sys.stderr)
    return runs


def task_status(run: dict, task_id: str) -> str:
    for t in run.get('tasks', []):
        if t.get('task_id') == task_id:
            return t.get('status', '?')
    return '-'


def task_attempts(run: dict, task_id: str) -> int:
    for t in run.get('tasks', []):
        if t.get('task_id') == task_id:
            return t.get('attempts', 0)
    return 0


def task_duration(run: dict, task_id: str):
    for t in run.get('tasks', []):
        if t.get('task_id') == task_id:
            return t.get('duration_seconds')
    return None


def fmt_status(status: str, attempts: int) -> str:
    if status == 'DONE':
        return f"DONE({attempts})" if attempts > 1 else "DONE"
    if status == '-':
        return '-'
    return status


def cell(runs, tool, model, task_id):
    key = (tool, model)
    if key not in runs:
        return 'N/A'
    run = runs[key]
    status = task_status(run, task_id)
    attempts = task_attempts(run, task_id)
    return fmt_status(status, attempts)


def md_table(headers, rows):
    widths = [max(len(str(h)), max((len(str(r[i])) for r in rows), default=0)) for i, h in enumerate(headers)]
    sep = '|' + '|'.join('-' * (w + 2) for w in widths) + '|'
    header_row = '|' + '|'.join(f" {str(h):<{w}} " for h, w in zip(headers, widths)) + '|'
    lines = [header_row, sep]
    for row in rows:
        lines.append('|' + '|'.join(f" {str(v):<{w}} " for v, w in zip(row, widths)) + '|')
    return '\n'.join(lines)


def generate(runs: dict) -> str:
    lines = []
    now = datetime.utcnow().strftime('%Y-%m-%d %H:%M UTC')
    lines.append(f"# Days in Office — Benchmark Report\n\n_Generated {now}_\n")

    # ── Overall Rankings ────────────────────────────────────────────────────────
    lines.append("## Overall Rankings\n")
    headers = ['Tool', 'Model', 'Tasks Done', 'Tasks Failed', 'Total Time (min)', 'Avg Task (s)', 'Test Passes', 'Build Successes']
    rows = []
    for tool in TOOLS:
        for model in MODELS:
            key = (tool, model)
            if key not in runs:
                continue
            r = runs[key]
            total_min = f"{r.get('total_duration_seconds', 0) // 60}" if r.get('total_duration_seconds') else '?'
            rows.append([
                tool, MODEL_DISPLAY.get(model, model),
                r.get('tasks_done', '?'),
                r.get('tasks_failed', '?'),
                total_min,
                r.get('average_task_duration_seconds', '?'),
                r.get('total_unit_test_passes', '?'),
                r.get('total_build_successes', '?'),
            ])
    rows.sort(key=lambda r: -(r[2] if isinstance(r[2], int) else -1))
    lines.append(md_table(headers, rows))
    lines.append('')

    # ── Heatmap: Tasks Completed ─────────────────────────────────────────────
    lines.append("## Tasks Completed Heatmap\n")
    model_headers = ['Tool'] + [MODEL_DISPLAY.get(m, m) for m in MODELS]
    heatmap_rows = []
    for tool in TOOLS:
        row = [tool]
        for model in MODELS:
            key = (tool, model)
            if key not in runs:
                row.append('N/A')
            else:
                row.append(f"{runs[key].get('tasks_done', '?')}/20")
        heatmap_rows.append(row)
    lines.append(md_table(model_headers, heatmap_rows))
    lines.append('')

    # ── Heatmap: Avg Task Duration ───────────────────────────────────────────
    lines.append("## Average Task Duration (seconds)\n")
    dur_rows = []
    for tool in TOOLS:
        row = [tool]
        for model in MODELS:
            key = (tool, model)
            if key not in runs:
                row.append('N/A')
            else:
                v = runs[key].get('average_task_duration_seconds')
                row.append(str(v) if v is not None else '?')
        dur_rows.append(row)
    lines.append(md_table(model_headers, dur_rows))
    lines.append('')

    # ── Per-Task Matrix ───────────────────────────────────────────────────────
    lines.append("## Per-Task Results\n")
    for task_id in ALL_TASKS:
        lines.append(f"### {task_id}\n")
        task_rows = []
        for tool in TOOLS:
            row = [tool]
            for model in MODELS:
                row.append(cell(runs, tool, model, task_id))
            task_rows.append(row)
        lines.append(md_table(model_headers, task_rows))
        lines.append('')

    # ── Phase Completion ─────────────────────────────────────────────────────
    lines.append("## Phase Completion by Tool\n")
    for tool in TOOLS:
        lines.append(f"### {tool}\n")
        phase_rows = []
        for phase, task_ids in PHASE_MAP.items():
            row = [phase]
            for model in MODELS:
                key = (tool, model)
                if key not in runs:
                    row.append('N/A')
                    continue
                r = runs[key]
                pct = r.get('phase_completion', {}).get(
                    phase.lower().replace(' ', '_').replace('/', '_'), None)
                row.append(f"{pct:.0%}" if pct is not None else '?')
            phase_rows.append(row)
        lines.append(md_table(model_headers, phase_rows))
        lines.append('')

    # ── Build Stability ──────────────────────────────────────────────────────
    lines.append("## Build Stability\n")
    lines.append("Y = build passed after task, N = failed, ? = not measured\n")
    build_rows = []
    for tool in TOOLS:
        for model in MODELS:
            key = (tool, model)
            if key not in runs:
                continue
            r = runs[key]
            task_map = {t['task_id']: t for t in r.get('tasks', [])}
            seq = []
            for tid in ALL_TASKS:
                t = task_map.get(tid)
                if t is None:
                    seq.append('-')
                elif t.get('build_success') is True:
                    seq.append('Y')
                elif t.get('build_success') is False:
                    seq.append('N')
                else:
                    seq.append('?')
            build_rows.append([f"{tool}/{MODEL_DISPLAY.get(model, model)}"] + seq)
    if build_rows:
        task_headers = ['Run'] + [f"T{i:02d}" for i in range(1, 21)]
        lines.append(md_table(task_headers, build_rows))
    lines.append('')

    # ── Retry Rate Analysis ──────────────────────────────────────────────────
    lines.append("## Retry Rate Analysis\n")
    retry_rows = []
    for tool in TOOLS:
        row = [tool]
        for model in MODELS:
            key = (tool, model)
            if key not in runs:
                row.append('N/A')
                continue
            r = runs[key]
            tasks = r.get('tasks', [])
            if tasks:
                avg = sum(t.get('attempts', 0) for t in tasks) / len(tasks)
                row.append(f"{avg:.2f}")
            else:
                row.append('?')
        retry_rows.append(row)
    lines.append(md_table(model_headers, retry_rows))
    lines.append('')

    # ── Lint Error Trend ─────────────────────────────────────────────────────
    lines.append("## Lint Error Trend (milestone tasks)\n")
    lint_headers = ['Run'] + sorted(LINT_MILESTONES)
    lint_rows = []
    for tool in TOOLS:
        for model in MODELS:
            key = (tool, model)
            if key not in runs:
                continue
            r = runs[key]
            task_map = {t['task_id']: t for t in r.get('tasks', [])}
            row = [f"{tool}/{MODEL_DISPLAY.get(model, model)}"]
            for tid in sorted(LINT_MILESTONES):
                t = task_map.get(tid)
                v = t.get('lint_error_count') if t else None
                row.append(str(v) if v is not None else '-')
            lint_rows.append(row)
    if lint_rows:
        lines.append(md_table(lint_headers, lint_rows))
    lines.append('')

    # ── Fairness Verification ────────────────────────────────────────────────
    lines.append("## Fairness Verification\n")

    # Template hash consistency
    hashes_by_run = {}
    for key, r in runs.items():
        hashes_by_run[key] = r.get('prompt_template_hashes', {})
    all_hashes = list(hashes_by_run.values())
    if all_hashes:
        reference = all_hashes[0]
        mismatches = [k for k, h in hashes_by_run.items() if h != reference]
        if mismatches:
            lines.append(f"**TEMPLATE HASH MISMATCH** in runs: {mismatches}\n")
        else:
            lines.append("Template hashes: consistent across all runs. ✓\n")

    # Tool version consistency
    lines.append("\n### Tool Versions\n")
    ver_rows = []
    for tool in TOOLS:
        versions = {}
        for model in MODELS:
            key = (tool, model)
            if key in runs:
                versions[model] = runs[key].get('tool_version', '?')
        if versions:
            unique = set(versions.values())
            flag = " ⚠ MISMATCH" if len(unique) > 1 else ""
            for model, ver in versions.items():
                ver_rows.append([tool, MODEL_DISPLAY.get(model, model), ver, flag])
    if ver_rows:
        lines.append(md_table(['Tool', 'Model', 'Version', 'Flag'], ver_rows))
    lines.append('')

    # ── Manual Annotations ───────────────────────────────────────────────────
    lines.append("""## Manual Observations

### Tool Behaviour Notes
<!-- How did each tool handle multi-file edits, git commits, and shell commands?
     Did any tool ignore the task scope and create extra files?
     Did any tool fail to commit, breaking the git history assumption? -->

### Model Behaviour Notes
<!-- Per model: observations on architectural adherence, naming conventions,
     Kotlin idiom quality, Hilt annotation correctness, Compose patterns. -->

### Tool × Model Interaction Effects
<!-- Were there combinations where a good model performed badly due to the tool,
     or a weaker model performed better than expected with a particular tool? -->

### Notable Failure Patterns
<!-- Recurring error types per combination. Did models lose context in later tasks?
     Did tools over-edit files outside the task scope? -->

### Conclusion and Recommendation
<!-- Ranked list of combinations with rationale.
     Which combination would you use for a production Android project?
     What are the cost/quality/speed trade-offs? -->
""")

    return '\n'.join(lines)


def main():
    parser = argparse.ArgumentParser(description='Generate comparison report')
    parser.add_argument('--results-dir', required=True, help='Path to experiment/results/')
    parser.add_argument('--output', required=True, help='Output markdown file (- for stdout)')
    args = parser.parse_args()

    results_dir = Path(args.results_dir)
    if not results_dir.exists():
        print(f"ERROR: {results_dir} not found", file=sys.stderr)
        sys.exit(1)

    runs = load_runs(results_dir)
    if not runs:
        print(f"WARNING: no run.json files found under {results_dir}", file=sys.stderr)

    report = generate(runs)

    if args.output == '-':
        sys.stdout.write(report)
    else:
        out = Path(args.output)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(report)
        print(f"Report written to {out} ({len(runs)} runs)")


if __name__ == '__main__':
    main()
