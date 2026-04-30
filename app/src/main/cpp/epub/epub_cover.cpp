#include "epub_cover.h"
#include "zip_archive.h"
#include <android/log.h>
#include <algorithm>
#include "../storage/cache_manager.h"

#define LOG_TAG "EPUB_COVER"

namespace miyu {
namespace epub {

std::string extractCover(const std::string& filePath, CacheManager& cache) {
    if (auto* cached = cache.get(filePath); cached && !cached->coverBase64.empty()) {
        return cached->coverBase64;
    }

    ZipArchive zip;
    if (!zip.open(filePath)) return "";

    // Scan for cover image
    std::string coverPath;
    for (const auto& e : zip.entries()) {
        std::string lower = e.name;
        std::transform(lower.begin(), lower.end(), lower.begin(), ::tolower);
        if (lower.find("cover") != std::string::npos &&
            (ends_with(lower, ".jpg") || ends_with(lower, ".jpeg") ||
             ends_with(lower, ".png") || ends_with(lower, ".gif"))) {
            coverPath = e.name;
            break;
        }
    }

    if (coverPath.empty()) return "";

    auto imgData = zip.readBinary(coverPath);
    if (imgData.empty()) return "";

    // Base64 encode
    static const char* chars =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string result;
    result.reserve(((imgData.size() + 2) / 3) * 4);
    for (size_t i = 0; i < imgData.size(); i += 3) {
        uint32_t triple = static_cast<uint32_t>(imgData[i]) << 16;
        int bytes = 1;
        if (i + 1 < imgData.size()) { triple |= static_cast<uint32_t>(imgData[i+1]) << 8; bytes++; }
        if (i + 2 < imgData.size()) { triple |= static_cast<uint32_t>(imgData[i+2]); bytes++; }
        for (int j = 3; j >= 0; j--) {
            int idx = j <= bytes ? (triple >> (6 * j)) & 0x3F : 64;
            result += (idx < 64) ? chars[idx] : '=';
        }
    }

    // Update cache
    if (auto* cached = const_cast<CacheManager::CachedData*>(cache.get(filePath))) {
        cached->coverBase64 = result;
    }
    return result;
}

} // namespace epub
} // namespace miyu
