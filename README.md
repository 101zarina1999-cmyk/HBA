# HBA - Android Application

A modern Android application built with Kotlin and Jetpack Compose.

## Project Structure

```
HBA/
├── app/                          # Main Android application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/hba/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── ui/theme/
│   │   │   │       ├── Theme.kt
│   │   │   │       ├── Color.kt
│   │   │   │       └── Type.kt
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── themes.xml
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   └── androidTest/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Gradle settings
└── .gitignore                    # Git ignore rules
```

## Requirements

- Android SDK 34
- Minimum SDK 21
- Kotlin 1.8.0
- Gradle 8.0.0

## Building the Project

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build the project: `Ctrl+F9` (or `Cmd+F9` on Mac)
5. Run on emulator or device

## Technologies Used

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Build System**: Gradle (Kotlin DSL)
- **Target API**: 34

## License

MIT License
