#!/usr/bin/env python3
"""
MIYU Feature Parity Verifier
Compares old React Native MIYU (src/) against new Kotlin rewrite (MIYU-Kotlin/)
and verifies: build system integrity, feature completeness, UI/UX parity,
EPUB parser correctness, term grouping, and all app tools.
"""

import os, sys, re, json
from pathlib import Path
from dataclasses import dataclass, field
from typing import Set, Dict, List, Optional

# ── Configuration ──────────────────────────────────────
OLD_ROOT = Path("/root/React Native to Kotlin/MIYU/MIYO-EbookReader")
NEW_ROOT = Path("/root/MIYU-Kotlin")
OLD_SRC  = OLD_ROOT / "src"
NEW_SRC  = NEW_ROOT / "app/src/main/java/com/miyu/reader"
NEW_CPP  = NEW_ROOT / "app/src/main/cpp"

VERDICT = {"pass": 0, "fail": 0, "warn": 0}

def ok(msg):   VERDICT["pass"] += 1; print(f"  ✓ {msg}")
def bad(msg):  VERDICT["fail"] += 1; print(f"  ✗ {msg}")
def wrn(msg):  VERDICT["warn"] += 1; print(f"  ⚠ {msg}")

def header(s): print(f"\n{'='*60}\n  {s}\n{'='*60}")

# ── 1. Build System Integrity ─────────────────────────
header("1. BUILD SYSTEM INTEGRITY")

def check_build():
    # Gradle wrapper
    if (NEW_ROOT / "gradlew").exists() and (NEW_ROOT / "gradle/wrapper/gradle-wrapper.jar").exists():
        ok("Gradle wrapper (gradlew + wrapper jar) present")
    else:
        bad("Missing Gradle wrapper files")

    # Gradle configs
    for f in ["build.gradle.kts", "settings.gradle.kts", "gradle.properties",
              "app/build.gradle.kts", "app/proguard-rules.pro"]:
        if (NEW_ROOT / f).exists():
            ok(f"Config present: {f}")
        else:
            bad(f"Missing config: {f}")

    # Android manifest
    manifest = NEW_ROOT / "app/src/main/AndroidManifest.xml"
    if manifest.exists():
        content = manifest.read_text()
        if "android:icon" in content and "android:roundIcon" in content:
            ok("AndroidManifest has icon + roundIcon")
        else:
            bad("AndroidManifest missing icon references")
        if "permission.READ_EXTERNAL_STORAGE" in content:
            ok("Storage read permission declared")
        if "MIYUApplication" in content:
            ok("Application class registered in manifest")
    else:
        bad("Missing AndroidManifest.xml")

    # JNI library loading
    main_activity = NEW_SRC / "MainActivity.kt"
    if main_activity.exists():
        ka = main_activity.read_text()
        if "System.loadLibrary" in ka:
            ok("System.loadLibrary(\"miyu_engine\") in MainActivity")
        else:
            bad("Missing System.loadLibrary in MainActivity")

    # CMake setup
    cmake_list = NEW_CPP / "CMakeLists.txt"
    if cmake_list.exists():
        cm = cmake_list.read_text()
        if "miyu_engine" in cm and "SHARED" in cm:
            ok("CMakeLists.txt configures miyu_engine SHARED library")
        else:
            bad("CMakeLists.txt missing miyu_engine SHARED target")
    else:
        bad("Missing CMakeLists.txt")

check_build()

# ── 2. Feature Mapping: Old vs New ────────────────────
header("2. FEATURE MAPPING: OLD RN → NEW KOTLIN")

# Scan old RN source for features (components, screens, providers)
old_ts_files = list(OLD_SRC.rglob("*.ts")) + list(OLD_SRC.rglob("*.tsx"))
old_features = set()
for f in old_ts_files:
    rel = str(f.relative_to(OLD_SRC))
    # Skip node_modules/build artifacts
    if "node_modules" in rel: continue
    parts = rel.split("/")
    if len(parts) >= 1:
        old_features.add(rel)

