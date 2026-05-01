
#include "android/log.h"
#include <stdarg.h>
extern "C" {
    int __android_log_print(int prio, const char *tag,  const char *fmt, ...) {
        va_list ap;
        va_start(ap, fmt);
        printf("[%s] ", tag);
        vprintf(fmt, ap);
        printf("\n");
        va_end(ap);
        return 0;
    }
}
