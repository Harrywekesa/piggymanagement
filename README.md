# 🐖 Pig Husbandry & Management System (PHMS)

![Android Version](https://img.shields.io/badge/Platform-Android-green.svg)
![App Version](https://img.shields.io/badge/Version-v1.0.0-blue.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-darkgreen.svg)
![License](https://img.shields.io/badge/License-MIT-orange.svg)

**PHMS** is an advanced, offline-first Android application designed for commercial pig farmers, farm managers, and agricultural extension officers. It delivers end-to-end digital farm management—from pig lifecycle tracking and breeding schedules to automated feed formulation (in kg and grams), PnL financial accounting, and market buyer notifications.

---

## 👨‍💻 Lead Developer & Contact Details

- **Lead Developer**: **Harrison Wekesa**
- **WhatsApp / Phone**: `+254791496057`
- **Application Version**: `v1.0.0`
- **GitHub Repository**: [https://github.com/Harrywekesa/piggymanagement](https://github.com/Harrywekesa/piggymanagement)

---

## ✨ Key Features & Modules

### 🌾 1. Feed Formulator & Auto-Calculations
- **Dynamic Batch Output**: Enter any desired feed batch quantity (e.g. `50 kg`, `200 kg`, `500 kg`, `1000 kg`) or custom amount.
- **Auto-Computed Measurements**: Calculates exact ingredient weights in **kilograms (kg) and grams (g)** in real-time.
- **Uneditable Measurement Boxes**: Formulated ingredient weights are displayed inside locked, read-only boxes to prevent accidental edits during milling.
- **Preset Formulas**: Creep Starter, Weaner Starter, Grower Mash, Finisher Meal, Sow & Weaner.
- **Display Unit Switcher**: Toggle between `kg & g`, `kg only`, or `grams only`.

### 🐖 2. Pig Herd & Lifecycle Tracking
- **Stage Categorization**: Piglets, Weaners, Growers, Finishers, Breeding Sows, and Boars.
- **Farm Pen & Assignment**: Track pens, breed types, birth dates, weights, and health status.
- **Stage History**: Full visual audit trail showing when pigs transition across growth stages.

### 🤰 3. Breeding & Farrowing Tracker
- **Pregnancy Inception Tracker**: Record mating dates and calculate expected farrowing dates automatically (114 days gestation).
- **Gestation Countdown**: Live countdown for pregnant sows with alerts when farrowing is imminent.

### 🛒 4. Buyer Actions & Market Readiness Notifications
- **Market Ready Pigs**: Automatically identifies pigs reaching market weight (~70–100 kg).
- **Buyer Inquiry & Sales Workflow**: Generate sale listings for external pork buyers, track sold batches, and record sales revenue directly into PnL accounting.
- **Farmer Market Notifications**: In-app alerts notify farmers when pigs reach optimal market weight for buyers.

### 💰 5. PnL Financial Management
- Track feed purchases, veterinary costs, equipment overheads, and pig sales revenue.
- Comprehensive profit and loss breakdown per batch and overall farm performance.

### 🚀 6. Direct GitHub Releases & In-App Updates
- **Automatic GitHub Check**: The app checks the [GitHub Releases API](https://api.github.com/repos/Harrywekesa/piggymanagement/releases/latest) for updates.
- **Direct Update Download**: When a new version is released on GitHub, users receive an in-app notification prompt to download and install the update directly.
- **Install Insights & Telemetry**: Tracks local launch counts, device metadata, and installation date to optimize app performance.

---

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3 Dark Theme) |
| **Architecture** | MVVM (Model-View-ViewModel) + StateFlow |
| **Database** | Room SQLite Database (Offline-first) |
| **Concurrency** | Kotlin Coroutines & Flow |
| **Build System** | Gradle 8.x + Android Gradle Plugin 8.x |
| **Min SDK** | API 24 (Android 7.0) |
| **Target SDK** | API 34 (Android 14) |

---

## 🚀 Building & Installing

### Prerequisites
- JDK 17 or higher
- Android SDK 34
- Connected Android Device or Emulator

### Commands

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Harrywekesa/piggymanagement.git
   cd piggymanagement
   ```

2. **Build Debug APK & Install on Device**:
   ```bash
   ./gradlew.bat installDebug
   ```

3. **Build Release APK**:
   ```bash
   ./gradlew.bat assembleRelease
   ```
   The APK will be generated at `app/build/outputs/apk/release/app-release-unsigned.apk`.

---

## 📲 Direct GitHub Releases Workflow

To release a new update to users:
1. Update `versionName` and `versionCode` in `app/build.gradle.kts`.
2. Push your changes to the `main` branch on GitHub.
3. Create a **New Release** on GitHub with a version tag (e.g. `v1.1.0`) and attach the built `app-release.apk`.
4. Users running the PHMS app will automatically receive an in-app update notification pointing directly to the new release!

---

## 📄 License & Copyright

© 2026 **Harrison Wekesa**. All Rights Reserved.  
Distributed under the MIT License. Contact Harrison Wekesa (`+254791496057`) for custom extension modules or support.