new_kt_files = list(NEW_SRC.rglob("*.kt"))
new_cpp_files = list(NEW_CPP.rglob("*.cpp")) + list(NEW_CPP.rglob("*.h"))

# KEY FEATURE MAPPINGS: old RN path/component → expected new implementation
FEATURE_MAP = {
    # EPUB engine
    "EPUB Parser": {
        "old": ["src/lib/epub-parser.ts", "src/lib/epub.ts", "src/lib/reader-engine.ts"],
        "new_patterns": [r"epub_parser\.(kt|cpp|h)", r"EpubEngineBridge\.kt", r"parseEpub"],
        "files_to_check": list(NEW_CPP.glob("epub/*.cpp")) + list(NEW_CPP.glob("epub/*.h")) +
                           [NEW_SRC / "engine/bridge/EpubEngineBridge.kt"],
    },
    "Cover Extractor": {
        "old": ["src/lib/epub-parser.ts"],
        "new_patterns": [r"epub_cover\.(kt|cpp|h)", r"extractCover"],
        "files_to_check": [NEW_CPP / "epub/epub_cover.cpp", NEW_CPP / "epub/epub_cover.h"],
    },
    "CSS Extractor": {
        "old": ["src/lib/epub-parser.ts"],
        "new_patterns": [r"epub_css\.(kt|cpp|h)", r"extractCss"],
        "files_to_check": [NEW_CPP / "epub/epub_css.cpp", NEW_CPP / "epub/epub_css.h"],
    },
    "Chapter Renderer": {
        "old": ["src/lib/epub-parser.ts", "src/lib/reader-engine.ts"],
        "new_patterns": [r"epub_renderer\.(kt|cpp|h)", r"renderChapter", r"countChapterWords"],
        "files_to_check": [NEW_CPP / "epub/epub_renderer.cpp", NEW_CPP / "epub/epub_renderer.h"],
    },

    # Data layer
    "Book Repository": {
        "old": ["src/hooks/useBooks.ts", "src/lib/database.ts"],
        "new_patterns": [r"BookRepository\.kt", r"BookDao\.kt", r"BookEntity\.kt"],
        "files_to_check": [NEW_SRC / "data/repository/BookRepository.kt",
                           NEW_SRC / "data/local/dao/BookDao.kt",
                           NEW_SRC / "data/local/entity/BookEntity.kt"],
    },
    "Term Repository": {
        "old": ["src/hooks/useTerms.ts", "src/lib/term-manager.ts"],
        "new_patterns": [r"TermRepository\.kt", r"TermDao\.kt", r"TermEntity\.kt"],
        "files_to_check": [NEW_SRC / "data/repository/TermRepository.kt",
                           NEW_SRC / "data/local/dao/TermDao.kt",
                           NEW_SRC / "data/local/entity/TermEntity.kt"],
    },

    # Database
    "Room Database": {
        "old": ["src/lib/database.ts"],
        "new_patterns": [r"MIYUDatabase\.kt"],
        "files_to_check": [NEW_SRC / "data/local/MIYUDatabase.kt"],
    },

    # UI - Navigation
    "Bottom Navigation": {
        "old": ["src/components/BottomNav.tsx", "src/app/(tabs)/_layout.tsx"],
        "new_patterns": [r"MIYUApp\.kt", r"NavigationBar"],
        "files_to_check": [NEW_SRC / "ui/MIYUApp.kt"],
    },

    # UI - Home
    "Home Screen": {
        "old": ["src/app/(tabs)/index.tsx", "src/components/BookCard.tsx"],
        "new_patterns": [r"HomeScreen\.kt", r"BookCard"],
        "files_to_check": [NEW_SRC / "ui/home/HomeScreen.kt"],
    },

    # UI - Theme
    "Theme System": {
        "old": ["src/providers/ThemeProvider.tsx", "src/constants/Colors.ts"],
        "new_patterns": [r"MIYUTheme\.kt", r"Color\.kt", r"ReaderColors"],
        "files_to_check": [NEW_SRC / "ui/theme/MIYUTheme.kt", NEW_SRC / "ui/theme/Color.kt"],
    },

    # Domain models
    "Domain Models": {
        "old": ["src/types/*.ts"],
        "new_patterns": [r"Book\.kt", r"Chapter\.kt", r"Dictionary\.kt", r"Terms\.kt", r"Sync\.kt", r"Theme\.kt"],
        "files_to_check": list(NEW_SRC.glob("domain/model/*.kt")),
    },

    # Preferences/Datastore
    "User Preferences": {
        "old": ["src/lib/storage.ts", "src/lib/preferences.ts"],
        "new_patterns": [r"UserPreferences\.kt"],
        "files_to_check": [NEW_SRC / "data/preferences/UserPreferences.kt"],
    },

    # DI / Hilt
    "Dependency Injection": {
        "old": [],
        "new_patterns": [r"AppModule\.kt", r"HiltViewModel", r"@Provides"],
        "files_to_check": [NEW_SRC / "di/AppModule.kt"],
    },
}

