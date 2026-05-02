package com.miyu.reader.downloads

import com.miyu.reader.domain.model.GeneratedOnlineNovelEpub
import com.miyu.reader.domain.model.OnlineDownloadStatus
import com.miyu.reader.domain.model.OnlineDownloadTaskState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineDownloadCoordinator @Inject constructor() {
    private val _tasks = MutableStateFlow<Map<String, OnlineDownloadTaskState>>(emptyMap())
    val tasks: StateFlow<Map<String, OnlineDownloadTaskState>> = _tasks.asStateFlow()

    fun upsert(task: OnlineDownloadTaskState) {
        _tasks.update { current -> current + (task.key to task) }
    }

    fun updateProgress(key: String, title: String, completed: Int, total: Int, paused: Boolean = false) {
        upsert(
            OnlineDownloadTaskState(
                key = key,
                title = title,
                completed = completed,
                total = total,
                status = if (paused) OnlineDownloadStatus.PAUSED else OnlineDownloadStatus.RUNNING,
            ),
        )
    }

    fun complete(key: String, title: String, total: Int, epub: GeneratedOnlineNovelEpub) {
        upsert(
            OnlineDownloadTaskState(
                key = key,
                title = title,
                completed = total,
                total = total,
                status = OnlineDownloadStatus.COMPLETED,
                generatedEpub = epub,
            ),
        )
    }

    fun error(key: String, title: String, completed: Int, total: Int, message: String) {
        upsert(
            OnlineDownloadTaskState(
                key = key,
                title = title,
                completed = completed,
                total = total,
                status = OnlineDownloadStatus.ERROR,
                error = message,
            ),
        )
    }

    fun cancel(key: String, title: String, completed: Int, total: Int) {
        upsert(
            OnlineDownloadTaskState(
                key = key,
                title = title,
                completed = completed,
                total = total,
                status = OnlineDownloadStatus.CANCELED,
            ),
        )
    }

    fun clear(key: String) {
        _tasks.update { current -> current - key }
    }
}
