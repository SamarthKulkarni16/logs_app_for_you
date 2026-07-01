package com.samarth.logsapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samarth.logsapp.data.local.LogFileStore
import com.samarth.logsapp.sync.SyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val AUTOSAVE_DEBOUNCE_MILLIS = 600L

enum class SaveStatus {
    IDLE,
    SAVING,
    SAVED_LOCALLY,
    SYNCING,
    SYNCED,
    SYNC_FAILED
}

/**
 * Backs the log screen. Unlike a freeform notes app, this always resolves
 * to *today's* file: on init it reads today's existing body (if the day
 * was already started earlier) rather than presenting a blank page, so
 * reopening the app mid-day resumes where you left off instead of losing
 * the morning's entry.
 */
class LogViewModel(
    private val fileStore: LogFileStore,
    private val syncManager: SyncManager,
    private val currentUserId: () -> String?
) : ViewModel() {

    val todayKey: String = fileStore.todayKey()
    val dateLabel: String = fileStore.dateLabelFor(todayKey)

    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()

    private val _saveStatus = MutableStateFlow(SaveStatus.IDLE)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val existing = fileStore.readBody(todayKey)
            if (!existing.isNullOrEmpty()) {
                _body.value = existing
                _saveStatus.value = SaveStatus.SAVED_LOCALLY
            }
        }
    }

    fun onBodyChanged(newBody: String) {
        _body.value = newBody
        if (newBody.isNotBlank()) {
            _saveStatus.value = SaveStatus.SAVING
        }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MILLIS)
            persistAndSync(newBody)
        }
    }

    private suspend fun persistAndSync(text: String) {
        if (text.isBlank()) {
            _saveStatus.value = SaveStatus.IDLE
            return
        }

        fileStore.write(todayKey, text)
        _saveStatus.value = SaveStatus.SAVED_LOCALLY

        val userId = currentUserId()
        if (userId == null) {
            return
        }

        _saveStatus.value = SaveStatus.SYNCING
        val synced = syncManager.registerAndSync(todayKey, userId)
        _saveStatus.value = if (synced) SaveStatus.SYNCED else SaveStatus.SYNC_FAILED
    }
}
