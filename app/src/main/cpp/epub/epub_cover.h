#pragma once

#include <string>
#include "epub_types.h"

namespace miyu {
class CacheManager;

namespace epub {

std::string extractCover(const std::string& filePath, CacheManager& cache);

} // namespace epub
} // namespace miyu