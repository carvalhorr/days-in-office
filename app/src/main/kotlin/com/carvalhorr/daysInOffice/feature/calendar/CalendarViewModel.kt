package com.carvalhorr.daysInOffice.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import com.carvalhorr.daysInOffice.core.domain.usecase.GetCalendarMonthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

sealed class CalendarUiState {
    data object Loading : CalendarUiState()
    data class Success(
        val currentMonth: YearMonth,
        val days: List<DayRecord>,
        val selectedDay: DayRecord?
    ) : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCalendarMonthUseCase: GetCalendarMonthUseCase,
    private val dayRecordRepository: DayRecordRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private val _retrySignal = MutableStateFlow(0)

    val state: StateFlow<CalendarUiState> = _retrySignal.flatMapLatest {
        combine(
            _currentMonth.flatMapLatest { month ->
                getCalendarMonthUseCase(month).map { days -> month to days }
            },
            _selectedDate
        ) { monthAndDays, selectedDate ->
            val (month, days) = monthAndDays
            val result: CalendarUiState = CalendarUiState.Success(
                currentMonth = month,
                days = days,
                selectedDay = selectedDate?.let { date -> days.find { it.date == date } }
            )
            result
        }
        .catch { e -> emit(CalendarUiState.Error(e.message ?: "Unknown error")) }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState.Loading
        )

    fun goToPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun selectDay(date: LocalDate) {
        _selectedDate.value = date
    }

    fun dismissDay() {
        _selectedDate.value = null
    }

    fun overrideStatus(date: LocalDate, status: DayStatus) {
        viewModelScope.launch {
            dayRecordRepository.upsertDayRecord(
                DayRecord(
                    date = date,
                    status = status,
                    detectionMethod = if (status == DayStatus.OFFICE || status == DayStatus.REMOTE) DetectionMethod.MANUAL else null,
                    confirmedByUser = true
                )
            )
            dismissDay()
        }
    }

    fun retry() {
        _retrySignal.value++
    }
}