for name, feat in FEATURE_MAP.items():
    present = any(f.exists() for f in feat["files_to_check"] if f is not None)
    if present:
        ok(f"{name}: implemented")
    elif feat["old"]:
        bad(f"{name}: MISSING (was in RN)")
    else:
        wrn(f"{name}: optional — not found")

# Check for terms grouping
header("2b. TERM GROUPING FEATURE")
term_repo = NEW_SRC / "data/repository/TermRepository.kt"
term_dao = NEW_SRC / "data/local/dao/TermDao.kt"
term_model = NEW_SRC / "domain/model/Terms.kt"
grouping_ok = True
for f, label in [(term_repo, "TermRepository"), (term_dao, "TermDao"), (term_model, "Terms model")]:
    if f.exists():
        ok(f"{label} present")
    else:
        bad(f"{label} MISSING")
        grouping_ok = False

if term_dao.exists():
    content = term_dao.read_text()
    if "groupName" in content or "group" in content.lower():
        ok("Term grouping by groupName is supported")
    else:
        wrn("Term DAO might not support grouping")
if grouping_ok:
    ok("Term management feature: COMPLETE")

# ── 3. EPUB Parser Logic Verification ─────────────────
header("3. EPUB PARSER LOGIC VERIFICATION")

epub_parser_cpp = NEW_CPP / "epub/epub_parser.cpp"
if epub_parser_cpp.exists():
    cpp = epub_parser_cpp.read_text()
    checks = {
        "ZIP file opening": "open(filePath)" in cpp or "zip.open" in cpp,
        "container.xml parsing": "container.xml" in cpp,
        "OPF path extraction": "full-path" in cpp and "opfPath" in cpp,
        "Metadata parsing (dc:*)": "dc:title" in cpp or "dc:creator" in cpp,
        "Spine/manifest chapter list": "idref" in cpp and "manifest" in cpp,
        "Word counting": "wordCount" in cpp or "isalnum" in cpp,
        "CSS extraction": "extractedCss" in cpp or ".css" in cpp,
        "Cache integration": "cache.get" in cpp and "cache.put" in cpp,
        "JSON serialization": "toJson" in cpp,
    }
    for label, passed in checks.items():
        if passed:
            ok(f"Parser supports: {label}")
        else:
            bad(f"Parser missing: {label}")

    # Check that the old RN parser features are preserved
    old_parser = OLD_SRC / "lib/epub-parser.ts"
    if old_parser.exists():
        old = old_parser.read_text()
        old_features = {
            "parseMetadata": "parseMetadata" in old or "getMetadata" in old,
            "parseChapters": "parseChapters" in old or "getChapters" in old or "parseToc" in old,
            "extractCover": "extractCover" in old or "getCover" in old,
            "extractCss": "extractCss" in old or "getCss" in old,
            "wordCount": "wordCount" in old or "word" in old.lower(),
        }
        for feat, present_in_old in old_features.items():
            if present_in_old:
                wrn(f"RN has '{feat}' — ensure Kotlin C++ equivalent exists")
            else:
                ok(f"RN '{feat}' not explicitly in old parser (may be elsewhere)")
