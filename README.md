# IntimaLog（亲密记录）

面向个人使用、隐私优先、本地优先的亲密行为记录 App。Miuix（HyperOS 设计语言）UI，
支持多人模式（一次记录关联多位参与者）、桌面小组件一键记录、SQLCipher 加密存储。

[![Latest Release](https://img.shields.io/github/v/release/imalti0r/intimalog?style=flat-square)](https://github.com/imalti0r/intimalog/releases/latest)

## 核心特性

- **3 秒记录**：首页大按钮开始/结束计时，通知栏常驻；结束弹出参与者多选 + 快速评分
- **多人模式**：一次记录关联多位伴侣（多对多关联表 + 参与顺序 + 主参与者），统计口径可切换（场次/人次）
- **组合视图**：搭档组合排行（如「与 A + B 共 8 场」）、组合质量对比、精确组合筛选
- **每人高潮次数**：记录内按参与者分别计数，统计页排行榜归属清晰
- **跨日拆分**：单场记录跨自然日自动按 00:00 拆成多段，统计口径保持完整
- **桌面小组件**（Glance）：今日概览 + 开始/结束按钮，半透明 QuickAdd 不进主界面
- **快捷磁贴**：Quick Settings 一键开始/结束
- **统计**：热力图（GitHub 风格）、折线趋势、环形占比（保护措施/多人场次）、组合排行、Streak，口径全局切换

### 隐私红线

- **本地优先**：所有数据（记录、伴侣、标签、PIN）仅存本机——Room + SQLCipher 全库加密（密钥 Keystore 派生 + EncryptedSharedPreferences 保存）
- **应用锁**：PIN（PBKDF2 派生）+ 生物识别
- **FLAG_SECURE** 默认禁止截屏与最近任务预览
- **伪装模式**：应用名「日常」；强伪装下多人场次显示为「活动 · N 人」
- **紧急清除**：长按设置标题 5 次触发一键清除（二次确认）
- **不接任何分析/崩溃上报 SDK**，唯一联网行为是你主动配置的 WebDAV 同步

### 备份与同步

- **加密备份**（`.intima`）：AES-256-GCM，PIN 派生密钥，覆盖范围——伴侣、记录（含高潮明细/拆分组）、体位/玩具/保护措施/地点四路自定义标签、场景模板、用户资料
- **覆盖式恢复**：单事务清空后按备份原样写回，恢复后与备份完全一致
- **WebDAV 同步**：可选，上传/下载加密备份（密码经 Android Keystore 加密存储）
- **CSV 导出**：明文，可选脱敏（P1/P2 代替伴侣名）

## 下载安装

从 [Releases](https://github.com/imalti0r/intimalog/releases/latest) 下载 `IntimaLog-vX.Y.Z-debug.apk` 安装（debug 签名，覆盖安装即可）。

## 编译

环境要求：JDK 17+，Android SDK 37。

```powershell
.\gradlew.bat assembleDebug
# 产物：app\build\outputs\apk\debug\app-debug.apk
```

工具链版本（`gradle/libs.versions.toml`）：

| 组件 | 版本 | 备注 |
|---|---|---|
| Kotlin | 2.4.21 | AGP 9 内置 Kotlin 支持，无需 kotlin-android 插件 |
| AGP | 9.4.1 | 需 Gradle 9.7+；Hilt Gradle 插件 2.59+ 也要求 AGP 9 |
| Gradle | 9.7.1 | |
| KSP | 2.3.12 | KSP2，与 Kotlin 版本解耦 |
| Hilt | 2.60.1 | |
| Room | 2.8.5 | + SQLCipher `net.zetetic:sqlcipher-android` |
| Compose BOM | 2026.09.00 | ui-android 1.12.1 要求 compileSdk 37 |
| Glance | 1.2.0 | |
| Miuix | 0.8.8 | `top.yukonga.miuix.kmp:miuix`（单 artifact；0.9.x 才拆分 miuix-ui） |

> **注意**：Miuix 0.8.x → 0.9.x 是破坏性迁移（坐标改为 `miuix-ui` + 各子模块）。
> 升级时参照 [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) 的迁移说明。
> protobuf-gradle-plugin 0.9.5 与 AGP 9 不兼容（BaseExtension 强转失败），故 DataStore 使用 Preferences 方案。

## 真机验证（ADB）

连接设备后运行：

```powershell
.\scripts\verify.ps1          # 构建 + 安装 + 启动 + 崩溃检查 + 截图
.\scripts\logcat-crash.ps1    # 单独抓崩溃日志
```

手动验证要点：

1. `adb install -r app\build\outputs\apk\debug\app-debug.apk`
2. `adb shell am start -n com.intima.log.debug/com.intima.log.ui.MainActivity`
3. 首次引导 → 设置 PIN → 添加伴侣 → 开始/结束记录 → 统计口径切换 → 组合视图

## 隐私说明

- 数据库与 PIN 仅存本机，密钥经 Android Keystore 保护
- 导出文件用你设定的密码加密，密码丢失无法恢复
- 应用申请 `INTERNET` 权限仅用于你主动配置的 WebDAV 备份同步；不配置则无任何网络请求

## 免责声明

生理期/安全期相关提示（若开启周期模块）仅为记录参考，**不构成医疗建议**。
