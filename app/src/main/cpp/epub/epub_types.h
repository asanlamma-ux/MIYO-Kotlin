#pragma once

#include <string>
#include <vector>
#include <algorithm>
#include <cctype>
#include <cstdint>

namespace miyu {
namespace epub {

// C++17-compatible ends_with (std::string::ends_with is C++20)
inline bool ends_with(const std::string& str, const std::string& suffix) {
    return str.size() >= suffix.size() &&
           str.compare(str.size() - suffix.size(), suffix.size(), suffix) == 0;
}

inline std::string to_lower_copy(std::string value) {
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char c) {
        return static_cast<char>(std::tolower(c));
    });
    return value;
}

inline std::string image_mime_for_path(const std::string& path) {
    const std::string lower = to_lower_copy(path);
    if (ends_with(lower, ".png")) return "image/png";
    if (ends_with(lower, ".gif")) return "image/gif";
    if (ends_with(lower, ".webp")) return "image/webp";
    if (ends_with(lower, ".svg") || ends_with(lower, ".svgz")) return "image/svg+xml";
    return "image/jpeg";
}

inline std::string base64_encode(const std::vector<unsigned char>& data) {
    static const char* chars =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    std::string result;
    result.reserve(((data.size() + 2) / 3) * 4);

    for (size_t i = 0; i < data.size(); i += 3) {
        const int bytes = static_cast<int>(std::min<size_t>(3, data.size() - i));
        uint32_t triple = static_cast<uint32_t>(data[i]) << 16;
        if (bytes > 1) triple |= static_cast<uint32_t>(data[i + 1]) << 8;
        if (bytes > 2) triple |= static_cast<uint32_t>(data[i + 2]);

        result += chars[(triple >> 18) & 0x3F];
        result += chars[(triple >> 12) & 0x3F];
        result += bytes > 1 ? chars[(triple >> 6) & 0x3F] : '=';
        result += bytes > 2 ? chars[triple & 0x3F] : '=';
    }

    return result;
}

inline std::string image_data_uri(const std::string& path, const std::vector<unsigned char>& data) {
    if (data.empty()) return "";
    return "data:" + image_mime_for_path(path) + ";base64," + base64_encode(data);
}

struct EpubMetadata {
    std::string title;
    std::string author;
    std::string description;
    std::string language;
    std::string publisher;
    std::string identifier;
    std::string coverImageBase64;
    std::vector<std::string> subjects;
    std::string publishDate;
};

struct EpubChapter {
    std::string id;
    std::string title;
    std::string href;
    int order = 0;
    std::string content;
    int wordCount = 0;
};

struct ParsedEpub {
    EpubMetadata metadata;
    std::vector<EpubChapter> chapters;
    int totalChapters = 0;
    std::string extractedCss;
};

} // namespace epub
} // namespace miyu
