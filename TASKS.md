# Days in Office — Implementation Tasks

> **Agent Instructions:**
> 1. Read `ARCHITECTURE.md` fully before starting any task.
> 2. Tasks must be completed in order within each phase. Cross-phase dependencies are noted per task.
> 3. After completing a task, update its **Status** to `DONE` and commit.
> 4. The QA agent must verify all acceptance criteria before a task is marked `DONE`.
> 5. Do not skip acceptance criteria — each criterion maps to a specific test or verifiable behavior.
> 6. If a task reveals a design issue, update `ARCHITECTURE.md` before proceeding.

---

## Task Status Legend
- `NOT_STARTED` — not yet begun
- `IN_PROGRESS` — currently being implemented
- `IN_REVIEW` — implementation done, QA verification pending
- `DONE` — all acceptance criteria verified

---

## Phase 1: Project Foundation

---

### TASK-001: Android Project Setup
**Status:** NOT_STARTED
**Dependencies:** none
**Complexity:** Medium

#### Context
Creates the Android project skeleton with all dependencies configured. Every subsequent task builds on this foundation.

#### Scope — Files to Create
- `build.gradle.kts` (project-level)
- `app/build.gradle.kts`
- `gradle/libs.versions.toml` (version catalog)
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/DaysInOfficeApp.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/MainActivity.kt`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values/colors.xml`

#### Implementation Details
1. Use `com.carvalhorr.daysInOffice` as the application ID.
2. Set `minSdk = 28`, `targetSdk = 35`, `compileSdk = 35`.
3. Configure `libs.versions.toml` with all versions from `ARCHITECTURE.md` Section 2.
4. Enable Kotlin 2.0 compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`).
5. Enable `kotlinOptions { jvmTarget = "17" }`.
6. Add all dependencies from ARCHITECTURE.md Section 2.
7. Add all permissions from ARCHITECTURE.md Section 13 to `AndroidManifest.xml`.
8. `DaysInOfficeApp` must be annotated `@HiltAndroidApp`.
9. `MainActivity` must be annotated `@AndroidEntryPoint` and set `enableEdgeToEdge()`.
10. Theme: Material 3, dynamic color enabled, fallback primary color `#1565C0` (Blue 700).

#### Acceptance Criteria
- [ ] `./gradlew assembleDebug` completes with no errors.
- [ ] `./gradlew testDebugUnitTest` runs with 0 tests (no tests yet) and exits successfully.
- [ ] `DaysInOfficeApp` extends `Application`, is annotated `@HiltAndroidApp`.
- [ ] `MainActivity` is annotated `@AndroidEntryPoint`.
- [ ] All permissions listed in ARCHITECTURE.md Section 13 are present in `AndroidManifest.xml`.
- [ ] Version catalog `libs.versions.toml` contains entries for all libraries in ARCHITECTURE.md Section 2.

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
grep -r "HiltAndroidApp" app/src/main/kotlin/
grep -r "AndroidEntryPoint" app/src/main/kotlin/
grep "ACCESS_FINE_LOCATION" app/src/main/AndroidManifest.xml
grep "READ_CALENDAR" app/src/main/AndroidManifest.xml
```

---

### TASK-002: Core Domain Models
**Status:** NOT_STARTED
**Dependencies:** TASK-001
**Complexity:** Simple

#### Context
Defines all pure Kotlin data classes and enums that form the domain model. No Android dependencies. These are the contracts everything else is built against.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/model/DayStatus.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/model/MandatePeriod.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/model/DetectionMethod.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/model/DayRecord.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/model/MandateConfig.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/model/DetectionConfig.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/model/ComplianceResult.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/model/Holiday.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/domain/model/ComplianceResultTest.kt`

#### Implementation Details
Implement exactly as specified in ARCHITECTURE.md Sections 5.1. Additional detail:

`Holiday`:
```kotlin
data class Holiday(
    val date: LocalDate,
    val name: String,
    val isPublicHoliday: Boolean,
    val source: String
)
```

`ComplianceResult` — add a computed property:
```kotlin
val projectedPercentage: Float  // if remaining unknown days = office days, what % would be reached
```
Computed as: `(officeDays + unknownDays).toFloat() / totalWorkingDays.coerceAtLeast(1)`

#### Acceptance Criteria
- [ ] All model classes compile with no warnings.
- [ ] `DayStatus` has exactly 6 values: `OFFICE, REMOTE, HOLIDAY, PTO, WEEKEND, UNKNOWN`.
- [ ] `MandatePeriod` has exactly 4 values: `WEEKLY, MONTHLY, QUARTERLY, ROLLING_4_WEEKS`.
- [ ] `DetectionMethod` has exactly 4 values: `WIFI_CONNECTED, WIFI_SCAN, GEOFENCE, MANUAL`.
- [ ] `ComplianceResult.daysNeededToComply` is computed (not stored): `maxOf(0, ceil(targetPercentage * totalWorkingDays).toInt() - officeDays)`.
- [ ] `ComplianceResult.isCompliant` is computed: `currentPercentage >= targetPercentage`.
- [ ] `ComplianceResult.currentPercentage` is computed: `officeDays.toFloat() / totalWorkingDays.coerceAtLeast(1)`.
- [ ] Unit tests in `ComplianceResultTest` pass:
  - `given 10 working days and 5 office days when targetPercentage=0.5 then isCompliant=true and daysNeededToComply=0`
  - `given 10 working days and 4 office days when targetPercentage=0.5 then isCompliant=false and daysNeededToComply=1`
  - `given 0 working days when any configuration then daysNeededToComply=0 and currentPercentage=0`

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.domain.model.*"
```

---

### TASK-003: Room Database Setup
**Status:** NOT_STARTED
**Dependencies:** TASK-002
**Complexity:** Medium

#### Context
Sets up the local database. All app data is persisted here. The schema must match ARCHITECTURE.md exactly — changing it later requires migrations.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/db/AppDatabase.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/db/entity/DayRecordEntity.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/db/entity/HolidayEntity.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/db/converter/TypeConverters.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/db/dao/DayRecordDao.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/db/dao/HolidayDao.kt`
- `app/src/androidTest/kotlin/com/carvalhorr/daysInOffice/core/data/db/DayRecordDaoTest.kt`
- `app/src/androidTest/kotlin/com/carvalhorr/daysInOffice/core/data/db/HolidayDaoTest.kt`

