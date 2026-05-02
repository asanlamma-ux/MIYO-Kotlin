package com.miyu.reader.storage

import android.content.Context
import java.io.File

object MiyoStorage {
    private const val ROOT_NAME = "Miyo"
    private const val BOOKS_NAME = "Books"
    private const val COVERS_NAME = "Covers"
    private const val DOWNLOADS_NAME = "Downloads"
    private const val ONLINE_EPUB_NAME = "Online EPUB"

    fun rootDir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, ROOT_NAME)
            .canonicalFile
            .apply { mkdirs() }

    fun booksDir(context: Context): File =
        File(rootDir(context), BOOKS_NAME)
            .canonicalFile
            .apply { mkdirs() }

    fun coversDir(context: Context): File =
        File(rootDir(context), COVERS_NAME)
            .canonicalFile
            .apply { mkdirs() }

    fun onlineEpubDir(context: Context): File =
        File(File(rootDir(context), DOWNLOADS_NAME), ONLINE_EPUB_NAME)
            .canonicalFile
            .apply { mkdirs() }

    fun tempDir(context: Context): File =
        File(context.cacheDir, "miyo")
            .canonicalFile
            .apply { mkdirs() }

    fun safeChild(parent: File, childName: String): File {
        val canonicalParent = parent.canonicalFile.apply { mkdirs() }
        val target = File(canonicalParent, childName).canonicalFile
        if (!target.path.startsWith(canonicalParent.path + File.separator)) {
            error("Invalid storage destination.")
        }
        return target
    }

    fun legacyBooksDir(context: Context): File =
        File(context.filesDir, "books").canonicalFile

    fun legacyCoversDir(context: Context): File =
        File(context.filesDir, "covers").canonicalFile
}
