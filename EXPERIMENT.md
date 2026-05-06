# Days in Office — LLM Coding Benchmark Experiment

**Project root:** `/Users/carvalhorr/code/days-in-office`

---

## 1. Overview

This experiment measures how well different combinations of **AI coding tools** and **local LLM models** can autonomously implement the "Days in Office" Android application from scratch, given only the architecture and task specifications.

The experiment has two independent variables:
- **Tool** — the agentic coding framework driving the implementation
- **Model** — the LLM being invoked by the tool

Every tool is tested against every model, producing a full matrix of results. Each cell in the matrix is one complete run of all 20 tasks.

### 1.1 Hypothesis

Code-specialized models will complete more tasks with fewer retries regardless of tool. Tools with better file-editing and context management will complete tasks faster. The interaction between tool and model may matter as much as either variable alone.

---

## 2. Experiment Matrix

### 2.1 Models Under Test

| Model ID (Ollama) | Short Name | Size | Pre-pulled |
|---|---|---|---|
| `gemma4:31b` | `gemma4-31b` | ~19 GB | Yes |
| `devstral` | `devstral` | ~14 GB | No |
| `qwen2.5-coder:32b` | `qwen25coder-32b` | ~20 GB | No |
| `deepseek-r1:32b` | `deepseek-r1-32b` | ~20 GB | No |

### 2.2 Tools Under Test