#### Implementation Details
1. Entities: match schema in ARCHITECTURE.md Section 4.1 exactly.
2. `TypeConverters`: `LocalDate` ↔ `String` (ISO-8601), `Instant` ↔ `Long` (epoch ms), enums ↔ `String`.
3. `DayRecordDao` methods (all as per ARCHITECTURE.md Section 4.2):
   - `getByDateRange(start, end): Flow<List<DayRecordEntity>>` — use `WHERE date >= :start AND date <= :end ORDER BY date ASC`
   - `getByDate(date): Flow<DayRecordEntity?>` — single record
   - `upsert(record)` — use `@Upsert`
   - `delete(date)` — deletes by primary key
4. `HolidayDao` methods match ARCHITECTURE.md Section 4.2.
5. `AppDatabase`: version 1, `exportSchema = true`, schema exported to `app/schemas/`.
6. Dao tests use **in-memory Room database** (`Room.inMemoryDatabaseBuilder`). No mocking of Room.

#### Acceptance Criteria
- [ ] `./gradlew assembleDebug` succeeds (schema exported to `app/schemas/`).
- [ ] `DayRecordDaoTest` passes all tests:
  - `given empty db when upsert then getByDate returns record`
  - `given existing record when upsert with same date then record is updated`
  - `given multiple records when getByDateRange then only records in range returned`
  - `given record when delete then getByDate returns null`
- [ ] `HolidayDaoTest` passes all tests:
  - `given holidays when getByDateRange then correct holidays returned`
  - `given holidays when deleteAll then db is empty`
  - `given holidays when deleteByDateRange then only records outside range remain`
- [ ] `TypeConverters` correctly round-trips `LocalDate.of(2024, 1, 15)` through String.
- [ ] `TypeConverters` correctly round-trips `Instant.now()` through Long (within 1ms).

#### QA Verification Steps
```bash
./gradlew connectedAndroidTest --tests "com.carvalhorr.daysInOffice.core.data.db.*"
```

---

### TASK-004: Repository Interfaces and Implementations
**Status:** NOT_STARTED
**Dependencies:** TASK-003
**Complexity:** Medium

#### Context
Implements the data layer repositories that bridge the domain layer to Room and DataStore. ViewModels and use cases depend only on the interfaces, never the implementations.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/repository/DayRecordRepository.kt` (interface)
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/repository/HolidayRepository.kt` (interface)
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/repository/MandateConfigRepository.kt` (interface)
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/datasource/PreferencesDataSource.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/repository/DayRecordRepositoryImpl.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/repository/HolidayRepositoryImpl.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/repository/MandateConfigRepositoryImpl.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/di/DatabaseModule.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/di/RepositoryModule.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/data/repository/DayRecordRepositoryImplTest.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/data/repository/MandateConfigRepositoryImplTest.kt`

#### Implementation Details
1. Repository interfaces: match ARCHITECTURE.md Section 5.2 exactly.
2. `DayRecordRepositoryImpl`: maps `DayRecordEntity` ↔ `DayRecord` (domain model). Injected with `DayRecordDao`.
3. `HolidayRepositoryImpl`: maps `HolidayEntity` ↔ `Holiday`. Injected with `HolidayDao`.
4. `MandateConfigRepositoryImpl`: reads/writes all settings from `PreferencesDataSource`.
5. `PreferencesDataSource`: wraps DataStore Preferences. Keys match ARCHITECTURE.md Section 8. Uses `kotlinx.serialization` for JSON serialization of `Set<DayOfWeek>` and `Set<DetectionMethod>`.
6. Default `MandateConfig`: `targetPercentage=0.5f`, `period=MONTHLY`, `workingDays=MON-FRI`.
7. Default `DetectionConfig`: `enabledMethods=emptySet()`, all nullable fields null.
8. `DatabaseModule`: `@InstallIn(SingletonComponent::class)`, provides `AppDatabase` (singleton) and DAOs.
9. `RepositoryModule`: `@InstallIn(SingletonComponent::class)`, binds implementations to interfaces.
10. Unit tests use MockK to mock DAOs and DataStore.

#### Acceptance Criteria
- [ ] `./gradlew assembleDebug` succeeds.
- [ ] `DayRecordRepositoryImplTest` passes:
  - `given dao emits entities when getDayRecords then flow emits mapped domain models`
  - `given domain model when upsertDayRecord then dao upsert called with mapped entity`
  - `given dao emits null when getDayRecord then flow emits null`
- [ ] `MandateConfigRepositoryImplTest` passes:
  - `given no saved config when getMandateConfig then returns default config`
  - `given saved config when getMandateConfig then returns saved config`
  - `given config when saveMandateConfig then preferences updated`
- [ ] All Hilt modules compile — `./gradlew kaptDebugKotlin` passes.
- [ ] No direct DAO or DataStore references exist outside the `data` package.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.data.repository.*"
./gradlew kaptDebugKotlin
```

---

## Phase 2: Core Business Logic

---

### TASK-005: Working Days and Compliance Use Cases
**Status:** NOT_STARTED
**Dependencies:** TASK-004
**Complexity:** Complex

#### Context
The mandate calculation is the heart of the app. This task implements the use cases that determine which days count as working days and computes the compliance percentage. Gets everything right or the whole app is wrong.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/GetWorkingDaysUseCase.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/GetComplianceUseCase.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/RecordOfficeDayUseCase.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/RecordRemoteDayUseCase.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/GetCalendarMonthUseCase.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/GetWorkingDaysUseCaseTest.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/GetComplianceUseCaseTest.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/RecordOfficeDayUseCaseTest.kt`

#### Implementation Details

**`GetWorkingDaysUseCase`**
- Input: `start: LocalDate`, `end: LocalDate`
- Output: `List<LocalDate>`
- Logic:
  1. Generate all dates in range.
  2. Remove weekends (Saturday, Sunday always excluded).
  3. Fetch holidays in range from `HolidayRepository`.
  4. Remove holiday dates (both `isPublicHoliday=true` and `false`/PTO).
- Note: uses `suspend` + collects from Flow once (use `first()`).

**`GetComplianceUseCase`**
- Input: none (reads from `MandateConfigRepository`)
- Output: `Flow<ComplianceResult>`
- Logic:
  1. Combine `MandateConfig` flow + current date to determine period start/end.
  2. For `WEEKLY`: current ISO week (Mon–Sun).
  3. For `MONTHLY`: current calendar month.
  4. For `QUARTERLY`: current calendar quarter (Q1 = Jan–Mar, Q2 = Apr–Jun, Q3 = Jul–Sep, Q4 = Oct–Dec).
  5. For `ROLLING_4_WEEKS`: last 28 days from today.
  5. Call `GetWorkingDaysUseCase` for period.
  6. Fetch `DayRecord` list for period.
  7. Count `OFFICE`, `REMOTE`, `UNKNOWN` from records; days with no record = `UNKNOWN`.
  8. Build and emit `ComplianceResult`.
