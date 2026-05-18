# Experiment Runbook

Operator guide for running the Days-in-Office LLM-coding experiment by hand. Commands are written to be runnable as-is from the repo root.

```
ROOT = /Users/carvalhorr/code/days-in-office
```

All scripts live in `experiment/scripts/` and assume `git -C "$(dirname $0)" rev-parse --show-toplevel` resolves to `ROOT`.

---

## 1. One-time host setup

Run once per machine. Idempotent.

```bash
# Install host deps (Homebrew, JDK 17, Python deps, etc.)
bash experiment/scripts/install_deps.sh

# Bootstrap the Android emulator (downloads cmdline-tools if missing,
# creates AVD 'exp_avd', seeds Quick Boot snapshot, ~5-10 min first time):
bash experiment/scripts/setup_emulator.sh
```

If the snapshot is corrupted or the AVD gets into a bad state:

```bash
bash experiment/scripts/setup_emulator.sh --rebuild
```

---

## 2. Starting a run

```bash
# 1. Create the run worktree + branch
bash experiment/scripts/setup_run.sh <tool> <model_short_name>

# 2. Start the experiment (blocking — runs all tasks sequentially)
bash experiment/scripts/run_experiment.sh <tool> <model_short_name> <model_id>
```

Valid `<tool>` values: `aider`, `claude`, `goose`, `openhands`.
Valid `<model_short_name>` examples: `sonnet-4-6`, `gemma4-31b`, `devstral`.
`<model_id>` is the full identifier passed to the underlying tool (e.g. `claude-sonnet-4-6` or `gemma3:27b`).

**Concrete example — Claude:**
```bash
bash experiment/scripts/setup_run.sh claude sonnet-4-6
bash experiment/scripts/run_experiment.sh claude sonnet-4-6 claude-sonnet-4-6
```

**Run it in the background** so the shell isn't tied up:
```bash
nohup bash experiment/scripts/run_experiment.sh claude sonnet-4-6 claude-sonnet-4-6 \
  > /tmp/run-claude-sonnet-4-6.log 2>&1 &
echo $! > /tmp/run-claude-sonnet-4-6.pid
```

---

## 3. Pausing and resuming

The orchestrator checks for `experiment/PAUSE` between tasks and exits cleanly if it finds it.

```bash
# Pause after the currently-running task finishes
touch experiment/PAUSE

# Resume
rm experiment/PAUSE
bash experiment/scripts/run_experiment.sh <tool> <model_short_name> <model_id>
```

Resume picks up at the first `NOT_STARTED` task whose dependencies are `DONE`. Already-completed tasks are not re-run.

**Note:** the PAUSE file currently only halts *between* tasks. To stop an in-flight task, kill the process (next section).

---

## 4. Stopping a run mid-flight

```bash
# Find the running orchestrator
cat experiment/.lock                              # shows tool, model, pid, started
ps -p $(grep '^pid=' experiment/.lock | cut -d= -f2) -o pid,etime,command

# Or search broadly:
pgrep -af "run_experiment\|run_claude\|run_aider"
```

**Graceful stop** (recommended — lets the current task commit its state):
```bash
touch experiment/PAUSE              # next inter-task boundary, the loop exits
```

**Hard stop** (kills immediately, may leave a half-written commit):
```bash
LOCK_PID=$(grep '^pid=' experiment/.lock | cut -d= -f2)
kill "$LOCK_PID"                    # SIGTERM; SIGKILL if it doesn't exit in 10s
```

After a hard stop, clear the stale lock if it remains:
```bash
bash experiment/scripts/check_lock.sh             # exit 0=clean, 1=live, 2=stale
rm experiment/.lock                               # only if exit was 2 (stale)
```

---

## 5. Observing a running experiment

```bash
# High-level dashboard (live state + per-run task counts)
bash experiment/scripts/check_progress.sh

# Tail the full orchestration log for a specific run
tail -f runs/<tool>/<model>/experiment/run.log

# Per-task structured data (JSON)
cat runs/<tool>/<model>/experiment/run.json | python3 -m json.tool | less

# Live run state (what the orchestrator is doing right now)
cat experiment/run_state.json
```

To browse session data in a web UI:
```bash
python3 experiment/scripts/viewer.py              # opens viewer.html on a local port
```

For deep dives into a specific failed attempt:
```bash
ls runs/<tool>/<model>/experiment/TASK-XXX/
#   attemptN_prompt.txt    — exact prompt sent to the LLM
#   attemptN_failure.txt   — QA failure output (or tool error)
#   attemptN_session.json  — full Claude/aider session transcript
#   attemptN_usage.json    — token usage breakdown
```

---

## 6. Resetting a run

