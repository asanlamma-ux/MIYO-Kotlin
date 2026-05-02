package com.miyu.reader.storage

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object MiyoBackupManager {
    private const val SCHEMA_ENTRY = "miyo-backup-v1.txt"
    private const val FILES_PREFIX = "files/"
    private const val DATABASES_PREFIX = "databases/"
    private const val EXTERNAL_PREFIX = "external-miyo/"

    suspend fun exportToUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val output = resolver.openOutputStream(uri) ?: error("Unable to open backup destination.")
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(SCHEMA_ENTRY))
            zip.write("MIYO backup schema 1\n".toByteArray())
            zip.closeEntry()

            addDirectory(zip, context.filesDir, FILES_PREFIX)
            addDirectory(zip, File(context.applicationInfo.dataDir, "databases"), DATABASES_PREFIX)
            addDirectory(zip, MiyoStorage.rootDir(context), EXTERNAL_PREFIX)
        }
        "Exported MIYO data archive."
    }

    suspend fun importFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val input = resolver.openInputStream(uri) ?: error("Unable to open backup archive.")
        var restored = 0
        ZipInputStream(input.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                if (!entry.isDirectory && entry.name != SCHEMA_ENTRY) {
                    val destination = destinationForEntry(context, entry.name)
                    destination.parentFile?.mkdirs()
                    FileOutputStream(destination).use { output -> zip.copyTo(output) }
                    restored += 1
                }
                zip.closeEntry()
            }
        }
        "Imported $restored MIYO backup files. Restart the app if database-backed lists do not refresh immediately."
    }

    private fun addDirectory(zip: ZipOutputStream, root: File, prefix: String) {
        if (!root.exists()) return
        val canonicalRoot = root.canonicalFile
        canonicalRoot.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val canonicalFile = file.canonicalFile
                val relative = canonicalFile.relativeTo(canonicalRoot).invariantSeparatorsPath
                if (relative.isBlank()) return@forEach
                zip.putNextEntry(ZipEntry(prefix + relative))
                FileInputStream(canonicalFile).use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
    }

    private fun destinationForEntry(context: Context, entryName: String): File {
        val (root, relative) = when {
            entryName.startsWith(FILES_PREFIX) -> context.filesDir to entryName.removePrefix(FILES_PREFIX)
            entryName.startsWith(DATABASES_PREFIX) -> File(context.applicationInfo.dataDir, "databases") to entryName.removePrefix(DATABASES_PREFIX)
            entryName.startsWith(EXTERNAL_PREFIX) -> MiyoStorage.rootDir(context) to entryName.removePrefix(EXTERNAL_PREFIX)
            else -> error("Unsupported backup entry.")
        }
        val canonicalRoot = root.canonicalFile.apply { mkdirs() }
        val destination = File(canonicalRoot, relative).canonicalFile
        if (!destination.path.startsWith(canonicalRoot.path + File.separator)) {
            error("Blocked unsafe backup path.")
        }
        return destination
    }
}
