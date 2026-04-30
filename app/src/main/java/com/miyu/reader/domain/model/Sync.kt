package com.miyu.reader.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SyncConfig(
    val adapterType: SyncAdapterType,
    val enabled: Boolean = false,
    val lastSyncAt: String? = null,
    // WebDAV
    val webDavUrl: String? = null,
    val webDavUsername: String? = null,
    val webDavPassword: String? = null,
    // Google Drive
    val googleDriveFolderId: String? = null,
    val googleDriveRefreshToken: String? = null,
)

@Keep
enum class SyncAdapterType { NONE, WEBDAV, GOOGLE_DRIVE }

@Keep
enum class SyncState { IDLE, SYNCING, ERROR }

@Keep
@Serializable
data class SyncProgress(
    val state: SyncState = SyncState.IDLE,
    val totalBooks: Int = 0,
    val syncedBooks: Int = 0,
    val currentBook: String? = null,
    val error: String? = null,
)