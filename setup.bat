@echo off
chcp 65001 >nul
echo ==========================================
echo G30Scanner 项目设置脚本
echo ==========================================
echo.

:: 检查是否安装了Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Java，请先安装 JDK 17 或更高版本
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo [1/3] Java 环境已检测到
echo.

:: 检查 Android SDK
if "%ANDROID_HOME%"=="" (
    echo [警告] 未设置 ANDROID_HOME 环境变量
    echo 如果使用 Android Studio，可以忽略此警告
    echo.
)

:: 下载 gradle-wrapper.jar
echo [2/3] 下载 Gradle Wrapper...
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    powershell -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle/wrapper/gradle-wrapper.jar'"
    if errorlevel 1 (
        echo [警告] 自动下载失败，请手动下载 gradle-wrapper.jar 放到 gradle/wrapper/ 目录
        echo 下载地址: https://services.gradle.org/distributions/gradle-8.2-bin.zip
    ) else (
        echo [成功] Gradle Wrapper 下载完成
    )
) else (
    echo [跳过] Gradle Wrapper 已存在
)

echo.
echo [3/3] 项目设置完成！
echo.
echo ==========================================
echo 使用方法:
echo   1. 使用 Android Studio 打开此文件夹
echo   2. 等待 Gradle 同步完成
echo   3. 点击 Build -^> Build APK
echo.
echo 或使用命令行构建:
echo   .\gradlew.bat assembleDebug
echo ==========================================
pause
