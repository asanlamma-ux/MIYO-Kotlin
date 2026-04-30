# =============================================================================
# MIYU Android App - Comprehensive ProGuard/R8 Rules
# Version: 2.0.0
# Updated: 2024
# =============================================================================

# =============================================================================
# R8 OPTIMIZATION SETTINGS
# =============================================================================

# R8: Keep generic signatures for reflection/serialization
-keepattributes Signature

# R8: Keep exceptions for proper error handling
-keepattributes Exceptions

# R8: Keep inner classes (required by Compose, Kotlin)
-keepattributes InnerClasses

# R8: Keep enclosing method info (enables better obfuscation)
-keepattributes EnclosingMethod

# R8: Keep method parameters (for reflection frameworks)
-keepattributes MethodParameters

# R8: Keep deprecation info
-keepattributes Deprecated

# =============================================================================
# GENERAL RULES
# =============================================================================

# Keep the Application class and its methods
-keep public class * extends android.app.Application { *; }

# Keep MainActivity with native methods
-keep class com.miyu.reader.MainActivity {
    native <methods>;
    public *;
}

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep native library loading classes
-keep class com.miyu.reader.engine.bridge.** {
    native <methods>;
    *;
}

# Keep custom exceptions
-keep public class * extends java.lang.Exception
-keep public class * extends java.lang.RuntimeException

# Keep annotations
-keepattributes *Annotation*

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable implementations
-keep class * implements java.io.Serializable { *; }

# Keep R classes (resources)
-keepclassmembers class **.R$* {
    public static <fields>;
}

# =============================================================================
# ANDROIDX & COMPOSE
# =============================================================================

# AndroidX Core
-keep class androidx.core.** { *; }
-dontwarn androidx.core.**

# AndroidX Lifecycle
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class androidx.lifecycle.** { *; }

# AndroidX Activity
-keep class androidx.activity.** { *; }
-dontwarn androidx.activity.**

# Compose Compiler
-keep class androidx.compose.compiler.** { *; }

# Compose Runtime
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }

# Compose UI
-keep class androidx.compose.ui.** { *; }
-keepclassmembers class androidx.compose.ui.** { *; }

# Compose Material
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep Compose Preview functions
-keepclasseswithmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}

# Keep @Composable functions
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# =============================================================================
# NAVIGATION COMPOSE
# =============================================================================

-keep class androidx.navigation.** { *; }
-keepclassmembers class * {
    @androidx.navigation.NavType <fields>;
}

# Keep Navigation arguments
-keepclassmembers class * {
    @androidx.navigation.NavDestination.Argument <fields>;
}

# Keep Hilt Navigation Compose
-keep class androidx.hilt.navigation.compose.** { *; }

# =============================================================================
# DATASTORE PREFERENCES
# =============================================================================

-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.Preferences {
    <init>(...);
}

# Keep DataStore serializers
-keepclassmembers class * implements androidx.datastore.core.Serializer {
    <init>(...);
}

# =============================================================================
# WEBKIT (WebView)
# =============================================================================

-keep class androidx.webkit.** { *; }
-keepclassmembers class * extends android.webkit.WebView {
    public *;
}

# Keep JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# =============================================================================
# HILT / DAGGER (DI)
# =============================================================================

# Keep Dagger/Hilt generated code
-keep class dagger.** { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Hilt modules
-keep class * extends dagger.hilt.internal.GeneratedComponent {
    <init>(...);
}

-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper {
    <init>(...);
    *;
}

# Keep Hilt entry points
-keep class * extends dagger.hilt.android.EntryPointAccessors {
    *;
}

# Keep @AndroidEntryPoint classes
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# Keep Hilt ViewModel factories
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModelFactory { *; }

# Keep assisted inject factories
-keep class * extends dagger.assisted.AssistedInject { *; }
-keep class * extends dagger.assisted.AssistedFactory { *; }

# Keep module bindings
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Provides class * { *; }
-keep @dagger.Binds class * { *; }

# Keep component builders
-keep class * extends dagger.Binder { *; }

# Keep generated component holder
-keep class dagger.hilt.android.internal.managers.ActivityComponentManager {
    *;
}

# =============================================================================
# ROOM (Database)
# =============================================================================

# Keep Room entities (full package protection)
-keep class com.miyu.reader.data.local.entity.** { *; }
-keep class com.miyu.reader.data.local.dao.** { *; }
-keep class com.miyu.reader.data.local.converter.** { *; }

# Keep Room database class
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
    *;
}

