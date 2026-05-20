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

## BUG-008: Home tab does nothing after opening Settings via the gear icon
**Found:** 2026-05-19 — **Status:** OPEN — **Severity:** Medium

**Observed behaviour (emulator, 2026-05-19):** From Dashboard, tap the gear icon in the top-right to open Settings. While on Settings, tap the **Home** tab in the bottom navigation bar. Nothing happens — the user remains on Settings with no working way back to Dashboard short of system back.

**Note vs BUG-001:** BUG-001 was triaged 2026-05-18 with an assumption that "tapping the Dashboard tab returns to Dashboard" worked as a mitigation. That assumption is wrong — see this bug. BUG-001 has already been actioned (fix landed) with the "back arrow in Settings TopAppBar" route. This bug captures the remaining underlying nav-stack issue that the back-arrow fix sidestepped rather than resolved.

**Suspected cause:** `app/navigation/DaysInOfficeNavHost.kt:47-49` — the gear icon does a plain `navController.navigate(Destination.Settings.route)` push, placing Settings on top of `[Dashboard]`. The bottom-nav `onClick` in `app/navigation/BottomNavBar.kt:32-40` uses `popUpTo(graph.startDestinationId) { saveState = true }` + `launchSingleTop = true` + `restoreState = true`. With Dashboard *already* on the back stack (start destination, just covered by Settings), Nav-Compose appears to treat the navigate-to-Dashboard as a no-op rather than popping Settings. Final root cause to be confirmed during the fix task.

**Desired fix direction (user, 2026-05-19):** Do **not** add or rely on a back button in Settings. Instead, ensure the Home tab in the bottom navigation always returns the user to Dashboard from any other destination — including when Settings was opened via the gear icon.

**Fix scope (likely — to be confirmed during implementation):** Make the gear icon's `onNavigateToSettings` in `DaysInOfficeNavHost.kt:48` use the same tab-switch nav options as `BottomNavBar.kt:33-39`:
```kotlin
navController.navigate(Destination.Settings.route) {
    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```
This keeps the back stack as `[Dashboard]` with Settings as the active destination, so tapping Home in the bottom nav restores Dashboard via the normal tab-switch path. If after this change the Home tab still fails to navigate when no drill-in push is on the stack, dig deeper — the bug then lies in the bottom nav's `popUpTo`/`launchSingleTop` interaction with the current destination.

**Acceptance criteria:**
- From Dashboard, tap gear → Settings opens
- From Settings (opened via gear), tap Home tab → returns to Dashboard with state preserved
- From Settings (opened via gear), tap Calendar tab → goes to Calendar; from there tap Home → returns to Dashboard
- No back arrow / `navigationIcon` is added to `SettingsScreen.kt`'s TopAppBar as part of this fix
- Compose UI test: Dashboard → (gear) → Settings → (Home tab) → Dashboard, assert final destination is Dashboard

**Related:** BUG-001 (same surface; fixed via back-arrow route which left this underlying nav-stack issue unaddressed).

---

## BUG-009: Empty white strip above the TopAppBar title
**Found:** 2026-05-19 — **Status:** OPEN — **Severity:** Low

**Observed behaviour (emulator, 2026-05-19):** A blank/white horizontal strip is visible at the top of the app, above the screen title in the `TopAppBar`. The title sits lower than expected, with an empty gap between it and the very top edge of the screen. Reads as a status-bar-inset handling issue, not a deliberate design space.

**Where observed:** Dashboard (Settings/Calendar to be re-checked — likely the same since they share the `TopAppBar` pattern). The prototype (`prototype/index.html`) has no such gap; the title sits flush under the status bar with the app's primary color extending behind the status bar.

**Suspected cause (not confirmed — for the fix task to verify):**
- `app/MainActivity.kt:31` calls `enableEdgeToEdge()`, which makes the app draw under the system bars. Combined with the `Scaffold` propagating `paddingValues` (which includes the status-bar inset) to the `NavHost`, each screen's content — including the `TopAppBar` — is pushed below the status bar. The `TopAppBar` then *also* applies its own window insets internally (Material 3 `TopAppBar` consumes status-bar insets by default), double-counting the inset and producing the visible blank strip.
- Likely fix: either (a) stop forwarding the status-bar portion of `paddingValues` into screens that already have a `TopAppBar` (let the `TopAppBar` own the inset), or (b) pass `windowInsets = WindowInsets(0)` to each `TopAppBar` and let the Scaffold's `paddingValues` handle it once. Pick whichever matches the prototype's "color behind the status bar" expectation — option (a) is the usual Material 3 idiom.

**Files likely touched:**
- `app/MainActivity.kt` — `Scaffold` content padding handling
- `app/navigation/DaysInOfficeNavHost.kt` — modifier propagation
- Each screen's `Scaffold` / `TopAppBar` — Dashboard, Settings, Calendar

**Acceptance criteria:**
- On Dashboard, Settings, and Calendar: no blank strip above the title; the title sits directly under the status bar
- Status-bar area is filled with the app's primary/surface color (matching the prototype), not white
- Visual regression check via screenshot test or manual capture against `experiment/audit-shots/`

**Related:** BUG-002 (overall prototype divergence audit) — this is a layout/inset issue distinct from the BUG-002 sub-bugs but in the same "match the prototype" family.

---

## BUG-010: Dashboard check-in buttons (Office / Remote) do nothing when tapped
**Found:** 2026-05-19 — **Status:** OPEN — **Severity:** High

**Observed behaviour (emulator, 2026-05-19):** On the Dashboard, tapping the **Office** check-in button or the **Remote** check-in button produces no visible change — no selected state, no stats update, no compliance ring change. The buttons appear to be a no-op end-to-end from the user's perspective.

