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

- **[x] T1 — Identify the winning experiment run.** Done 2026-05-22 via `RUN_RECOMMENDATION.md` (kept in the experiment-side clone). `runs/claude/sonnet-4-6` selected: 43/43 tasks DONE, 98 main src files, smoke green except the BUG-025 regression guard. Runner-up gemma4 reached 2 DONE — order-of-magnitude gap.
- **[x] T2 — Draft `SPLIT_PLAN.md`.** Done 2026-05-22. Strategy: clone + `git rm` (rather than `git filter-repo`) — simpler, reversible, history of each path fully preserved in the repo it ends up in.
- **[x] T3 — Execute the repo split.** Done 2026-05-22, local only, no push. Experiment clone at `/Users/carvalhorr/code/days-in-office-experiment/` (includes physical copy of `runs/` since that directory is gitignored). Both repos verified to build (`./gradlew assembleDebug`).
- **[x] T4 — Promote the winning run into `app/`.** Done 2026-05-22. Wholesale swap of `app/src/`, `app/build.gradle.kts`, root `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties` from `runs/claude/sonnet-4-6/`. `assembleDebug` + `testDebugUnitTest` both green.
- **[x] T5 — Wire defensive signing config.** Done 2026-05-22. `app/upload-keystore.jks` generated (RSA 2048, 10 000 day validity), `keystore.properties` + `KEYSTORE_README.md` written locally — all three gitignored. `app/build.gradle.kts` reads the properties file if present; release builds are signed (apksigner v2 verified), debug + unit tests are unaffected.
- **[x] T6 — Enable R8/minify with verified keep rules.** Done 2026-05-22. `isMinifyEnabled = true`, `isShrinkResources = true`. `app/proguard-rules.pro` covers Hilt, Room (incl. `_Impl`), kotlinx-serialization `$$serializer`, WorkManager workers, manifest-declared entry points, Glance widget, Compose runtime, Play services Location. `bundleRelease` produces a 5.5 MB signed `app-release.aab`. **On-device smoke deferred** — no emulator/device connected to this host; manual install run still required before first Play upload.

## Parallel tracks (no inter-dependencies; can start any time)

- **[x] T7 — Audit launcher icon + produce store assets.** Done 2026-05-22 via `STORE_LISTING.md`. Title (14 chars), short description (69/80 chars), full description (~3000/4000 chars), and a per-asset checklist with specs. **Icon flagged as placeholder** (foreground = single white circle on blue) — must be replaced before publishing; screenshots also outstanding (need a device).
- **[x] T8 — Write + host privacy policy.** Drafted 2026-05-22 at `docs/privacy-policy.md`. Covers every declared permission, the on-device-only data model, the "delete all data" flow, and the GitHub Pages hosting setup. **Hosting itself still outstanding** — repo's Pages source needs flipping to `main` / `/docs` before submission; the URL the policy already cites is `https://carvalhorr.github.io/days-in-office/privacy-policy`.
- **[x] T9 — Play Console paperwork.** Done 2026-05-22 via `PLAY_CONSOLE_CHEATSHEET.md`. Paste-ready answers for app access, ads, content rating, target audience, data safety, the background-location declaration + a 30–45 s justification-video storyboard, store settings. Also includes a final pre-submission checklist.
- **[x] T10 — CI: build + test on push.** `.github/workflows/ci.yml` — JDK 17 (temurin) + Gradle action, parallel `build-and-test` and `lint` jobs, JUnit reports uploaded on failure, lint report uploaded always. No secrets required.

## Automation (the "future automated state")

- **[x] T11 — CI: signed `.aab` on tag.** `.github/workflows/release.yml` — on `v[0-9]+.[0-9]+.[0-9]+` push, decode `KEYSTORE_BASE64`, write `keystore.properties` from `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`, run `bundleRelease`, rename the AAB with the tag name, attach to an auto-created GitHub Release with generated notes, scrub credentials from the runner in an `always:` cleanup step.
- **[x] T12 — Automate Play Store internal-track upload.** `gradle-play-publisher` plugin (v3.10.1) wired at root + applied in `app/build.gradle.kts`. `play { }` block reads `play-service-account.json` (gitignored locally; written from the `PLAY_SERVICE_ACCOUNT_JSON` secret in CI), targets the `internal` track, defaults to AAB uploads, `releaseStatus = COMPLETED`. The release workflow runs `publishReleaseBundle` whenever the secret is set; if it's absent, the publish step is skipped with a `::warning::` and the bundle still lands on the GitHub Release.

