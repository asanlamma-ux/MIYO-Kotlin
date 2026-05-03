#include "epub_parser.h"
#include "zip_archive.h"
#include <android/log.h>
#include <sstream>
#include <algorithm>
#include <regex>
#include <map>
#include <set>
#include "../storage/cache_manager.h"

#define LOG_TAG "EPUB_PARSER"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace miyu {
namespace epub {

namespace {

std::string stripTags(const std::string& xml) {
    std::string result;
    result.reserve(xml.size());
    bool inTag = false;
    for (char c : xml) {
        if (c == '<') inTag = true;
        else if (c == '>') inTag = false;
        else if (!inTag) result += c;
    }
    return result;
}

std::string extractTag(const std::string& xml, const std::string& tag) {
    std::string openTag = "<" + tag;
    std::string closeTag = "</" + tag + ">";
    size_t start = xml.find(openTag);
    if (start == std::string::npos) return "";
    start = xml.find('>', start);
    if (start == std::string::npos) return "";
    start++;
    size_t end = xml.find(closeTag, start);
    if (end == std::string::npos) return "";
    return xml.substr(start, end - start);
}

std::string extractAttr(const std::string& attrs, const std::string& name) {
    std::regex attrRegex(name + R"(\s*=\s*["']([^"']+)["'])", std::regex::icase);
    std::smatch match;
    return std::regex_search(attrs, match, attrRegex) ? match[1].str() : "";
}

std::string readZipTextLoose(ZipArchive& zip, const std::string& path) {
    std::string content = zip.readText(path);
    if (!content.empty()) return content;

    std::string normalized = path;
    std::replace(normalized.begin(), normalized.end(), '\\', '/');
    while (!normalized.empty() && normalized.front() == '/') {
        normalized.erase(normalized.begin());
    }

    std::string lowerTarget = normalized;
    std::transform(lowerTarget.begin(), lowerTarget.end(), lowerTarget.begin(), ::tolower);
    for (const auto& entry : zip.entries()) {
        std::string lowerEntry = entry.name;
        std::transform(lowerEntry.begin(), lowerEntry.end(), lowerEntry.begin(), ::tolower);
        if (lowerEntry == lowerTarget || ends_with(lowerEntry, "/" + lowerTarget)) {
            content = zip.readText(entry.name);
            if (!content.empty()) return content;
        }
    }
    return "";
}

std::string normalizeZipPath(std::string path) {
    std::replace(path.begin(), path.end(), '\\', '/');

    std::vector<std::string> parts;
    std::stringstream current;
    while (!path.empty() && path.front() == '/') {
        path.erase(path.begin());
    }
    for (char c : path) {
        if (c == '/') {
            std::string part = current.str();
            current.str("");
            current.clear();
            if (part.empty() || part == ".") continue;
            if (part == "..") {
                if (!parts.empty()) parts.pop_back();
            } else {
                parts.push_back(part);
            }
        } else {
            current << c;
        }
    }
    std::string tail = current.str();
    if (!tail.empty() && tail != ".") {
        if (tail == "..") {
            if (!parts.empty()) parts.pop_back();
        } else {
            parts.push_back(tail);
        }
    }

    std::ostringstream normalized;
    for (size_t i = 0; i < parts.size(); ++i) {
        if (i > 0) normalized << "/";
        normalized << parts[i];
    }
    return normalized.str();
}

std::string dirName(const std::string& path) {
    const std::string normalized = normalizeZipPath(path);
    const size_t slash = normalized.find_last_of('/');
    return slash == std::string::npos ? "" : normalized.substr(0, slash);
}

std::vector<unsigned char> readZipBinaryLoose(ZipArchive& zip, const std::string& path) {
    std::vector<unsigned char> data = zip.readBinary(path);
    if (!data.empty()) return data;

    const std::string normalized = normalizeZipPath(path);
    const std::string lowerTarget = to_lower_copy(normalized);
    for (const auto& entry : zip.entries()) {
        const std::string lowerEntry = to_lower_copy(normalizeZipPath(entry.name));
        if (lowerEntry == lowerTarget || ends_with(lowerEntry, "/" + lowerTarget)) {
            data = zip.readBinary(entry.name);
            if (!data.empty()) return data;
        }
    }
    return {};
}

std::string inlineChapterImages(ZipArchive& zip, const std::string& html, const std::string& chapterPath) {
    std::regex imgSrcRegex(R"((<img\b[^>]*?\bsrc\s*=\s*["'])([^"']+)(["'][^>]*>))", std::regex::icase);
    std::string result;
    result.reserve(html.size());

    const std::string chapterDir = dirName(chapterPath);
    size_t last = 0;
    auto begin = std::sregex_iterator(html.begin(), html.end(), imgSrcRegex);
    auto end = std::sregex_iterator();
    for (auto it = begin; it != end; ++it) {
        const std::smatch& match = *it;
        const std::string src = match[2].str();
        std::string replacementSrc = src;
        const std::string lowerSrc = to_lower_copy(src);

        if (!lowerSrc.empty() &&
            lowerSrc.find("data:") != 0 &&
            lowerSrc.find("http://") != 0 &&
            lowerSrc.find("https://") != 0) {
            std::string cleanSrc = src;
            const size_t fragment = cleanSrc.find('#');
            if (fragment != std::string::npos) cleanSrc = cleanSrc.substr(0, fragment);
            const size_t query = cleanSrc.find('?');
            if (query != std::string::npos) cleanSrc = cleanSrc.substr(0, query);

            const std::vector<std::string> candidates = {
                normalizeZipPath(chapterDir.empty() ? cleanSrc : chapterDir + "/" + cleanSrc),
                normalizeZipPath(cleanSrc),
                normalizeZipPath(cleanSrc.substr(cleanSrc.find_last_of('/') == std::string::npos ? 0 : cleanSrc.find_last_of('/') + 1)),
            };

            for (const auto& candidate : candidates) {
                const auto data = readZipBinaryLoose(zip, candidate);
                const std::string dataUri = image_data_uri(candidate, data);
                if (!dataUri.empty()) {
                    replacementSrc = dataUri;
                    break;
                }
            }
        }

        result.append(html, last, static_cast<size_t>(match.position()) - last);
        result += match[1].str();
        result += replacementSrc;
        result += match[3].str();
        last = static_cast<size_t>(match.position() + match.length());
    }

    result.append(html, last, std::string::npos);
    return result;
}

EpubMetadata parseOpfMetadata(const std::string& opfXml) {
    EpubMetadata meta;
    meta.title = stripTags(extractTag(opfXml, "dc:title"));
    if (meta.title.empty()) meta.title = stripTags(extractTag(opfXml, "title"));
    meta.author = stripTags(extractTag(opfXml, "dc:creator"));
    if (meta.author.empty()) meta.author = stripTags(extractTag(opfXml, "creator"));
    meta.description = stripTags(extractTag(opfXml, "dc:description"));
    if (meta.description.empty()) meta.description = stripTags(extractTag(opfXml, "description"));
    meta.language = stripTags(extractTag(opfXml, "dc:language"));
    if (meta.language.empty()) meta.language = stripTags(extractTag(opfXml, "language"));
    meta.publisher = stripTags(extractTag(opfXml, "dc:publisher"));
    if (meta.publisher.empty()) meta.publisher = stripTags(extractTag(opfXml, "publisher"));
    meta.publishDate = stripTags(extractTag(opfXml, "dc:date"));
    if (meta.publishDate.empty()) meta.publishDate = stripTags(extractTag(opfXml, "date"));

    std::regex idRegex(R"(<dc:identifier[^>]*>([^<]+)</dc:identifier>)");
    std::smatch m;
    if (std::regex_search(opfXml, m, idRegex)) {
        meta.identifier = m[1].str();
    }

    std::regex subjRegex(R"(<dc:subject[^>]*>([^<]+)</dc:subject>)");
    auto begin = std::sregex_iterator(opfXml.begin(), opfXml.end(), subjRegex);
    auto end = std::sregex_iterator();
    for (auto it = begin; it != end; ++it) {
        meta.subjects.push_back((*it)[1].str());
    }
    return meta;
}

std::vector<EpubChapter> parseChapters(ZipArchive& zip, const std::string& opfXml, const std::string& opfDir) {
    std::vector<EpubChapter> chapters;
    std::map<std::string, std::string> manifestMap;

    std::regex itemRegex(R"(<item\b([^>]*)>)", std::regex::icase);
    auto begin = std::sregex_iterator(opfXml.begin(), opfXml.end(), itemRegex);
    auto end = std::sregex_iterator();
    for (auto it = begin; it != end; ++it) {
        std::string attrs = (*it)[1].str();
        std::string id = extractAttr(attrs, "id");
        std::string href = extractAttr(attrs, "href");
        std::string mediaType = extractAttr(attrs, "media-type");
        if (!id.empty() && !href.empty() &&
            (mediaType.empty() || mediaType.find("xhtml") != std::string::npos || mediaType.find("html") != std::string::npos)) {
            manifestMap[id] = href;
        }
    }

    std::regex spineRefRegex(R"(<itemref\b([^>]*)>)", std::regex::icase);
    auto spineBegin = std::sregex_iterator(opfXml.begin(), opfXml.end(), spineRefRegex);
    auto spineEnd = std::sregex_iterator();

    int order = 0;
    for (auto it = spineBegin; it != spineEnd; ++it) {
        std::string idref = extractAttr((*it)[1].str(), "idref");
        auto manifestIt = manifestMap.find(idref);
        if (manifestIt == manifestMap.end()) continue;

        std::string href = manifestIt->second;
        std::string fullPath = opfDir.empty() ? href : opfDir + "/" + href;

        std::string content = readZipTextLoose(zip, fullPath);
        if (content.empty()) continue;
        content = inlineChapterImages(zip, content, fullPath);

        std::string title = stripTags(extractTag(content, "title"));
        if (title.empty()) {
            std::regex h1Regex(R"(<h[1-6][^>]*>([^<]+)</h[1-6]>)");
            std::smatch hm;
            if (std::regex_search(content, hm, h1Regex)) {
                title = hm[1].str();
            } else {
                title = "Chapter " + std::to_string(order + 1);
            }
        }

        std::string text = stripTags(content);
        int wordCount = 0;
        bool inWord = false;
        for (char c : text) {
            if (std::isalnum(static_cast<unsigned char>(c))) {
                if (!inWord) { inWord = true; wordCount++; }
            } else {
                inWord = false;
            }
        }

        EpubChapter ch;
        ch.id = idref;
        ch.title = title;
        ch.href = href;
        ch.order = order++;
        ch.content = content;
        ch.wordCount = wordCount;
        chapters.push_back(ch);
    }
    return chapters;
}

std::string escapeJson(const std::string& s) {
    std::string out;
    out.reserve(s.size());
    for (char c : s) {
        switch (c) {
            case '"':  out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:   out += c;
        }
    }
    return out;
}

std::string toJson(const ParsedEpub& epub) {
    std::ostringstream json;
    json << "{";
    json << "\"metadata\":{";
    json << "\"title\":\"" << escapeJson(epub.metadata.title) << "\",";
    json << "\"author\":\"" << escapeJson(epub.metadata.author) << "\",";
    json << "\"description\":\"" << escapeJson(epub.metadata.description) << "\",";
    json << "\"language\":\"" << escapeJson(epub.metadata.language) << "\",";
    json << "\"publisher\":\"" << escapeJson(epub.metadata.publisher) << "\",";
    json << "\"identifier\":\"" << escapeJson(epub.metadata.identifier) << "\",";
    json << "\"publishDate\":\"" << escapeJson(epub.metadata.publishDate) << "\",";
    json << "\"subjects\":[";
    for (size_t i = 0; i < epub.metadata.subjects.size(); i++) {
        if (i > 0) json << ",";
        json << "\"" << escapeJson(epub.metadata.subjects[i]) << "\"";
    }
    json << "]";
    json << "},";
    json << "\"chapters\":[";
    for (size_t i = 0; i < epub.chapters.size(); i++) {
        if (i > 0) json << ",";
        const auto& ch = epub.chapters[i];
        json << "{";
        json << "\"id\":\"" << escapeJson(ch.id) << "\",";
        json << "\"title\":\"" << escapeJson(ch.title) << "\",";
        json << "\"href\":\"" << escapeJson(ch.href) << "\",";
        json << "\"order\":" << ch.order << ",";
        json << "\"wordCount\":" << ch.wordCount;
        json << "}";
    }
    json << "],";
    json << "\"totalChapters\":" << epub.totalChapters << ",";
    json << "\"extractedCss\":\"" << escapeJson(epub.extractedCss) << "\"";
    json << "}";
    return json.str();
}

} // anonymous namespace

