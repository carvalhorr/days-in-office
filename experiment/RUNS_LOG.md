# Experiment Runs Log

This document records findings, infrastructure fixes, and per-run analysis from all experiment executions. It is updated before starting each new run so the next executor starts from an honest baseline.

---

## Infrastructure Fixes Log

Bugs discovered and fixed across early runs. All fixes are in the scripts and apply to every future run.

| # | Bug | Symptom | Fix | Commit |
|---|-----|---------|-----|--------|
| 1 | `measure_metrics.sh` stdout bleed | Gradle output mixed into `METRICS_JSON` → `JSONDecodeError: Expecting value` after every task | Added `> /dev/null` to all three Gradle subshell commands | — |
| 2 | `FAILURE_REASONS[-1]` bash 3.2 incompatibility | `bad array subscript` / `unbound variable` crashing script on every QA failure — macOS ships bash 3.2 which does not support negative array indices | Replaced with `$CURRENT_OUTCOME` variable set at the point of assignment | — |
| 3 | `TASKS.md` statuses not reset on setup | `setup_run.sh` copied main repo `TASKS.md` as-is; if it contained `DONE` statuses from prior editing, fresh runs silently skipped those tasks | `setup_run.sh` now runs a Python one-liner to reset every `**Status:**` to `NOT_STARTED` after copying | — |
| 4 | `tasks_total` hardcoded as 20 | `run_state.json` always reported 20 tasks regardless of actual TASKS.md content | `write_state()` now counts `**Status:**` occurrences dynamically | — |
| 5 | Retry prompt had no attempt history | Model had no memory of previous attempts; it repeated the same broken approach on every retry | New `retry_prompt.txt` includes a `{{ATTEMPT_HISTORY}}` block — one-line summary per prior attempt — and a "do not repeat a failed approach" instruction | — |
| 6 | Viewer only showed completed tasks | Sidebar was populated from `run.json` which only has finished tasks; in-progress and queued tasks were invisible | `viewer.py` now parses `TASKS.md` and returns a full `all_tasks` list; `viewer.html` uses this as the sidebar source | — |
| 7 | TASK-003 QA used `connectedAndroidTest --tests "..."` | `Unknown command-line option '--tests'` — the `--tests` filter only works on `Test` task subclasses, not `DeviceProviderInstrumentTestTask`. Previously masked by `SKIPPED_NO_DEVICE` short-circuit (bug 12) — gemma4's TASK-003 "DONE" was actually a silent skip | TASK-003 converted to Robolectric (JVM tests under `src/test/`, runs via `testDebugUnitTest` — no emulator needed) | — |
| 8 | TASK-004 QA called `./gradlew kaptDebugKotlin` | `Task 'kaptDebugKotlin' not found. Some candidates are: 'kspDebugKotlin'.` Project uses KSP, no kapt plugin. Unfixable from model side — blocked gemma4 across 7 retries and claude across 2 retries | Replaced all `kaptDebugKotlin` references in `TASKS.md` and `CLAUDE.md` with `kspDebugKotlin` (3 occurrences) | — |
| 9 | `parse_tasks.py` didn't honor `\` line continuations | TASK-021's two-line QA command was split into 3 separate "commands" (incl. a leading `-Pandroid...` flag treated as a standalone exe). Caused 3 spurious failures on TASK-021 | Added `re.sub(r'\\\n\s*', ' ', raw)` before splitlines in `_extract_qa_commands` | — |
| 10 | `parse_tasks.py` required ` ```bash ` immediately after QA header | Any prelude text (notes, prerequisites) between `#### QA Verification Steps` and the bash block silently dropped the entire QA block — returned empty command list | Relaxed regex to `#### QA Verification Steps\n.*?```bash\n(.*?)```` with DOTALL | — |
| 11 | Relative paths in QA commands didn't resolve | TASK-021 QA spec had `experiment/scripts/with_emulator.sh` (relative); orchestrator runs QA from `$WORK_DIR = runs/<tool>/<model>/`, so this resolved to a non-existent path. `timeout: failed to run command 'experiment/scripts/...': No such file or directory` | TASK-021 QA path changed to `../../../experiment/scripts/with_emulator.sh` (run dir → main repo) | — |
| 12 | `run_qa.sh` silently skipped `connectedAndroidTest` when no device | `output: "SKIPPED_NO_DEVICE", passed: true` — made any task with instrumented-test QA appear to pass when no device was attached. Created false-positive DONEs (gemma4 TASK-003) and hid bug 7 for weeks | Removed the SKIPPED_NO_DEVICE branch; TASK-021 wraps its QA in `experiment/scripts/with_emulator.sh` which guarantees a booted device or fails loudly | — |
| 13 | `run_openhands.sh` used obsolete OpenHands CLI flags | `main.py: error: unrecognized arguments: --workspace-dir /opt/workspace_base --no-browser-actions --headless` — every attempt failed in ~7s with exit 2 from argparse, before any LLM call. Run completed in 70s with 0/11 done, but no model was ever invoked | Replaced `--workspace-dir <path>` with `-d <path>`; removed `--no-browser-actions` and `--headless` (default behavior in current openhands handles both). Archive `archive/openhands/gemma4-31b/2026-05-17-adapter-bug` preserves the failure | — |
| 14 | `run_openhands.sh` had no writable state mount | `PermissionError: [Errno 13] Permission denied: '/.openhands/.jwt_secret'` — every attempt failed in ~7s with exit 1. HOME unset inside container → Python expanded `~/.openhands` to `/.openhands` (root of FS, unwritable). Run completed in 73s with 0/11 done, no model invoked. Surfaced after bug 13 was fixed | Added `STATE_DIR=$(mktemp -d -t openhands-state-XXXXXX)` and `-v "$STATE_DIR:/.openhands"` to the docker run. EXIT trap cleans up. Replaced `exec` with explicit run + `exit $?` so the trap actually fires. Archive `archive/openhands/gemma4-31b/2026-05-17-jwt-perm` | — |
| 15 | `run_openhands.sh` pinned sandbox runtime to `runtime:latest` | `docker.errors.APIError: 400 ... runc create failed: micromamba: no such file or directory`. OpenHands main launches a second "runtime sandbox" container; the `:latest` tag for `ghcr.io/all-hands-ai/runtime` drifts out of sync with main, so the expected `/openhands/micromamba/bin/micromamba` entrypoint is absent. All 10 attempts ~8s each, exit 1. Run finalized 80s, no model invoked. Surfaced after bug 14 fix | Dropped `-e SANDBOX_RUNTIME_CONTAINER_IMAGE` entirely so openhands main selects the runtime image internally (it knows which version matches itself). Archive `archive/openhands/gemma4-31b/2026-05-17-runtime-image` | — |
| 16 | OpenHands on-demand runtime build fails with `apt-get update` exit 100 | Without a pinned runtime image (bug 15 fix), openhands tries to `docker build` a custom runtime inside its own container. The build's `apt-get update` returns exit 100 — apt can't reach repositories from the nested build environment (network/DNS issue 3 docker layers deep). All 10 attempts ~8s each. **NOT YET FIXED** — adapter needs full rewrite, see Known Issues. Archive `archive/openhands/gemma4-31b/2026-05-17-apt-build` | (deferred — adapter rewrite project) | — |
| 17 | `run_goose.sh` used obsolete CLI surface | `' found / Usage: goose run [OPTIONS]` — goose 1.33.1's `run` subcommand no longer accepts positional args after `--`. Prompt must be passed via `--text <TEXT>` or `-i <FILE>`. Also `--with-extension computercontroller` used wrong syntax (current `--with-extension` expects a shell command, not just a name). All 10 attempts ~0.4s each, exit 2 from argparse. Run finalized 4s, no model invoked | Replaced `-- "$(cat …)"` with `--text "$(cat …)"`; dropped `--with-extension computercontroller` (extensions are configured globally in `~/.config/goose/config.yaml`). Added `--no-session` for cleaner automated runs. Archive `archive/goose/gemma4-31b/2026-05-17-cli-flags` | — |

