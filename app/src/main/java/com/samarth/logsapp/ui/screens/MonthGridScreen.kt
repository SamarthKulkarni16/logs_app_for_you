package com.samarth.logsapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samarth.logsapp.ui.components.TripleTapBox
import com.samarth.logsapp.ui.theme.AccentColor
import com.samarth.logsapp.ui.theme.AccentDimColor
import com.samarth.logsapp.ui.theme.InputBorderColor
import com.samarth.logsapp.ui.theme.MutedColor
import kotlin.math.ln
import kotlin.math.min

private const val WORD_COUNT_CAP = 150 // word count at/above which a dot reaches max size
private val MIN_DOT_SIZE = 5.dp
private val MAX_DOT_SIZE = 18.dp

/**
 * Month-grid history. Every cell is a real calendar day (Monday-first);
 * days with no entry show only a faint static outline and are not
 * tappable at all — the grid itself communicates "you wrote or you
 * didn't," with no separate list needed. Days that were written show a
 * filled dot whose *size* scales with word count on a log curve, so a
 * two-line day and a two-page day are visually distinct at a glance
 * without needing numbers or a legend.
 */
@Composable
fun MonthGridScreen(
    viewModel: MonthGridViewModel,
    onTripleTapToLog: () -> Unit
) {
    val displayedMonth by viewModel.displayedMonth.collectAsState()
    val days by viewModel.days.collectAsState()
    val selectedDateKey by viewModel.selectedDateKey.collectAsState()
    val selectedBody by viewModel.selectedBody.collectAsState()
    val summaryState by viewModel.summaryState.collectAsState()
    val summaryText by viewModel.summaryText.collectAsState()

    TripleTapBox(onTripleTap = onTripleTapToLog) {
        if (selectedDateKey != null) {
            DayDetailPanel(
                dateKey = selectedDateKey!!,
                body = selectedBody,
                onDismiss = viewModel::closeDay
            )
            return@TripleTapBox
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 44.dp)
        ) {
            MonthHeader(
                displayedMonth = displayedMonth,
                onPrevious = viewModel::goToPreviousMonth,
                onNext = viewModel::goToNextMonth
            )

            Spacer(modifier = Modifier.height(28.dp))
            WeekdayLabels()
            Spacer(modifier = Modifier.height(10.dp))
            CalendarGrid(days = days, onDayTap = viewModel::openDay)

            Spacer(modifier = Modifier.height(40.dp))
            SummarySection(
                state = summaryState,
                text = summaryText,
                onRequestSummary = viewModel::requestMonthSummary
            )
        }
    }
}

@Composable
private fun MonthHeader(
    displayedMonth: java.time.YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val label = "${displayedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${displayedMonth.year}"
    val isCurrentMonth = displayedMonth == java.time.YearMonth.now()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            style = MaterialTheme.typography.headlineMedium,
            color = MutedColor,
            modifier = Modifier
                .clickable(onClick = onPrevious)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "›",
            style = MaterialTheme.typography.headlineMedium,
            color = if (isCurrentMonth) InputBorderColor else MutedColor,
            modifier = Modifier
                .clickable(enabled = !isCurrentMonth, onClick = onNext)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun WeekdayLabels() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MutedColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CalendarGrid(days: List<DayCell>, onDayTap: (String) -> Unit) {
    if (days.isEmpty()) return

    // Monday-first leading blanks so day 1 lands under the correct weekday label.
    val firstDate = java.time.LocalDate.parse(days.first().dateKey)
    val leadingBlanks = firstDate.dayOfWeek.value - 1 // MONDAY=1 -> 0 blanks

    val cells: List<DayCell?> = List(leadingBlanks) { null } + days
    val rows = cells.chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { cell ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (cell != null) DayCellView(cell, onDayTap)
                    }
                }
                // Pad the final partial row so cells stay left-aligned under their weekday column.
                repeat(7 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayCellView(cell: DayCell, onDayTap: (String) -> Unit) {
    val written = cell.wordCount != null && cell.wordCount > 0
    val dotSize = if (written) dotSizeForWordCount(cell.wordCount!!) else MIN_DOT_SIZE

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .aspectRatio(1f)
            .then(
                if (written) Modifier.clickable { onDayTap(cell.dateKey) } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .size(MAX_DOT_SIZE)
                .then(
                    if (cell.isToday) {
                        Modifier.drawBehind {
                            drawCircle(
                                color = AccentDimColor,
                                radius = size.minDimension / 2,
                                style = Stroke(width = 1.4.dp.toPx())
                            )
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (written) AccentColor else InputBorderColor)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = cell.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (written) MutedColor else InputBorderColor,
            fontSize = 10.sp
        )
    }
}

/** Log-scaled dot diameter so a handful of extra words doesn't dominate the visual, but the difference between a short and long day is still obvious. */
private fun dotSizeForWordCount(wordCount: Int): androidx.compose.ui.unit.Dp {
    val ratio = min(1f, (ln((wordCount + 1).toDouble()) / ln((WORD_COUNT_CAP + 1).toDouble())).toFloat())
    return MIN_DOT_SIZE + (MAX_DOT_SIZE - MIN_DOT_SIZE) * ratio
}

@Composable
private fun DayDetailPanel(dateKey: String, body: String?, onDismiss: () -> Unit) {
    val date = java.time.LocalDate.parse(dateKey)
    val label = "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}, ${date.year}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
            .padding(horizontal = 24.dp, vertical = 44.dp)
    ) {
        Text(text = label, style = com.samarth.logsapp.ui.theme.EntryHeaderStyle.Date)
        Text(
            text = "tap anywhere to close",
            style = MaterialTheme.typography.labelSmall,
            color = MutedColor,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = body ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 28.dp)
        )
    }
}

@Composable
private fun SummarySection(
    state: SummaryState,
    text: String?,
    onRequestSummary: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (state) {
            SummaryState.IDLE -> {
                Text(
                    text = "reflect on this month",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentDimColor,
                    modifier = Modifier.clickable(onClick = onRequestSummary)
                )
            }
            SummaryState.LOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = AccentDimColor,
                    strokeWidth = 2.dp
                )
            }
            SummaryState.READY -> {
                Text(
                    text = text ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            SummaryState.UNAVAILABLE -> {
                Text(
                    text = "no reflection available right now",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedColor
                )
            }
        }
    }
}