std::string parseEpub(const std::string& filePath, CacheManager& cache) {
    if (auto cached = cache.get(filePath)) {
        LOGI("Cache hit for %s", filePath.c_str());
        return toJson(cached->parsed);
    }

    LOGI("Parsing EPUB: %s", filePath.c_str());

    ZipArchive zip;
    if (!zip.open(filePath)) {
        LOGE("Failed to open EPUB ZIP: %s", filePath.c_str());
        return "{}";
    }

    // Find OPF via container.xml
    std::string containerXml = readZipTextLoose(zip, "META-INF/container.xml");
    if (containerXml.empty()) {
        LOGE("No META-INF/container.xml in EPUB");
        return "{}";
    }

    std::regex pathRegex(R"(full-path=\"([^\"]+)\")");
    std::smatch match;
    if (!std::regex_search(containerXml, match, pathRegex)) {
        LOGE("No rootfile in container.xml");
        return "{}";
    }
    std::string opfPath = match[1].str();
    LOGI("OPF path: %s", opfPath.c_str());

    // Determine OPF directory
    size_t lastSlash = opfPath.find_last_of('/');
    std::string opfDir = (lastSlash != std::string::npos) ? opfPath.substr(0, lastSlash) : "";

    std::string opfXml = readZipTextLoose(zip, opfPath);
    if (opfXml.empty()) {
        LOGE("Failed to read OPF: %s", opfPath.c_str());
        return "{}";
    }

    ParsedEpub epub;
    epub.metadata = parseOpfMetadata(opfXml);
    epub.chapters = parseChapters(zip, opfXml, opfDir);
    epub.totalChapters = static_cast<int>(epub.chapters.size());

    // Extract CSS from all CSS files in the ZIP
    std::ostringstream cssBuf;
    auto entries = zip.entries();
    for (const auto& e : entries) {
        if (e.name.size() >= 4) {
            std::string lower = e.name;
            std::transform(lower.begin(), lower.end(), lower.begin(), ::tolower);
            if (ends_with(lower, ".css")) {
                std::string css = readZipTextLoose(zip, e.name);
                if (!css.empty()) {
                    cssBuf << "/* " << e.name << " */\n" << css << "\n";
                }
            }
        }
    }
    epub.extractedCss = cssBuf.str();

    // Populate cache
    CacheManager::CachedData cd;
    cd.parsed = epub;
    cache.put(filePath, cd);

    LOGI("Parsed EPUB: title=%s, chapters=%d", epub.metadata.title.c_str(), epub.totalChapters);
    return toJson(epub);
}

