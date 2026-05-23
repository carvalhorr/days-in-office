package com.carvalhorr.daysInOffice.feature.calendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.carvalhorr.daysInOffice.app.theme.DayStatusHolidayColor
import com.carvalhorr.daysInOffice.app.theme.DayStatusOfficeColor
import com.carvalhorr.daysInOffice.app.theme.DayStatusRemoteColor
import com.carvalhorr.daysInOffice.app.theme.DayStatusUnknownColor
import com.carvalhorr.daysInOffice.app.ui.common.EmptyState
import com.carvalhorr.daysInOffice.app.ui.common.ErrorState
import com.carvalhorr.daysInOffice.feature.calendar.CalendarUiState
import com.carvalhorr.daysInOffice.feature.calendar.CalendarViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                windowInsets = WindowInsets(0)
            )
        }
    ) { paddingValues ->
        when (val s = state) {
            is CalendarUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is CalendarUiState.Error -> {
                ErrorState(
                    message = s.message,
                    onRetry = viewModel::retry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is CalendarUiState.Success -> {
                if (s.days.isEmpty()) {
                    EmptyState(
                        message = "This month is empty",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = viewModel::goToPreviousMonth) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous month"
                                )
                            }
                            Text(
                                text = s.currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = viewModel::goToNextMonth) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next month"
                                )
                            }
                        }
                        MonthCalendarView(
                            yearMonth = s.currentMonth,
                            days = s.days,
                            onDayClick = viewModel::cycleDayStatus,
                            onDayLongClick = viewModel::selectDay,
                            modifier = Modifier.weight(1f)
                        )
                        CalendarLegend(modifier = Modifier.padding(vertical = 12.dp))
                    }
                    s.selectedDay?.let { day ->
                        DayDetailSheet(
                            dayRecord = day,
                            onDismiss = viewModel::dismissDay,
                            onOverrideStatus = viewModel::overrideStatus
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(color = DayStatusOfficeColor, label = "Office")
        LegendItem(color = DayStatusRemoteColor, label = "Remote")
        LegendItem(color = DayStatusHolidayColor, label = "Holiday/PTO")
        LegendItem(color = Color(0xFFBDBDBD), label = "Weekend")
        LegendItem(color = DayStatusUnknownColor, label = "Unknown")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = color
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
