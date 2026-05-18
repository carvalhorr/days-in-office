# Bugs — Days in Office

Bug register for the app produced by the 2026-05-17/18 claude × sonnet-4-6 experiment run (`run/claude/sonnet-4-6`). Each bug here is a candidate for a future TASK-XXX (bug-fix flavour) when we decide to implement.

Status legend: `OPEN` (logged, not investigated), `TRIAGED` (root cause confirmed, scope nailed down, ready to file as a task), `FIXED`, `WONT_FIX`.

**Triage notes added 2026-05-18 after reading the relevant source files.** Bugs 1, 3, 4, 5, 7 are TRIAGED with confirmed root causes. Bug 2 needs a separate manual visual audit. Bug 6 is a feature gap requiring a do/won't-do decision.

---

## BUG-001: No way to navigate back to Dashboard from Settings
**Found:** 2026-05-18 — **Status:** TRIAGED — **Severity:** Medium

**Confirmed root cause:** `feature/settings/ui/SettingsScreen.kt:87`:
```kotlin
TopAppBar(title = { Text("Settings") })
```
No `navigationIcon` parameter, so no back arrow in the top app bar.

**Combined with** `app/navigation/DaysInOfficeNavHost.kt:47-49`:
```kotlin
DashboardScreen(
    onNavigateToSettings = {
        navController.navigate(Destination.Settings.route)   // ← plain push, no popUpTo/launchSingleTop
    },
```
The gear icon performs a drill-in push (Dashboard → Settings on top of stack), not a tab switch.

**Mitigating but inadequate:** `app/MainActivity.kt:53` (`showBottomBar = currentDestination?.route != Destination.Onboarding.route`) — the bottom nav IS visible on Settings. So technically tapping the Dashboard tab returns to Dashboard, and system back also works. The user just didn't perceive these as "the way back" without an explicit affordance in the screen.