---

## Known Issues / TODO

### OpenHands adapter — full rewrite needed (deferred 2026-05-17)

`run_openhands.sh` was written against an old openhands version. In a single session we hit **4 consecutive adapter bugs** (13, 14, 15, 16) — each fix exposed the next layer of API drift. Net result: 4 archive refs, ~5 minutes of wall-clock, **zero LLM invocations**. The adapter needs a clean rewrite targeting current openhands rather than incremental patching.

What a rewrite should address:
- **CLI surface:** openhands CLI flags have all changed (`--workspace-dir` → `-d`, removed `--no-browser-actions`, removed `--headless`). Re-read the current `--help` output, build from scratch.
- **State directory:** mount a per-invocation temp dir at `/.openhands` for JWT secret + session state. Cannot use `exec` for the docker run if you want the EXIT trap to clean up.
- **Runtime image selection:** do NOT pin to `:latest`. Either (a) find the matching `runtime:0.X.Y-nikolaik` tag for the openhands main version and pin to that, or (b) configure docker-in-docker networking so on-demand build's `apt-get update` works (needs Docker Desktop network proxy or `--network=host`).
- **Test the adapter standalone** before re-wiring it into the orchestrator. A simple "run openhands main with a one-line task, capture the output, exit 0" smoke test would have caught bugs 13–16 in seconds each.
- **Pin the OpenHands main image to a specific version**, not `:latest`. `:latest` moves; `0.X.Y` is reproducible.

