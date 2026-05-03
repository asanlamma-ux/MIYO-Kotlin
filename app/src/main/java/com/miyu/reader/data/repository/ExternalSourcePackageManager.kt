package com.miyu.reader.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import java.net.URI
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
        validateSelectedPackage(uri)
        validateArchiveSize(uri)
        val tempDir = File(appContext.cacheDir, "source-package-import-${System.currentTimeMillis()}").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                unzipInto(tempDir, input)
            } ?: error("Could not open the selected package.")
            installExtractedPackages(tempDir).lastOrNull()
                ?: error("No source packages were found in the selected archive.")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    suspend fun removePackage(packageId: String) = withContext(Dispatchers.IO) {
        packageDirectory(packageId).deleteRecursively()
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
        val scriptFile = resolvePackageChild(directory, manifest.entry)
        validateManifest(manifest, directory, scriptFile)
        return InstalledExternalSourcePackage(
            manifest = manifest,
            providerId = OnlineNovelProviderId.valueOf(manifest.providerId),
            sourceId = manifest.packageId.toSourceId(),
            script = scriptFile.readText(),
            origin = ExternalSourcePackageOrigin.IMPORTED,
            installPath = directory.absolutePath,
        )
    }

    private fun installExtractedPackages(directory: File): List<ExternalSourcePackageDescriptor> {
        if (File(directory, MANIFEST_FILE_NAME).isFile) {
            return listOf(installPackageDirectory(directory))
        }

        val nestedArchives = directory
            .walkTopDown()
            .filter { it.isFile && it.name.endsWith(".miyuplugin.zip", ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
            .toList()
        if (nestedArchives.isEmpty()) {
            error("Package manifest.json is missing.")
        }

        return nestedArchives.mapIndexed { index, archive ->
            val nestedDir = File(directory, "__nested_package_$index").apply {
                deleteRecursively()
                mkdirs()
            }
            archive.inputStream().use { nestedInput ->
                unzipInto(nestedDir, nestedInput)
            }
            installPackageDirectory(nestedDir)
        }
    }

    private fun installPackageDirectory(directory: File): ExternalSourcePackageDescriptor {
        val manifest = readManifest(File(directory, MANIFEST_FILE_NAME))
        val scriptFile = resolvePackageChild(directory, manifest.entry)
        validateManifest(manifest, directory, scriptFile)
        val destination = packageDirectory(manifest.packageId)
        destination.deleteRecursively()
        copyDirectory(directory, destination)
        return loadImportedPackage(destination).descriptor
    }

    private fun readManifest(file: File): ExternalSourcePackageManifest {
        if (!file.isFile) error("Package manifest.json is missing.")
        return json.decodeFromString(ExternalSourcePackageManifest.serializer(), file.readText())
    }

    private fun validateManifest(
        manifest: ExternalSourcePackageManifest,
        packageDirectory: File? = null,
        scriptFile: File? = null,
        scriptText: String? = null,
    ) {
        if (manifest.schemaVersion != 1) {
            error("Unsupported package schema version: ${manifest.schemaVersion}.")
        }
        if (!manifest.packageId.matches(PACKAGE_ID_REGEX)) {
            error("Package id must use lowercase letters, numbers, dots, dashes, or underscores.")
        }
        OnlineNovelProviderId.valueOf(manifest.providerId)
        if (!manifest.bridgeScope.matches(PACKAGE_ID_REGEX) || manifest.bridgeScope != manifest.packageId) {
            error("Package bridgeScope must match the package id.")
        }
        if (manifest.name.isBlank()) error("Package name is missing.")
        if (manifest.version.isBlank()) error("Package version is missing.")
        if (manifest.site.isBlank()) error("Package site is missing.")
        validateStartUrl(manifest)
        validateEntryPath(manifest.entry)
        if (manifest.entry.isBlank()) error("Package entry is missing.")
        if (packageDirectory != null && scriptFile?.canonicalPath?.startsWith(packageDirectory.canonicalPath + File.separator) != true) {
            error("Package entry points outside the package directory.")
        }
        val scriptBody = scriptText ?: scriptFile?.takeIf { it.isFile }?.readText()
        if (scriptBody.isNullOrBlank()) {
            error("Package entry script is missing.")
        }
        if (scriptBody.length > MAX_SCRIPT_CHAR_COUNT) {
            error("Package entry script is too large.")
        }
    }

    private fun ensurePackageRoot() {
        if (!packageRoot.exists() && !packageRoot.mkdirs()) {
            error("Could not create the source package directory.")
        }
    }

    private fun validateArchiveSize(uri: Uri) {
        val archiveBytes = runCatching {
            appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.takeIf { it > 0L }
            }
        }.getOrNull()
        if (archiveBytes != null && archiveBytes > MAX_ARCHIVE_BYTES) {
            error("Package archive is too large.")
        }
    }

    private fun unzipInto(destination: File, input: java.io.InputStream) {
        val rootPath = destination.canonicalPath + File.separator
        var entryCount = 0
        var totalBytes = 0L
        ZipInputStream(input).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                entryCount += 1
                if (entryCount > MAX_ENTRY_COUNT) error("Package contains too many files.")
                validateArchiveEntryPath(entry.name)
                if (!entry.isDirectory && entry.size > MAX_ENTRY_BYTES) {
                    error("Package entry is too large.")
                }
                val outFile = resolvePackageChild(destination, entry.name)
                val canonical = outFile.canonicalFile
                if (!canonical.path.startsWith(rootPath) && canonical.path != destination.canonicalPath) {
                    error("Package contains invalid paths.")
                }
                if (entry.isDirectory) {
                    canonical.mkdirs()
                } else {
                    canonical.parentFile?.mkdirs()
                    var written = 0L
                    FileOutputStream(canonical).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read <= 0) break
                            written += read
                            totalBytes += read
                            if (written > MAX_ENTRY_BYTES || totalBytes > MAX_UNCOMPRESSED_BYTES) {
                                error("Package archive exceeds supported limits.")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
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

    private fun packageDirectory(packageId: String): File {
        if (!packageId.matches(PACKAGE_ID_REGEX)) error("Invalid package id.")
        return resolvePackageChild(packageRoot, packageId)
    }

    private fun resolvePackageChild(root: File, relativePath: String): File {
        if (relativePath.isBlank()) error("Package path is missing.")
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) error("Package path must be relative.")
        val normalized = relativePath.replace('\\', '/').trimEnd('/')
        if (normalized.split('/').any { it == ".." || it.isBlank() }) {
            error("Package path is invalid.")
        }
        val canonicalRoot = root.canonicalFile
        val target = File(canonicalRoot, normalized).canonicalFile
        if (!target.path.startsWith(canonicalRoot.path + File.separator)) {
            error("Package path resolves outside the package root.")
        }
        return target
    }

    private fun validateStartUrl(manifest: ExternalSourcePackageManifest) {
        val parsed = runCatching { URI(manifest.startUrl.trim()) }.getOrNull()
            ?: error("Package startUrl is invalid.")
        if (parsed.scheme?.lowercase() != "https") {
            error("Package startUrl must use HTTPS.")
        }
        val host = parsed.host?.lowercase().orEmpty()
        if (host.isBlank()) error("Package startUrl host is missing.")
        val declaredHost = manifest.site.trim().removePrefix("https://").removePrefix("http://").trimEnd('/').lowercase()
        if (declaredHost.isNotBlank() && !host.endsWith(declaredHost.removePrefix("www.")) && !host.endsWith(declaredHost)) {
            error("Package startUrl host must match the declared site.")
        }
    }

    private fun validateSelectedPackage(uri: Uri) {
        val name = appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?.trim()
            .orEmpty()
        if (name.isBlank()) return
        if (name.endsWith(".json", ignoreCase = true)) {
            error("Select a .miyuplugin.zip package file, not the feed catalog JSON.")
        }
        if (!name.endsWith(".miyuplugin.zip", ignoreCase = true) && !name.endsWith(".zip", ignoreCase = true)) {
            error("Source packages must be imported from a .miyuplugin.zip file.")
        }
    }

    private fun validateArchiveEntryPath(entry: String) {
        val normalized = entry.replace('\\', '/').trimEnd('/')
        if (normalized.isBlank()) error("Package contains an empty entry path.")
        if (!normalized.matches(PACKAGE_ENTRY_REGEX)) {
            error("Package contains an invalid entry path.")
        }
    }

    private fun validateEntryPath(entry: String) {
        val normalized = entry.replace('\\', '/').trimEnd('/')
        if (!normalized.matches(PACKAGE_ENTRY_REGEX)) {
            error("Package entry path is invalid.")
        }
        if (!normalized.endsWith(".js") && !entry.endsWith("/")) {
            error("Package entry must point to a JavaScript file.")
        }
    }

    private fun String.toSourceId(): String = "external:${sanitizedPackageId()}"

    private fun String.sanitizedPackageId(): String = trim().lowercase()

    private companion object {
        const val BUNDLED_ASSET_ROOT = "source-packages"
        const val MANIFEST_FILE_NAME = "manifest.json"
        val PACKAGE_ID_REGEX = Regex("^[a-z0-9._-]+$")
        val PACKAGE_ENTRY_REGEX = Regex("^[A-Za-z0-9._/-]+$")
        const val MAX_ARCHIVE_BYTES = 4L * 1024L * 1024L
        const val MAX_UNCOMPRESSED_BYTES = 8L * 1024L * 1024L
        const val MAX_ENTRY_BYTES = 2L * 1024L * 1024L
        const val MAX_ENTRY_COUNT = 32
        const val MAX_SCRIPT_CHAR_COUNT = 512_000
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
