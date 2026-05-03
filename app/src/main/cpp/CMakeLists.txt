cmake_minimum_required(VERSION 3.22.1)
project("miyu_engine")

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Include directories
include_directories(
    ${CMAKE_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/epub
    ${CMAKE_SOURCE_DIR}/storage
)

# Source files
file(GLOB_RECURSE ENGINE_SOURCES
    "epub/*.cpp"
    "storage/*.cpp"
    "jni_bridge.cpp"
    "zip_archive.cpp"
)

add_library(
    miyu_engine
    SHARED
    ${ENGINE_SOURCES}
)

# miniz library
add_library(
    miniz
    STATIC
    miniz.c
)
target_include_directories(miniz PUBLIC ${CMAKE_SOURCE_DIR})

# Find system libraries
find_library(log-lib log)
find_library(z-lib z)

# Link libraries
target_link_libraries(
    miyu_engine
    miniz
    ${log-lib}
    ${z-lib}
)
