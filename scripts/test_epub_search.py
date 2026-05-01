#!/usr/bin/env python3

import os
import sys
import zipfile
import subprocess
import shutil

# Directories
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CPP_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "cpp")
BUILD_DIR = os.path.join(PROJECT_ROOT, "build", "test_parser")
TEST_EPUB = os.path.join(BUILD_DIR, "test.epub")

def create_test_epub():
    print(f"Creating test EPUB at {TEST_EPUB}...")
    os.makedirs(BUILD_DIR, exist_ok=True)
    
    with zipfile.ZipFile(TEST_EPUB, 'w') as zf:
        # mimetype
        zf.writestr('mimetype', 'application/epub+zip')
        
        # container.xml
        container_xml = """<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>"""
        zf.writestr('META-INF/container.xml', container_xml)
        
        # content.opf
        content_opf = """<?xml version="1.0"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="pub-id" version="2.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>Test Book</dc:title>
    <dc:creator>Parser Tester</dc:creator>
  </metadata>
  <manifest>
    <item id="ch1" href="ch1.html" media-type="application/xhtml+xml"/>
    <item id="cover" href="images/cover.png" media-type="image/png" properties="cover-image"/>
  </manifest>
  <spine>
    <itemref idref="ch1"/>
  </spine>
</package>"""
        zf.writestr('OEBPS/content.opf', content_opf)
        
        # ch1.html
        ch1_html = "<html><head><title>Opening</title></head><body><p>The <b>quick</b> fox</p><p>Jiu Xinnai arrived.</p><img src=\"images/inline.png\"/></body></html>"
        zf.writestr('OEBPS/ch1.html', ch1_html)
        zf.writestr('OEBPS/images/cover.png', b'fake-cover-bytes')
        zf.writestr('OEBPS/images/inline.png', b'fake-inline-image')

def write_cpp_test():
    cpp_code = """
#include <iostream>
#include <map>
#include <string>
#include "epub_parser.h"
#include "epub_cover.h"
#include "epub_renderer.h"
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
    std::string rendered = renderChapter(epubPath, 0, {}, cache);
    std::map<std::string, std::string> terms = {
        {"Jiu Xinnai", "<span class=\\\"miyu-term\\\">Red Apple</span>"},
        {"Xinnai", "Apple"}
    };
    std::string termRendered = renderChapter(epubPath, 0, terms, cache);
    
    std::cout << parsed << std::endl;
    std::cout << json << std::endl;
    std::cout << "cover-bytes=" << cover.size() << std::endl;
    std::cout << "cover-prefix=" << cover.substr(0, 14) << std::endl;
    std::cout << "render-has-inline-image=" << (rendered.find("data:image/png;base64,") != std::string::npos ? "yes" : "no") << std::endl;
    std::cout << "term-longest-replacement=" << (termRendered.find("miyu-term") != std::string::npos && termRendered.find("Red Apple") != std::string::npos && termRendered.find("Jiu Apple") == std::string::npos ? "yes" : "no") << std::endl;
    return 0;
}
"""
    with open(os.path.join(BUILD_DIR, "test_main.cpp"), "w") as f:
        f.write(cpp_code)

def build_test():
    print("Building C++ test binary...")
    
    # Create fake android/log.h
    android_dir = os.path.join(BUILD_DIR, "android")
    os.makedirs(android_dir, exist_ok=True)
    fake_log_h = """
#pragma once
#include <stdio.h>
#define ANDROID_LOG_INFO 1
#define ANDROID_LOG_ERROR 2
#define ANDROID_LOG_DEBUG 3
#ifdef __cplusplus
extern "C" {
#endif
    int __android_log_print(int prio, const char *tag,  const char *fmt, ...);
#ifdef __cplusplus
}
#endif
"""
    with open(os.path.join(android_dir, "log.h"), "w") as f:
        f.write(fake_log_h)

    fake_log_cpp = """
#include "android/log.h"
#include <stdarg.h>
extern "C" {
    int __android_log_print(int prio, const char *tag,  const char *fmt, ...) {
        va_list ap;
        va_start(ap, fmt);
        printf("[%s] ", tag);
        vprintf(fmt, ap);
        printf("\\n");
        va_end(ap);
        return 0;
    }
}
"""
    with open(os.path.join(BUILD_DIR, "fake_log.cpp"), "w") as f:
        f.write(fake_log_cpp)

    try:
        # Compile miniz as C
        subprocess.run(["gcc", "-c", "-o", "miniz.o", os.path.join(CPP_DIR, "miniz.c")], cwd=BUILD_DIR, check=True)
        
        # Compile C++ files
        sources = [
            "test_main.cpp",
            os.path.join(CPP_DIR, "epub", "epub_parser.cpp"),
            os.path.join(CPP_DIR, "epub", "epub_cover.cpp"),
            os.path.join(CPP_DIR, "epub", "epub_renderer.cpp"),
            os.path.join(CPP_DIR, "zip_archive.cpp"),
            "fake_log.cpp"
        ]
        
        includes = f"-I{CPP_DIR} -I{os.path.join(CPP_DIR, 'epub')} -I{os.path.join(CPP_DIR, 'storage')} -I{BUILD_DIR}"
        
        cmd = ["g++", "-std=c++17"] + includes.split() + ["-o", "test_parser"] + sources + ["miniz.o"]
        
        subprocess.run(cmd, cwd=BUILD_DIR, check=True)
        print("Build successful.")
    except subprocess.CalledProcessError as e:
        print(f"Build failed: {e}")
        sys.exit(1)

def run_test():
    print("Running test...")
    cmd = ["./test_parser", TEST_EPUB, "quick"]
    
    try:
        result = subprocess.run(cmd, cwd=BUILD_DIR, capture_output=True, text=True, check=True)
        print("Test output:")
        print(result.stdout)
        
        if (
            '"title":"Test Book"' in result.stdout and
            '"author":"Parser Tester"' in result.stdout and
            '"totalChapters":1' in result.stdout and
            "matchStartHtmlIndex" in result.stdout and
            '"matchStartHtmlIndex":57' in result.stdout and
            "cover-bytes=" in result.stdout and
            "cover-bytes=0" not in result.stdout
            and "cover-prefix=data:image/png" in result.stdout
            and "render-has-inline-image=yes" in result.stdout
            and "term-longest-replacement=yes" in result.stdout
        ):
            print("SUCCESS: The parser extracted metadata, chapter contents, search offsets, and cover data.")
            print("Test PASS.")
            return True
        else:
            print("FAILURE: Did not find expected index 22 in JSON output.")
            return False
    except subprocess.CalledProcessError as e:
        print(f"Test execution failed: {e}")
        print(e.stderr)
        return False

if __name__ == "__main__":
    create_test_epub()
    write_cpp_test()
    build_test()
    if not run_test():
        sys.exit(1)
