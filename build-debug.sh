#!/bin/bash
# G30Scanner 一键编译脚本
# 由 Kilo 自动生成

set -e

PROJECT_DIR="/media/lijian/data/test/Android/G30Scanner"
SDK_DIR="$PROJECT_DIR/.android-sdk"

echo "========================================"
echo "  G30Scanner Android 编译脚本"
echo "========================================"

# 1. 检查 gradle-wrapper.jar
echo ""
echo "[1/5] 检查 gradle-wrapper.jar..."
JAR_FILE="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar"
JAR_SIZE=$(stat -c%s "$JAR_FILE" 2>/dev/null || echo "0")
if [ "$JAR_SIZE" -eq 0 ]; then
    echo "  ⚠️ gradle-wrapper.jar 为空文件，正在下载修复..."
    curl -L -o "$JAR_FILE" https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar
    echo "  ✅ 已下载修复 gradle-wrapper.jar"
else
    echo "  ✅ gradle-wrapper.jar 正常 ($JAR_SIZE 字节)"
fi

# 2. 检查 local.properties
echo ""
echo "[2/5] 检查 local.properties..."
if [ ! -f "$PROJECT_DIR/local.properties" ]; then
    echo "  ⚠️ local.properties 不存在，正在创建..."
    echo "sdk.dir=$SDK_DIR" > "$PROJECT_DIR/local.properties"
    echo "  ✅ 已创建 local.properties"
else
    echo "  ✅ local.properties 已存在"
fi

# 3. 设置环境变量
echo ""
echo "[3/5] 设置 Android SDK 环境变量..."
export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
echo "  ANDROID_HOME=$ANDROID_HOME"
echo "  ✅ 环境变量已设置"

# 4. 检查 Java
echo ""
echo "[4/5] 检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "  ❌ 错误: 未找到 Java，请先安装 OpenJDK 17 或 21"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1)
echo "  ✅ $JAVA_VERSION"

# 5. 执行编译
echo ""
echo "[5/5] 开始编译..."
echo "  命令: ./gradlew assembleDebug"
echo ""
cd "$PROJECT_DIR"
./gradlew assembleDebug

echo ""
echo "========================================"
echo "  ✅ 编译完成！"
echo "========================================"
echo ""
echo "APK 输出路径:"
echo "  $PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "安装到设备命令:"
echo "  adb install -r $PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
echo ""
