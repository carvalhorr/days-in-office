# Days in Office — Architecture Document

> **Purpose:** This document is the authoritative reference for all agents implementing the Days in Office Android app. Read it fully before starting any task. Do not deviate from the structure defined here without updating this document first.

---

## 1. App Overview

**Days in Office** is an Android app that automatically tracks whether a user went to the office each workday and calculates their compliance with a configurable in-office mandate (default: 50%). It supports multiple detection methods, excludes PTO and public holidays from the calculation, and integrates with device calendars to read time-off data.

---

## 2. Tech Stack

| Component | Library / Version |
|---|---|
| Language | Kotlin 2.0.0 |
| Min SDK | 28 (Android 9.0) |
| Target SDK | 35 |
| Build Tools | AGP 8.5.0 |
| UI | Jetpack Compose (BOM 2024.09.00) |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt 2.51.1 |
| Database | Room 2.6.1 |
| Background | WorkManager 2.9.1 |
| Navigation | Navigation Compose 2.8.0 |
| Preferences | DataStore Preferences 1.1.1 |
| Location | Google Play Services Location 21.3.0 |
| Coroutines | Kotlinx Coroutines 1.8.1 |
| Unit Testing | JUnit 5.10.2 + MockK 1.13.12 |
| Flow Testing | Turbine 1.1.0 |
| Compose Testing | Compose UI Test (from BOM) |
| Serialization | Kotlinx Serialization 1.7.1 |

---

## 3. Package Structure

```
com.carvalhorr.daysInOffice/
├── app/
│   ├── DaysInOfficeApp.kt          # @HiltAndroidApp Application class
│   └── MainActivity.kt             # Single activity, hosts NavHost
│
├── core/
│   ├── data/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt      # Room database
│   │   │   ├── dao/
│   │   │   │   ├── DayRecordDao.kt
│   │   │   │   └── HolidayDao.kt
│   │   │   └── converter/
│   │   │       └── TypeConverters.kt
│   │   ├── repository/
│   │   │   ├── DayRecordRepositoryImpl.kt
│   │   │   ├── HolidayRepositoryImpl.kt
│   │   │   └── MandateConfigRepositoryImpl.kt
│   │   └── datasource/
│   │       ├── CalendarDataSource.kt
│   │       └── PreferencesDataSource.kt
│   │
│   ├── domain/
│   │   ├── model/
│   │   │   ├── DayRecord.kt
│   │   │   ├── DayStatus.kt        # enum: OFFICE, REMOTE, HOLIDAY, PTO, WEEKEND, UNKNOWN
│   │   │   ├── MandateConfig.kt
│   │   │   ├── MandatePeriod.kt    # enum: WEEKLY, MONTHLY, QUARTERLY, ROLLING_4_WEEKS
│   │   │   ├── DetectionMethod.kt  # enum: WIFI_CONNECTED, WIFI_SCAN, GEOFENCE, MANUAL
│   │   │   ├── DetectionConfig.kt
│   │   │   ├── ComplianceResult.kt
│   │   │   └── Holiday.kt
│   │   ├── repository/
│   │   │   ├── DayRecordRepository.kt      # interface
│   │   │   ├── HolidayRepository.kt        # interface
│   │   │   └── MandateConfigRepository.kt  # interface
│   │   └── usecase/
│   │       ├── GetComplianceUseCase.kt
│   │       ├── GetCalendarMonthUseCase.kt
│   │       ├── RecordOfficeDayUseCase.kt
│   │       ├── RecordRemoteDayUseCase.kt
│   │       ├── SyncCalendarUseCase.kt
│   │       └── GetWorkingDaysUseCase.kt
│   │
│   └── detection/
│       ├── DetectionOrchestrator.kt
│       ├── detector/
│       │   ├── WifiConnectedDetector.kt
│       │   ├── WifiScanDetector.kt
│       │   ├── GeofenceDetector.kt
│       │   └── ManualDetector.kt
│       ├── worker/
│       │   └── DayDetectionWorker.kt
│       └── receiver/
│           └── GeofenceBroadcastReceiver.kt
│
├── feature/
│   ├── onboarding/
│   │   ├── OnboardingViewModel.kt
│   │   └── ui/
│   │       ├── OnboardingScreen.kt
│   │       ├── MandateSetupStep.kt
│   │       ├── PeriodSelectionStep.kt
│   │       ├── DetectionSetupStep.kt
│   │       └── CalendarSetupStep.kt
│   │
│   ├── dashboard/
│   │   ├── DashboardViewModel.kt
│   │   └── ui/
│   │       ├── DashboardScreen.kt
│   │       ├── ComplianceRing.kt
│   │       └── QuickCheckInButton.kt
│   │
│   ├── calendar/
│   │   ├── CalendarViewModel.kt
│   │   └── ui/
│   │       ├── CalendarScreen.kt
│   │       ├── MonthCalendarView.kt
│   │       └── DayDetailSheet.kt
│   │
│   └── settings/
│       ├── SettingsViewModel.kt
│       └── ui/
│           └── SettingsScreen.kt
│
├── widget/
│   ├── CheckInWidget.kt
│   └── CheckInWidgetReceiver.kt
│
└── notification/
    ├── NotificationScheduler.kt
    └── DailyCheckInNotificationWorker.kt
```

