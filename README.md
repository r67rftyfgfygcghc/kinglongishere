# 跑步分享 RunShare

一款Android跑步位置分享应用，类似微信位置分享功能，支持实时位置分享、历史记录查看和分享。

[![Android CI/CD](https://github.com/YOUR_USERNAME/runshare/actions/workflows/android.yml/badge.svg)](https://github.com/YOUR_USERNAME/runshare/actions)

## ✨ 功能特性

- 🗺️ **多地图支持** - OpenStreetMap、高德地图、百度地图、腾讯地图自由选择
- 📍 **实时位置追踪** - 后台持续追踪，记录跑步轨迹
- 📊 **跑步数据统计** - 实时显示距离、时长、配速
- 🔗 **位置分享** - 生成二维码/链接，好友可查看您的位置
- 📚 **历史记录** - 保存所有跑步记录，随时查看轨迹
- 📤 **轨迹导出** - 支持导出GPX格式文件
- 🎨 **Material You设计** - 现代化UI，支持深色模式

## 📱 截图

(待添加应用截图)

## 🚀 快速开始

### GitHub一键编译

1. Fork此仓库
2. 进入 `Actions` 页面
3. 点击 `Android CI/CD` 工作流
4. 点击 `Run workflow` 按钮
5. 等待编译完成后下载APK

### 本地编译

```bash
# 克隆仓库
git clone https://github.com/YOUR_USERNAME/runshare.git
cd runshare

# 编译Debug版本
./gradlew assembleDebug

# APK输出位置
# app/build/outputs/apk/debug/app-debug.apk
```

## ⚙️ 配置

### 地图API Key配置（可选）

如需使用国内地图，请在 `gradle.properties` 中配置对应的API Key：

```properties
# 高德地图
AMAP_API_KEY=your_amap_key_here

# 百度地图
BAIDU_API_KEY=your_baidu_key_here

# 腾讯地图
TENCENT_API_KEY=your_tencent_key_here
```

> 💡 默认使用OpenStreetMap，无需配置即可使用

## 📋 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose + Material3
- **架构**: MVVM
- **数据库**: Room
- **位置服务**: Google Play Services Location
- **地图**: OSMDroid (多源支持)
- **CI/CD**: GitHub Actions

## 📁 项目结构

```
app/src/main/java/com/runshare/app/
├── MainActivity.kt          # 主入口
├── RunShareApp.kt           # Application类
├── data/                    # 数据层
│   ├── RunDatabase.kt       # Room数据库
│   ├── RunDao.kt            # 数据访问接口
│   ├── RunEntity.kt         # 跑步记录实体
│   └── PreferencesRepository.kt # 偏好设置
├── model/                   # 数据模型
│   └── Models.kt            # LocationPoint, MapProvider等
├── service/                 # 服务
│   └── LocationService.kt   # 位置追踪前台服务
├── ui/                      # UI层
│   ├── components/          # 可复用组件
│   ├── screens/             # 页面
│   └── theme/               # 主题
└── utils/                   # 工具类
    ├── LocationUtils.kt     # 位置计算
    └── ShareUtils.kt        # 分享功能
```

## 🔐 权限说明

| 权限 | 用途 |
|------|------|
| ACCESS_FINE_LOCATION | 获取精确GPS位置 |
| ACCESS_COARSE_LOCATION | 获取粗略位置 |
| ACCESS_BACKGROUND_LOCATION | 后台定位（跑步时） |
| FOREGROUND_SERVICE | 前台服务 |
| INTERNET | 加载地图瓦片 |

## 📄 开源协议

MIT License

## 🤝 贡献

欢迎提交Issue和Pull Request！