**Wiring observed in code (looks correct at every layer — actual failure is elsewhere):**
- `feature/dashboard/ui/QuickCheckInButton.kt:35,43` → both Office button paths call `onCheckInOffice`
- `feature/dashboard/ui/QuickCheckInButton.kt:54,62` → both Remote button paths call `onCheckInRemote`
- `feature/dashboard/ui/DashboardScreen.kt:101-102` → wires the lambdas to `viewModel::checkInAsOffice` / `viewModel::checkInAsRemote`
- `feature/dashboard/DashboardViewModel.kt:83-93` → calls `recordOfficeDayUseCase(LocalDate.now(), DetectionMethod.MANUAL)` / `recordRemoteDayUseCase(LocalDate.now())` inside `viewModelScope.launch`

So the click → lambda → ViewModel → use case chain is wired. "Nothing happens" must be one of:
1. **Use case throws and the exception is swallowed by `viewModelScope.launch`** (no `try/catch`, no `CoroutineExceptionHandler`, no error UI). The user sees no toast, no log surface — the click silently fails. Likely candidates: a Room write failing because no schema migration / no inserted prerequisite row, or an invariant guard in the use case rejecting the write (cf. BUG-007's "overwrite confirmed" guard pattern).
2. **Use case succeeds but the Dashboard `Flow` doesn't re-emit.** The Dashboard `uiState` is built from a `Flow` (line ~75 in `DashboardViewModel.kt`); if the upstream repo `Flow` isn't observing the changed `DayRecord` (e.g. one-shot read instead of Room `Flow<DayRecord>`), the UI never updates. User reads this as "nothing happens".
3. **Use case succeeds and state updates, but the visual selected-state of the buttons isn't bound to `today`'s record** — i.e. the click works, the data updates, the stats strip updates, but the *button itself* doesn't show "Office: selected", so the user perceives no feedback. This is the failure mode adjacent to BUG-007's "visual selected-state pass" note.

**Diagnostic suggestions for the fix task:**
- Add a temporary `Log.d` in `checkInAsOffice` / `checkInAsRemote` to confirm the lambda fires
- Wrap the use case call in `runCatching` and surface failures via a `Snackbar` or error state — even if the long-term fix is something else, never swallow these silently
- Inspect Room: after a tap, `adb shell run-as ... cat .../databases/days_in_office.db` (or run a unit test) to confirm whether the `DayRecord` row is being written
- Check the repo's `observeDay(date)` / `observeAllDays()` returns a Room `Flow`, not a one-shot `suspend fun`

**Likely files:**
- `feature/dashboard/DashboardViewModel.kt` — error handling around `viewModelScope.launch`
- `core/domain/usecase/RecordOfficeDayUseCase.kt`, `RecordRemoteDayUseCase.kt` — possible silent rejection
- `core/data/repository/DayRecordRepositoryImpl.kt` (or equivalent) — Flow vs suspend, write path
- `feature/dashboard/ui/QuickCheckInButton.kt` — visual selected-state bound to today's record type

**Acceptance criteria:**
- Tapping Office on a clean day records a `DayRecord(date=today, type=OFFICE, confirmedByUser=true)` and the Dashboard reflects it (stats strip count goes up, compliance ring updates, the Office button shows as selected)
- Tapping Remote on a clean day does the equivalent for REMOTE
- If the use case fails for any reason, a user-visible error appears (Snackbar) — no silent swallowing
- Unit test on `DashboardViewModel` covering both check-in paths, asserting the repo write occurs and the state flow emits
- Compose UI test: tap Office → assert stats strip Office count incremented

**Related:**
- BUG-007 (can't change today's check-in *after the first* selection) — different symptom (works once then breaks) vs. this bug (doesn't appear to work at all). May share a root cause if the use case is silently rejecting writes; verify before deduping.
- BUG-002f (check-in card wording) and BUG-002 (general visual selected-state pass) — cosmetic neighbours; do not conflate.

---

## BUG-011: App crashes when opening Wi-Fi (Connected / Scan) or Geofencing settings — shared `@HiltViewModel` factory misuse in pickers
**Found:** 2026-05-19 — **Status:** OPEN — **Severity:** High

**Observed behaviour (emulator, 2026-05-19):** From Settings, tapping any of the following rows crashes the app:
- **Wi-Fi (Connected)** — opens a `ModalBottomSheet` embedding `WifiSsidPicker`
- **Wi-Fi (Scan)** — same picker
- **Geofencing** — opens a `ModalBottomSheet` embedding `GeofencePicker`

All three rows crash on the same code path (picker composition), with the same class of failure.

**Confirmed root cause (one bug, two files):**

`feature/shared/ui/WifiSsidPicker.kt:73`:
```kotlin
@Composable
fun WifiSsidPicker(
    currentSsid: String?,
    onUpdate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WifiSsidPickerViewModel = viewModel()   // ← wrong factory
)
```

`feature/shared/ui/GeofencePicker.kt:75`:
```kotlin
viewModel: GeofencePickerViewModel = viewModel()       // ← wrong factory, same mistake
```

Both `WifiSsidPickerViewModel` and `GeofencePickerViewModel` are declared `@HiltViewModel` with injected constructors (`WifiScanner` and `LocationProvider` / equivalent respectively). The plain `viewModel()` call from `androidx.lifecycle.viewmodel.compose` does **not** know about Hilt's factory and falls back to the default factory, which requires a no-arg constructor. The VMs have none, so instantiation throws — almost certainly along the lines of:

```
java.lang.RuntimeException: Cannot create an instance of class ...PickerViewModel
  Caused by: java.lang.NoSuchMethodException: ...PickerViewModel.<init> []
```

The crash fires the first time the bottom sheet's content composes, which is immediately on tap → matches all three observed symptoms.

**Fix scope (two near-identical one-line changes, plus imports):**

```kotlin
// at top of BOTH WifiSsidPicker.kt AND GeofencePicker.kt
import androidx.hilt.navigation.compose.hiltViewModel

// in each picker's signature:
viewModel: WifiSsidPickerViewModel = hiltViewModel()
viewModel: GeofencePickerViewModel = hiltViewModel()
```

**Files to change:**
- `feature/shared/ui/WifiSsidPicker.kt` — replace `viewModel()` with `hiltViewModel()`
- `feature/shared/ui/GeofencePicker.kt` — same change

**Verification step before fixing:**
- Capture the actual stack trace from `adb logcat *:E` while reproducing the tap (try one of the Wi-Fi rows and the Geofencing row; expect both traces to be the same class of crash, just naming different VM classes). If the trace matches the `Cannot create an instance` pattern above, ship the two-line fix. If it's something else (e.g. a `SecurityException` from `WifiManager` / `LocationManager` due to a missing manifest permission, or a `NullPointerException` inside the scanner/provider on emulator), update this bug entry with the real trace and rescope.

**Audit step (catch any others):**
- `grep -rn "viewModel()" --include="*.kt" app/src/main/` and for every match where the target VM is `@HiltViewModel`, switch to `hiltViewModel()`. This avoids surfacing the same bug elsewhere in the future (e.g. if a new picker is added).

**Acceptance criteria:**
- Tapping Settings → Wi-Fi (Connected) opens the bottom sheet without crashing
- Tapping Settings → Wi-Fi (Scan) opens the bottom sheet without crashing
- Tapping Settings → Geofencing opens the bottom sheet without crashing
- Enabling the relevant switch reveals the picker, and the "Scan for networks" / "Use current location" action either works or fails with a visible message — not a crash
- Project-wide grep confirms no other `@HiltViewModel` is instantiated via plain `viewModel()`
- Instrumented Compose UI test (or unit-test pattern) that asserts each picker composes without throwing

**Related:**
- BUG-003 / BUG-005 — both call out the picker/scanner pattern as needing an auto-detect implementation. This bug is about the *existing* pickers crashing on entry, independent of those feature gaps. Fix BUG-011 first; BUG-003/005 are layered on top.

---

## BUG-012: Geofence coordinates do not persist between sessions
**Found:** 2026-05-20 — **Status:** OPEN — **Severity:** Medium

**Observed (device, 2026-05-20):** In Settings → Geofencing, set latitude / longitude / radius (manually or via auto-detect) and Save. Close and re-open the app, return to Settings → Geofencing — the values are blank again. As a consequence, the user cannot validate geofence-based detection on a physical device because the coordinates are lost the moment they're needed.

**Suspected scope:** One or more of:
- `SettingsViewModel.updateGeofence(enabled, lat, lng, radius)` calls a repository method that doesn't actually write all four fields to DataStore (e.g. the enabled flag persists but lat/lng/radius don't).
- The write path is correct but the read path doesn't seed `DetectionConfig.geofenceLatitude / geofenceLongitude / geofenceRadiusMeters` from DataStore on app start, so the sheet's `currentLat` / `currentLng` / `currentRadius` come in null.
- The Save handler in `GeofenceSheet.kt` constructs the save payload with the wrong field names (e.g. raw Strings vs Doubles, swallowed parse exception).

**Files to inspect:**
- `feature/settings/ui/sheets/GeofenceSheet.kt` — `onSave` payload construction
- `feature/settings/SettingsViewModel.kt` — `updateGeofence(...)` function
- `core/data/repository/DetectionConfigRepositoryImpl.kt` (or equivalent) — DataStore read/write of geofence fields
- The DataStore keys for `geofenceLatitude / geofenceLongitude / geofenceRadiusMeters`

**Diagnostic suggestion:** After Save, dump DataStore from adb (`adb shell run-as <pkg> cat .../datastore/...`) to confirm whether the values are on disk. If on disk but not displayed, the read path is broken. If not on disk, the write path is broken.

**Acceptance criteria:**
- Set geofence in Settings → Save → kill and re-open the app → reopen Settings → Geofencing — lat/lng/radius are populated with the previously saved values.
- The `DetectionOrchestrator.geofenceDetector` uses the persisted values across process restarts (i.e. detection actually works on the next run).
- Unit test on the repository round-trip (write then read all three fields).

**Related:** BUG-003 (the feature gap, addressed by TASK-026); BUG-011 (Hilt factory crash blocks reaching this UI on fresh state — must be fixed first to even reproduce BUG-012).

---

## BUG-013: Wi-Fi scan does not list any available networks
**Found:** 2026-05-20 — **Status:** OPEN — **Severity:** Medium

**Observed (device, 2026-05-20):** In Settings → Wi-Fi (scan only) → Enable → tap "Scan for networks". The user expects a tappable list of nearby SSIDs per TASK-026's design. Instead, no list appears — either the scan silently returns empty or the result isn't rendered. Same likely true for the Wi-Fi (connected) sheet's picker.

**Most likely causes (one of):**
1. **Permission missing.** `WifiManager.startScan()` requires `ACCESS_FINE_LOCATION` (runtime, since Android 8/9) plus `CHANGE_WIFI_STATE` (manifest). If the picker requests location permission but Android 13+ also needs `NEARBY_WIFI_DEVICES`, the scan succeeds with an empty list. Verify manifest declarations and the picker's `permissionRequester.request(...)` set.
2. **Throttling.** Android imposes a 30s rolling cap on `startScan()` calls; in foreground apps that's 4/120s. If the user opened the picker multiple times during testing, subsequent scans return cached empty results without surfacing the throttle error.
3. **Result-render path silently drops.** `WifiSsidPicker.kt` renders the success state with an empty list as "No networks detected." — which may be what the user sees but interpreted as "nothing happens" because it's small/under-styled.

**Files to inspect:**
- `AndroidManifest.xml` — confirm `CHANGE_WIFI_STATE`, `ACCESS_WIFI_STATE`, and `NEARBY_WIFI_DEVICES` (API 33+) are declared
- `core/data/datasource/WifiScanner.kt` — the actual scan invocation + result handling (success vs failure, empty handling, throttle detection)
- `feature/shared/ui/WifiSsidPicker.kt` — the state-rendering branches
- Logcat filter: `adb logcat -s WifiScanner WifiManager WifiHal`

**Acceptance criteria:**
- On a device with Wi-Fi enabled and at least one visible network in range, tapping "Scan for networks" with location permission granted produces a tappable list of SSIDs.
- If the scan fails (throttle, denied permission, Wi-Fi off), a user-visible explanatory error is shown — not a silent empty state.
- Manual SSID entry remains available as fallback (already works per TASK-026).
- The empty state's "No networks detected." text is visually clearer (e.g. larger / centred / with a retry button).

**Related:** BUG-005 (the original feature gap, addressed by TASK-026); BUG-011 (Hilt factory crash blocks reaching the picker until fixed).

---

## BUG-014: Remove "Export data" / "Export CSV" from Settings → Data
**Found:** 2026-05-20 — **Status:** OPEN — **Severity:** Low (scope cleanup)

**Observed:** Settings → Data section contains an Export CSV row. The product currently has no requirement for data export — the use case was never specified. The row exists likely because the prototype or an earlier scope draft included it. Remove for now; revisit if and when a real export need surfaces.

**Scope — Files to Modify:**
- `feature/settings/ui/SettingsScreen.kt` — remove the Export row from `DataSection`.
- `feature/settings/ui/sheets/` — remove any export-related bottom sheet, if one exists.
- `feature/settings/SettingsViewModel.kt` — remove the `exportData()` (or similar) function and its dependencies.
- `core/domain/usecase/ExportDataUseCase.kt` (if it exists) — remove.
- `core/data/repository/...` — if the repository has an `exportToCsv` method used exclusively by the above, remove. If unused but harmless, optional cleanup.
- `prototype/index.html` — remove the export entry too so the prototype and emulator stay aligned (per the "prototype is visual source of truth" rule).

**Do not remove:**
- The underlying `DayRecordDao` read queries — they're harmless and may be useful later.
- The Reset onboarding row (separate concern, see BUG-015).

**Acceptance criteria:**
- "Export" / "Export CSV" no longer appears in Settings.
- No dead code remains referencing the removed feature.
- Unit/lint passes.
- Prototype matches the emulator.

---

## BUG-015: "Reset onboarding" row is unclear in label and visually grouped with the (to-be-removed) export feature
**Found:** 2026-05-20 — **Status:** OPEN — **Severity:** Low/Medium (UX)

**Observed:** The "🔁 Reset onboarding" row sits in Settings → Data, visually adjacent to the Export CSV row. The user reads them as related actions. Two distinct issues:

(a) **Purpose is not obvious from the label.** From the code (`SettingsViewModel.resetOnboarding()`), it flips `mandateConfig.onboardingComplete = false` so the next app launch lands back on the onboarding wizard. This is intended for "re-run the setup wizard if my config drifted". Nothing about the label "Reset onboarding" communicates that — it sounds destructive (will it wipe my data?).

(b) **Visual grouping is misleading.** Sitting in "Data" alongside Export reads as a data operation. It isn't — it's a setup-flow restart. The grouping needs to change once Export is removed (BUG-014).

**Proposed fix:**
- Rename to **"Re-run setup wizard"** (or "Restart setup"). Add a sub-label / description: "Walks you through the initial setup again. Your data is kept."
- Move to its own section, e.g. **"Setup"**, distinct from "Data".
- Optional: confirmation dialog ("This will return you to the setup wizard. Continue?") to remove the ambiguity around whether data is wiped.

**Files:**
- `feature/settings/ui/SettingsScreen.kt` — `DataSection` → move the row out into a new `SetupSection` (or rename `DataSection` if it ends up empty after BUG-014).
- The row's label / description.
- Optionally: add a confirmation dialog before invoking `onResetOnboarding`.
- `prototype/index.html` — mirror the section + label change.

**Acceptance criteria:**
- Row label clearly conveys "go back to the setup wizard, keep my data".
- Row is in its own section, visually separated from any data-modification actions.
- The action itself behaves the same (`onboardingComplete = false`; no data loss).

**Related:** BUG-014 (export removal opens up the Data section's layout).

---

## BUG-016: Geofence sheet has redundant "Set Location" button alongside "Save" — likely the underlying cause of BUG-012
**Found:** 2026-05-20 — **Status:** OPEN — **Severity:** Low (UX), but probably the real root cause of BUG-012's persistence failure

**Observed (device, 2026-05-20):** The Geofence bottom sheet exposes two action buttons that both *appear* to commit values:
- **"Set Location"** inside `GeofencePicker` — per TASK-026's spec, "only enabled when all 3 fields are valid"; intended to apply the picker's local state to the parent sheet's draft.
- **"Save"** at the bottom of the sheet — per every other Settings sheet's convention, supposed to persist the sheet's draft to DataStore.

The user reports that **clicking "Set Location" makes values persist**, but that "Save" alone does not. This is exactly the symptom one would expect if the picker stores its lat/lng/radius in *its own* local `remember` state and only calls the parent's `onUpdate(...)` when "Set Location" is tapped — meaning the sheet's `draftLat / draftLng / draftRadius` stay stale (or null) until that button is pressed. "Save" then persists whatever is in the sheet's draft — which is empty when the user hasn't tapped "Set Location" first.

This makes BUG-016 the most plausible *underlying cause* of BUG-012, and TASK-031 (BUG-012's planned fix) should pick the right resolution: not "fix the write path" but "remove the extra button and make value-edits flow into the draft immediately."

**Fix scope:**
- **Remove the "Set Location" button** from `GeofencePicker`. Every value change inside the picker (text-field edits, "Use current location" success) should call `onUpdate(lat, lng, radius)` directly. The parent sheet's draft then always reflects the latest picker state.
- The single **"Save"** button on the sheet remains the only persistence trigger — same UX as `TargetSheet`, `PeriodSheet`, etc.
- Same change for any other call site of `GeofencePicker` (onboarding flow).

**Files to modify:**
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/shared/ui/GeofencePicker.kt` — remove the `Button("Set Location")`; route every internal state change through `onUpdate(...)`.
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/sheets/GeofenceSheet.kt` — confirm `onUpdate = { lat, lng, radius -> draftLat = lat; draftLng = lng; draftRadius = radius }` fires for every change.
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/DetectionSetupStep.kt` — same picker call site; same change.
- `prototype/index.html` — mirror the simpler UX.

**Acceptance criteria:**
- "Set Location" button is gone from the Geofence picker (both in Settings sheet and Onboarding step).
- Editing lat/lng/radius (or tapping "Use current location") immediately reflects in the sheet's draft state — no intermediate apply action needed.
- Tapping Save once persists the latest values to DataStore.
- BUG-012's symptom is resolved: set values → tap Save → kill app → re-launch → values are still there.

**Related:**
- **BUG-012** — geofence coords don't persist. BUG-016 is most likely the same bug seen from the UX angle. Resolving BUG-016 probably closes BUG-012 too; if not, the remaining gap is genuine DataStore plumbing.
- **BUG-003 / TASK-026** — the auto-detect feature; the redundant "Set Location" button was introduced by TASK-026's picker design. Removing it doesn't undo TASK-026, just streamlines the apply mechanism.
- **TASK-031** — the planned BUG-012 fix should be re-scoped to target BUG-016's resolution as its primary work.

---

## BUG-017: Dashboard Office button still can't override a prior Remote selection — TASK-024 regression; rename "Check in" → "Office" to clarify the toggle
**Found:** 2026-05-20 — **Status:** OPEN — **Severity:** Medium — **Supersedes:** BUG-007 (whose fix in TASK-024 did not resolve the symptom)

**Observed (device, 2026-05-20):** From the Dashboard, tap **🏠 Remote** for today (record persists). Then tap **🏢 Check in** to switch the record to OFFICE — nothing happens. The user remains unable to toggle freely between Office and Remote for today. This is the same symptom BUG-007 reported, even after TASK-024 was marked DONE in the prior orchestrator run; either the fix didn't actually land in the buttons' click path, or it covered only one direction (e.g. Office→Remote works but Remote→Office doesn't).

**Symmetric-label suggestion (user, 2026-05-20):** The current button labels are **"🏢 Check in"** and **"🏠 Remote"** — asymmetric. Renaming the office button to **"🏢 Office"** makes it clear at a glance that these are mutually-exclusive states the user can toggle between, not "primary action vs secondary". This change alone makes the broken toggle behaviour more glaring (which is good — it forces the fix) and matches every other "two-state selector" pattern in the app.

**Fix scope (two parts, same commit):**

1. **Functional — actually allow re-selection in both directions.**
   - Audit `RecordOfficeDayUseCase` and `RecordRemoteDayUseCase` for any guard that prevents overwriting an existing same-date record. The "never overwrite confirmedByUser = true with automated detection" invariant must remain, but **manual** clicks from the Dashboard are not automated detection — they are explicit user intent and must always replace the record's `status`.
   - Confirm the `DashboardViewModel.checkInAsOffice() / checkInAsRemote()` paths invoke the use case with `confirmedByUser = true` (since the user explicitly tapped) and that the use case's upsert path honours that.
   - Add a unit test on both use cases: pre-existing OFFICE record + `checkInAsRemote()` → record becomes REMOTE; pre-existing REMOTE record + `checkInAsOffice()` → record becomes OFFICE.

2. **Cosmetic — rename "🏢 Check in" → "🏢 Office".**
   - `feature/dashboard/ui/QuickCheckInButton.kt:39` and `:48` — change the two `Text("🏢 Check in")` to `Text("🏢 Office")`.
   - `prototype/index.html` — mirror the rename so the prototype source-of-truth matches.
   - `MainFlowSmokeTest.kt` — update the finder in `b01_dashboardCheckInOfficeButtonRespondsToClick`: `hasText("🏢 Check in")` → `hasText("🏢 Office")`.

**Files:**
- `core/domain/usecase/RecordOfficeDayUseCase.kt`
- `core/domain/usecase/RecordRemoteDayUseCase.kt`
- `core/data/repository/DayRecordRepositoryImpl.kt` (if the upsert lives there)
- `feature/dashboard/DashboardViewModel.kt`
- `feature/dashboard/ui/QuickCheckInButton.kt`
- `prototype/index.html`
- `app/src/androidTest/kotlin/com/carvalhorr/daysInOffice/smoke/ui/MainFlowSmokeTest.kt`
- Relevant unit tests under `app/src/test/.../core/domain/usecase/`

**Acceptance criteria:**
- From a clean state: tap 🏠 Remote → record = REMOTE; tap 🏢 Office → record = OFFICE; tap 🏠 Remote again → record = REMOTE. All three transitions persist and re-render Dashboard immediately (no app restart needed).
- The 🏢 button reads "Office" everywhere (Dashboard, prototype, smoke test).
- Unit tests cover both `Office→Remote` and `Remote→Office` overwrites at the use-case level.
- `MainFlowSmokeTest.b01_*` continues to pass after the text rename.
- The "confirmedByUser true won't be overwritten by automated detection" invariant remains intact (key invariant #1 in CLAUDE.md).

**Related:**
- **BUG-007** — the original "can't change today's check-in" report. TASK-024 was its fix; this entry exists because that fix did not resolve the symptom in practice.
- **BUG-010** — Dashboard check-in click had "no visible state change". TASK-029 was its fix. If TASK-029's repository-Flow rework is correct, the UI *would* re-render once the use case actually writes the new status; so BUG-017's likely root cause is in the use case / upsert layer, not the UI layer.
- **BUG-002f** — already notes a copy issue around the check-in subtitle; cluster cosmetic check-in copy decisions together.

---

## BUG-018: Marking a day as PTO from the Calendar still doesn't reduce the working-days denominator — TASK-022 regression
**Found:** 2026-05-20 — **Status:** OPEN — **Severity:** High — **Supersedes:** BUG-004 (whose fix in TASK-022 did not resolve the symptom)

**Observed (device, 2026-05-20):** From Calendar, tap a weekday → DayDetailSheet → tap "Mark as PTO". The day's status updates to PTO on the calendar view (visible status change). However, on returning to Dashboard:
- The **total working days** count is unchanged.
- The **compliance percentage** is unchanged.
- The **compliance ring** is unchanged.

Expected: marking a working day as PTO should reduce `totalWorkingDays` by 1, recompute the percentage against the smaller denominator (so e.g. 5 OFFICE of 10 working days = 50% becomes 5 OFFICE of 9 effective = ~55.5%), and re-render Dashboard. This is exactly what BUG-004 described and what TASK-022's spec required — but the symptom persists.

**Possible regression points (one or more):**

1. **TASK-022's fix didn't actually land.** Confirm `GetComplianceUseCase.buildResult` was updated per TASK-022's spec — the `userExcludedDays` set computed from `records.filter { it.status == DayStatus.PTO || it.status == DayStatus.HOLIDAY }` and subtracted from `workingDays`. If the existing code is the original (numerator-only handling), TASK-022 never ran or was reverted.
2. **Compliance Flow doesn't re-emit when a record is updated to PTO.** The use case is correct, but `DashboardViewModel.state` doesn't observe the repository as a reactive Flow — so the new PTO record never propagates to the compliance recalculation until the next process restart. This is the BUG-010 family failure mode applied to PTO writes.
3. **Calendar's `markAsPto` writes through a different repository path than Dashboard reads from** — e.g. a different DAO method that bypasses the Flow observers.

**Diagnostic order (cheapest first):**
- Grep `GetComplianceUseCase.kt` for `PTO` and `HOLIDAY`. If the filter is present, hypothesis 1 is ruled out — go to hypothesis 2.
- Kill and re-launch the app after marking PTO. If the percentage NOW reflects the change, hypothesis 2 is confirmed (write reaches DB, read isn't reactive). If still unchanged after restart, hypothesis 1 or 3 is the cause.

**Files to inspect:**
- `core/domain/usecase/GetComplianceUseCase.kt` — confirm post-TASK-022 PTO/HOLIDAY exclusion
- `core/data/repository/DayRecordRepositoryImpl.kt` — confirm `getAll() / observeAll()` returns a Room `Flow<List<DayRecordEntity>>` consumed by both Dashboard and Calendar VMs
- `feature/dashboard/DashboardViewModel.kt` — confirm `state` collects from the Flow that re-emits on record updates
- `feature/calendar/CalendarViewModel.kt` — confirm `markAsPto` writes through the same repo path Dashboard reads from
- `core/domain/usecase/RecordPtoUseCase.kt` (if it exists) — confirm it writes a `DayRecord(status = PTO, confirmedByUser = true)` rather than just inserting a holiday-table row

**Acceptance criteria:**
- Mark a working day as PTO from Calendar → Dashboard immediately shows the updated `totalWorkingDays` and percentage (no app restart needed).
- The PTO/HOLIDAY filter in `GetComplianceUseCase.buildResult` is verifiably present (grep- and test-checkable).
- Unit test on `GetComplianceUseCase`: given a 10-working-day window with 2 days marked PTO, `totalWorkingDays == 8` and the percentage uses 8 as denominator.
- Compose UI test (or smoke addition): mark today as PTO from the Calendar day-detail sheet → return to Dashboard → assert the stats strip changed.
- `daysNeededToComply` never goes negative even after the denominator changes.

**Related:**
- **BUG-004** — original report; TASK-022 was its planned fix.
- **BUG-010 / TASK-029** — Dashboard re-render after record changes. If TASK-029 was complete, hypothesis 2 should be ruled out by reading the code; if not complete, BUG-018's UI symptom inherits BUG-010's root cause.
- **Smoke-test gap** — `MainFlowSmokeTest` currently has no test covering "mark PTO → assert Dashboard denominator changed". Adding one as part of BUG-018's fix would prevent the next regression.

---

## BUG-019: Detection infrastructure is never wired up at app start — geofence not registered, worker not scheduled
**Found:** 2026-05-20 — **Status:** OPEN — **Severity:** **High** (silent feature failure)

**Observed (device, 2026-05-20):** The user sets a geofence in Settings → Geofencing, enables detection, walks into the office, and waits. Expected: today's `DayRecord` is automatically marked OFFICE (or a confirmation prompt appears). Actual: nothing happens — no record change, no notification, the user is stranded thinking the feature is silently broken.

**Confirmed root cause (greppable):**

```bash
$ grep -rn "DayDetectionWorker.schedule\|setupGeofence()" app/src/main/kotlin/
# only the function DEFINITIONS appear — no call sites.
```

Two pieces of infrastructure exist but are never invoked at app start:

1. **`GeofenceDetector.setupGeofence()`** registers a `Geofence` (enter + exit transitions) via Google's `GeofencingClient`. Without this call, Android Location services never start watching for the configured region, so `GeofenceBroadcastReceiver.onReceive()` cannot fire. No `geofenceInside` writes ever happen.

2. **`DayDetectionWorker.schedule(workManager)`** enqueues the periodic worker that aggregates detector signals into a `DayRecord` every 2h on weekdays 07:00–19:00. Without this call, even if a geofence broadcast somehow fired, no worker ever runs to act on it.

Both functions exist, both are tested in isolation (per existing unit tests), neither is reachable from any user action.

**Where the calls should live:**

- `setupGeofence()` should be called when:
  - The user saves Settings → Geofencing with detection enabled (write-side trigger). `SettingsViewModel.updateGeofence(...)` is the natural hook.
  - The app starts up if the user already has geofencing enabled from a previous session. `DaysInOfficeApp.onCreate()` is the natural hook for this.
  - It should be **un-registered** when the user disables geofencing, to avoid a stale Geofence pinging an old coordinate.

- `DayDetectionWorker.schedule(workManager)` should be called from `DaysInOfficeApp.onCreate()` after the WorkManager configuration is set up. `WorkManager.enqueueUniquePeriodicWork(...)` with `ExistingPeriodicWorkPolicy.KEEP` makes this safe to call every launch.

**Likely files to modify:**
- `app/DaysInOfficeApp.kt` — call `DayDetectionWorker.schedule(WorkManager.getInstance(this))` in `onCreate()`. If geofencing is enabled in persisted state, also call `GeofenceDetector.setupGeofence()` (likely via a `DetectionWiringInitializer` class to keep `App` thin).
- `feature/settings/SettingsViewModel.kt` — in `updateGeofence(enabled = true, ...)`, additionally invoke `GeofenceDetector.setupGeofence()`. In `updateGeofence(enabled = false, ...)`, invoke `GeofenceDetector.removeGeofence()` (add that method to mirror setup).
- `core/detection/detector/GeofenceDetector.kt` — add `removeGeofence()` that calls `geofencingClient.removeGeofences(...)`.

**Required manifest permission (verify):**
- `ACCESS_BACKGROUND_LOCATION` (API 29+) is required for geofence transitions to fire when the app is backgrounded. If only `ACCESS_FINE_LOCATION` is granted, the geofence registers but never fires events from background. The permission flow must escalate to background after the user grants foreground.

**Acceptance criteria:**
- App start: `DayDetectionWorker` is scheduled (verify via `adb shell dumpsys jobscheduler | grep day_detection`).
- App start, with geofencing previously enabled: the configured geofence is registered (verify via `adb logcat -s GeofenceHardwareImpl` or similar).
- User saves Settings → Geofencing → enables: the geofence is registered immediately (no app restart required).
- User disables geofencing: the geofence is removed.
- Walking into the geofenced area triggers `GeofenceBroadcastReceiver.onReceive()` → `geofenceInside = true` lands in DataStore.
- Next worker run (at most 2h later) reads `geofenceInside`, writes today's `DayRecord` as OFFICE, Dashboard updates.
- For testing: a debug-only "Run detection now" button or `adb shell cmd jobscheduler run` recipe in the runbook so we don't have to wait 2h to verify.
- Background location permission flow handled — request it after foreground location is granted.

**Related:**
- **TASK-009 / TASK-010** (original run) — created the detector + worker classes but never wired them up. The orchestrator marked those tasks DONE because per-task QA was "unit test passes" — the integration gap was invisible to that signal.
- **BUG-020** (filed below) — adds a user-visible notification for the silent-detection case.

---

## BUG-020: Detection must require user confirmation before writing today's record — no silent auto-mark
**Found:** 2026-05-20 — **Status:** OPEN — **Severity:** Medium (data-integrity / user-trust)

**Observed (device, 2026-05-20):** The user expects a notification asking "You're at the office — mark today as Office?" when detection fires, NOT a silent auto-mark. The current architecture writes a `DayRecord(... confirmedByUser=false ...)` immediately on detection — which has a concrete user-facing failure mode (see "drive-by" scenario below).

**Why confirmation is required, not just an undo affordance** (user direction 2026-05-20):

> "There is a possibility that I pass by the office and the detection would trigger and replace a remote day as in office. For this reason I want to have a confirmation for when office is detected."

The user is identifying a real false-positive class:
- **Drive-by:** dropping the kid off at school passes within geofence radius → ENTER transition fires → today silently becomes OFFICE. User was actually remote.
- **Brief Wi-Fi catch:** phone roams near the office briefly and picks up the SSID → silent OFFICE write. Same problem from a different detector.
- **Already-decided remote day:** user manually marked today REMOTE (`confirmedByUser=true`). Key Invariant #1 protects this from automated overwrite, BUT only if it was explicitly confirmed. If the user *hadn't* yet marked the day (the typical morning case), drive-by would silently land OFFICE.

**Required behaviour:** detection fires a notification; nothing is written to the DB until the user confirms. The `DayRecord(confirmedByUser=false)` write path is removed for detection events — replaced with "fire notification, await user choice".

**Design:**

1. Detector emits a positive signal (geofence enter, Wi-Fi SSID match, scan match).
2. `DetectionOrchestrator` checks: does today already have a `confirmedByUser=true` record? If yes, ignore the detection event entirely (Invariant #1 already covers this; no notification either, to avoid noise).
3. Otherwise, fire a notification: **"Are you at the office? Mark today as Office day?"** with two actions:
   - **"Yes, Office"** → writes `DayRecord(status=OFFICE, confirmedByUser=true)` (user explicitly confirmed, so it's a manual decision per Invariant #1).
   - **"No, dismiss"** → no write. Detection events for the rest of the day for the same detector are suppressed (one prompt per detector per day).
4. Tapping the notification body (not an action button) opens the Dashboard with the prompt rendered in-app (same Yes / No choices). For users who don't engage with the notification system actions.
5. If the user takes no action and the notification times out / is swept, no write happens. The day remains UNKNOWN.

This makes detection **suggestive**, not **authoritative** — the user is always the sole authority for what today is. Matches Invariant #1 ("never overwrite confirmedByUser=true with automated detection") in spirit: extends the invariant to "never write at all without user confirmation".

**Files:**
- `core/detection/DetectionOrchestrator.kt` — remove the `DayRecord(confirmedByUser=false)` write path. Replace with a "fire confirmation prompt" path. Add suppression state (per-detector per-day) so the user isn't pinged repeatedly.
- `notification/DetectionPromptNotificationWorker.kt` (new) — sibling of `DailyCheckInNotificationWorker`. Posts the confirmation notification with two `PendingIntent` actions.
- `notification/DetectionPromptActionReceiver.kt` (new) — `BroadcastReceiver` that handles "Yes, Office" / "No, dismiss" notification actions; calls `RecordOfficeDayUseCase` on Yes.
- `core/detection/DetectionConfig.kt` (or wherever notification suppression lives) — track which detectors have already prompted for today.
- `AndroidManifest.xml` — POST_NOTIFICATIONS (API 33+) flow already in place per existing notification work; ensure the new channel is created.
- `feature/dashboard/DashboardScreen.kt` — when launched from a detection notification (via Intent extra), show the same Yes / No prompt in-app rather than just landing on Dashboard.

**Acceptance criteria:**
- Walking into the geofenced area triggers a single notification within the next worker run.
- Tapping "Yes, Office" → today's record becomes OFFICE with `confirmedByUser=true`. Dashboard reflects the change.
- Tapping "No, dismiss" → no write happens; today's record stays as it was. No further prompts from the same detector for the rest of the day.
- If today already has `confirmedByUser=true` (any status), no notification fires — the user already decided.
- **Drive-by test:** walk into geofence then walk out without confirming. Verify no `DayRecord` was written.
- **Already-remote test:** mark today REMOTE manually, then walk into geofence. Verify no notification fires and no write happens.
- A dedicated notification channel `DETECTION_PROMPT` exists so the user can mute this without muting daily check-in reminders.
- Smoke-test addition: simulate a positive detector signal in test, assert no DB write happens until the confirmation action fires.

**Related:**
- **BUG-019** — prerequisite; without detection actually wiring up, this UX is never exercised.
- **Key Invariant #1** in CLAUDE.md — should be strengthened to: *"automated detection never writes a `DayRecord` directly; it only fires a confirmation notification."* Worth a follow-up `docs(architecture)` update.
- **BUG-007 / BUG-017** — user-driven re-selection (Office ↔ Remote ↔ PTO) must still work from Dashboard regardless of confirmation flow. Unrelated to this bug but in the same data-authority neighbourhood.

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
| BUG-008 | OPEN | Medium | Home tab is a no-op when Settings opened via gear; fix likely = tab-switch nav options on gear-icon push. No back arrow. |
| BUG-009 | OPEN | Low | Blank white strip above TopAppBar title; suspected double-application of status-bar inset (edge-to-edge + Scaffold padding + TopAppBar own insets). |
| BUG-010 | OPEN | **High** | Dashboard Office/Remote check-in buttons appear to do nothing. Wiring looks correct — suspect silent use-case failure swallowed by `viewModelScope.launch`, non-reactive repo Flow, or unbound visual selected-state. Verify vs BUG-007 before deduping. |
| BUG-011 | OPEN | **High** | App crashes when opening Wi-Fi (Connected/Scan) **and** Geofencing settings. Confirmed root cause: both `WifiSsidPicker.kt:73` and `GeofencePicker.kt:75` use `viewModel()` instead of `hiltViewModel()` for a `@HiltViewModel`. Two-line fix + project-wide audit. |
| BUG-012 | OPEN | Medium | Geofence lat/lng/radius do not persist between sessions. Suspect either write path doesn't hit DataStore for all three fields or read path doesn't seed the sheet's `currentLat/Lng/Radius`. Repository round-trip test would catch. |
| BUG-013 | OPEN | Medium | Wi-Fi "Scan for networks" produces no list. Suspect missing `NEARBY_WIFI_DEVICES` (API 33+), or throttling, or silent empty-state render. Audit manifest + logcat. |
| BUG-014 | OPEN | Low | Remove unspecified "Export CSV" row from Settings → Data. No product requirement; revisit later. Delete row + use-case + sheet + prototype entry. |
| BUG-015 | OPEN | Low/Medium | "Reset onboarding" label is unclear; visually grouped with Export (BUG-014). Rename to "Re-run setup wizard" + move to its own section + clarify "your data is kept". |
| BUG-016 | OPEN | Low (UX) — but probably the real BUG-012 root cause | Geofence picker has a redundant "Set Location" button. Likely explanation: picker holds its own local state, only calls `onUpdate` when that button is tapped, so Save persists stale draft. Remove the button; route every change through `onUpdate(...)`. |
| BUG-017 | OPEN | Medium | Dashboard Office button still can't override prior Remote (TASK-024 regression of BUG-007). Plus rename "🏢 Check in" → "🏢 Office" for symmetry with Remote. Two-part fix: use-case re-selection guards + label rename. |
| BUG-018 | OPEN | **High** | Marking a day PTO from Calendar still doesn't reduce Dashboard's working-days denominator (TASK-022 regression of BUG-004). Check whether buildResult filter actually landed; if yes, the regression is in repo-Flow reactivity (BUG-010 family). |
| BUG-019 | OPEN | **High** | Detection infrastructure never wired up at app start — `setupGeofence()` and `DayDetectionWorker.schedule(...)` are defined but never called. Plumbing exists, on/off switch was never flipped. Likely from TASK-009/010 integration gap. |
| BUG-020 | OPEN | Medium | Detection must REQUIRE confirmation before writing today's record. User flagged drive-by false positives (passing the office en route elsewhere → silent OFFICE write replaces correct REMOTE). Detection becomes suggestive (notification only); nothing written until user confirms. Tightens Invariant #1. |

**Recommended grouping for task filing:**

- **Standalone, high-value:** BUG-004 (PTO calculation) — highest severity, smallest fix scope, ships invariant-correctness
- **Standalone, small:** BUG-001 (Settings nav), BUG-007 (check-in re-selection) — each ~half-day, independent
- **Group together:** BUG-003 + BUG-005 — share infrastructure (`LocationProvider`/`WifiScanner` pattern, `PermissionRequester` integration, shared picker composables)
- **Block on decision:** BUG-006 — need a yes/no first
- **Bundle as one task:** BUG-002a–j (UI polish) — many small fixes across UI files; cheaper to do in one pass than spread across multiple tasks. Onboarding sub-audit (002k+) can fold in if you want a second audit pass first.
