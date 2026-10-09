# LinkFlow — Liquid-Glass Media Downloader for Android

LinkFlow is a native Android application built with **Kotlin** and **Jetpack Compose (Material 3)** for inspecting, streaming, and managing authorized **MP4 video** and **MP3 audio** files with a custom **Liquid-Glass** aesthetic, real-time HTTP byte-stream progress animations, and Google Play Store-style pull-to-refresh.

---

## 1. Architecture & Repository Structure

```text
├── .github/workflows/
│   └── android-ci.yml                     # GitHub Actions CI for tests & APK build
├── .env.example                           # Safe environment variable template
├── app/
│   ├── build.gradle.kts                   # App module configuration (AGP, Compose, Room, KSP, OkHttp)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml        # Permissions & FileProvider declarations
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt        # Edge-to-edge host, navigation & overlay coordinator
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── Entities.kt              # Room entities (DownloadTaskEntity, RecentAnalysisEntity)
│       │   │   │   │   ├── LinkFlowDatabase.kt      # Room DAO & database singleton
│       │   │   │   │   └── PreferencesRepository.kt # Jetpack DataStore user preferences
│       │   │   │   ├── model/
│       │   │   │   │   └── MediaModels.kt           # Domain models, state machine enum, quality options
│       │   │   │   └── service/
│       │   │   │       ├── DownloadQueueManager.kt  # Coroutine OkHttp byte-stream downloader & file writer
│       │   │   │       └── MediaAnalyzerEngine.kt   # URL validation, SSRF guard, HTTP HEAD/Range & Archive.org/oEmbed adapters
│       │   │   ├── ui/
│       │   │   │   ├── LinkFlowViewModel.kt         # Reactive MVVM state management
│       │   │   │   ├── components/
│       │   │   │   │   └── LiquidGlassComponents.kt # Reusable glass cards, pull-to-refresh, bottom nav, waveform
│       │   │   │   ├── screens/
│       │   │   │   │   ├── ActiveDownloadProgressModal.kt # Full-screen liquid-glass progress ring & completion modal
│       │   │   │   │   ├── DownloadsQueueScreen.kt        # Queue manager & job details sheet
│       │   │   │   │   ├── HomeScreen.kt                  # Main dashboard, smart URL bar, format selector & activity
│       │   │   │   │   ├── LibraryScreen.kt               # Media library & real Android MediaPlayer/VideoView modal
│       │   │   │   │   ├── QualitySelectorSheet.kt        # Multi-resolution MP4 & MP3 bitrate selector sheet
│       │   │   │   │   ├── SettingsAndInfoScreens.kt      # Settings, Supported Sources, Privacy & About modals
│       │   │   │   │   └── SplashScreen.kt                # Animated splash screen with cobalt-blue liquid-glass orb
│       │   │   │   └── theme/
│       │   │   │       ├── Color.kt                 # Midnight & liquid-glass palette tokens
│       │   │   │       ├── Theme.kt                 # LinkFlowTheme & CompositionLocal design tokens
│       │   │   │       └── Type.kt                  # Bundled Space Grotesk, Plus Jakarta Sans & JetBrains Mono
│       │   │   └── util/
│       │   │       └── FormatUtils.kt               # Byte/speed/ETA formatting & FileProvider share/open intents
│       │   └── res/                                 # Bundled fonts, adaptive launcher icons, strings & XML paths
│       └── test/java/com/example/
│           ├── ExampleUnitTest.kt                   # JVM unit tests for URL validation, SSRF guard & byte formatting
│           └── ExampleRobolectricTest.kt            # Robolectric tests for Room persistence & Android resources
├── build.gradle.kts                       # Root Gradle configuration
├── gradle/libs.versions.toml              # Centralized dependency version catalog
└── settings.gradle.kts                    # Project settings (rootProject.name = "LinkFlow")
```

---

## 2. Implemented Features

- **Animated Splash Screen (`SplashScreen.kt`)**:
  - Displays the custom 3D cobalt-blue liquid-glass orb icon with rotating luminous ring, radial light rays, floating bokeh particles, animated brand typography, and reduced-motion accessibility support.
- **Smart URL Analyzer & SSRF Security (`MediaAnalyzerEngine.kt`)**:
  - Validates URL syntax and protocol (`https` / `http`).
  - Blocks loopback (`localhost`, `127.0.0.1`, `::1`) and private RFC-1918 network ranges (`10.x`, `192.168.x`, `172.16-31.x`, `169.254.x`) both on initial URL input and across HTTP redirects via an OkHttp network interceptor.
  - Performs real HTTP `HEAD` and `Range: bytes=0-0` inspection to determine genuine `Content-Type` and `Content-Length`.
  - Queries the **Internet Archive Metadata API** (`https://archive.org/metadata/<id>`) when an `archive.org/details/<id>` URL is provided, returning all genuinely available MP4 resolutions and MP3/FLAC/OGG audio bitrates.
  - Queries official **oEmbed** endpoints for YouTube, Vimeo, TikTok, and SoundCloud to verify metadata while enforcing DRM and Terms-of-Service protections.
- **Real HTTP Byte-Stream Download Engine (`DownloadQueueManager.kt`)**:
  - Streams authorized files over HTTP/HTTPS using `OkHttpClient` with `Range` header support for pause and resume.
  - Persists job states (`QUEUED → PREPARING → DOWNLOADING → COMPLETED`, plus `PAUSED`, `FAILED`, `CANCELLED`) in **Room SQLite** (`LinkFlowDatabase`).
  - Reports real transferred bytes, total bytes, transfer speed, and remaining time to the UI.
- **Media Library & Native Playback (`LibraryScreen.kt`)**:
  - Lists only genuinely completed downloads stored on disk.
  - Supports Grid/List views, search, MP4/MP3 filtering, sorting (Date, Name, Size, Type), file renaming, deletion with confirmation, native Android `MediaPlayer` / `VideoView` playback, and `FileProvider` sharing.
- **Google Play Store-Style Pull-to-Refresh (`LiquidGlassComponents.kt`)**:
  - Custom pointer gesture handling with natural pull resistance and a floating liquid-glass refresh indicator.

---

## 3. Provider Limitations & Configuration

- **Direct Media Links & Internet Archive**: Fully active out of the box with zero external configuration required.
- **Social Platforms (YouTube, Vimeo, TikTok, SoundCloud, Instagram)**: Official public `oEmbed` metadata lookup works out of the box. Direct stream extraction from DRM-protected or ToS-restricted social platforms is intentionally disabled to comply with Google Play Developer Policy and copyright law.

---

## 4. How to Build, Test & Run

### Prerequisites
- JDK 17+
- Android SDK (API 36 compileSdk, minSdk 24)

### Commands
- **Run Unit & Robolectric Tests**:
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Build Debug APK**:
  ```bash
  gradle :app:assembleDebug
  ```
- **Run Android Lint**:
  ```bash
  gradle :app:lintDebug
  ```
