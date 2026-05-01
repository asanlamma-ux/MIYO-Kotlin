
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