---

## 4. Data Layer

### 4.1 Room Entities

#### `DayRecordEntity`
```kotlin
@Entity(tableName = "day_records")
data class DayRecordEntity(
    @PrimaryKey val date: LocalDate,        // ISO date, stored as String via TypeConverter
    val status: DayStatus,                  // stored as String
    val detectionMethod: DetectionMethod?,  // stored as String, null if not yet determined
    val confirmedByUser: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

#### `HolidayEntity`
```kotlin
@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey val date: LocalDate,  // stored as String via TypeConverter
    val name: String,
    val isPublicHoliday: Boolean,     // false = PTO
    val source: String                // "CALENDAR", "MANUAL"
)
```

### 4.2 DAOs

#### `DayRecordDao`
```kotlin
interface DayRecordDao {
    fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<DayRecordEntity>>
    fun getByDate(date: LocalDate): Flow<DayRecordEntity?>
    suspend fun upsert(record: DayRecordEntity)
    suspend fun delete(date: LocalDate)
}
```

#### `HolidayDao`
```kotlin
interface HolidayDao {
    fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<HolidayEntity>>
    suspend fun upsert(holiday: HolidayEntity)
    suspend fun deleteByDateRange(start: LocalDate, end: LocalDate)
    suspend fun deleteAll()
}
```

### 4.3 Room TypeConverters

All `LocalDate` values stored as `String` in ISO-8601 format (`yyyy-MM-dd`).
All `Instant` values stored as `Long` (epoch milliseconds).
All enums stored as `String` (name).

### 4.4 AppDatabase

```kotlin
@Database(
    entities = [DayRecordEntity::class, HolidayEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase()
```

---

## 5. Domain Layer

### 5.1 Core Models

#### `DayRecord`
```kotlin
data class DayRecord(
    val date: LocalDate,
    val status: DayStatus,
    val detectionMethod: DetectionMethod?,
    val confirmedByUser: Boolean
)
```

#### `DayStatus` (enum)
```
OFFICE      — counted as in-office
REMOTE      — counted as remote
HOLIDAY     — public holiday, excluded from mandate
PTO         — personal time off, excluded from mandate
WEEKEND     — excluded from mandate
UNKNOWN     — no data yet (default for past workdays)
```

#### `MandateConfig`
```kotlin
data class MandateConfig(
    val targetPercentage: Float,       // e.g. 0.5f for 50%
    val period: MandatePeriod,
    val workingDays: Set<DayOfWeek>    // default: MON-FRI
)
```

#### `DetectionConfig`
```kotlin
data class DetectionConfig(
    val enabledMethods: Set<DetectionMethod>,
    val wifiSsid: String?,             // for WIFI_CONNECTED and WIFI_SCAN
    val geofenceLatitude: Double?,
    val geofenceLongitude: Double?,
    val geofenceRadiusMeters: Float?
)
```

#### `ComplianceResult`
```kotlin
data class ComplianceResult(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalWorkingDays: Int,         // excludes holidays and PTO
    val officeDays: Int,
    val remoteDays: Int,
    val unknownDays: Int,
    val targetPercentage: Float,
    val currentPercentage: Float,
    val daysNeededToComply: Int,       // 0 if already compliant
    val isCompliant: Boolean
)
```

### 5.2 Repository Interfaces

```kotlin
interface DayRecordRepository {
    fun getDayRecords(start: LocalDate, end: LocalDate): Flow<List<DayRecord>>
    fun getDayRecord(date: LocalDate): Flow<DayRecord?>
    suspend fun upsertDayRecord(record: DayRecord)
    suspend fun deleteDayRecord(date: LocalDate)
}

interface HolidayRepository {
    fun getHolidays(start: LocalDate, end: LocalDate): Flow<List<Holiday>>
    suspend fun upsertHoliday(holiday: Holiday)
    suspend fun syncFromCalendar(start: LocalDate, end: LocalDate)
    suspend fun clearAndReplace(holidays: List<Holiday>)
}

interface MandateConfigRepository {
    fun getMandateConfig(): Flow<MandateConfig>
    suspend fun saveMandateConfig(config: MandateConfig)
    fun getDetectionConfig(): Flow<DetectionConfig>
    suspend fun saveDetectionConfig(config: DetectionConfig)
}
```

### 5.3 Use Cases

Each use case is a class with a single `invoke` operator.

#### `GetComplianceUseCase`
- Input: `MandatePeriod` (or use current period from config)
- Output: `Flow<ComplianceResult>`
- Logic: fetches day records + holidays for period, computes compliance

#### `GetCalendarMonthUseCase`
- Input: `YearMonth`
- Output: `Flow<List<DayRecord>>` (all days including weekends/holidays for display)

#### `RecordOfficeDayUseCase`
- Input: `LocalDate`, `DetectionMethod`
- Output: `Unit`
- Logic: upserts a `DayRecord` with `DayStatus.OFFICE`

#### `RecordRemoteDayUseCase`
- Input: `LocalDate`
- Output: `Unit`

#### `SyncCalendarUseCase`
- Input: none (reads from system calendar via `CalendarDataSource`)
- Output: `Result<Int>` (number of events synced)
- Logic: reads PTO/holiday events from device calendar, stores in `HolidayRepository`

#### `GetWorkingDaysUseCase`
- Input: `LocalDate` start, `LocalDate` end
- Output: `List<LocalDate>` (excludes weekends, holidays, PTO)

---

## 6. Detection Subsystem

### 6.1 Detector Interface

```kotlin
interface Detector {
    suspend fun isAtOffice(): Boolean
    fun isAvailable(context: Context): Boolean
}
```

### 6.2 WifiConnectedDetector
- Requires `ACCESS_FINE_LOCATION` (Android 9+)
- Returns true if connected SSID matches configured `wifiSsid`
- **API version switch for SSID retrieval:**
  - API >= 31: `ConnectivityManager.getNetworkCapabilities(activeNetwork)?.transportInfo as? WifiInfo`
  - API < 31: `WifiManager.connectionInfo.ssid` (deprecated; suppress with `@Suppress("DEPRECATION")` on that branch only)

### 6.3 WifiScanDetector
- Uses `WifiManager.startScan()` + `getScanResults()`
- Returns true if configured `wifiSsid` appears in scan results
- Note: throttled on Android 8+ (4 scans/2 min foreground, 1/30 min background)

### 6.4 GeofenceDetector
- Sets up a `Geofence` using `GeofencingClient`
- Listens for `GEOFENCE_TRANSITION_ENTER` / `GEOFENCE_TRANSITION_EXIT`
- State persisted via DataStore (isInsideGeofence: Boolean)
- Returns current state from DataStore

### 6.5 ManualDetector
- No-op detector; always returns false
- Manual check-in is handled directly via `RecordOfficeDayUseCase`

### 6.6 DetectionOrchestrator
- Injected with all enabled detectors (from `DetectionConfig`)
- Logic: returns true if **any** enabled detector returns true
- Called by `DayDetectionWorker`

### 6.7 DayDetectionWorker
- Periodic `CoroutineWorker` via WorkManager
- Runs every 2 hours between 07:00–19:00 on weekdays
- On detection: calls `RecordOfficeDayUseCase` if any detector returns true
- Does not overwrite a user-confirmed record

---

## 7. Calendar Integration

### 7.1 CalendarDataSource
- Reads from Android `CalendarContract.Events` ContentProvider
- Queries events where: title contains "PTO", "Vacation", "Holiday", "Time Off", "Day Off" (case-insensitive)
- Also reads events from calendar named "Holidays in [Country]" (Google Calendar holiday calendars)
- Returns `List<Holiday>`

### 7.2 Permission Required
- `READ_CALENDAR` permission

### 7.3 No Workday API (MVP)
Workday direct integration is deferred. The device calendar sync covers the majority of cases since most Workday deployments sync PTO to Google Calendar / Outlook.

---

## 8. Preferences (DataStore)

Stored in a single `PreferencesDataSource` using DataStore Preferences:

| Key | Type | Description |
|---|---|---|
| `mandate_percentage` | Float | e.g. 0.5 |
| `mandate_period` | String | MandatePeriod enum name |
| `working_days` | String | JSON array of DayOfWeek names |
| `detection_methods` | String | JSON array of DetectionMethod names |
| `wifi_ssid` | String | configured SSID |
| `geofence_lat` | Float | |
| `geofence_lng` | Float | |
| `geofence_radius` | Float | meters |
| `onboarding_complete` | Boolean | |
| `calendar_sync_enabled` | Boolean | |
| `geofence_inside` | Boolean | current geofence state |

---

## 9. Navigation

Single `NavHost` in `MainActivity`. Routes defined as sealed class or string constants:

```
ONBOARDING      /onboarding
DASHBOARD       /dashboard
CALENDAR        /calendar
SETTINGS        /settings
```

- App starts at `ONBOARDING` if `onboarding_complete == false`, otherwise `DASHBOARD`
- Bottom navigation bar visible on: DASHBOARD, CALENDAR, SETTINGS

---

## 10. UI/UX Conventions

### Visual source of truth — `prototype/index.html`

The canonical visual specification for every screen, layout, copy string, and interaction is the file `prototype/index.html` in the repository root. It is a single-page interactive HTML/CSS prototype covering all four app screens (onboarding, dashboard, calendar, settings) plus all bottom sheets and pickers. Open it in a browser to see exact spacing, typography weights, label wording, button states, and transitions.

**Implementing agents for any UI task (TASK-011 through TASK-020) must open `prototype/index.html` and match its visual structure as closely as Compose allows.** When the prototype and this architecture document conflict on visual detail, the prototype wins. When they conflict on architectural shape (package structure, ViewModel responsibilities, navigation graph), this document wins.

The prototype is **not** a behavioral spec — it doesn't define ViewModel logic, repository wiring, or persistence semantics. Those remain governed by the rest of this document. The prototype defines *what the user sees*; the architecture defines *what the app does*.

### Design system

- **Design system:** Material 3 (Material You)
- **Theme:** Dynamic color where available, fallback to brand colors (Blue 600 primary)
- **Color coding for days:**
  - `OFFICE` → Green
  - `REMOTE` → Blue
  - `HOLIDAY` / `PTO` → Grey
  - `WEEKEND` → Light Grey
  - `UNKNOWN` → Amber/Yellow
- **Compliance indicator:** Circular progress ring on Dashboard. Thresholds are relative to the user's configured `targetPercentage` (T):
  - `currentPercentage ≥ T` → Green
  - `currentPercentage ≥ T − 10pp` and `< T` → Amber
  - `currentPercentage < T − 10pp` → Red
- **Working days:** User-configurable in onboarding (Mandate step) and Settings → Mandate. Displayed as a row of toggleable day chips (M T W T F S S). Default: Mon–Fri. Saturday and Sunday can be enabled for non-standard schedules but are never included unless explicitly selected.
- All screens use `Scaffold` + top app bar
- State management: `UiState` sealed class per ViewModel (`Loading`, `Success`, `Error`)

---

## 11. Testing Strategy

The testing approach is layered for fast feedback in the per-task development loop, with on-device validation reserved for the release smoke suite.

### Unit Tests (JVM, no emulator)
- Location: `src/test/`
- Framework: JUnit 5 + MockK, Turbine for Flow assertions
- Runs under `./gradlew testDebugUnitTest`
- Required for: all use cases, all repositories, all detectors, all ViewModels
- **Room DAO tests** also live here, running on the JVM under **Robolectric** with `Room.inMemoryDatabaseBuilder` and `ApplicationProvider.getApplicationContext()`. Robolectric uses JUnit 4, run under the JUnit 5 Platform via `junit-vintage-engine`.

### Compose UI Tests (JVM where possible, instrumented when required)
- Compose snapshot/interaction tests that don't depend on real system services should use Compose UI Test under `src/test/` with Robolectric where feasible.
- Tests that genuinely need a device (real input dispatch, accessibility) live in `src/androidTest/` but are exercised only by the release smoke suite — not per-task QA.

### Release Smoke Suite (instrumented, emulator-backed)
- Location: `src/androidTest/kotlin/com/carvalhorr/daysInOffice/smoke/`
- Framework: `androidx.test.runner.AndroidJUnitRunner` + `AndroidJUnit4`
- Runs under `experiment/scripts/with_emulator.sh ./gradlew connectedAndroidTest -P…package=com.carvalhorr.daysInOffice.smoke`
- Covers the system-service-dependent happy paths: real on-device SQLite, manual office-day recording, compliance calculation, WorkManager scheduling.
- Defined in TASKS.md as TASK-021. This is the release gate.

### Test Naming Convention
```
fun `given [state] when [action] then [expected result]`()
```

### Mocking Strategy
- Repository interfaces are mocked with MockK in ViewModel tests
- Room DAOs are tested with a real (in-memory) Room database, never mocked
- Android system services (WifiManager, LocationManager) mocked with MockK in unit tests; exercised for real in the smoke suite

---

## 12. Dependency Injection (Hilt)

Modules to create:

| Module | Provides |
|---|---|
| `DatabaseModule` | `AppDatabase`, DAOs |
| `RepositoryModule` | Repository implementations |
| `DetectorModule` | Detector instances |
| `UseCaseModule` | Use cases (if needed) |
| `DataSourceModule` | CalendarDataSource, PreferencesDataSource |

All modules in `com.carvalhorr.daysInOffice.core.di` package.

---

## 13. Permissions

Declared in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

Permissions requested at runtime:
- `ACCESS_FINE_LOCATION` — requested during onboarding if WIFI or GEOFENCE detection is selected
- `ACCESS_BACKGROUND_LOCATION` — requested separately after fine location (Android 11+)
- `READ_CALENDAR` — requested during onboarding if calendar sync is enabled
- `POST_NOTIFICATIONS` — requested during onboarding (Android 13+)

---

## 14. Widget

`CheckInWidget` is a Glance widget (Jetpack Glance):
- Displays current compliance percentage
- Shows "Check In" button that triggers `RecordOfficeDayUseCase` for today
- Updates on: check-in, WorkManager job completion

---

## 15. Key Invariants

1. **Never overwrite a user-confirmed record** (`confirmedByUser = true`) with an automated detection result.
2. **Weekends are always excluded** from mandate calculations regardless of config.
3. **Today's record** can be in `UNKNOWN` state until detection runs or user acts.
4. **One record per day** — `DayRecordEntity.date` is the primary key.
5. **ComplianceResult.daysNeededToComply** must never be negative — floor at 0.
6. **Detection runs only on configured working days** (Mon–Fri default).