`reset_run.sh` deletes the worktree AND the `run/<tool>/<model>` branch in the main repo. **Always archive first** if you want to preserve the failure history:

```bash
TODAY=$(date +%Y-%m-%d)
RUN="claude/sonnet-4-6"   # change as needed

# 1. Commit anything uncommitted in the run worktree (e.g. an in-flight prompt)
git -C runs/$RUN add -A
git -C runs/$RUN diff --cached --quiet || \
  git -C runs/$RUN commit -m "chore: archive uncommitted state at reset time"

# 2. Snapshot the branch into an archive ref that reset_run.sh won't touch
git fetch -f runs/$RUN HEAD:archive/$RUN/$TODAY

# 3. Reset
bash experiment/scripts/reset_run.sh --confirm claude sonnet-4-6
```

Verify the archive survives:
```bash
git log archive/claude/sonnet-4-6/$TODAY --oneline | head
```

To later inspect an archived run without restoring:
```bash
git ls-tree -r archive/claude/sonnet-4-6/$TODAY | head
git show archive/claude/sonnet-4-6/$TODAY:experiment/TASK-003/attempt5_prompt.txt
```

---

## 7. Emulator commands

The emulator is needed only for TASK-021 (the release smoke suite). All other tasks run on the JVM.

```bash
# One-off boot for manual testing
bash experiment/scripts/with_emulator.sh adb shell getprop ro.product.cpu.abi

# Run smoke tests against the booted emulator (this is what TASK-021's QA does)
bash experiment/scripts/with_emulator.sh ./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.carvalhorr.daysInOffice.smoke

# Interactive shell on the emulator (boots, then drops you into adb shell)
bash experiment/scripts/with_emulator.sh adb shell
```

`with_emulator.sh` holds a mkdir-based lock at `/tmp/days_in_office_emulator_5554.lock.d`. Concurrent invocations serialise.

**Cleaning up after a crash:**
```bash
# Kill any hung emulator/qemu
pgrep -af "qemu-system\|emulator -avd" | awk '{print $1}' | xargs -r kill -9

# Reset the adb daemon
"$HOME/Library/Android/sdk/platform-tools/adb" kill-server

# Remove stale AVD locks
rm -f ~/.android/avd/exp_avd.avd/{multiinstance,snapshot.lock,hardware-qemu.ini}.lock

# Remove stale with_emulator.sh lock
rm -rf /tmp/days_in_office_emulator_*.lock.d
```

If the snapshot itself is bad, rebuild:
```bash
bash experiment/scripts/setup_emulator.sh --rebuild
```

---

## 8. Running the app interactively on the emulator

For manual UI testing or development iteration. **Not** the same flow as TASK-021 / `with_emulator.sh` — that one is headless and shuts down after a single command. For dev work you want a window, a long-lived emulator, and a repeatable build-install-launch loop.

Run the emulator manually so you can see the UI:

```bash
# Put SDK tools on PATH
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

# Boot the AVD with a window, in the background
emulator -avd exp_avd -port 5554 -no-audio -no-boot-anim &
EMU_PID=$!

# Wait until it's responsive
adb -s emulator-5554 wait-for-device
adb -s emulator-5554 shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'
```

Build, install, and launch the app from a run directory:

```bash
cd runs/<tool>/<model>                                          # e.g. runs/claude/sonnet-4-6
./gradlew installDebug
adb -s emulator-5554 shell am start -n com.carvalhorr.daysInOffice/.app.MainActivity
```

Tail logs while you use it:

```bash
adb -s emulator-5554 logcat | grep -E "daysInOffice|AndroidRuntime|System.err"
```

Iterate (after code edits):

```bash
./gradlew installDebug && \
  adb -s emulator-5554 shell am start -n com.carvalhorr.daysInOffice/.app.MainActivity
```

Stop the emulator when done:

```bash
adb -s emulator-5554 emu kill
wait $EMU_PID 2>/dev/null || true
```

**Conflicts to avoid:**
- This grabs port 5554, the same port `with_emulator.sh` uses. If an experiment is mid-run on TASK-021, either wait for it to finish or stop it (§4) before booting your own emulator.
- The `with_emulator.sh` lock at `/tmp/days_in_office_emulator_5554.lock.d` is *not* used by the interactive flow — you can collide with a parallel `with_emulator.sh` invocation. Coordinate manually.

If the emulator won't boot or crashes:
- See the cleanup commands in §7 (kill emulator/qemu, `adb kill-server`, remove stale AVD locks).
- For a clean snapshot, `bash experiment/scripts/setup_emulator.sh --rebuild`.

---

## 9. Comparing runs

Each run is a fully independent git repo at `runs/<tool>/<model>/`, with its tip continuously fetched into the main repo as `run/<tool>/<model>`. This lets you diff runs against each other:

