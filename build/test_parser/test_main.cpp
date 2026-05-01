
#include <iostream>
#include <string>
#include "epub_parser.h"
#include "epub_cover.h"
#include "../storage/cache_manager.h"

using namespace miyu::epub;

int main(int argc, char** argv) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " <epub_path> <query>" << std::endl;
        return 1;
    }

    std::string epubPath = argv[1];
    std::string query = argv[2];

    miyu::CacheManager cache;
    std::string parsed = parseEpub(epubPath, cache);
    std::string json = searchEpub(epubPath, cache, query);
    std::string cover = extractCover(epubPath, cache);
    
    std::cout << parsed << std::endl;
    std::cout << json << std::endl;
    std::cout << "cover-bytes=" << cover.size() << std::endl;
    return 0;
}