Treat as a separate piece of work. Don't re-attempt openhands runs against the current adapter — they will fail.

### Orchestrator

- **Detect Claude CLI rate-limit / quota exhaustion as a non-retryable error and pause the run instead of burning all 10 attempts.** Confirmed on 2026-05-18: claude × sonnet-4-6 Phase 4 run sailed through TASK-011 to TASK-018 (8/8 first-try, ~80 min total) then hit the rate limit on TASK-019. All 10 retry attempts produced `TOOL_ERROR (exit 1)` in ~83s each (835s total wasted), with no transcript output. Downstream TASK-020 and TASK-021 were then skipped. This is the same failure pattern observed on the 2026-05-17 run's TASK-021 attempts 6-10 — now confirmed to be rate-limit-related rather than per-prompt fragility.
  - **Fix:** in `run_claude.sh` or `run_experiment.sh`, treat exit code 1 with empty session/transcript as a special "rate limited" signal. Instead of consuming a retry, touch `experiment/PAUSE` and exit cleanly so the operator can resume after the rate window resets.
  - **Even simpler interim mitigation:** if attempt N exits with code 1 within < 30s and produced no session.json, abort that task's retry loop immediately and move on (or pause). Currently the orchestrator happily retries 9 more times producing identical instant failures.
  - **Bonus:** capture `attempt*_session.json` size / content even on failure so we can distinguish "model failed mid-conversation" from "CLI failed before starting".

- **Pause granularity is per-task, not per-attempt.** `run_experiment.sh` only checks `experiment/PAUSE` between tasks. Touching the PAUSE file mid-task still lets the current task burn through up to 10 attempts before stopping — costs real money on cloud runs and hours on local runs. Move the pause check inside the attempt loop (after the QA verdict is recorded but before the next attempt builds its prompt) so an operator can halt within minutes.

### Prompt templates — pending for next run

- **Add SEARCH/REPLACE content-matching discipline to both templates.** The 2026-05-17 gemma4 re-run is burning retries on `SearchReplaceNoExactMatch` errors — model produces syntactically valid SEARCH/REPLACE blocks but the SEARCH content doesn't byte-for-byte match the file (e.g., guessing at JUnit 4 imports that aren't there). Templates don't currently address this. Apply **before** kicking off the next run; do NOT change mid-run.

  Add to `experiment/templates/task_prompt.txt` just before the final "CRITICAL RULE" line:
  ```
  EDIT DISCIPLINE — read carefully, this is the #1 cause of retries:
  Aider applies your changes via SEARCH/REPLACE blocks. The SEARCH section must
  match the file's current contents EXACTLY — every character, every space, every
  blank line. There is no fuzzy matching. Before writing a SEARCH block:
  - If the file already exists, look at its actual content. Do not guess at imports,
    package declarations, or formatting based on what you "expect" to be there.
  - If the file is new (doesn't exist yet), use an empty SEARCH section — do not
    invent placeholder text.
  - Keep each SEARCH block small. The fewer lines in SEARCH, the fewer ways it
    can mismatch.
  - Never paraphrase, reformat, or "tidy up" the SEARCH section. Quote it verbatim.
  ```

  Add to `experiment/templates/retry_prompt.txt` after step 4 (and renumber commit to step 6):
  ```
  5. EDIT DISCIPLINE — if the previous failure was a "SearchReplaceNoExactMatch" error:
     - The SEARCH section you wrote did not byte-for-byte match the file. Aider
       compares character-by-character with zero tolerance for whitespace or
       formatting differences.
     - DO NOT re-send the same SEARCH text. Look at the file's actual current
       contents and emit a SEARCH that matches exactly. Keep it as small as
       possible — fewer lines means fewer mismatch points.
     - If creating a new file, use an empty SEARCH section.
  ```

  If this doesn't measurably reduce retry rates on weak-model runs, the next step is `--edit-format whole` in `run_aider.sh` (eliminates SEARCH/REPLACE entirely at the cost of more tokens per edit).

