# Experiment Findings: Autonomous LLM Coding on a Real Android Project

**Date range:** 2026-05-09 to 2026-05-18
**Task plan:** `TASKS.md` — 11 tasks (TASK-001 through TASK-010 + TASK-021) covering Android project skeleton, data layer (Room + Robolectric), domain layer, detection subsystem, WorkManager orchestration, and a release smoke suite executed on an emulator. ~3,000 lines of production code expected across the plan.
**Hardware:** Intel Mac (host), Linux GPU server with RTX 3090 Ti (24 GB VRAM, ~5–10 tok/s on 27B-class local models).
**Companion docs:** `RUNS_LOG.md` (chronological run-by-run record incl. all 17 infrastructure bugs found and fixed), `RUNBOOK.md` (operator guide).

---

## 1. Executive summary

- **Frontier cloud model is the only configuration that ships this experiment as designed.** Claude (claude-sonnet-4-6 via Claude Code CLI) completed 10/11 tasks first-try across 9 of those tasks; the single TASK-021 failure was a mix of orchestrator bugs and one real WorkManager test that needed iteration.
- **Every local model in the lineup hit a specific, identifiable Android-knowledge ceiling within the first 1-3 tasks.** Not a tool problem, not an edit-format problem. The bottleneck is the model weights' knowledge of Android-specific framework APIs (Material 3 theme names, Robolectric's JUnit-4-only constraint).
- **Tool choice mattered less than expected for outcomes.** Holding model constant (gemma4:31b), aider and goose both passed TASK-001/002 and both failed TASK-003 — different specific symptoms, same wall.
- **Adapter health varied wildly across tools.** The orchestrator's tool adapters (`run_*.sh`) ranged from "just works" (claude, aider) through "one flag stale" (goose) to "four versions out of date, needs rewrite" (openhands).
- **17 distinct infrastructure bugs were discovered and fixed during the experiment.** Each one previously hid a real LLM behavior or made a failure look like something it wasn't (`SKIPPED_NO_DEVICE` masked a kapt-vs-ksp QA error for weeks; `kaptDebugKotlin` in QA scripts blocked every model at TASK-004 regardless of capability).

---

## 2. Combinations tested

11 tools × models combinations attempted across the experiment. Bold = the canonical result for that combination (after all fixes were in place; earlier failures are documented but should not be treated as evidence of model capability).

| # | Tool | Model | Date | Result | Notes |
|---|---|---|---|---|---|
| 1 | aider | qwen2.5-coder:32b | 2026-05-09 | infrastructure bug | Setup completed, no tasks recorded |
| 2 | aider | gemma4:31b (run 1) | 2026-05-10 | infrastructure bug | Multiple early bugs |
| 3 | aider | gemma4:31b (run 2) | 2026-05-10 | infrastructure bug | `FAILURE_REASONS[-1]` crash |
| 4 | aider | gemma4:31b (run 3) | 2026-05-10 | TASK-001 failed × 10 (~8.5h) | First infra-clean run; model couldn't write `pluginManagement` correctly |
| 5 | aider | devstral | 2026-05-10 | infrastructure bug | Killed early |
| 6 | aider | gemma4:31b (baseline) | 2026-05-11 | **2/11 done, false-positive at TASK-003** | TASK-003 "passed" because `SKIPPED_NO_DEVICE` masked the real `--tests` flag error |
| 7 | claude | claude-sonnet-4-6 (pre-fix) | 2026-05-16 | 2/11 done, killed | Stalled on TASK-003 (`--tests` flag on `connectedAndroidTest` — bug 7) |
| 8 | **claude** | **claude-sonnet-4-6 (post-fix)** | **2026-05-17** | **10/11 done, TASK-021 FAILED (10 att)** | ~2h 40m wall-clock. TASK-001-010 all DONE (8 of 10 first-try). TASK-021 fell to orchestrator bugs 9-11 + 1 real WorkManager test failure + TOOL_ERROR cascade on attempts 6-10 |
| 9 | aider | gemma4:31b (re-run) | 2026-05-17 | 2/11 done, stopped on TASK-003 (att 9) | `SearchReplaceNoExactMatch` errors — model guessing at JUnit 4 / 5 imports |
| 10 | aider | qwen2.5-coder:32b | 2026-05-17 | VRAM overflow, killed | 31 GB Q4 model doesn't fit 24 GB VRAM; ~24% spilled to CPU |
| 11 | aider | qwen2.5-coder:14b | 2026-05-17 | TASK-001 spiraled (7 att, killed) | Material 3 theme hallucination; attempts 6-7 drifted off-scope into `gradlew.bat` |
| 12 | openhands | gemma4:31b (× 4) | 2026-05-17 | 4 consecutive adapter bugs, deferred | 0 LLM invocations across all 4 runs |
| 13 | **goose** | **gemma4:31b** | **2026-05-17/18** | **2/11 done, TASK-003 FAILED (10 att, ~3.5h)** | gemma4 hallucinated `@ExtendWith(RobolectricExtension::class)` (JUnit 5) instead of spec'd `@RunWith(RobolectricTestRunner::class)` (JUnit 4) |
| 14 | goose | devstral (× 2) | 2026-05-18 | TASK-001 failed × 10 (~220s each) | Devstral can't emit goose's MCP tool calls — emitted prose, 0 files written |
| 15 | **aider** | **devstral** | **2026-05-18** | **TASK-001 failed × 10 (44 min)** | Engaged via SEARCH/REPLACE successfully but hit the **same Material 3 theme hallucination** as qwen2.5-coder:14b |