**End state of T12:** `git tag v1.0.1 && git push --tags` → within ~10 minutes the new build is live to internal testers. Promotion to the production track stays a manual one-click in Play Console (intentional — full-auto-to-prod is risky).

## Remaining manual gates before first ship

Things automated by this pass:

- ✅ Launcher icon replaced (palette-5 ring + "50%"). `play-assets/icon-512.png`.
- ✅ Play Store screenshots taken at 1080×2400, light mode, on the
  release build (R8-minified). `play-assets/screenshots/{1..4}.png`.
- ✅ R8 release build smoke-verified on emulator. No shrinker-induced
  crashes; all screens render.
- ✅ `FOREGROUND_SERVICE` permission removed (was unused; Play Console
  would have flagged it).
- ✅ Both repos pushed to GitHub remotes.

Still on your side:

1. **Enable GitHub Pages.** Repo Settings → Pages → Source = `main` /
   `/docs`. Confirm `https://carvalhorr.github.io/days-in-office/privacy-policy`
   resolves. The policy URL is already named in
   `PLAY_CONSOLE_CHEATSHEET.md`.

2. **Design the 1024×500 feature graphic.** Required by Play Console
   for the main listing. Suggested content in
   `STORE_LISTING.md` → "Store-listing assets checklist". Tools: Figma /
   Canva / similar. Save as `play-assets/feature-graphic-1024x500.png`.

3. **Back up the keystore.** `app/upload-keystore.jks` + the password
   from `KEYSTORE_README.md`. Three locations recommended:
   1Password (Document attach), external drive / encrypted vault,
   and the GitHub Secrets created in step 5 below.

4. **Add the five GitHub repo secrets** for the auto-publish workflow:
   `KEYSTORE_BASE64` (`base64 -i app/upload-keystore.jks | tr -d '\n'`),
   `KEYSTORE_PASSWORD` (from `KEYSTORE_README.md`),
   `KEY_ALIAS` (`upload`),
   `KEY_PASSWORD` (same as `KEYSTORE_PASSWORD`),
   `PLAY_SERVICE_ACCOUNT_JSON` (raw contents of the JSON key — see step 6).

5. **Bootstrap upload to Play Console** (the first upload has to be
   manual — gradle-play-publisher can only *update* apps Play already
   knows about):
   1. Play Console → All apps → Create app. Name "Days in the Office",
      English (UK), free, app.
   2. Paste store listing fields from `STORE_LISTING.md` (title, short
      description, full description). Upload `play-assets/icon-512.png`
      and the four screenshots from `play-assets/screenshots/`.
   3. App content → fill every section using `PLAY_CONSOLE_CHEATSHEET.md`
      answers (privacy policy URL = step 1; data safety; content
      rating; target audience 18+; ads = no; background-location
      declaration + 30-45 s justification video).
   4. Enable Play App Signing when prompted (recommended — Google
      holds the distribution key; you keep only the upload key).
   5. Internal testing → Create new release → upload
      `app/build/outputs/bundle/release/app-release.aab` manually.

6. **Create the Play service account for auto-publish** (steps in
   `PLAY_CONSOLE_CHEATSHEET.md` §16): GCP service account → grant
   *Release Manager* in Play Console → save the JSON key as the
   `PLAY_SERVICE_ACCOUNT_JSON` secret from step 4.

7. **From v1.0.1 onward**: `git tag v1.0.1 && git push --tags` runs the
   `.github/workflows/release.yml` workflow, which builds + signs the
   bundle, attaches it to a GitHub Release, and pushes to Play's
   internal testing track. Production promotion stays manual.

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
