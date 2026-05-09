#!/bin/bash
# G30Scanner 自动编译脚本
set -e
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="${PROJECT_DIR}/.build-tools/zulu17.50.19-ca-jdk17.0.11-macosx_aarch64/zulu-17.jdk/Contents/Home"
export ANDROID_HOME="${PROJECT_DIR}/.android-sdk"
export ANDROID_SDK_ROOT="${ANDROID_HOME}"

echo "========================================"
echo "  G30Scanner Auto Build"
echo "  JAVA:  $($JAVA_HOME/bin/java -version 2>&1 | head -1)"
echo "  SDK:   ${ANDROID_HOME}"
echo "========================================"
echo ""

cd "${PROJECT_DIR}"
./gradlew assembleDebug

echo ""
echo "========================================"
echo "  ✅ Build Success"
echo "========================================"
echo ""
APK_SRC="${PROJECT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
APK_DST="${PROJECT_DIR}/G30Scanner-debug-$(date +%Y%m%d-%H%M).apk"
cp "${APK_SRC}" "${APK_DST}"
echo "APK: ${APK_DST}"
ls -lh "${APK_DST}"
