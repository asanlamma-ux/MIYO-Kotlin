package com.miyu.reader.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.downloads.OnlineDownloadCoordinator
import com.miyu.reader.downloads.OnlineDownloadService
import com.miyu.reader.domain.model.OnlineDownloadHistoryEntry
import com.miyu.reader.domain.model.OnlineDownloadStatus
import com.miyu.reader.domain.model.OnlineDownloadTaskState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DownloadsUiState(
    val queueItems: List<OnlineDownloadTaskState> = emptyList(),
    val todayHistory: List<OnlineDownloadHistoryEntry> = emptyList(),
    val todayTotalChapters: Int = 0,
    val activeDownloads: Int = 0,
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val preferences: UserPreferences,
    private val downloadCoordinator: OnlineDownloadCoordinator,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val dayKey = MutableStateFlow(todayKey())

    init {
        viewModelScope.launch {
            while (true) {
                val now = java.time.ZonedDateTime.now(ZoneId.systemDefault())
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L))
                dayKey.value = todayKey()
            }
        }
        viewModelScope.launch {
            downloadCoordinator.tasks.collect { tasks ->
                syncUi(tasks = tasks.values.toList())
            }
        }
        viewModelScope.launch {
            preferences.onlineDownloadHistory.collect {
                syncUi(history = it)
            }
        }
        viewModelScope.launch {
            dayKey.collect {
                syncUi()
            }
        }
    }

    fun pause(task: OnlineDownloadTaskState) {
        if (task.status == OnlineDownloadStatus.RUNNING) {
            OnlineDownloadService.pause(context)
        }
    }

    fun resume(task: OnlineDownloadTaskState) {
        if (task.status == OnlineDownloadStatus.PAUSED) {
            OnlineDownloadService.resume(context)
        }
    }

    fun cancel(task: OnlineDownloadTaskState) {
        if (task.status == OnlineDownloadStatus.RUNNING || task.status == OnlineDownloadStatus.PAUSED) {
            OnlineDownloadService.cancel(context)
        }
    }

    private fun syncUi(
        tasks: List<OnlineDownloadTaskState>? = null,
        history: List<OnlineDownloadHistoryEntry>? = null,
    ) {
        _uiState.update { current ->
            val queueItems = (tasks ?: current.queueItems).filter {
                it.status == OnlineDownloadStatus.RUNNING ||
                    it.status == OnlineDownloadStatus.PAUSED ||
                    it.status == OnlineDownloadStatus.ERROR
            }.sortedWith(
                compareByDescending<OnlineDownloadTaskState> { it.status == OnlineDownloadStatus.RUNNING }
                    .thenByDescending { it.status == OnlineDownloadStatus.PAUSED }
                    .thenBy { it.title.lowercase() },
            )
            val today = (history ?: current.todayHistory).filter { it.dayKey == dayKey.value }
            current.copy(
                queueItems = queueItems,
                todayHistory = today.sortedByDescending { it.completedAt },
                todayTotalChapters = today.sumOf { it.chapterCount },
                activeDownloads = queueItems.count { it.status == OnlineDownloadStatus.RUNNING || it.status == OnlineDownloadStatus.PAUSED },
            )
        }
    }

    private companion object {
        fun todayKey(): String = LocalDate.now(ZoneId.systemDefault()).toString()
    }
}
