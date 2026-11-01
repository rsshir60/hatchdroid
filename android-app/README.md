# HatchDroid Android App

Modern Jetpack Compose Android client for the **HatchZip** in-place archive extraction engine.

> [!IMPORTANT]
> **Community Project & Upstream Attribution:**
> HatchDroid is an **unofficial, independent community port** maintained by [@rsshir60](https://github.com/rsshir60).
> The core in-place extraction engine (`hatchzip-core`) was created and designed by **[xevrion/hatchzip](https://github.com/xevrion/hatchzip)** ([hatchzip.dev](https://hatchzip.dev)). We are not the official developers of the HatchZip project.

---

## Highlights

- **Jetpack Compose + Material 3:** Built completely with Declarative Compose UI following modern Android architecture.
- **High-Performance Rust Core:** Powered by the upstream `hatchzip-core` engine via JNI / UniFFI bindings (`libhatchzip_android.so`).
- **Android 15+ Ready (16 KB Page Aligned):** Shared libraries compiled with `-Wl,-z,max-page-size=16384` for modern Android memory paging requirements.
- **Dual-Progress Foreground Service:** Keeps extractions alive even when the app is in the background, updating live reclaimed and extracted metrics in the system notification drawer.
- **Crash-Proof & Resumable:** Automatically detects interrupted extractions and presents an instant one-tap **Resume** banner.
- **Pre-Flight Storage Telemetry:** Visual Before vs. After storage planning bar, preventing out-of-storage panics.

---

## Project Structure

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/dev/hatchzip/android/
│   │   │   ├── HatchZipApp.kt                 # Application class & initialization
│   │   │   ├── MainActivity.kt                # Single activity Compose host
│   │   │   ├── service/
│   │   │   │   └── ExtractionForegroundService.kt  # Background extraction service
│   │   │   ├── ui/
│   │   │   │   ├── components/                # StorageTelemetryCard, DualProgressCard, etc.
│   │   │   │   ├── screens/                   # HomeScreen, ExtractionScreen, SettingsScreen
│   │   │   │   └── theme/                     # Material 3 colors, shapes, typography
│   │   │   ├── util/                          # StorageUtil, PermissionUtil
│   │   │   └── viewmodel/                     # ExtractionViewModel & UiState
│   │   ├── jniLibs/
│   │   │   └── arm64-v8a/
│   │   │       └── libhatchzip_android.so     # 16 KB page-aligned Rust binary
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── scripts/
│   └── build-release.ps1                      # 16 KB alignment & packaging pipeline
└── build.gradle.kts
```

---

## Build Instructions

### 1. Compile the Rust Engine
Before building the APK, compile the native Rust library for Android:

```powershell
# From the repository root:
.\scripts\build-android.ps1 -Release
```

Or on Linux/macOS:
```bash
./scripts/build-android.sh --release
```

### 2. Build the Android App

Using Gradle:
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

Or run the end-to-end automated pipeline (compiles Rust, verifies 16 KB ELF alignment, builds and signs APK/AAB):
```powershell
.\scripts\build-release.ps1
```

The output APK will be located in `app/build/outputs/apk/release/`.