### Viewer (`experiment/scripts/viewer.py` + `viewer.html`)

- **Attempts not individually expandable.** Each attempt's prompt / failure / session content currently appears inline (or not at all). They should be collapsible cards — closed by default, expand to show prompt + failure trace + session summary on demand. Otherwise a 10-attempt task is unreadable.
- **Auto-refresh fights the scroll position.** Polling re-renders the whole content panel, which snaps the viewport back to the top every refresh. Impossible to read an attempt while the page is live. Fix options: (a) replace `innerHTML = ...` re-render with targeted DOM diff / incremental append so untouched nodes stay put, or (b) preserve `window.scrollY` (and the scroll position of any open attempt) across refreshes.
- **Missing timing data.** Add: (1) run start time at the top of the page; (2) per-attempt elapsed time on each attempt card (start → end, or start → now if in flight). The data is already in `run.json` (`start_time`, `end_time`, `duration_seconds`) — just isn't surfaced.
- **Tasks/attempts don't appear without a manual refresh.** New attempts written to disk during a run aren't reliably picked up by the poll loop. Either the poll interval is too long, the request is cached, or the JSON read happens before the orchestrator's atomic-write completes. Investigate: confirm the fetch URL has cache-busting (`?t=${Date.now()}`), shorten the poll interval to ~2s, and check whether `viewer.py` re-reads `run.json` on every request or caches.
- **Format durations as `hh:mm:ss` instead of raw seconds everywhere they're displayed.** Currently the viewer, `run.log`, and `check_progress.sh` show things like `12665s` or `4405s` which require mental arithmetic to interpret as 3h31m or 1h13m. Apply a uniform formatter (`hh:mm:ss`, or `Hh Mm` if cleaner) to:
  - `viewer.html` — per-task and per-attempt durations
  - `experiment/scripts/run_experiment.sh` — the `✓ TASK-XXX: DONE (Ns, N attempt(s))` lines
  - `experiment/scripts/check_progress.sh` — task durations in the summary
  - `experiment/scripts/generate_report.py` — final report tables
  - The "Detailed Analysis" tables in `RUNS_LOG.md` (manual; not auto-generated)
  Keep `run.json`'s `duration_seconds` as a raw integer — format only at display time. Reusable helper: `python3 -c "import datetime; print(str(datetime.timedelta(seconds=N)))"` produces `hh:mm:ss`; bash equivalent: `printf '%02d:%02d:%02d' $((N/3600)) $(((N/60)%60)) $((N%60))`.

---

## Runs Summary

