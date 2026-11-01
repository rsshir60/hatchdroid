<div align="center">

  <img src="assets/icon/hatchdroid_store_icon.svg" width="128" height="128" alt="HatchDroid Logo" />

  # HatchDroid

  **In-place archive extraction for Android**<br>
  *An unofficial community fork & Android port powered by the [`hatchzip-core`](https://github.com/xevrion/hatchzip) engine.*

  <p>
    <a href="https://github.com/rsshir60/hatchdroid/actions"><img src="https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square" alt="Build Status" /></a>
    <a href="#license--attribution"><img src="https://img.shields.io/badge/License-MIT%20%7C%20Apache--2.0-blue?style=flat-square" alt="License" /></a>
    <a href="#android-app-hatchdroid"><img src="https://img.shields.io/badge/Android-API%2033%2B%20(16%20KB%20Aligned)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" /></a>
    <a href="https://github.com/xevrion/hatchzip"><img src="https://img.shields.io/badge/Upstream-xevrion%2Fhatchzip-orange?style=flat-square" alt="Upstream HatchZip" /></a>
    <img src="https://img.shields.io/badge/Telemetry-100%25%20Offline-success?style=flat-square" alt="Zero Telemetry" />
    <a href="https://github.com/rsshir60/hatchdroid/stargazers"><img src="https://img.shields.io/github/stars/rsshir60/hatchdroid?style=flat-square" alt="GitHub Stars" /></a>
  </p>

  <h4>
    <a href="#important-community-fork-notice">Project Notice</a> •
    <a href="#the-problem--solution">The Breakthrough</a> •
    <a href="#android-app-hatchdroid">Android App</a> •
    <a href="#desktop--cli-upstream-hatchzip">Desktop & CLI</a> •
    <a href="#supported-formats">Formats</a> •
    <a href="#how-in-place-extraction-works">How It Works</a> •
    <a href="#building-from-source">Build from Source</a>
  </h4>

  <br>

  <img src="docs/media/hatchzip.gif" width="760" alt="HatchZip/HatchDroid extracts from the end and truncates the source archive as files hatch" />

</div>

---

## ⚠️ Important Community Fork Notice

> [!IMPORTANT]
> **Unofficial Community Fork & Android Port:**
> This repository is an **independent community fork** maintained by [@rsshir60](https://github.com/rsshir60). We are **not** the official developers or owners of the HatchZip project.
>
> All credit for the invention of the in-place extraction concept, the core Rust engine (`hatchzip-core`), and the original desktop applications belongs to **[xevrion/hatchzip](https://github.com/xevrion/hatchzip)**. Please visit the official project website at **[hatchzip.dev](https://hatchzip.dev)** for official desktop builds and upstream releases.
>
> **Focus of this Fork:** Our work focuses solely on extending the ecosystem to mobile devices through **HatchDroid** — introducing a modern Jetpack Compose interface, Android foreground service, UniFFI / JNI bindings, and 16 KB ELF page alignment for Android 15+.

---

## The Problem & Solution

Traditional unarchivers (WinRAR, 7-Zip, Android system extractors) require **2x disk space**: enough space for the compressed archive **and** all uncompressed files at the same time.

```
Traditional Extraction:
[  30 GB Archive  ] + [    32 GB Unpacked Files    ] = 62 GB Peak Storage Required
❌ On a phone with only 35 GB free, extraction fails with "Not enough storage".

HatchDroid In-Place Extraction (powered by hatchzip-core):
[ 15 GB Tail Cut ] ──(space reclaimed)──> [ 16 GB Written Files ]
✅ Unpacks progressively from the tail while shrinking the source archive.
✅ Peak storage required is only the uncompressed size + a tiny working buffer (~33 GB total).
```

| Scenario | Standard Mobile Extractors | HatchDroid |
|:---|:---:|:---:|
| **30 GB game archive on 35 GB free storage** | ❌ Fails (*Out of Space*) | ✅ **Extracts smoothly** |
| **Space reclamation** | ⏳ Reclaimed only *after* extraction finishes | ⚡ **Continuous, on-the-fly tail truncation** |
| **Interrupted / Dead Battery** | ⚠️ Corrupts output or restarts from 0% | 🛡️ **Instant crash-safe resume** |
| **Integrity & Pre-flight check** | ❌ Modifies files blindly until disk fills | 🔍 **Simulates space plan before writing a single byte** |

---

## 📱 Android App (HatchDroid)

**HatchDroid** brings the power of the Rust `hatchzip-core` engine to mobile devices with a native, modern Jetpack Compose interface.

<div align="center">
  <img src="assets/icon/hatchdroid_store_icon.svg" width="96" height="96" alt="HatchDroid Android App Icon" />
</div>

### Key Features
- **True In-Place Storage Reclaimer:** Truncates archives on internal storage via native POSIX `ftruncate` or punches holes in streamed `tar.gz` files via `fallocate`.
- **Pre-Flight Space Telemetry:** Inspects archive tables before modifying disk bytes. Visualizes a clear **Before vs. After** storage bar to guarantee the extraction will fit.
- **Safety Buffer Enforcement:** Reserves a protected system buffer (default 2 GB) so Android never hits zero free space or destabilizes system apps.
- **Foreground Service with Live Dual-Progress:** Tracks both metrics in real-time in the notification drawer:
  - 🔻 **Archive Reclaimed:** e.g., `24.5 GB / 30.0 GB reclaimed`
  - 🔺 **Files Hatched:** e.g., `26.2 GB / 32.0 GB extracted`
- **Crash-Proof & Resumable:** If Android terminates the app or battery dies, state indices and flush barriers guarantee you can reopen HatchDroid and hit **Resume**.
- **Android 15+ Ready (16 KB Page Alignment):** Native Rust shared libraries (`libhatchzip_android.so`) are compiled with 16 KB ELF page alignment for Google's latest memory requirements.
- **Multi-Volume Support:** Detects split archives (`.zip.001`, `.part1.rar`, `.7z.001`) and purges each volume file immediately once its contents are extracted.
- **100% Offline & Private:** Zero advertisements, zero trackers, and zero network libraries included.

> [!NOTE]
> Read the complete Google Play Store declarations, privacy policy metadata, and store copy in [`docs/play_store_listing.md`](docs/play_store_listing.md).

---

## 💻 Desktop & CLI (Upstream HatchZip)

Desktop (GUI) and command-line (CLI) tools included in this monorepo are developed and maintained upstream by **[xevrion/hatchzip](https://github.com/xevrion/hatchzip)**.

For official desktop downloads, installer packages (MSI, DMG, DEB, RPM, AppImage), and verification attestations, visit:
- **Official Website:** [hatchzip.dev](https://hatchzip.dev)
- **Official GitHub Releases:** [xevrion/hatchzip/releases](https://github.com/xevrion/hatchzip/releases)

### Upstream CLI Usage Summary
```sh
# Preview space plan without modifying anything
hatchzip check huge_game.zip

# Extract in-place (shrinks source as files hatch)
hatchzip extract huge_game.zip

# Extract split archive (any volume name works)
hatchzip x game.7z.001

# Extract traditionally without modifying the archive
hatchzip extract archive.zip --keep-archive

# Resume an interrupted extraction after power loss or Ctrl+C
hatchzip extract huge_game.zip
```

---

## 📦 Supported Formats

| Format | Support Level | Space Reclaimed In Steps Of |
|:---|:---:|:---|
| **ZIP / ZIP64** | Full (Stored, Deflate, Deflate64, Bzip2, LZMA, PPMd, XZ, Zstd) | One file |
| **7-Zip (`.7z`)** | Full (LZMA, LZMA2, BCJ, Bzip2, Deflate, PPMd, Zstd) | One solid block |
| **RAR (RAR4 & RAR5)** | Full (Single file & multi-part volume sets like `.part1.rar`) | One file / volume |
| **TAR (uncompressed)** | Full (POSIX tar, GNU tar, long names, symlinks) | One file |
| **TAR.GZ / TGZ** | Full (punches filesystem holes behind forward decompressor) | 256 MiB chunks |
| **Split Sets** | Full (`.001`, `.002`, `.part1.rar`, `.z01`) | Volume by volume |
| **Encrypted Archives** | Full (ZipCrypto, AES-128, AES-256) | Same as unencrypted |

---

## ⚙️ How In-Place Extraction Works

Traditional archives place metadata tables at either end of the file:
1. **Reverse Ingestion:** For formats like ZIP and 7z, the central directory index is stored at the end. The engine reads this index and serializes a lightweight rescue state (`.hatchzip` recovery journal).
2. **Tail-to-Head Batching:** Files located physically at the end of the archive are decoded and written to disk first.
3. **Atomic Flush Barrier:** Each extracted file is written, flushed to the underlying hardware storage (`fsync`), and verified.
4. **Tail Truncation:** Once a batch is safely secured on disk, the archive file is truncated (`ftruncate`) to the start offset of that batch.
5. **Space Reclaim:** The filesystem marks those freed sectors as available, allowing subsequent files to write into that reclaimed space.

```
Initial:      [ Entry 1 | Entry 2 | Entry 3 | Entry 4 | Index ]
Phase 1:      [ Entry 1 | Entry 2 | Entry 3 ]  ==> (Entry 4 written & verified)
Phase 2:      [ Entry 1 | Entry 2 ]            ==> (Entry 3 written & verified)
Phase 3:      [ Entry 1 ]                      ==> (Entry 2 written & verified)
Finished:     [ All files extracted! Original archive cleanly consumed. ]
```

---

## 🛡️ Safety Guarantees

Because in-place extraction modifies the source archive, the system adheres to strict data-preservation rules:

- **Pre-Flight Simulation:** Simulates the entire extraction timeline before a single byte is touched. If the peak storage margin ever drops below `--keep-free` (2 GB default), the run is aborted.
- **Write-Flush-Truncate Cycle:** An archive segment is **never** cut until every file it contains is fully written and synced to disk.
- **CRC-32 Checksum Validation:** Every file's CRC-32 is checked as it decodes. If a file is corrupted, extraction halts immediately, keeping the rest of the archive intact.
- **Path Traversal & Security Hardening:** Prevents zip-slip attacks, path escaping (`../`), unauthorized symlink overwrites, and permission escalation.
- **FUSE & Android Storage Resilience:** Specifically engineered with advisory lock and `syncfs` fallbacks for Android FUSE (`/storage/emulated/0`) and external FAT32/exFAT SD cards.

---

## 🛠️ Monorepo Architecture

```
hatchdroid/
├── android-app/            # Modern Jetpack Compose Android application (Fork Addition)
│   ├── app/                # UI, ViewModel, Foreground Service, Storage Telemetry
│   ├── gradle/             # Kotlin Gradle DSL build configuration
│   └── scripts/            # Automated 16 KB alignment & release packaging pipeline
├── crates/
│   ├── android/            # UniFFI bridge exposing Rust core to Kotlin/JNI (Fork Addition)
│   ├── uniffi-bindgen/     # UniFFI code generation toolchain (Fork Addition)
│   ├── core/               # Upstream in-place extraction engine (hatchzip-core)
│   ├── cli/                # Upstream command-line interface
│   ├── gui/                # Upstream desktop user interface
│   └── shell/              # Upstream Windows Explorer context menu extension
├── docs/                   # Deep dives, format notes, and Play Store metadata
├── scripts/                # Automated cross-compilation build scripts
└── assets/                 # Icons and visual media
```

---

## 🏗️ Building from Source

### Prerequisites
- **Rust:** Stable toolchain (`rustup`)
- **Android SDK & NDK (for Android build):** NDK r27+ installed via Android Studio or command-line tools.
- **JDK:** Java 21+

### 1. Build Android Native Libraries & App (HatchDroid)
Run the automated build script to cross-compile the Rust core with 16 KB ELF alignment and compile the APK:

**On Windows (PowerShell):**
```powershell
# Compiles aarch64-linux-android & x86_64-linux-android with 16 KB page size
.\scripts\build-android.ps1 -Release

# Build the Android APK via Gradle
cd android-app
.\gradlew assembleRelease
```

**On Linux / macOS (Bash):**
```bash
./scripts/build-android.sh --release
cd android-app
./gradlew assembleRelease
```

### 2. Build Desktop & CLI Tools (Upstream HatchZip)
```bash
# Build CLI and Desktop GUI
cargo build --release

# Binaries located at:
# target/release/hatchzip      (CLI)
# target/release/hatchzip-gui  (Desktop app)
```

---

## 📄 License & Attribution

This project is open-source under dual licensing:
- **[Apache License, Version 2.0](LICENSE-APACHE)**
- **[MIT License](LICENSE-MIT)**

### Upstream Project Attribution & Acknowledgments
- **Original Engine & Desktop Architecture:** The in-place extraction engine (`hatchzip-core`), algorithm, and desktop applications were created and are actively maintained by **[xevrion](https://github.com/xevrion)** in the **[hatchzip](https://github.com/xevrion/hatchzip)** repository ([hatchzip.dev](https://hatchzip.dev)).
- **HatchDroid (This Fork):** An unofficial community port created by [@rsshir60](https://github.com/rsshir60) to bring `hatchzip-core` to Android.
- **UnRAR:** RAR archive extraction incorporates the UnRAR library under its respective [freeware license](LICENSE-UNRAR).

---

<div align="center">
  <b>⭐ Star this repo if it saved your disk space!</b><br>
  Check out the original upstream project at <a href="https://github.com/xevrion/hatchzip">xevrion/hatchzip</a>.
</div>
