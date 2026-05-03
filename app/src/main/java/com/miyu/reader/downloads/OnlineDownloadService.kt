package com.miyu.reader.downloads

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.data.repository.NovelSourcePluginRegistry
import com.miyu.reader.domain.model.OnlineDownloadHistoryEntry
import com.miyu.reader.domain.model.OnlineDownloadRequest
import com.miyu.reader.notifications.OnlineDownloadNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class OnlineDownloadService : Service() {
    @Inject lateinit var sourceRegistry: NovelSourcePluginRegistry
    @Inject lateinit var bookRepository: BookRepository
    @Inject lateinit var preferences: UserPreferences
    @Inject lateinit var notifier: OnlineDownloadNotifier
    @Inject lateinit var coordinator: OnlineDownloadCoordinator

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private var activeRequest: OnlineDownloadRequest? = null
    private var activeJob: Job? = null
    private var activeTitle: String = ""
    private var activeTotal: Int = 0
    private val paused = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val cancelled = AtomicBoolean(false)
    private var completedCount = AtomicInteger(0)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val payload = intent.getStringExtra(EXTRA_REQUEST_JSON) ?: return START_NOT_STICKY
                val request = runCatching { json.decodeFromString(OnlineDownloadRequest.serializer(), payload) }.getOrNull()
                    ?: return START_NOT_STICKY
                startDownload(request)
            }
            ACTION_PAUSE -> setPaused(true)
            ACTION_RESUME -> setPaused(false)
            ACTION_CANCEL -> cancelActive(clearNotification = true)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startDownload(request: OnlineDownloadRequest) {
        val current = activeRequest
        if (current?.key == request.key) return
        cancelActive(clearNotification = false)
        activeRequest = request
        activeTitle = request.summary.title
        activeTotal = 0
        completedCount = AtomicInteger(0)
        paused.value = false
        cancelled.set(false)
        coordinator.updateProgress(request.key, request.summary.title, 0, 1, paused = false)
        startForeground(notifier.notificationId(request.key), notifier.buildProgressNotification(request.key, request.summary.title, 0, 1, paused = false))
        activeJob = serviceScope.launch {
            try {
                val sourceId = sourceRegistry.sourceIdForProvider(request.summary.providerId)
                val details = sourceRegistry.details(request.summary)
                val selectedOrders = request.selectedChapterOrders.toSet()
                val chapters = details.chapters
                    .sortedBy { it.order }
                    .filter { selectedOrders.isEmpty() || it.order in selectedOrders }
                if (chapters.isEmpty()) error("No chapters were found for ${details.title}.")
                activeTotal = chapters.size
                updateProgress(details.title, 0, chapters.size)
                val concurrency = preferences.downloadConcurrency.first()
                val selectedDetails = details.copy(
                    chapterCount = chapters.size,
                    chapters = chapters,
                )
                val generated = sourceRegistry.downloadAsEpub(
                    sourceId = sourceId,
                    novel = selectedDetails,
                    startChapter = chapters.first().order,
                    endChapter = chapters.last().order,
                    concurrency = concurrency,
                ) { completed, total ->
                    completedCount.set(completed)
                    activeTotal = total
                    updateProgress(details.title, completed, total)
                }
                val importedBook = bookRepository.importGeneratedOnlineNovelEpub(
                    filePath = generated.filePath,
                    fileName = generated.fileName,
                    suggestedTitle = details.title,
                    identityKey = onlineIdentityKey(request.summary.providerId, request.summary.path),
                )
                val importedEpub = generated.copy(
                    filePath = importedBook.filePath,
                    fileName = importedBook.fileName ?: generated.fileName,
                    title = importedBook.title,
                )
                val completedAt = Instant.now().toString()
                preferences.recordOnlineDownload(
                    OnlineDownloadHistoryEntry(
                        id = UUID.randomUUID().toString(),
                        key = request.key,
                        title = details.title,
                        chapterCount = chapters.size,
                        completedAt = completedAt,
                        dayKey = LocalDate.now(ZoneId.systemDefault()).toString(),
                    ),
                )
                coordinator.complete(request.key, importedBook.title, chapters.size, importedEpub)
                notifier.showComplete(request.key, importedBook.title, importedEpub.fileName)
            } catch (cancelledError: CancellationException) {
                coordinator.cancel(request.key, activeTitle, completedCount.get(), activeTotal)
                notifier.clear(request.key)
            } catch (error: Exception) {
                val message = error.message ?: "Novel download failed."
                coordinator.error(request.key, activeTitle, completedCount.get(), activeTotal, message)
                notifier.showError(request.key, activeTitle, message)
            } finally {
                if (activeRequest?.key == request.key) {
                    activeRequest = null
                    activeJob = null
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            }
        }
    }

    private fun setPaused(value: Boolean) {
        val request = activeRequest ?: return
        paused.value = value
        coordinator.updateProgress(
            key = request.key,
            title = activeTitle.ifBlank { request.summary.title },
            completed = completedCount.get(),
            total = activeTotal.coerceAtLeast(1),
            paused = value,
        )
        notifier.showProgress(
            taskKey = request.key,
            title = activeTitle.ifBlank { request.summary.title },
            completed = completedCount.get(),
            total = activeTotal.coerceAtLeast(1),
            paused = value,
        )
    }

    private fun updateProgress(title: String, completed: Int, total: Int) {
        val request = activeRequest ?: return
        coordinator.updateProgress(request.key, title, completed, total, paused.value)
        val notification: Notification = notifier.buildProgressNotification(
            request.key,
            title,
            completed,
            total,
            paused = paused.value,
        )
        startForeground(notifier.notificationId(request.key), notification)
    }

    private fun cancelActive(clearNotification: Boolean) {
        cancelled.set(true)
        paused.value = false
        activeJob?.cancel()
        val request = activeRequest
        if (request != null) {
            coordinator.cancel(request.key, activeTitle.ifBlank { request.summary.title }, completedCount.get(), activeTotal)
            if (clearNotification) notifier.clear(request.key)
        }
        activeRequest = null
        activeJob = null
    }

    companion object {
        private const val ACTION_START = "com.miyu.reader.download.START"
        private const val ACTION_PAUSE = "com.miyu.reader.download.PAUSE"
        private const val ACTION_RESUME = "com.miyu.reader.download.RESUME"
        private const val ACTION_CANCEL = "com.miyu.reader.download.CANCEL"
        private const val EXTRA_REQUEST_JSON = "request_json"

        fun start(context: Context, request: OnlineDownloadRequest) {
            val payload = Json { encodeDefaults = true }.encodeToString(OnlineDownloadRequest.serializer(), request)
            val intent = Intent(context, OnlineDownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_REQUEST_JSON, payload)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pauseIntent(context: Context): Intent =
            Intent(context, OnlineDownloadService::class.java).apply { action = ACTION_PAUSE }

        fun resumeIntent(context: Context): Intent =
            Intent(context, OnlineDownloadService::class.java).apply { action = ACTION_RESUME }

        fun cancelIntent(context: Context): Intent =
            Intent(context, OnlineDownloadService::class.java).apply { action = ACTION_CANCEL }

        fun pause(context: Context) {
            ContextCompat.startForegroundService(context, pauseIntent(context))
        }

        fun resume(context: Context) {
            ContextCompat.startForegroundService(context, resumeIntent(context))
        }

        fun cancel(context: Context) {
            ContextCompat.startForegroundService(context, cancelIntent(context))
        }

        private fun onlineIdentityKey(providerId: com.miyu.reader.domain.model.OnlineNovelProviderId, path: String): String =
            "online:${providerId.name.lowercase()}:${path.trim()}"
    }
}
