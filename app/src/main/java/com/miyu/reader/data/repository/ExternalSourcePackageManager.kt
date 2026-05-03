package com.miyu.reader.data.repository

import android.content.Context
import android.net.Uri
import com.miyu.reader.domain.model.ExternalSourcePackageDescriptor
import com.miyu.reader.domain.model.ExternalSourcePackageManifest
import com.miyu.reader.domain.model.ExternalSourcePackageOrigin
import com.miyu.reader.domain.model.OnlineNovelProviderId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExternalSourcePackageManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val packageRoot by lazy { File(appContext.filesDir, "source-packages") }

    suspend fun importPackage(uri: Uri): ExternalSourcePackageDescriptor = withContext(Dispatchers.IO) {
        ensurePackageRoot()
        val tempDir = File(appContext.cacheDir, "source-package-import-${System.currentTimeMillis()}").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                unzipInto(tempDir, input)
            } ?: error("Could not open the selected package.")
            val manifest = readManifest(File(tempDir, MANIFEST_FILE_NAME))
            val scriptFile = File(tempDir, manifest.entry)
            validateManifest(manifest, scriptFile)
            val destination = File(packageRoot, manifest.packageId)
            destination.deleteRecursively()
            copyDirectory(tempDir, destination)
            loadImportedPackage(destination).descriptor
        } finally {
            tempDir.deleteRecursively()
        }
    }

    suspend fun removePackage(packageId: String) = withContext(Dispatchers.IO) {
        File(packageRoot, packageId.sanitizedPackageId()).deleteRecursively()
    }

    fun installedPackages(): List<InstalledExternalSourcePackage> {
        // Imported packages intentionally override bundled ones when the package id matches.
        val bundled = bundledPackages().associateBy { it.manifest.packageId }
        val imported = importedPackages().associateBy { it.manifest.packageId }
        return (bundled + imported)
            .values
            .sortedBy { it.manifest.name.lowercase() }
    }

    fun packageForSource(sourceId: String): InstalledExternalSourcePackage? =
        installedPackages().firstOrNull { it.sourceId == sourceId }

    fun packageForProvider(providerId: OnlineNovelProviderId): InstalledExternalSourcePackage? =
        installedPackages().firstOrNull { it.providerId == providerId }

    fun installedPackageDescriptors(): List<ExternalSourcePackageDescriptor> =
        installedPackages().map { it.descriptor }

    private fun bundledPackages(): List<InstalledExternalSourcePackage> {
        val packageIds = appContext.assets.list(BUNDLED_ASSET_ROOT).orEmpty()
        return packageIds.mapNotNull { packageId ->
            runCatching {
                val manifest = appContext.assets
                    .open("$BUNDLED_ASSET_ROOT/$packageId/$MANIFEST_FILE_NAME")
                    .bufferedReader()
                    .use { reader -> json.decodeFromString(ExternalSourcePackageManifest.serializer(), reader.readText()) }
                val script = appContext.assets
                    .open("$BUNDLED_ASSET_ROOT/$packageId/${manifest.entry}")
                    .bufferedReader()
                    .use { it.readText() }
                validateManifest(manifest, scriptText = script)
                InstalledExternalSourcePackage(
                    manifest = manifest,
                    providerId = OnlineNovelProviderId.valueOf(manifest.providerId),
                    sourceId = manifest.packageId.toSourceId(),
                    script = script,
                    origin = ExternalSourcePackageOrigin.BUNDLED,
                    installPath = null,
                )
            }.getOrNull()
        }
    }

    private fun importedPackages(): List<InstalledExternalSourcePackage> {
        ensurePackageRoot()
        return packageRoot.listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { directory ->
                runCatching { loadImportedPackage(directory) }.getOrNull()
            }
    }

    private fun loadImportedPackage(directory: File): InstalledExternalSourcePackage {
        val manifest = readManifest(File(directory, MANIFEST_FILE_NAME))
        val scriptFile = File(directory, manifest.entry)
        validateManifest(manifest, scriptFile)
        return InstalledExternalSourcePackage(
            manifest = manifest,
            providerId = OnlineNovelProviderId.valueOf(manifest.providerId),
            sourceId = manifest.packageId.toSourceId(),
            script = scriptFile.readText(),
            origin = ExternalSourcePackageOrigin.IMPORTED,
            installPath = directory.absolutePath,
        )
    }

    private fun readManifest(file: File): ExternalSourcePackageManifest {
        if (!file.isFile) error("Package manifest.json is missing.")
        return json.decodeFromString(ExternalSourcePackageManifest.serializer(), file.readText())
    }

    private fun validateManifest(
        manifest: ExternalSourcePackageManifest,
        scriptFile: File? = null,
        scriptText: String? = null,
    ) {
        if (!manifest.packageId.matches(PACKAGE_ID_REGEX)) {
            error("Package id must use lowercase letters, numbers, dots, dashes, or underscores.")
        }
        OnlineNovelProviderId.valueOf(manifest.providerId)
        if (manifest.bridgeScope.isBlank()) error("Package bridgeScope is missing.")
        if (manifest.name.isBlank()) error("Package name is missing.")
        if (manifest.startUrl.isBlank()) error("Package startUrl is missing.")
        if (manifest.entry.isBlank()) error("Package entry is missing.")
        if (scriptText.isNullOrBlank() && (scriptFile == null || !scriptFile.isFile)) {
            error("Package entry script is missing.")
        }
    }

    private fun ensurePackageRoot() {
        if (!packageRoot.exists()) {
            packageRoot.mkdirs()
        }
    }

    private fun unzipInto(destination: File, input: java.io.InputStream) {
        val rootPath = destination.canonicalPath + File.separator
        ZipInputStream(input).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val outFile = File(destination, entry.name)
                val canonical = outFile.canonicalFile
                if (!canonical.path.startsWith(rootPath) && canonical.path != destination.canonicalPath) {
                    error("Package contains invalid paths.")
                }
                if (entry.isDirectory) {
                    canonical.mkdirs()
                } else {
                    canonical.parentFile?.mkdirs()
                    FileOutputStream(canonical).use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
            }
        }
    }

    private fun copyDirectory(source: File, destination: File) {
        destination.mkdirs()
        source.listFiles().orEmpty().forEach { child ->
            child.copyRecursively(File(destination, child.name), overwrite = true)
        }
    }

    private fun String.toSourceId(): String = "external:${sanitizedPackageId()}"

    private fun String.sanitizedPackageId(): String = trim().lowercase()

    private companion object {
        const val BUNDLED_ASSET_ROOT = "source-packages"
        const val MANIFEST_FILE_NAME = "manifest.json"
        val PACKAGE_ID_REGEX = Regex("^[a-z0-9._-]+$")
    }
}

data class InstalledExternalSourcePackage(
    val manifest: ExternalSourcePackageManifest,
    val providerId: OnlineNovelProviderId,
    val sourceId: String,
    val script: String,
    val origin: ExternalSourcePackageOrigin,
    val installPath: String?,
) {
    val descriptor: ExternalSourcePackageDescriptor
        get() = ExternalSourcePackageDescriptor(
            packageId = manifest.packageId,
            sourceId = sourceId,
            providerId = providerId,
            name = manifest.name,
            version = manifest.version,
            site = manifest.site,
            language = manifest.language,
            startUrl = manifest.startUrl,
            requiresVerification = manifest.requiresVerification,
            description = manifest.description,
            origin = origin,
            installPath = installPath,
        )
}
