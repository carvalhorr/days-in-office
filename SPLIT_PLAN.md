# Repo split plan (DEPLOYMENT T2)

**Date:** 2026-05-22

## Goal

Separate the experiment infrastructure (which is research output) from the
product app being shipped to the Play Store. Two repos after the split:

| Repo | Purpose |
|---|---|
| `days-in-office` (this repo) | The Android product. Buildable Play Store app. |
| `days-in-office-experiment` (new, sibling clone) | LLM coding-benchmark harness, all model runs, the prototype, and the experiment write-up. |

T3 will execute this plan **locally only** (no push to any remote).

## Path inventory (current repo, tracked paths)

```
.gitignore                  → both repos (each tuned)
ARCHITECTURE.md             → both repos (experiment uses it as fixture; product keeps it as design doc)
BUGS.md                     → experiment repo
CLAUDE.md                   → both repos (different content)
DECISIONS.md                → product repo (product-level decision log)
DEPLOYMENT.md               → product repo
EXPERIMENT.md               → experiment repo
RUN_RECOMMENDATION.md       → experiment repo (then delete; it's a one-shot memo)
SPLIT_PLAN.md (this file)   → both as historical record; not required
TASKS.md                    → experiment repo
app/                        → product repo (will be replaced by promoted run in T4)
build.gradle.kts            → both repos (each builds its own app)
experiment/                 → experiment repo
gradle/                     → both repos
gradlew, gradlew.bat        → both repos
prototype/                  → experiment repo (visual source of truth for the experiment)
runs/                       → experiment repo
settings.gradle.kts         → both repos
```

## Product repo content (after T3)

```
.github/workflows/          (added in T10–T12)
.gitignore
app/
  src/                      (promoted from runs/claude/sonnet-4-6/app/src/ via T4)
  build.gradle.kts          (with signing + R8 per T5/T6)
  proguard-rules.pro        (per T6)
ARCHITECTURE.md             (kept; it's still the product design doc)
build.gradle.kts
CLAUDE.md                   (slimmed: drop "experiment workflow" sections)
DECISIONS.md
DEPLOYMENT.md
docs/
  privacy-policy.md         (added in T8)
gradle/
gradlew, gradlew.bat
KEYSTORE_README.md          (gitignored; T5)
keystore.properties         (gitignored; T5)
app/upload-keystore.jks     (gitignored; T5)
PLAY_CONSOLE_CHEATSHEET.md  (T9)
settings.gradle.kts
STORE_LISTING.md            (T7)
```

## Experiment repo content (after T3)

```
.gitignore                  (keeps the `runs/` rule etc.)
ARCHITECTURE.md             (still the fixture spec the agents read)
BUGS.md
CLAUDE.md                   (the existing experiment workflow content)
EXPERIMENT.md
RUN_RECOMMENDATION.md
TASKS.md
app/                        (kept as the empty/skeleton fixture so the harness still runs)
build.gradle.kts
experiment/                 (orchestrator scripts + reports)
gradle/
gradlew, gradlew.bat
prototype/
runs/                       (all model outputs, the actual research data)
settings.gradle.kts
```

## Strategy for T3 (local, no push)

We are **not** using `git filter-repo` for this initial split — that tool is
not guaranteed to be installed and the deployment doc only mandates
"preserve history for moved paths". A pragmatic equivalent:

1. **Clone this repo** to `../days-in-office-experiment` with `git clone .
   ../days-in-office-experiment`. The clone has the full history of every
   path. History for experiment paths is fully preserved there.
2. **In the experiment clone**, `git rm` the product-only paths
   (`DECISIONS.md`, `DEPLOYMENT.md`, `docs/`, `KEYSTORE_README.md`,
   `keystore.properties`, store/play files, `.github/workflows/`) — these
   may not yet exist at split time, but the rule documents intent.
   Commit: `chore: extract experiment repo from product repo`.
3. **In the product repo (this directory)**, `git rm -r` the experiment-only
   paths: `experiment/`, `runs/`, `BUGS.md`, `TASKS.md`, `prototype/`,
   `EXPERIMENT.md`, `RUN_RECOMMENDATION.md`. Slim `CLAUDE.md` to product
   workflow only. Commit: `chore: extract product repo from experiment repo`.
4. **Verify both repos build independently**:
   - Product: `./gradlew assembleDebug` succeeds.
   - Experiment: harness scripts in `experiment/` still discover their
     fixture files (`ARCHITECTURE.md`, `TASKS.md`, `prototype/`).

Trade-off accepted: the experiment paths' history is preserved fully in the
experiment repo, and the product paths' history is preserved fully in the
product repo. The "removed in repo A but kept in repo B" history is split
cleanly along the commit where T3 lands. We do **not** rewrite history; this
keeps it boring and reversible.

If, later, a stricter history rewrite is wanted (e.g. removing experiment
blobs from the product repo's pack files to shrink it), `git filter-repo`
can be run as a follow-up. The git-rm split is the safe minimum that
unblocks deployment work.

## Push policy

Per the answers given at the start of this deployment pass: **no push** in
T3. Both repos stay local until the user signs off after reviewing them.