```bash
# What did claude do differently from gemma4 on TASK-003?
git diff run/aider/gemma4-31b run/claude/sonnet-4-6 -- app/src/

# Compare two attempts on the same task within one run
git log --oneline run/claude/sonnet-4-6 -- experiment/TASK-003/
git diff <attempt1-hash> <attempt2-hash> -- app/src/
```

---

## 10. Generating a final report

After one or more runs complete:

```bash
python3 experiment/scripts/generate_report.py --results-dir runs/ > report.md
```

---

## 11. Environment variables

Override defaults inline (`VAR=value bash script.sh ...`) or export them in your shell.

| Variable | Default | Used by |
|---|---|---|
| `TOOL_TIMEOUT` | `7200` (s) | `run_experiment.sh` — per LLM invocation |
| `QA_TIMEOUT` | `1800` (s) | `run_qa.sh` — per QA verification command |
| `OLLAMA_HOST` | `http://192.168.68.74:11434` | `setup_run.sh` — pre-flight check for local LLM runs |
| `ANDROID_HOME` | `$HOME/Library/Android/sdk` | emulator scripts |
| `JAVA_HOME` | `/usr/local/opt/openjdk@17` | emulator scripts, gradle |
| `EMU_AVD_NAME` | `exp_avd` | both emulator scripts |
| `EMU_SYSTEM_IMAGE` | best installed match, else `system-images;android-35;google_apis;<arch>` | `setup_emulator.sh` |
| `EMU_DEVICE` | `pixel_6` | `setup_emulator.sh` |
| `EMU_PORT` | `5554` | `with_emulator.sh` (set per-run for parallelism) |
| `EMU_BOOT_TIMEOUT` | `120` (s) | `with_emulator.sh` — snapshot boot |
| `EMU_COLD_BOOT_TIMEOUT` | `300` (s) | `setup_emulator.sh` — first-time boot |
| `EMU_LOCK_TIMEOUT` | `1800` (s) | `with_emulator.sh` — max wait for emulator lock |
| `EMU_CMDLINE_BUILD` | `11076708` | `setup_emulator.sh` — Google cmdline-tools version |

---

## 12. Common troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `run_experiment.sh` exits immediately with "PAUSE FILE detected" | Stale `experiment/PAUSE` from a prior run | `rm experiment/PAUSE` |
| `ERROR: Another run is active (PID X)` | Stale lock or genuine concurrent run | `bash check_lock.sh` to disambiguate; `rm experiment/.lock` if stale |
| `setup_emulator.sh` hangs after "Boot complete" | Emulator process won't exit cleanly | Wait up to 5 min for the 300s shutdown timeout to SIGKILL; if longer, `kill` the bash PID |
| `with_emulator.sh` times out waiting for lock | Previous invocation crashed without releasing | `rm -rf /tmp/days_in_office_emulator_*.lock.d` |
| `connectedAndroidTest` fails with `Unknown command-line option '--tests'` | The `--tests` filter isn't supported on `connectedAndroidTest` | Use `-Pandroid.testInstrumentationRunnerArguments.package=...` instead |
| `sdkmanager not found` | Cmdline-tools never installed | Re-run `setup_emulator.sh` — it auto-installs them now |
| `run.json` missing or empty | Run was killed before first task started | Just re-run `run_experiment.sh`; it will re-initialise |
| TASK-003 passes with `output: "SKIPPED_NO_DEVICE"` | Old QA wrapper bug, should not occur post-fix | Verify `run_qa.sh` no longer contains the `SKIPPED_NO_DEVICE` branch |

---

## 13. File layout reference

```
ROOT/
├── ARCHITECTURE.md, TASKS.md, CLAUDE.md     ← seed docs, copied per run
├── experiment/
│   ├── .lock                                ← active-run lock (tool/model/pid)
│   ├── PAUSE                                ← create to halt between tasks
│   ├── run_state.json                       ← live orchestrator state
│   ├── env_snapshot.json                    ← host env at first run
│   ├── RUNBOOK.md                           ← this file
│   ├── RUNS_LOG.md                          ← chronological history of runs
│   ├── skeleton/                            ← pre-seeded Gradle files
│   └── scripts/                             ← all orchestrator + helper scripts
└── runs/<tool>/<model>/                     ← per-run git repo, branch = run/<tool>/<model>
    ├── app/, gradle/, *.kts                 ← what the LLM writes
    └── experiment/
        ├── run.json                         ← structured per-task results
        ├── run.log                          ← full orchestration log
        └── TASK-XXX/
            ├── attemptN_prompt.txt
            ├── attemptN_failure.txt
            ├── attemptN_session.json
            └── attemptN_usage.json
```
