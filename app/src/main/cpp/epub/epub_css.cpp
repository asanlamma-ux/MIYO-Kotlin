#include "epub_css.h"
#include "zip_archive.h"
#include <android/log.h>
#include <sstream>
#include <set>
#include <algorithm>
#include "../storage/cache_manager.h"

#define LOG_TAG "EPUB_CSS"

namespace miyu {
namespace epub {

std::string extractCss(const std::string& filePath, CacheManager& cache) {
    if (auto cached = cache.get(filePath)) {
        if (!cached->mergedCss.empty()) return cached->mergedCss;
    }

    ZipArchive zip;
    if (!zip.open(filePath)) return "";

    std::ostringstream cssBuf;
    for (const auto& e : zip.entries()) {
        std::string lower = e.name;
        std::transform(lower.begin(), lower.end(), lower.begin(), ::tolower);
        if (ends_with(lower, ".css")) {
            std::string css = zip.readText(e.name);
            if (!css.empty()) {
                cssBuf << "/* " << e.name << " */\n" << css << "\n";
            }
        }
    }

    std::string result = cssBuf.str();
    cache.update(filePath, [&](CacheManager::CachedData& cached) {
        cached.mergedCss = result;
    });
    return result;
}

} // namespace epub
} // namespace miyu