- Must use `combine()` to react to both config changes and record changes.

**`RecordOfficeDayUseCase`**
- Input: `date: LocalDate`, `method: DetectionMethod`
- Logic: fetch existing record; if `confirmedByUser == true`, do nothing (invariant from ARCHITECTURE.md Section 15, rule 1). Otherwise upsert with `DayStatus.OFFICE`.

**`RecordRemoteDayUseCase`**
- Input: `date: LocalDate`
- Logic: same guard as above; upsert with `DayStatus.REMOTE` and `confirmedByUser = true`.

**`GetCalendarMonthUseCase`**
- Input: `YearMonth`
- Output: `Flow<List<DayRecord>>`
- Logic: returns all calendar days in month (including weekends) as `DayRecord` objects. Days with no DB record: weekends → `WEEKEND`, holidays → `HOLIDAY`/`PTO`, rest → `UNKNOWN`.

#### Acceptance Criteria
- [ ] `GetWorkingDaysUseCaseTest` passes:
  - `given a week with no holidays when invoked then 5 working days returned`
  - `given a week with one public holiday on Wednesday when invoked then 4 working days returned`
  - `given a range spanning a weekend when invoked then Saturday and Sunday excluded`
  - `given a week with one PTO day when invoked then that day excluded`
- [ ] `GetComplianceUseCaseTest` passes:
  - `given monthly period and 10 working days and 5 office records when invoked then currentPercentage=0.5 and isCompliant=true`
  - `given monthly period and 10 working days and 4 office records when invoked then daysNeededToComply=1`
  - `given quarterly period when invoked then period spans correct calendar quarter`
  - `given rolling 4 weeks when invoked then period spans last 28 days from today`
  - `given a week with a holiday when invoked then holiday excluded from totalWorkingDays`
  - `given day with confirmedByUser=true when RecordOfficeDayUseCase called then record unchanged`
- [ ] `RecordOfficeDayUseCaseTest` passes:
  - `given no existing record when invoked then OFFICE record created`
  - `given confirmed record when invoked then record NOT updated`
  - `given unconfirmed REMOTE record when invoked then record updated to OFFICE`
- [ ] `./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.domain.usecase.*"` — all pass.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.domain.usecase.*"
```

---

### TASK-006: Calendar Data Source and SyncCalendarUseCase
**Status:** NOT_STARTED
**Dependencies:** TASK-004
**Complexity:** Medium

#### Context
Reads PTO and public holidays from the device calendar so they are excluded from the mandate calculation. This is the primary mechanism to avoid penalizing users for time off.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/datasource/CalendarDataSource.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/SyncCalendarUseCase.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/di/DataSourceModule.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/data/datasource/CalendarDataSourceTest.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/SyncCalendarUseCaseTest.kt`

#### Implementation Details

**`CalendarDataSource`**
- Constructor-injected with `ContentResolver` (use `@ApplicationContext context: Context`).
- Method: `suspend fun readHolidays(start: LocalDate, end: LocalDate): List<Holiday>`
- Query `CalendarContract.Events`:
  - `dtstart` and `dtend` overlap the range.
  - Match events where `title` (case-insensitive) contains any of: `"pto"`, `"vacation"`, `"holiday"`, `"time off"`, `"day off"`, `"annual leave"`.
  - OR: events from a calendar named matching `"Holiday"` (catches Google's "Holidays in [Country]" calendars).
- Events from holiday calendars → `isPublicHoliday = true`.
- Events matching title keywords → `isPublicHoliday = false` (PTO).
- Handle multi-day events: expand into individual `Holiday` objects per day.
- Source string: `"CALENDAR"`.
- Return empty list if `READ_CALENDAR` permission not granted.

**`SyncCalendarUseCase`**
- Input: `start: LocalDate`, `end: LocalDate` (default: current year start to end)
- Output: `Result<Int>` (count of holidays synced)
- Logic: reads from `CalendarDataSource`, calls `HolidayRepository.clearAndReplace()`.

#### Acceptance Criteria
- [ ] `CalendarDataSourceTest` passes:
  - `given ContentResolver returns PTO event when readHolidays then Holiday with isPublicHoliday=false returned`
  - `given ContentResolver returns event from holiday calendar when readHolidays then Holiday with isPublicHoliday=true returned`
  - `given READ_CALENDAR permission not granted when readHolidays then empty list returned`
  - `given multi-day PTO event spanning 3 days when readHolidays then 3 Holiday objects returned`
- [ ] `SyncCalendarUseCaseTest` passes:
  - `given CalendarDataSource returns 5 holidays when invoke then repository clearAndReplace called with 5 holidays`
  - `given CalendarDataSource throws exception when invoke then Result.failure returned`
- [ ] `CalendarDataSource` has no real ContentResolver calls in unit tests (mocked with MockK).

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.data.datasource.*"
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.domain.usecase.SyncCalendarUseCaseTest"
```

---

## Phase 3: Detection

---

### TASK-007: Wi-Fi Connected Detector
**Status:** NOT_STARTED
**Dependencies:** TASK-005
**Complexity:** Simple

#### Context
Detects office presence by checking if the device is currently connected to the configured office Wi-Fi SSID. The simplest and most reliable detection method.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/detection/Detector.kt` (interface)
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/detection/detector/WifiConnectedDetector.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/detection/WifiConnectedDetectorTest.kt`

#### Implementation Details
`Detector` interface:
```kotlin
interface Detector {
    suspend fun isAtOffice(): Boolean
    fun isAvailable(context: Context): Boolean
}
```

`WifiConnectedDetector`:
- Constructor: `context: Context`, `config: DetectionConfig`
- `isAtOffice()`:
  1. If `config.wifiSsid` is null or blank, return false.
  2. Retrieve the connected SSID using a version switch:
     - **API >= 31:** use `ConnectivityManager`:
       ```kotlin
       val cm = context.getSystemService(ConnectivityManager::class.java)
       val ssid = (cm.getNetworkCapabilities(cm.activeNetwork)?.transportInfo as? WifiInfo)?.ssid
       ```
     - **API < 31:** use `WifiManager` (deprecated but functional):
       ```kotlin
       val ssid = context.getSystemService(WifiManager::class.java).connectionInfo.ssid
       ```
  3. Strip surrounding quotes from SSID (`"\"OfficeName\""` → `"OfficeName"`).
  4. Compare to `config.wifiSsid` (case-insensitive).
- `isAvailable()`: returns `context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)`.

#### Acceptance Criteria
- [ ] `WifiConnectedDetectorTest` passes:
  - `given connected SSID matches config when isAtOffice then returns true`
  - `given connected SSID does not match config when isAtOffice then returns false`
  - `given SSID wrapped in quotes when isAtOffice then quotes stripped before comparison`
  - `given config has null SSID when isAtOffice then returns false`
  - `given config has blank SSID when isAtOffice then returns false`
  - `given API level >= 31 when isAtOffice then ConnectivityManager path used`
  - `given API level < 31 when isAtOffice then WifiManager.connectionInfo path used`
- [ ] `WifiManager` and `ConnectivityManager` are mocked — no real Wi-Fi access in tests.
- [ ] No deprecation suppression annotations on the class level — suppress only on the `< 31` branch with `@Suppress("DEPRECATION")`.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.detection.WifiConnectedDetectorTest"
```