# Keep Room DAO interfaces
-keep interface * extends androidx.room.Dao { *; }

# Keep Room Entity classes
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.DatabaseView class * { *; }

# Keep TypeConverters
-keep @androidx.room.TypeConverter class * { *; }

# Keep Fts4/Fts3 entities
-keep @androidx.room.Fts4 class * { *; }
-keep @androidx.room.Fts3 class * { *; }

# Keep AutoMigration
-keep @androidx.room.AutoMigration class * { *; }

# Room warnings
-dontwarn androidx.room.paging.**

# =============================================================================
# KTOR CLIENT
# =============================================================================

-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }

# Keep Ktor client engine
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.client.engine.android.** { *; }

# Keep Ktor plugins
-keep class io.ktor.client.plugins.** { *; }
-keep class io.ktor.client.plugins.logging.** { *; }
-keep class io.ktor.client.plugins.contentnegotiation.** { *; }

# Keep Ktor serialization
-keep class io.ktor.serialization.** { *; }
-keep class io.ktor.serialization.kotlinx.** { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }

# Keep Ktor HTTP
-keep class io.ktor.http.** { *; }
-keep class io.ktor.util.** { *; }

# Keep Ktor utils
-keep class io.ktor.utils.io.** { *; }

# =============================================================================
# R8 FIX: Ktor + JVM-Only Classes Missing on Android (PRIORITY 1 - CRITICAL)
# =============================================================================

# Ktor's IntellijIdeaDebugDetector uses java.lang.management APIs
# that don't exist in Android (ManagementFactory, RuntimeMXBean, etc.)
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn java.lang.management.*
-dontwarn javax.management.**

# Additional JVM-only classes that may be referenced
-dontwarn java.lang.instrument.**
-dontwarn javax.tools.**

# Ktor debug utilities
-dontwarn io.ktor.util.debug.IntellijIdeaDebugDetector
-dontwarn io.ktor.util.debug.**

# =============================================================================
# SUPABASE
# =============================================================================

-keep class io.github.jan-tennert.supabase.** { *; }
-keepclassmembers class io.github.jan-tennert.supabase.** { *; }

# Supabase Auth
-keep class io.github.jan-tennert.supabase.gotrue.** { *; }

# Supabase PostgREST
-keep class io.github.jan-tennert.supabase.postgrest.** { *; }

# Supabase Storage
-keep class io.github.jan-tennert.supabase.storage.** { *; }

# Supabase Realtime
-keep class io.github.jan-tennert.supabase.realtime.** { *; }

# Keep Supabase error classes
-keep class io.github.jan-tennert.supabase.exceptions.** { *; }

# Keep Supabase types
-keep class io.github.jan-tennert.supabase.types.** { *; }

# =============================================================================
# KOTLIN SERIALIZATION
# =============================================================================

-keepattributes *Annotation*, InnerClasses
-keepattributes Signature
-keepattributes Exceptions

# Keep kotlinx.serialization annotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

# Keep serializer classes
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated serializers for app classes
-keep,includedescriptorclasses class com.miyu.reader.**$$serializer { *; }
-keepclassmembers class com.miyu.reader.** {
    *** Companion;
}
-keepclasseswithmembers class com.miyu.reader.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep SerializersModule
-keep class kotlinx.serialization.modules.** { *; }

# Keep PolymorphicSerializer
-keep class kotlinx.serialization.PolymorphicSerializer { *; }

# Don't warn about serialization internals
-dontnote kotlinx.serialization.AnnotationsKt

# =============================================================================
# COIL (Image Loading)
# =============================================================================

-keep class coil.** { *; }
-keepclassmembers class coil.** { *; }

# Coil Compose
-keep class coil.compose.** { *; }

# Coil request builders
-keep class coil.request.** { *; }

# Coil components
-keep class coil.decode.** { *; }
-keep class coil.fetch.** { *; }
-keep class coil.map.** { *; }
-keep class coil.size.** { *; }
-keep class coil.transform.** { *; }

# Keep ImageLoader
-keep class coil.ImageLoader { *; }

# Coil network
-keep class coil.network.** { *; }

# =============================================================================
# MMKV (Fast Key-Value Storage)
# =============================================================================

