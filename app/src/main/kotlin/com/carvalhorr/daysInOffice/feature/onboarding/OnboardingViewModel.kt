package com.carvalhorr.daysInOffice.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandateConfig
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val mandateConfig: MandateConfig = MandateConfig(
        targetPercentage = 0.5f,
        period = MandatePeriod.MONTHLY,
        workingDays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
    ),
    val detectionConfig: DetectionConfig = DetectionConfig(
        enabledMethods = emptySet(),
        wifiSsid = null,
        geofenceLatitude = null,
        geofenceLongitude = null,
        geofenceRadiusMeters = null
    ),
    val calendarSyncEnabled: Boolean = false,
    val navigationEvent: OnboardingNavigationEvent? = null
)

sealed class OnboardingNavigationEvent {
    data object NavigateToDashboard : OnboardingNavigationEvent()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val mandateConfigRepository: MandateConfigRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    companion object {
        const val TOTAL_STEPS = 4
    }

    fun next() {
        _state.update { s ->
            if (s.currentStep < TOTAL_STEPS - 1) s.copy(currentStep = s.currentStep + 1) else s
        }
    }

    fun back() {
        _state.update { s ->
            if (s.currentStep > 0) s.copy(currentStep = s.currentStep - 1) else s
        }
    }

    fun updateMandatePercentage(percentage: Float) {
        _state.update { s -> s.copy(mandateConfig = s.mandateConfig.copy(targetPercentage = percentage)) }
    }

    fun updateWorkingDays(days: Set<DayOfWeek>) {
        _state.update { s -> s.copy(mandateConfig = s.mandateConfig.copy(workingDays = days)) }
    }

    fun updatePeriod(period: MandatePeriod) {
        _state.update { s -> s.copy(mandateConfig = s.mandateConfig.copy(period = period)) }
    }

    fun updateFiscalYearStartMonth(month: Int) {
        _state.update { s ->
            s.copy(mandateConfig = s.mandateConfig.copy(fiscalYearStartMonth = month.coerceIn(1, 12)))
        }
    }

    fun toggleDetectionMethod(method: DetectionMethod) {
        _state.update { s ->
            val methods = s.detectionConfig.enabledMethods.toMutableSet()
            if (methods.contains(method)) methods.remove(method) else methods.add(method)
            s.copy(detectionConfig = s.detectionConfig.copy(enabledMethods = methods))
        }
    }

    fun updateWifiSsid(ssid: String) {
        _state.update { s ->
            s.copy(detectionConfig = s.detectionConfig.copy(wifiSsid = ssid.ifBlank { null }))
        }
    }

    fun updateGeofence(lat: Double, lng: Double, radius: Float) {
        _state.update { s ->
            s.copy(
                detectionConfig = s.detectionConfig.copy(
                    geofenceLatitude = lat,
                    geofenceLongitude = lng,
                    geofenceRadiusMeters = radius
                )
            )
        }
    }

    fun updateCalendarSync(enabled: Boolean) {
        _state.update { s -> s.copy(calendarSyncEnabled = enabled) }
    }

    fun complete() {
        viewModelScope.launch {
            val s = _state.value
            mandateConfigRepository.saveMandateConfig(s.mandateConfig)
            mandateConfigRepository.saveDetectionConfig(s.detectionConfig)
            mandateConfigRepository.saveCalendarSyncEnabled(s.calendarSyncEnabled)
            mandateConfigRepository.saveOnboardingComplete(true)
            _state.update { it.copy(navigationEvent = OnboardingNavigationEvent.NavigateToDashboard) }
        }
    }

    fun onNavigationHandled() {
        _state.update { it.copy(navigationEvent = null) }
    }
}
