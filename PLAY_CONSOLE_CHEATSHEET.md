# Play Console cheat-sheet — Days in Office v1.0

A drop-in answer set for every Play Console form that gates the first
release. Open Play Console → App content (and adjacent sections), match
the section headings below, paste/select.

---

## 1. App access

> "Is all of your app's functionality available without any access
> restrictions, such as a login?"

**Answer:** *All functionality is available without special access*.

The app has no account, no login, no paywall. No credentials need to be
provided to the Play reviewer.

---

## 2. Ads

> "Does your app contain ads?"

**Answer:** *No, my app does not contain ads*.

No ad SDK is bundled. (Verifiable: `grep -ri "admob\|adsense\|admanager" app/src/` returns nothing.)

---

## 3. Content rating

Use Play Console's IARC questionnaire. Answers tuned for "productivity
utility, no objectionable content":

| Question | Answer |
|---|---|
| Violence | None |
| Sexuality | None |
| Language | None |
| Controlled Substances | None |
| Gambling | None |
| User-generated content | None (single-user, no upload/share) |
| Shares user's physical location with other users | **No** |
| Allows users to interact / communicate / exchange content | **No** |
| Personal info is shared with other users | **No** |
| Allows digital purchases | **No** |
| Web browser / unrestricted internet | **No** |
| Miscellaneous (gambling references, social media features, etc.) | All No |

**Expected rating:** *Everyone / PEGI 3 / USK 0+ / IARC Generic 3+*.

---

## 4. Target audience and content

> "What is the target age group for your app?"

**Answer:** *Ages 18 and over*. The app is aimed at working adults
tracking a corporate in-office mandate.

> "Are children one of the intended audiences of your app?"

**Answer:** *No*.

> "Do you have ads in your app that could appeal to children?"

**Answer:** *No ads in this app*.

---

## 5. News apps

> "Is your app a news app?"

**Answer:** *No*.

---

## 6. COVID-19 contact tracing and status apps

**Answer:** *My app is not a publicly available COVID-19 contact tracing
or status app*.

---

## 7. Data safety form

This is the big one. Fill it as below.

### 7.1 Data collection and security

> "Does your app collect or share any of the required user data types?"

**Answer:** *Yes* (because we store location for geofencing — qualifies
even though it never leaves the device).

> "Is all of the user data collected by your app encrypted in transit?"

**Answer:** *Yes — no data is transmitted; the criterion is vacuously
true*. (Play Console accepts this; the "in transit" question covers the
case where data does leave.)

> "Do you provide a way for users to request that their data be deleted?"

**Answer:** *Yes — via Settings → Privacy → Delete all data*.

> "Has your app been independently validated against a global security
> standard?"

**Answer:** *No*.

### 7.2 Data types — declare for each

For each data type, the answer follows this template:

* Collected? **Yes** (the app reads it / stores it locally)
* Shared with third parties? **No**
* Required or optional? **Optional** (each detection method is opt-in)
* Used for: **App functionality** only
* Processing: **Processed ephemerally on device — Not transmitted off device**

Specific declarations:

| Data type | Why declared | Purpose | Required/Optional |
|---|---|---|---|
| **Location → Approximate location** | Geofence detection uses coarse location for the broad fix | App functionality | Optional |
| **Location → Precise location** | Geofence detection uses fine location for the radius check | App functionality | Optional |
| **App activity → App interactions** | We store "did the user tap Yes on a detection notification?" | App functionality, Analytics?: **No** | Optional |
| **App info and performance → Crash logs** | **Do NOT declare.** We do not collect crash logs. |  |  |
| **Device or other IDs** | **Do NOT declare.** We do not read advertising ID, IMEI, or any other device identifier. |  |  |

For the two location types, Play's UI will surface the secondary
"Is this data processed ephemerally?" — answer **Yes** for both.

### 7.3 Data sharing summary

* **Is any of the collected data shared with third parties?** No.
* **Is any of the collected data sold?** No.

---

## 8. Government apps

**Answer:** *No, my app is not a government app*.

---

## 9. Financial features

**Answer:** *My app doesn't provide any financial features*.

---

## 10. Health apps

**Answer:** *No*.

---

## 11. Permissions declaration (the big one — background location)

Required because the manifest declares `ACCESS_BACKGROUND_LOCATION`.

### 11.1 Permission usage statement

```
The app uses background location only to detect when the user
arrives at an office geofence that the user has explicitly
configured. The location data is processed on-device, stored only
as a single "office day" flag for the day in question, and is
never transmitted, shared, or sold. The background access is
required because geofence triggers fire while the app is not in
the foreground.

The feature is opt-in: a new user does not see a background
location prompt until they enable geofence detection in Settings.
Disabling geofence detection or revoking the permission stops all
background location use.
```

### 11.2 Justification video (Play requires one)

**Length target:** 30 – 45 seconds. Screen-record on a real device or
emulator.

**Storyboard:**

1. (0–5 s) Onboarding screen, narration: "Days in Office is an
   on-device app that tracks whether you went to your office each
   workday."
2. (5–15 s) Settings → Detection → Geofence row. Tap it. Show the
   "Add office" map screen. Narration: "Geofencing is one of four
   ways to detect office attendance. The user defines the office
   location here."
