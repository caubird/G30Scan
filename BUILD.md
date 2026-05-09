# G30信标扫描工具 - 构建说明

## 方法1: 使用 Android Studio (推荐)

### 前置要求
- [Android Studio](https://developer.android.com/studio) (推荐最新版)
- Android SDK (通过Android Studio安装)

### 构建步骤

1. **打开项目**
   - 启动 Android Studio
   - 选择 "Open an existing Android Studio project"
   - 选择 `G30Scanner` 文件夹

2. **同步项目**
   - 打开项目后，Android Studio 会自动开始同步 Gradle
   - 等待同步完成（首次可能需要下载依赖，耗时较长）

3. **构建 APK**
   - 点击菜单栏 `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 或者使用快捷键 `Ctrl+F9` (Windows/Linux) / `Cmd+F9` (Mac)

4. **获取 APK**
   - 构建成功后，点击右下角的提示 "locate"
   - APK 文件路径：`app/build/outputs/apk/debug/app-debug.apk`

### 直接运行到设备

1. 使用 USB 连接 Android 设备（需开启开发者选项和USB调试）
2. 点击工具栏的绿色运行按钮 ▶️ 或按 `Shift+F10`
3. 选择设备，App 将自动安装并运行

---

## 方法2: 使用命令行

### 前置要求
- [JDK 17](https://www.oracle.com/java/technologies/downloads/#java17) 或更高
- [Android SDK](https://developer.android.com/studio#downloads) (下载 Command line tools only)
- 设置环境变量：
  ```bash
  export JAVA_HOME=/path/to/jdk
  export ANDROID_HOME=/path/to/android-sdk
  export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
  ```

### 构建步骤

1. **安装 SDK 组件** (首次使用)
   ```bash
   sdkmanager "platforms;android-34" "build-tools;34.0.0"
   ```

2. **构建 Debug APK**
   ```bash
   cd G30Scanner
   ./gradlew assembleDebug
   ```
   Windows:
   ```cmd
   cd G30Scanner
   gradlew.bat assembleDebug
   ```

3. **获取 APK**
   - 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### 安装到设备

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 构建 Release APK

### Android Studio
1. `Build` → `Generate Signed Bundle / APK`
2. 选择 `APK`
3. 创建或选择签名密钥
4. 选择 `release` 构建类型
5. 点击 `Finish`

### 命令行
```bash
./gradlew assembleRelease
```
Release APK 输出路径：`app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 常见问题

### 1. Gradle 同步失败

**问题**: 同步时提示依赖下载失败
**解决**: 
- 检查网络连接
- 添加阿里云镜像（国内网络）到 `settings.gradle`:
```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        maven { url 'https://maven.aliyun.com/repository/public' }
    }
}
```

### 2. USB 库找不到

**问题**: 编译时提示 `usb-serial-for-android` 找不到
**解决**: 确保 `settings.gradle` 中已添加 `maven { url 'https://jitpack.io' }`

### 3. 构建工具版本不匹配

**问题**: 提示 build tools 版本错误
**解决**: 使用 Android Studio 的 SDK Manager 安装对应的 Build Tools 版本

### 4. 安装时解析包错误

**问题**: 安装 APK 提示解析包时出现问题
**解决**: 
- 确保设备 Android 版本 >= 5.0 (API 21)
- 检查 APK 是否完整下载/传输
- 开启「允许安装未知来源应用」权限

---

## 项目依赖

| 库 | 版本 | 用途 |
|----|------|------|
| androidx.appcompat | 1.6.1 | 基础UI支持 |
| material | 1.11.0 | Material Design 组件 |
| constraintlayout | 2.1.4 | 约束布局 |
| usb-serial-for-android | 3.7.0 | USB 串口通信 |

---

## 验证构建

成功构建后，你应该看到以下输出：
```
BUILD SUCCESSFUL in Xs
56 actionable tasks: 56 executed
```

APK 文件信息：
- 文件名: `app-debug.apk`
- 大小: 约 2-3 MB
- 最低 Android 版本: 5.0 (API 21)
