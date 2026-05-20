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
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/DaysInOfficeApp.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/MainActivity.kt`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values/colors.xml`

> **Note:** `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, and `gradle/libs.versions.toml` are all pre-seeded. Do NOT modify them — they are correct as-is.

#### Implementation Details
1. Add all permissions from ARCHITECTURE.md Section 13 to `AndroidManifest.xml`.
2. `DaysInOfficeApp` must extend `Application` and be annotated `@HiltAndroidApp`.
3. `MainActivity` must be annotated `@AndroidEntryPoint` and call `enableEdgeToEdge()`.
4. Theme: Material 3, dynamic color enabled, fallback primary color `#1565C0` (Blue 700).

#### Acceptance Criteria
- [x] `./gradlew assembleDebug` completes with no errors.
- [x] `./gradlew testDebugUnitTest` runs and exits successfully.
- [x] `DaysInOfficeApp` extends `Application`, is annotated `@HiltAndroidApp`.
- [x] `MainActivity` is annotated `@AndroidEntryPoint`.
- [x] All permissions listed in ARCHITECTURE.md Section 13 are present in `AndroidManifest.xml`.

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
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/data/db/DayRecordDaoTest.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/data/db/HolidayDaoTest.kt`

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
6. Dao tests run on the JVM under **Robolectric** with an **in-memory Room database** (`Room.inMemoryDatabaseBuilder`). No mocking of Room. Test classes are annotated:
   ```kotlin
   @RunWith(RobolectricTestRunner::class)
   @Config(sdk = [34])
   class DayRecordDaoTest { ... }
   ```
   Use `androidx.test.core.app.ApplicationProvider.getApplicationContext()` for the database builder context. Tests live in `src/test/` (not `src/androidTest/`) so they run under `testDebugUnitTest` without an emulator.

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
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.data.db.*"
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
- [ ] All Hilt modules compile — `./gradlew kspDebugKotlin` passes.
- [ ] No direct DAO or DataStore references exist outside the `data` package.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.data.repository.*"
./gradlew kspDebugKotlin
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

#### Acceptance Criteria
- [ ] `DetectionOrchestrator` injected with `Map<DetectionMethod, @JvmSuppressWildcards Detector>`.
- [ ] `runDetection` short-circuits on first positive result.
- [ ] `runDetection` never overwrites a record with `confirmedByUser = true` (asserted in a test that pre-seeds such a record).
- [ ] `DayDetectionWorker` is `@HiltWorker` with `@AssistedInject` constructor.
- [ ] Worker test passes using `WorkManagerTestInitHelper` (the same helper used by TASK-021's smoke suite — keep the pattern consistent).
- [ ] `DetectorModule` provides a `MapKey`-annotated entry for each `DetectionMethod`.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.detection.DetectionOrchestratorTest"
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.detection.DayDetectionWorkerTest"
./gradlew kspDebugKotlin
```

---

## Phase 4: User Interface

> **Visual source of truth.** Every task in this phase must implement the screens, layouts, copy, and interactions documented in `prototype/index.html` at the repo root. Open it in a browser; match what you see as closely as Compose permits. ARCHITECTURE.md §10 is authoritative for architectural shape; the prototype is authoritative for visual detail.

> **Compose-test approach.** Per ARCHITECTURE.md §11, prefer Compose UI Tests under `src/test/` with Robolectric where feasible. Only use `src/androidTest/` for tests that genuinely require a device — those become part of TASK-021's smoke suite, not per-task QA. ViewModel logic tests are unit tests under `src/test/`.

---

### TASK-011: Compose Theme and Design Tokens
**Status:** NOT_STARTED
**Dependencies:** TASK-001
**Complexity:** Simple

#### Context
Establishes the Material 3 theme, brand color palette, and typography scale used by every screen. All later UI tasks consume these. The prototype's `:root` CSS custom properties (in `prototype/index.html`, look in the `<style>` block near the top) name every color and define the typographic weights — port those to Compose color tokens and typography styles.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/theme/Color.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/theme/Type.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/theme/Theme.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/theme/DayStatusColors.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/app/theme/DayStatusColorsTest.kt`

#### Implementation Details
1. `Color.kt`: define brand primary `#1565C0` (Blue 700), brand variants, and Material 3 surface/background tokens matching the prototype.
2. `Type.kt`: standard Material 3 `Typography` with the prototype's weight/size scale.
3. `Theme.kt`: `DaysInOfficeTheme(content: @Composable () -> Unit)` — uses `dynamicLightColorScheme` / `dynamicDarkColorScheme` on Android 12+, falls back to brand colors otherwise. Always wraps content in `MaterialTheme`.
4. `DayStatusColors.kt`: extension property `ColorScheme.dayStatusColor(status: DayStatus): Color` returning the color per ARCHITECTURE.md §10:
   - `OFFICE` → green `#2E7D32`
   - `REMOTE` → blue `#1565C0`
   - `HOLIDAY` / `PTO` → grey `#9E9E9E`
   - `WEEKEND` → light grey `#BDBDBD`
   - `UNKNOWN` → amber `#F57F17`

#### Acceptance Criteria
- [ ] `./gradlew assembleDebug` compiles cleanly.
- [ ] `DaysInOfficeTheme` exists and wraps content in `MaterialTheme`.
- [ ] `DayStatusColorsTest` passes: a small Robolectric-driven Compose test asserts `DayStatus.OFFICE` maps to `Color(0xFF2E7D32)` etc.
- [ ] No hard-coded color literals inside `Theme.kt` outside the brand palette definition (use named constants).

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.app.theme.*"
```

---

### TASK-012: Navigation Graph and App Shell
**Status:** NOT_STARTED
**Dependencies:** TASK-004, TASK-011
**Complexity:** Medium

#### Context
Builds the single-activity Compose navigation that hosts the four top-level screens. The bottom nav (dashboard / calendar / settings) is visible after onboarding; onboarding has no bottom nav. Start destination is computed from `onboarding_complete` in DataStore (read via `MandateConfigRepository.getDetectionConfig()` or a dedicated flag — add one to `MandateConfigRepository` if not already there).

#### Scope — Files to Create / Modify
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/navigation/Destination.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/navigation/DaysInOfficeNavHost.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/navigation/BottomNavBar.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/MainActivity.kt` (modify — replace placeholder with `DaysInOfficeTheme { AppRoot() }` calling the NavHost)
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/app/navigation/NavigationTest.kt`

#### Implementation Details
1. `Destination` sealed class with `Onboarding`, `Dashboard`, `Calendar`, `Settings` — each exposing a `route` string and (for tabs) an `icon` + `label`.
2. `DaysInOfficeNavHost`: NavHost with the four destinations. Start destination is decided at first composition by reading the `onboardingComplete` flag.
3. `BottomNavBar`: Material 3 `NavigationBar` with three items (Dashboard / Calendar / Settings). Hidden when current destination is `Onboarding`. Match icons and labels to `prototype/index.html`'s bottom nav (look for `.bottom-nav` class near line 899).
4. `MainActivity`: must continue to call `enableEdgeToEdge()`; wrap content in `DaysInOfficeTheme`; place NavHost inside a `Scaffold` with the `BottomNavBar` slot.

#### Acceptance Criteria
- [ ] `Destination` is a sealed class with exactly the four routes named above.
- [ ] Starting the app with `onboarding_complete = false` shows the Onboarding destination.
- [ ] Starting with `onboarding_complete = true` shows the Dashboard destination.
- [ ] Bottom nav is hidden on Onboarding, visible on the other three.
- [ ] Compose UI test (Robolectric) verifies the three nav items are clickable and navigate correctly.
- [ ] No XML navigation graph — Compose Navigation only.

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.app.navigation.*"
```

---

### TASK-013: Onboarding Flow
**Status:** NOT_STARTED
**Dependencies:** TASK-005, TASK-006, TASK-012
**Complexity:** Complex

#### Context
A four-step onboarding wizard that captures the user's mandate target, period, working days, detection methods, and calendar sync preference. On completion, writes config to `MandateConfigRepository` and flips `onboarding_complete = true`. Match the prototype exactly: 4 ob-content steps, top progress dots, large hero copy per step.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/OnboardingViewModel.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/OnboardingScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/MandateSetupStep.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/PeriodSelectionStep.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/DetectionSetupStep.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/CalendarSetupStep.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/OnboardingViewModelTest.kt`

