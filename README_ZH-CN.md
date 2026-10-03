# 🚬 榔烟记 (Betel & Smoke Tracker)

> **一款基于 Jetpack Compose 与 Material 3 打造、支持独立记录烟草与槟榔行为的 Android 应用。**

[![License: MIT](https://img.shields.io/badge/开源协议-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/开发语言-Kotlin_100%25-purple.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/平台支持-Android_12%2B-green.svg)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/UI框架-Jetpack_Compose_M3-4285F4.svg)](https://developer.android.com/jetpack/compose)

---

🌐 **语言切换 / Language**:  
**[English](README.md)** | **[简体中文 (默认)](README_ZH-CN.md)**

---

## 🖼️ 应用展示与效果

<p align="center">
  <img src="artwork/launcher_icon.svg" width="120" height="120" alt="Betel & Smoke Tracker Icon" style="border-radius: 24px;" /><br/>
  <b>榔烟记 · Betel & Smoke Tracker</b><br/>
  <i>官方应用图标 · App Icon</i>
</p>

<br/>

![榔烟记 · Betel & Smoke Tracker 横幅](artwork/readme-banner.svg)

### 📱 核心功能导航结构

```text
┌───────────────────────────────────────────────────────────┐
│              榔烟记 · Betel & Smoke Tracker               │
├────────┬──────────┬──────────────┬─────────┬─────────────┤
│ 首页   │ 分析     │ 烟草 ↔ 槟榔  │ 品牌    │ 设置        │
│        │          │ 切换开关     │         │             │
└────────┴──────────┴──────────────┴─────────┴─────────────┘
```

切换主体后，首页、分析、品牌和设置同步切换；一次只展示一个主体的数据。

---

## ✨ 核心亮点与特色功能

### 1. 🚬 三大社交场景精准打卡
传统的吸烟记录应用无法区分现实社交中的“递烟”与“接烟”。**榔烟记** 针对真实生活场景细分三大打卡模式：

- **🚬 自购自抽**: 自己购买的香烟自己抽。同时计入**个人健康抽烟量**与**财务开销**。
- **🤝 社交递烟**: 掏自己的烟递给朋友或同事。计入**财务开销**，但**不计入**个人健康抽烟摄入量。
- **🎁 社交接烟**: 接受别人递过来的烟抽（他人买单）。计入**个人健康抽烟量**，但财务成本记录为 **¥0**（免费）。

### 2. 📊 智能趋势分析与交互图表
- **灵活的时间范围**: 支持查看 **近7天**、**本周**、**本月**、**指定月份**、**指定年份** 以及 **自定义任意时间段** 的统计数据。
- **双图表模式**:
  - **堆叠柱状图**: 直观对比每日“自购自抽”、“社交递烟”与“社交接烟”的数量构成。
  - **折线对比图**: 动态对比“实际总吸烟量”与“社交接烟”趋势。
- **Canvas 触摸交互**: 点击图表上的任意柱条或节点，顶部条幅即刻展示当天的详细统计。
- **智能习惯洞察**:
  - **日均抽烟量**: 准确计算所选时间段内的日均自抽支数。
  - **平均间隔时间**: 自动排除夜间睡眠（> 6小时）无打卡时段，精准算出抽烟时间间隔。
  - **高峰时段窗口**: 自动分析 24 小时分布，找出吸烟最密集的窗口（如 `14:00 - 16:00`）。
  - **控烟达标预警**: 对比自定义的目标上限，实时显示“控烟达标”或“超标预警”。

### 3. 📦 烟盒管理与灵活计价规则
- 支持添加多种香烟品牌与每包支数设置（默认 20 支/包）。
- **两种计价规则**:
  - **单包零售价规则**: 直接输入单包价格（例如：¥25.00 / 包）。
  - **单条零售价规则**: 输入整条（如10包）零售价格（例如：¥230.00 / 条）。
- **自动折算与预览**: 实时自动换算并展示单包折算价、整条折算价以及精确到“单支”的成本。

### 4. 🎯 戒烟/控烟目标与 Gemini AI 健康助手
- **量化目标**: 设定每日抽烟上限支数、目标抽烟间隔时间。
- **财务目标**: 设定每月香烟预算与期望节省金额。
- **Gemini AI 健康顾问**: 结合用户的实际打卡频率、高峰时段和开销数据，生成温和、有建设性的个性化控烟建议与心理支持。

### 5. 🌐 多语言与多货币支持
- **一键切换语言**: 支持 **简体中文 (ZH)** 与 **English (EN)** 全界面无缝切换（包含所有提示框、图表图例、弹窗及 AI 建议）。
- **多货币符号**:
  - **人民币 (¥)**, **美元 ($)**, **欧元 (€)**, **英镑 (£)**, **日元 (¥)**, **韩元 (₩)**, **港币 (HK$)**。

### 6. 💾 100% 离线隐私保护与数据备份
- 基于 **Room 数据库** 本地持久化存储，无需强制联网，彻底保护个人健康与财务隐私。
- **槟榔独立监控**：通过底部中央的烟草/槟榔切换按钮选择监控主体；首页记录食用、分析页查看趋势与开销、品牌页管理品类（每包价格、颗数）、设置页单独配置每日上限和月预算。烟草统计与槟榔统计互不混用；本地备份包含槟榔记录。
- **独立演示数据**：两个模式的“数据与同步”各有 Demo 开关；槟榔演示记录带独立标记，关闭时仅移除演示记录，保留真实记录与品牌。
- **JSON 数据导入与导出**: 支持一键将所有香烟品牌、打卡记录与目标设置导出为 JSON 文件，或从备份文件快速恢复。
- **数据重置**: 提供安全清空数据库功能，方便重新开始。

---

## 🛠️ 技术栈与架构设计

| 架构分层 / 模块 | 使用技术 / 库 | 说明 |
| :--- | :--- | :--- |
| **开发语言** | Kotlin 2.0+ | 100% Kotlin 编写，类型安全与高可读性 |
| **UI 界面框架** | Jetpack Compose + Material 3 | 声明式 UI，支持 Material 3 动态色彩与深色模式 |
| **架构模式** | MVVM (Model-View-ViewModel) | 单向数据流，结合 StateFlow 与 Coroutines 响应式编程 |
| **本地持久化** | Room Database + KSP | SQLite 抽象层，KSP 高性能代码生成 |
| **图表绘制** | Jetpack Compose Custom Canvas | 纯手绘 Canvas 交互图表，流畅 60fps 体验 |
| **AI 智能建议** | Google Gemini API | 接入 Gemini 服务端/客户端接口生成健康建议 |
| **序列化解析** | `kotlinx.serialization` | 高效 JSON 解析，用于数据备份与恢复 |

---

## 📁 项目目录结构

```
app/src/main/java/com/example/
├── data/
│   ├── Cigarette.kt            # 香烟品牌与价格规则实体类 (Room Entity)
│   ├── SmokingLog.kt           # 打卡记录实体类 (Room Entity)
│   ├── Goal.kt                 # 目标与习惯设置实体类 (Room Entity)
│   ├── AppDatabase.kt          # Room 数据库定义与 Migration
│   └── SmokingDao.kt           # 数据访问对象 (DAO)
├── ui/
│   ├── i18n/
│   │   └── AppStrings.kt       # 多语言国际化字典 (中英双语)
│   ├── SmokingApp.kt           # 主界面、各 Tab 视图、弹窗及自定义 Canvas 图表
│   └── SmokingViewModel.kt     # MVVM ViewModel，状态管理与趋势分析算法
└── MainActivity.kt             # 应用入口与 Edge-to-Edge 边到边布局配置
```

---

## 🚀 编译与运行说明

### 环境要求
- **Android Studio**: 建议使用 Ladybug (2024.2.1) 或更高版本
- **JDK**: Version 17
- **Android SDK**: API Level 34 (最低支持 SDK 26 / Android 8.0)

### 构建步骤

1. **克隆项目仓库**:
   ```bash
   git clone <repository-url>
   cd smoking-tracker
   ```

2. **在 Android Studio 中打开** 并等待 Gradle 依赖同步完成。

3. **使用 Gradle 命令行编译 Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

---

## ❓ 常见问题 (FAQ)

### Q: 修改或同步烟盒数据中的香烟价格，会影响历史的吸烟开销数据吗？
**A: 不会。** 每次打卡（记录吸烟）时，系统都会实时计算并保存一条 **独立的花费快照 (Snapshot Cost)**。因此，无论是修改已有香烟的价格、调整单包/整条计价规则，还是通过 JSON URL 同步更新品牌数据，都**仅对后续新增的打卡生效**，历史已产生的开销记录与统计均保持原样不变，确保财务数据的准确与真实。

### Q: 如何解决 GitHub Actions 打包出的应用每次签名不同导致无发覆盖更新（提示签名冲突需卸载重装）的问题？
**A:** Android 系统要求覆盖更新应用时必须使用完全相同的签名证书。产生此问题的原因通常是构建时动态生成了新的临时密钥。
1. **开箱即用（推荐提交项目基准密钥）**：请确保仓库根目录下的 `debug.keystore.base64` 文件已提交推送至 GitHub 仓库。GitHub Actions 会在每次打包时自动解密该基准密钥进行签名，确保生成的每一个 APK 签名完全一致。
2. **自定义正式签名（通过 GitHub Secrets）**：
   - 将你的 `.jks` 或 `.keystore` 密钥转码为 Base64 文本（例如执行命令 `base64 -w 0 my-upload-key.jks > key.txt`）。
   - 进入 GitHub 仓库页面 -> **Settings** -> **Secrets and variables** -> **Actions**。
   - 新增一个名为 `RELEASE_KEYSTORE_BASE64` 的 Secret，将其内容粘贴进去。
   - GitHub Actions 在构建时会自动优先解密该正式密钥并完成签名，确保版本升级无缝覆盖安装。

---

## ☕ 赞赏支持

如果榔烟记对你有所帮助，欢迎自愿扫码赞赏，支持后续开发。点击图片可查看原图。

<table align="center">
  <tr><th>支付宝</th><th>微信支付</th></tr>
  <tr>
    <td align="center" valign="top"><a href="assets/alipay_square.png"><img src="assets/alipay_square.png" width="260" alt="支付宝赞赏二维码" /></a></td>
    <td align="center" valign="top"><a href="assets/wechat_pay.png"><img src="assets/wechat_pay.png" width="260" alt="微信支付赞赏码" /></a></td>
  </tr>
</table>

---

## 📄 协议与版权

- **开源协议**: 本项目遵循 **[MIT License](LICENSE)** 协议。
- **版权所有**: © 2026 nonion (nonion.pl@gmail.com)。

---

<p align="center">Crafted with ❤️ using Jetpack Compose & Kotlin</p>
