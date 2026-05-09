# G30信标扫描工具 - Android版

仿照WinForm程序TestG30编写的Android应用，用于访问和操作G30硬件设备。

## 功能特点

1. **USB串口通信**
   - 支持USB OTG连接G30硬件设备
   - 自动识别常见USB转串口芯片（FT232, CP210x, CH340, PL2303, CDC/ACM）
   - 波特率：115200，数据位：8，停止位：1，无校验

2. **信标扫描**
   - 发送扫描指令：`AT+STARTSCAN=NORMAL`
   - 实时接收并解析信标数据
   - 自动计算RSSI平均值和接收次数

3. **数据显示**
   - 原始数据接收显示区
   - 解析后的信标列表（MAC / 资产编号 / 单位 / 距离 / RSSI）
   - MAC地址前缀过滤功能
   - 资产编号过滤功能
   - 按信号强度排序

4. **数据导出**
   - 导出为CSV格式文件
   - 保存在应用私有目录 `/Android/data/com.example.g30scanner/files/Exports/`

## 界面布局

| 区域 | 功能 |
|------|------|
| 顶部控制栏 | USB设备选择、打开/关闭串口、刷新设备列表、登出 |
| 状态显示 | 显示串口连接状态 |
| 过滤模式 | MAC过滤 / 资产编号过滤 切换 |
| 过滤操作区 | MAC/资产过滤、RSSI限值、查询资产、导出CSV |
| 统计信息 | 运行时间、所有MAC/账号MAC/匹配资产（点击切换类别） |
| 信标列表 | 显示解析后的信标信息（MAC / 资产编号 / 单位 / 距离 / RSSI） |

### 统计面板

点击统计项可快速筛选列表，同时自动清空过滤输入框：

| 统计项 | 含义 |
|--------|------|
| **所有MAC** | 扫描到的全部信标数量 |
| **账号MAC** | 后台系统中有记录的信标数量 |
| **匹配资产** | 有资产编号的信标数量 |

## 使用方法

1. 使用USB OTG线连接G30设备和手机
2. 打开App，从下拉列表选择USB设备
3. 点击"打开串口"按钮
4. 点击"发送指令"发送 `AT+STARTSCAN=NORMAL`
5. 观察信标数据实时显示
6. 使用MAC前缀过滤特定信标
7. 点击"导出数据"保存为CSV文件

## 技术说明

- **开发语言**: Java
- **最低API**: 21 (Android 5.0)
- **目标API**: 34 (Android 14)
- **依赖库**: 
  - usb-serial-for-android: 用于USB串口通信
  - Material Design Components: UI组件

## 与WinForm版本的区别

| 功能 | WinForm | Android |
|------|---------|---------|
| 串口类型 | 物理串口(COM) | USB OTG |
| 设备选择 | 自动枚举COM口 | 自动识别USB设备 |
| 存储位置 | 用户选择 | 应用私有目录 |
| 数据刷新 | Timer定时器 | Handler定时器 |
| 资产查询 | 无 | 后台API查询资产编号与单位 |
| 登录认证 | 无 | 用户名+密码+验证码 |

## 文件结构

```
G30Scanner/
├── app/
│   ├── build.gradle              # 应用级Gradle配置
│   └── src/main/
│       ├── AndroidManifest.xml   # 应用清单
│       ├── java/com/example/g30scanner/
│       │   ├── MainActivity.java          # 主Activity
│       │   ├── LoginActivity.java         # 登录Activity
│       │   ├── data/
│       │   │   ├── BeaconItem.java        # 信标数据模型
│       │   │   └── model/                 # 后台API数据模型
│       │   │       ├── LoginRequest.java
│       │   │       ├── LoginResponse.java
│       │   │       ├── LabelListRequest.java
│       │   │       ├── LabelListResponse.java
│       │   │       └── LabelItem.java
│       │   ├── network/
│       │   │   ├── ApiClient.java         # OkHttp网络封装
│       │   │   ├── ApiService.java        # 业务API
│       │   │   └── ApiCallback.java       # 请求回调
│       │   ├── usb/
│       │   │   └── UsbSerialManager.java  # USB串口管理
│       │   └── utils/
│       │       └── TokenManager.java      # Token管理
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml      # 主界面布局
│           │   └── item_beacon.xml        # 列表项布局
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           ├── drawable/
│           │   ├── bg_edit_text.xml
│           │   ├── bg_list_view.xml
│           │   └── ic_launcher_foreground.xml
│           └── xml/
│               └── device_filter.xml      # USB设备过滤
├── build.gradle                  # 项目级Gradle配置
├── settings.gradle
└── README.md
```

## 编译说明

使用Android Studio打开项目，同步Gradle后即可编译安装。

或者使用命令行:
```bash
./gradlew assembleDebug
```

APK将生成在 `app/build/outputs/apk/debug/app-debug.apk`