3. (15–25 s) After the geofence is added, jump to: device leaves
   the geofence (simulate by quitting the app and walking out — or
   use the Android emulator's geofence simulator). When the user
   re-enters, a notification fires: "Were you at the office today?"
   Narration: "When the device enters the configured geofence, the
   app sends a confirmation notification. Background location is
   required because the user has put the app in the background by
   then."
4. (25–35 s) User taps Yes on the notification. Dashboard updates.
   Narration: "The day is logged only after the user explicitly
   confirms. No location history is kept — only the office-day
   yes/no for the day in question."
5. (35–45 s) Settings → toggle off Geofence. Narration: "Background
   location is opt-in and can be revoked at any time."

Upload as unlisted YouTube; paste the URL into Play Console.

---

## 12. News + claims, government + claims

Skip — not applicable.

---

## 13. App category

**Category:** *Productivity*.
**Tags:** *time management, productivity, work, attendance*.

---

## 14. Store settings

* **Default language:** English (UK) — `en-GB`.
* **Email contact:** carvalhorr@gmail.com (matches the privacy policy).
* **Phone contact:** leave blank.
* **Website:** `https://carvalhorr.github.io/days-in-office/` (the same
  GitHub Pages host as the privacy policy; the index page can be a
  short blurb pointing to the privacy policy and a link to the Play
  Store listing once live).
* **Privacy policy URL:** `https://carvalhorr.github.io/days-in-office/privacy-policy`
  (see `docs/privacy-policy.md` — must be live before submission).

---

## 15. Release types and recommended order

1. **Internal testing track** — first push. T11/T12 CI uploads land
   here automatically on tag push. Add yourself as the sole tester.
2. **Closed testing (alpha)** — once you've installed the internal
   track build and confirmed the four core flows work on your real
   device.
3. **Open testing (beta)** — optional. Useful if you want a few
   colleagues to dogfood.
4. **Production** — manual promotion in Play Console, not automated.

---

## 16. Wiring the CI auto-upload (T12)

The `.github/workflows/release.yml` workflow uses
[gradle-play-publisher](https://github.com/Triple-T/gradle-play-publisher)
to push the signed bundle to the **internal** testing track on every
`v*.*.*` tag. Before this works, you need a Play service account.

### One-time setup

1. **Google Cloud → IAM & Admin → Service Accounts.** Create a service
   account named e.g. `days-in-office-publisher`. No GCP roles needed
   (Play API is granted separately). Generate a JSON key and download
   it (this is the only time it's shown — re-key if lost).
2. **Play Console → Setup → API access.** Link the GCP project that
   contains the service account, then "Grant access" to the account
   with permissions:
   - **App permissions:** Days in Office (this app).
   - **Account permissions:** *Release manager* (lets it create, edit,
     and roll out releases on the internal track, but not full Admin).
3. **GitHub repo → Settings → Secrets and variables → Actions.** Add
   four repo secrets, matching what `release.yml` expects:
   - `KEYSTORE_BASE64` — `base64 -i app/upload-keystore.jks | tr -d '\n'`
   - `KEYSTORE_PASSWORD` — value from `KEYSTORE_README.md`
   - `KEY_ALIAS` — `upload`
   - `KEY_PASSWORD` — same as `KEYSTORE_PASSWORD` (we use one password)
   - `PLAY_SERVICE_ACCOUNT_JSON` — the **raw contents** of the
     downloaded service-account JSON (paste the whole `{ ... }`
     blob; the workflow does **not** base64-decode this one).

### Bootstrap step (must happen before the first auto-upload)

`gradle-play-publisher` can only upload to an app that already exists in
Play Console. For v1, you have to do one manual upload to seed it:

1. Build the `.aab` locally (`./gradlew bundleRelease` — already done at T6).
2. Play Console → All apps → "Create app". Fill the inline metadata.
3. Internal testing → Create new release → upload `app-release.aab`
   manually. This registers the app + applicationId + signing
   fingerprint with Play.
4. **Enable Play App Signing** when prompted (recommended — see
   `KEYSTORE_README.md`).
5. From v1.0.1 onward, `git tag v1.0.1 && git push --tags` triggers
   `release.yml`, builds and signs the `.aab` in CI, attaches it to a
   GitHub Release, and pushes it to Play's internal track.

### Verifying

Push a test tag like `v0.0.1-dryrun` on a throwaway branch first. The
workflow will:
- Build the signed bundle ✓
- If `PLAY_SERVICE_ACCOUNT_JSON` is set: attempt `publishReleaseBundle`.
- If not set: skip Play upload, just attach the bundle to the GitHub
  Release (the workflow logs a `::warning::` line).

On first push: GPP error "App with package ... not found" → that's the
bootstrap step talking; finish the manual upload first.

## 17. Final pre-submission checklist

Tick all before clicking "Submit to internal testing":

- [ ] Launcher icon: real glyph, not the placeholder circle.
- [ ] 512×512 hi-res icon uploaded.
- [ ] 1024×500 feature graphic uploaded.
- [ ] Four phone screenshots uploaded.
- [ ] Title, short description, full description pasted.
- [ ] Privacy policy URL live and pasted.
- [ ] Data safety form completed per §7.
- [ ] Content rating questionnaire completed per §3.
- [ ] Target audience set to 18+ per §4.
- [ ] Background-location justification video uploaded per §11.
- [ ] App access form = no restrictions (§1).
- [ ] App is signed by upload key (§7.4 of DEPLOYMENT.md / T5).
- [ ] `versionCode = 1`, `versionName = "1.0"` in `app/build.gradle.kts`.
- [ ] Keystore backed up to at least one off-device location.
