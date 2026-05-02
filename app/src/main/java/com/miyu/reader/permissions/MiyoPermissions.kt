package com.miyu.reader.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class MiyoPermissionSnapshot(
    val storageManagerGranted: Boolean,
    val mediaImagesGranted: Boolean,
    val notificationGranted: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val legacyStorageGranted: Boolean,
) {
    val storageReady: Boolean
        get() = storageManagerGranted && legacyStorageGranted

    val backgroundDownloadReady: Boolean
        get() = notificationGranted && batteryOptimizationIgnored

    val missingCriticalStorage: Boolean
        get() = !storageReady
}

object MiyoPermissions {
    fun snapshot(context: Context): MiyoPermissionSnapshot =
        MiyoPermissionSnapshot(
            storageManagerGranted = hasStorageManagerAccess(context),
            mediaImagesGranted = hasReadMediaImages(context),
            notificationGranted = hasPostNotifications(context),
            batteryOptimizationIgnored = isIgnoringBatteryOptimizations(context),
            legacyStorageGranted = hasLegacyStoragePermission(context),
        )

    fun runtimePermissionsToRequest(context: Context): Array<String> =
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!hasPostNotifications(context)) add(Manifest.permission.POST_NOTIFICATIONS)
                if (!hasReadMediaImages(context)) add(Manifest.permission.READ_MEDIA_IMAGES)
            } else if (!hasLegacyStoragePermission(context)) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }.toTypedArray()

    fun openStoragePermissionSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        }
        context.safeStartActivity(intent)
    }

    @SuppressLint("BatteryLife")
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.safeStartActivity(intent)
    }

    fun openAppSettings(context: Context) {
        context.safeStartActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
    }

    private fun hasStorageManagerAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasLegacyStoragePermission(context)
        }

    private fun hasLegacyStoragePermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun hasReadMediaImages(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun hasPostNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        runCatching {
            val powerManager = context.getSystemService(PowerManager::class.java)
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }.getOrDefault(false)

    private fun Context.safeStartActivity(intent: Intent) {
        val launchIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(launchIntent) }
            .recoverCatching {
                if (it is ActivityNotFoundException) {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } else {
                    throw it
                }
            }
    }
}