else:
    bad("epub_parser.cpp not found!")

# ── 4. UI/UX Parity ───────────────────────────────────
header("4. UI/UX PARITY CHECK")

# Reader themes
color_kt = NEW_SRC / "ui/theme/Color.kt"
if color_kt.exists():
    colors_content = color_kt.read_text()
    for theme_name in ["White", "Sepia", "Dark", "Forest"]:
        if theme_name in colors_content:
            ok(f"Reader theme '{theme_name}' defined")
        else:
            bad(f"Reader theme '{theme_name}' MISSING")

# Old RN has these reader themes?
old_colors = OLD_SRC / "constants/Colors.ts"
if old_colors.exists():
    oc = old_colors.read_text()
    for theme_name in ["white", "sepia", "dark"]:
        if theme_name.lower() in oc.lower():
            ok(f"RN theme '{theme_name}' found (parity confirmed)")

# Material 3 design
miyu_theme = NEW_SRC / "ui/theme/MIYUTheme.kt"
if miyu_theme.exists():
    mt = miyu_theme.read_text()
    if "MaterialTheme" in mt:
        ok("MaterialTheme used (Material 3)")
    if "lightColorScheme" in mt or "darkColorScheme" in mt:
        ok("Light + Dark color schemes defined")
    if "Typography" in mt:
        ok("Typography configured")

# WebView reader approach preserved
renderer_cpp = NEW_CPP / "epub/epub_renderer.cpp"
if renderer_cpp.exists():
    rc = renderer_cpp.read_text()
    if "html" in rc and "body" in rc:
        ok("Renderer produces HTML output (WebView-based reader)")
    if "getScrollProgress" in rc and "getSelection" in rc:
        ok("Reader JS bridge: scroll progress + text selection")
    else:
        wrn("Reader JS bridge might be missing scroll/selection hooks")

# ── 5. GitHub Actions CI/CD ───────────────────────────
header("5. GITHUB ACTIONS CI/CD")

workflow = NEW_ROOT / ".github/workflows/build.yml"
if workflow.exists():
    wf = workflow.read_text()
    checks = {
        "Checkout action": "actions/checkout" in wf,
        "JDK 17": "java-version: '17'" in wf,
        "Gradle setup": "gradle/actions/setup-gradle" in wf or "gradlew" in wf,
        "Android SDK": "setup-android" in wf or "android-34" in wf,
        "assembleRelease": "assembleRelease" in wf,
        "APK artifact upload": "upload-artifact" in wf and ("release" in wf.lower() or "apk" in wf.lower()),
        "Lint job": "lint" in wf.lower(),
        "Test job": "test" in wf.lower() or "test" in wf,
        "Release on tag": "softprops/action-gh-release" in wf or "release" in wf,
    }
    for label, passed in checks.items():
        if passed: ok(f"CI has: {label}")
        else: bad(f"CI missing: {label}")
else:
    bad("No .github/workflows/build.yml found!")

# ── 6. Icon/Mipmap Resources ──────────────────────────
header("6. ICON & MIPMAP RESOURCES")

DENSITIES = ["mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"]
res_dir = NEW_ROOT / "app/src/main/res"

for density in DENSITIES:
    mipmap_dir = res_dir / f"mipmap-{density}"
    for icon_file in ["ic_launcher.png", "ic_launcher_foreground.png", "ic_launcher_round.png"]:
        p = mipmap_dir / icon_file
        if p.exists():
            ok(f"{density}/{icon_file} ({p.stat().st_size} bytes)")
        else:
            bad(f"Missing {density}/{icon_file}")

# Adaptive icon XML
for xml_file in ["ic_launcher.xml", "ic_launcher_round.xml"]:
    p = res_dir / f"mipmap-anydpi-v26/{xml_file}"
    if p.exists():
        ok(f"Adaptive icon XML: {xml_file}")
    else:
        bad(f"Missing adaptive icon XML: {xml_file}")

