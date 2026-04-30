#pragma once

#include <string>
#include <vector>

namespace miyu {
namespace epub {

// C++17-compatible ends_with (std::string::ends_with is C++20)
inline bool ends_with(const std::string& str, const std::string& suffix) {
    return str.size() >= suffix.size() &&
           str.compare(str.size() - suffix.size(), suffix.size(), suffix) == 0;
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