#### Implementation Details
1. `OnboardingViewModel`: `state: StateFlow<OnboardingUiState>` with fields: current step (0–3), draft `MandateConfig`, draft `DetectionConfig`, draft calendarSyncEnabled. Methods: `next()`, `back()`, `updateMandatePercentage(Float)`, `updateWorkingDays(Set<DayOfWeek>)`, `updatePeriod(MandatePeriod)`, `toggleDetectionMethod(DetectionMethod)`, `updateWifiSsid(String)`, `updateGeofence(lat, lng, radius)`, `updateCalendarSync(Boolean)`, `complete()`. `complete()` calls the repository to persist + sets `onboarding_complete = true`, then signals navigation.
2. `OnboardingScreen`: hosts the 4 step composables; top progress dots; `Back`/`Next` buttons. Step 0 → MandateSetup, Step 1 → PeriodSelection, Step 2 → DetectionSetup, Step 3 → CalendarSetup. Final `Next` becomes `Finish` and calls `vm.complete()`.
3. Step composables: match prototype IDs `ob0`–`ob3`. MandateSetup includes a percentage slider and day chips. PeriodSelection has a single-select list. DetectionSetup has checkbox-style options + the shared Wi-Fi SSID picker (when either Wi-Fi option is on) + geofence location setter. CalendarSetup is a single toggle plus a "request permission" CTA — actual permission request lives in TASK-017 and is wired here.
4. Persisted via `MandateConfigRepository.saveMandateConfig(...)` and `.saveDetectionConfig(...)`.

#### Acceptance Criteria
- [ ] All 4 steps render with the prototype's layout, labels, and controls (verify with a `composeTestRule` Robolectric test).
- [ ] `MandateSetupStep` supports slider values from 0–100% in 5% increments, displaying the average days-per-week below the value.
- [ ] `DetectionSetupStep` reveals the shared Wi-Fi SSID picker when either Wi-Fi method is enabled.
- [ ] `DetectionSetupStep` reveals the geofence picker when Geofencing is enabled.
- [ ] `OnboardingViewModelTest` covers: starting state, step advance/retreat, all field updaters, and `complete()` persisting and emitting a navigation event.
- [ ] No direct DataStore access from the ViewModel — only via `MandateConfigRepository`.

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.onboarding.*"
```

---

### TASK-014: Dashboard Screen
**Status:** NOT_STARTED
**Dependencies:** TASK-005, TASK-012
**Complexity:** Medium

#### Context
The main screen after onboarding. Shows current-period compliance ring, OFFICE/REMOTE/UNKNOWN stat strip, today's check-in card, and a "Sync calendar" link. Match the prototype's `#sc-dashboard` section (line ~741) — period chip at top, ring centered, stats below, check-in card, sync link.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/DashboardViewModel.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/DashboardScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/ComplianceRing.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/QuickCheckInButton.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/DashboardViewModelTest.kt`

#### Implementation Details
1. `DashboardViewModel`: `state: StateFlow<DashboardUiState>` derived from `GetComplianceUseCase()` (current period). Methods: `checkInAsOffice()`, `checkInAsRemote()`, `syncCalendar()`.
2. `ComplianceRing`: a `Canvas` composable drawing a 190×190 circular progress ring matching the prototype's SVG. Ring color is computed from `currentPercentage` vs `targetPercentage` per ARCHITECTURE.md §10 (Green / Amber / Red thresholds).
3. `QuickCheckInButton`: a row of two buttons (Office / Remote) with the prototype's brand styling. Disabled state when today's record is already `confirmedByUser = true`.
4. `DashboardScreen`: composes all of the above, using `collectAsStateWithLifecycle()` from the ViewModel.

#### Acceptance Criteria
- [ ] Ring color matches the threshold logic: ≥ T = green, T-10pp to <T = amber, <T-10pp = red.
- [ ] Stat strip shows correct counts from `ComplianceResult`.
- [ ] Tapping "Check in" calls `RecordOfficeDayUseCase` with `DetectionMethod.MANUAL`.
- [ ] Tapping "Remote" calls `RecordRemoteDayUseCase`.
- [ ] `DashboardViewModelTest` covers: state derivation from compliance flow, check-in actions, and the disabled state when today is already confirmed.
- [ ] Period chip displays the current period's label (e.g. "MAY 2026 · MONTHLY").

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.dashboard.*"
```

---

### TASK-015: Calendar Screen
**Status:** NOT_STARTED
**Dependencies:** TASK-005, TASK-006, TASK-012
**Complexity:** Medium

#### Context
Month-grid view of every day with color-coded status, weekday headers, prev/next month navigation, and a day-detail bottom sheet on tap. Match the prototype's `#sc-calendar` section (line ~796) — month nav, S-M-T-W-T-F-S headers, grid, legend.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/calendar/CalendarViewModel.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/calendar/ui/CalendarScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/calendar/ui/MonthCalendarView.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/calendar/ui/DayDetailSheet.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/feature/calendar/CalendarViewModelTest.kt`

#### Implementation Details
1. `CalendarViewModel`: `state: StateFlow<CalendarUiState>` exposing `currentMonth: YearMonth`, `days: List<DayRecord>` (driven by `GetCalendarMonthUseCase`), `selectedDay: DayRecord?`. Methods: `goToPreviousMonth()`, `goToNextMonth()`, `selectDay(date)`, `dismissDay()`, `overrideStatus(date, status)`.
2. `MonthCalendarView`: 7-column grid, days from previous/next month dimmed. Each cell is a color dot per `DayStatus` plus the day number.
3. `DayDetailSheet`: Material 3 `ModalBottomSheet` showing date, current status, detection method, "Mark as Office" / "Mark as Remote" / "Mark as PTO" buttons. Hitting one calls `overrideStatus()` with `confirmedByUser = true`.
4. Legend at bottom of `CalendarScreen` matching prototype's `.legend` block.

#### Acceptance Criteria
- [ ] Month grid renders every day of the displayed month plus padding from adjacent months.
- [ ] Day color matches the `DayStatus`.
- [ ] Tapping a day opens the `DayDetailSheet`.
- [ ] Status override marks `confirmedByUser = true` and never gets clobbered by detection (verified by checking `core.detection.DetectionOrchestrator` honors the flag — already tested in TASK-010).
- [ ] `CalendarViewModelTest` covers month navigation, day selection, and override.

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.calendar.*"
```

---

### TASK-016: Settings Screen
**Status:** NOT_STARTED
**Dependencies:** TASK-004, TASK-006, TASK-012
**Complexity:** Complex

#### Context
A grouped settings list with bottom-sheet pickers for each adjustable value. Match prototype's `#sc-settings` (line ~822) — four sections: Mandate, Detection, Calendar, Data. Each row opens a sheet via `openSettingsSheet('<id>')` in the prototype's JS — port each sheet to a Compose `ModalBottomSheet` with the same fields.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/SettingsViewModel.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/SettingsScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/sheets/TargetSheet.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/sheets/PeriodSheet.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/sheets/WorkingDaysSheet.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/sheets/WifiConnectedSheet.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/sheets/WifiScanSheet.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/sheets/GeofenceSheet.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/sheets/CalendarSyncSheet.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/feature/settings/SettingsViewModelTest.kt`

#### Implementation Details
1. `SettingsViewModel`: reads `MandateConfig` and `DetectionConfig` flows; exposes them as a single `SettingsUiState`. One `update<Field>(value)` method per editable field. `resetOnboarding()` flips `onboarding_complete = false` and triggers navigation back to Onboarding.
2. Each sheet is a small composable owning its own draft state, with Save/Cancel actions. Save calls the corresponding ViewModel updater.
3. `SettingsScreen`: matches the prototype's 4-section grouping with header text, card containers, and row layout (icon + label/value + chevron).
4. The Wi-Fi (connected) and Wi-Fi (scan) rows reuse the SSID-picker component from `feature/onboarding` (extract to a shared package `feature/shared/ui/` if not already there).

