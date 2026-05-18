# Why No Local Model Could Complete This Experiment

**Date:** 2026-05-18
**Experiment:** Autonomous LLM coding of a real Android application
**Question:** All five local models tested failed to complete the 21-task plan. Why?

---

## TL;DR

**Cloud frontier (Claude sonnet-4-6) completed all 21 tasks.** Local models (gemma4:31b, qwen2.5-coder:14b/32b, devstral) all failed within the first 1–3 tasks. The cause is not the tool scaffolding, the edit format, the orchestrator's retry policy, or the model size in any naive sense. It is **specific gaps in Android-framework knowledge baked into the model weights** — gaps that the retry loop cannot fill because feeding back failure messages does not teach a model APIs it never learned.

Each local model failed at a different specific knowledge gap:

| Model | Failed at | Specific gap |
|---|---|---|
| gemma4:31b (general, 27B) | TASK-003 (Robolectric Room tests) | Hallucinated `@ExtendWith(RobolectricExtension::class)` (JUnit 5 pattern) — but Robolectric only supports JUnit 4 |
| qwen2.5-coder:14b (code, 14B) | TASK-001 (Material 3 themes) | Hallucinated `Theme.Material3.DayNight.NoActionBar` parent style — this style does not exist in Material 3 |
| devstral (code, 23B) | TASK-001 (Material 3 themes) | Same hallucination as qwen2.5-coder:14b |
| qwen2.5-coder:32b (code, 32B) | could not start | 31 GB model overflowed 24 GB VRAM; 7.5 GB spilled to CPU, throughput collapsed |

Cloud Claude wrote correct code on the first attempt for the same tasks; it has more recent and more thorough Android-specific training in its weights.

---

## 1. The experimental setup

### 1.1 The project

A real Android application: **"Days in Office"** — a productivity app that tracks user attendance against a configurable in-office mandate. Realistic scope, modern stack, no shortcuts. The full architecture is specified in `ARCHITECTURE.md`. Highlights:

- **Language:** Kotlin 2.0
- **Min SDK:** 28 / **Target SDK:** 35 (AGP 8.5.0)
- **UI:** Jetpack Compose (BOM 2024.09.00), Material 3, Navigation Compose
- **Architecture:** MVVM + Clean Architecture, single activity
- **Data:** Room 2.6.1 + DataStore Preferences
- **DI:** Hilt 2.51.1, KSP-based (no kapt)
- **Background:** WorkManager 2.9.1
- **Location:** Google Play Services Location 21.3.0
- **Tests:** JUnit 5 + MockK + Turbine + Robolectric (JVM); AndroidJUnitRunner for smoke (emulator)
- **Visual spec:** A pre-built HTML/CSS prototype (`prototype/index.html`) defines exact screens, layouts, copy, and interactions

### 1.2 The task plan

21 tasks broken into 5 phases:

| Phase | Tasks | Focus |
|---|---|---|
| Phase 1: Project Foundation | TASK-001 – TASK-004 | Android skeleton, domain models, Room database, repositories + Hilt modules |
| Phase 2: Core Business Logic | TASK-005 – TASK-006 | Use cases, calendar data source |
| Phase 3: Detection | TASK-007 – TASK-010 | Wi-Fi connected/scan detectors, geofence detector, orchestrator + WorkManager worker |
| Phase 4: User Interface | TASK-011 – TASK-020 | Material 3 theme, navigation, 4 screens (onboarding/dashboard/calendar/settings), permissions, notifications, widget, polish |
| Phase 5: Release Validation | TASK-021 | Release smoke suite on emulator (real instrumented tests) |

Each task specifies: **Scope** (files to create), **Implementation Details**, **Acceptance Criteria** (checkable list), **QA Verification Steps** (exact shell commands). The orchestrator parses these per task and runs the QA shell commands after the LLM finishes.

### 1.3 The orchestrator

A Bash + Python orchestrator (`experiment/scripts/run_experiment.sh`) drives the loop:

```
for each task in TASKS.md whose deps are DONE:
    for attempt in 1..10:
        render prompt (task body + arch + prior failure context for retries)
        invoke tool (claude / aider / openhands / goose)
        commit any changes
        run QA verification shell commands
        if QA passed: mark DONE, break
    if all 10 attempts failed: mark FAILED, dependents skipped
```

