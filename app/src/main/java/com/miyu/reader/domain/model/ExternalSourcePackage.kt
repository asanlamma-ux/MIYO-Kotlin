package com.miyu.reader.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExternalSourcePackageManifest(
    val schemaVersion: Int = 1,
    val packageId: String,
    val providerId: String,
    val bridgeScope: String,
    val name: String,
    val version: String,
    val site: String,
    val language: String,
    val startUrl: String,
    val entry: String = "main.js",
    val requiresVerification: Boolean = false,
    val description: String = "",
)

enum class ExternalSourcePackageOrigin {
    IMPORTED,
}

data class ExternalSourcePackageDescriptor(
    val packageId: String,
    val sourceId: String,
    val providerId: OnlineNovelProviderId,
    val name: String,
    val version: String,
    val site: String,
    val language: String,
    val startUrl: String,
    val requiresVerification: Boolean,
    val description: String,
    val origin: ExternalSourcePackageOrigin,
    val installPath: String? = null,
)
