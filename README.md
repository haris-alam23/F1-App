# F1 Telemetry App

## Overview

The **F1 Telemetry App** is an Android application that visualizes real Formula 1 telemetry and session data in a clean, interactive way. The app allows users to explore driver performance, compare drivers within the same session, and view race and qualifying results using real-world F1 data.

This project was built as part of a CSCI4237: Software Design for Handheld Devices, with a focus on API integration, modern Android UI, and clear data presentation.

## Features

* **Live & Historical F1 Data**

  * Fetches telemetry and session data from the OpenF1 API

* **Telemetry Visualization**

  * Speed, throttle, brake, and gear data plotted over time

* **Driver Comparison**

  * Side-by-side comparison of two drivers in the same session

* **Session & Race Results**

  * View qualifying and race results by event

## Tech Stack

* **Kotlin**
* **Android Studio**
* **Jetpack Compose** (UI)
* **OpenF1 API** (race & telemetry data)
* **Firebase Authentication**
* **MVVM Architecture**

## App Architecture

The app follows an MVVM-based structure:

* **UI Layer**: Jetpack Compose screens and components
* **ViewModel Layer**: Handles state and business logic
* **Data Layer**: API services and repositories

## Project Structure

```text
.
├── .idea/
├── .kotlin/
├── app/
│   ├── src/
│   │   ├── androidTest/
│   │   │   └── java/com/example/f1telemetry/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/f1telemetry/
│   │   │   │   ├── DriverComparisonActivity.kt
│   │   │   │   ├── HomeScreenActivity.kt
│   │   │   │   ├── LoginActivity.kt
│   │   │   │   ├── ResultsActivity.kt
│   │   │   │   ├── SignUpActivity.kt
│   │   │   │   └── TelemetryActivity.kt
│   │   │   └── res/
│   │   └── test/
│   │       └── java/com/example/f1telemetry/
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── proguard-rules.pro
│
├── gradle/
│   └── wrapper/
├── libs.versions.toml
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── .gitignore
```

> Notes:
>
> * `app/src/main/java/com/example/f1telemetry/` contains the main activities/screens.
> * `app/src/main/res/` contains layouts, drawables, strings, and other Android resources.
> * `google-services.json` is required for Firebase Authentication.

```text
app/
├── ui/
│   ├── telemetry/
│   ├── comparison/
│   ├── results/
│   └── auth/
│
├── viewmodel/
├── data/
│   ├── api/
│   └── repository/
│
├── MainActivity.kt
└── build.gradle
```

## How to Run

1. Clone the repository
2. Open the project in Android Studio
3. Add Firebase configuration (`google-services.json`)
4. Build and run on an emulator or physical device

## Limitations

* Telemetry availability depends on OpenF1 API coverage
* No offline caching of telemetry data
* UI optimized for phone screens only

## Future Improvements

* Add lap-by-lap performance summaries
* Cache telemetry data for offline viewing
* Enhance comparison with sector-level breakdowns
* Add charts for tire and stint analysis