Each task gets up to 10 attempts. Each tool invocation is timeout-capped at 2 hours (cloud rarely hits this; local often does). Run state is persisted to `experiment/.lock` (single-run mutex), `experiment/run_state.json` (live state), and `runs/<tool>/<model>/experiment/run.json` (final per-task data).

### 1.4 Test infrastructure

JVM tests live in `src/test/` (run with `./gradlew testDebugUnitTest`). Room DAO tests use **Robolectric** to run on the JVM with `Room.inMemoryDatabaseBuilder` — no emulator. The release smoke suite lives in `src/androidTest/kotlin/.../smoke/` and runs only as part of TASK-021 via `experiment/scripts/with_emulator.sh`, which boots an Android Virtual Device from a pre-warmed Quick Boot snapshot, runs `connectedAndroidTest`, and shuts down. The snapshot infrastructure was built specifically for this experiment.

---

## 2. The combinations tested

15 distinct tool × model attempts across 10 days (2026-05-09 to 2026-05-18). Many of these caught infrastructure bugs before delivering useful model data.

| # | Tool | Model | Outcome | Why it ended where it did |
|---|---|---|---|---|
| 1 | aider | qwen2.5-coder:32b | 0 tasks recorded | Infra bug (early adapter) |
| 2 | aider | gemma4:31b (run 1) | 0 tasks | Infra bug |
| 3 | aider | gemma4:31b (run 2) | 0 tasks | Infra bug (FAILURE_REASONS[-1]) |
| 4 | aider | gemma4:31b (run 3) | 0 tasks | TASK-001 failed 10× (pluginManagement) |
| 5 | aider | devstral | 0 tasks | Infra bug |
| 6 | aider | gemma4:31b (baseline) | 2 done | TASK-003 false-positive (silent skip) |
| 7 | claude | claude-sonnet-4-6 | 2 done, killed | Stalled on TASK-003 (`--tests` flag bug) |
| 8 | **claude** | **claude-sonnet-4-6 (post-bugfix)** | **10/11 done** | TASK-021 hit rate-limit cascade |
| 9 | aider | gemma4:31b (re-run) | 2 done, stopped | TASK-003 — SearchReplaceNoExactMatch |
| 10 | aider | qwen2.5-coder:32b | 0 tasks, killed | VRAM overflow (31GB > 24GB) |
| 11 | aider | qwen2.5-coder:14b | 0 tasks, killed | TASK-001 — Material 3 hallucination |
| 12 | openhands | gemma4:31b (4 attempts) | 0 tasks, deferred | 4 cascading adapter bugs, 0 LLM calls |
| 13 | **goose** | **gemma4:31b** | **2 done, FAILED** | TASK-003 — RobolectricExtension hallucination |
| 14 | goose | devstral (2 runs) | 0 tasks | Devstral can't emit MCP tool calls |
| 15 | **aider** | **devstral** | **0 tasks, FAILED** | TASK-001 — same Material 3 hallucination |
| 16 | **claude** | **claude-sonnet-4-6 (Phase 4 + final)** | **21/21 done** | Complete (2 runs, rate-limit between them) |

**Canonical runs** (bold) are post-bugfix attempts that produced fair model-comparison data. Earlier rows caught infrastructure bugs.

---

## 3. Outcome summary

### 3.1 The one combination that completed: claude × sonnet-4-6

