package com.samarth.logsapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samarth.logsapp.data.local.LogFileStore
import com.samarth.logsapp.data.remote.GeminiRepository
import com.samarth.logsapp.sync.SyncManager

class LogViewModelFactory(
    private val fileStore: LogFileStore,
    private val syncManager: SyncManager,
    private val currentUserId: () -> String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
            return LogViewModel(fileStore, syncManager, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}

class MonthGridViewModelFactory(
    private val fileStore: LogFileStore,
    private val geminiRepository: GeminiRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MonthGridViewModel::class.java)) {
            return MonthGridViewModel(fileStore, geminiRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
