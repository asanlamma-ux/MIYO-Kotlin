#include "zip_archive.h"
#include <cstring>
#include <cstdio>

namespace miyu {

ZipArchive::ZipArchive() : archive_(std::make_unique<mz_zip_archive_tag>()) {
    std::memset(archive_.get(), 0, sizeof(mz_zip_archive));
}

ZipArchive::~ZipArchive() {
    close();
}

bool ZipArchive::open(const std::string& path) {
    if (open_) close();
    if (!mz_zip_reader_init_file(archive_.get(), path.c_str(), 0)) {
        return false;
    }
    open_ = true;
    return true;
}

void ZipArchive::close() {
    if (open_) {
        mz_zip_reader_end(archive_.get());
        open_ = false;
    }
}

std::string ZipArchive::readText(const std::string& path) {
    if (!open_) return {};
    size_t size = 0;
    void* data = mz_zip_reader_extract_file_to_heap(archive_.get(), path.c_str(), &size, 0);
    if (!data) return {};
    std::string result(static_cast<const char*>(data), size);
    mz_free(data);
    return result;
}

std::vector<unsigned char> ZipArchive::readBinary(const std::string& path) {
    if (!open_) return {};
    size_t size = 0;
    void* data = mz_zip_reader_extract_file_to_heap(archive_.get(), path.c_str(), &size, 0);
    if (!data) return {};
    std::vector<unsigned char> result(size);
    std::memcpy(result.data(), data, size);
    mz_free(data);
    return result;
}

std::vector<ZipArchive::Entry> ZipArchive::entries() {
    std::vector<Entry> result;
    if (!open_) return result;
    mz_uint numFiles = mz_zip_reader_get_num_files(archive_.get());
    for (mz_uint i = 0; i < numFiles; i++) {
        char name[512];
        mz_zip_archive_file_stat stat;
        if (!mz_zip_reader_file_stat(archive_.get(), i, &stat)) continue;
        Entry e;
        e.name = stat.m_filename;
        e.uncompressedSize = stat.m_uncomp_size;
        e.isDirectory = stat.m_is_directory != 0;
        result.push_back(e);
    }
    return result;
}

} // namespace miyu