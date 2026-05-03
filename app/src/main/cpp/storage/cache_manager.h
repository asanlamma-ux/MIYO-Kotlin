#pragma once

#include <string>
#include <unordered_map>
#include <mutex>
#include <memory>
#include "epub/epub_types.h"

namespace miyu {

class CacheManager {
public:
    struct CachedData {
        epub::ParsedEpub parsed;
        std::string coverBase64;
        std::string mergedCss;
    };

    using Snapshot = std::shared_ptr<const CachedData>;

    Snapshot get(const std::string& filePath) const {
        std::lock_guard<std::mutex> lock(mutex_);
        auto it = cache_.find(filePath);
        return (it != cache_.end()) ? it->second : nullptr;
    }

    void put(const std::string& filePath, CachedData data) {
        std::lock_guard<std::mutex> lock(mutex_);
        cache_[filePath] = std::make_shared<const CachedData>(std::move(data));
    }

    bool contains(const std::string& filePath) const {
        std::lock_guard<std::mutex> lock(mutex_);
        return cache_.find(filePath) != cache_.end();
    }

    template <typename Mutator>
    bool update(const std::string& filePath, Mutator&& mutator) {
        std::lock_guard<std::mutex> lock(mutex_);
        auto it = cache_.find(filePath);
        if (it == cache_.end()) return false;
        auto copy = std::make_shared<CachedData>(*it->second);
        mutator(*copy);
        it->second = std::const_pointer_cast<const CachedData>(copy);
        return true;
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
    std::unordered_map<std::string, Snapshot> cache_;
    mutable std::mutex mutex_;
};

} // namespace miyu