---

### TASK-008: Wi-Fi Scan Detector
**Status:** NOT_STARTED
**Dependencies:** TASK-007
**Complexity:** Simple

#### Context
Detects office presence by scanning for the office Wi-Fi SSID without requiring connection. Useful for users who use mobile data at the office.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/detection/detector/WifiScanDetector.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/detection/WifiScanDetectorTest.kt`

#### Implementation Details
`WifiScanDetector`:
- Constructor: `context: Context`, `config: DetectionConfig`
- `isAtOffice()`:
  1. If `config.wifiSsid` is null or blank, return false.
  2. Get `WifiManager`.
  3. Call `startScan()`.
  4. Get `scanResults` — list of `ScanResult`.
  5. Return true if any result's `SSID` matches `config.wifiSsid` (case-insensitive, strip quotes).
- `isAvailable()`: same as `WifiConnectedDetector`.
- Note: Throttling is handled at the orchestrator level (this detector just attempts the scan). If `startScan()` returns false (throttled), fall through to current `scanResults` (may be stale — acceptable).

#### Acceptance Criteria
- [ ] `WifiScanDetectorTest` passes:
  - `given scan results contain office SSID when isAtOffice then returns true`
  - `given scan results do not contain office SSID when isAtOffice then returns false`
  - `given startScan returns false when isAtOffice then falls back to existing scan results`
  - `given config has null SSID when isAtOffice then returns false`
  - `given empty scan results when isAtOffice then returns false`

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.detection.WifiScanDetectorTest"
```

---

### TASK-009: Geofence Detector
**Status:** NOT_STARTED
**Dependencies:** TASK-007
**Complexity:** Complex

#### Context
Detects office presence based on GPS proximity to the configured office location. Covers users who don't connect to office Wi-Fi.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/detection/detector/GeofenceDetector.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/detection/receiver/GeofenceBroadcastReceiver.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/detection/GeofenceDetectorTest.kt`

#### Implementation Details
`GeofenceDetector`:
- Constructor: `context: Context`, `config: DetectionConfig`, `preferencesDataSource: PreferencesDataSource`
- `isAtOffice()`: reads `geofence_inside` Boolean from `PreferencesDataSource`. Returns its value.
- `isAvailable()`: returns true if `geofenceLatitude`, `geofenceLongitude`, `geofenceRadiusMeters` are all non-null.
- `fun setupGeofence()`:
  1. Build a `Geofence` with id `"OFFICE_GEOFENCE"`, lat/lng/radius from config.
  2. Transitions: `GEOFENCE_TRANSITION_ENTER | GEOFENCE_TRANSITION_EXIT`.
  3. Expiration: `Geofence.NEVER_EXPIRE`.
  4. Register with `GeofencingClient`.
  5. Pending intent targets `GeofenceBroadcastReceiver`.

`GeofenceBroadcastReceiver`:
- Extends `BroadcastReceiver`.
- On `GEOFENCE_TRANSITION_ENTER`: writes `geofence_inside = true` to DataStore.
- On `GEOFENCE_TRANSITION_EXIT`: writes `geofence_inside = false` to DataStore.
- Register in `AndroidManifest.xml`.

#### Acceptance Criteria
- [ ] `GeofenceDetectorTest` passes:
  - `given geofence_inside=true in DataStore when isAtOffice then returns true`
  - `given geofence_inside=false in DataStore when isAtOffice then returns false`
  - `given geofence_inside not set in DataStore when isAtOffice then returns false`
  - `given config missing lat/lng when isAvailable then returns false`
  - `given config with lat/lng/radius when isAvailable then returns true`
- [ ] `GeofenceBroadcastReceiver` registered in `AndroidManifest.xml`.
- [ ] No actual `GeofencingClient` calls in unit tests (mocked).

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.detection.GeofenceDetectorTest"
grep "GeofenceBroadcastReceiver" app/src/main/AndroidManifest.xml
```

---

### TASK-010: Detection Orchestrator and WorkManager Worker
**Status:** NOT_STARTED
**Dependencies:** TASK-007, TASK-008, TASK-009
**Complexity:** Complex

#### Context
The orchestrator coordinates all detection methods. The WorkManager worker schedules automatic detection throughout the workday. This is the glue that makes the app automatic.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/detection/DetectionOrchestrator.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/detection/worker/DayDetectionWorker.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/di/DetectorModule.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/detection/DetectionOrchestratorTest.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/detection/DayDetectionWorkerTest.kt`

#### Implementation Details
`DetectionOrchestrator`:
- Constructor-injected: `detectors: Map<DetectionMethod, Detector>`, `configRepository: MandateConfigRepository`, `recordOfficeDayUseCase: RecordOfficeDayUseCase`
- `suspend fun runDetection(date: LocalDate)`:
  1. Fetch `DetectionConfig` (use `first()`).
  2. For each enabled method in `config.enabledMethods`, get the corresponding detector.
  3. Call `isAvailable()` and `isAtOffice()` on each.
  4. If **any** returns true → call `RecordOfficeDayUseCase(date, method)`.
  5. Detection stops after first positive result (short-circuit).