-keep class com.tencent.mmkv.** { *; }
-keepclassmembers class com.tencent.mmkv.** { *; }

# Keep MMKV handler
-keep class * implements com.tencent.mmkv.MMKVHandler { *; }
-keep class * implements com.tencent.mmkv.MMKVContentChangeNotification { *; }

# Keep native methods
-keepclasseswithmembernames class com.tencent.mmkv.* {
    native <methods>;
}

# =============================================================================
# ACCOMPANIST
# =============================================================================

-keep class com.google.accompanist.** { *; }
-keepclassmembers class com.google.accompanist.** { *; }

# System UI Controller
-keep class com.google.accompanist.systemuicontroller.** { *; }

# Permissions
-keep class com.google.accompanist.permissions.** { *; }

# Keep permission state
-keepclassmembers class * {
    @com.google.accompanist.permissions.ExperimentalPermissionsApi <methods>;
}

# =============================================================================
# LOTTIE
# =============================================================================

-keep class com.airbnb.lottie.** { *; }
-keepclassmembers class com.airbnb.lottie.** { *; }

# Lottie Compose
-keep class com.airbnb.lottie.compose.** { *; }

# Keep Lottie models
-keep class com.airbnb.lottie.model.** { *; }
-keep class com.airbnb.lottie.value.** { *; }
-keep class com.airbnb.lottie.parser.** { *; }
-keep class com.airbnb.lottie.animation.** { *; }
-keep class com.airbnb.lottie.network.** { *; }

# Keep Lottie composition cache
-keep class com.airbnb.lottie.LottieComposition { *; }
-keep class com.airbnb.lottie.LottieCompositionFactory { *; }

# Keep Lottie listeners
-keep interface com.airbnb.lottie.**$* { *; }

# Keep Lottie async updates
-keepclassmembers class com.airbnb.lottie.** {
    *** update**(...);
}

# =============================================================================
# KOTLIN COROUTINES
# =============================================================================

-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep CoroutineExceptionHandler
-keep class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# Keep Dispatchers
-keep class kotlinx.coroutines.Dispatchers { *; }

# Keep coroutines debug
-dontwarn kotlinx.coroutines.debug.**

# =============================================================================
# SLF4J (Logging) - PRIORITY 1 (CRITICAL)
# =============================================================================

# Keep SLF4J API (application code uses this)
-keep class org.slf4j.** { *; }
-keepclassmembers class org.slf4j.** { *; }

# CRITICAL: Don't warn about missing SLF4J implementation bindings
# These don't exist on Android - Ktor falls back to Android logging
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder
-dontwarn org.slf4j.impl.SimpleLoggerFactory
-dontwarn org.slf4j.impl.Log4jLoggerFactory
-dontwarn org.slf4j.impl.JDK14LoggerFactory
-dontwarn org.slf4j.impl.Slf4jLogger
-dontwarn org.slf4j.impl.Slf4jLoggerFactory
-dontwarn org.slf4j.helpers.MessageFormatter
-dontwarn org.slf4j.**

# =============================================================================
# OKHTTP (used by Ktor/Coil)
# =============================================================================

-keep class okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep OkHttp Platform
-keep class okhttp3.internal.platform.** { *; }
-keep class okhttp3.internal.** { *; }

# =============================================================================
# OKIO (used by OkHttp)
# =============================================================================

-keep class okio.** { *; }
-keepclassmembers class okio.** { *; }

# =============================================================================
# GENERAL SAFETY RULES
# =============================================================================

# Keep generic signatures
-keepattributes Signature

# Keep exceptions
-keepattributes Exceptions

# Keep inner classes
-keepattributes InnerClasses

# Keep enclosing method info
-keepattributes EnclosingMethod

# Keep runtime visible annotations
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Keep *Reflection* methods
-keepclassmembers class * {
    *** *Reflection***(...);
}

# Keep meta-annotations
-keep @interface ** { *; }

# =============================================================================
# ADDITIONAL CRITICAL FIXES
# =============================================================================

# Google Play Services / Firebase (if ever added)
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**

# Apache HTTP Client (legacy)
-dontwarn org.apache.http.**
-dontwarn android.net.http.**

# JSR305 annotations (used by many libraries)
-dontwarn javax.annotation.**
-dontwarn javax.annotation.meta.**
-dontwarn edu.umd.cs.findbugs.annotations.**

# Remove logging in release (optional but recommended)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
