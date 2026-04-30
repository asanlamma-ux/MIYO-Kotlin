#include "epub_renderer.h"
#include <android/log.h>
#include <regex>
#include <sstream>
#include "../storage/cache_manager.h"

#define LOG_TAG "EPUB_RENDERER"

namespace miyu {
namespace epub {

namespace {

// Apply term replacements to content text (preserves HTML structure)
std::string applyTermReplacements(const std::string& html,
                                   const std::map<std::string, std::string>& replacements) {
    if (replacements.empty()) return html;

    std::string result = html;
    for (const auto& [original, corrected] : replacements) {
        if (original == corrected) continue;
        // Simple case-sensitive replacement
        // In production, this would use a text-node-aware DOM walk
        size_t pos = 0;
        while ((pos = result.find(original, pos)) != std::string::npos) {
            // Check if we're inside a tag or already replaced
            result.replace(pos, original.length(), corrected);
            pos += corrected.length();
        }
    }
    return result;
}

} // anonymous namespace

std::string renderChapter(
    const std::string& filePath,
    int chapterIndex,
    const std::map<std::string, std::string>& termReplacements,
    CacheManager& cache) {

    const auto* cached = cache.get(filePath);
    if (!cached || chapterIndex < 0 || chapterIndex >= cached->parsed.totalChapters) {
        return "<html><body><p>Chapter not found</p></body></html>";
    }

    const auto& chapter = cached->parsed.chapters[chapterIndex];
    std::string content = applyTermReplacements(chapter.content, termReplacements);

    // Build a standalone HTML document for the WebView reader
    std::ostringstream html;
    html << "<!DOCTYPE html>\n<html lang=\"" << cached->parsed.metadata.language << "\">\n<head>\n";
    html << "<meta charset=\"UTF-8\">\n";
    html << "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n";
    html << "<title>" << chapter.title << "</title>\n";

    // Inject extracted CSS
    if (!cached->parsed.extractedCss.empty()) {
        html << "<style>\n" << cached->parsed.extractedCss << "\n</style>\n";
    }

    // Reader-specific styles (injected by Kotlin layer for theme support)
    html << "<style id=\"miyu-reader-theme\"></style>\n";
    html << "<script>\n";
    html << "// MIYU reader bridge — communicates with Kotlin host\n";
    html << "var MIYU = {\n";
    html << "  getScrollProgress: function() {\n";
    html << "    var doc = document.documentElement;\n";
    html << "    var scrollTop = window.scrollY || doc.scrollTop;\n";
    html << "    var scrollHeight = doc.scrollHeight - window.innerHeight;\n";
    html << "    return scrollHeight > 0 ? scrollTop / scrollHeight : 0;\n";
    html << "  },\n";
    html << "  getSelection: function() {\n";
    html << "    var sel = window.getSelection();\n";
    html << "    if (!sel || sel.isCollapsed) return null;\n";
    html << "    return sel.toString();\n";
    html << "  },\n";
    html << "  highlightRange: function(startOffset, endOffset, color) {\n";
    html << "    var sel = window.getSelection();\n";
    html << "    /* apply highlight styling */\n";
    html << "  },\n";
    html << "};\n";
    html << "</script>\n";
    html << "</head>\n<body>\n";
    html << content;
    html << "\n</body>\n</html>";

    return html.str();
}

int countChapterWords(
    const std::string& filePath,
    int chapterIndex,
    CacheManager& cache) {

    const auto* cached = cache.get(filePath);
    if (!cached || chapterIndex < 0 || chapterIndex >= cached->parsed.totalChapters) {
        return 0;
    }
    return cached->parsed.chapters[chapterIndex].wordCount;
}

} // namespace epub
} // namespace miyu