**Fix scope (pick one or both):**
- **A (recommended).** Add a navigation back arrow in `SettingsScreen.kt`'s TopAppBar:
  ```kotlin
  TopAppBar(
      title = { Text("Settings") },
      navigationIcon = {
          IconButton(onClick = onNavigateBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
      }
  )
  ```
  Pass `onNavigateBack: () -> Unit = { navController.popBackStack() }` from `DaysInOfficeNavHost.kt`. Show the back arrow only when `navController.previousBackStackEntry != null` (so it doesn't render when Settings is reached via the tab bar).

- **B (matches prototype literally).** Change the gear icon's `onNavigateToSettings` in `DaysInOfficeNavHost.kt:48` to do a tab switch (same pattern as `BottomNavBar.kt:30-36`):
  ```kotlin
  navController.navigate(Destination.Settings.route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
  }
  ```
  This keeps the back stack as `[Dashboard]` with Settings as the active tab; user returns to Dashboard via bottom nav, exactly as the prototype expects.

Both fixes complement each other. The prototype literally only navigates between tabs via the bottom nav — option B alone matches it best — but option A is the universally familiar convention for "drill-in" navigation. Doing both is defensible.

**Acceptance criteria for the fix:**
- Tapping the gear icon on Dashboard navigates to Settings
- From Settings, tapping the Dashboard tab in the bottom nav returns to Dashboard with state preserved
- Either: a back arrow in the Settings TopAppBar pops back to Dashboard, OR the gear icon is a tab switch so the back stack only contains Dashboard
- Compose UI test: navigate Dashboard → Settings → Dashboard via the back affordance, assert destination ends at Dashboard

---

## BUG-002: Layout doesn't match prototype — concrete divergences from 2026-05-18 audit
**Audit performed:** 2026-05-18 on the post-completion build (claude × sonnet-4-6). Screenshots persisted at `experiment/audit-shots/2026-05-18/`.

**Audit coverage:**
- ✓ Dashboard (`01_launch.png`)
- ✓ Calendar (`02_calendar.png`)
- ✓ Settings (`03_settings.png`)
- ✓ One bottom sheet — Target (`04_target_sheet.png`)
- ⏸ Onboarding 4 steps — not yet captured (requires resetting onboarding-complete via Settings → Reset; deferred to a follow-up audit pass)

**Status legend below:** each sub-bug is TRIAGED with concrete file:line scope. Severity is Cosmetic / Low / Medium per impact.

---

### BUG-002a: Icons everywhere are Material vector icons instead of prototype's emoji
**Severity:** Low (consistent, but a coherent design choice the prototype made)
**Where:** Multiple — Dashboard top app bar (gear icon), Settings row icons (Target/Period/Working-days/Wi-Fi/Geofencing/Calendar/Reset), check-in buttons, sync-now button.
**Prototype:** Uses emojis (`⚙️`, `🎯`, `📅`, `📆`, `📶`, `📡`, `📍`, `🗓️`, `🔄`, `📤`, `🔁`, `🏢`, `🏠`).
**Emulator:** Uses Material icons (`Icons.Default.Settings`, `.DateRange`, `.LocationOn`, `.Refresh`). Settings rows show a generic gear icon for `Target` (which should be `🎯` bullseye), generic calendar for `Period` (which should be `📅`), etc. The visual identity diverges noticeably.
**Files:**
  - `feature/dashboard/ui/DashboardScreen.kt:64` — `Icon(Icons.Default.Settings, ...)`
  - `feature/settings/ui/SettingsScreen.kt` — all `s-row` icons use Material vector icons (need to grep for `Icon(Icons.` and replace with `Text("emoji")`)
  - `feature/dashboard/ui/QuickCheckInButton.kt:35, 45` — button labels are "Check in" / "Remote" without the 🏢/🏠 emoji
**Fix scope:** Replace Material `Icon` calls with `Text(emoji, style = LocalTextStyle...)` in those specific places, OR introduce a small helper composable `EmojiIcon` that does the right styling consistently. Simpler is the explicit `Text` approach.

---

### BUG-002b: Stats strip — label layout is column (count over label) instead of row (count over [dot label])
**Severity:** Cosmetic
**Where:** Dashboard, stats strip (1 OFFICE / 1 REMOTE / 18 UNKNOWN row).
**Prototype:** `.stat` is a vertical block with: big number at top, then a row with `[dot] LABEL` horizontally. Items separated by `border-right: 1px solid #E6E1E5;`.
**Emulator:** Number at top, then a separate row with `[dot] LABEL` — **structurally similar but no vertical dividers between stats**. The stat numbers also feel a bit smaller (28px proto vs Material `headlineMedium` ≈ 28sp — actually probably similar).
**Files:** `feature/dashboard/ui/DashboardScreen.kt:200-235` (`StatsStrip`, `StatItem`).
**Fix scope:** Add `HorizontalDivider` (or a thin vertical `Box(modifier = Modifier.width(1.dp).height(60.dp).background(...))`) between `StatItem`s. Use the dividers' position to match prototype's `.stat` flex-with-border-right layout.

---

### BUG-002c: Dashboard period chip uses muted `secondaryContainer` color
**Severity:** Cosmetic
**Where:** Dashboard, "MAY 2026 · MONTHLY" chip below the app bar.
**Prototype:** chip has a distinctive purple/violet pill background (`background: #EADDFF` or similar Material 3 purple-50; text is dark purple).
**Emulator:** Uses `MaterialTheme.colorScheme.secondaryContainer` which on this dynamic-color emulator renders as a muted blue-grey. Visually less prominent than the prototype's purple.
**Files:** `feature/dashboard/ui/DashboardScreen.kt:166-180` (`PeriodChip`).
**Note:** This is dynamic-color-dependent. On a device with a different wallpaper the colour changes. Whether this matters depends on whether you want the period chip to be a brand-coded element (use a fixed colour like `#EADDFF`) or a Material-You element that adapts (current behaviour). Lean towards fixed for consistency with the prototype.

---

### BUG-002d: Compliance ring track stroke is thicker (visual weight off)
**Severity:** Low
**Where:** Dashboard, the donut ring.
**Prototype:** `stroke-width: 18` on the 190×190 SVG → roughly 9.5% of diameter is stroke. The track colour is `#EDE7F6` (very light lavender).
**Emulator:** Track appears thicker proportionally (looks closer to 14–16% of diameter), and the colour is a more saturated grey. Progress arc renders a small red blob at top rather than a clean arc.
**Files:** `feature/dashboard/ui/ComplianceRing.kt` (Canvas drawing — likely uses a `stroke = Stroke(width = X.dp)` value).
**Fix scope:** Reduce stroke width to match the 190:18 ratio. Lighten the track colour to `#EDE7F6`. Verify the progress arc renders smoothly — at 5% the arc should be ~18° of the circle, a small but clean arc; the current render looks like a rounded-rectangle indicator instead.

---

### BUG-002e: Bottom nav uses M3 pill indicator; prototype has no pill
**Severity:** Cosmetic
**Where:** All three main screens, bottom nav bar.
**Prototype:** Selected tab has the icon highlighted (filled vs outlined) and the label coloured; no pill background.
**Emulator:** Material 3 `NavigationBar` default — selected tab has a rounded-pill background around the icon (the `M3 NavigationBarItem` "indicator" container).
**Files:** `app/navigation/BottomNavBar.kt:18-40` — `NavigationBar { ... NavigationBarItem(...) }` uses default Material 3 styling.
**Fix scope:** Customize the `colors` parameter of `NavigationBarItem`:
```kotlin
NavigationBarItem(
    ...
    colors = NavigationBarItemDefaults.colors(
        indicatorColor = Color.Transparent  // remove the pill
    )
)
```
Plus optionally swap icons between filled (selected) and outlined (unselected) variants to match the prototype's distinction.

---

### BUG-002f: Check-in card heading/sub-text wording deviates from prototype
**Severity:** Low (copy + minor styling)
**Where:** Dashboard, check-in card.
**Prototype:** Title "Today — Friday, 8 May" + subtitle "Are you in the office today?" (when UNKNOWN) / "✓ Checked in for the office" (when OFFICE) / similar for REMOTE.
**Emulator:** Title "Today — Monday, 18 May" ✓ matches. Subtitle "Checked in — remote day" — slightly different wording but close.
**Files:** `feature/dashboard/ui/DashboardScreen.kt:245-251` (`CheckInCard`'s `subtitleText` block).
**Fix scope:** Align copy with prototype where it differs. Low priority, mostly preference.

---

### BUG-002g: Calendar month nav uses ← / → in app bar instead of separate `< Month >` row
**Severity:** Cosmetic
**Where:** Calendar screen header.
**Prototype:** App bar has just `<span class="title">Calendar</span>`. **Below** it, a `.month-nav` row with `<button>‹</button>` Month-name `<button>›</button>`.
**Emulator:** App bar shows `← May 2026 →` inline — the prev/next arrows are placed alongside the title in the top app bar, no separate month-nav row.
**Files:** `feature/calendar/ui/CalendarScreen.kt` (the top app bar setup).
**Fix scope:** Restructure to use two separate UI elements:
  1. `TopAppBar(title = { Text("Calendar") })` (no navigation arrows)
  2. A separate `Row` below it with `IconButton(prev)`, `Text(monthName)`, `IconButton(next)` — matches prototype's `.month-nav`.

---

### BUG-002h: Calendar legend is missing the "Weekend" item
**Severity:** Low (incomplete legend doesn't break function but is misleading)
**Where:** Calendar, bottom legend.
**Prototype (line 812-818):** Five items — Office (green), Remote (blue), Unknown (amber), Holiday/PTO (grey #9E9E9E), Weekend (light grey #BDBDBD).
**Emulator:** Four items — Office, Remote, Holiday/PTO, Unknown. **No Weekend entry.**
**Files:** `feature/calendar/ui/CalendarScreen.kt` (the legend block at the bottom).
**Fix scope:** Add one more legend item for `DayStatus.WEEKEND` with the light-grey colour. Verify the colour matches the dimmed/light-grey rendering of weekend cells in `MonthCalendarView.kt`.

---

### BUG-002i: Settings section header style — emulator uses `MANDATE` blue but spacing/typography differs
**Severity:** Cosmetic
**Where:** Settings, each section label (MANDATE / DETECTION / CALENDAR / DATA).
**Prototype:** Section title is bold dark blue (`#0D47A1` or similar), uppercase, with consistent spacing before each card.
**Emulator:** Same uppercase + blue colour but smaller/lighter font weight, and the spacing between sections feels tighter than prototype.
**Files:** `feature/settings/ui/SettingsScreen.kt` (section title `Text` style).
**Fix scope:** Increase font weight to `FontWeight.SemiBold` or `Bold`. Add `Modifier.padding(top = 24.dp)` for spacing above each section. Verify colour against `#0D47A1` (or a colour token aliased to brand-deep-blue).

---

### BUG-002j: Target sheet's slider widget is a custom dotted-track design — actually matches prototype well
**Severity:** Cosmetic / verify
**Where:** Settings → Target bottom sheet.
**Observation:** Surprisingly, this widget renders as a custom dotted-track slider with a vertical handle bar that **closely matches** the prototype's `.mandate-slider`. Claude implemented this well. The "50%" big number above the slider also matches.
**Files:** `feature/settings/ui/sheets/TargetSheet.kt` — actual implementation worth reviewing to extract the slider style for reuse.
**Action:** No fix needed for the slider itself. Document this as a positive baseline — when other sheets use a slider, copy the pattern from `TargetSheet.kt` rather than using the default `Slider` composable.

---

### Onboarding (not yet captured — follow-up needed)
A complete audit also requires capturing the 4 onboarding steps (`#ob0` Mandate / `#ob1` Period / `#ob2` Detection / `#ob3` Calendar). This was deferred from the 2026-05-18 audit because reaching them requires resetting `onboarding_complete = false` (Settings → Data → Reset onboarding) and then capturing each step via taps through Next/Back. Estimated ~10 min of additional capture work.

Likely sub-bugs in onboarding (from code reading):
  - `ob0` Mandate: the percentage slider should match the same dotted-track widget noted in BUG-002j; the day-chips row layout might differ
  - `ob2` Detection: the SSID picker and geofence picker visual designs are tied to BUG-003 + BUG-005 fixes (auto-detect missing)
  - Top-of-screen "step dots" indicator — prototype uses 4 visible dots with active one filled; emulator likely uses a different progress affordance

When ready to file the BUG-002 fixes as tasks, run a second audit pass for onboarding and add BUG-002k, BUG-002l, etc.

---

### Audit-level summary

| Sub-bug | Severity | Effort | Pairs with |
|---|---|---|---|
| 002a — Material icons vs emoji | Low | Medium (many call sites) | — |
| 002b — Stats strip dividers | Cosmetic | Small | — |
| 002c — Period chip colour | Cosmetic | Tiny | — |
| 002d — Compliance ring stroke | Low | Small | — |
| 002e — Bottom nav pill indicator | Cosmetic | Tiny | — |
| 002f — Check-in subtitle copy | Low | Tiny | BUG-007 (also touches QuickCheckInButton) |
| 002g — Calendar month nav row | Cosmetic | Small | — |
| 002h — Missing weekend legend item | Low | Tiny | — |
| 002i — Settings section header style | Cosmetic | Tiny | — |
| 002j — Target slider (matches well) | (no fix) | (no work) | — |

**Recommended task structure when filing:** group all of BUG-002a–j into a single TASK "UI polish to match prototype" (Phase 4 has TASK-020 reserved for this — see TASKS.md). The fixes are small individually but rely on touching many UI files at once; a single task lets the model do one big visual pass.

---

## BUG-003: Geofencing setup missing "Use current location" auto-detect
**Found:** 2026-05-18 — **Status:** TRIAGED — **Severity:** Medium

**Confirmed root cause:** Two places hand-roll geofence input, both manual-only:

- `feature/onboarding/ui/DetectionSetupStep.kt:102-157` (`GeofenceInputSection`) — three `OutlinedTextField`s (lat / lng / radius) and a "Set Location" button that just parses the typed values.
- `feature/settings/ui/sheets/GeofenceSheet.kt:82-103` — same lat/lng/radius `OutlinedTextField` pattern inside a `ModalBottomSheet`.

**Verified absent in the codebase:**
- No `FusedLocationProviderClient` references
- No `LocationServices` references
- No integration with `core/permissions/PermissionRequester` (which exists, from TASK-017)
- No data-source layer for current-location retrieval

**Fix scope:**
1. New data source `core/data/datasource/LocationProvider.kt`:
   ```kotlin
   class LocationProvider @Inject constructor(
       @ApplicationContext private val context: Context
   ) {
       suspend fun getCurrentLocation(): Result<Location> = …  // uses FusedLocationProviderClient.lastLocation or getCurrentLocation
   }
   ```
2. New shared composable `feature/shared/ui/GeofencePicker.kt`:
   - Primary "📍 Use current location" button → requests `ACCESS_FINE_LOCATION` via `PermissionRequester`, then calls `LocationProvider.getCurrentLocation()`, fills in lat/lng
   - Manual lat/lng/radius fields below as fallback
   - Error state when permission denied or location unavailable
3. Replace `GeofenceInputSection` in `DetectionSetupStep.kt` with the new `GeofencePicker`
4. Replace inline lat/lng fields in `GeofenceSheet.kt` with `GeofencePicker`
5. Wire `PermissionRequester` through from both call sites
6. Update DI: add `LocationProvider` to `DataSourceModule.kt`

**Add dependency:** `play-services-location` is already in `libs.versions.toml` (it's used by `GeofenceDetector` from TASK-009).

**Acceptance criteria:**
- Tapping "Use current location" requests location permission (if not granted) and then fetches the device's current location, populating lat/lng in the UI
- Permission-denied state shows a friendly message and falls back to manual entry
- Manual lat/lng inputs remain functional as fallback
- Unit test for `LocationProvider` (mock `FusedLocationProviderClient`)
- Compose test verifying the auto-detect button is present and clickable in both onboarding and settings entry points

**Pattern overlap:** see BUG-005 — same auto-detect-with-manual-fallback shape. The fixes should be done together so the `GeofencePicker` and `WifiSsidPicker` share the same UI structure and the data sources sit in the same package.

---

## BUG-004: PTO days are not excluded from the compliance calculation
**Found:** 2026-05-18 — **Status:** TRIAGED — **Severity:** High (breaks a key architectural invariant)

**Confirmed root cause:** Two-part bug in `core/domain/usecase/`:

**Part 1 — `GetWorkingDaysUseCase.kt:12-19`** only filters out HOLIDAY records from the `holidays` table, not `DayRecord` entries marked as PTO:
```kotlin
val holidays = holidayRepository.getHolidays(start, end).first()
val holidayDates = holidays.map { it.date }.toSet()
return generateSequence(start) { it.plusDays(1) }
    .takeWhile { !it.isAfter(end) }
    .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
    .filter { it !in holidayDates }   // ← excludes HolidayEntity rows, NOT DayRecord with status=PTO
    .toList()
```

**Part 2 — `GetComplianceUseCase.kt:61-83` (`buildResult`)** uses the unfiltered `workingDays.size` as the denominator and computes the OFFICE/REMOTE/UNKNOWN counts. A PTO day:
- Is still in `workingDays` (Part 1 didn't exclude it)
- Doesn't match `recordMap[it]?.status == DayStatus.OFFICE` → not counted as office
- Doesn't match `… == DayStatus.REMOTE` → not counted as remote
- Doesn't match `… == DayStatus.UNKNOWN` → not counted as unknown (because it's status=PTO, not UNKNOWN)
- Just silently dropped from all counts but still inflates `totalWorkingDays`

Result: marking a day PTO doesn't shrink the denominator, and the percentage doesn't change. Exactly the user's reported symptom.

**Verified working:** `DayDetailSheet.kt:98-103` does have a "Mark as PTO" button that calls `onOverrideStatus(dayRecord.date, DayStatus.PTO)`. The status persists fine. The bug is purely in the use case.

**Fix scope (recommended approach: modify `GetComplianceUseCase.buildResult`):**

```kotlin
private fun buildResult(
    config: MandateConfig,
    records: List<DayRecord>,
    workingDays: List<LocalDate>,    // potential working days (weekdays minus public holidays)
    start: LocalDate, end: LocalDate
): ComplianceResult {
    val recordMap = records.associateBy { it.date }
    // User-marked exclusions (PTO and HOLIDAY day-records) — exclude from mandate calc
    val userExcludedDays = records
        .filter { it.status == DayStatus.PTO || it.status == DayStatus.HOLIDAY }
        .map { it.date }
        .toSet()
    val effectiveWorkingDays = workingDays.filter { it !in userExcludedDays }

    val officeDays = effectiveWorkingDays.count { recordMap[it]?.status == DayStatus.OFFICE }
    val remoteDays = effectiveWorkingDays.count { recordMap[it]?.status == DayStatus.REMOTE }
    val unknownDays = effectiveWorkingDays.count {
        recordMap[it] == null || recordMap[it]?.status == DayStatus.UNKNOWN
    }
    return ComplianceResult(
        …
        totalWorkingDays = effectiveWorkingDays.size,   // shrinks when PTO is marked
        …
    )
}
```

Keeps `GetWorkingDaysUseCase` as "potential working days" (still useful for the Calendar view that wants to show every weekday). Compliance applies the user-exclusions on top.

**Acceptance criteria:**
- Unit test in `GetComplianceUseCaseTest`: given N weekday days with K marked PTO, `totalWorkingDays = N − K`
- Unit test: given a day marked PTO, `currentPercentage` recomputes against the smaller denominator
- Manual test on emulator: mark a workday as PTO → return to Dashboard → ring percentage updates
- Same logic should apply to a day marked HOLIDAY by the user (calendar sync also creates HolidayEntity rows separately; both paths must be honoured)

**Architectural note:** `ARCHITECTURE.md` §15 Key Invariant #2 mentions weekends; the implicit PTO/HOLIDAY exclusion is documented in §5.1's `ComplianceResult` field comments. Consider promoting it to an explicit numbered invariant when filing the fix.

---

## BUG-005: Wi-Fi SSID setup requires manual typing — no scan/pick-from-list
**Found:** 2026-05-18 — **Status:** TRIAGED — **Severity:** Medium

**Confirmed root cause:** Three places have manual-only SSID entry, all just text fields:

- `feature/onboarding/ui/DetectionSetupStep.kt:81-88` — `OutlinedTextField` for SSID, shown when either Wi-Fi method is enabled
- `feature/settings/ui/sheets/WifiConnectedSheet.kt:73-82` — same `OutlinedTextField`
- `feature/settings/ui/sheets/WifiScanSheet.kt:73-82` — same `OutlinedTextField` (the two settings sheets are essentially identical templates)

**Verified absent:** no `WifiManager.startScan()` references anywhere, no `getScanResults()` references, no Wi-Fi data-source layer.

**Fix scope:** structurally identical to BUG-003. Three layers:

1. New data source `core/data/datasource/WifiScanner.kt`:
   ```kotlin
   class WifiScanner @Inject constructor(
       @ApplicationContext private val context: Context
   ) {
       suspend fun scanForSsids(): Result<List<String>> = …  // wraps WifiManager.startScan() + getScanResults()
   }
   ```
   Handle Android's scan throttling (4 scans / 2 min foreground, 1 / 30 min background): return a clear error result when throttled rather than spinning forever.

2. New shared composable `feature/shared/ui/WifiSsidPicker.kt`:
   - Primary "📡 Scan for networks" button → requests `ACCESS_FINE_LOCATION` via `PermissionRequester` (required for scan results on Android 9+), calls `WifiScanner.scanForSsids()`, displays a tappable list
   - Manual SSID text input as fallback (essential for hidden SSIDs)

3. Replace text fields in all three entry points (`DetectionSetupStep.kt`, `WifiConnectedSheet.kt`, `WifiScanSheet.kt`) with the new `WifiSsidPicker`.

4. DI: add `WifiScanner` to `DataSourceModule.kt`.

**Acceptance criteria:**
- Tapping "Scan for networks" requests permission (if needed), runs a Wi-Fi scan, displays the detected SSIDs
- Tapping an SSID in the list fills it in as the selected value
- Manual entry still works for hidden SSIDs
- Throttled-scan response shows a clear message rather than indefinite loading
- Unit test for `WifiScanner` (mock `WifiManager`)
- Compose test verifying scan button is present in all three entry points

**Pattern overlap:** see BUG-003. Both should land together — they share the auto-detect-with-manual-fallback shape, the permission flow, and the data-source-in-DI pattern. Refactoring afterward would be more work than doing both at once.

---

## BUG-006: No in-app theme override (Light / Dark / Follow system)
**Found:** 2026-05-18 — **Status:** TRIAGED → DECIDE — **Severity:** Low (feature gap, not strict bug)

**Confirmed current behaviour:** `app/theme/Theme.kt:34-53`:
```kotlin
@Composable
fun DaysInOfficeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),    // follow-system by default
    content: @Composable () -> Unit
) { … }
```
`MainActivity` calls `DaysInOfficeTheme { … }` with no argument, so it uses the default → follows the system theme. **There is no DataStore reading; no override mechanism.**

**Decision required before this can become a task.** Two valid choices:

- **Won't do.** Many apps don't expose an in-app theme toggle and just follow the OS. This is the simpler choice; aligns with the prototype (which has no theme row); keeps Settings tight.
- **Do.** Add a Settings → Display → Theme row with a 3-option sheet (System / Light / Dark) per modern Android convention. The implementation scope is small:
  - New DataStore key `theme_mode: String` in `PreferencesDataSource.kt`
  - New repository method `getThemeMode(): Flow<ThemeMode>` / `setThemeMode(...)` (or extend `MandateConfigRepository`)
  - New `feature/settings/ui/sheets/ThemeSheet.kt` (3-option radio list)
  - New row in `SettingsScreen.kt` ("Theme: System")
  - `MainActivity` reads the preference and passes `darkTheme = …` to `DaysInOfficeTheme`
  - **Prototype update:** if "do", `prototype/index.html` should add a Display section with this row to stay aligned. Otherwise BUG-002 (layout mismatch) becomes permanent on this surface.

**Recommendation:** lean **won't-do** unless there's a specific user-stated need. The OS-level theme switch on Android/iOS is well-known and works correctly today. Adding an in-app override is a small but real maintenance surface (DataStore migrations, snapshot tests for both themes, etc.) for a feature most users won't touch. Revisit if usage data shows demand.

**If do, acceptance criteria:**
- Settings → Theme row visible with current value (e.g. "System")
- Tapping opens a sheet with 3 radio options (System, Light, Dark)
- Selection persists across app restarts (verify with a `PreferencesDataSourceTest`)
- The active theme honours the override (`MainActivity` correctly forwards the value to `DaysInOfficeTheme`)
- Prototype updated to include the Display section in Settings

---

## BUG-007: Can't change today's check-in after the first selection
**Found:** 2026-05-18 — **Status:** TRIAGED — **Severity:** Medium

**Confirmed root cause:** `feature/dashboard/ui/QuickCheckInButton.kt:31, 40`:
```kotlin
Button(
    onClick = onCheckInOffice,
    enabled = !isConfirmedToday,   // ← disables BOTH buttons once today is confirmed
    …
)
OutlinedButton(
    onClick = onCheckInRemote,
    enabled = !isConfirmedToday,   // ← disables BOTH buttons once today is confirmed
    …
)
```

This is the literal implementation of TASK-014's "disabled state when today's record is already `confirmedByUser = true`" acceptance criterion. It over-applies the architecture's Key Invariant #1 ("Never overwrite a user-confirmed record with automated detection"), which is specifically about `DetectionOrchestrator`, not about the user's own subsequent taps.

**Verified safe to change:** `DashboardViewModel.kt:83-93`:
```kotlin
fun checkInAsOffice() {
    viewModelScope.launch { recordOfficeDayUseCase(LocalDate.now(), DetectionMethod.MANUAL) }
}
fun checkInAsRemote() {
    viewModelScope.launch { recordRemoteDayUseCase(LocalDate.now()) }
}
```
The ViewModel does NOT block confirmed re-writes — it would happily update the record if called. The bug is purely in the UI's `enabled` predicate.

**Verified safe at the use-case level:** `DetectionOrchestrator` (TASK-010, separate file) is the layer that honours the never-overwrite invariant for automated detection. User-driven taps from `QuickCheckInButton` go through `RecordOfficeDayUseCase` / `RecordRemoteDayUseCase`, which are user-initiated and should overwrite freely.

**Fix scope:**
1. `QuickCheckInButton.kt` — remove the `enabled = !isConfirmedToday` predicate. Both buttons always enabled.
2. Add a visual "selected" state so the user can see which choice is current:
   - When `todayRecord?.status == DayStatus.OFFICE`: Office button uses filled `Button`, Remote button uses `OutlinedButton` (current default)
   - When `todayRecord?.status == DayStatus.REMOTE`: Office uses `OutlinedButton`, Remote uses filled `Button`
   - When confirmed but neither (shouldn't happen, but defensive): both `OutlinedButton`
3. Update the signature: pass `currentStatus: DayStatus?` instead of `isConfirmedToday: Boolean` so the composable can render the active selection.
4. `DashboardScreen.kt:273-278` — update the call site to pass `currentStatus = todayRecord?.status`.

**Acceptance criteria:**
- Tapping either button updates today's record regardless of `confirmedByUser` state
- The currently-selected button has a visually distinct style (filled vs outlined)
- Re-tapping the same button is idempotent (no flicker / no error)
- Unit test in `DashboardViewModelTest`: assert that calling `checkInAsOffice()` after a previous `checkInAsRemote()` updates the record from REMOTE to OFFICE
- Verify the `DetectionOrchestrator` invariant about "never overwrite confirmed by automated" still holds — that path is in `core/detection/DetectionOrchestrator.kt`, not touched by this fix

**Related:** see BUG-004. Both bugs were caused by over-strict guarding around user-confirmed records. The fixes are independent but reflect the same architectural-invariant misunderstanding.

---

## Triage summary

| Bug | Status | Severity | Notes |
|---|---|---|---|
| BUG-001 | TRIAGED | Medium | Add back arrow to Settings TopAppBar + consider tab-switch for gear icon |
| BUG-002 | TRIAGED (a–j) | Cosmetic-Low | Audit 2026-05-18 produced 10 sub-bugs (BUG-002a..j). Onboarding capture deferred (would add 002k+). File as one combined "UI polish" task |
| BUG-003 | TRIAGED | Medium | New `LocationProvider` + `GeofencePicker`; bundle with BUG-005 fix |
| BUG-004 | TRIAGED | **High** | Two-line fix in `GetComplianceUseCase.buildResult` to exclude PTO/HOLIDAY records |
| BUG-005 | TRIAGED | Medium | New `WifiScanner` + `WifiSsidPicker`; bundle with BUG-003 fix |
| BUG-006 | TRIAGED → DECIDE | Low | Feature gap, not bug. Lean won't-do unless demand emerges |
| BUG-007 | TRIAGED | Medium | One-line change in `QuickCheckInButton` + visual selected-state pass |

**Recommended grouping for task filing:**

- **Standalone, high-value:** BUG-004 (PTO calculation) — highest severity, smallest fix scope, ships invariant-correctness
- **Standalone, small:** BUG-001 (Settings nav), BUG-007 (check-in re-selection) — each ~half-day, independent
- **Group together:** BUG-003 + BUG-005 — share infrastructure (`LocationProvider`/`WifiScanner` pattern, `PermissionRequester` integration, shared picker composables)
- **Block on decision:** BUG-006 — need a yes/no first
- **Bundle as one task:** BUG-002a–j (UI polish) — many small fixes across UI files; cheaper to do in one pass than spread across multiple tasks. Onboarding sub-audit (002k+) can fold in if you want a second audit pass first.