#### Acceptance Criteria
- [ ] All 7 sheets implemented as `ModalBottomSheet`s.
- [ ] Tapping a row opens its sheet; Save persists via the ViewModel; Cancel discards.
- [ ] `Sync now` row triggers `SyncCalendarUseCase` and shows the result inline.
- [ ] `Reset onboarding` flips the flag and navigates back to the Onboarding destination.
- [ ] `SettingsViewModelTest` covers all `update*` methods + `resetOnboarding`.

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.settings.*"
```

---

### TASK-017: Runtime Permission Flow
**Status:** NOT_STARTED
**Dependencies:** TASK-012
**Complexity:** Medium

#### Context
Compose-friendly helpers for requesting `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `READ_CALENDAR`, and `POST_NOTIFICATIONS` with appropriate rationale UI. Used by TASK-013 (onboarding) and TASK-016 (settings).

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/permissions/AppPermission.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/permissions/PermissionRequester.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/permissions/RationaleDialog.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/permissions/PermissionRequesterTest.kt`

#### Implementation Details
1. `AppPermission` enum: `FINE_LOCATION`, `BACKGROUND_LOCATION`, `CALENDAR`, `NOTIFICATIONS`. Each maps to the Android string + a rationale string + a "denied permanently" string.
2. `PermissionRequester`: `@Composable fun rememberPermissionRequester(): PermissionRequester` returning an API with `suspend fun request(AppPermission): PermissionState` (Granted / Denied / DeniedPermanently). Uses `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` under the hood.
3. `RationaleDialog`: Material 3 dialog with the rationale text, "Grant" button (triggers the system permission dialog), "Not now" button.
4. Order for location: request `FINE_LOCATION` first; only if granted, request `BACKGROUND_LOCATION` (Android 11+).
5. Skip `POST_NOTIFICATIONS` request on API < 33 (Android < 13).

#### Acceptance Criteria
- [ ] `AppPermission.NOTIFICATIONS` is silently treated as Granted on API < 33.
- [ ] `BACKGROUND_LOCATION` is only requested after `FINE_LOCATION` is granted.
- [ ] `RationaleDialog` is shown before the system dialog the first time, and again on a "denied permanently" state (with a "Settings" button that links to app settings).
- [ ] `PermissionRequesterTest` (Robolectric) covers the API-version branching for notifications and the FINE → BACKGROUND ordering.

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.permissions.*"
```

---

### TASK-018: Notification Scheduler
**Status:** NOT_STARTED
**Dependencies:** TASK-005, TASK-010, TASK-014
**Complexity:** Medium

#### Context
A daily reminder notification at user-configurable time (default 18:00) that prompts the user to confirm whether they went to the office, with quick-action buttons. Falls back to a weekly summary on Fridays.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/notification/NotificationScheduler.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/notification/DailyCheckInNotificationWorker.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/notification/NotificationChannelManager.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/notification/NotificationActionReceiver.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/notification/DailyCheckInNotificationWorkerTest.kt`
- Manifest entries: register `NotificationActionReceiver` (exported=false), keep `BOOT_COMPLETED` permission usage minimal.

#### Implementation Details
1. `NotificationChannelManager`: creates the "daily_checkin" channel (Default importance) and "weekly_summary" channel on first run.
2. `NotificationScheduler`: schedules `DailyCheckInNotificationWorker` as a periodic WorkRequest, daily at the user's configured time (default 18:00 local). Reschedules on boot via a `BootReceiver` (already covered by `RECEIVE_BOOT_COMPLETED` permission).
3. `DailyCheckInNotificationWorker`: `@HiltWorker` `@AssistedInject`. On run: reads today's `DayRecord` via `DayRecordRepository`. If status is `UNKNOWN` and today is a working day, posts the notification with `Office` / `Remote` / `Open app` actions. Skips on holidays/PTO/weekends.
4. `NotificationActionReceiver`: handles the action buttons. `Office` calls `RecordOfficeDayUseCase(today, DetectionMethod.MANUAL)`; `Remote` calls `RecordRemoteDayUseCase(today)`; both with `confirmedByUser = true`.

#### Acceptance Criteria
- [ ] Notification only posts when today's record status is `UNKNOWN`.
- [ ] Notification is suppressed on `HOLIDAY` / `PTO` / `WEEKEND`.
- [ ] Action buttons correctly trigger the use cases.
- [ ] Channel exists and is created idempotently.
- [ ] `DailyCheckInNotificationWorkerTest` covers all three suppression conditions and the post-notification path.

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.notification.*"
```

---

### TASK-019: Home Screen Widget
**Status:** NOT_STARTED
**Dependencies:** TASK-005, TASK-014
**Complexity:** Medium

#### Context
A Glance-based home-screen widget showing current-period compliance percentage + a "Check In" button. Tapping the widget opens the app at Dashboard; tapping the Check In button records office for today without leaving the home screen.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/widget/CheckInWidget.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/widget/CheckInWidgetReceiver.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/widget/CheckInActionReceiver.kt`
- `app/src/main/res/xml/check_in_widget_info.xml`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/widget/CheckInWidgetTest.kt`
- Manifest entries for the widget receiver.

#### Implementation Details
1. Add `androidx.glance:glance-appwidget` dependency to the version catalog — note this is the only new dependency added by Phase 4 and must be approved before proceeding.
2. `CheckInWidget`: `GlanceAppWidget` rendering an `Image` (mini ring) + `Text` (e.g. "50%") + `Button("Check In")`. Use `actionStartActivity` on the body to open MainActivity, and `actionSendBroadcast(CheckInActionReceiver)` on the button.
3. `CheckInWidgetReceiver`: `GlanceAppWidgetReceiver` wiring the widget into the system.
4. `CheckInActionReceiver`: `BroadcastReceiver` that, on receive, calls `RecordOfficeDayUseCase(today, DetectionMethod.MANUAL)` and triggers a widget refresh.
5. Compliance percentage is read once per widget update via the use case; updates are triggered after each `RecordOfficeDayUseCase` call and after each WorkManager run.

#### Acceptance Criteria
- [ ] Widget renders with compliance % and a Check In button.
- [ ] Tapping the Check In button records today as office (verified in `CheckInWidgetTest` using a fake `RecordOfficeDayUseCase`).
- [ ] Tapping the widget body launches MainActivity with the Dashboard destination.
- [ ] Widget refreshes when an office-day is recorded (assert that `GlanceAppWidgetManager.updateAll()` is called).

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.widget.*"
grep "CheckInWidgetReceiver" app/src/main/AndroidManifest.xml
```

---

### TASK-020: Empty / Error States, Dark Theme, and Final Polish
**Status:** NOT_STARTED
**Dependencies:** TASK-013, TASK-014, TASK-015, TASK-016, TASK-017, TASK-018, TASK-019
**Complexity:** Medium

#### Context
Catches the rough edges before TASK-021 (release smoke). Adds empty states, error states, dark-theme verification, accessibility passes, and a few visual regression-style Compose tests under Robolectric.

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/ui/common/EmptyState.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/ui/common/ErrorState.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/app/ui/common/EmptyStateTest.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/app/ui/common/ErrorStateTest.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/DashboardScreenSnapshotTest.kt` (Robolectric Compose test verifying dashboard renders in both light and dark themes)
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/feature/calendar/CalendarScreenSnapshotTest.kt`

#### Implementation Details
1. `EmptyState`: shared composable used by Dashboard ("No records yet — check in to get started") and Calendar ("This month is empty").
2. `ErrorState`: shared composable for use-case errors (e.g. calendar permission denied, DataStore read failure). Includes a retry button.
3. Dashboard ViewModel updated to emit empty + error states alongside the existing success state.
4. Dark theme: every screen must render correctly when the system is in dark mode. Verify via `DashboardScreenSnapshotTest` and `CalendarScreenSnapshotTest` rendering the screen twice (once light, once dark) and asserting no exceptions / no empty hierarchy.
5. Accessibility: every interactive composable has a non-default `Modifier.semantics { contentDescription = ... }` for icons-only buttons. Day cells in the calendar have `contentDescription = "2026-05-08, Office"` etc.

#### Acceptance Criteria
- [ ] `EmptyState` and `ErrorState` composables exist and are used by Dashboard and Calendar.
- [ ] `EmptyStateTest` and `ErrorStateTest` pass under Robolectric.
- [ ] Dashboard and Calendar snapshot tests render successfully in both light and dark themes.
- [ ] All icon-only buttons have content descriptions (grep for `IconButton` to verify none are bare).

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.app.ui.common.*"
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.dashboard.DashboardScreenSnapshotTest"
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.calendar.CalendarScreenSnapshotTest"
```

---

## Phase 6: Bug Fixes (from 2026-05-18 audit)

> This phase fixes bugs discovered after the first complete run (claude × sonnet-4-6 reached 21/21 on 2026-05-18). Each task corresponds to one or more bugs from `BUGS.md` and is fully self-contained — you don't need to read `BUGS.md` to do the work, but the bug register has the full triage and the original audit screenshots at `experiment/audit-shots/2026-05-18/`.
>
> These tasks **modify existing code**, not greenfield-implement it. Be surgical: change only what the task specifies. Always run the existing test suite to make sure unrelated tests still pass.

---

### TASK-022: Fix BUG-004 — exclude PTO and HOLIDAY days from compliance calculation
**Status:** NOT_STARTED
**Dependencies:** TASK-005
**Complexity:** Simple

#### Context
When a user marks a day as PTO via the Calendar's `DayDetailSheet`, the compliance calculation does not reflect it: `totalWorkingDays` stays the same and the ring percentage doesn't change. This breaks the architecture's implicit invariant (`ARCHITECTURE.md` §5.1 + §15) that PTO/HOLIDAY days are excluded from the mandate calculation.