`DayDetectionWorker`:
- `CoroutineWorker` with `@HiltWorker` and `@AssistedInject`.
- Injected: `DetectionOrchestrator`.
- `doWork()`: calls `orchestrator.runDetection(LocalDate.now())`, returns `Result.success()`.
- Scheduling: in `DaysInOfficeApp.onCreate()`, schedule a `PeriodicWorkRequest`:
  - Repeat interval: 2 hours.
  - Constraints: no network required.
  - Use `ExistingPeriodicWorkPolicy.KEEP` so reboots don't create duplicates.
  - Tag: `"day_detection"`.

`DetectorModule`:
- Provides `Map<DetectionMethod, Detector>` via Hilt multibindings.
- Binds `WifiConnectedDetector` to `DetectionMethod.WIFI_CONNECTED`.
- Binds `WifiScanDetector` to `DetectionMethod.WIFI_SCAN`.
- Binds `GeofenceDetector` to `DetectionMethod.GEOFENCE`.

#### Acceptance Criteria
- [ ] `DetectionOrchestratorTest` passes:
  - `given WIFI_CONNECTED enabled and WifiConnectedDetector returns true when runDetection then RecordOfficeDayUseCase called`
  - `given WIFI_CONNECTED enabled and detector returns false when runDetection then RecordOfficeDayUseCase NOT called`
  - `given multiple detectors enabled and first returns true when runDetection then second detector NOT called (short-circuit)`
  - `given MANUAL is the only enabled method when runDetection then no detector called and no record created`
  - `given detector isAvailable=false when runDetection then detector skipped`
- [ ] `DayDetectionWorkerTest` passes:
  - `given orchestrator runs successfully when doWork then returns Result.success`
  - `given orchestrator throws when doWork then returns Result.failure`
- [ ] WorkManager scheduled in `DaysInOfficeApp` (verify by grep).
- [ ] `./gradlew assembleDebug` succeeds.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.detection.*"
grep "DayDetectionWorker" app/src/main/kotlin/com/carvalhorr/daysInOffice/app/DaysInOfficeApp.kt
```

---

## Phase 4: Navigation and Onboarding

---

### TASK-011: Navigation Setup
**Status:** NOT_STARTED
**Dependencies:** TASK-001
**Complexity:** Simple

#### Context
Sets up the navigation graph. All screens route through this. Must be in place before any screen UI is implemented.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/navigation/AppNavigation.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/navigation/Screen.kt`

#### Implementation Details
`Screen` — sealed class with object entries and `route: String`:
```kotlin
sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")
}
```

`AppNavigation` — `@Composable` function:
- Accepts `navController: NavHostController`, `startDestination: String`.
- `NavHost` with routes for all 4 screens (composable placeholders if screen not yet implemented).
- Bottom navigation bar (`NavigationBar`) shown on Dashboard, Calendar, Settings screens.
- Bottom nav items: Dashboard (icon: `Home`), Calendar (icon: `CalendarMonth`), Settings (icon: `Settings`).

`MainActivity`:
- Determines `startDestination` based on `onboarding_complete` preference (read via `MandateConfigRepository` injected into `MainViewModel`).
- If `onboarding_complete == false` → `Screen.Onboarding.route`.
- Else → `Screen.Dashboard.route`.

Create `MainViewModel` with single state: `startDestination: String`.

#### Acceptance Criteria
- [ ] `./gradlew assembleDebug` succeeds.
- [ ] `Screen` sealed class has exactly 4 routes: `onboarding`, `dashboard`, `calendar`, `settings`.
- [ ] `AppNavigation` composable compiles and renders without crash (smoke test via `composeTestRule`).
- [ ] `MainViewModel` correctly emits `Screen.Onboarding.route` when `onboarding_complete=false`.
- [ ] `MainViewModel` correctly emits `Screen.Dashboard.route` when `onboarding_complete=true`.

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.app.*"
```

---

### TASK-012: Onboarding Flow
**Status:** NOT_STARTED
**Dependencies:** TASK-005, TASK-011
**Complexity:** Complex

#### Context
The first-run experience. Users configure their mandate, period, detection method, and calendar sync. Must be frictionless — a bad onboarding means users abandon the app. Completes by setting `onboarding_complete=true`.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/OnboardingViewModel.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/OnboardingScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/MandateSetupStep.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/PeriodSelectionStep.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/DetectionSetupStep.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/CalendarSetupStep.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/OnboardingViewModelTest.kt`

#### Implementation Details
`OnboardingViewModel`:
- State: `OnboardingUiState(currentStep: Int, mandateConfig: MandateConfig, detectionConfig: DetectionConfig, calendarSyncEnabled: Boolean)`.
- 4 steps (0-indexed): Mandate → Period → Detection → Calendar.
- `fun nextStep()`: advances step; on step 4 (after Calendar): saves all config, sets `onboarding_complete=true`, emits navigation event.
- `fun prevStep()`: goes back.
- `fun updateMandatePercentage(value: Float)`: updates state.
- `fun updateWorkingDays(days: Set<DayOfWeek>)`: updates `mandateConfig.workingDays`; must contain at least one day.
- `fun updatePeriod(period: MandatePeriod)`: updates state.
- `fun toggleDetectionMethod(method: DetectionMethod)`: toggles in set.
- `fun updateWifiSsid(ssid: String)`: updates state.
- `fun setGeofence(lat: Double, lng: Double, radius: Float)`: updates state.
- `fun setCalendarSync(enabled: Boolean)`: updates state.

**Step 1 — Mandate Setup:**
- Title: "What's your office mandate?"
- Slider for percentage (25%–100%, step 5%).
- Display: "X% of working days".
- Default: 50%.
- Working days row: label "Which days do you work?" + a row of 7 toggleable day chips (M T W T F S S). Mon–Fri selected by default. At least one day must remain selected. Sat/Sun chips are visually de-emphasised but still tappable.

**Step 2 — Period Selection:**
- Title: "How is your mandate measured?"
- 4 radio options: Weekly / Monthly / Quarterly / Rolling 4 weeks.
- Brief description under each option.

**Step 3 — Detection Setup:**
- Title: "How should we detect when you're at the office?"
- Checkboxes for each `DetectionMethod` with descriptions.
- If `WIFI_CONNECTED` or `WIFI_SCAN` selected: show text field for SSID.
- If `GEOFENCE` selected: show "Set Office Location" button (opens system location picker — for MVP, show lat/lng fields).
- `MANUAL` always available as fallback note.

