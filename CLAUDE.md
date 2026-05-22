# Days in Office — product repo

Android app that tracks compliance with an in-office mandate. The implementation
was promoted from the LLM-coding-benchmark experiment (its full history lives
in the sibling `days-in-office-experiment` repo). This repo is the shipped
product — Play Store release pipeline is documented in `DEPLOYMENT.md`.

## Project structure

- `app/src/` — Kotlin sources, tests, resources for the Android app.
- `app/build.gradle.kts` — module build script (signing + R8 configured here).
- `app/proguard-rules.pro` — R8 keep rules.
- `app/upload-keystore.jks` — Play upload key (gitignored).
- `keystore.properties` — keystore credentials lookup (gitignored).
- `docs/privacy-policy.md` — privacy policy, also hosted on GitHub Pages.
- `ARCHITECTURE.md` — design doc carried over from the experiment fixture; still
  the authoritative reference for module boundaries and invariants.
- `DECISIONS.md` — product-level decision log.
- `DEPLOYMENT.md` — Play Store release plan (T1–T13 checklist).
- `STORE_LISTING.md` — title, descriptions, asset checklist for Play Console.
- `PLAY_CONSOLE_CHEATSHEET.md` — answers for data-safety / content-rating / etc.
- `.github/workflows/` — CI (build/test on PR, signed `.aab` on tag).

## Project commands

```bash
# Debug build
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug

# Signed release bundle (needs keystore.properties present)
./gradlew bundleRelease

# Instrumented tests (needs connected device or emulator)
./gradlew connectedDebugAndroidTest
```

## Key invariants (do not violate)

1. Automated detection never writes a `DayRecord` directly. Detection only
   fires a confirmation prompt; only an explicit user action (in-app tap or
   notification action) writes a `DayRecord`.
2. Weekends are always excluded from mandate calculations.
3. `ComplianceResult.daysNeededToComply` is never negative.
4. One `DayRecord` per calendar date.
5. Detection workers only run on configured working days (Mon–Fri default).
6. Domain layer (`core/domain`) has zero Android dependencies.
7. ViewModels never reference Room entities or DataStore directly — only
   domain models.

## Known issues at ship

- Wi-Fi SSID reads `<unknown ssid>` on some devices (BUG-025 in the experiment
  repo). Geofence + manual check-in are the supported detectors at 1.0; Wi-Fi
  is shipped as opt-in beta. Track follow-up in this repo's issues.