**Canonical, post-fix runs to compare** (bold rows above): claude × sonnet-4-6, goose × gemma4, aider × devstral. The pre-fix runs are diagnostic artifacts only.

---

## 3. Headline finding: local models have specific Android knowledge ceilings

The most reproducible result of the experiment is that **each local model fails at a different specific Android-framework API knowledge gap**, not at general programming capability.

### 3.1 Material 3 theme hallucination (TASK-001)

Two of three code-tuned local models tested failed TASK-001 in exactly the same way:

```
error: resource style/Theme.Material3.DayNight.NoActionBar
       (aka com.carvalhorr.daysInOffice:style/Theme.Material3.DayNight.NoActionBar) not found.
error: style attribute 'attr/colorPrimary' not found.
error: style attribute 'attr/colorOnPrimary' not found.
error: style attribute 'attr/colorPrimaryContainer' not found.
... [8 attrs in total]
```

Affected models: **qwen2.5-coder:14b**, **devstral** (~23B).

The model writes `themes.xml` with parent style `Theme.Material3.DayNight.NoActionBar`. This style does not exist in Material 3. The legacy Material / AppCompat themes have `Theme.AppCompat.DayNight.NoActionBar`; Material 3 split the day-night and NoActionBar dimensions and uses different style hierarchies. The model has learned the Material 2 pattern and is mis-applying it.

Once the parent style is invalid, every attr reference inside the style fails to resolve, producing the cascade of 8 errors. The model often "fixes" subsequent attempts by changing things downstream (different attr names, different child resources) instead of fixing the parent style.

This failure was identical across **two model families** (Qwen and Mistral), across **two tools** (aider, goose) for devstral, and consistent across **all 10 retry attempts per run** even with the failure output included in retry prompts.

### 3.2 Robolectric / JUnit 4 vs JUnit 5 confusion (TASK-003)

The general-purpose model **gemma4:31b** passes TASK-001/002 cleanly (it knows the Material 3 syntax), then fails TASK-003 in a different specific way:

```kotlin
// What gemma4 writes (incorrect):
@ExtendWith(RobolectricExtension::class)
class DayRecordDaoTest { ... }
// → Unresolved reference 'RobolectricExtension'

// What TASK-003's spec explicitly tells the model to use:
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayRecordDaoTest { ... }
```

Robolectric only supports JUnit 4 even in 2026. The project uses JUnit 5 platform (`useJUnitPlatform()`), so the model "sees" JUnit 5 everywhere and assumes Robolectric must integrate via JUnit 5's `@ExtendWith(SomeExtension::class)` pattern. It hallucinates `RobolectricExtension` (a class that does not exist) instead of using the JUnit 4 `@RunWith` pattern.

The spec text for TASK-003 explicitly documents the required pattern, including the exact code snippet. gemma4 ignored the spec across all 10 attempts — across both aider (`SearchReplaceNoExactMatch` errors trying to swap imports) and goose (`compileDebugUnitTestKotlin FAILED: Unresolved reference 'RobolectricExtension'`). 

