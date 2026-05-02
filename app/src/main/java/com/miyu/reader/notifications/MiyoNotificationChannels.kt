package com.miyu.reader.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object MiyoNotificationChannels {
    const val DOWNLOAD_PROGRESS = "miyo_download_progress"
    const val DOWNLOAD_COMPLETE = "miyo_download_complete"
    const val DOWNLOAD_ERROR = "miyo_download_error"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    DOWNLOAD_PROGRESS,
                    "Download progress",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Progress for background novel and EPUB downloads."
                },
                NotificationChannel(
                    DOWNLOAD_COMPLETE,
                    "Download complete",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Successful source download and import notifications."
                },
                NotificationChannel(
                    DOWNLOAD_ERROR,
                    "Download errors",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Failed source download and parser notifications."
                },
            ),
        )
    }
}
