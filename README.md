# GN Management (Golden Nest Society Management App) 

A modern Android application built with **Kotlin** and **Jetpack Compose** designed for residential society management, amenity booking, and real-time community announcements.

---

##  Features

- **🏛️ Amenity & Hall Booking**:
  - Book community halls and club facilities with real-time slot conflict detection.
  - Interactive hourly duration slider and dynamic pricing calculations.
  - Instant confirmation and booking history.

- ** Community Announcements & Push Notifications**:
  - Direct integration with **Firebase Cloud Messaging (FCM)** using secure HTTP v1 API.
  - Broadcast notifications directly to all society residents under topic-based subscriptions.

- ** Modern Material 3 UI**:
  - Built 100% with **Jetpack Compose** for smooth, responsive native Android UI.
  - Dynamic theming with customized color palettes, typography, and clean card layouts.

- ** Maintenance & Billing Overview**:
  - Transparent pricing breakdowns, status chips, and automated invoice calculation.

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3)
- **Backend / Cloud**: Firebase Cloud Messaging (FCM), Google Services
- **Build System**: Gradle with Kotlin DSL (`build.gradle.kts`)
- **Target Android SDK**: Android 14+ (API 34/35)

---

##  Project Structure

```
GNManagement/
├── app/
│   ├── src/main/
│   │   ├── java/com/society/buildingmanager/
│   │   │   ├── MainActivity.kt                # Primary Jetpack Compose UI & navigation
│   │   │   ├── MyFirebaseMessagingService.kt  # FCM background & foreground handling
│   │   │   └── ui/theme/                      # Theme, Color, and Typography definitions
│   │   ├── res/                               # App icons, strings, and XML configurations
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts                       # App-level dependencies & plugins
├── gradle/                                    # Gradle wrapper & version catalogs
├── build.gradle.kts                           # Project-level build configuration
├── settings.gradle.kts                        # Gradle settings
└── README.md                                  # Project documentation
```

---

##  Getting Started

### 1. Prerequisites
- **Android Studio** (Koala / Ladybug or newer recommended)
- **JDK 17+**
- Android device or Emulator running **API 26+**

### 2. Setup & Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/dhyeyd1810/GN-manager.git
   ```
2. Open the project folder in **Android Studio**.
3. Allow Gradle to sync all dependencies automatically.
4. Set up your `google-services.json` in the `/app` directory for Firebase integration.
5. Click **Run (`Shift + F10`)** to launch the app on your emulator or physical device.

---

## 📄 License
This project is developed for Golden Nest Building / Society Management.
