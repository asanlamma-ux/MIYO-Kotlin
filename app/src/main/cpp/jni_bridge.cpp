/**
 * JNI implementation for the MIYU ebook engine bridge.
 * Connects Java/Kotlin (com.miyu.reader.engine.bridge.EpubEngineBridge) to the C++ parsing layer.
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <map>

#include "epub/epub_parser.h"
#include "epub/epub_cover.h"
#include "epub/epub_css.h"
#include "epub/epub_renderer.h"
#include "storage/cache_manager.h"

#define LOG_TAG "MIYU_ENGINE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace miyu {

// Global cache manager instance
static CacheManager g_cache_manager;

// Convert a Java String-to-String Map to std::map
std::map<std::string, std::string> javaMapToCpp(JNIEnv* env, jobject map) {
    std::map<std::string, std::string> result;
    if (!map) return result;

    jclass mapClass = env->GetObjectClass(map);
    jmethodID entrySet = env->GetMethodID(mapClass, "entrySet", "()Ljava/util/Set;");
    jobject set = env->CallObjectMethod(map, entrySet);

    jclass setClass = env->GetObjectClass(set);
    jmethodID iterator = env->GetMethodID(setClass, "iterator", "()Ljava/util/Iterator;");
    jobject iter = env->CallObjectMethod(set, iterator);

    jclass iteratorClass = env->GetObjectClass(iter);
    jmethodID hasNext = env->GetMethodID(iteratorClass, "hasNext", "()Z");
    jmethodID next = env->GetMethodID(iteratorClass, "next", "()Ljava/lang/Object;");

    jclass entryClass = env->FindClass("java/util/Map$Entry");
    jmethodID getKey = env->GetMethodID(entryClass, "getKey", "()Ljava/lang/Object;");
    jmethodID getValue = env->GetMethodID(entryClass, "getValue", "()Ljava/lang/Object;");

    while (env->CallBooleanMethod(iter, hasNext)) {
        jobject entry = env->CallObjectMethod(iter, next);
        jstring jkey = (jstring)env->CallObjectMethod(entry, getKey);
        jstring jvalue = (jstring)env->CallObjectMethod(entry, getValue);

        const char* ckey = env->GetStringUTFChars(jkey, nullptr);
        const char* cvalue = env->GetStringUTFChars(jvalue, nullptr);

        result[ckey] = cvalue;

        env->ReleaseStringUTFChars(jkey, ckey);
        env->ReleaseStringUTFChars(jvalue, cvalue);
    }

    return result;
}

} // namespace miyu

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_miyu_reader_engine_bridge_EpubEngineBridge_parseEpub(
    JNIEnv* env, jobject /* this */, jstring filePath) {
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    LOGI("parseEpub: %s", path);

    std::string result = miyu::epub::parseEpub(path, miyu::g_cache_manager);

    env->ReleaseStringUTFChars(filePath, path);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_miyu_reader_engine_bridge_EpubEngineBridge_extractCoverImage(
    JNIEnv* env, jobject /* this */, jstring filePath) {
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    LOGI("extractCoverImage: %s", path);

    std::string cover = miyu::epub::extractCover(path, miyu::g_cache_manager);

    env->ReleaseStringUTFChars(filePath, path);
    if (cover.empty()) return nullptr;
    return env->NewStringUTF(cover.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_miyu_reader_engine_bridge_EpubEngineBridge_extractStylesheet(
    JNIEnv* env, jobject /* this */, jstring filePath) {
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    LOGI("extractStylesheet: %s", path);

    std::string css = miyu::epub::extractCss(path, miyu::g_cache_manager);

    env->ReleaseStringUTFChars(filePath, path);
    return env->NewStringUTF(css.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_miyu_reader_engine_bridge_EpubEngineBridge_renderChapter(
    JNIEnv* env, jobject /* this */,
    jstring filePath, jint chapterIndex, jobject termReplacements) {
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    auto terms = miyu::javaMapToCpp(env, termReplacements);

    LOGI("renderChapter: %s chapter=%d terms=%zu", path, chapterIndex, terms.size());

    std::string html = miyu::epub::renderChapter(
        path, static_cast<int>(chapterIndex), terms, miyu::g_cache_manager);

    env->ReleaseStringUTFChars(filePath, path);
    return env->NewStringUTF(html.c_str());
}

JNIEXPORT jint JNICALL
Java_com_miyu_reader_engine_bridge_EpubEngineBridge_countChapterWords(
    JNIEnv* env, jobject /* this */, jstring filePath, jint chapterIndex) {
    const char* path = env->GetStringUTFChars(filePath, nullptr);

    int count = miyu::epub::countChapterWords(
        path, static_cast<int>(chapterIndex), miyu::g_cache_manager);

    env->ReleaseStringUTFChars(filePath, path);
    return count;
}

JNIEXPORT void JNICALL
Java_com_miyu_reader_engine_bridge_EpubEngineBridge_evictCache(
    JNIEnv* env, jobject /* this */, jstring filePath) {
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    LOGI("evictCache: %s", path);

    miyu::g_cache_manager.evict(path);

    env->ReleaseStringUTFChars(filePath, path);
}

JNIEXPORT jstring JNICALL
Java_com_miyu_reader_engine_bridge_EpubEngineBridge_searchInBook(
    JNIEnv* env, jobject /* this */, jstring filePath, jstring query) {
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    const char* q = env->GetStringUTFChars(query, nullptr);
    LOGI("searchInBook: %s query=%s", path, q);

    std::string jsonResult = miyu::epub::searchEpub(path, miyu::g_cache_manager, q);

    env->ReleaseStringUTFChars(filePath, path);
    env->ReleaseStringUTFChars(query, q);

    return env->NewStringUTF(jsonResult.c_str());
}

} // extern "C"