| Tool | Short Name | How it uses the model | Automatable |
|---|---|---|---|
| [Aider](https://aider.chat) | `aider` | `--model ollama/<id> --api-base <url>` | Yes — `--message` flag |
| [OpenHands](https://github.com/All-Hands-AI/OpenHands) | `openhands` | Set `LLM_BASE_URL` + `LLM_MODEL` env vars | Yes — REST API / headless mode |
| [Goose](https://github.com/block/goose) | `goose` | OpenAI-compatible Ollama endpoint | Yes — `--message` flag |

### 2.3 Full Run Matrix

20 tasks × 12 runs (4 models × 3 tools) = **240 task executions** total.

| | gemma4:31b | devstral | qwen2.5-coder:32b | deepseek-r1:32b |
|---|---|---|---|---|
| **aider** | ✦ | ✦ | ✦ | ✦ |
| **openhands** | ✦ | ✦ | ✦ | ✦ |
| **goose** | ✦ | ✦ | ✦ | ✦ |

✦ = one full run (TASK-001 → TASK-020)

---

## 3. Isolation Design

### 3.1 Directory Structure

```
/Users/carvalhorr/code/days-in-office/
├── ARCHITECTURE.md              # Source of truth — never modified
├── TASKS.md                     # Source of truth — never modified
├── CLAUDE.md                    # Source of truth — never modified
├── EXPERIMENT.md                # This document
│
├── experiment/
│   ├── scripts/                 # All orchestration scripts (see Section 7)
│   ├── templates/               # Prompt templates (read-only during runs)
│   │   ├── task_prompt.txt
│   │   └── retry_prompt.txt
│   ├── env_snapshot.json        # Captured environment at experiment start
│   └── results/
│       ├── aider/
│       │   ├── gemma4-31b/
│       │   │   ├── run.json
│       │   │   └── run.log
│       │   ├── devstral/
│       │   ├── qwen25coder-32b/
│       │   └── deepseek-r1-32b/
│       ├── openhands/
│       │   └── <same structure>
│       ├── goose/
│       │   └── <same structure>
│       └── report/
│           └── comparison.md    # Final generated report
│
└── runs/
    ├── aider/
    │   ├── gemma4-31b/          # Isolated Android project
    │   │   ├── ARCHITECTURE.md  # Copied from source of truth
    │   │   ├── TASKS.md         # Working copy (statuses updated during run)
    │   │   ├── CLAUDE.md        # Copied from source of truth
    │   │   └── <android project files created by the model>
    │   ├── devstral/
    │   ├── qwen25coder-32b/
    │   └── deepseek-r1-32b/
    ├── openhands/
    │   └── <same structure>
    └── goose/
        └── <same structure>
```

### 3.2 Isolation Rules

1. Each run directory (`runs/<tool>/<model>/`) is created fresh by `setup_run.sh` and must not exist beforehand.
2. Only `ARCHITECTURE.md`, `TASKS.md`, and `CLAUDE.md` are copied in. No code, no Gradle files, no IDE config.
3. The tool's process for each run operates only inside that run directory.
4. Runs are **strictly sequential** — no two runs share the GPU simultaneously. This is enforced by a **lock file** (`experiment/.lock`), not by convention alone. See Section 3.4.
5. The Ollama model is flushed from GPU memory between runs of different models (see `flush_model.sh`).
6. Between runs of different tools using the same model, the model does NOT need to be flushed (it can stay loaded).
7. Each run directory is initialized as its own git repo (`git init`) so all tools that require git work correctly.

### 3.4 GPU Lock

Only one run may hold the GPU at a time. `run_experiment.sh` enforces this with a lock file at `experiment/.lock`.

**Lock file contents:**
```
tool=aider
model=gemma4-31b
pid=12345
started=2026-05-07T09:00:00Z
```

**Acquire (at start of `run_experiment.sh`):**
```bash
LOCK_FILE="$(git rev-parse --show-toplevel)/experiment/.lock"

acquire_lock() {
  if [[ -f "$LOCK_FILE" ]]; then
    LOCK_PID=$(grep "^pid=" "$LOCK_FILE" | cut -d= -f2)
    if kill -0 "$LOCK_PID" 2>/dev/null; then
      echo "ERROR: Another run is active (PID $LOCK_PID). See $LOCK_FILE for details."
      cat "$LOCK_FILE"
      exit 1
    else
      echo "WARNING: Stale lock found (PID $LOCK_PID no longer running). Removing."
      rm -f "$LOCK_FILE"
    fi
  fi
  printf "tool=%s\nmodel=%s\npid=%s\nstarted=%s\n" \
    "$TOOL" "$MODEL_SHORT_NAME" "$$" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$LOCK_FILE"
}
```

**Release (always, including on crash via `trap`):**
```bash
release_lock() {
  rm -f "$LOCK_FILE"
}
trap release_lock EXIT INT TERM
```

**Stale lock handling:** if the PID in the lock file is no longer running (process died without cleanup), the lock is considered stale and is removed automatically. A warning is printed to the log.

**`experiment/.lock` is gitignored** — it is ephemeral and must never be committed.

### 3.3 Run Order

Run all models for one tool before moving to the next tool. This minimises model load/unload cycles:

```
aider      × gemma4-31b
aider      × devstral
aider      × qwen25coder-32b
aider      × deepseek-r1-32b
── flush all models ──
openhands  × gemma4-31b
openhands  × devstral
openhands  × qwen25coder-32b
openhands  × deepseek-r1-32b
── flush all models ──
goose      × gemma4-31b
goose      × devstral
goose      × qwen25coder-32b
goose      × deepseek-r1-32b
```

---

## 4. The Agent Loop

### 4.1 Overview

The orchestrator script (`run_experiment.sh`) drives a single tool+model combination through all 20 tasks. The loop is tool-agnostic — it delegates the actual invocation to a **tool adapter** script.

```
for TASK_ID in TASK-001 ... TASK-020:
    for attempt in 1..3:
        build prompt (task_prompt.txt or retry_prompt.txt)
        invoke tool adapter (run_<tool>.sh)
        run QA verification commands
        if all pass → mark DONE, break
        else        → capture failure output, continue
    if not DONE → mark FAILED
    record metrics
```

### 4.2 Task Ordering

Tasks run in strictly numeric order (TASK-001 → TASK-020). TASKS.md was designed so that numeric order always satisfies all dependencies. If a task is FAILED, the run continues to the next task rather than aborting — this measures how far each combination gets even when earlier tasks fail.

### 4.3 Prompt Construction

Prompts are rendered from templates using `render_prompt.py`.

**Variables available in all templates:**

| Variable | Value |
|---|---|
| `{{TASK_ID}}` | e.g. `TASK-005` |
| `{{TASK_TITLE}}` | e.g. `Working Days and Compliance Use Cases` |
| `{{TASK_BODY}}` | Full text of the task section from TASKS.md |
| `{{ARCHITECTURE_SUMMARY}}` | Full text of ARCHITECTURE.md |
| `{{ATTEMPT_NUMBER}}` | 1, 2, or 3 |
| `{{FAILURE_OUTPUT}}` | Last 4000 chars of failing QA output (retry only) |

#### `task_prompt.txt` (attempt 1)
```
You are implementing an Android application called "Days in Office".

The architecture specification is:

<architecture>
{{ARCHITECTURE_SUMMARY}}
</architecture>

Your current task is {{TASK_ID}}: {{TASK_TITLE}}.

<task>
{{TASK_BODY}}
</task>

Instructions:
1. Read the architecture specification fully before writing any code.
2. Implement exactly the files listed in "Scope — Files to Create".
3. Follow every constraint in "Implementation Details" exactly.
4. Write all tests specified in the task.
5. Do not create files not listed in the task scope.
6. After implementing, update the task status in TASKS.md from NOT_STARTED to DONE.
7. Commit with message: feat: complete {{TASK_ID}} — {{TASK_TITLE}}
```

#### `retry_prompt.txt` (attempts 2–3)
```
The previous implementation attempt for {{TASK_ID}} failed QA verification.

Failing command output:

<failure>
{{FAILURE_OUTPUT}}
</failure>

Review the error carefully. Fix the implementation so the QA verification passes.
Commit the fix with message: fix: retry {{ATTEMPT_NUMBER}} for {{TASK_ID}}
```

### 4.4 Tool Adapters

Each tool has a thin adapter script that accepts a rendered prompt file and a working directory, and exits 0 on success or non-zero on error/timeout.

#### `run_aider.sh`
```bash
#!/usr/bin/env bash
# Usage: run_aider.sh <prompt_file> <work_dir> <model_ollama_id> <files...>
PROMPT_FILE=$1; WORK_DIR=$2; MODEL_ID=$3; shift 3; FILES="$@"

timeout 600 aider \
  --model "ollama/$MODEL_ID" \
  --api-base "http://192.168.68.74:11434" \
  --no-auto-commits \
  --yes-always \
  --no-suggest-shell-commands \
  --model-settings "temperature=0.2" \
  --message "$(cat $PROMPT_FILE)" \
  --read ARCHITECTURE.md \
  --read TASKS.md \
  --read CLAUDE.md \
  $FILES
```

#### `run_openhands.sh`
```bash
#!/usr/bin/env bash
# Usage: run_openhands.sh <prompt_file> <work_dir> <model_ollama_id>
PROMPT_FILE=$1; WORK_DIR=$2; MODEL_ID=$3

# OpenHands is invoked via its Python CLI in headless mode.
# LLM_BASE_URL points to Ollama's OpenAI-compatible endpoint.
timeout 600 python3 -m openhands.core.main \
  --task "$(cat $PROMPT_FILE)" \
  --workspace-dir "$WORK_DIR" \
  -l "ollama/$MODEL_ID" \
  --llm-base-url "http://192.168.68.74:11434/v1" \
  --no-browser-actions \
  --headless
```

#### `run_goose.sh`
```bash
#!/usr/bin/env bash
# Usage: run_goose.sh <prompt_file> <work_dir> <model_ollama_id>
PROMPT_FILE=$1; WORK_DIR=$2; MODEL_ID=$3

cd "$WORK_DIR"
timeout 600 goose run \
  --provider openai \
  --model "$MODEL_ID" \
  --with-extension computercontroller \
  -- "$(cat $PROMPT_FILE)"
```
> Goose uses Ollama's OpenAI-compatible API (`/v1`). Set `OPENAI_BASE_URL=http://192.168.68.74:11434/v1` and `OPENAI_API_KEY=ollama` before invoking.

#### Adding More Tools

To add a fourth tool, create `experiment/scripts/run_<toolname>.sh` matching the interface above, add its short name to the `TOOLS` array in `run_experiment.sh`, and add a setup step to `setup_tool.sh`.

### 4.5 QA Verification

After each tool invocation, the orchestrator runs the exact commands from the task's "QA Verification Steps" section (parsed from TASKS.md by `parse_tasks.py`).

- Each command runs inside the run directory with a **5-minute timeout**.
- Exit code 0 = pass. Any non-zero = fail.
- All stdout+stderr is captured to the run log.
- `./gradlew connectedAndroidTest` is skipped if no device is detected (`adb devices` returns empty), recorded as `SKIPPED_NO_DEVICE`.
- ALL commands must pass for the task to be DONE.

---

## 5. Metrics

### 5.1 Per-Task Metrics (in `run.json` `tasks` array)

| Field | Type | Description |
|---|---|---|
| `task_id` | string | e.g. `"TASK-005"` |
| `status` | string | `"DONE"` / `"FAILED"` / `"SKIPPED"` |
| `start_time` | ISO-8601 | Wall clock at first invocation |
| `end_time` | ISO-8601 | Wall clock at resolution |
| `duration_seconds` | integer | |
| `attempts` | integer | 1–3 |
| `failure_reasons` | string[] | Per failed attempt: `"QA_FAIL"`, `"TIMEOUT"`, `"TOOL_ERROR"` |
| `qa_results` | object[] | One per QA command: `{command, exit_code, passed, duration_seconds, skipped_reason}` |
| `build_success` | boolean\|null | `./gradlew assembleDebug` result after task |
| `lint_error_count` | integer\|null | Parsed from lint XML (milestone tasks only) |
| `unit_test_pass_count` | integer\|null | Parsed from JUnit XML |
| `unit_test_fail_count` | integer\|null | Parsed from JUnit XML |
| `files_created` | string[] | From `git diff --name-only HEAD~1` |
| `commit_hash` | string\|null | |

### 5.2 Per-Run Metrics (top-level in `run.json`)

| Field | Type | Description |
|---|---|---|
| `tool` | string | Tool short name |
| `model_id` | string | Full Ollama model ID |
| `model_short_name` | string | |
| `run_start_time` | ISO-8601 | |
| `run_end_time` | ISO-8601 | |
| `total_duration_seconds` | integer | |
| `tool_version` | string | Captured at run start |
| `ollama_version` | string | |
| `tasks_done` | integer | |
| `tasks_failed` | integer | |
| `tasks_skipped` | integer | |
| `total_attempts` | integer | |
| `total_build_successes` | integer | |
| `total_unit_test_passes` | integer | |
| `total_unit_test_failures` | integer | |
| `average_task_duration_seconds` | float | |
| `phase_completion` | object | Keys: phase names; values: 0.0–1.0 fraction DONE |
| `prompt_template_hashes` | object | SHA-256 of each template file |

### 5.3 Measurement Points

- **Build success**: measured after every task (`./gradlew assembleDebug`).
- **Test counts**: measured after every task that has unit tests in its QA steps (parsed from `app/build/test-results/testDebugUnitTest/TEST-*.xml`).
- **Lint error count**: measured after tasks TASK-001, TASK-005, TASK-010, TASK-015, TASK-020 only (to limit overhead).

---

## 6. Fairness Constraints

All of the following must be identical across every run in the matrix.

### 6.1 Pinned Software Versions

| Component | Constraint | Verification |
|---|---|---|
| Each tool | Same version for all models within that tool's runs | Recorded in `run.json.tool_version` |
| Ollama server | Same version across all runs | `curl .../api/version` |
| JDK | Same installation | `java -version 2>&1` |
| Android SDK | Same `ANDROID_HOME` | Env var |
| Python | Same for helper scripts | `python3 --version` |

`generate_report.py` flags any mismatch in the fairness verification section.

### 6.2 Identical Environment Variables (All Runs)

```bash
export JAVA_HOME=/path/to/jdk       # identical path for all runs
export ANDROID_HOME=/path/to/sdk    # identical path for all runs
export OPENAI_API_KEY=ollama        # for tools using OpenAI-compatible endpoint
export OPENAI_BASE_URL=http://192.168.68.74:11434/v1
```

Captured into `experiment/env_snapshot.json` before the first run. Verified before each subsequent run.

### 6.3 Prompt Templates

SHA-256 hashes of both template files are recorded in `run.json`. `generate_report.py` flags any hash mismatch as a fairness violation.

### 6.4 Per-Task Constraints

| Constraint | Value |
|---|---|
| Max attempts per task | 3 |
| Tool invocation timeout | 600 seconds |
| QA command timeout | 300 seconds |
| Retry failure context | Last 4000 chars of stdout+stderr |
| Task order | TASK-001 → TASK-020, sequential |
| Context always included | ARCHITECTURE.md + TASKS.md + CLAUDE.md as read-only |
| Model temperature | 0.2 (set via tool config for all tools) |

### 6.5 Model Context Reset Between Models

Before switching to a new model (regardless of tool), evict the previous model from GPU memory:

```bash
curl -X POST http://192.168.68.74:11434/api/generate \
  -d '{"model": "<previous_model_id>", "keep_alive": 0}'
```

Wait for HTTP 200 before proceeding.

---

## 7. Scripts to Build

All scripts live in `experiment/scripts/`. All are Bash unless noted (Python 3.8+ for data scripts).

### 7.1 `setup_run.sh`
**Usage:** `setup_run.sh <tool> <model_short_name>`
- Fail if `runs/<tool>/<model>/` already exists.
- Create directory, copy the three reference docs, run `git init`, create initial commit.
- Create `experiment/results/<tool>/<model>/` directory.
- Write/verify `env_snapshot.json`.
- Verify tool is installed and Ollama is reachable.

### 7.2 `run_experiment.sh`
**Usage:** `run_experiment.sh <tool> <model_short_name> <model_ollama_id>`

The main loop. Drives one tool+model combination through all 20 tasks:
1. **Acquire the GPU lock** (`experiment/.lock`). Exit immediately if another run holds it. Register `trap release_lock EXIT INT TERM` so the lock is always released, even on crash or Ctrl-C.
2. Read all task IDs from TASKS.md (`parse_tasks.py --list`).
3. For each task:
   a. Extract task body, QA commands, scope files (`parse_tasks.py`).
   b. Render prompt (`render_prompt.py`).
   c. Invoke `run_<tool>.sh` with timeout.
   d. Run QA commands (`run_qa.sh`).
   e. Evaluate pass/fail; retry up to 3 times.
   f. Run post-task measurements (`measure_metrics.sh`).
   g. Append result to `run.json` (`update_run_json.py`).
   h. Update TASKS.md status in run dir (`update_tasks_status.py`).
4. Finalize `run.json` with aggregate metrics.
5. **Release the GPU lock** (also released automatically by `trap` on any exit).

### 7.3 Tool Adapters (one per tool)

- `run_aider.sh` — wraps Aider CLI (see Section 4.4)
- `run_openhands.sh` — wraps OpenHands headless CLI
- `run_goose.sh` — wraps Goose CLI

**Interface contract** (all adapters must match):
```
Input:  $1=prompt_file  $2=work_dir  $3=model_ollama_id  $4..=files (optional, tool-dependent)
Output: exit 0 on success, non-zero on failure or timeout
Stdout/stderr: all tool output (captured by caller to run.log)
```

### 7.4 `check_lock.sh`
**Usage:** `check_lock.sh`

Prints the current lock status — useful to check whether a run is active before starting a new one:
- If `experiment/.lock` exists and the PID is alive: prints the lock contents and exits 1.
- If `experiment/.lock` exists but the PID is dead (stale): prints a warning and exits 2.
- If no lock: prints "No run active." and exits 0.

### 7.5 `setup_tool.sh`
**Usage:** `setup_tool.sh <tool>`

Verifies the tool is installed and prints its version. For OpenHands: verifies Docker is running and the OpenHands image is available. For Goose: verifies the binary is in PATH. Exits 1 if the tool is not ready.

### 7.6 `pull_model.sh`
**Usage:** `pull_model.sh <model_ollama_id>`

POSTs to `http://192.168.68.74:11434/api/pull`, streams progress, exits 0 on success.

### 7.7 `flush_model.sh`
**Usage:** `flush_model.sh <model_ollama_id>`

POSTs `{"model": "<id>", "keep_alive": 0}` to the Ollama generate endpoint. Waits for HTTP 200.

### 7.8 `run_qa.sh`
**Usage:** `run_qa.sh <work_dir> <command>`

Runs a single QA command inside `work_dir` with a 300-second timeout. Outputs JSON: `{"exit_code": 0, "passed": true, "duration_seconds": 12, "output": "..."}`.

### 7.9 `measure_metrics.sh`
**Usage:** `measure_metrics.sh <work_dir> <task_id> [--build] [--tests] [--lint]`

Runs the requested measurements and outputs a JSON fragment:
```json
{"build_success": true, "unit_test_pass_count": 12, "unit_test_fail_count": 0, "lint_error_count": null}
```

### 7.10 `reset_run.sh`
**Usage:** `reset_run.sh --confirm <tool> <model_short_name>`

Deletes `runs/<tool>/<model>/` and `experiment/results/<tool>/<model>/`. Requires `--confirm`.

### 7.11 Python Helper Scripts

| Script | Responsibility |
|---|---|
| `parse_tasks.py` | Extract task body, QA commands, scope files, task list from TASKS.md |
| `render_prompt.py` | Substitute `{{VAR}}` variables in a template file; read values from files with `@` prefix |
| `update_run_json.py` | Append a task result object to `run.json` atomically (write-tmp-rename) |
| `update_tasks_status.py` | In-place update of a TASKS.md status line |
| `parse_lint.py` | Count `<issue severity="error">` in a Gradle lint XML report |
| `generate_report.py` | Read all `run.json` files; produce `comparison.md` |

---

## 8. Comparison Report

Generated by `generate_report.py --results-dir experiment/results/ --output experiment/results/report/comparison.md`.

### 8.1 Auto-Generated Sections

#### Overall Rankings Table
One row per tool+model combination, sorted by tasks completed:

| Tool | Model | Tasks Done | Tasks Failed | Total Time | Avg Task (s) | Test Passes | Build Success Rate |
|---|---|---|---|---|---|---|---|

#### 2D Heatmap — Tasks Completed
Rows = tools, columns = models. Value = tasks DONE out of 20:

```
                 gemma4-31b  devstral  qwen25coder-32b  deepseek-r1-32b
aider               17          14           19               15
openhands           12          10           15               11
goose               15          13           17               14
```

#### 2D Heatmap — Average Task Duration (seconds)
Same shape, values = mean seconds per task.

#### Per-Task Matrix
One table per task. Rows = tools, columns = models. Value = `DONE(N)` / `FAILED` / `SKIPPED`.

#### Phase Completion by Tool
For each tool: bar chart of phase completion fraction across models.

#### Phase Completion by Model
For each model: bar chart of phase completion fraction across tools.

#### Timing Analysis
Total wall-clock duration per run. Highlights fastest and slowest tool+model combination.

#### Build Stability
For each run: sequence of Y/N after each task. Shows which combinations maintain a compiling codebase throughout.

#### Test Quality Trend
Cumulative unit test pass count per task, per run. Shows degradation or improvement over time.

#### Lint Error Trend
At milestone tasks (001, 005, 010, 015, 020): error count per run.

#### Retry Rate Analysis
Average attempts per task per run. Low retries = model understood the task first time. High retries = model needed correction.

#### Fairness Verification
- Tool versions per run (flag mismatches within same tool's runs).
- Prompt template hashes (flag any differences).
- `SKIPPED_NO_DEVICE` entries.
- Environment variable snapshot consistency.

### 8.2 Manual Annotations (Appended by Experimenter)

The script appends the following placeholder sections which the experimenter fills in:

```markdown
## Manual Observations

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
```

---

## 9. Execution Playbook

### 9.1 Pre-Experiment Checklist
- [ ] Ollama reachable: `curl http://192.168.68.74:11434/api/tags`
- [ ] `gemma4:31b` pre-pulled (already done)
- [ ] Aider installed: `aider --version`
- [ ] OpenHands installed: `python3 -m openhands.core.main --help`
- [ ] Goose installed: `goose --version`
- [ ] Java 17+: `java -version`
- [ ] Android SDK configured: `echo $ANDROID_HOME`
- [ ] Python 3.8+: `python3 --version`
- [ ] All scripts in `experiment/scripts/` verified: `bash experiment/scripts/parse_tasks.py --verify TASKS.md`

### 9.2 Pull Missing Models

```bash
bash experiment/scripts/pull_model.sh devstral
bash experiment/scripts/pull_model.sh qwen2.5-coder:32b
bash experiment/scripts/pull_model.sh deepseek-r1:32b
```

### 9.3 Run the Experiment

```bash
# ── AIDER RUNS ──────────────────────────────────────────────
bash experiment/scripts/setup_run.sh aider gemma4-31b
bash experiment/scripts/run_experiment.sh aider gemma4-31b gemma4:31b

bash experiment/scripts/setup_run.sh aider devstral
bash experiment/scripts/run_experiment.sh aider devstral devstral

bash experiment/scripts/setup_run.sh aider qwen25coder-32b
bash experiment/scripts/run_experiment.sh aider qwen25coder-32b qwen2.5-coder:32b

bash experiment/scripts/setup_run.sh aider deepseek-r1-32b
bash experiment/scripts/run_experiment.sh aider deepseek-r1-32b deepseek-r1:32b

bash experiment/scripts/flush_model.sh deepseek-r1:32b

# ── OPENHANDS RUNS ───────────────────────────────────────────
bash experiment/scripts/setup_run.sh openhands gemma4-31b
bash experiment/scripts/run_experiment.sh openhands gemma4-31b gemma4:31b

bash experiment/scripts/setup_run.sh openhands devstral
bash experiment/scripts/run_experiment.sh openhands devstral devstral

bash experiment/scripts/setup_run.sh openhands qwen25coder-32b
bash experiment/scripts/run_experiment.sh openhands qwen25coder-32b qwen2.5-coder:32b

bash experiment/scripts/setup_run.sh openhands deepseek-r1-32b
bash experiment/scripts/run_experiment.sh openhands deepseek-r1-32b deepseek-r1:32b

bash experiment/scripts/flush_model.sh deepseek-r1:32b

# ── GOOSE RUNS ───────────────────────────────────────────────
bash experiment/scripts/setup_run.sh goose gemma4-31b
bash experiment/scripts/run_experiment.sh goose gemma4-31b gemma4:31b

bash experiment/scripts/setup_run.sh goose devstral
bash experiment/scripts/run_experiment.sh goose devstral devstral

bash experiment/scripts/setup_run.sh goose qwen25coder-32b
bash experiment/scripts/run_experiment.sh goose qwen25coder-32b qwen2.5-coder:32b

bash experiment/scripts/setup_run.sh goose deepseek-r1-32b
bash experiment/scripts/run_experiment.sh goose deepseek-r1-32b deepseek-r1:32b

bash experiment/scripts/flush_model.sh deepseek-r1:32b
```

### 9.4 Generate the Report

```bash
python3 experiment/scripts/generate_report.py \
  --results-dir experiment/results/ \
  --output experiment/results/report/comparison.md
```

Then add manual annotations to the generated file.

### 9.5 Restart a Failed Run

```bash
bash experiment/scripts/reset_run.sh --confirm aider gemma4-31b
bash experiment/scripts/setup_run.sh aider gemma4-31b
bash experiment/scripts/run_experiment.sh aider gemma4-31b gemma4:31b
```

---

## 10. Known Limitations

| Limitation | Mitigation |
|---|---|
| Single sample per combination | Each combination runs once. Results are one data point, not a statistical distribution. Note in report. |
| Tool non-determinism | Temperature fixed at 0.2 for all tools+models to reduce variance. |
| Instrumented tests require a device | Recorded as `SKIPPED_NO_DEVICE`, does not count as failure. Affects all runs equally. |
| Context window limits | All 4 models support 128K tokens. ARCHITECTURE.md + TASKS.md + task body ≈ 25K tokens. No truncation expected. Monitor Aider `--tokens` output in logs. |
| Tools may handle git differently | OpenHands and Goose may not auto-commit. The QA step `grep <commit>` is relaxed: if a tool doesn't commit, the file content is checked directly instead. |
| Gradle build cache | Shared across runs within the same machine. First build per model is cold; subsequent tasks benefit from the cache. This is consistent across all tools for the same model. |
| Tool installation differences | `setup_tool.sh` must be run and pass for each tool before any runs begin. |

---

## Appendix A: Phase-to-Task Mapping

```python
PHASE_MAP = {
    "phase_1_foundation":            ["TASK-001", "TASK-002", "TASK-003", "TASK-004"],
    "phase_2_business_logic":        ["TASK-005", "TASK-006"],
    "phase_3_detection":             ["TASK-007", "TASK-008", "TASK-009", "TASK-010"],
    "phase_4_navigation_onboarding": ["TASK-011", "TASK-012"],
    "phase_5_ui":                    ["TASK-013", "TASK-014", "TASK-015"],
    "phase_6_notifications_widget":  ["TASK-016", "TASK-017"],
    "phase_7_quality":               ["TASK-018", "TASK-019", "TASK-020"],
}
```

## Appendix B: Lint Measurement Milestones

```python
LINT_MILESTONES = ["TASK-001", "TASK-005", "TASK-010", "TASK-015", "TASK-020"]
```

## Appendix C: Model Context Windows

| Model | Context Window |
|---|---|
| `gemma4:31b` | 128K tokens |
| `devstral` | 128K tokens |
| `qwen2.5-coder:32b` | 128K tokens |
| `deepseek-r1:32b` | 128K tokens |

## Appendix D: Tool Version Capture Commands

| Tool | Version Command |
|---|---|
| Aider | `aider --version` |
| OpenHands | `python3 -m openhands.core.main --version` |
| Goose | `goose --version` |
