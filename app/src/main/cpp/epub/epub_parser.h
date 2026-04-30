#pragma once

#include <string>
#include "epub_types.h"

namespace miyu {
class CacheManager;

namespace epub {

/**
 * Parse a full EPUB from [filePath] into a JSON string representation.
 * Uses CacheManager to avoid re-parsing on subsequent calls.
 */
std::string parseEpub(const std::string& filePath, CacheManager& cache);

} // namespace epub
} // namespace miyu