package com.carvalhorr.daysInOffice.core.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val MANDATE_PERCENTAGE = floatPreferencesKey("mandate_percentage")
        private val MANDATE_PERIOD = stringPreferencesKey("mandate_period")
        private val FISCAL_YEAR_START_MONTH = intPreferencesKey("fiscal_year_start_month")
        private val WORKING_DAYS = stringPreferencesKey("working_days")
        private val DETECTION_METHODS = stringPreferencesKey("detection_methods")
        private val WIFI_SSID = stringPreferencesKey("wifi_ssid")
        private val GEOFENCE_LAT = floatPreferencesKey("geofence_lat")
        private val GEOFENCE_LNG = floatPreferencesKey("geofence_lng")
        private val GEOFENCE_RADIUS = floatPreferencesKey("geofence_radius")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val CALENDAR_SYNC_ENABLED = booleanPreferencesKey("calendar_sync_enabled")
        val GEOFENCE_INSIDE = booleanPreferencesKey("geofence_inside")
        private val DETECTOR_DISMISSALS = stringPreferencesKey("detector_dismissals")

        private val DEFAULT_WORKING_DAYS = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
    }

    val mandatePercentage: Flow<Float> = dataStore.data.map { prefs ->
        prefs[MANDATE_PERCENTAGE] ?: 0.5f
    }

    val mandatePeriod: Flow<MandatePeriod> = dataStore.data.map { prefs ->
        prefs[MANDATE_PERIOD]?.let { MandatePeriod.valueOf(it) } ?: MandatePeriod.MONTHLY
    }

    val fiscalYearStartMonth: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[FISCAL_YEAR_START_MONTH] ?: 1).coerceIn(1, 12)
    }

    val workingDays: Flow<Set<DayOfWeek>> = dataStore.data.map { prefs ->
        prefs[WORKING_DAYS]?.let { json ->
            Json.parseToJsonElement(json).jsonArray
                .map { DayOfWeek.valueOf(it.jsonPrimitive.content) }.toSet()
        } ?: DEFAULT_WORKING_DAYS
    }

    val detectionMethods: Flow<Set<DetectionMethod>> = dataStore.data.map { prefs ->
        prefs[DETECTION_METHODS]?.let { json ->
            Json.parseToJsonElement(json).jsonArray
                .map { DetectionMethod.valueOf(it.jsonPrimitive.content) }.toSet()
        } ?: emptySet()
    }

    val wifiSsid: Flow<String?> = dataStore.data.map { prefs ->
        prefs[WIFI_SSID]
    }

    val geofenceLat: Flow<Double?> = dataStore.data.map { prefs ->
        prefs[GEOFENCE_LAT]?.toDouble()
    }

    val geofenceLng: Flow<Double?> = dataStore.data.map { prefs ->
        prefs[GEOFENCE_LNG]?.toDouble()
    }

    val geofenceRadius: Flow<Float?> = dataStore.data.map { prefs ->
        prefs[GEOFENCE_RADIUS]
    }

    val onboardingComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETE] ?: false
    }

    val calendarSyncEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CALENDAR_SYNC_ENABLED] ?: false
    }

    val geofenceInside: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[GEOFENCE_INSIDE] ?: false
    }

    suspend fun saveMandatePercentage(value: Float) {
        dataStore.edit { prefs -> prefs[MANDATE_PERCENTAGE] = value }
    }

    suspend fun saveMandatePeriod(value: MandatePeriod) {
        dataStore.edit { prefs -> prefs[MANDATE_PERIOD] = value.name }
    }

    suspend fun saveFiscalYearStartMonth(value: Int) {
        dataStore.edit { prefs -> prefs[FISCAL_YEAR_START_MONTH] = value.coerceIn(1, 12) }
    }

    suspend fun saveWorkingDays(value: Set<DayOfWeek>) {
        val json = JsonArray(value.map { JsonPrimitive(it.name) }).toString()
        dataStore.edit { prefs -> prefs[WORKING_DAYS] = json }
    }

    suspend fun saveDetectionMethods(value: Set<DetectionMethod>) {
        val json = JsonArray(value.map { JsonPrimitive(it.name) }).toString()
        dataStore.edit { prefs -> prefs[DETECTION_METHODS] = json }
    }

    suspend fun saveWifiSsid(value: String?) {
        dataStore.edit { prefs ->
            if (value != null) prefs[WIFI_SSID] = value else prefs.remove(WIFI_SSID)
        }
    }

    suspend fun saveGeofenceLat(value: Double?) {
        dataStore.edit { prefs ->
            if (value != null) prefs[GEOFENCE_LAT] = value.toFloat() else prefs.remove(GEOFENCE_LAT)
        }
    }

    suspend fun saveGeofenceLng(value: Double?) {
        dataStore.edit { prefs ->
            if (value != null) prefs[GEOFENCE_LNG] = value.toFloat() else prefs.remove(GEOFENCE_LNG)
        }
    }

    suspend fun saveGeofenceRadius(value: Float?) {
        dataStore.edit { prefs ->
            if (value != null) prefs[GEOFENCE_RADIUS] = value else prefs.remove(GEOFENCE_RADIUS)
        }
    }

    suspend fun saveOnboardingComplete(value: Boolean) {
        dataStore.edit { prefs -> prefs[ONBOARDING_COMPLETE] = value }
    }

    suspend fun saveCalendarSyncEnabled(value: Boolean) {
        dataStore.edit { prefs -> prefs[CALENDAR_SYNC_ENABLED] = value }
    }

    suspend fun saveGeofenceInside(value: Boolean) {
        dataStore.edit { prefs -> prefs[GEOFENCE_INSIDE] = value }
    }

    suspend fun isDetectorDismissedToday(method: DetectionMethod, date: LocalDate): Boolean {
        val prefs = dataStore.data.first()
        val json = prefs[DETECTOR_DISMISSALS] ?: return false
        return try {
            val map = Json.parseToJsonElement(json).jsonObject
            map[method.name]?.jsonPrimitive?.contentOrNull == date.toString()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markDetectorDismissed(method: DetectionMethod, date: LocalDate) {
        dataStore.edit { prefs ->
            val current = prefs[DETECTOR_DISMISSALS]
            val existing = if (current != null) {
                try {
                    Json.parseToJsonElement(current).jsonObject
                        .entries.associate { (k, v) -> k to v }
                } catch (e: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
            val updated = buildJsonObject {
                existing.forEach { (k, v) -> put(k, v) }
                put(method.name, JsonPrimitive(date.toString()))
            }
            prefs[DETECTOR_DISMISSALS] = updated.toString()
        }
    }
}