**Confirmed root cause:**
1. `core/domain/usecase/GetWorkingDaysUseCase.kt:18` filters only by `HolidayEntity` rows in the `holidays` table, not by `DayRecord` rows with `status = PTO` or `status = HOLIDAY`.
2. `core/domain/usecase/GetComplianceUseCase.kt:61-83` (`buildResult`) then uses `workingDays.size` as the denominator. PTO/HOLIDAY day-records are silently dropped from numerator counts (don't match OFFICE/REMOTE/UNKNOWN) but still inflate the denominator.

The PTO button in `DayDetailSheet.kt:98-103` works fine; the bug is purely in the use-case logic.

#### Scope — Files to Modify
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/GetComplianceUseCase.kt`

#### Scope — Files to Create
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/domain/usecase/GetComplianceUseCaseTest.kt` (if it doesn't already exist)

#### Implementation Details
Modify `GetComplianceUseCase.buildResult` to compute a user-exclusion set from records with `status = PTO` or `status = HOLIDAY`, and subtract those days from the working-days set before computing counts:

```kotlin
private fun buildResult(
    config: MandateConfig,
    records: List<DayRecord>,
    workingDays: List<LocalDate>,
    start: LocalDate, end: LocalDate
): ComplianceResult {
    val recordMap = records.associateBy { it.date }
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
        periodStart = start,
        periodEnd = end,
        totalWorkingDays = effectiveWorkingDays.size,
        officeDays = officeDays,
        remoteDays = remoteDays,
        unknownDays = unknownDays,
        targetPercentage = config.targetPercentage
    )
}
```

Leave `GetWorkingDaysUseCase` alone — it correctly represents "potential working days from a calendar perspective" (used by the Calendar view to display every weekday). The compliance use case applies the user-exclusions on top.

#### Acceptance Criteria
- [ ] `GetComplianceUseCaseTest` has a test: given 10 working days with 2 marked PTO, `totalWorkingDays == 8`.
- [ ] `GetComplianceUseCaseTest` has a test: given 10 working days, 5 OFFICE, 2 PTO, the `currentPercentage` computes against the smaller denominator (5/8 ≈ 0.625), not the original 10.
- [ ] Days marked as HOLIDAY (via day-record, not the holidays table) are treated the same way as PTO — also excluded.
- [ ] Pre-existing tests in the repo still pass — don't break anything.
- [ ] `daysNeededToComply` is never negative (`ComplianceResult` enforces this).

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.domain.usecase.GetComplianceUseCaseTest"
./gradlew assembleDebug
```

---

### TASK-023: Fix BUG-001 — add back navigation from Settings to Dashboard
**Status:** NOT_STARTED
**Dependencies:** TASK-012, TASK-016
**Complexity:** Simple

#### Context
When the user taps the gear icon on Dashboard, they reach Settings — but there's no visible back affordance, and many users don't realize the bottom-nav Dashboard tab is the way back. `SettingsScreen.kt:87` has no `navigationIcon` in its TopAppBar; `DaysInOfficeNavHost.kt:47-49` pushes Settings as a drill-in instead of switching tabs.

#### Scope — Files to Modify
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/SettingsScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/navigation/DaysInOfficeNavHost.kt`

#### Implementation Details
1. **In `SettingsScreen.kt`:** add a `navigationIcon` to the TopAppBar with a back arrow. Take a new `onNavigateBack: (() -> Unit)?` parameter (nullable) — if null, render no nav icon (so when Settings is reached via the bottom-nav tab there's no redundant back arrow):
```kotlin
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)?,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SettingsViewModel
) {
    ...
    TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        }
    )
    ...
}
```

2. **In `DaysInOfficeNavHost.kt`:** pass `onNavigateBack = if (navController.previousBackStackEntry != null) { { navController.popBackStack() } } else null`. This ensures the back arrow only shows when there's somewhere to pop to (which is true when the gear-icon path is used; false when the user lands on Settings via the bottom-nav tab from a clean state).

3. Keep the existing bottom-nav behaviour unchanged — Settings remains a tab destination, and the bottom-nav Dashboard tab still works as a way back.

#### Acceptance Criteria
- [ ] Tapping the gear icon on Dashboard navigates to Settings.
- [ ] A back arrow is visible in the Settings TopAppBar when Settings was reached via the gear icon.
- [ ] Tapping the back arrow returns to Dashboard.
- [ ] When Settings is reached via the bottom-nav Settings tab (after restoring state), the back arrow is **not** shown (no previous back-stack entry).
- [ ] Bottom-nav still works for navigating between Dashboard / Calendar / Settings.
- [ ] No other navigation flows are affected.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.app.navigation.*"
./gradlew assembleDebug
```

---

### TASK-024: Fix BUG-007 — allow re-selecting today's check-in from Dashboard
**Status:** NOT_STARTED
**Dependencies:** TASK-014
**Complexity:** Simple

#### Context
After the user taps either "Check in" or "Remote" on the Dashboard, both buttons go disabled (`QuickCheckInButton.kt:31, 40` — `enabled = !isConfirmedToday`). This was implemented from TASK-014's literal "disabled state when confirmed" acceptance criterion but over-applies the architecture invariant. Key Invariant #1 (`ARCHITECTURE.md` §15) is about **automated detection** not overwriting user choices — it has nothing to do with the user manually correcting their own mistake.

The ViewModel methods (`DashboardViewModel.kt:83-93`) already allow re-write — they call `RecordOfficeDayUseCase` / `RecordRemoteDayUseCase` which write `confirmedByUser = true` regardless. The bug is purely the UI's `enabled` predicate.

#### Scope — Files to Modify
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/QuickCheckInButton.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/DashboardScreen.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/DashboardViewModelTest.kt` (add a test if not already covered)

#### Implementation Details
1. **Change `QuickCheckInButton` signature** to accept `currentStatus: DayStatus?` instead of `isConfirmedToday: Boolean`. Always enable both buttons. Render the active selection visually:
```kotlin
@Composable
fun QuickCheckInButton(
    onCheckInOffice: () -> Unit,
    onCheckInRemote: () -> Unit,
    currentStatus: DayStatus?,
    modifier: Modifier = Modifier
) {
    val officeSelected = currentStatus == DayStatus.OFFICE
    val remoteSelected = currentStatus == DayStatus.REMOTE
    Row(...) {
        if (officeSelected) {
            Button(onClick = onCheckInOffice, ..., colors = ButtonDefaults.buttonColors(containerColor = colorOfficeGreen)) { Text("🏢 Check in") }
        } else {
            OutlinedButton(onClick = onCheckInOffice, ..., colors = ButtonDefaults.outlinedButtonColors(contentColor = colorOfficeGreen), border = BorderStroke(1.dp, colorOfficeGreen)) { Text("🏢 Check in") }
        }
        // Same pattern flipped for Remote button using colorRemoteBlue
    }
}
```
2. **Update `DashboardScreen.kt:CheckInCard`** call site (around line 273-278) to pass `currentStatus = todayRecord?.status` instead of `isConfirmedToday`.
3. Verify the architectural invariant about "never overwrite confirmed records" still holds for **automated detection** — that's in `core/detection/DetectionOrchestrator.kt`, unaffected by this fix. Don't touch that path.

#### Acceptance Criteria
- [ ] Tapping either button always works, regardless of `confirmedByUser`.
- [ ] The currently-selected status (OFFICE / REMOTE) is visually distinct (filled button vs outlined).
- [ ] Re-tapping the same button is idempotent (no error, no flicker, status remains the same).
- [ ] Switching from OFFICE → REMOTE → OFFICE updates the day record through both transitions (verify in DashboardViewModelTest).
- [ ] `DetectionOrchestrator` still respects the `confirmedByUser` flag for automated detection (don't break this — keep its existing tests passing).
- [ ] Button labels include the prototype's emojis: `🏢 Check in` and `🏠 Remote`.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.dashboard.*"
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.detection.*"
./gradlew assembleDebug
```

---

### TASK-025: Fix BUG-002 — UI polish to better match prototype
**Status:** NOT_STARTED
**Dependencies:** TASK-020
**Complexity:** Complex (many small fixes across many files)

#### Context
A visual audit on 2026-05-18 (`experiment/audit-shots/2026-05-18/`) found 10 specific divergences between the implemented app and `prototype/index.html`. This task fixes all 10 in one pass. Each is small individually; bundling reduces total work vs filing 10 separate tasks.

#### Scope — Files to Modify
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/DashboardScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/QuickCheckInButton.kt` (also modified by TASK-024 — re-read after that lands)
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/ComplianceRing.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/calendar/ui/CalendarScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/SettingsScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/navigation/BottomNavBar.kt`

#### Implementation Details
Open `prototype/index.html` in a browser (or read the relevant sections) to confirm each visual detail.

**1. Replace Material vector icons with emoji** (matches prototype convention):
   - `DashboardScreen.kt:64` — `Icon(Icons.Default.Settings)` → `Text("⚙️", style = MaterialTheme.typography.titleLarge)`
   - `SettingsScreen.kt` — every settings row icon (currently uses `Icons.Default.Settings`, `Icons.Default.DateRange`, `Icons.Default.LocationOn`, `Icons.Default.Refresh`):
     - Target row: `🎯`
     - Period row: `📅`
     - Working days row: `📆`
     - Wi-Fi (connected): `📶`
     - Wi-Fi (scan only): `📡`
     - Geofencing: `📍`
     - Calendar sync: `🗓️`
     - Sync now: `🔄`
     - Export CSV: `📤`
     - Reset onboarding: `🔁`
   - Wrap each in a small composable like `EmojiIcon(emoji)` if you find yourself repeating styling.

**2. Add vertical dividers between StatItems** (`DashboardScreen.kt:200-235`, `StatsStrip`):
   - Between each `StatItem`, render a `Box(modifier = Modifier.width(1.dp).height(60.dp).background(MaterialTheme.colorScheme.outlineVariant))`.
   - Use `Row` with `Arrangement.SpaceEvenly` or compute proper spacing so dividers don't collide with text.

**3. Fix Period chip colour** (`DashboardScreen.kt:166-180`, `PeriodChip`):
   - Replace `color = MaterialTheme.colorScheme.secondaryContainer` with a fixed brand purple (`Color(0xFFEADDFF)` for background, `Color(0xFF21005D)` for text). Define in `app/theme/Color.kt` as `BrandPeriodChipBg` and `BrandPeriodChipText`.

**4. Fix compliance ring** (`ComplianceRing.kt`):
   - Track stroke width should be 18/190 of the ring diameter (so for a 190dp ring, 18dp stroke; scale if you used a different size).
   - Track colour should be `#EDE7F6` (light lavender). Use `Color(0xFFEDE7F6)`.
   - Progress arc should render as a clean arc, not a rounded-rect blob. If using `Canvas.drawArc`, ensure `useCenter = false` and `style = Stroke(width, cap = StrokeCap.Round)`. The arc should sweep clockwise from 12 o'clock proportional to the percentage.

**5. Remove M3 NavigationBar pill indicator** (`BottomNavBar.kt:20-36`):
   ```kotlin
   NavigationBarItem(
       ...
       colors = NavigationBarItemDefaults.colors(
           indicatorColor = Color.Transparent
       )
   )
   ```

**6. Tweak check-in card subtitle copy** (`DashboardScreen.kt:245-251`):
   - When `currentStatus == OFFICE`: `"✓ Checked in for the office"`
   - When `currentStatus == REMOTE`: `"Marked as remote day"`
   - When `currentStatus == null` or `UNKNOWN`: `"Are you in the office today?"`
   - When `PTO`: `"On PTO today"`

**7. Restructure Calendar month nav** (`CalendarScreen.kt`):
   - TopAppBar should have just `title = { Text("Calendar") }` — no `navigationIcon` row.
   - Below the TopAppBar (inside the screen content), add a separate `Row` with `IconButton(prev)`, `Text(monthName)`, `IconButton(next)` — matching prototype's `.month-nav`.

**8. Add Weekend legend item** (`CalendarScreen.kt`, the legend at the bottom):
   - Add a 5th legend item: `LegendItem(color = Color(0xFFBDBDBD), label = "Weekend")` between Holiday/PTO and Unknown (or at the end — match the prototype's ordering).

**9. Strengthen Settings section header style** (`SettingsScreen.kt`, section title `Text`):
   - Add `fontWeight = FontWeight.SemiBold` (or `Bold`).
   - Add `modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 16.dp)`.
   - Verify colour is the same blue used throughout the section labels.

**10. (No fix needed for the Target slider — it already matches the prototype reasonably well.** Leave `TargetSheet.kt` as-is.)

#### Acceptance Criteria
- [ ] `./gradlew assembleDebug` succeeds.
- [ ] `./gradlew testDebugUnitTest` passes — no existing tests broken.
- [ ] Manual visual check (after install on emulator): Dashboard, Calendar, Settings each match the prototype more closely than before:
  - Dashboard: emoji gear icon, period chip purple, ring stroke thinner, stats dividers present, check-in card emojis on buttons.
  - Calendar: separate `< Month >` row below the app bar, 5-item legend including Weekend.
  - Settings: emoji icons on every row, stronger section header style.
  - Bottom nav: no pill indicator around the selected icon.
- [ ] No regressions in navigation, data flow, or persistence.

#### QA Verification Steps
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

---

### TASK-026: Fix BUG-003 + BUG-005 — auto-detect for geofence and Wi-Fi SSID
**Status:** NOT_STARTED
**Dependencies:** TASK-013, TASK-016, TASK-017
**Complexity:** Complex

#### Context
The geofence picker (in onboarding's Detection step + Settings → Geofencing sheet) and the Wi-Fi SSID picker (in onboarding's Detection step + Settings → Wi-Fi (connected) / Wi-Fi (scan only) sheets) currently require manual text-field entry only. The prototype has primary auto-detect buttons in both: "📍 Use current location" for geofence, "📡 Scan for networks" for Wi-Fi. These are missing.

This task adds both auto-detect flows together because they share infrastructure (new data sources, new shared picker composables, integration with the existing `PermissionRequester` from TASK-017).

#### Scope — Files to Create
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/datasource/LocationProvider.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/data/datasource/WifiScanner.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/shared/ui/GeofencePicker.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/shared/ui/WifiSsidPicker.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/data/datasource/LocationProviderTest.kt`
- `app/src/test/kotlin/com/carvalhorr/daysInOffice/core/data/datasource/WifiScannerTest.kt`

#### Scope — Files to Modify
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/core/di/DataSourceModule.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/onboarding/ui/DetectionSetupStep.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/sheets/GeofenceSheet.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/sheets/WifiConnectedSheet.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/sheets/WifiScanSheet.kt`

#### Implementation Details

**1. `LocationProvider.kt`** — wraps `FusedLocationProviderClient`:
```kotlin
class LocationProvider @Inject constructor(@ApplicationContext private val context: Context) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }
    suspend fun getCurrentLocation(): Result<Location> = suspendCancellableCoroutine { cont ->
        try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) cont.resume(Result.success(loc))
                    else cont.resume(Result.failure(IllegalStateException("Location unavailable")))
                }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        } catch (e: SecurityException) {
            cont.resume(Result.failure(e))
        }
    }
}
```
Note: caller must hold `ACCESS_FINE_LOCATION` permission. Returns a `Result` so the caller can handle errors without exceptions.

**2. `WifiScanner.kt`** — wraps `WifiManager`:
```kotlin
class WifiScanner @Inject constructor(@ApplicationContext private val context: Context) {
    private val wifi by lazy { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    suspend fun scanForSsids(): Result<List<String>> {
        // Register a BroadcastReceiver for SCAN_RESULTS_AVAILABLE_ACTION, call startScan,
        // wait for the receiver (with a 10s timeout), collect getScanResults().
        // Return Result.failure if startScan returns false (throttled) or timeout.
        // Filter results to unique non-empty SSIDs.
    }
}
```
Implementation note: Android's `WifiManager.startScan()` returns boolean; false means throttled. Use `BroadcastReceiver` with `SCAN_RESULTS_AVAILABLE_ACTION` to know when results are ready. Have a 10s timeout.

**3. `GeofencePicker.kt`** — shared composable used by onboarding and settings:
```kotlin
@Composable
fun GeofencePicker(
    latitude: Double?,
    longitude: Double?,
    radius: Float?,
    onUpdate: (lat: Double, lng: Double, radius: Float) -> Unit,
    locationProvider: LocationProvider = hiltViewModel<GeofencePickerViewModel>().locationProvider,
    permissionRequester: PermissionRequester = rememberPermissionRequester()
) {
    // Primary "📍 Use current location" button:
    //   - request ACCESS_FINE_LOCATION
    //   - call locationProvider.getCurrentLocation()
    //   - fill in lat/lng on success
    //   - show friendly error and fall back to manual entry on denial/failure
    // Manual lat/lng/radius text fields below as fallback
    // "Set Location" button only enabled when all 3 fields are valid
}
```
Note: don't put `LocationProvider` directly in the composable — extract a ViewModel or use Hilt's `hiltViewModel()` pattern. Or use `LocalContext.current` to get a Hilt-injected instance through `EntryPointAccessors`.

**4. `WifiSsidPicker.kt`** — same pattern, for Wi-Fi:
```kotlin
@Composable
fun WifiSsidPicker(
    currentSsid: String?,
    onUpdate: (String) -> Unit,
    wifiScanner: WifiScanner = …,
    permissionRequester: PermissionRequester = rememberPermissionRequester()
) {
    // Primary "📡 Scan for networks" button:
    //   - request ACCESS_FINE_LOCATION (required for scan results on Android 9+)
    //   - call wifiScanner.scanForSsids()
    //   - display tappable list of detected SSIDs
    //   - graceful "throttled, try again later" message if scan returns failure
    // Manual SSID text input below as fallback (for hidden SSIDs)
}
```

**5. `DataSourceModule.kt`** — add provides:
```kotlin
@Provides @Singleton
fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider = LocationProvider(context)
@Provides @Singleton
fun provideWifiScanner(@ApplicationContext context: Context): WifiScanner = WifiScanner(context)
```

**6. Wire pickers into call sites:**
- `DetectionSetupStep.kt:81-88` — replace the SSID `OutlinedTextField` with `WifiSsidPicker`.
- `DetectionSetupStep.kt:102-157` — replace `GeofenceInputSection` with `GeofencePicker`.
- `GeofenceSheet.kt:82-103` — replace inline lat/lng fields with `GeofencePicker`.
- `WifiConnectedSheet.kt:73-82` — replace SSID text field with `WifiSsidPicker`.
- `WifiScanSheet.kt:73-82` — same.

#### Acceptance Criteria
- [ ] `LocationProvider` and `WifiScanner` data sources exist and have unit tests.
- [ ] `GeofencePicker` composable exists and is used in 2 places.
- [ ] `WifiSsidPicker` composable exists and is used in 3 places.
- [ ] Tapping "Use current location" requests ACCESS_FINE_LOCATION; if granted, fills in lat/lng.
- [ ] Tapping "Scan for networks" requests ACCESS_FINE_LOCATION; if granted, performs a scan and displays a tappable list of detected SSIDs.
- [ ] Permission denial falls back gracefully to manual entry with a brief explainer message.
- [ ] Wi-Fi scan throttling produces a "try again later" message rather than an indefinite spinner.
- [ ] Manual entry still works as the fallback in both pickers.
- [ ] `./gradlew testDebugUnitTest` passes — including the new tests and all existing tests.
- [ ] No new dependencies added — `play-services-location` is already in the version catalog from TASK-009.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.data.datasource.*"
./gradlew assembleDebug
```

---

### TASK-027: Fix BUG-008 — Home tab is a no-op when Settings is opened via gear icon
**Status:** NOT_STARTED
**Dependencies:** TASK-012
**Complexity:** Simple

#### Context
When the user opens Settings via the ⚙️ gear icon on the Dashboard, tapping the **Home** tab in the bottom navigation does nothing. The user is stranded on Settings. See `BUGS.md` BUG-008 for the full repro. The `MainFlowSmokeTest.c02_homeTabReturnsToDashboardAfterGearIconSettings` test is the regression guard and currently fails.

**Confirmed root cause:** `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/navigation/DaysInOfficeNavHost.kt:47-49` — the gear icon does a plain `navController.navigate(Destination.Settings.route)` push, placing Settings on top of `[Dashboard]`. The bottom-nav `onClick` in `app/navigation/BottomNavBar.kt:32-40` then uses `popUpTo(graph.startDestinationId) { saveState = true }` + `launchSingleTop = true` + `restoreState = true`. With Dashboard already on the back stack covered by Settings, Nav-Compose treats the navigate-to-Dashboard as a no-op rather than popping Settings.

#### Scope — Files to Modify
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/navigation/DaysInOfficeNavHost.kt`
- `CLAUDE.md` (root + run-repo copy) — remove the `c02_...` row from "Known-Failing UI Smoke Tests" once green.

#### Implementation Details
Change the gear icon's `onNavigateToSettings` lambda to use the same tab-switch nav options as `BottomNavBar.kt`:

```kotlin
DashboardScreen(
    onNavigateToSettings = {
        navController.navigate(Destination.Settings.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    },
    viewModel = viewModel
)
```

Settings becomes the active destination, the back stack stays as `[Dashboard]`, and tapping the Home tab via the normal tab-switch path restores Dashboard with its saved state.

Do **not** revert the existing back arrow added by TASK-023's BUG-001 fix — leave that affordance alone.

#### Acceptance Criteria
- [ ] From Dashboard, tapping ⚙️ navigates to Settings.
- [ ] From Settings (opened via gear), tapping the Home tab returns to Dashboard with state preserved.
- [ ] After the round trip, the back stack contains only the start destination.
- [ ] The existing BUG-001 back arrow continues to work.
- [ ] `MainFlowSmokeTest.c02_homeTabReturnsToDashboardAfterGearIconSettings` passes; its row is removed from `Known-Failing UI Smoke Tests` in CLAUDE.md (both copies).

#### QA Verification Steps
```bash
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.carvalhorr.daysInOffice.smoke.ui.MainFlowSmokeTest
# Expected: c02_homeTabReturnsToDashboardAfterGearIconSettings PASSES.
```

---

### TASK-028: Fix BUG-009 — empty white strip above the TopAppBar title
**Status:** NOT_STARTED
**Dependencies:** TASK-012
**Complexity:** Simple

#### Context
A blank/white horizontal strip is visible at the top of the app, above the screen title in the `TopAppBar`. The title sits lower than expected, with an empty gap to the top edge of the screen. The prototype (`prototype/index.html`) has no such gap. See `BUGS.md` BUG-009 for the full report.

**Suspected root cause:** `app/src/main/kotlin/com/carvalhorr/daysInOffice/app/MainActivity.kt:31` calls `enableEdgeToEdge()`, which makes the app draw under the system bars. Combined with the `Scaffold` propagating `paddingValues` (which includes the status-bar inset) to the `NavHost`, each screen's content — including the `TopAppBar` — is pushed below the status bar. Material 3 `TopAppBar` also applies its own window insets internally, producing a double-applied status-bar inset and the visible blank strip.

#### Scope — Files to Modify
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/dashboard/ui/DashboardScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/calendar/ui/CalendarScreen.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/settings/ui/SettingsScreen.kt`

#### Implementation Details
In each screen's `TopAppBar` invocation, set `windowInsets = WindowInsets(0)`:

```kotlin
TopAppBar(
    title = { Text("Days in Office") },
    actions = { /* ... */ },
    windowInsets = WindowInsets(0)
)
```

The Scaffold's `paddingValues` already carries the status-bar inset and is forwarded to the screen content; the `TopAppBar` taking zero insets prevents the double-application. The status-bar background remains the app's surface color via the Scaffold's standard window-inset handling.

Verify the same fix works on Calendar and Settings — apply it consistently.

#### Acceptance Criteria
- [ ] On Dashboard, Settings, and Calendar: no blank strip above the title; the title sits directly under the status bar.
- [ ] The status-bar area is filled with the app's primary/surface color, matching the prototype.
- [ ] `./gradlew lintDebug` does not introduce new warnings.
- [ ] All existing UI tests still pass.

#### QA Verification Steps
```bash
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
# Visual verification on emulator — capture a screenshot of each of the
# three screens and compare against prototype/index.html. No blank strip
# above the title; status-bar area is the app's surface color.
```

---

### TASK-029: Fix BUG-010 — Dashboard check-in buttons produce no visible state change
**Status:** NOT_STARTED
**Dependencies:** TASK-005, TASK-014
**Complexity:** Medium

#### Context
On the Dashboard, tapping the **Office** or **Remote** check-in button writes the record to the DB but produces no visible UI change — no stats update, no selected state, no compliance-ring change. See `BUGS.md` BUG-010 for the full report.

**Useful observation from `MainFlowSmokeTest`:** the `d02_calendarDayDetailSheetOpens` test successfully finds today's cell with a content description after a Dashboard check-in — meaning the use-case write does succeed and the record IS persisted. So the failure is not "click does nothing"; it's that the Dashboard UI itself doesn't re-emit / re-render after the write. The root cause is one of:

1. The repo `Flow` powering `DashboardViewModel.state` is not a Room `Flow<...>` (e.g. it's a one-shot `suspend fun` collected once), so the new record never reaches the VM.
2. The button's visual selected state is not bound to today's `DayRecord` in `QuickCheckInButton.kt`.

#### Scope — Files to Investigate (then Modify as needed)
- `core/data/repository/DayRecordRepositoryImpl.kt` — verify the read path returns Room `Flow`.
- `feature/dashboard/DashboardViewModel.kt` — verify `state` is built from that Flow with `stateIn(...)`.
- `feature/dashboard/ui/QuickCheckInButton.kt` — verify the visual selected state binds to today's record type.
- `feature/dashboard/ui/DashboardScreen.kt` — verify the stats strip + ring re-render on state change.

#### Implementation Details
1. Confirm the read path is reactive — `DayRecordDao.observe...(): Flow<...>`, mapped to domain models, consumed by the VM via `stateIn`. If it's a one-shot suspend, change to a Flow-returning query.
2. If the click handler succeeds but no Flow emission lands, add a temporary `Log.d` in the VM's state collector and in the use case to pinpoint where the data stops flowing; remove the logs before commit.
3. Once the Flow is reactive, wire the `QuickCheckInButton` visual state so the active button (Office vs Remote) reflects today's `DayRecord.status`. Today both variants paint the same regardless of today's record.
4. Wrap the use-case call in `runCatching` inside the VM so any future use-case failure surfaces as a user-visible Snackbar rather than silently swallowed by `viewModelScope.launch`.

#### Acceptance Criteria
- [ ] Tapping Office on a clean day immediately updates the stats-strip Office count and the compliance ring.
- [ ] Tapping Remote on a clean day does the equivalent for REMOTE.
- [ ] The Office and Remote buttons visually indicate which is selected for today after a tap.
- [ ] If a use-case throws, a user-visible Snackbar appears (no silent swallowing).
- [ ] Unit test on `DashboardViewModel` covering both check-in paths, asserting the state Flow emits a new value after each call.
- [ ] Existing `MainFlowSmokeTest.b01_...` and `.b02_...` continue to pass.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.dashboard.DashboardViewModelTest"
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.carvalhorr.daysInOffice.smoke.ui.MainFlowSmokeTest
```

---

### TASK-030: Fix BUG-011 — Wi-Fi and Geofence pickers crash via `@HiltViewModel` factory misuse
**Status:** NOT_STARTED
**Dependencies:** TASK-016, TASK-026
**Complexity:** Simple

#### Context
Opening any of Settings → Wi-Fi (connected), Settings → Wi-Fi (scan only), or Settings → Geofencing, then toggling the row's Enable switch, crashes the app with:

```
java.lang.RuntimeException: Cannot create an instance of class
  com.carvalhorr.daysInOffice.feature.shared.ui.WifiSsidPickerViewModel
  (or .GeofencePickerViewModel)
  at androidx.lifecycle.viewmodel.internal.JvmViewModelProviders.createViewModel
```

See `BUGS.md` BUG-011. The `MainFlowSmokeTest.e02_*`, `.e03_*`, `.e04_*` tests capture this exact stack and are the regression guards.

**Confirmed root cause (one bug, two files):**
- `feature/shared/ui/WifiSsidPicker.kt:73` uses `viewModel: WifiSsidPickerViewModel = viewModel()`.
- `feature/shared/ui/GeofencePicker.kt:75` uses `viewModel: GeofencePickerViewModel = viewModel()`.

Both VMs are declared `@HiltViewModel` with `@Inject` constructors needing injected scanners/providers. The plain `viewModel()` factory from `androidx.lifecycle.viewmodel.compose` doesn't know about Hilt and falls back to the default factory, which requires a no-arg constructor.

#### Scope — Files to Modify
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/shared/ui/WifiSsidPicker.kt`
- `app/src/main/kotlin/com/carvalhorr/daysInOffice/feature/shared/ui/GeofencePicker.kt`
- `CLAUDE.md` (root + run-repo copy) — remove `e02_*`, `e03_*`, `e04_*` rows from "Known-Failing UI Smoke Tests".

#### Implementation Details
In each picker file, swap the factory:

```kotlin
// Remove:
import androidx.lifecycle.viewmodel.compose.viewModel
// Add:
import androidx.hilt.navigation.compose.hiltViewModel

// In the composable signature, change:
viewModel: WifiSsidPickerViewModel = viewModel()
// To:
viewModel: WifiSsidPickerViewModel = hiltViewModel()
```

Same change in `GeofencePicker.kt` with `GeofencePickerViewModel`.

Then audit the project for any other place a `@HiltViewModel` is instantiated via plain `viewModel()`:

```bash
grep -rn "= viewModel()" --include="*.kt" app/src/main/
```

For each match where the target VM is `@HiltViewModel`, switch to `hiltViewModel()`. This prevents the same bug appearing elsewhere.

#### Acceptance Criteria
- [ ] Settings → Wi-Fi (connected) → toggle Enable opens the picker without crashing.
- [ ] Settings → Wi-Fi (scan only) → toggle Enable opens the picker without crashing.
- [ ] Settings → Geofencing → toggle Enable opens the picker without crashing.
- [ ] Project-wide grep confirms no other `@HiltViewModel` is instantiated via plain `viewModel()`.
- [ ] `MainFlowSmokeTest.e02_*`, `.e03_*`, `.e04_*` all pass and are removed from the `Known-Failing UI Smoke Tests` table in CLAUDE.md.

#### QA Verification Steps
```bash
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.carvalhorr.daysInOffice.smoke.ui.MainFlowSmokeTest
# Expected: e02_*, e03_*, e04_* all PASS.
```

---

### TASK-031: Fix BUG-012 — geofence coordinates do not persist between sessions
**Status:** NOT_STARTED
**Dependencies:** TASK-026
**Complexity:** Simple

#### Context
Setting geofence lat/lng/radius in Settings → Geofencing and saving does not retain the values across sessions. On re-opening the app the sheet's lat/lng/radius are blank, so the user cannot test geofence detection on a physical device. See `BUGS.md` BUG-012 for the report.

Likely one of:
- The write path does not call DataStore for all three numeric fields (e.g. only the enabled flag persists; or `null` parameters are skipped).
- The read path does not seed `DetectionConfig.geofenceLatitude / geofenceLongitude / geofenceRadiusMeters` from DataStore on app start, so the sheet's `currentLat / currentLng / currentRadius` props arrive null.
- The write payload uses wrong field names or swallowed parse exceptions.

#### Scope — Files to Investigate / Modify
- `feature/settings/SettingsViewModel.kt` — `updateGeofence(enabled, lat, lng, radius)` and the upstream `state` Flow.
- `core/data/repository/DetectionConfigRepositoryImpl.kt` (or equivalent) — DataStore read/write of the three fields.
- `core/data/datasource/DetectionConfigDataSource.kt` (if it exists) — DataStore key definitions.
- `feature/settings/ui/sheets/GeofenceSheet.kt` — confirm `currentLat / currentLng / currentRadius` flow in from props, not from local defaults.

#### Implementation Details
1. Verify write: invoking `updateGeofence(enabled=true, lat=37.7749, lng=-122.4194, radius=100)` results in all four values landing in DataStore (dump via `adb shell run-as <pkg> cat .../datastore/...` if needed).
2. Verify read: `getDetectionConfig(): Flow<DetectionConfig>` emits the same lat/lng/radius after restart. Mismatched keys between writer and reader are a common culprit — confirm a single set of `PreferencesKey` constants is shared.
3. Add a unit test on the repository round-trip: write all three fields, read them back, assert equality.

#### Acceptance Criteria
- [ ] Set geofence in Settings → Save → kill and re-launch the app → reopen Settings → Geofencing — lat/lng/radius are populated with the saved values.
- [ ] Unit test `DetectionConfigRepositoryImplTest.geofenceRoundTrip` writes and reads back all three fields.
- [ ] On second launch, `DetectionOrchestrator`'s geofence detector uses the persisted values (verified via log or a second unit test on the orchestrator).

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.data.repository.*"
./gradlew :app:assembleDebug
```

---

### TASK-032: Fix BUG-013 — Wi-Fi scan does not list any networks
**Status:** NOT_STARTED
**Dependencies:** TASK-026
**Complexity:** Medium

#### Context
In Settings → Wi-Fi (scan only) → Enable → tap "Scan for networks", no list of SSIDs appears. Same likely applies to the Wi-Fi (connected) sheet's picker. See `BUGS.md` BUG-013.

Most likely causes (one or more):
1. Missing `NEARBY_WIFI_DEVICES` permission on Android 13+ — without it, `WifiManager.startScan()` succeeds with an empty list.
2. Android's scan throttle (4 calls / 120s in foreground) returns false from `startScan()` after repeated taps — silently treated as an empty result by the UI.
3. The empty-state render ("No networks detected.") is too subtle and the user reads it as "nothing happened".

#### Scope — Files to Investigate / Modify
- `app/src/main/AndroidManifest.xml` — confirm `CHANGE_WIFI_STATE`, `ACCESS_WIFI_STATE`, `NEARBY_WIFI_DEVICES` (with `tools:targetApi="33"` and `usesPermissionFlags="neverForLocation"` per Android 13 requirements).
- `core/data/datasource/WifiScanner.kt` — scan invocation, throttle detection, result mapping.
- `feature/shared/ui/WifiSsidPicker.kt` — request `NEARBY_WIFI_DEVICES` alongside `FINE_LOCATION` on API 33+; render error/empty states more clearly.
- `core/permissions/AppPermission.kt` — add `NEARBY_WIFI_DEVICES` if missing.

#### Implementation Details
1. Add the manifest permission (with the `neverForLocation` flag to avoid the location-permission requirement on Android 13+).
2. Update `permissionRequester.request(...)` in the picker to include `NEARBY_WIFI_DEVICES` on API 33+, falling back to `ACCESS_FINE_LOCATION` on earlier versions.
3. In `WifiScanner.scanForSsids()`, detect `startScan()` returning false and surface as `Result.failure(ScanThrottledException("Try again in a moment"))` (or similar). Detect SecurityException as `Result.failure(PermissionMissingException(...))`.
4. In `WifiSsidPicker`, render error states with an explicit message and a "Retry" button. The empty-state copy should remain but be visually distinct (e.g. larger font, retry affordance).
5. Add a unit test for `WifiScanner` with a mocked `WifiManager`: cover success, empty, throttled, no-permission cases.

#### Acceptance Criteria
- [ ] On a real device with Wi-Fi enabled and at least one SSID in range, tapping Scan produces a tappable list.
- [ ] If scan is throttled, the user sees "Try again in a moment" rather than an empty list.
- [ ] If permissions are denied, the user sees a clear message and the manual SSID input remains usable.
- [ ] `WifiScannerTest` covers all four state branches.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.core.data.datasource.WifiScannerTest"
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

---

### TASK-033: Remove BUG-014 — delete unspecified "Export CSV" feature from Settings → Data
**Status:** NOT_STARTED
**Dependencies:** TASK-016
**Complexity:** Simple

#### Context
Settings → Data → "Export CSV" (or similar) is not in the product spec and confuses the layout. Remove it. See `BUGS.md` BUG-014. Pure deletion; no replacement.

#### Scope — Files to Modify
- `feature/settings/ui/SettingsScreen.kt` — remove the Export row from `DataSection`.
- `feature/settings/ui/sheets/` — remove any Export-related bottom sheet (e.g. `ExportSheet.kt`) if exclusively used here.
- `feature/settings/SettingsViewModel.kt` — remove the `exportData()` (or similar) function and its imports.
- `core/domain/usecase/ExportDataUseCase.kt` (if it exists) — remove file and its tests.
- `core/data/repository/...` — if a repository method is used exclusively by the removed use case, remove. Otherwise leave (harmless).
- `prototype/index.html` — remove the corresponding entry to keep the prototype aligned with the emulator (per the "prototype is visual source of truth" rule).

#### Implementation Details
Pure deletion. Confirm no compile errors after removal. Leave `DayRecordDao` read queries alone — they're harmless.

#### Acceptance Criteria
- [ ] "Export" / "Export CSV" row no longer appears in Settings.
- [ ] `./gradlew :app:assembleDebug` clean.
- [ ] `./gradlew testDebugUnitTest` clean.
- [ ] `./gradlew lintDebug` does not surface new warnings.
- [ ] `prototype/index.html` no longer shows the Export entry.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

---

### TASK-034: Fix BUG-015 — rename and re-section "Reset onboarding"
**Status:** NOT_STARTED
**Dependencies:** TASK-016, TASK-033
**Complexity:** Simple

#### Context
The "🔁 Reset onboarding" row sits in Settings → Data with a label that sounds destructive ("will it wipe my data?"). It's grouped with the (now removed) Export feature, which makes it read as a data operation. It isn't — it's a re-run-the-setup-wizard action. See `BUGS.md` BUG-015.

#### Scope — Files to Modify
- `feature/settings/ui/SettingsScreen.kt` — move the row from `DataSection` to a new `SetupSection`; relabel.
- `prototype/index.html` — mirror the section + label change.

#### Implementation Details
1. Create a new "Setup" section in `SettingsScreen` (between Calendar and Data, or wherever fits the flow).
2. Move the Reset row from `DataSection` into the new `SetupSection`.
3. Rename label to **"Re-run setup wizard"** with sub-text **"Walks you through the initial setup again. Your data is kept."** — wire the sub-text into the existing `SettingsRow` (extend the component if needed, or use a custom row variant).
4. Optional but recommended: wrap `onResetOnboarding` in an AlertDialog confirmation ("This will return you to the setup wizard. Your data is kept. Continue?") to remove any remaining doubt.
5. Keep the existing `SettingsViewModel.resetOnboarding()` behavior unchanged — it should still flip `onboardingComplete = false` only.

#### Acceptance Criteria
- [ ] "Re-run setup wizard" appears in its own "Setup" section, with the descriptive sub-text.
- [ ] No mention of "reset" / "data" in the row.
- [ ] Tapping the row (and confirming, if dialog added) returns the user to the onboarding wizard on next launch; no `DayRecord` rows are deleted.
- [ ] Prototype matches.

#### QA Verification Steps
```bash
./gradlew testDebugUnitTest --tests "com.carvalhorr.daysInOffice.feature.settings.*"
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

---

## Phase 5: Release Validation

### TASK-021: Release Smoke Test Suite
**Status:** NOT_STARTED
**Dependencies:** TASK-020 (transitively all prior implementation tasks)
**Complexity:** Medium

#### Context
Per-task QA in this experiment runs on the JVM (unit tests + Robolectric) — fast and deterministic, but does not exercise the real Android runtime. This task adds a small **release smoke suite** of instrumented tests, executed on an emulator booted automatically by `experiment/scripts/with_emulator.sh`. It is the single release gate that proves the app actually works end-to-end on-device before shipping.

The smoke suite is intentionally narrow: a few happy paths through the system-service-dependent code (WorkManager, Room on-device, manual office-day recording, compliance recalculation). It is **not** a full UI suite — Compose UI behaviour is covered by Compose UI Test in earlier tasks.

#### Scope — Files to Create
- `app/src/androidTest/kotlin/com/carvalhorr/daysInOffice/smoke/DatabaseSmokeTest.kt`
- `app/src/androidTest/kotlin/com/carvalhorr/daysInOffice/smoke/ManualDetectionSmokeTest.kt`
- `app/src/androidTest/kotlin/com/carvalhorr/daysInOffice/smoke/ComplianceSmokeTest.kt`
- `app/src/androidTest/kotlin/com/carvalhorr/daysInOffice/smoke/WorkerSchedulingSmokeTest.kt`

#### Implementation Details
1. All four tests run under `androidx.test.runner.AndroidJUnitRunner` with `@RunWith(AndroidJUnit4::class)`. Test package is `com.carvalhorr.daysInOffice.smoke`.
2. `DatabaseSmokeTest`:
   - Builds `AppDatabase` against the **real on-device SQLite** (`Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, "smoke.db")`).
   - Inserts a `DayRecordEntity`, reads it back, asserts equality. Deletes the file afterward.
3. `ManualDetectionSmokeTest`:
   - Resolves `RecordOfficeDayUseCase` via a Hilt test rule (`@HiltAndroidTest`).
   - Calls it with `DetectionMethod.MANUAL` for today's date.
   - Asserts `DayRecordRepository.getByDate(today)` emits a record with `confirmedByUser = true`.
4. `ComplianceSmokeTest`:
   - Seeds `MandateConfig` (50% mandate, Mon–Fri) and three `DayRecord`s spanning a calendar week.
   - Invokes `CalculateComplianceUseCase`.
   - Asserts `ComplianceResult.daysNeededToComply` is correct and non-negative.
5. `WorkerSchedulingSmokeTest`:
   - Uses `WorkManagerTestInitHelper` to drive a synchronous WorkManager.
   - Enqueues `DayDetectionWorker`, advances time, asserts it ran to completion (Result.success).

#### Acceptance Criteria
- [ ] All four smoke tests live in `app/src/androidTest/kotlin/com/carvalhorr/daysInOffice/smoke/`.
- [ ] Each test class is annotated `@RunWith(AndroidJUnit4::class)`.
- [ ] `experiment/scripts/setup_emulator.sh` has been run on the host (one-time bootstrap; AVD `exp_avd` exists with a Quick Boot snapshot).
- [ ] The QA command (below) passes all four tests with `0 failures, 0 errors`.
- [ ] No smoke test references mocks for Room or WorkManager — the point is real-runtime coverage.
- [ ] `app/build/reports/androidTests/connected/index.html` exists after the run.

#### QA Verification Steps
> Prerequisite (one-time host bootstrap, run by the operator before this task):
> `experiment/scripts/setup_emulator.sh` — creates the `exp_avd` AVD and seeds a Quick Boot snapshot.
> `with_emulator.sh` will fail loudly if the AVD is missing.

```bash
../../../experiment/scripts/with_emulator.sh ./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.carvalhorr.daysInOffice.smoke
```

