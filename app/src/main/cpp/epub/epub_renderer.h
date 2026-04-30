#pragma once

#include <string>
#include <map>
#include "epub_types.h"

namespace miyu {
class CacheManager;

namespace epub {

std::string renderChapter(
    const std::string& filePath,
    int chapterIndex,
    const std::map<std::string, std::string>& termReplacements,
    CacheManager& cache);

int countChapterWords(
    const std::string& filePath,
    int chapterIndex,
    CacheManager& cache);

} // namespace epub
} // namespace miyu