| Task | Status | Attempts | Wall-clock |
|---|---|---|---|
| TASK-001 (skeleton) | DONE | 1 | 3:00 |
| TASK-002 (domain models) | DONE | 1 | 2:00 |
| TASK-003 (Room + Robolectric) | DONE | 1 | 14:55 |
| TASK-004 (repos + Hilt) | DONE | 3 | 20:11 |
| TASK-005 (use cases) | DONE | 1 | 7:31 |
| TASK-006 (calendar sync) | DONE | 1 | 13:27 |
| TASK-007 (Wi-Fi connected) | DONE | 1 | 4:31 |
| TASK-008 (Wi-Fi scan) | DONE | 1 | 2:23 |
| TASK-009 (Geofence) | DONE | 1 | 1:43 |
| TASK-010 (Orchestrator + Worker) | DONE | 1 | 15:18 |
| TASK-011 (Theme) | DONE | 1 | 2:27 |
| TASK-012 (Navigation) | DONE | 1 | 13:52 |
| TASK-013 (Onboarding) | DONE | 1 | 8:54 |
| TASK-014 (Dashboard) | DONE | 1 | 11:22 |
| TASK-015 (Calendar screen) | DONE | 1 | 9:20 |
| TASK-016 (Settings + sheets) | DONE | 1 | 14:46 |
| TASK-017 (Permissions) | DONE | 1 | 12:03 |
| TASK-018 (Notifications) | DONE | 1 | 4:52 |
| TASK-019 (Widget) | DONE | 1 | 2:15 |
| TASK-020 (Polish + snapshots) | DONE | 1 | 17:42 |
| TASK-021 (Smoke on emulator) | DONE | 2 | 17:42 |