| Run | Tool | Model | Tasks Done | Tasks Failed | Notes |
|-----|------|-------|-----------|--------------|-------|
| 2026-05-09 | aider | qwen2.5-coder:32b | 0 | 0 | Ollama crashed on attempt 1 of TASK-001; script then hit `FAILURE_REASONS[-1]` bug and aborted |
| 2026-05-10 (run 1) | aider | gemma4:31b | 0 | 0 | Multiple infrastructure bugs; JSONDecodeError from `measure_metrics.sh` after attempt 10 of TASK-001 |
| 2026-05-10 (run 2) | aider | gemma4:31b | 0 | 0 | Script crashed immediately on first QA failure — `FAILURE_REASONS[-1]` bug |
| 2026-05-10 (run 3) | aider | gemma4:31b | 0 | 1 | Infrastructure stable; TASK-001 FAILED across 10 attempts (30,844s total). Stopped manually during TASK-002. |
| 2026-05-11 | aider | gemma4:31b | 3 | 0 (1 false) | After build files pre-seeded. TASK-001/002 DONE legitimately. TASK-003 "DONE" was false positive (bug 12 → silent skip). Stalled on TASK-004 across 7 retries (Hilt module wouldn't compile under KSP) + bug 8 (kapt task name); killed during attempt 8. |
| 2026-05-16 | claude | claude-sonnet-4-6 | 2 | 0 | Stalled on TASK-003 — 5 attempts blocked by bug 7 (`--tests` flag on `connectedAndroidTest`). Killed manually after attempt 4 hit TOOL_TIMEOUT. Archive: `archive/claude/sonnet-4-6/2026-05-17`. |
| 2026-05-17 | claude | claude-sonnet-4-6 | **10** | 1 | First substantial completion. TASK-001–010 all DONE. TASK-021 FAILED after 10 attempts (mixed orchestrator bugs 9/10/11 + 1 real smoke-test failure + TOOL_ERROR cascade). See detailed analysis below. Archive: `archive/claude/sonnet-4-6/2026-05-17-completed`. |
| 2026-05-17 | aider | gemma4:31b | 2 | 0 (stopped) | Re-run after all fixes. TASK-001/002 DONE, killed on TASK-003 attempt 9 with persistent `SearchReplaceNoExactMatch` errors (model guessing at file contents). Archive: `archive/aider/gemma4-31b/2026-05-17-stopped`. |
| 2026-05-17 | aider | qwen2.5-coder:32b | 0 | 0 (killed) | First attempt: 32B Q4_K_M (31 GB) overflowed 24 GB VRAM on RTX 3090 Ti — Ollama split 7.5 GB to CPU, throughput collapsed. Killed during TASK-001 attempt 1 setup. Archive: `archive/aider/qwen25coder-32b/2026-05-17-vram-overflow`. |
| 2026-05-17 | aider | qwen2.5-coder:14b | 0 | 0 (killed) | After switching to smaller variant (fits 13.3 GB / 24 GB cleanly). **Stuck on TASK-001 across 7 attempts**: hand-rolled Material 3 themes referencing non-existent parent `Theme.Material3.DayNight.NoActionBar`. Attempts 2–6 degenerated to ~1-min near-empty responses. Attempt 7 drifted off-scope (started rewriting `gradlew.bat`). Killed. Archive: `archive/aider/qwen25coder-14b/2026-05-17-spiraled`. Note: prompt template SEARCH/REPLACE discipline fix was active — failure mode here was content correctness, not edit format. |
| 2026-05-17/18 | goose | gemma4:31b | 2 | 1 | First goose run after bug 17 fix. TASK-001/002 DONE in 1 attempt each (~13/~7 min). **TASK-003 FAILED after 10 attempts (3.5 hours)** — gemma4 hallucinated `@ExtendWith(RobolectricExtension::class)` (JUnit 5 style) instead of the spec's `@RunWith(RobolectricTestRunner::class)` (JUnit 4 required by Robolectric). Goose ran ~5 gradle builds per attempt (its internal develop-test-fix loop). Confirms model-side bottleneck — same TASK-003 wall as aider×gemma4, different specific symptom. Archive: `archive/goose/gemma4-31b/...` (run branch). |
| 2026-05-18 | goose | devstral | 0 | 1 (2 runs) | devstral × goose fundamentally incompatible. Two identical runs (~210s each, 10 attempts × ~20s). Devstral emits plain prose, not MCP-format tool calls that goose's developer extension needs to make file edits. No code was ever written. Archives: `archive/goose/devstral/2026-05-18-no-tool-calls` (× 2 essentially). |
| 2026-05-18 | aider | devstral | 0 | 1 | aider + devstral DID engage (44 min, 10 attempts × 4.4 min each — real iteration). But devstral hit the **same Material 3 theme hallucination as qwen2.5-coder:14b**: parent style `Theme.Material3.DayNight.NoActionBar` doesn't exist, all `colorPrimary`/`colorOnPrimary`/etc. attrs cascade-fail. 10 attempts of the same failure. Confirms: bottleneck is model's Android knowledge, not tool format. Archive: `archive/aider/devstral/2026-05-18-pre-fix` (the pre-fix old run; current run on `run/aider/devstral` branch). |
| 2026-05-18 | aider | openhands variants | — | 4 (deferred) | Hit 4 consecutive openhands adapter bugs (13–16), 0 LLM invocations. Adapter is 4 API versions out of date, needs full rewrite. Treated as separate project. Archives: `archive/openhands/gemma4-31b/2026-05-17-{adapter-bug,jwt-perm,runtime-image,apt-build}`. |

---

## Detailed Analysis: claude × sonnet-4-6 — 2026-05-17

First end-to-end run under the post-fix orchestrator. 10/11 tasks DONE; TASK-021 (release smoke suite) FAILED. Total wall-clock: ~2h 40m.

### Per-task outcomes

| Task | Status | Attempts | Duration | Notes |
|------|--------|----------|----------|-------|
| TASK-001 | DONE | 1 | 180s | Project skeleton: App + MainActivity + manifest + theme |
| TASK-002 | DONE | 1 | 120s | 8 domain models + ComplianceResultTest |
| TASK-003 | DONE | 1 | 895s | Room DAOs + Robolectric tests. Verified real: 391 files including KSP-generated `*_Impl.java`, full Gradle task list in QA output. **Not** the silent-skip false positive that bug 12 used to produce. |
| TASK-004 | DONE | 3 | 1211s | Repository impls + Hilt modules + DataStore. Attempts 1–2 failed on bug 8 (`kaptDebugKotlin`); bug fixed mid-flight, attempt 3 passed immediately. |
| TASK-005 | DONE | 1 | 451s | Working-days + compliance use cases |
| TASK-006 | DONE | 1 | 807s | Calendar data source + sync use case |
| TASK-007 | DONE | 1 | 271s | Wi-Fi Connected Detector |
| TASK-008 | DONE | 1 | 143s | Wi-Fi Scan Detector |
| TASK-009 | DONE | 1 | 103s | Geofence Detector |
| TASK-010 | DONE | 1 | 918s | Detection Orchestrator + DayDetectionWorker |
| TASK-021 | **FAILED** | 10 | 4405s | See breakdown below |

### TASK-021 failure breakdown (10 attempts, ~73 minutes)

Three distinct phases. Only one was a fair test of Claude's coding ability.

**Phase 1 — attempts 1–3: orchestrator-side bugs**
QA parser split the multi-line `with_emulator.sh ... \\` command on every newline, producing three malformed sub-commands. All three "failures" were `No such file or directory` errors for path fragments. Fixed mid-flight by bugs 9, 10, 11.

**Phase 2 — attempts 4–5: emulator infrastructure proven, one real smoke-test failure**
Significant: the `with_emulator.sh` wrapper booted the AVD from snapshot, ran Gradle's `connectedDebugAndroidTest`, and executed real instrumented tests. Claude wrote **9 smoke tests** (more than the 4 specced). 8 passed. One failure:
```
WorkerSchedulingSmokeTest > whenDayDetectionWorkerEnqueuedItCompletesWithResultSuccess
  java.lang.AssertionError: expected:<SUCCEEDED> but was:<FAILED>
```
`DayDetectionWorker` returned `Result.failure()` under the WorkManagerTestInitHelper harness. Likely root cause (not confirmed): worker depends on Hilt-provided state (e.g. `MandateConfig` from DataStore) that the test didn't seed.

**Phase 3 — attempts 6–10: TOOL_ERROR cascade**
All five consecutive attempts: `Tool exited with code 1`, no transcript or error detail in failure files. The Claude CLI itself aborted before producing output. Persistent (not flaky) — same error 5 times in a row. Most likely candidates, in order of probability:
1. Rate limit / quota throttled (5 dense invocations on top of prior tasks)
2. Prompt size bloat — by attempt 6 the prompt likely contained the full attempt-5 `connectedAndroidTest` log (~5 KB of Gradle task lines)
3. Claude Code CLI bug specific to this input

Not investigated further — `attempt6_session.json` + `attempt6_usage.json` would resolve which.

### Interpretation

The 10/11 headline is real; the 1/11 failure is **partly unfair to Claude**. Of the 10 TASK-021 attempts, only 2 (attempts 4 and 5) actually got past orchestrator bugs and into Claude's hands. The other 8 were either bug-blocked (1–3) or CLI-failed (6–10) before Claude could iterate. A more honest interpretation: Claude got the data layer + domain + detection done first-try across 9 tasks, and got 8/9 smoke tests right on the first real attempt at TASK-021. The single fixable bug (WorkManager test) didn't get the retries it needed because Phase 3 cut things short.

### Notable observations

- **Robolectric conversion worked.** TASK-003's switch from instrumented to JVM tests succeeded first try, verified by 391 files generated and a 15-minute build/test cycle. The previous spec's `connectedAndroidTest` would have been masked by bug 12.
- **Emulator infrastructure proven.** `setup_emulator.sh` + `with_emulator.sh` + snapshot Quick Boot worked end-to-end on this Intel Mac. Booted, ran 9 instrumented tests, shut down cleanly, lock released — all unattended.
- **Mid-flight patching works.** Bug 8 was fixed during TASK-004 attempt 3 by editing the run-local `TASKS.md`. The orchestrator's per-attempt re-parse picked it up immediately. Saved hours vs killing + restarting.
- **No UI tasks were implemented.** Plan only covers data/domain/detection + smoke; tasks 11–20 (UI work) are placeholders in `TASKS.md` and were never written. MainActivity is a single-Text skeleton — running the app shows a blank screen as expected. This is a known plan gap, not a Claude failure.

---

## Detailed Analysis: aider × qwen2.5-coder:14b — 2026-05-17

Killed manually after 7 attempts on TASK-001. Significant because qwen2.5-coder:14b is the strongest code-tuned local model that fits cleanly in 24 GB VRAM (13.3 GB load, 10+ GB free for KV cache) — no infrastructure-side excuse for failure.

### The failure mode: degeneration, not convergence

| Attempt | Duration | Outcome | What aider got back |
|---|---|---|---|
| 1 | 18 min | QA fail | Full themes.xml + manifest + Hilt files; AAPT error on Material 3 parent theme |
| 2 | 1 min | QA fail | Tiny edit; same AAPT error |
| 3 | 1 min | QA fail | Same |
| 4 | 2 min | QA fail | Same |
| 5 | 1.5 min | QA fail | Same |
| 6 | 1.5 min | QA fail | Same |
| 7 | 13 min (killed) | tool error 143 | Model started writing `gradlew.bat` — out-of-scope file, in protected list. SIGKILL'd before completion. |

The persistent error across attempts 1–6:
```
error: resource style/Theme.Material3.DayNight.NoActionBar not found
error: style attribute 'attr/colorPrimary' not found
... [8 more attrs not found]
```

Model is naming a Material 3 parent theme variant that doesn't exist (`Theme.Material3.DayNight.NoActionBar`). In Material 3 the day-night and NoActionBar dimensions are split differently than in Material 2 / AppCompat. The model appears to have learned the Material 2 naming pattern and is applying it to Material 3, which has different style hierarchies. The cascade of "attr not found" is downstream of that one wrong parent.

### Why the retry loop didn't help

Each retry prompt included the full previous failure output. The model never converged because:
1. It doesn't know the correct Material 3 parent name. Adding more "what failed" context doesn't supply the missing knowledge.
2. Attempts 2–6 collapsed to ~1 min each — aider received very short responses that didn't materially change `themes.xml`. The model couldn't generate alternative themes; it was stuck looping.
3. Attempt 7 drifted off-scope into `gradlew.bat` (protected file, explicitly named in the CRITICAL RULE). When a model exhausts the in-scope ideas it knows, it starts wandering — a clear "out of ideas" signal.

### What this means for the experiment

- **The prompt template SEARCH/REPLACE discipline fix was in effect during this run and made no observable difference to qwen14b's outcome.** That's not a failure of the fix — the fix targets one specific failure mode (content-matching) that was less prevalent here than expected. The real bottleneck for qwen14b was content correctness (Material 3 knowledge), not edit format.
- **Pre-seeding the skeleton has limits.** We pre-seeded Gradle config (settings, build, libs.versions) for similar reasons in earlier gemma4 runs. The same logic suggests pre-seeding `themes.xml` and `colors.xml` would unblock qwen14b on TASK-001 — but that erodes the test's meaningfulness, since "create the project skeleton" becomes "fill in `MainActivity.kt`". Worth considering as a deliberate scope reduction for "weak local models", labeled as such.
- **Code-tuned ≠ Android-tuned.** qwen2.5-coder is generally code-tuned but evidently weak on the very-specific Android-XML / Material-theme corner. Future weak-model runs likely fail here regardless.

### Aborted predecessor: aider × qwen2.5-coder:32b — 2026-05-17

Same architecture, larger model, killed on the spot when VRAM overflow was detected. Ollama reported 23.9 GB of 31.4 GB model loaded in VRAM with 7.5 GB on CPU — partial offload to system RAM causes PCIe-bound inference, several × slower than fully-in-VRAM. Diagnosed via `curl /api/ps` showing `size_vram` < `size`. Did not produce any tasks. Archive preserved for the diagnostic record.

This is the first infrastructure-clean run. All script bugs were fixed before it started.

### TASK-001: Android Project Setup — FAILED (10 attempts, ~8.5 hours)

**Attempt breakdown:**

| Attempt | Duration | Outcome | Root error |
|---------|----------|---------|------------|
| 1 | 7200s (timeout) | TIMEOUT | Aider hung — likely hit the 2h cap waiting on model or Ollama |
| 2 | 1089s | QA_FAIL | `Project directory is not part of the build` — model created `settings.gradle.kts` without `include(":app")` or placed it incorrectly |
| 3 | 507s | QA_FAIL | `Plugin [id: 'com.google.dagger.hilt.android.plugin'] was not found` — wrong plugin ID (missing `.gradle` segment) |
| 4 | 1473s | QA_FAIL | `Version catalog: version reference 'coreKtx' doesn't exist` — model used alias names in `libs.versions.toml` that didn't match declarations |
| 5 | 6247s | QA_FAIL | Same version catalog error — no progress despite history |
| 6 | 2316s | QA_FAIL | `Plugin [id: 'com.google.dagger.hilt.android.gradle.plugin'] was not found` — fixed plugin ID but plugin resolution still broken (missing `google()` in `pluginManagement.repositories`) |
| 7–10 | ~2500s each | QA_FAIL | Same Hilt plugin resolution failure — model unable to converge |

**Pattern:** The model fixes one error and regresses elsewhere. It cycles between the plugin ID being wrong and the repository configuration being incomplete. It never stabilises both simultaneously across 10 attempts.

**The specific blocker:** The Hilt Android Gradle plugin requires:
1. `google()` in `pluginManagement.repositories` in `settings.gradle.kts`
2. The exact plugin ID `com.google.dagger.hilt.android.gradle.plugin` in the project-level `build.gradle.kts`
3. These two things to coexist — the model keeps fixing one and breaking the other

**Model self-diagnosis (observed in TASK-002 attempt 3 log):** When working on TASK-002, the model correctly identified that the Hilt plugin resolution failure was a carry-over from TASK-001 and described the exact fix needed ("add `google()` to pluginManagement block"). It understood the problem but could not reliably produce the correct file from scratch.

### TASK-002: Core Domain Models — Incomplete (run stopped)

TASK-002 started (incorrectly) in an earlier aborted run due to `TASKS.md` bug #3 above. It was also showing up because TASK-001 was marked DONE in the main repo TASKS.md. The TASK-002 attempts that existed were all failing on the same Hilt plugin error inherited from the broken TASK-001 build.

---

## Conclusions and Recommended Changes Before Next Run

### What the data shows

1. **TASK-001 is a consistent blocker for gemma4:31b.** Across two complete 10-attempt runs (~17 hours of wall-clock time combined), the model never produced a working Android Gradle project setup. The model understands the problem intellectually but cannot produce the exact configuration reliably.

2. **The failure is task design, not model capability.** The model can write Android Kotlin code (it correctly generated `DaysInOfficeApp`, `MainActivity`, the manifest permissions, and themes). It fails specifically on the Gradle plugin ecosystem — the `pluginManagement` / `repositories` / plugin ID triple that must be exactly right together. This is specialised boilerplate that even experienced Android developers look up.

3. **The retry prompt history improvement had no measurable effect on TASK-001** — the model still cycled through the same errors. This is because the fundamental problem is that generating correct Gradle configuration from prose requires precision the model doesn't have, not reasoning it needs to be reminded of.

4. **Tasks that depend on a broken build are unrunnable.** Every task from TASK-002 onward requires `./gradlew` to work. If TASK-001 fails, the entire run is blocked regardless of model capability on domain code.

### Recommended change before next run

**Pre-seed `settings.gradle.kts` and root `build.gradle.kts` in the skeleton.**

These two files are pure known-correct boilerplate for a Kotlin Android project with this exact dependency set. They never vary across runs. Pre-seeding them:
- Removes the #1 failure mode entirely
- Lets TASK-001 scope down to: `app/build.gradle.kts`, `libs.versions.toml`, source files, and manifest — things the model reliably produces
- Does not pre-solve the experiment; it calibrates the task to the executor's capability (as the experimenter's job)

The files to add to `experiment/skeleton/`:
- `settings.gradle.kts` — with correct `pluginManagement.repositories` (google, mavenCentral, gradlePluginPortal) and `include(":app")`
- `build.gradle.kts` — project-level with all plugin declarations and `apply false`

`setup_run.sh` copies them alongside the Gradle wrapper. The agent receives them as protected read-only files (like `gradlew`) and focuses on the application-level configuration.

This change should be made and verified before running any further tool/model combinations.

---

## Environment Snapshot

Recorded at first run (2026-05-09), consistent across all subsequent runs:

| Component | Version |
|-----------|---------|
| Java | OpenJDK 17.0.19 |
| Ollama | 0.22.1 |
| aider | 0.86.2 |
| Gradle wrapper | 8.10.2 |
| Ollama host | 192.168.68.74:11434 |
| GPU server | RTX 3090 Ti (~5–10 tok/s) |
