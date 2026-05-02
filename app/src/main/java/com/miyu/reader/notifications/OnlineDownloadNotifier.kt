package com.miyu.reader.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.miyu.reader.MainActivity
import com.miyu.reader.R
import com.miyu.reader.downloads.OnlineDownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class OnlineDownloadNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun showProgress(taskKey: String, title: String, completed: Int, total: Int, paused: Boolean = false) {
        if (!canNotify()) return
        notify(taskKey = taskKey, notification = buildProgressNotification(taskKey, title, completed, total, paused))
    }

    fun showComplete(taskKey: String, title: String, fileName: String) {
        if (!canNotify()) return
        notify(
            taskKey = taskKey,
            notification = baseBuilder(MiyoNotificationChannels.DOWNLOAD_COMPLETE, "EPUB downloaded", "$title exported as $fileName")
                .setOngoing(false)
                .build(),
        )
    }

    fun showError(taskKey: String, title: String, message: String) {
        if (!canNotify()) return
        notify(
            taskKey = taskKey,
            notification = baseBuilder(MiyoNotificationChannels.DOWNLOAD_ERROR, "EPUB download failed", "$title: $message")
                .setOngoing(false)
                .build(),
        )
    }

    fun buildProgressNotification(
        taskKey: String,
        title: String,
        completed: Int,
        total: Int,
        paused: Boolean,
    ): Notification {
        val safeTotal = total.coerceAtLeast(1)
        val safeCompleted = completed.coerceIn(0, safeTotal)
        val percent = ((safeCompleted * 100f) / safeTotal.toFloat()).toInt()
        val stateLabel = if (paused) "Paused EPUB download" else "Downloading EPUB"
        val text = "$percent% · $safeCompleted/$safeTotal chapters"
        return baseBuilder(MiyoNotificationChannels.DOWNLOAD_PROGRESS, stateLabel, "$title: $text")
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setOngoing(safeCompleted < safeTotal)
            .setProgress(safeTotal, safeCompleted, false)
            .addAction(
                if (paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (paused) "Resume" else "Pause",
                serviceActionPendingIntent(if (paused) OnlineDownloadService.resumeIntent(context) else OnlineDownloadService.pauseIntent(context)),
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                serviceActionPendingIntent(OnlineDownloadService.cancelIntent(context)),
            )
            .build()
    }

    fun clear(taskKey: String) {
        manager.cancel(notificationId(taskKey))
    }

    @SuppressLint("MissingPermission")
    private fun notify(
        taskKey: String,
        notification: Notification,
    ) {
        manager.notify(notificationId(taskKey), notification)
    }

    private fun baseBuilder(channelId: String, title: String, text: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_nav_browse)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notificationId(taskKey: String): Int {
        val hash = taskKey.hashCode()
        return if (hash == Int.MIN_VALUE) 1 else abs(hash).coerceAtLeast(1)
    }

    private fun serviceActionPendingIntent(intent: Intent): PendingIntent =
        PendingIntent.getService(
            context,
            intent.action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
