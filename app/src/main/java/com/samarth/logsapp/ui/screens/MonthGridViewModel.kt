package com.samarth.logsapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samarth.logsapp.data.local.LogFileStore
import com.samarth.logsapp.data.remote.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** One calendar cell's data — null wordCount means nothing was written that day. */
data class DayCell(
    val dateKey: String,
    val dayOfMonth: Int,
    val wordCount: Int?,
    val isToday: Boolean
)

enum class SummaryState { IDLE, LOADING, READY, UNAVAILABLE }

class MonthGridViewModel(
    private val fileStore: LogFileStore,
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val monthKeyFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val todayKey = fileStore.todayKey()

    private val _displayedMonth = MutableStateFlow(YearMonth.now())
    val displayedMonth: StateFlow<YearMonth> = _displayedMonth.asStateFlow()

    private val _days = MutableStateFlow<List<DayCell>>(emptyList())
    val days: StateFlow<List<DayCell>> = _days.asStateFlow()

    /** dateKey of the currently opened read-only day, or null if none selected. */
    private val _selectedDateKey = MutableStateFlow<String?>(null)
    val selectedDateKey: StateFlow<String?> = _selectedDateKey.asStateFlow()

    private val _selectedBody = MutableStateFlow<String?>(null)
    val selectedBody: StateFlow<String?> = _selectedBody.asStateFlow()

    private val _summaryState = MutableStateFlow(SummaryState.IDLE)
    val summaryState: StateFlow<SummaryState> = _summaryState.asStateFlow()

    private val _summaryText = MutableStateFlow<String?>(null)
    val summaryText: StateFlow<String?> = _summaryText.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val month = _displayedMonth.value
            val writtenKeys = fileStore.listWrittenDateKeys()
            val cells = (1..month.lengthOfMonth()).map { day ->
                val date = month.atDay(day)
                val key = date.toString() // yyyy-MM-dd, matches LogFileStore's dateKey format
                val wordCount = if (key in writtenKeys) {
                    // Only compute word counts for days actually written —
                    // avoids touching disk for every empty cell in the grid.
                    fileStore.wordCount(key)
                } else null
                DayCell(
                    dateKey = key,
                    dayOfMonth = day,
                    wordCount = wordCount,
                    isToday = key == todayKey
                )
            }
            _days.value = cells
            // Reset any stale AI summary when the visible month changes.
            _summaryState.value = SummaryState.IDLE
            _summaryText.value = null
        }
    }

    fun goToPreviousMonth() {
        _displayedMonth.value = _displayedMonth.value.minusMonths(1)
        refresh()
    }

    fun goToNextMonth() {
        val next = _displayedMonth.value.plusMonths(1)
        // Never navigate into the future beyond the current month.
        if (!next.isAfter(YearMonth.now())) {
            _displayedMonth.value = next
            refresh()
        }
    }

    /** Only ever called for cells where wordCount != null (grid enforces this at the UI layer, but this is the single source of truth). */
    fun openDay(dateKey: String) {
        viewModelScope.launch {
            _selectedDateKey.value = dateKey
            _selectedBody.value = fileStore.readBody(dateKey)
        }
    }

    fun closeDay() {
        _selectedDateKey.value = null
        _selectedBody.value = null
    }

    fun requestMonthSummary() {
        viewModelScope.launch {
            _summaryState.value = SummaryState.LOADING
            val monthKey = _displayedMonth.value.format(monthKeyFormatter)
            val logs = fileStore.readAllForMonth(monthKey)
            if (logs.isEmpty()) {
                _summaryState.value = SummaryState.UNAVAILABLE
                return@launch
            }
            val summary = geminiRepository.summarizeMonth(
                logs.map { it.dateLabel to it.body }
            )
            if (summary != null) {
                _summaryText.value = summary
                _summaryState.value = SummaryState.READY
            } else {
                _summaryState.value = SummaryState.UNAVAILABLE
            }
        }
    }
}
