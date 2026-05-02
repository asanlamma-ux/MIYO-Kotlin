package com.miyu.reader.notifications

import android.Manifest
import android.annotation.SuppressLint
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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class OnlineDownloadNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun showProgress(taskKey: String, title: String, completed: Int, total: Int) {
        if (!canNotify()) return
        val safeTotal = total.coerceAtLeast(1)
        val safeCompleted = completed.coerceIn(0, safeTotal)
        val text = "$title: $safeCompleted/$safeTotal chapters"
        notify(
            taskKey = taskKey,
            builder = baseBuilder(MiyoNotificationChannels.DOWNLOAD_PROGRESS, "Downloading EPUB", text)
                .setOnlyAlertOnce(true)
                .setOngoing(safeCompleted < safeTotal)
                .setProgress(safeTotal, safeCompleted, false),
        )
    }

    fun showComplete(taskKey: String, title: String, fileName: String) {
        if (!canNotify()) return
        notify(
            taskKey = taskKey,
            builder = baseBuilder(MiyoNotificationChannels.DOWNLOAD_COMPLETE, "EPUB downloaded", "$title exported as $fileName")
                .setOngoing(false),
        )
    }

    fun showError(taskKey: String, title: String, message: String) {
        if (!canNotify()) return
        notify(
            taskKey = taskKey,
            builder = baseBuilder(MiyoNotificationChannels.DOWNLOAD_ERROR, "EPUB download failed", "$title: $message")
                .setOngoing(false),
        )
    }

    fun clear(taskKey: String) {
        manager.cancel(notificationId(taskKey))
    }

    @SuppressLint("MissingPermission")
    private fun notify(
        taskKey: String,
        builder: NotificationCompat.Builder,
    ) {
        manager.notify(notificationId(taskKey), builder.build())
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

    private fun notificationId(taskKey: String): Int {
        val hash = taskKey.hashCode()
        return if (hash == Int.MIN_VALUE) 1 else abs(hash).coerceAtLeast(1)
    }
}
