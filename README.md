# 🚬 Betel & Smoke Tracker (榔烟记)

> **A smart, privacy-first, offline-ready Android application built with Jetpack Compose & Material 3 for separately tracking tobacco and betel habits, social sharing, expenses, reduction goals, and AI-powered health advice.**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_100%25-purple.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Platform-Android_12%2B-green.svg)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4.svg)](https://developer.android.com/jetpack/compose)

---

🌐 **Languages / 语言选择**:  
**[English (Default)](README.md)** | **[简体中文](README_ZH-CN.md)**

---

## 🖼️ Overview & Showcase

<p align="center">
  <img src="artwork/launcher_icon.svg" width="120" height="120" alt="Betel & Smoke Tracker Icon" style="border-radius: 24px;" /><br/>
  <b>Betel &amp; Smoke Tracker (榔烟记)</b><br/>
  <i>Official App Icon</i>
</p>

<br/>

![榔烟记 · Betel & Smoke Tracker 横幅](artwork/readme-banner.svg)

### 📱 Main Navigation Screens

```text
┌───────────────────────────────────────────────────────────┐
│                   Betel & Smoke Tracker                   │
├────────┬──────────┬──────────────┬─────────┬─────────────┤
│ Home   │ Analysis │ Tobacco ↔    │ Brands  │ Settings    │
│        │          │ Betel switch │         │             │
└────────┴──────────┴──────────────┴─────────┴─────────────┘
```

The switch changes all four sections together; only one subject’s records and statistics are shown at a time.

---

## ✨ Key Features & Highlights

### 1. 🚬 3-Scenario Social Logging
Standard smoking trackers treat all cigarettes the same. **Betel & Smoke Tracker** accurately categorizes smoking events based on real-world social context:

- **Self Purchase (自购自抽)**: Cigarettes you bought and smoked yourself. Counted towards **both** personal health smoking volume and personal financial expenses.
- **Social Shared Out (社交递烟)**: Handing your own cigarette to a friend or colleague. Counted as a **financial expense**, but **not** added to your personal health smoking intake.
- **Social Received In (社交接烟)**: Accepting a cigarette offered by someone else. Counted towards your **personal health smoking volume**, but recorded at **$0 cost** (free / gifted).

### 2. 📊 Smart Trends & Interactive Charts
- **Time Range Selector**: View analytics for **Last 7 Days**, **This Week**, **This Month**, **Specific Month**, **Specific Year**, or a **Custom Date Range**.
- **Dual Chart Modes**:
  - **Stacked Bar Chart**: Displays daily composition of Self Smoked, Shared Out, and Received In.
  - **Line Chart**: Compares total smoked volume against social received volume.
- **Canvas Gestures & Inspection**: Tap any bar or line node to highlight details in a live summary banner.
- **Automated Insights**:
  - **Daily Average**: Real-time average daily sticks.
  - **Average Interval**: Mean time gap between logs (excluding overnight gaps > 6 hours).
  - **Peak Smoking Window**: Identifies most frequent 2-hour smoking slots (e.g. `14:00 - 16:00`).
  - **Target Warning**: Visual indicators showing if current habits stay within target limits.

### 3. 📦 Cigarette Box & Flexible Pricing Rules
- Support for multiple cigarette brands and custom pack sizes (e.g., 20 sticks per pack).
- **Dual Pricing Modes**:
  - **Per Pack Rule (单包规则)**: Enter price per pack (e.g., $10.00 / pack).
  - **Per Carton Rule (单条规则)**: Enter price per carton (e.g., $90.00 / 10 packs).
- **Live Cost Calculation**: Automatically computes exact unit cost down to the individual stick.

### 4. 🎯 Reduction Goals & Gemini AI Health Assistant
- **Habit Targets**: Set daily limit ceilings and target smoking intervals.
- **Financial Targets**: Define monthly cost budgets and money-saving goals.
- **Gemini AI Health Advisor**: Integrated AI companion providing empathetic, non-judgmental advice, craving management tips, and progress encouragement tailored to your actual data.

### 5. 🌐 Multi-Language & Multi-Currency
- **Language Switch**: Seamless toggle between **English (EN)** and **Simplified Chinese (ZH)** across all screens, charts, tooltips, dialogs, and AI prompts.
- **Currency Switch**: Choose your preferred currency symbol:
  - **USD ($)**, **CNY (¥)**, **EUR (€)**, **GBP (£)**, **JPY (¥)**, **KRW (₩)**, **HKD (HK$)**.

### 6. 💾 100% Offline Privacy & Data Backup
- Powered by **Room Database** for high performance and offline independence.
- **Separate betel nut tracker**: Use the center tobacco/betel switch to change all four sections together. The betel home logs consumption and sharing, Analysis shows trends and spending, Brands manages pack price and piece count, and Settings has independent daily limits and monthly budgets. Betel data is included in local backups and never mixed into cigarette totals.
- **Separate demo switches**: Both subjects offer sample data in Data & Sync. Turning off betel demo mode removes only marked sample records, not personal logs or brands.
- **JSON Backup & Restore**: Export full database state to a JSON file and restore anytime.
- **Clear Data**: Full reset options for privacy and fresh starts.

---

## 🛠️ Tech Stack & Architecture

| Layer / Aspect | Technology / Library | Description |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0+ | Modern type-safe programming language |
| **UI Framework** | Jetpack Compose + Material 3 | Declarative UI framework with dynamic M3 themes |
| **Architecture** | MVVM (Model-View-ViewModel) | Unidirectional data flow with StateFlow & Coroutines |
| **Local Persistence**| Room Database + KSP | SQLite abstraction layer with type-safe DAOs |
| **Analytics Visualization** | Jetpack Compose Custom Canvas | High-performance 60fps custom rendered interactive charts |
| **AI Integration** | Google Gemini API | REST / Server-side AI integration for health insights |
| **Serialization** | `kotlinx.serialization` | Fast JSON parsing for data backup & restore |

---

## 📁 Directory Structure

```
app/src/main/java/com/example/
├── data/
│   ├── Cigarette.kt            # Room Entity for Cigarette Brands & Pricing
│   ├── SmokingLog.kt           # Room Entity for Individual Smoking Logs
│   ├── Goal.kt                 # Room Entity for Targets & Goals
│   ├── AppDatabase.kt          # Room Database Definition & Migrations
│   └── SmokingDao.kt           # Data Access Object for CRUD Operations
├── ui/
│   ├── i18n/
│   │   └── AppStrings.kt       # Multi-language String Dictionary (EN & ZH)
│   ├── SmokingApp.kt           # Main Compose UI, Screens, Dialogs & Custom Charts
│   └── SmokingViewModel.kt     # MVVM State Manager & Trend Calculations
└── MainActivity.kt             # Application Entry Point & Edge-to-Edge Setup
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or newer recommended
- **JDK**: Version 17
- **Android SDK**: API Level 34 (Minimum SDK: 26 / Android 8.0)

### Building the Project

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd smoking-tracker
   ```

2. **Open in Android Studio** and sync Gradle dependencies.

3. **Build APK via Gradle Command**:
   ```bash
   ./gradlew assembleDebug
   ```

---

## ❓ Frequently Asked Questions (FAQ)

### Q: Will updating a cigarette brand's price change my historical smoking expense data?
**A: No.** Every smoking log takes a financial snapshot of the exact unit cost (`cost`) at the moment you hit the log button. Modifying a cigarette brand's price, changing pricing rules, or syncing brand data from a JSON URL will only apply to **future** logs. All past expense records remain intact and historically accurate.

### Q: How to prevent "signature inconsistent" error when updating APKs built by GitHub Actions?
**A:** Android requires all updates to be signed with the exact same keystore certificate.
1. **Default Method (Automatic)**: Ensure `debug.keystore.base64` in the project root is committed to your GitHub repository. GitHub Actions will automatically decode this file and sign every build with the exact same certificate.
2. **Custom Release Keystore (Recommended)**:
   - Convert your `.jks` or `.keystore` file to Base64 (`base64 -w 0 my-release-key.jks > key.txt`).
   - Go to your GitHub Repository -> **Settings** -> **Secrets and variables** -> **Actions**.
   - Create a Secret named `RELEASE_KEYSTORE_BASE64` with the Base64 string value.
   - GitHub Actions will automatically decode and use your persistent release keystore.

---

## ☕ Support the Project

If this app is helpful, you can optionally support its development by scanning either appreciation code. Click an image to view it at full size.

<table align="center">
  <tr><th>Alipay</th><th>WeChat Pay</th></tr>
  <tr>
    <td align="center" valign="top"><a href="assets/alipay_square.png"><img src="assets/alipay_square.png" width="260" alt="Alipay appreciation QR code" /></a></td>
    <td align="center" valign="top"><a href="assets/wechat_pay.png"><img src="assets/wechat_pay.png" width="260" alt="WeChat Pay appreciation code" /></a></td>
  </tr>
</table>

---

## 📄 License & Copyright

- **License**: Released under the **[MIT License](LICENSE)**.
- **Copyright**: © 2026 nonion (nonion.pl@gmail.com).

---

<p align="center">Crafted with ❤️ using Jetpack Compose & Kotlin</p>
