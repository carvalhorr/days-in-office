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
**Status:** DONE
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
- [x] `./gradlew assembleDebug` completes with no errors.
- [x] `./gradlew testDebugUnitTest` runs with 0 tests (no tests yet) and exits successfully.
- [x] `DaysInOfficeApp` extends `Application`, is annotated `@HiltAndroidApp`.
- [x] `MainActivity` is annotated `@AndroidEntryPoint`.
- [x] All permissions listed in ARCHITECTURE.md Section 13 are present in `AndroidManifest.xml`.
- [x] Version catalog `libs.versions.toml` contains entries for all libraries in ARCHITECTURE.md Section 2.

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
- `// ...` (rest of the logic)

#### ... (rest of the file)
