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

EpubMetadata parseOpfMetadata(const std::string& opfXml) {
    EpubMetadata meta;
    meta.title = stripTags(extractTag(opfXml, "dc:title"));
    meta.author = stripTags(extractTag(opfXml, "dc:creator"));
    meta.description = stripTags(extractTag(opfXml, "dc:description"));
    meta.language = stripTags(extractTag(opfXml, "dc:language"));
    meta.publisher = stripTags(extractTag(opfXml, "dc:publisher"));
    meta.publishDate = stripTags(extractTag(opfXml, "dc:date"));

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

    std::regex itemRegex(R"(<item\s[^>]*id=\"([^\"]+)\"[^>]*href=\"([^\"]+)\")");
    auto begin = std::sregex_iterator(opfXml.begin(), opfXml.end(), itemRegex);
    auto end = std::sregex_iterator();
    for (auto it = begin; it != end; ++it) {
        manifestMap[(*it)[1].str()] = (*it)[2].str();
    }

    std::regex spineRefRegex(R"(<itemref\s[^>]*idref=\"([^\"]+)\")");
    auto spineBegin = std::sregex_iterator(opfXml.begin(), opfXml.end(), spineRefRegex);
    auto spineEnd = std::sregex_iterator();

    int order = 0;
    for (auto it = spineBegin; it != spineEnd; ++it) {
        std::string idref = (*it)[1].str();
        auto manifestIt = manifestMap.find(idref);
        if (manifestIt == manifestMap.end()) continue;

        std::string href = manifestIt->second;
        std::string fullPath = opfDir.empty() ? href : opfDir + "/" + href;

        std::string content = zip.readText(fullPath);
        if (content.empty()) continue;

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
    if (auto* cached = cache.get(filePath)) {
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
    std::string containerXml = zip.readText("META-INF/container.xml");
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

    std::string opfXml = zip.readText(opfPath);
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
                std::string css = zip.readText(e.name);
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

} // namespace epub
} // namespace miyu