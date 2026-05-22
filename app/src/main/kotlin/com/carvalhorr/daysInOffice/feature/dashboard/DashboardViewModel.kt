package com.carvalhorr.daysInOffice.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carvalhorr.daysInOffice.core.domain.model.ComplianceResult
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import com.carvalhorr.daysInOffice.core.domain.usecase.GetComplianceUseCase
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordOfficeDayUseCase
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordRemoteDayUseCase
import com.carvalhorr.daysInOffice.core.domain.usecase.SyncCalendarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data object Empty : DashboardUiState()
    data class Success(
        val complianceResult: ComplianceResult,
        val mandatePeriod: MandatePeriod,
        val todayRecord: DayRecord?,
        val isSyncing: Boolean = false
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getComplianceUseCase: GetComplianceUseCase,
    private val recordOfficeDayUseCase: RecordOfficeDayUseCase,
    private val recordRemoteDayUseCase: RecordRemoteDayUseCase,
    private val syncCalendarUseCase: SyncCalendarUseCase,
    private val dayRecordRepository: DayRecordRepository,
    private val mandateConfigRepository: MandateConfigRepository
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _retrySignal = MutableStateFlow(0)
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val state: StateFlow<DashboardUiState> = _retrySignal.flatMapLatest {
        combine(
            getComplianceUseCase(),
            mandateConfigRepository.getMandateConfig(),
            dayRecordRepository.getDayRecord(LocalDate.now()),
            _isSyncing
        ) { compliance, config, todayRecord, isSyncing ->
            val result: DashboardUiState = if (
                compliance.officeDays + compliance.remoteDays + compliance.unknownDays == 0
            ) {
                DashboardUiState.Empty
            } else {
                DashboardUiState.Success(
                    complianceResult = compliance,
                    mandatePeriod = config.period,
                    todayRecord = todayRecord,
                    isSyncing = isSyncing
                )
            }
            result
        }
        .catch { e -> emit(DashboardUiState.Error(e.message ?: "Unknown error")) }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState.Loading
        )

    fun checkInAsOffice() {
        viewModelScope.launch {
            runCatching {
                recordOfficeDayUseCase(LocalDate.now(), DetectionMethod.MANUAL)
            }.onFailure { e ->
                _snackbarMessage.emit(e.message ?: "Check-in failed")
            }
        }
    }

    fun confirmOfficeFromDetection() {
        viewModelScope.launch {
            runCatching {
                recordOfficeDayUseCase(LocalDate.now(), DetectionMethod.MANUAL_CONFIRMED_FROM_DETECTION)
            }.onFailure { e ->
                _snackbarMessage.emit(e.message ?: "Check-in failed")
            }
        }
    }

    fun dismissDetectionPrompt(method: DetectionMethod?) {
        if (method == null) return
        viewModelScope.launch {
            runCatching {
                mandateConfigRepository.markDetectorDismissed(method, LocalDate.now())
            }
        }
    }

    fun checkInAsRemote() {
        viewModelScope.launch {
            runCatching {
                recordRemoteDayUseCase(LocalDate.now())
            }.onFailure { e ->
                _snackbarMessage.emit(e.message ?: "Check-in failed")
            }
        }
    }

    fun syncCalendar() {
        viewModelScope.launch {
            _isSyncing.value = true
            syncCalendarUseCase()
            _isSyncing.value = false
        }
    }

    fun retry() {
        _retrySignal.value++
    }
}
