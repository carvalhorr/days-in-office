# Days in Office — Agent Workflow

This is an Android app project. All implementation decisions are documented in `ARCHITECTURE.md`. All work items are in `TASKS.md`.

## How to Pick Up Work

1. Read `ARCHITECTURE.md` fully.
2. Read `TASKS.md` and find the first task with `Status: NOT_STARTED` whose dependencies are all `DONE`.
3. Update the task status to `IN_PROGRESS`.
4. Implement the task following the **Scope** and **Implementation Details** exactly.
5. Run the **QA Verification Steps** for the task.
6. If all **Acceptance Criteria** pass: update status to `DONE`.
7. Commit with message: `feat: complete TASK-XXX — <title>`.
8. Move to the next task.

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
- Update the task status to `DONE` only when every criterion is confirmed.

## Project Commands

```bash
# Build
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Specific test class
./gradlew testDebugUnitTest --tests "com.daysInOffice.some.ClassName"

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lintDebug

# Release build
./gradlew assembleRelease

# Generate Hilt code
./gradlew kaptDebugKotlin
```

## Key Invariants (Do Not Violate)

1. Never overwrite a `confirmedByUser = true` record with automated detection.
2. Weekends are always excluded from mandate calculations.
3. `ComplianceResult.daysNeededToComply` is never negative.
4. One `DayRecord` per calendar date.
5. Detection workers only run on configured working days (Mon–Fri default).
6. Domain layer (`core/domain`) has zero Android dependencies.
7. ViewModels never reference Room entities or DataStore directly — only domain models.