**Step 4 — Calendar Setup:**
- Title: "Exclude holidays and time off"
- Toggle: "Sync from device calendar"
- If enabled: explain what will be read (PTO, holidays).
- "Connect Calendar" button triggers `SyncCalendarUseCase`.
- "Skip for now" option.

UI: use `HorizontalPager` or step-by-step with animated progress indicator at top. "Next" / "Back" buttons. "Skip" on Calendar step.

#### Acceptance Criteria
- [ ] `OnboardingViewModelTest` passes:
  - `given initial state when nextStep 4 times then onboarding_complete saved as true`
  - `given step 0 when prevStep then step stays at 0 (no underflow)`
  - `given step 3 when nextStep then navigation event emitted`
  - `given toggleDetectionMethod WIFI_CONNECTED twice then method removed from set`
  - `given updateWifiSsid called then state reflects new SSID`
  - `given updateWorkingDays called with empty set then working days unchanged (at least one required)`
  - `given updateWorkingDays called with {MON, WED, FRI} then mandateConfig.workingDays updated`
- [ ] All 4 steps render without crash (Compose UI smoke test).
- [ ] Progress indicator shows correct step.
- [ ] Working days chips on Mandate step: Mon–Fri selected by default; toggling a chip updates selection; deselecting the last chip has no effect.
- [ ] "Next" disabled on Detection step if no method selected (and MANUAL not explicitly counted as a "setup" step — it's always available).
- [ ] `./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.onboarding.*"` passes.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.onboarding.*"
./gradlew assembleDebug
```

---

## Phase 5: Core UI Screens

---

### TASK-013: Dashboard Screen
**Status:** NOT_STARTED
**Dependencies:** TASK-005, TASK-011
**Complexity:** Complex

#### Context
The main screen users see every day. Must immediately answer: "Am I on track?" Compliance ring is the hero element.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/DashboardViewModel.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/DashboardScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/ComplianceRing.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/QuickCheckInButton.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/DashboardViewModelTest.kt`

#### Implementation Details
`DashboardViewModel`:
- Collects `GetComplianceUseCase` flow.
- State: `DashboardUiState` — `Loading` | `Success(result: ComplianceResult, todayStatus: DayStatus)` | `Error(message: String)`.
- `fun checkInToday()`: calls `RecordOfficeDayUseCase(LocalDate.now(), DetectionMethod.MANUAL)`.
- `fun markRemoteToday()`: calls `RecordRemoteDayUseCase(LocalDate.now())`.

`ComplianceRing` composable:
- Circular arc progress indicator (use `Canvas`).
- Center text: percentage (e.g. "47%") in large bold font.
- Below center: "X days to reach goal" or "On track ✓".
- Color: matches ARCHITECTURE.md Section 10 (Red < 40%, Amber 40–49%, Green ≥ 50%).
- Animated: percentage change animates smoothly.

`QuickCheckInButton`:
- Large FAB-style button.
- States: "Check in to office" (if today = UNKNOWN/REMOTE), "Checked in ✓" (if today = OFFICE), "Mark as Remote" secondary button.
- If today is HOLIDAY or PTO: show message "Today is a day off — no action needed."

`DashboardScreen` layout:
1. Top app bar: "Days in Office", settings icon.
2. Period label: "This Month" / "This Week" / "Rolling 4 weeks".
3. `ComplianceRing` (large, centered).
4. Stats row: `Office: X | Remote: Y | Unknown: Z`.
5. `QuickCheckInButton`.
6. "Sync calendar" action (calls `SyncCalendarUseCase`).

#### Acceptance Criteria
- [ ] `DashboardViewModelTest` passes:
  - `given compliance use case emits result when state then DashboardUiState.Success emitted`
  - `given checkInToday called when state then todayStatus becomes OFFICE`
  - `given today is a confirmed office day when checkInToday called then record not overwritten`
- [ ] `ComplianceRing` renders with correct color for 35% (Red), 45% (Amber), 55% (Green).
- [ ] `QuickCheckInButton` shows "Checked in ✓" when `todayStatus == OFFICE`.
- [ ] `./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.dashboard.*"` passes.
- [ ] `./gradlew assembleDebug` succeeds.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.dashboard.*"
./gradlew assembleDebug
```

---

### TASK-014: Calendar Screen
**Status:** NOT_STARTED
**Dependencies:** TASK-005, TASK-011
**Complexity:** Complex

#### Context
Shows users their full history. Users can review and correct any day. This is the trust mechanism — users can see exactly how the app has been tracking them and fix mistakes.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/calendar/CalendarViewModel.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/calendar/ui/CalendarScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/calendar/ui/MonthCalendarView.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/calendar/ui/DayDetailSheet.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/feature/calendar/CalendarViewModelTest.kt`

#### Implementation Details
`CalendarViewModel`:
- State: selected `YearMonth` (default: current month) + `List<DayRecord>` for that month.
- Collects `GetCalendarMonthUseCase(selectedMonth)`.
- `fun prevMonth()` / `fun nextMonth()`: changes `selectedMonth`, re-collects.
- `fun updateDayStatus(date: LocalDate, status: DayStatus)`: calls appropriate use case based on status.

`MonthCalendarView` composable:
- 7-column grid (Sun–Sat headers).
- Each day cell: colored dot + day number.
- Colors per `DayStatus` (ARCHITECTURE.md Section 10).
- Tapping a cell opens `DayDetailSheet`.
- Days from adjacent months shown greyed out (not tappable).

`DayDetailSheet` (bottom sheet):
- Shows date, current status, detection method (if any), whether user-confirmed.
- Action buttons: "Mark as Office", "Mark as Remote", "Mark as PTO".
- "Mark as Office" calls `RecordOfficeDayUseCase(date, MANUAL)`.
- "Mark as Remote" calls `RecordRemoteDayUseCase(date)`.
- "Mark as PTO" upserts a `Holiday(date, "PTO", false, "MANUAL")` into `HolidayRepository`.

Month navigation: left/right chevrons, current month name + year centered.

#### Acceptance Criteria
- [ ] `CalendarViewModelTest` passes:
  - `given current month when initialized then correct month's records loaded`
  - `given nextMonth called when on December then advances to January next year`
  - `given prevMonth called when on January then goes back to December prior year`
  - `given updateDayStatus called with OFFICE when state then RecordOfficeDayUseCase called with MANUAL`
- [ ] `MonthCalendarView` renders 28–31 day cells depending on month.
- [ ] Each `DayStatus` maps to a distinct color in the calendar.
- [ ] `DayDetailSheet` shows all 3 action buttons for an UNKNOWN day.
- [ ] `./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.calendar.*"` passes.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.calendar.*"
./gradlew assembleDebug
```

---

### TASK-015: Settings Screen
**Status:** NOT_STARTED
**Dependencies:** TASK-004, TASK-011
**Complexity:** Medium

#### Context
Users need to reconfigure the app after onboarding — change detection method, update the office SSID if it changes, adjust mandate target.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/SettingsViewModel.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/SettingsScreen.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/feature/settings/SettingsViewModelTest.kt`

#### Implementation Details
`SettingsViewModel`:
- Loads `MandateConfig` and `DetectionConfig` from repositories.
- Exposes same update functions as `OnboardingViewModel` (consider extracting a shared `ConfigViewModel` — only if it doesn't add complexity; otherwise just duplicate).
- Additional: `fun resetOnboarding()` — sets `onboarding_complete=false`, triggers navigation to Onboarding.
- `fun updateWorkingDays(days: Set<DayOfWeek>)`: same guard as onboarding (at least one day required).
- `fun exportData()`: deferred — show "Coming soon" snackbar.

`SettingsScreen` sections:
1. **Mandate** — percentage slider, period selector, working days chip row.
2. **Detection** — checkboxes per method, SSID field (if applicable), geofence setup.
3. **Calendar** — toggle sync, "Sync Now" button, last sync timestamp.
4. **Data** — "Export CSV" (coming soon), "Reset Onboarding".
5. App version at bottom.

Working days in Settings: tapping the "Working days" row opens a bottom sheet with the same M T W T F S S chip row used in onboarding. Current selection is pre-filled from `MandateConfig`. Save updates `MandateConfig.workingDays` via `MandateConfigRepository`.

#### Acceptance Criteria
- [ ] `SettingsViewModelTest` passes:
  - `given loaded config when updateMandatePercentage then config saved`
  - `given resetOnboarding called then onboarding_complete set to false`
  - `given updateWorkingDays called with non-empty set then mandateConfig saved with new working days`
  - `given updateWorkingDays called with empty set then config unchanged`
- [ ] Settings screen renders all 4 sections without crash.
- [ ] Working days row displays current selection (e.g. "Mon – Fri" or individual chip labels).
- [ ] Changes to mandate percentage are persisted (verify via `MandateConfigRepository` mock).
- [ ] `./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.settings.*"` passes.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.settings.*"
./gradlew assembleDebug
```

---

## Phase 6: Notifications and Widget

---

### TASK-016: Daily Notification
**Status:** NOT_STARTED
**Dependencies:** TASK-010
**Complexity:** Medium

#### Context
A morning prompt helps users who rely on manual check-in remember to log their day. Without this, manual users will forget and their data will be inaccurate.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/notification/NotificationScheduler.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/notification/DailyCheckInNotificationWorker.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/notification/DailyCheckInNotificationWorkerTest.kt`

#### Implementation Details
`DailyCheckInNotificationWorker` (`CoroutineWorker`, `@HiltWorker`):
- Runs daily at 09:00 local time.
- `doWork()`:
  1. Check if today is a working day (not weekend, not holiday/PTO from repo).
  2. Check if today already has a confirmed record — if yes, skip.
  3. Post notification: title "Office check-in", body "Are you in the office today?".
  4. Notification has two actions: "Yes, I'm in" (calls `RecordOfficeDayUseCase`, marks `MANUAL`) and "Working remotely" (calls `RecordRemoteDayUseCase`).
  5. Actions use `PendingIntent` with a `BroadcastReceiver`.
- Scheduled in `DaysInOfficeApp` as `PeriodicWorkRequest` with `PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS` interval (15 min minimum for WorkManager, actual time targeting via flex period).

`NotificationScheduler`:
- Helper that wraps WorkManager scheduling calls.
- `fun scheduleDaily(context: Context)`.
- `fun cancel(context: Context)`.

#### Acceptance Criteria
- [ ] `DailyCheckInNotificationWorkerTest` passes:
  - `given today is a weekend when doWork then no notification posted`
  - `given today is a holiday when doWork then no notification posted`
  - `given today already has confirmed record when doWork then no notification posted`
  - `given today is a working day with no record when doWork then notification posted`
  - `given doWork succeeds then returns Result.success`
- [ ] Notification has exactly 2 actions.
- [ ] `./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.notification.*"` passes.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.notification.*"
```

---

### TASK-017: Home Screen Widget
**Status:** NOT_STARTED
**Dependencies:** TASK-005
**Complexity:** Medium

#### Context
A glanceable widget lets users check compliance and check in without opening the app.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/widget/CheckInWidget.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/widget/CheckInWidgetReceiver.kt`
- `app/src/main/res/xml/check_in_widget_info.xml`

#### Implementation Details
Use **Jetpack Glance** (`androidx.glance:glance-appwidget`).

`CheckInWidget` (`GlanceAppWidget`):
- Layout:
  - Compliance percentage (large text).
  - Period label (small text).
  - "Check In" button — if today is not already OFFICE.
  - "Checked In ✓" text — if today is OFFICE.
- Data: uses `GlanceStateDefinition` + coroutine scope to read `GetComplianceUseCase` and today's record.
- "Check In" button: triggers `RecordOfficeDayUseCase` via `ActionCallback`.

`CheckInWidgetReceiver` (`GlanceAppWidgetReceiver`):
- Registered in `AndroidManifest.xml` with `android.appwidget.provider` meta-data.

`check_in_widget_info.xml`:
- `minWidth="110dp"`, `minHeight="40dp"`.
- `updatePeriodMillis="1800000"` (30 min).

Add `glance-appwidget` dependency to `libs.versions.toml` and `app/build.gradle.kts`.

#### Acceptance Criteria
- [ ] `./gradlew assembleDebug` succeeds with Glance dependency added.
- [ ] `CheckInWidgetReceiver` registered in `AndroidManifest.xml`.
- [ ] `check_in_widget_info.xml` exists with correct `updatePeriodMillis`.
- [ ] Widget shows "Checked In ✓" when `todayStatus == OFFICE` (verify composable logic with unit test on the ActionCallback).
- [ ] Widget shows compliance percentage from `ComplianceResult`.

#### QA Verification Steps
```bash
./gradlew assembleDebug
grep "CheckInWidgetReceiver" app/src/main/AndroidManifest.xml
grep "check_in_widget_info" app/src/main/AndroidManifest.xml
```

---

## Phase 7: Quality and Polish

---

### TASK-018: End-to-End Integration Tests
**Status:** NOT_STARTED
**Dependencies:** TASK-013, TASK-014, TASK-015
**Complexity:** Complex

#### Context
Verifies the critical user journeys work end-to-end with a real in-memory database. These are the QA regression baseline.

#### Scope — Files to Create
- `app/src/androidTest/kotlin/com/carvalhorr/daysInOffice/DashboardIntegrationTest.kt`
- `app/src/androidTest/kotlin/com/carvalhorr/daysInOffice/CalendarIntegrationTest.kt`
- `app/src/androidTest/kotlin/com/carvalhorr/daysInOffice/ComplianceCalculationIntegrationTest.kt`

#### Test Scenarios

`ComplianceCalculationIntegrationTest` (uses real Room in-memory DB, real use cases, mocked CalendarDataSource):
- Inserts 10 working days worth of records (5 OFFICE, 3 REMOTE, 2 UNKNOWN) → verifies `ComplianceResult` matches expected values.
- Inserts a holiday on a Monday → verifies working day count decreases by 1.
- Inserts PTO for 2 days → verifies those days excluded from denominator.
- Checks `daysNeededToComply` is correct for various configurations.

`DashboardIntegrationTest` (Compose UI test, real use cases, in-memory DB):
- App starts → Dashboard visible → compliance ring shows "0%" (no records).
- Tap "Check in to office" → ring updates, button shows "Checked in ✓".
- Navigate to Calendar → current month shown → today's cell is green.

`CalendarIntegrationTest` (Compose UI test):
- Open Calendar → current month cells visible.
- Tap a past UNKNOWN day → bottom sheet appears.
- Tap "Mark as Office" → cell turns green.
- Tap "Mark as PTO" → cell turns grey.

#### Acceptance Criteria
- [ ] All tests in `ComplianceCalculationIntegrationTest` pass.
- [ ] All tests in `DashboardIntegrationTest` pass.
- [ ] All tests in `CalendarIntegrationTest` pass.
- [ ] `./gradlew connectedAndroidTest` exits with 0 failures.

#### QA Verification Steps
```bash
./gradlew connectedAndroidTest
```

---

### TASK-019: Permissions and Runtime Request Flow
**Status:** NOT_STARTED
**Dependencies:** TASK-012
**Complexity:** Medium

#### Context
Android runtime permissions must be requested correctly. Asking at the wrong time or not handling denial gracefully breaks detection silently.

#### Scope — Files to Create/Modify
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/permission/PermissionManager.kt`
- Modify: `DetectionSetupStep.kt` (TASK-012) — integrate permission requests

#### Implementation Details
`PermissionManager`:
- `fun requiredPermissions(methods: Set<DetectionMethod>): List<String>`:
  - `WIFI_CONNECTED` or `WIFI_SCAN` → `ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`.
  - `GEOFENCE` → `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`.
  - `MANUAL` → none.
- `fun hasAllPermissions(context: Context, methods: Set<DetectionMethod>): Boolean`.

In `DetectionSetupStep`:
- When user taps "Next" after selecting detection methods:
  1. Call `PermissionManager.requiredPermissions()`.
  2. Request permissions via `rememberLauncherForActivityResult(RequestMultiplePermissions)`.
  3. For `ACCESS_BACKGROUND_LOCATION`: must be requested **after** `ACCESS_FINE_LOCATION` is granted (Android 11+ requirement).
  4. If denied: show rationale dialog explaining why the permission is needed. Detection method still saved, but warn user detection may not work.
- `ACCESS_BACKGROUND_LOCATION` shown as separate step with explanation.

#### Acceptance Criteria
- [ ] `PermissionManagerTest` passes:
  - `given WIFI_CONNECTED method when requiredPermissions then ACCESS_FINE_LOCATION included`
  - `given GEOFENCE method when requiredPermissions then ACCESS_BACKGROUND_LOCATION included`
  - `given MANUAL only when requiredPermissions then empty list`
- [ ] Permission request flow: fine location requested before background location.
- [ ] Denial does not crash the app; user can still complete onboarding.
- [ ] `./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.permission.*"` passes.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.permission.*"
./gradlew assembleDebug
```

---

### TASK-020: Final Build Verification
**Status:** NOT_STARTED
**Dependencies:** All previous tasks
**Complexity:** Simple

#### Context
Final sanity check. All tests must pass, no lint errors, APK builds in release mode.

#### Acceptance Criteria
- [ ] `./gradlew testDebugUnitTest` — 0 failures.
- [ ] `./gradlew connectedAndroidTest` — 0 failures.
- [ ] `./gradlew lintDebug` — 0 errors (warnings acceptable).
- [ ] `./gradlew assembleRelease` — succeeds (unsigned APK).
- [ ] No hardcoded strings visible in UI (all in `strings.xml`).
- [ ] No `TODO` comments remain in production code.
- [ ] `ARCHITECTURE.md` is up to date with any deviations made during implementation.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
grep -r "TODO" app/src/main/kotlin/ && echo "TODOs found" || echo "No TODOs"
grep -r "hardcoded" app/build/reports/lint-results-debug.xml || echo "No hardcoded string errors"
```

---

## Summary

| Task | Phase | Complexity | Dependencies |
|---|---|---|---|
| TASK-001 | Foundation | Medium | none |
| TASK-002 | Foundation | Simple | 001 |
| TASK-003 | Foundation | Medium | 002 |
| TASK-004 | Foundation | Medium | 003 |
| TASK-005 | Business Logic | Complex | 004 |
| TASK-006 | Business Logic | Medium | 004 |
| TASK-007 | Detection | Simple | 005 |
| TASK-008 | Detection | Simple | 007 |
| TASK-009 | Detection | Complex | 007 |
| TASK-010 | Detection | Complex | 007, 008, 009 |
| TASK-011 | Navigation | Simple | 001 |
| TASK-012 | Onboarding | Complex | 005, 011 |
| TASK-013 | UI | Complex | 005, 011 |
| TASK-014 | UI | Complex | 005, 011 |
| TASK-015 | UI | Medium | 004, 011 |
| TASK-016 | Notifications | Medium | 010 |
| TASK-017 | Widget | Medium | 005 |
| TASK-018 | QA | Complex | 013, 014, 015 |
| TASK-019 | Polish | Medium | 012 |
| TASK-020 | Final | Simple | All |
