#include "epub_renderer.h"
#include "epub_parser.h"
#include <android/log.h>
#include <algorithm>
#include <regex>
#include <sstream>
#include <vector>
#include "../storage/cache_manager.h"

#define LOG_TAG "EPUB_RENDERER"

namespace miyu {
namespace epub {

namespace {

// Apply term replacements to content text (preserves HTML structure)
std::string applyTermReplacements(const std::string& html,
                                   const std::map<std::string, std::string>& replacements) {
    if (replacements.empty()) return html;

    std::vector<std::pair<std::string, std::string>> ordered;
    ordered.reserve(replacements.size());
    for (const auto& [original, corrected] : replacements) {
        if (!original.empty() && original != corrected) {
            ordered.emplace_back(original, corrected);
        }
    }
    std::sort(ordered.begin(), ordered.end(), [](const auto& left, const auto& right) {
        return left.first.size() > right.first.size();
    });

    auto replaceText = [&ordered](const std::string& text) {
        std::string out;
        out.reserve(text.size());
        size_t i = 0;
        while (i < text.size()) {
            bool matched = false;
            for (const auto& [original, corrected] : ordered) {
                if (i + original.size() <= text.size() &&
                    text.compare(i, original.size(), original) == 0) {
                    out += corrected;
                    i += original.size();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                out += text[i];
                i += 1;
            }
        }
        return out;
    };

    std::regex tagRegex(R"(<[^>]+>)");
    std::sregex_iterator it(html.begin(), html.end(), tagRegex);
    std::sregex_iterator end;
    std::string result;
    size_t cursor = 0;
    for (; it != end; ++it) {
        const auto& match = *it;
        if (static_cast<size_t>(match.position()) > cursor) {
            result += replaceText(html.substr(cursor, match.position() - cursor));
        }
        result += match.str();
        cursor = match.position() + match.length();
    }
    if (cursor < html.size()) {
        result += replaceText(html.substr(cursor));
    }
    return result;
}

std::string extractBodyInnerHtml(const std::string& html) {
    std::regex bodyRegex(R"(<body[^>]*>([\s\S]*?)</body>)", std::regex::icase);
    std::smatch match;
    if (std::regex_search(html, match, bodyRegex) && match.size() > 1) {
        return match[1].str();
    }
    return html;
}

} // anonymous namespace

std::string renderChapter(
    const std::string& filePath,
    int chapterIndex,
    const std::map<std::string, std::string>& termReplacements,
    CacheManager& cache) {

    if (!cache.get(filePath)) {
        parseEpub(filePath, cache);
    }
    const auto* cached = cache.get(filePath);
    if (!cached || chapterIndex < 0 || chapterIndex >= cached->parsed.totalChapters) {
        return "<html><body><p>Chapter not found</p></body></html>";
    }

    const auto& chapter = cached->parsed.chapters[chapterIndex];
    std::string content = applyTermReplacements(extractBodyInnerHtml(chapter.content), termReplacements);

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

    if (!cache.get(filePath)) {
        parseEpub(filePath, cache);
    }
    const auto* cached = cache.get(filePath);
    if (!cached || chapterIndex < 0 || chapterIndex >= cached->parsed.totalChapters) {
        return 0;
    }
    return cached->parsed.chapters[chapterIndex].wordCount;
}

} // namespace epub
} // namespace miyu
