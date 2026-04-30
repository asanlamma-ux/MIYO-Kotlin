#pragma once
// Lightweight ZIP reading wrapper around miniz
// EPUB files are ZIP archives, and this provides
// the zip_open/read/close API the epub parser expects.

#include <string>
#include <vector>
#include <memory>

#define MINIZ_HEADER_FILE_ONLY
#include "miniz.h"

namespace miyu {

struct mz_zip_archive_tag : public mz_zip_archive {};

class ZipArchive {
public:
    struct Entry {
        std::string name;
        size_t uncompressedSize = 0;
        bool isDirectory = false;
    };

    ZipArchive();
    ~ZipArchive();

    bool open(const std::string& path);
    void close();

    // Read entire entry by path into string
    std::string readText(const std::string& path);
    std::vector<unsigned char> readBinary(const std::string& path);

    // List all entries (for scanning)
    std::vector<Entry> entries();

    bool isOpen() const { return open_; }

private:
    std::unique_ptr<mz_zip_archive_tag> archive_;
    bool open_ = false;
};

} // namespace miyu