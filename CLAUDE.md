# Days in Office — Agent Workflow

This is an Android app project. All implementation decisions are documented in `ARCHITECTURE.md`. All work items are in `TASKS.md`.

## How to Pick Up Work

1. Read `ARCHITECTURE.md` fully.
2. Read `TASKS.md` and find the first task with `Status: NOT_STARTED` whose dependencies are all `DONE`.
3. Update the task status to `IN_PROGRESS`.
4. Implement the task following the **Scope** and **Implementation Details** exactly.
5. Run the **QA Verification Steps** for the task.
6. Run the **UI smoke test** (see "Project Commands") and **report** its result
   in the commit body (`smoke: N/14 passing, failures: <list>`). Smoke failures
   are reporting only — they do NOT block marking the task `DONE`. Failures
   outside the `Known-Failing UI Smoke Tests` list are noted so they can be
   triaged separately, but they don't gate the current task.
7. If all **Acceptance Criteria** pass: update status to `DONE`.
8. Commit with message: `feat: complete TASK-XXX — <title>`.
9. Move to the next task.

## How to Fix a Bug

Bug-fix tasks follow the same workflow as feature tasks. The smoke run is the
natural place to verify the fix, but it is **report-only**, never blocking:

1. Identify the smoke test(s) listed as that bug's regression guard in
   `Known-Failing UI Smoke Tests` below (if any).
2. Implement the fix.
3. Re-run the UI smoke test. The previously-failing test(s) should now pass —
   if they don't, the fix may be incomplete; investigate, but the commit is not
   blocked by smoke results.
4. If a guard flipped green, remove its row from `Known-Failing UI Smoke Tests`
   in the same commit.
5. If the bug had no regression guard, add one. A bug fixed without a regression
   test will likely come back.
6. Commit with `fix: BUG-XXX — <short description>` and include the smoke
   result in the commit body.

## Role: Implementing Agent

- Follow `ARCHITECTURE.md` package structure and class names exactly.
- Do not invent new abstractions not in the architecture document.
- If you discover the architecture is wrong or incomplete, update `ARCHITECTURE.md` before writing code and note the change in your commit.
- Write tests for every class as specified in the task. No task is done without tests passing.
- Do not mark a task DONE unless all acceptance criteria are verified with actual test runs.

## Role: QA Agent

- Read the task's **Acceptance Criteria** and **QA Verification Steps**.
- Run the exact commands in **QA Verification Steps**.
- For each criterion: verify it passes. If it fails, report the failure with the exact command output.
- Do not approve a task if any criterion is unmet, even if tests pass — some criteria are checked by grep, not tests.
- **Always run the UI smoke test** (`MainFlowSmokeTest`, see "Project Commands") in
  addition to the task's own verification steps and **report** its failure set
  alongside the QA outcome. Smoke results are report-only — they do NOT block
  approval; failures outside the `Known-Failing UI Smoke Tests` list are flagged
  for separate triage but don't gate the current task.
- For bug-fix tasks: check whether the bug's regression-guard test(s) flipped
  green and whether the entry was removed from `Known-Failing UI Smoke Tests`.
  Note any discrepancy in the QA report; still don't block on it.
- Update the task status to `DONE` only when every Acceptance Criterion is confirmed.

## Git Commit Conventions

Every change must be committed. Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>
```

**Types:**
- `feat` — new task implemented or new file created
- `fix` — retry fix for a failing task
- `docs` — change to ARCHITECTURE.md, TASKS.md, or CLAUDE.md
- `chore` — tooling, .gitignore, scripts, setup

**Scopes (for run repos):** the task ID — e.g. `feat(TASK-003): set up Room database`

**Required commit messages:**
- Task completed first attempt: `feat: complete TASK-XXX — <Title>`
- Task completed after retries: `feat: complete TASK-XXX — <Title>` (final commit only)
- Each retry fix: `fix: retry N for TASK-XXX — <what was wrong>`
- Architecture deviation: `docs(architecture): <what changed and why>` — must precede the code commit

**One commit per task.** Do not squash retry commits — the history of attempts is part of the experiment record.

## Project Commands

```bash
# Build
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Specific test class
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.some.ClassName"

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# UI smoke test — drives the live app on the connected emulator, every flow
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.carvalhorr.daysInOffice.smoke.ui.MainFlowSmokeTest

# Lint
./gradlew lintDebug

# Release build
./gradlew assembleRelease

# Generate Hilt code
./gradlew kspDebugKotlin
```

## Protected Files (Do Not Modify)

The following files are pre-seeded by the experiment framework and must not be modified:
- `gradlew` and the `gradle/` wrapper folder
- `settings.gradle.kts` (project-level — plugin repositories and module includes)
- `build.gradle.kts` (project-level — plugin version declarations)
- `app/build.gradle.kts` (app-level — dependencies and build config)
- `gradle/libs.versions.toml` (version catalog — all library aliases and versions)
- `ARCHITECTURE.md`, `TASKS.md`, `CLAUDE.md`

## Key Invariants (Do Not Violate)

1. Automated detection never writes a `DayRecord` directly. Detection can only fire a confirmation prompt (notification or in-app card); only an explicit user action — an in-app tap or a notification action — writes a `DayRecord`. (Tightened 2026-05-20 via BUG-020 / TASK-039; subsumes the earlier "never overwrite `confirmedByUser=true`" form.)
2. Weekends are always excluded from mandate calculations.
3. `ComplianceResult.daysNeededToComply` is never negative.
4. One `DayRecord` per calendar date.
5. Detection workers only run on configured working days (Mon–Fri default).
6. Domain layer (`core/domain`) has zero Android dependencies.
7. ViewModels never reference Room entities or DataStore directly — only domain models.

## Known-Failing UI Smoke Tests

`MainFlowSmokeTest` currently has tests that fail because they are regression
guards for filed bugs that have not yet been fixed. **These are expected
failures.** A task is allowed to be marked DONE if the smoke run's failure set
is exactly this list (or a subset).

| Test method | Guarding |
|---|---|

**When you fix a bug:** remove its row(s) above as part of the fix commit. If
you fix a bug whose regression guard isn't in the suite yet, add the guard at
the same time. A bug fixed without a regression test will return.

**When you add a new feature or screen:** add `@Test` coverage to
`MainFlowSmokeTest` for the new flow (one method per distinct interaction).
The point is to keep "every flow exercised" true after every change.