Same model, same task, two tools, identical failure root cause. **The retry loop with failure output included was insufficient to push the model toward the correct API.**

### 3.3 Pattern

| Model | Type | Hits wall at | Specific gap |
|---|---|---|---|
| qwen2.5-coder:14b | code-tuned, 14B | TASK-001 | Material 3 theme parent names |
| devstral | code-tuned, 23B | TASK-001 | Material 3 theme parent names (same as above) |
| gemma4:31b | general, 27B | TASK-003 | Robolectric is JUnit 4 only |
| claude-sonnet-4-6 | frontier cloud | TASK-021 (only) | One real WorkManager smoke test |

Frontier cloud models have more recent and more thorough Android-specific training. Local code-tuned models in the 14-23B range have visible Android-framework knowledge gaps that retry-with-failure-context cannot work around — they don't *know* the right API, and supplying error messages doesn't teach them.

---

## 4. Tool comparison (model held constant)

Holding the model at gemma4:31b across two well-supported tools:

| | aider × gemma4:31b (2026-05-17) | goose × gemma4:31b (2026-05-17/18) |
|---|---|---|
| TASK-001 | DONE (3 attempts, post-bug-12 fix) | DONE (1 attempt) |
| TASK-002 | DONE (1 attempt) | DONE (1 attempt) |
| TASK-003 | Killed on attempt 9 — `SearchReplaceNoExactMatch` (edit format failure: model guessing at file contents) | FAILED after 10 attempts — `compileDebugUnitTestKotlin FAILED: Unresolved reference 'RobolectricExtension'` (real code failure) |
| Wall-clock | ~2 hours | ~3.5 hours |
| Per-attempt wall-clock on TASK-003 | ~30-60 min (aider edits, orchestrator runs gradle) | ~20-25 min (goose itself runs gradle inside the agent loop, possibly multiple times per attempt) |

**What this comparison shows:**

1. **Both tools cleared the same two tasks** (1 and 2) with the same model. Tool choice did not produce different outcomes there.
2. **Both tools hit the same task as a wall** (3). Different specific symptoms, same underlying issue (model doesn't know the right Robolectric API).
3. **The hypothesis "maybe aider's SEARCH/REPLACE edit format was the actual bottleneck on TASK-003"** (motivated by the `SearchReplaceNoExactMatch` errors in run 9) **was falsified by goose's run** — goose uses whole-file rewrites via its `developer` extension, no diff format, and hit the same wall.
4. **Wall-clock economics differ.** Goose's internal develop-test-fix loop runs more gradle builds per "attempt" than aider does, costing more wall-clock for the same outcome. On easier tasks this enabled faster success (TASK-001 in 1 attempt vs aider's 3); on the failing TASK-003 it produced longer churn without convergence.

### 4.1 Architectural difference between aider and goose

