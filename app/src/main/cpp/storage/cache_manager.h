#pragma once

#include <string>
#include <unordered_map>
#include <mutex>
#include "epub/epub_types.h"

namespace miyu {

class CacheManager {
public:
    struct CachedData {
        epub::ParsedEpub parsed;
        std::string coverBase64;
        std::string mergedCss;
    };

    const CachedData* get(const std::string& filePath) const {
        std::lock_guard<std::mutex> lock(mutex_);
        auto it = cache_.find(filePath);
        return (it != cache_.end()) ? &it->second : nullptr;
    }

    void put(const std::string& filePath, const CachedData& data) {
        std::lock_guard<std::mutex> lock(mutex_);
        cache_[filePath] = data;
    }

    bool contains(const std::string& filePath) const {
        std::lock_guard<std::mutex> lock(mutex_);
        return cache_.find(filePath) != cache_.end();
    }

    void evict(const std::string& filePath) {
        std::lock_guard<std::mutex> lock(mutex_);
        cache_.erase(filePath);
    }

    void clear() {
        std::lock_guard<std::mutex> lock(mutex_);
        cache_.clear();
    }

private:
    std::unordered_map<std::string, CachedData> cache_;
    mutable std::mutex mutex_;
};

} // namespace miyu