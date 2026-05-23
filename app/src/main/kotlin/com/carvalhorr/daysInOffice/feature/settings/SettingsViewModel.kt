package com.carvalhorr.daysInOffice.feature.settings

import android.Manifest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.carvalhorr.daysInOffice.core.detection.detector.GeofenceDetector
import com.carvalhorr.daysInOffice.core.detection.worker.DayDetectionWorker
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandateConfig
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import com.carvalhorr.daysInOffice.core.domain.usecase.SyncCalendarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

private const val TAG = "Detection"

sealed class SettingsUiState {
    data object Loading : SettingsUiState()
    data class Success(
        val mandateConfig: MandateConfig,
        val detectionConfig: DetectionConfig,
        val calendarSyncEnabled: Boolean,
        val isSyncing: Boolean = false,
        val syncResult: String? = null
    ) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

sealed class SettingsNavigationEvent {
    data object NavigateToOnboarding : SettingsNavigationEvent()
}

sealed class OneShotResult {
    data object NotificationFired : OneShotResult()
    data object NoSignal : OneShotResult()
    data class PermissionMissing(val permission: String) : OneShotResult()
    data class Error(val message: String) : OneShotResult()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val mandateConfigRepository: MandateConfigRepository,
    private val syncCalendarUseCase: SyncCalendarUseCase,
    private val geofenceDetector: GeofenceDetector,
    private val workManager: WorkManager
) : ViewModel() {

    private val _mandateConfig = MutableStateFlow(
        MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.MONTHLY,
            workingDays = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )
        )
    )
    private val _detectionConfig = MutableStateFlow(
        DetectionConfig(
            enabledMethods = emptySet(),
            wifiSsid = null,
            geofenceLatitude = null,
            geofenceLongitude = null,
            geofenceRadiusMeters = null
        )
    )
    private val _calendarSyncEnabled = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)
    private val _syncResult = MutableStateFlow<String?>(null)
    private val _navigationEvent = MutableStateFlow<SettingsNavigationEvent?>(null)
    private val _oneShotResult = MutableSharedFlow<OneShotResult>(extraBufferCapacity = 1)
    val oneShotResult: SharedFlow<OneShotResult> = _oneShotResult.asSharedFlow()

    val state: StateFlow<SettingsUiState> = combine(
        _mandateConfig,
        _detectionConfig,
        _calendarSyncEnabled,
        _isSyncing,
        _syncResult
    ) { mandateConfig, detectionConfig, calendarSyncEnabled, isSyncing, syncResult ->
        val result: SettingsUiState = SettingsUiState.Success(
            mandateConfig = mandateConfig,
            detectionConfig = detectionConfig,
            calendarSyncEnabled = calendarSyncEnabled,
            isSyncing = isSyncing,
            syncResult = syncResult
        )
        result
    }
        .catch { e -> emit(SettingsUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState.Loading
        )

    val navigationEvent: StateFlow<SettingsNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        viewModelScope.launch {
            mandateConfigRepository.getMandateConfig().collect { _mandateConfig.value = it }
        }
        viewModelScope.launch {
            mandateConfigRepository.getDetectionConfig().collect { _detectionConfig.value = it }
        }
        viewModelScope.launch {
            mandateConfigRepository.getCalendarSyncEnabled().collect { _calendarSyncEnabled.value = it }
        }
    }

    fun updateTargetPercentage(percentage: Float) {
        viewModelScope.launch {
            val newConfig = _mandateConfig.value.copy(targetPercentage = percentage)
            _mandateConfig.value = newConfig
            mandateConfigRepository.saveMandateConfig(newConfig)
        }
    }

    fun updatePeriod(period: MandatePeriod) {
        viewModelScope.launch {
            val newConfig = _mandateConfig.value.copy(period = period)
            _mandateConfig.value = newConfig
            mandateConfigRepository.saveMandateConfig(newConfig)
        }
    }

    fun updateWorkingDays(days: Set<DayOfWeek>) {
        viewModelScope.launch {
            val newConfig = _mandateConfig.value.copy(workingDays = days)
            _mandateConfig.value = newConfig
            mandateConfigRepository.saveMandateConfig(newConfig)
        }
    }

    fun updateFiscalYearStartMonth(month: Int) {
        viewModelScope.launch {
            val newConfig = _mandateConfig.value.copy(fiscalYearStartMonth = month.coerceIn(1, 12))
            _mandateConfig.value = newConfig
            mandateConfigRepository.saveMandateConfig(newConfig)
        }
    }

    fun updateWifiConnected(enabled: Boolean, ssid: String?) {
        viewModelScope.launch {
            val current = _detectionConfig.value
            val methods = current.enabledMethods.toMutableSet()
            if (enabled) methods.add(DetectionMethod.WIFI_CONNECTED)
            else methods.remove(DetectionMethod.WIFI_CONNECTED)
            val newConfig = current.copy(enabledMethods = methods, wifiSsid = ssid?.ifBlank { null })
            _detectionConfig.value = newConfig
            mandateConfigRepository.saveDetectionConfig(newConfig)
            if (enabled) enqueueOneShotDetection("Wi-Fi Connected")
        }
    }

    fun updateWifiScan(enabled: Boolean, ssid: String?) {
        viewModelScope.launch {
            val current = _detectionConfig.value
            val methods = current.enabledMethods.toMutableSet()
            if (enabled) methods.add(DetectionMethod.WIFI_SCAN)
            else methods.remove(DetectionMethod.WIFI_SCAN)
            val newConfig = current.copy(enabledMethods = methods, wifiSsid = ssid?.ifBlank { null })
            _detectionConfig.value = newConfig
            mandateConfigRepository.saveDetectionConfig(newConfig)
            if (enabled) enqueueOneShotDetection("Wi-Fi Scan")
        }
    }

    fun updateGeofence(enabled: Boolean, lat: Double?, lng: Double?, radius: Float?) {
        viewModelScope.launch {
            val current = _detectionConfig.value
            val methods = current.enabledMethods.toMutableSet()
            if (enabled) methods.add(DetectionMethod.GEOFENCE)
            else methods.remove(DetectionMethod.GEOFENCE)
            val newConfig = current.copy(
                enabledMethods = methods,
                geofenceLatitude = lat,
                geofenceLongitude = lng,
                geofenceRadiusMeters = radius
            )
            _detectionConfig.value = newConfig
            mandateConfigRepository.saveDetectionConfig(newConfig)
            if (enabled && lat != null && lng != null && radius != null) {
                geofenceDetector.setupGeofence(lat, lng, radius)
            } else {
                geofenceDetector.removeGeofence()
            }
            if (enabled) enqueueOneShotDetection("Geofence")
        }
    }

    private fun enqueueOneShotDetection(methodLabel: String) {
        val request = OneTimeWorkRequestBuilder<DayDetectionWorker>()
            .setInputData(workDataOf("force_run" to true))
            .build()
        workManager.enqueue(request)
        Log.i(TAG, "SettingsViewModel.enqueueOneShotDetection: enqueued one-shot for $methodLabel")
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(request.id)
                .filter { it?.state?.isFinished == true }
                .take(1)
                .collect { workInfo ->
                    val outcome = workInfo?.outputData?.getString("outcome") ?: "NO_SIGNAL"
                    _oneShotResult.emit(when (outcome) {
                        "NOTIFICATION_FIRED" -> OneShotResult.NotificationFired
                        "PERMISSION_DENIED" -> OneShotResult.PermissionMissing(Manifest.permission.POST_NOTIFICATIONS)
                        else -> OneShotResult.NoSignal
                    })
                }
        }
    }

    fun updateCalendarSync(enabled: Boolean) {
        viewModelScope.launch {
            _calendarSyncEnabled.value = enabled
            mandateConfigRepository.saveCalendarSyncEnabled(enabled)
        }
    }

    fun syncCalendar() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncResult.value = null
            val result = syncCalendarUseCase()
            _isSyncing.value = false
            _syncResult.value = result.fold(
                onSuccess = { count -> "Synced $count event${if (count == 1) "" else "s"}" },
                onFailure = { "Sync failed" }
            )
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            mandateConfigRepository.saveOnboardingComplete(false)
            _navigationEvent.value = SettingsNavigationEvent.NavigateToOnboarding
        }
    }

    fun onNavigationHandled() {
        _navigationEvent.value = null
    }
}
