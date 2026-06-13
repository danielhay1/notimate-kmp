# NotiMate

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0+-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-lightgrey.svg)](#)

A Kotlin Multiplatform engine for on-device, AI-driven notification interception, routing, and management. 

NotiMate utilizes local machine learning to categorize and filter inbound system alerts, drastically reducing cognitive load without exposing user notification payloads to external servers.

## Architecture

*   `:shared`: Kotlin Multiplatform module containing the domain layer, local storage, and on-device AI inference logic.
*   `:androidApp`: Native Android frontend utilizing Jetpack Compose.
*   `:iosApp`: Native iOS frontend utilizing SwiftUI.

## Getting Started

### Android
Requires Android Studio (Koala/Ladybug) and JDK 17+.
```bash
./gradlew :androidApp:assembleDebug