**Total: 21/21 done. 19 of 21 first try.** Two tasks needed retries:
- TASK-004: 3 attempts. Attempts 1-2 hit a real orchestrator bug (the QA script called `./gradlew kaptDebugKotlin` but the project is KSP-only — bug 8 in the infrastructure log). Once the bug was patched mid-flight on attempt 3, Claude's code passed immediately.
- TASK-021: 2 attempts. Attempt 1 had a real test failure (`WorkerSchedulingSmokeTest` — DayDetectionWorker returned `Result.failure()` under WorkManagerTestInitHelper because the test harness didn't seed DataStore). Attempt 2 fixed it with the failure context.

So of the 21 tasks, 20 were either first-try successes or a single iterative fix; only TASK-004 (which hit an orchestrator bug not a Claude bug) needed multiple genuine retries.

The two model-evaluation runs that produced this result spanned 2026-05-17 and 2026-05-18, totaling roughly **5 hours of cloud wall-clock**. The Claude API rate-limit cut in twice during the runs (after TASK-021 on 2026-05-17, after TASK-018 on 2026-05-18); both times all remaining retry attempts immediately failed with `TOOL_ERROR (exit 1)` until the rate-limit window reset, after which the next attempt passed.

### 3.2 What every local model produced

None made it past Phase 1 (Project Foundation). Three of four hit walls inside TASK-001 or TASK-003. The fourth couldn't even start (VRAM overflow). After the orchestrator's bugs were all fixed and the local models had a level playing field:

| Model | Best result | Wall-clock to failure | Final failure mode |
|---|---|---|---|
| gemma4:31b (with aider, post-fix) | 2/21 DONE | killed at ~2h on TASK-003 | SearchReplaceNoExactMatch — model guessing at JUnit imports |
| gemma4:31b (with goose) | 2/21 DONE | 3h 52m, FAILED at TASK-003 | RobolectricExtension hallucination — JUnit 5 form for a JUnit 4-only library |
| qwen2.5-coder:14b (with aider) | 0/21 | ~10 min, killed | Material 3 parent style hallucination on TASK-001; attempts 6-7 drifted off-scope into rewriting `gradlew.bat` |
| qwen2.5-coder:32b (with aider) | 0/21 | seconds | 31 GB model didn't fit in 24 GB VRAM, ~7.5 GB on CPU → throughput unusable |
| devstral (with aider, post-fix) | 0/21 | 44 min, FAILED at TASK-001 | Same Material 3 hallucination as qwen2.5-coder:14b — 10 attempts of the same error |
| devstral (with goose) | 0/21 | ~220 s, FAILED | Devstral cannot reliably emit MCP-format tool calls; goose's developer extension can't apply edits without them |

---

## 4. Why each local model failed (specifically)

### 4.1 gemma4:31b — Robolectric JUnit 4 vs JUnit 5 confusion

gemma4 has enough Android-themes knowledge to pass TASK-001 (where the code-tuned models failed) but hits a precise wall on TASK-003. The project is configured with JUnit 5 platform (`useJUnitPlatform()` in `app/build.gradle.kts`), and the model assumes Robolectric must integrate the JUnit-5 way: with an `@ExtendWith(SomeExtension::class)` annotation.

It writes:
```kotlin
@ExtendWith(RobolectricExtension::class)   // ← made up; doesn't exist
class DayRecordDaoTest { … }
```

The compile error is consistent across all 10 attempts:
```
e: DayRecordDaoTest.kt:21:30 Unresolved reference 'RobolectricExtension'
e: DayRecordDaoTest.kt:25:13 Unresolved reference 'RobolectricExtension'
e: DayRecordDaoTest.kt:25:13 Annotation argument must be a compile-time constant
```

Robolectric, even in 2026, **only supports JUnit 4**. The correct (and spec-mandated) form is:
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayRecordDaoTest { … }
```

TASK-003's spec text explicitly documents this — including the exact code snippet, the package import path, and a note that Robolectric runs under JUnit 4 even though the project's platform is JUnit 5. gemma4 ignored the spec across all 10 attempts. **Feeding the failure output into retry prompts didn't help** because the model doesn't *know* the JUnit-4 `@RunWith` pattern in this context; supplying an error message about a non-existent class doesn't teach it the correct alternative.

This failure is identical on both aider (different specific symptom: `SearchReplaceNoExactMatch` while trying to swap JUnit imports) and goose (`compileDebugUnitTestKotlin FAILED: Unresolved reference 'RobolectricExtension'`). Same model, two tools, same wall.

### 4.2 qwen2.5-coder:14b — Material 3 theme parent hallucination

qwen2.5-coder is "code-tuned" — fine-tuned on code corpora — and is one of the strongest local code models that fits cleanly in 24 GB VRAM. It still fails TASK-001. The model writes a `themes.xml` with:

```xml
<style name="Theme.DaysInOffice" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="colorPrimary">@color/blue_700</item>
    …
</style>
```

`Theme.Material3.DayNight.NoActionBar` does not exist in Material 3. The legacy Material / AppCompat themes had `Theme.AppCompat.DayNight.NoActionBar`; Material 3 split the day-night and NoActionBar dimensions differently. The correct Material 3 parent for an app theme is `Theme.Material3.DayNight` (with NoActionBar applied via overlay or window flags, not as part of the parent name).

The cascading compile errors:
```
error: resource style/Theme.Material3.DayNight.NoActionBar not found
error: style attribute 'attr/colorPrimary' not found
error: style attribute 'attr/colorOnPrimary' not found
error: style attribute 'attr/colorPrimaryContainer' not found
… [8 attrs in total cascade-fail]
```

The cascade happens because once the parent style is invalid, all the `colorPrimary` / `colorSecondary` / etc. attributes that would normally come from the Material 3 parent are unresolved. The model "fixes" subsequent attempts by changing things downstream (different attr names, different child resources) instead of fixing the parent style — exactly the wrong direction.

By attempt 6 the responses degenerated to near-empty content (~1 minute per attempt instead of the ~5-15 min real iteration). By attempt 7 the model started drifting off-scope, writing content for `gradlew.bat` — a file in the protected list that has nothing to do with themes. This "out of ideas, start wandering" pattern is a clear capability ceiling.

### 4.3 devstral — same Material 3 hallucination as qwen2.5-coder:14b

Devstral is Mistral's code-tuned model, around 23B parameters. With aider's SEARCH/REPLACE format it produced real iteration (44 minutes of work, 10 attempts × ~4.4 min each), unlike qwen2.5-coder:14b which degenerated faster. But the failure was identical:

```
error: resource style/Theme.Material3.DayNight.NoActionBar not found
error: style attribute 'attr/colorPrimary' not found
… (cascade)
```

Same Material 3 parent hallucination, same cascade. Two completely different model families (Qwen and Mistral) producing the same wrong API name strongly suggests this is the dominant pattern in the local-model training mix — both were apparently exposed to more Material 2 / AppCompat code than to current Material 3 code.

### 4.4 devstral × goose — incompatible architectures

Separately, devstral cannot be evaluated under goose at all. Goose's "developer" extension uses MCP-style structured tool calls (e.g. `developer__text_editor` with JSON arguments) to make file edits. Devstral apparently emits plain prose responses; goose can't parse them as tool calls, applies no edits, and exits within ~22 seconds per attempt. Across two consecutive 10-attempt runs (total ~7 minutes of wall-clock), **zero files were written**.

This is not a model knowledge issue, it's a model interface issue — devstral wasn't trained to produce the structured JSON tool-call format that newer agentic frameworks expect. Aider's text-only SEARCH/REPLACE format sidesteps this entirely (aider works fine with devstral), but it also means tools that require tool-call training won't work with devstral.

### 4.5 qwen2.5-coder:32b — VRAM overflow

The 32B variant of the same Qwen code model never got a fair test. At Q4_K_M quantization the model weights are 31.4 GB; the RTX 3090 Ti host has 24 GB VRAM. Ollama loaded 23.9 GB to GPU and spilled the remaining 7.5 GB to system RAM. Per-token inference then required transferring data between CPU memory and GPU memory through PCIe — a roughly 10–20× throughput hit compared to fully-resident inference. The run was killed within seconds of starting.

Worth noting: when devstral was loaded (a similarly-sized model in disk footprint), it also peaked at 21.6 GB VRAM — but only because its default 128K context window allocated ~8 GB for the KV cache on top of the ~13 GB weight footprint. Reducing the context window would have unlocked more headroom. The 32B Qwen weights themselves don't have that headroom even with zero KV cache.

---

## 5. The common pattern: knowledge gaps, not edit format

Three falsifiable hypotheses were tested during the experiment about *why* local models fail. Two were ruled out; one was confirmed.

### 5.1 Hypothesis: "aider's SEARCH/REPLACE format is the bottleneck"

Motivation: gemma4 × aider failed on TASK-003 with `SearchReplaceNoExactMatch` errors — the model was emitting valid SEARCH/REPLACE blocks but the SEARCH content didn't byte-for-byte match the file. Maybe aider's diff-style format was forcing the model into errors it wouldn't otherwise make.

**Falsification:** goose × gemma4 was run as a direct control. Goose uses whole-file rewrites via tool calls, with no SEARCH/REPLACE format. Result: gemma4 cleared TASK-001 and TASK-002 on both tools (often faster on goose, in fewer orchestrator-level retries), and **hit the same wall on TASK-003**. The specific symptoms differed — goose surfaced the underlying compile error (`RobolectricExtension` undefined) while aider couldn't even land an edit — but both runs stopped at the same task with the same root cause.

The edit format only affects *whether the model engages at all*. Once engaged, the model's knowledge ceiling is what it is.

### 5.2 Hypothesis: "the retry loop should teach the model the right API"

Motivation: each retry prompt includes the previous failure's output (compile error, test failure, etc.). One might expect the model to see "Unresolved reference 'RobolectricExtension'" and pivot to a different name. Maybe the retry signal is just not strong enough.

**Falsification:** strengthening the signal didn't help. We applied a "SEARCH/REPLACE discipline" reminder to retry prompts before the qwen2.5-coder:14b run, hoping to reduce the content-matching errors. It made no observable difference to the outcome — qwen2.5-coder:14b still hallucinated `Theme.Material3.DayNight.NoActionBar` across all attempts.

Adding more context to a retry prompt doesn't supply the missing API knowledge. The model's weights either know `RunWith(RobolectricTestRunner)` is the right form or they don't. When they don't, asking nicely with the failure message attached doesn't change that.

### 5.3 Hypothesis: "the model just doesn't know modern Android-framework APIs"

This is the surviving hypothesis. The evidence:

1. **Three different local models, three different specific gaps.** gemma4 hit Robolectric+JUnit 4. qwen2.5-coder:14b and devstral both hit the same Material 3 parent name — two model families converging on the same wrong API, which is what training-data correlation looks like.
2. **The specific errors are all "known classic"** — Material 2 → Material 3 was a non-trivial rename in 2022 that older training corpora still over-represent. JUnit 4 → JUnit 5 migration is still a thing developers stumble on. These aren't general programming errors; they are specific framework knowledge gaps.
3. **Cloud Claude got these right first try.** It also fixed the WorkManager test failure on TASK-021 attempt 2 with just the failure-context retry — which is what the retry loop is *supposed* to do, and what local models couldn't manage on much simpler issues.
4. **The gap is bidirectional.** When a model has the right API in its weights, the retry loop with failure context produces convergence. When a model doesn't have the right API, the retry loop produces degeneration (shorter responses, off-scope drift, eventually giving up). We saw both patterns clearly.

---

## 6. The supporting cast: why these failures hid for so long

Independent of model capability, the experiment exposed how easy it is to mistake orchestrator bugs for model failures. **18 distinct infrastructure bugs** were discovered and fixed during the experiment. Several of them, before being fixed, would have produced false conclusions about model capability:

| Bug | Without the fix, you would conclude… |
|---|---|
| Bug 7 (`--tests` on `connectedAndroidTest`) | Every model fails TASK-003 |
| Bug 8 (`kaptDebugKotlin` in QA but project is KSP-only) | Every model fails TASK-004 (Hilt modules) |
| Bug 12 (`SKIPPED_NO_DEVICE` silent skip) | gemma4 (or any model) "passed" TASK-003 when no test ever ran |
| Bug 9 (parser dropped multi-line QA commands) | Claude can't pass TASK-021 |
| Bug 11 (TASK-021 QA used a relative path that didn't resolve from the run dir) | Same as bug 9 |
| Bugs 13-16 (OpenHands adapter 4 versions out of date) | OpenHands is broken / OpenHands models can't code |
| Bug 17 (Goose CLI flags out of date) | Goose is broken |
| Bug 18 (Claude rate-limit looks like a model failure) | Claude can't pass certain tasks |

The full bug log is in `experiment/RUNS_LOG.md` Infrastructure Fixes Log. The point: **without rigorous fix-and-verify on each bug, the experiment would have produced confidently wrong conclusions about model capability.** Half of the wall-clock time on this experiment was bug-finding work; the model-evaluation work only became meaningful once the bugs were out of the way.

---

## 7. Resource economics

For the local-model side, where you ran the experiment matters as much as which model you ran.

### 7.1 VRAM

The RTX 3090 Ti has 24 GB. Loaded model footprint at Q4 / Q4_K_M for each:

| Model | Weights | KV cache (default ctx) | Total in VRAM |
|---|---|---|---|
| gemma4:31b | ~17 GB | small (32K) | ~17 GB — clean |
| qwen2.5-coder:14b | ~9 GB | moderate (32K) | 13.3 GB — clean, generous headroom |
| qwen2.5-coder:32b | ~24 GB | (irrelevant) | 23.9 GB on GPU + 7.5 GB on CPU — overflow |
| devstral | ~13 GB | ~8 GB (128K default ctx!) | 21.6 GB — tight, KV cache dominates |
| claude-sonnet-4-6 | N/A | N/A | cloud |

**Key insight:** model weights aren't the only consumer of VRAM. Devstral's default 131,072-token context window contributed more than half of its VRAM footprint — and you would never need 128K context for this task (the largest prompts are <20K tokens). **Context window choice can double or triple effective VRAM use.** Most operators don't realize this until they're already in spillover territory.

The diagnostic command for spillover is `curl /api/ps | jq '.models[].size_vram'` — compare against `size` (disk footprint). If `size_vram < size`, the model is offloading to CPU and throughput will collapse.

### 7.2 Wall-clock

For successful tasks:

| Tool / Model | TASK-001 wall-clock |
|---|---|
| Claude × sonnet-4-6 | 3:00 (1 attempt) |
| Aider × gemma4:31b (post-fix) | ~20 min (3 attempts) |
| Goose × gemma4:31b | ~13 min (1 attempt) |

For failing tasks (gemma4 on TASK-003):

| Tool | Per-attempt | 10-attempt total |
|---|---|---|
| Aider × gemma4 | ~30–60 min | ~6 hours |
| Goose × gemma4 | ~20–25 min (internal develop-test-fix loop) | ~3.5 hours |

A single failing task on local can therefore consume **5+ hours of wall-clock** before final FAILED. On cloud Claude, the equivalent is minutes. This is part of why local exploration is so much more expensive in practice than the per-token-cost comparison suggests.

### 7.3 Internal iteration architectures

Aider and goose have meaningfully different per-attempt behavior:

| | Aider | Goose |
|---|---|---|
| Per invocation | Edit files via SEARCH/REPLACE, exit | Edit → run gradle → see errors → fix → repeat → exit |
| Build runner | Orchestrator (after aider exits) | Goose itself (inside the agent loop) |
| Effective LLM calls per "attempt" | 1 | many (dozens possible) |
| Tool format requirement | Text only | Structured tool calls (MCP) |

This explains why devstral × goose produced zero edits while devstral × aider produced real iteration: aider's text-only format works with any chat model; goose requires the model to emit structured tool calls. Devstral wasn't trained to do that reliably.

It also explains why goose's "per-attempt" timing on hard tasks ballooned: gemma4 + goose on TASK-003 averaged 20+ minutes per orchestrator-level attempt because goose ran ~5 gradle builds per attempt as part of its internal loop.

---

## 8. What ruled out the alternative explanations

To be confident "model knowledge gap" is the real cause, several alternatives had to be eliminated:

1. **Was it tool choice?** No — same model on aider and goose hit the same wall (gemma4 × TASK-003). Code generated, build run, both failed with the same root cause.

2. **Was it edit format?** No — see §5.1. Goose's whole-file rewrites and aider's SEARCH/REPLACE both hit gemma4's TASK-003 ceiling.

3. **Was it the retry-prompt strength?** No — see §5.2. Stronger retry-prompt discipline didn't move the needle for qwen2.5-coder.

4. **Was it the orchestrator's bugs?** This was the most worrying alternative. Confirmed addressed by the 18 infrastructure fixes — each one was fixed and verified, and the canonical runs were re-done post-fix. The bug fixes did unblock real work (gemma4's TASK-001 success rate went from "failed every attempt" to "passed in 3 attempts" once the bugs were out). But once unblocked, gemma4 still hit TASK-003.

5. **Was it model size?** Partially yes (qwen2.5-coder:32b couldn't fit in VRAM), but more importantly no — qwen2.5-coder:14b and devstral (23B) failed on the same task as each other, and gemma4 (27B) failed on a different task. Size correlates weakly with task progression for these specific failures; the dominant variable is what's in the weights, not how many of them there are.

6. **Was it the task plan being unreasonably hard?** No — Claude solved it cleanly on the first attempt for 19 of 21 tasks. If the plan were inherently unreasonable, Claude would have struggled too. The tasks are deliberately scoped to real production-shape work, but each individual task is something a junior-to-mid Android developer could complete in an afternoon.

---

## 9. Implications

### 9.1 For background / autonomous coding today

| Use case | Recommendation |
|---|---|
| Highest success rate, cost not blocking | **Claude Code CLI + sonnet-4-6**. 21/21 done. Simplest adapter. Reliable. |
| Budget-sensitive, cloud OK | **aider + frontier cloud model** (Claude, GPT-4 class). Aider's diff format saves tokens; pair with a model that has the knowledge. |
| Strictly local / privacy-constrained | **aider + gemma4:31b** with heavy pre-seeding of framework-specific boilerplate (themes, Hilt config, anything where the model has visible knowledge gaps). Expect to need to intervene. |
| Investigative one-task-deep | Any tool + frontier cloud model |
| CI / fully unattended | Claude Code CLI. Smallest moving-parts count; least likely to break across runs. |

### 9.2 For local-model deployments

The honest reading of the data: **local models in the 14B-30B range cannot yet do unsupervised, end-to-end implementation of a moderately modern Android project.** They will hit specific framework-knowledge walls early in the work. The walls are not removable by retry loops, edit-format changes, or bigger models (a bigger model from the same training family hits the same walls — qwen-coder-14b and qwen-coder-32b are both Qwen-trained).

What does work for local:

- **Scope reduction.** Pre-seed all boilerplate (themes, build config, basic skeleton). Have the model only write business logic. Local models can handle pure Kotlin code reasonably well; they just can't write the Android-XML / Material-theme / Hilt-module surface from scratch.
- **Pair with strong code review.** A human reviewer can catch and fix the framework-knowledge errors in seconds; the model gets to do the bulk-typing.
- **Pick the right model class.** General-purpose models (gemma4) had broader Android knowledge than code-tuned models (qwen-coder, devstral) — counterintuitively, the "code" suffix narrows the training distribution in ways that hurt for framework-heavy work. For Android specifically, this matters.

### 9.3 What would close the local-cloud gap

A local model trained with substantially more recent and substantially more thorough Android-Kotlin training data — particularly Material 3 sample apps, Robolectric + JUnit 4 patterns, Hilt + KSP examples. The model architecture is probably not the bottleneck; the training data composition is.

---

## 10. Limitations and open questions

1. **N = 1 per combination.** Stochastic variation across runs could move some rankings. Multiple runs of each combination would tighten the conclusions; this experiment was already 10 days of wall-clock so we stopped at one.
2. **Task plan is Android-specific.** Tools and models with stronger Android Kotlin training (Claude, possibly others) get an unfair advantage. The conclusions about local-model framework knowledge gaps generalize less obviously to, say, a Rails app or a React frontend.
3. **Hardware bias.** The 24 GB VRAM ceiling shaped what models could be tested. A larger card (40 GB H100, 80 GB A100) would have run qwen2.5-coder:32b at full speed and potentially given a different result. We don't have data on whether a fully-resident 32B local model would have cleared TASK-003 — that's the most interesting open question.
4. **Pre-seeding hypothesis untested.** §9.2's claim that pre-seeding `themes.xml` would unblock qwen-coder/devstral past TASK-001 is plausible but not empirically verified. The natural experiment would be a future run with that pre-seed in place.
5. **OpenHands not evaluable.** The OpenHands adapter required a full rewrite that we deferred. No conclusions about OpenHands' capability are warranted from this experiment.
6. **Goose × non-MCP-trained models is structurally impossible.** This isn't a goose flaw or a model flaw — it's an interface mismatch. Worth noting for future tool selection.

---

## 11. Reproducibility

All artifacts preserved in this repo:

- **Source of truth code state per run:** `runs/<tool>/<model>/` directories (each is an independent git repo). Continuously fetched to `run/<tool>/<model>` branches in this main repo during runs.
- **Failure history archived:** `archive/<tool>/<model>/<date>[-suffix]` branches in this main repo. Survive `reset_run.sh`. Each contains the full code state, `run.json`, `run.log`, and per-attempt artifacts (`attempt*_prompt.txt`, `attempt*_failure.txt`, `attempt*_session.json`, `attempt*_usage.json`).
- **Bug log:** `experiment/RUNS_LOG.md` Infrastructure Fixes Log (18 bugs).
- **Detailed running analysis:** `experiment/FINDINGS.md`.
- **This report:** `experiment/REPORT.md` (you are here).
- **Operator runbook:** `experiment/RUNBOOK.md`.

To re-run any combination:
```bash
bash experiment/scripts/setup_run.sh <tool> <model>
bash experiment/scripts/run_experiment.sh <tool> <model> <model-id>
```
Valid tools: `aider`, `claude`, `openhands` (after rewrite), `goose`.
Valid models: see `setup_run.sh:18` for the current list.

The Claude success can be re-created at any time with the artifacts on the `run/claude/sonnet-4-6` branch. Local-model failure artifacts are similarly preserved.

---

## 12. Conclusion

The experiment set out to compare autonomous LLM coding tools and models on a real Android project. The headline result is the local-vs-cloud gap, but the **substantive finding** is more specific:

> **The bottleneck for local 14–30B models on this kind of work is not their reasoning, their tool integration, or their iteration capacity. It is the specific Android-framework knowledge in their weights.** When that knowledge is missing — Material 3 theme parent names, Robolectric's JUnit-4-only constraint — the retry loop cannot fix it, because retry loops feed back symptoms, not solutions. Cloud frontier models that have been trained on more recent and more thorough Android material make the same mistakes far less often and recover from them when they do.

The 18 infrastructure bugs found and fixed along the way are also part of the report's substance: without them, the model-capability conclusions would have been wrong in confidently misleading ways. Building rigorous experimental infrastructure for LLM evaluation is at least as hard as evaluating the LLMs themselves.
