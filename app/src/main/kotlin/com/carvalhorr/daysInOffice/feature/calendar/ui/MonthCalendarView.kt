package com.carvalhorr.daysInOffice.feature.calendar.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.carvalhorr.daysInOffice.app.theme.dayStatusColor
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthCalendarView(
    yearMonth: YearMonth,
    days: List<DayRecord>,
    onDayClick: (DayRecord) -> Unit,
    onDayLongClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    // Sunday-first grid: Sunday(7) → 0, Monday(1) → 1, ..., Saturday(6) → 6
    val leadingEmptyCells = firstDay.dayOfWeek.value % 7

    val dayMap = days.associateBy { it.date }

    val gridItems = buildList<Pair<LocalDate, Boolean>> {
        for (i in leadingEmptyCells - 1 downTo 0) {
            add(firstDay.minusDays((i + 1).toLong()) to false)
        }
        for (day in 1..daysInMonth) {
            add(yearMonth.atDay(day) to true)
        }
        val totalCells = leadingEmptyCells + daysInMonth
        val trailingCells = if (totalCells % 7 == 0) 0 else 7 - (totalCells % 7)
        val lastDay = yearMonth.atEndOfMonth()
        for (i in 1..trailingCells) {
            add(lastDay.plusDays(i.toLong()) to false)
        }
    }

    val rows = gridItems.chunked(7)

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { header ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        for (row in rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for ((date, isCurrentMonth) in row) {
                    val record = if (isCurrentMonth) dayMap[date] else null
                    DayCell(
                        date = date,
                        record = record,
                        isCurrentMonth = isCurrentMonth,
                        onClick = if (isCurrentMonth && record != null) ({ onDayClick(record) }) else null,
                        onLongClick = if (isCurrentMonth) ({ onDayLongClick(date) }) else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    date: LocalDate,
    record: DayRecord?,
    isCurrentMonth: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val cellAlpha = if (isCurrentMonth) 1f else 0.3f

    val cellSemantics = if (isCurrentMonth && record != null) {
        val statusName = record.status.name.lowercase().replaceFirstChar { it.uppercaseChar() }
        Modifier.semantics { contentDescription = "$date, $statusName" }
    } else Modifier

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .alpha(cellAlpha)
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else Modifier
            )
            .then(cellSemantics),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (record != null) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.dayStatusColor(record.status)
                ) {}
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