TextMapping extractPlainTextWithMapping(const std::string& html) {
    TextMapping mapping;
    mapping.plainText.reserve(html.size());
    mapping.byteIndices.reserve(html.size());

    bool inTag = false;
    for (size_t i = 0; i < html.size(); ++i) {
        char c = html[i];
        if (c == '<') {
            inTag = true;
        } else if (c == '>') {
            inTag = false;
        } else if (!inTag) {
            mapping.plainText += c;
            mapping.byteIndices.push_back(i);
        }
    }
    return mapping;
}

std::string searchEpub(const std::string& filePath, CacheManager& cache, const std::string& query) {
    if (query.empty()) {
        return "[]";
    }

    // Ensure the EPUB is parsed and in cache
    parseEpub(filePath, cache);

    auto cached = cache.get(filePath);
    if (!cached) {
        return "[]";
    }

    std::string lowerQuery = query;
    std::transform(lowerQuery.begin(), lowerQuery.end(), lowerQuery.begin(), ::tolower);

    std::ostringstream json;
    json << "[";
    bool firstResult = true;

    for (const auto& ch : cached->parsed.chapters) {
        TextMapping mapping = extractPlainTextWithMapping(ch.content);

        std::string lowerText = mapping.plainText;
        std::transform(lowerText.begin(), lowerText.end(), lowerText.begin(), ::tolower);

        size_t pos = 0;
        while ((pos = lowerText.find(lowerQuery, pos)) != std::string::npos) {
            size_t endPos = pos + lowerQuery.length() - 1;
            int startHtmlIndex = mapping.byteIndices[pos];
            int endHtmlIndex = mapping.byteIndices[endPos];

            // Extract excerpt around the match
            int contextChars = 40;
            int startExcerpt = std::max(0, static_cast<int>(pos) - contextChars);
            int endExcerpt = std::min(static_cast<int>(mapping.plainText.length()) - 1, static_cast<int>(endPos) + contextChars);
            std::string excerpt = mapping.plainText.substr(startExcerpt, endExcerpt - startExcerpt + 1);

            if (!firstResult) json << ",";
            json << "{";
            json << "\"chapterId\":\"" << escapeJson(ch.id) << "\",";
            json << "\"chapterTitle\":\"" << escapeJson(ch.title) << "\",";
            json << "\"chapterIndex\":" << ch.order << ",";
            json << "\"matchStartHtmlIndex\":" << startHtmlIndex << ",";
            json << "\"matchEndHtmlIndex\":" << endHtmlIndex << ",";
            json << "\"excerpt\":\"" << escapeJson(excerpt) << "\"";
            json << "}";

            firstResult = false;
            pos += lowerQuery.length();
        }
    }

    json << "]";
    return json.str();
}

} // namespace epub
} // namespace miyu
