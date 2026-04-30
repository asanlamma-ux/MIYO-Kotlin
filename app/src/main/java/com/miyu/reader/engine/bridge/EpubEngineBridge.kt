package com.miyu.reader.engine.bridge

/**
 * JNI bridge to the native C++ ebook parsing and rendering engine.
 *
 * The native layer handles:
 * - EPUB/MOBI/PDF/Comic archive parsing
 * - XML/HTML processing (metadata, TOC, chapters)
 * - Text layout (HarfBuzz + FreeType shaping)
 * - Inline image extraction and encoding
 */
class EpubEngineBridge {
    companion object {
        init {
            System.loadLibrary("miyu_engine")
        }
    }

    /**
     * Parse an EPUB file at [filePath] and return JSON with metadata + rendered chapters.
     * Returns JSON string matching ParsedEpub schema (EpubMetadata + chapters array).
     */
    external fun parseEpub(filePath: String): String

    /**
     * Extract the cover image from an EPUB file.
     * Returns base64-encoded image data, or null if no cover exists.
     */
    external fun extractCoverImage(filePath: String): String?

    /**
     * Extract all CSS from an EPUB, merged and deduplicated.
     * Returns a single CSS string for injection into reader HTML.
     */
    external fun extractStylesheet(filePath: String): String

    /**
     * Render a specific chapter from an EPUB, applying [termReplacements]
     * (MTL term correction map: originalText -> correctedText).
     * Returns the full HTML for the chapter with all styles inlined.
     */
    external fun renderChapter(
        filePath: String,
        chapterIndex: Int,
        termReplacements: Map<String, String> = emptyMap(),
    ): String

    /**
     * Count words in a specific chapter for reading stats.
     */
    external fun countChapterWords(filePath: String, chapterIndex: Int): Int

    /**
     * Free cached parsed data for an EPUB.
     * Call when a book is removed from library.
     */
    external fun evictCache(filePath: String)

    /**
     * Search all parsed chapter text in the native EPUB cache.
     * Returns a JSON array with chapter IDs, titles, HTML offsets, and excerpts.
     */
    external fun searchInBook(filePath: String, query: String): String
}
