# Deployment — Play Store publishing plan

Persistent todo list for shipping the Days in Office Android app to the Google Play Store. Lives outside `TASKS.md` (experiment fixture) and `BUGS.md` (experiment output) because deployment is product-side work, not experiment scope.

**Status legend:** `[ ]` not started · `[/]` in progress · `[x]` done · `[!]` blocked

**Decisions already made (2026-05-20):**
- The app being published is the promoted output of an experiment run — currently the leading candidate is `runs/claude/sonnet-4-6/`.
- Experiment infrastructure (`experiment/`, `runs/`, `BUGS.md`, `TASKS.md`, `prototype/`, `EXPERIMENT.md`) will be split into a separate repo before deployment work starts.
- Play Console developer account exists; no registration delay.
- Signing config will live in this repo's `app/build.gradle.kts` with a defensive `keystore.properties` lookup so the experiment fixture still builds without secrets.

---

## Critical path (sequential)

- **[ ] T1 — Identify the winning experiment run.** Survey `runs/<model>/<id>/` for task completion, smoke pass rate, bug count. Output: a short recommendation memo. *Note: tonight's BUG-024/BUG-025 work already confirms `claude/sonnet-4-6` is the de-facto leader; T1 may collapse to a brief sign-off.*
- **[ ] T2 — Draft `SPLIT_PLAN.md`.** Decide exact split between product repo (`days-in-office`) and new experiment repo (`days-in-office-experiment`). No git surgery yet. *Blocked by: T1.*
- **[ ] T3 — Execute the repo split.** Use `git filter-repo` (or subtree split) to preserve history for moved paths. Push the new experiment repo. Verify both repos build/run independently. **Destructive — explicit go-ahead required before running.** *Blocked by: T2.*
- **[ ] T4 — Promote the winning run into `app/`.** Copy the chosen run's `app/src/` into the (currently near-empty) repo `app/`. Get it building + tests passing outside the experiment harness. *Blocked by: T3.*
- **[ ] T5 — Wire defensive signing config.** Generate upload keystore (local, never commit). Add gitignored `keystore.properties`. Add `signingConfigs.release` in `app/build.gradle.kts` that reads `keystore.properties` if present and silently skips signing when absent. Document keystore backup location. *Blocked by: T4.*
- **[ ] T6 — Enable R8/minify with verified keep rules.** Set `isMinifyEnabled = true` on `release`. Keep rules for Hilt, Room entities/DAOs, kotlinx-serialization `@Serializable` classes, WorkManager workers. Build signed `.aab`, install on device, smoke every flow. Fix shrinker-induced crashes. *Blocked by: T5.*

## Parallel tracks (no inter-dependencies; can start any time)

- **[ ] T7 — Audit launcher icon + produce store assets.** Adaptive icon (foreground/background), 512×512 hi-res Play icon, 1024×500 feature graphic, ≥2 phone screenshots, 80-char short description, ≤4000-char full description, app title.
- **[ ] T8 — Write + host privacy policy.** Required because the app uses `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `READ_CALENDAR`. Draft policy (on-device-only data, retention, contact). Host on GitHub Pages at a stable URL. Provide URL to Play Console.
- **[ ] T9 — Play Console paperwork.** Data safety form (location + calendar, on-device processing), content rating questionnaire, target audience (18+ working adults), background-location declaration **with justification video** (required by Play), app access (no login), ads declaration (none). Gates the first release.
- **[ ] T10 — CI: build + test on push.** GitHub Actions workflow: on push to `main` and on PRs, run `./gradlew testDebugUnitTest` and `assembleDebug`. Gradle cache enabled. No secrets needed (debug only).

## Automation (the "future automated state")

- **[ ] T11 — CI: signed `.aab` on tag.** Workflow: on `v*.*.*` tag push, build `bundleRelease`. Inject `keystore.properties` + `keystore.jks` from base64-encoded GitHub Secrets. Upload `.aab` as a release artifact. *Blocked by: T5, T10.*
- **[ ] T12 — Automate Play Store internal-track upload.** Create Play Console service account, grant Release Manager role on the app, store JSON key as GitHub Secret. Extend T11's workflow with `gradle-play-publisher` or `fastlane supply` to upload the `.aab` to the **internal testing track**. *Blocked by: T9, T11.*

**End state of T12:** `git tag v1.0.1 && git push --tags` → within ~10 minutes the new build is live to internal testers. Promotion to the production track stays a manual one-click in Play Console (intentional — full-auto-to-prod is risky).

## Side concern

- **[ ] T13 — Audit and shrink `TASKS.md` / `BUGS.md`.** Files have grown to ~141 KB and ~98 KB; every agent run reads them in full (~35–40K tokens just to onboard). Token cost compounds across the experiment matrix. Options: archive completed work to `TASKS_DONE.md` / `BUGS_RESOLVED.md`; revise `CLAUDE.md` to instruct agents to read only the queue head; or split into per-task / per-bug files with an index. Defer until after deployment work — revisit before the next big experiment run.

---

## Open product blockers (not deployment work, but ship-blocking)

These bugs must be resolved (or explicitly accepted) before the app can be shipped, regardless of the deployment pipeline state:

- **BUG-013** — Wi-Fi scan returns no list.
- **BUG-024** — Detector stale-config + missing runtime permission requests. **Fixed in `runs/claude/sonnet-4-6/` on 2026-05-20; not yet ported into `app/` (will happen with T4).**
- **BUG-025** — `WifiConnectedDetector` reads `<unknown ssid>`; test plan drafted, on-device discriminator pass pending.
- Plus the OPEN/High items in `BUGS.md` triage table that pre-date this session.

If shipping with Wi-Fi detection broken is acceptable, BUG-025 / BUG-013 can be deferred to a 1.x patch.