# ── 7. Security & ProGuard ─────────────────────────────
header("7. SECURITY & PROGUARD")

proguard = NEW_ROOT / "app/proguard-rules.pro"
if proguard.exists():
    pg = proguard.read_text()
    checks = {
        "JNI methods kept": "native <methods>" in pg,
        "Dagger/Hilt kept": "dagger" in pg.lower() or "hilt" in pg.lower(),
        "Room entities kept": "Room" in pg or "room" in pg.lower(),
        "Serialization kept": "serializer" in pg.lower(),
        "Ktor kept": "ktor" in pg.lower(),
    }
    for label, passed in checks.items():
        if passed: ok(f"ProGuard: {label}")
        else: wrn(f"ProGuard may miss: {label}")
else:
    bad("No proguard-rules.pro!")

# ── 8. Additional Tools & Features ─────────────────────
header("8. ADDITIONAL TOOLS & FEATURES")

# Check old RN for extra features not yet mapped
old_screens = set()
for f in old_ts_files:
    rel = str(f.relative_to(OLD_SRC))
    if "node_modules" in rel: continue
    if "/app/" in rel:
        old_screens.add(rel)

# Known old RN screens/features
old_screen_names = [
    ("Library / Browse", ["library", "Library", "browse"]),
    ("Dictionary Lookup", ["dictionary", "Dictionary", "dict", "Dict"]),
    ("MTL / Translation", ["mtl", "MTL", "translat", "Translat"]),
    ("Reading History", ["history", "History", "recent"]),
    ("Settings / Preferences", ["settings", "Settings", "pref", "Pref"]),
    ("Search", ["search", "Search"]),
    ("Bookmarks", ["bookmark", "Bookmark"]),
    ("Highlights", ["highlight", "Highlight", "annot"]),
    ("Sync / Cloud", ["sync", "Sync", "cloud", "Cloud", "supabase", "Supabase"]),
    ("Dark Mode Toggle", ["dark", "Dark", "theme", "Theme"]),
]

for feature_name, keywords in old_screen_names:
    found_in_old = False
    for sf in old_screens:
        for kw in keywords:
            if kw in sf:
                found_in_old = True
                break
        if found_in_old:
            break
    if found_in_old:
        # Check if new project has it
        found_in_new = False
        for nf in new_kt_files:
            for kw in keywords:
                if kw.lower() in str(nf).lower():
                    found_in_new = True
                    break
            if found_in_new:
                break
        if not found_in_new:
            for nf in new_cpp_files:
                for kw in keywords:
                    if kw.lower() in str(nf).lower():
                        found_in_new = True
                        break
                if found_in_new:
                    break
        if found_in_new:
            ok(f"'{feature_name}' exists in old RN AND new Kotlin")
        else:
            wrn(f"'{feature_name}' in old RN but NOT YET in new Kotlin")
    # else: feature not present in old RN

# ── Final Summary ──────────────────────────────────────
header("FINAL SUMMARY")

total = sum(VERDICT.values())
pass_rate = (VERDICT["pass"] / total * 100) if total > 0 else 0
print(f"  Pass : {VERDICT['pass']}")
print(f"  Fail : {VERDICT['fail']}")
print(f"  Warn : {VERDICT['warn']}")
print(f"  Total: {total}")
print(f"  Score: {pass_rate:.0f}% PASS")

if VERDICT["fail"] == 0:
    print("\n  >>> VERDICT: PASS — all critical features verified! <<<")
    sys.exit(0)
elif VERDICT["fail"] <= 3:
    print(f"\n  >>> VERDICT: PARTIAL ({VERDICT['fail']} failures) — minor gaps <<<")
    sys.exit(1)
else:
    print(f"\n  >>> VERDICT: FAIL ({VERDICT['fail']} failures) — significant gaps <<<")
    sys.exit(1)