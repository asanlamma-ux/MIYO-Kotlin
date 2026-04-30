#pragma once

#include <string>
#include <vector>
#include "epub_types.h"

namespace miyu {
class CacheManager;

namespace epub {

/**
 * Parse a full EPUB from [filePath] into a JSON string representation.
 * Uses CacheManager to avoid re-parsing on subsequent calls.
 */
std::string parseEpub(const std::string& filePath, CacheManager& cache);

struct TextMapping {
    std::string plainText;
    std::vector<int> byteIndices; // Mapping from plain text index to HTML byte index
};

TextMapping extractPlainTextWithMapping(const std::string& html);

std::string searchEpub(const std::string& filePath, CacheManager& cache, const std::string& query);

} // namespace epub
} // namespace miyu
