#include "epub_cover.h"
#include "zip_archive.h"
#include <android/log.h>
#include <algorithm>
#include <map>
#include <regex>
#include "../storage/cache_manager.h"

#define LOG_TAG "EPUB_COVER"

namespace miyu {
namespace epub {

namespace {

struct ManifestImage {
    std::string href;
    std::string mediaType;
    std::string properties;
};

std::string extractAttr(const std::string& attrs, const std::string& name) {
    std::regex attrRegex(name + R"(\s*=\s*["']([^"']+)["'])", std::regex::icase);
    std::smatch match;
    return std::regex_search(attrs, match, attrRegex) ? match[1].str() : "";
}

std::string normalizeZipPath(std::string path) {
    std::replace(path.begin(), path.end(), '\\', '/');
    while (!path.empty() && path.front() == '/') {
        path.erase(path.begin());
    }
    while (path.find("//") != std::string::npos) {
        path.replace(path.find("//"), 2, "/");
    }
    return path;
}

std::string joinPath(const std::string& dir, const std::string& href) {
    if (dir.empty()) return normalizeZipPath(href);
    return normalizeZipPath(dir + "/" + href);
}

std::string readZipTextLoose(ZipArchive& zip, const std::string& path) {
    std::string content = zip.readText(path);
    if (!content.empty()) return content;

    const std::string target = to_lower_copy(normalizeZipPath(path));
    for (const auto& entry : zip.entries()) {
        const std::string entryName = to_lower_copy(normalizeZipPath(entry.name));
        if (entryName == target || ends_with(entryName, "/" + target)) {
            content = zip.readText(entry.name);
            if (!content.empty()) return content;
        }
    }
    return "";
}

std::vector<unsigned char> readZipBinaryLoose(ZipArchive& zip, const std::string& path) {
    std::vector<unsigned char> data = zip.readBinary(path);
    if (!data.empty()) return data;

    const std::string target = to_lower_copy(normalizeZipPath(path));
    for (const auto& entry : zip.entries()) {
        const std::string entryName = to_lower_copy(normalizeZipPath(entry.name));
        if (entryName == target || ends_with(entryName, "/" + target)) {
            data = zip.readBinary(entry.name);
            if (!data.empty()) return data;
        }
    }
    return {};
}

std::string extractOpfPath(ZipArchive& zip) {
    const std::string containerXml = readZipTextLoose(zip, "META-INF/container.xml");
    if (containerXml.empty()) return "";

    std::regex rootfileRegex(R"(full-path\s*=\s*["']([^"']+)["'])", std::regex::icase);
    std::smatch match;
    return std::regex_search(containerXml, match, rootfileRegex) ? match[1].str() : "";
}

std::map<std::string, ManifestImage> parseImageManifest(const std::string& opfXml) {
    std::map<std::string, ManifestImage> manifest;
    std::regex itemRegex(R"(<item\b([^>]*)>)", std::regex::icase);
    auto begin = std::sregex_iterator(opfXml.begin(), opfXml.end(), itemRegex);
    auto end = std::sregex_iterator();
    for (auto it = begin; it != end; ++it) {
        const std::string attrs = (*it)[1].str();
        const std::string id = extractAttr(attrs, "id");
        const std::string href = extractAttr(attrs, "href");
        const std::string mediaType = extractAttr(attrs, "media-type");
        const std::string properties = extractAttr(attrs, "properties");
        if (!id.empty() && !href.empty() && to_lower_copy(mediaType).find("image/") == 0) {
            manifest[id] = ManifestImage{href, mediaType, properties};
        }
    }
    return manifest;
}

std::string loadManifestImage(ZipArchive& zip, const std::string& opfDir, const ManifestImage& image) {
    const std::vector<std::string> candidates = {
        joinPath(opfDir, image.href),
        normalizeZipPath(image.href),
        normalizeZipPath(image.href.substr(image.href.find_last_of('/') == std::string::npos ? 0 : image.href.find_last_of('/') + 1)),
    };

    for (const auto& candidate : candidates) {
        const auto data = readZipBinaryLoose(zip, candidate);
        const std::string uri = image_data_uri(candidate, data);
        if (!uri.empty()) return uri;
    }
    return "";
}

std::string extractCoverFromOpf(ZipArchive& zip) {
    const std::string opfPath = extractOpfPath(zip);
    if (opfPath.empty()) return "";

    const std::string opfXml = readZipTextLoose(zip, opfPath);
    if (opfXml.empty()) return "";

    const size_t lastSlash = opfPath.find_last_of('/');
    const std::string opfDir = lastSlash == std::string::npos ? "" : opfPath.substr(0, lastSlash);
    const auto manifest = parseImageManifest(opfXml);

    for (const auto& [_, image] : manifest) {
        if (to_lower_copy(image.properties).find("cover-image") != std::string::npos) {
            const std::string uri = loadManifestImage(zip, opfDir, image);
            if (!uri.empty()) return uri;
        }
    }

    std::regex coverMetaRegex(R"(<meta\b[^>]*(?:name\s*=\s*["']cover["'][^>]*content\s*=\s*["']([^"']+)["']|content\s*=\s*["']([^"']+)["'][^>]*name\s*=\s*["']cover["'])[^>]*>)", std::regex::icase);
    std::smatch coverMeta;
    if (std::regex_search(opfXml, coverMetaRegex) && coverMeta.size() > 1) {
        const std::string coverId = coverMeta[1].matched ? coverMeta[1].str() : coverMeta[2].str();
        const auto it = manifest.find(coverId);
        if (it != manifest.end()) {
            const std::string uri = loadManifestImage(zip, opfDir, it->second);
            if (!uri.empty()) return uri;
        }
    }

    for (const auto& [id, image] : manifest) {
        const std::string lowerKey = to_lower_copy(id + " " + image.href);
        if (lowerKey.find("cover") != std::string::npos) {
            const std::string uri = loadManifestImage(zip, opfDir, image);
            if (!uri.empty()) return uri;
        }
    }

    return "";
}

} // anonymous namespace

std::string extractCover(const std::string& filePath, CacheManager& cache) {
    if (auto cached = cache.get(filePath); cached && !cached->coverBase64.empty()) {
        return cached->coverBase64;
    }

    ZipArchive zip;
    if (!zip.open(filePath)) return "";

    std::string result = extractCoverFromOpf(zip);
    if (!result.empty()) {
        cache.update(filePath, [&](CacheManager::CachedData& cached) {
            cached->coverBase64 = result;
        });
        return result;
    }

    // Final fallback: scan for any image file with "cover" in its path.
    for (const auto& e : zip.entries()) {
        std::string lower = to_lower_copy(e.name);
        if (lower.find("cover") != std::string::npos &&
            (ends_with(lower, ".jpg") || ends_with(lower, ".jpeg") ||
             ends_with(lower, ".png") || ends_with(lower, ".gif") || ends_with(lower, ".webp"))) {
            const auto imgData = zip.readBinary(e.name);
            result = image_data_uri(e.name, imgData);
            break;
        }
    }

    if (result.empty()) return "";

    cache.update(filePath, [&](CacheManager::CachedData& cached) {
        cached->coverBase64 = result;
    });
    return result;
}

} // namespace epub
} // namespace miyu