| | Aider | Goose |
|---|---|---|
| Per-tool-invocation behavior | Make file edits via SEARCH/REPLACE, exit | Make edits, run gradle, see errors, fix, repeat, exit |
| Edit mechanism | SEARCH/REPLACE blocks in chat (text-only) | Function/tool-call API ("developer__text_editor") |
| Orchestrator-side QA | Runs `./gradlew assembleDebug` etc. *after* aider exits | Goose itself runs gradle inside its loop; orchestrator runs it *again* afterward for QA |
| Effective LLM calls per orchestrator "attempt" | 1 (with internal retries inside aider for failed SEARCH/REPLACE) | Many (goose's agent loop can iterate dozens of times) |
| Tool format compatibility | Wide (any chat model) | Narrow (needs MCP/function-call training) — devstral failed because of this |

This explains why devstral × goose produced zero file edits (devstral can't emit MCP calls) while devstral × aider produced real iteration (aider's text format works with any model).

---

## 5. Model comparison (tool held constant)

Holding the tool at aider, after all 17 infrastructure bugs were fixed:

| Model | TASK-001 | TASK-002 | TASK-003 | TASK-004+ | Wall clock total |
|---|---|---|---|---|---|
| gemma4:31b | DONE (3 att) | DONE (1 att) | Killed on att 9 (SEARCH/REPLACE) | — | ~2 h before kill |
| qwen2.5-coder:14b | Killed att 7 (Material 3) | — | — | — | ~10 min |
| qwen2.5-coder:32b | VRAM overflow | — | — | — | (couldn't start) |
| devstral | Failed × 10 (Material 3) | — | — | — | 44 min |
| claude-sonnet-4-6 (cloud) | DONE (1) | DONE (1) | DONE (1, 15 min) | TASK-004-010 all DONE (8 first-try, 1 in 3 att) | ~2 h 40 m for 10 tasks |

**What this comparison shows:**

1. **Cloud frontier is in its own league for this task.** Single-try success across 9 of 10 tasks, with a single specialized retry chain on TASK-004 (which needed an orchestrator-fix anyway).
2. **Among local models, gemma4:31b got farthest** — it has just enough Android-themes knowledge to clear TASK-001 (where the code-tuned models failed) but gets stuck on Robolectric.
3. **Code-tuned models (qwen-coder, devstral) failed harder than the general-purpose gemma4** on this specific task plan. Both code-tuned models hit the Material 3 wall on TASK-001, while gemma4 cleared it. Likely the code-tuned datasets over-index on general code patterns and under-index on Android-specific framework boilerplate, while a general model with more breadth has better coverage of those Android specifics.
4. **The "bigger = better" assumption isn't borne out at the local end.** qwen2.5-coder:32b couldn't fit in 24 GB VRAM (spillover destroys throughput); qwen2.5-coder:14b was fast but hit Material 3. Going up to the 32B variant didn't fix the Material 3 problem in any meaningful way — it just changed the failure mode from "wrong answer" to "can't run".

---

## 6. Resource management findings

### 6.1 VRAM economics on 24 GB

| Model | Disk | VRAM at load | Fit |
|---|---|---|---|
| gemma4:31b (Q4) | ~17 GB | ~17 GB | clean, with headroom for KV cache |
| qwen2.5-coder:14b (Q4) | ~9 GB | 13.3 GB (with KV) | clean, generous headroom |
| qwen2.5-coder:32b (Q4_K_M) | 31.4 GB | 23.9 GB on GPU, 7.5 GB on CPU | overflow, throughput collapsed |
| devstral (Q4_K_M, 128K ctx) | ~13 GB | **21.6 GB** | tight — context window is the culprit, not the weights |
| claude-sonnet-4-6 | — | — | cloud, irrelevant |

**Key takeaway:** the model weights aren't the only consumer of VRAM. Devstral defaults to a 131,072-token context window, and the KV cache for that window adds ~8 GB on top of the ~13 GB weight footprint. **Context window choice can double or triple a model's effective VRAM use** — this matters more than people often realize when shopping by parameter count.

The diagnostic command `curl /api/ps | jq '.models[].size_vram'` (compared with `size`) gives an immediate signal of whether a model is spilling to CPU. **Spillover is catastrophic** — partial CPU offload made qwen2.5-coder:32b functionally unusable despite being a "fits in VRAM" 32B model on paper.

### 6.2 Throughput observations

- **Cloud Claude:** ~one task per 10-15 min on average. Single-attempt successes were 2-15 min depending on task complexity.
- **Local 27B (gemma4) on RTX 3090 Ti:** ~13 min per TASK-001 attempt (goose), ~30-60 min per TASK-003 retry attempt. Local is roughly **5-10× slower wall-clock** than cloud, sometimes worse.
- **The compounding effect on retries:** the orchestrator allows 10 attempts per task. At 30 min per attempt, a single failing task is **5 hours of wall-clock** before final FAILED. This is what made goose × gemma4 take 3.5 hours just to fail TASK-003.

---

## 7. Adapter health

Each tool in the lineup ships with an adapter (`experiment/scripts/run_<tool>.sh`) that wires the orchestrator to the tool's actual CLI. Their condition was wildly different:

| Adapter | Bugs found | Required action | Status |
|---|---|---|---|
| `run_claude.sh` | 0 | none | works as-is |
| `run_aider.sh` | 0 | none | works as-is |
| `run_goose.sh` | 1 (bug 17) | one flag rename (`-- "$prompt"` → `--text "$prompt"`) and drop a stale `--with-extension` | works after one small edit |
| `run_openhands.sh` | 4 (bugs 13-16) | CLI flags + state mount + runtime image + docker-in-docker build network | **needs full rewrite — deferred** |

The pattern: well-maintained / actively-used tools have current adapters; less-used adapters drift. Critically, **each bug had to be fixed (or in openhands' case, four had to be fixed) before the underlying model could even be evaluated**. Without these fixes the runs failed in seconds with cryptic exit codes that looked like model failures to the casual observer.

Full bug list is in `RUNS_LOG.md` Infrastructure Fixes table (currently 17 entries).

### 7.1 OpenHands: deliberately deferred

After 4 consecutive adapter bugs in a single session with 0 LLM invocations, OpenHands was treated as a separate rewrite project. The adapter targets an OpenHands version that has since changed in four orthogonal ways:

1. CLI flags (`--workspace-dir` → `-d`, removed `--headless`, removed `--no-browser-actions`)
2. State directory (no longer auto-created, needs explicit mount)
3. Sandbox runtime image (auto-versioned vs `:latest` is broken)
4. On-demand runtime build (network not available in nested docker)

The lesson: don't pin to `:latest` for Docker-based tools; pin to specific versions and update deliberately. Maintain a smoke test for the adapter independently of the experimental rig.

---

## 8. The 17 infrastructure bugs: bias-correction work

Without the bug fixes, the experiment would have produced **false positives** that misled conclusions. The most consequential:

- **Bug 12** (`SKIPPED_NO_DEVICE` silent skip in `run_qa.sh`): caused gemma4's TASK-003 to appear DONE in the 2026-05-11 baseline. With this fix, gemma4's TASK-003 is correctly recorded as a real failure.
- **Bug 7** (`--tests` flag on `connectedAndroidTest`): blocked every model at TASK-003 regardless of capability. Fix changed TASK-003 to JVM Robolectric tests, sidestepping the issue entirely.
- **Bug 8** (`kaptDebugKotlin` vs `kspDebugKotlin`): blocked every model at TASK-004. The project is KSP-only; the QA script asked for kapt. Without this fix, no Hilt-module task would have passed for any model.
- **Bug 13-16** (OpenHands adapter): made openhands evaluation impossible until adapter rewrite.

These bugs typically present as "model failed" but are actually QA-script or adapter-script bugs. **Spending time on bug-finding before declaring model rankings is essential** — without it, comparisons are between random orchestrator quirks, not actual model behaviors.

---

## 9. Practical recommendations

### For background / autonomous coding work today

| Use case | Recommendation | Reasoning |
|---|---|---|
| Highest success rate, cost not the bottleneck | Claude Code CLI + claude-sonnet-4-6 | 10/11 done, simplest adapter, fewest moving parts. The bar to clear. |
| Budget-sensitive but cloud-OK | aider + Claude (or GPT-4-class) | aider's diff format is cheaper per task; works fine with frontier models |
| Strictly local / private | aider + gemma4:31b *with heavy pre-seeding* | gemma4 has the broadest knowledge among tested local models; pre-seed `themes.xml`, theme XMLs, and anything boilerplate-heavy. Expect intervention. |
| Investigative / one-task-deep | Any tool with a frontier cloud model | Local models hit task-specific knowledge walls reliably |
| CI / unattended | Claude Code CLI | Smallest adapter surface area, least likely to break across runs |

### For this experimental rig specifically

1. **Pre-seed `themes.xml` and `colors.xml`** in the skeleton (alongside the existing Gradle config pre-seeding). This would unblock TASK-001 for qwen-coder and devstral, allowing the experiment to actually measure their downstream capability instead of stopping at theme syntax.
2. **Add a "smoke check" for adapter health** to `setup_tool.sh` — send one trivial task through the adapter and verify a non-zero result before considering the tool "READY". Would have caught bugs 13-17 in seconds instead of full experimental runs.
3. **Reduce devstral's context window** via a Modelfile or per-request option (probably to 16K-32K) so it stops eating 8 GB of VRAM for a context size we never use.
4. **Pin Docker images to versions, never `:latest`** — applies to openhands main and runtime images, also to any future Docker-based tool adapters.
5. **Add UI tasks (11-20) to the plan.** The plan currently ends with data/domain/detection + smoke; running the resulting app shows a blank screen. Until those are filled in, no run produces a visually-usable app — including Claude's "10/11 done" success. See known plan gap.
6. **Add per-attempt time visibility to the viewer.** The viewer currently doesn't show per-attempt durations; the data is in `run.json`. Adding it would make patterns like "goose runs ~13 min per attempt vs aider's ~5 min" visible at a glance.

---

## 10. Open questions / future work

1. **Does pre-seeding `themes.xml` change the qwen/devstral story?** The Material 3 hallucination might be the only Android-knowledge gap they have, in which case they'd progress comparably to gemma4. Or they might hit a second knowledge gap. Worth testing.
2. **Does gemma4 + Robolectric work if the spec puts the JUnit 4 requirement in **bold** at the top of the task body?** The current spec mentions it in the implementation details. Hard to know if the model would honor a stronger framing.
3. **Does `--edit-format whole` rescue any of the SEARCH/REPLACE failures?** We applied a discipline-reminder prompt fix (template Sec 4 / Sec 5 additions); we did not test the `whole` fallback. With qwen-coder/devstral apparently bottlenecked on theme syntax (not edit format), there's no clear test case until we get past TASK-001.
4. **What does aider × claude-sonnet-4-6 look like?** We've tested claude with the Claude Code CLI but not with aider. Same model, aider's diff format vs Claude Code's tool-call format. Would isolate "format" from "model" within the Claude family.
5. **OpenHands after rewrite.** If the openhands adapter is rewritten against the current API, does it perform comparably to claude/aider on the same model? Independent question, separate work.
6. **Smaller cloud models.** We didn't test claude-haiku-4-5 or other small cloud models. They'd give a cheaper-but-capable middle ground worth investigating.

---

## 11. Reproducibility

All runs preserved as git refs in this repo:

- **Active run branches:** `run/<tool>/<model>` — current state, continuously fetched from the run worktree during runs.
- **Archive refs:** `archive/<tool>/<model>/<date>[-suffix]` — frozen snapshots, will survive `reset_run.sh`. Each contains:
  - The full code state at the end of the run
  - `experiment/run.json` — structured per-task results
  - `experiment/run.log` — full orchestration log
  - `experiment/TASK-XXX/attemptN_{prompt,failure,session,usage}.{txt,json}` — per-attempt artifacts

Listed archives as of 2026-05-18:
```
archive/aider/devstral/2026-05-18-pre-fix
archive/aider/gemma4-31b/2026-05-17
archive/aider/gemma4-31b/2026-05-17-stopped
archive/aider/qwen25coder-14b/2026-05-17-spiraled
archive/aider/qwen25coder-32b/2026-05-17
archive/aider/qwen25coder-32b/2026-05-17-vram-overflow
archive/claude/sonnet-4-6/2026-05-17
archive/claude/sonnet-4-6/2026-05-17-completed
archive/goose/devstral/2026-05-18-no-tool-calls
archive/goose/gemma4-31b/(via run branch)
archive/openhands/gemma4-31b/2026-05-17-adapter-bug
archive/openhands/gemma4-31b/2026-05-17-jwt-perm
archive/openhands/gemma4-31b/2026-05-17-runtime-image
archive/openhands/gemma4-31b/2026-05-17-apt-build
```

Re-execution: `bash experiment/scripts/setup_run.sh <tool> <model>` + `bash experiment/scripts/run_experiment.sh <tool> <model> <ollama_model_id>`. See `RUNBOOK.md` for full operator instructions.

---

## 12. Closing thoughts

This experiment ended up being **as much an experiment about experiment infrastructure as about model capability**. Without the 17 bugs that needed finding and fixing, the conclusions would have been:

- "TASK-003 doesn't work for anyone" (it was a QA script bug)
- "OpenHands is broken" (it was an adapter mismatch)
- "Gemma4 passed TASK-003" (it didn't — `SKIPPED_NO_DEVICE` lied)

Several findings about **model capability** are robust and worth carrying forward:

- Cloud frontier (Claude 4.6 class) handles autonomous Android coding well; local 14-30B models do not, regardless of code-tuning.
- The bottleneck for the local models is **specific Android framework knowledge**, not general coding or reasoning capability. The Robolectric+JUnit 4 / Material 3 examples are concrete and reproducible.
- Tool choice matters less than expected once adapters work. Edit format (diff vs whole vs tool calls) matters mainly for *whether the model engages at all* — once engaged, the same model hits the same walls.
- Model-side training freshness on the **specific framework being used** is the largest predictor of success. Newer / more thorough Android training in a model's weights would likely outperform a larger model with stale Android coverage.
