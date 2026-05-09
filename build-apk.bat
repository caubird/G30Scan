@echo off
chcp 65001 >nul
echo ==========================================
echo G30Scanner APK 构建脚本
echo ==========================================
echo.

:: 设置环境变量
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
set "ANDROID_HOME=%USERPROFILE%\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/4] 环境变量已设置
echo JAVA_HOME: %JAVA_HOME%
echo ANDROID_HOME: %ANDROID_HOME%
echo.

:: 检查 Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] Java 未正确安装
    pause
    exit /b 1
)
echo [2/4] Java 检测通过
java -version 2>&1 | findstr "version"
echo.

:: 创建 Android SDK 目录结构
echo [3/4] 创建 SDK 目录结构...
if not exist "%ANDROID_HOME%\licenses" mkdir "%ANDROID_HOME%\licenses"
if not exist "%ANDROID_HOME%\platforms" mkdir "%ANDROID_HOME%\platforms"
if not exist "%ANDROID_HOME%\build-tools" mkdir "%ANDROID_HOME%\build-tools"

:: 创建许可证文件（接受所有许可证）
echo 24333f8a63b6825ea9c5514f83c2829b004d1fee > "%ANDROID_HOME%\licenses\android-sdk-license"
echo 84831b9409646a918e30573bab4c9c91346d8abd > "%ANDROID_HOME%\licenses\android-sdk-preview-license"
echo d975f751698a77b662f1254ddbeed3901e976f5a > "%ANDROID_HOME%\licenses\intel-android-extra-license"
echo.

echo [4/4] 开始构建 APK...
echo 这可能需要几分钟时间，请耐心等待...
echo.

:: 运行 Gradle 构建
cd /d "%~dp0"
call .\gradlew.bat assembleDebug --console=plain

if errorlevel 1 (
    echo.
    echo [错误] 构建失败！
    echo 如果提示缺少 SDK，请手动下载 Android Studio 安装
    pause
    exit /b 1
)

echo.
echo ==========================================
echo 构建成功！
echo ==========================================
echo APK 路径: app\build\outputs\apk\debug\app-debug.apk
echo.
pause
