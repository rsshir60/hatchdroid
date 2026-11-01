<div align="center">

  <img src="assets/icon/hatchdroid_store_icon.svg" width="128" height="128" alt="HatchDroid Logo" />

  # HatchDroid

  **True In-Place, Zero-Overhead Archive Extraction for Android**<br>
  *An unofficial community fork & native Android port powered by the [`hatchzip-core`](https://github.com/xevrion/hatchzip) Rust engine.*

  <p>
    <a href="https://github.com/rsshir60/hatchdroid/actions"><img src="https://img.shields.io/badge/Build-Passing-2ea043?style=flat-square&logo=githubactions&logoColor=white" alt="Build Status" /></a>
    <a href="https://github.com/rsshir60/hatchdroid/releases"><img src="https://img.shields.io/github/v/release/rsshir60/hatchdroid?style=flat-square&logo=android&label=APK%20Release&color=3DDC84" alt="Latest Release" /></a>
    <a href="#android-app-hatchdroid"><img src="https://img.shields.io/badge/Android-API%2033%2B%20(16%20KB%20ELF)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android API 33+" /></a>
    <a href="https://github.com/xevrion/hatchzip"><img src="https://img.shields.io/badge/Upstream-xevrion%2Fhatchzip-f97316?style=flat-square&logo=rust&logoColor=white" alt="Upstream HatchZip" /></a>
    <a href="#safety-guarantees"><img src="https://img.shields.io/badge/Telemetry-100%25%20Offline-10b981?style=flat-square&logo=shield&logoColor=white" alt="Zero Telemetry" /></a>
    <a href="#license--attribution"><img src="https://img.shields.io/badge/License-MIT%20%7C%20Apache--2.0-3b82f6?style=flat-square" alt="License" /></a>
    <a href="https://github.com/rsshir60/hatchdroid/stargazers"><img src="https://img.shields.io/github/stars/rsshir60/hatchdroid?style=flat-square&color=eab308" alt="GitHub Stars" /></a>
  </p>

  <h4>
    <a href="#important-community-fork-notice">Fork Notice</a> •
    <a href="#the-problem--solution">The Breakthrough</a> •
    <a href="#android-app-hatchdroid">Android App</a> •
    <a href="#supported-formats">Formats</a> •
    <a href="#how-in-place-extraction-works">How It Works</a> •
    <a href="#safety-guarantees">Safety</a> •
    <a href="#building-from-source">Build & Verify</a> •
    <a href="#frequently-asked-questions">FAQ</a> •
    <a href="#desktop--cli-upstream-hatchzip">Desktop & CLI</a>
  </h4>

  <br>

  <img src="docs/media/hatchzip.gif" width="780" alt="HatchZip/HatchDroid extracts from the end and truncates the source archive as files hatch" />

</div>

---

<a id="important-community-fork-notice"></a>
## ⚠️ Important Community Fork Notice

> [!IMPORTANT]
> **Unofficial Community Fork & Android Port**
>
> This repository is an **independent community fork** maintained by [@rsshir60](https://github.com/rsshir60). We are **not** the official developers or owners of the HatchZip project.
>
> All credit for the invention of the in-place extraction algorithm, the core Rust engine (`hatchzip-core`), and the original desktop/CLI applications belongs to **[xevrion/hatchzip](https://github.com/xevrion/hatchzip)**. Please visit **[hatchzip.dev](https://hatchzip.dev)** for official desktop builds and upstream releases.
>
> **Scope of This Fork:** Our work focuses exclusively on bringing the `hatchzip-core` ecosystem to Android through **HatchDroid**—introducing a native Jetpack Compose UI, UniFFI/JNI bindings, a dual-progress Android Foreground Service, Android FUSE / Scoped Storage resilience, and 16 KB ELF page alignment for Android 15+.

---

<a id="the-problem--solution"></a>
## 💡 The Problem & Solution

Traditional archive extractors (WinRAR, 7-Zip, ZArchiver, Files by Google) require **2x disk space** during extraction: enough room to hold the compressed archive **and** all uncompressed files simultaneously.

```text
[ Traditional Extraction (Requires 2x Storage): ]
+---------------------------+-----------------------------+
|    30 GB Source Archive   |    32 GB Unpacked Files     |  => 62 GB Peak Required
+---------------------------+-----------------------------+
Result on a phone with 35 GB free: FAILS ("Out of Storage Space")

[ HatchDroid In-Place Extraction (Powered by hatchzip-core): ]
+--------------+             +-----------------------------+
| 15 GB Carved |  --freed--> |    16 GB Hatched Files      |  => ~33 GB Peak Required
+--------------+             +-----------------------------+
Result on a phone with 35 GB free: SUCCEEDS (Source shrinks as files hatch)
```

| Real-World Scenario | Standard Mobile Extractors | 🐣 HatchDroid |
|:---|:---:|:---:|
| **30 GB archive on a phone with 35 GB free** | ❌ Fails (*Out of Space*) | ✅ **Extracts smoothly** |
| **Disk space reclamation** | ⏳ Only after 100% completion | ⚡ **Continuous, real-time tail truncation** |
| **App killed / Battery dies at 80%** | ⚠️ Partial/corrupt files; restart from 0% | 🛡️ **Instant, bit-for-bit crash-safe resume** |
| **Disk space validation** | ❌ Writes blindly until storage hits 0 B | 🔍 **Simulates full extraction timeline before touching disk** |
| **Split multi-volume sets (`.001`, `.part1.rar`)** | ❌ Keeps all parts until the very end | 🗑️ **Purges each volume immediately after extraction** |

---

<a id="android-app-hatchdroid"></a>
## 📱 Android App (HatchDroid)

HatchDroid pairs the memory-safe Rust `hatchzip-core` engine with a native Material 3 Jetpack Compose interface tailored for mobile storage constraints.

<div align="center">
  <img src="assets/icon/hatchdroid_store_icon.svg" width="96" height="96" alt="HatchDroid App Icon" />
</div>

### ✨ Key Android Capabilities

- 🗜️ **True In-Place Storage Reclaimer:** Truncates random-access archives from the tail via POSIX `ftruncate`, and punches sparse filesystem holes in forward-streamed `tar.gz` archives via `fallocate(FALLOC_FL_PUNCH_HOLE)`.
- 📊 **3-Phase Live Storage Telemetry:** Real-time animated canvas tracking the exact lifecycle of storage bytes:
  - 🔻 **Reclaimed:** Archive bytes permanently freed from physical storage via `ftruncate`.
  - ⏳ **Staged for Cut:** Unpacked files decoded in the current batch, awaiting the next atomic `fsync` flush.
  - 📦 **Untouched Archive:** Compressed data remaining to be processed.
- ⚡ **Dynamic Speed & ETA Telemetry:** Measures extraction throughput in MB/s, calculates completion ETA, and displays the exact entry path currently being decoded.
- 🔐 **Encrypted Archive Support with Zeroize RAM Scrubbing:** Supports password-protected ZIP (ZipCrypto, AES-128/256), 7z (AES-256), and RAR5 archives with a Material 3 bottom sheet and shake feedback on invalid entries. Passwords are automatically wiped from device RAM using `zeroize::Zeroize` once the session completes.
- 🔗 **System Content URI & Intent Integration (`ContentUriPathResolver`):** Transparently translates incoming `content://` and `file://` URIs from external file managers, cloud storage pickers, and browser download managers into low-level POSIX filesystem paths across internal storage (`/storage/emulated/0`) and physical SD cards.
- 🛑 **System Safety Buffer Enforcement:** Reserves a protected free-space floor (default 2 GB) so Android never hits zero storage or starves system SQLite databases.
- 🔔 **Foreground Service with Persistent Progress:** Runs resiliently in the background with persistent notification telemetry tracking files hatched and space reclaimed, featuring instant Pause / Resume actions.
- 🔄 **Crash-Proof & Resumable (`.hatchzip` Journal):** If Android force-stops the background task or the battery dies, atomic flush barriers and state checkpoints let you reopen HatchDroid and tap **Resume Safely**.
- 🧩 **Android 15+ Ready (16 KB Page Alignment):** Native Rust shared libraries (`libhatchzip_android.so`) are compiled and verified with 16 KB ELF segment alignment for modern ARM64 hardware.
- 📳 **Haptic Feedback Integration:** Provides subtle haptic confirmations as extraction batches commit and physical sectors are freed.
- 🔒 **100% Offline & Zero Telemetry:** No internet permission, no ads, no analytics SDKs, and zero network dependencies.

> [!NOTE]
> Full Google Play Store metadata, permission rationales, and privacy declarations are documented in [`docs/play_store_listing.md`](docs/play_store_listing.md).

---

<a id="supported-formats"></a>
## 📦 Supported Formats & Reclamation Granularity

| Format | Compression Codecs Supported | Space Reclaimed Every | Mechanism |
|:---|:---|:---:|:---|
| **ZIP / ZIP64** | Stored, Deflate, Deflate64, Bzip2, LZMA, PPMd, XZ, Zstd | **1 File** | Reverse tail `ftruncate` |
| **7-Zip (`.7z`)** | LZMA, LZMA2, BCJ, Bzip2, Deflate, PPMd, Zstd | **1 Solid Block** | Reverse block `ftruncate` |
| **RAR (RAR4 & RAR5)** | Full single-archive & multi-part (`.part1.rar`) | **1 File / Volume** | Tail `ftruncate` / Volume delete |
| **TAR (Uncompressed)** | POSIX ustar, GNU tar, long filenames, symlinks | **1 File** | Reverse tail `ftruncate` |
| **TAR.GZ / `.tgz`** | Gzip-compressed TAR streams | **256 MiB Chunk** | Forward `fallocate` hole-punching |
| **Split Archives** | `.001`, `.002`, `.part1.rar`, `.z01`, `.7z.001` | **1 Volume** | Immediate volume `unlink` |
| **Encrypted Archives** | ZipCrypto, AES-128, AES-256, RAR5 AES, 7z AES | Same as base format | Authenticated decrypt + `zeroize` RAM wipe |

---

<a id="how-in-place-extraction-works"></a>
## ⚙️ How In-Place Extraction Works

Standard extractors stream from byte 0 to EOF, keeping the entire source file locked on disk until the last byte is unpacked. HatchDroid reverses the pipeline:

```mermaid
flowchart LR
    A["Source Archive<br/>(Read Central Index)"] --> B["Init .hatchzip<br/>Recovery Journal"]
    B --> C["Decode Tail Batch<br/>(Last Physical Entries)"]
    C --> D["Write + CRC-32<br/>+ fsync() Barrier"]
    D --> E["ftruncate() Archive<br/>(Reclaim Tail Sectors)"]
    E --> F{"More Entries?"}
    F -- "Yes" --> C
    F -- "No" --> G["Extraction Complete<br/>(Source Cleanly Consumed)"]
```

1. **Reverse Ingestion:** For indexed formats (ZIP, 7z, RAR), the Central Directory lives at the end of the file. The engine parses this index first and writes a compact `.hatchzip` recovery journal to disk.
2. **Tail-to-Head Batching:** Entries located physically closest to the end of the archive are decoded and extracted first.
3. **Atomic Flush Barrier:** Each extracted file is written, checksum-verified (CRC-32), and flushed to physical flash storage via `fsync`.
4. **Tail Truncation:** Only after the hardware flush succeeds does HatchDroid call `ftruncate` to chop the processed bytes off the end of the source archive.
5. **Immediate Sector Reclamation:** The Android filesystem (`ext4` / `f2fs` / `FUSE`) immediately marks those freed blocks as available for the next batch of extracted files.

```text
Initial State:  [ Entry 1 | Entry 2 | Entry 3 | Entry 4 | Central Index ]
Step 1:         [ Entry 1 | Entry 2 | Entry 3 ] --> (Entry 4 written, verified & synced)
Step 2:         [ Entry 1 | Entry 2 ]           --> (Entry 3 written, verified & synced)
Step 3:         [ Entry 1 ]                     --> (Entry 2 written, verified & synced)
Final State:    [ Original archive cleanly consumed; 100% of files hatched on disk ]
```

---

<a id="safety-guarantees"></a>
## 🛡️ Safety & Storage Resilience Guarantees

Because in-place extraction mutates the source archive, `hatchzip-core` enforces strict transactional invariants:

- **Pre-Flight Timeline Simulation:** Computes the exact high-water mark across every single file batch before touching disk. If peak usage ever breaches `--keep-free` (default 2 GB), extraction refuses to start.
- **Strict Write -> Flush -> Truncate Ordering:** An archive byte range is never truncated until every file inside that range is completely written, checksummed, and synced to non-volatile storage.
- **Real-Time CRC-32 Integrity Checks:** Every extracted stream is verified against its header checksum on the fly. Any bit-rot or corruption halts the pipeline immediately before truncation occurs.
- **Path Traversal & Zip-Slip Hardening:** Sanitizes relative paths (`../`), rejects absolute paths, validates symlink targets, and prevents arbitrary permission escalation.
- **Android FUSE & External SD Card Fallbacks:** Engineered with graceful fallbacks for Android's `/storage/emulated/0` FUSE daemon and FAT32/exFAT mounts where POSIX advisory locks (`ENOSYS`), `fchmod` (`EPERM`), or `syncfs` are unsupported by the kernel driver.

---

<a id="monorepo-architecture"></a>
## 🗂️ Monorepo Architecture

```text
hatchdroid/
├── android-app/            # Native Jetpack Compose Android Application [Fork Addition]
│   ├── app/                # Material 3 UI, ViewModels, Foreground Service, Space Telemetry
│   │   ├── ui/components/  # LiveExtractionTelemetryCard, PasswordPromptSheet, DualProgressCard
│   │   ├── ui/extraction/  # LiveProgressState, TelemetryBridge
│   │   ├── ui/screens/     # HomeScreen, ExtractionScreen, SettingsAboutScreen, PermissionScreen
│   │   └── util/           # ContentUriPathResolver, StorageUtil, PermissionUtil
│   ├── gradle/             # Kotlin Gradle DSL & version catalogs
│   └── scripts/            # 16 KB ELF alignment verification & release signing tools
├── crates/
│   ├── android/            # UniFFI + JNI bridge exposing Rust core to Kotlin [Fork Addition]
│   │   ├── cancel.rs       # Thread-safe atomic CancellationToken
│   │   └── lib.rs          # Throttled TelemetryReporter, password check & zeroize wipe
│   ├── uniffi-bindgen/     # UniFFI Kotlin bindings generator [Fork Addition]
│   ├── core/               # Upstream in-place extraction engine (hatchzip-core)
│   ├── cli/                # Upstream command-line interface (hatchzip)
│   ├── gui/                # Upstream desktop GUI application
│   └── shell/              # Upstream Windows Explorer context-menu extension
├── docs/                   # Architecture deep dives, format specs, Play Store metadata
├── scripts/                # Cross-compilation build scripts (PowerShell & Bash)
└── assets/                 # App icons, SVGs, and demo media
```

---

<a id="building-from-source"></a>
## 🏗️ Building from Source

### Prerequisites
- **Rust:** Stable toolchain via [rustup](https://rustup.rs/) (`aarch64-linux-android` and `x86_64-linux-android` targets)
- **Android SDK & NDK:** API 33+ and NDK r27+ (required for 16 KB ELF page alignment)
- **JDK:** Java 21+

### 1. Build HatchDroid (Android Native Libraries + APK)

**Windows (PowerShell):**
```powershell
# Cross-compiles Rust core with 16 KB page alignment & generates UniFFI bindings
.\scripts\build-android.ps1 -Release

# Assemble signed/unsigned release APK & AAB
cd android-app
.\gradlew assembleRelease bundleRelease
```

**Linux / macOS (Bash):**
```bash
# Cross-compiles Rust core with 16 KB page alignment & generates UniFFI bindings
./scripts/build-android.sh --release

# Assemble release APK & AAB
cd android-app
./gradlew assembleRelease bundleRelease
```

### 2. Verify 16 KB Page Alignment & APK Signature

```bash
# 1. Verify ELF LOAD segments are 16 KB (0x4000) aligned
llvm-readelf -l target/aarch64-linux-android/release/libhatchzip_android.so | grep -E "LOAD|Align"

# 2. Verify APK zipalign at 16 KB boundaries
zipalign -c -P 16 -v 4 android-app/app/build/outputs/apk/release/app-release.apk

# 3. Verify APK v2/v3 cryptographic signature
apksigner verify --verbose android-app/app/build/outputs/apk/release/app-release.apk
```

### 3. Build Upstream Desktop & CLI Tools

```bash
cargo build --release --workspace --exclude hatchzip-android

# Compiled binaries:
# target/release/hatchzip      (CLI)
# target/release/hatchzip-gui  (Desktop GUI)
```

---

<a id="desktop--cli-upstream-hatchzip"></a>
## 💻 Desktop & CLI (Upstream HatchZip)

The desktop GUI and command-line interface (`hatchzip`) included in this monorepo are authored and maintained upstream by **[xevrion/hatchzip](https://github.com/xevrion/hatchzip)**.

For official prebuilt desktop installers (`.msi`, `.dmg`, `.deb`, `.rpm`, `.AppImage`) and provenance attestations:
- **Official Website:** [hatchzip.dev](https://hatchzip.dev)
- **Official Desktop Releases:** [github.com/xevrion/hatchzip/releases](https://github.com/xevrion/hatchzip/releases)

```bash
# Preview the exact storage high-water mark without modifying disk
hatchzip check huge_game.zip

# Extract in-place (shrinks source archive as files hatch)
hatchzip extract huge_game.zip

# Extract a multi-volume split archive (any part filename works)
hatchzip x game.7z.001

# Extract traditionally without truncating the original archive
hatchzip extract archive.zip --keep-archive

# Resume an interrupted extraction after power loss or Ctrl+C
hatchzip extract huge_game.zip
```

---

<a id="frequently-asked-questions"></a>
## ❓ Frequently Asked Questions

<details>
<summary><b>1. Why does HatchDroid require "All Files Access" (<code>MANAGE_EXTERNAL_STORAGE</code>)?</b></summary>
<br>

HatchDroid is a low-level archive management tool that operates directly on multi-gigabyte archives across user directories (e.g., `/storage/emulated/0/Download`). Android's standard Storage Access Framework (`content://` URIs) cannot perform POSIX random-access tail truncation (`ftruncate`) or hole-punching (`fallocate`). Direct file path access is strictly required to progressively shrink files on disk and manage crash-recovery companion journals (`.hatchzip`). HatchDroid operates 100% offline and requests zero internet permissions.
</details>

<details>
<summary><b>2. How does HatchDroid handle password-protected archives securely?</b></summary>
<br>

HatchDroid supports encrypted ZIP (ZipCrypto, AES-128, AES-256), 7-Zip (AES-256), and RAR5 archives. When an encrypted archive is encountered, a Material 3 Bottom Sheet prompts for the passphrase with instant feedback. In the native layer, HatchDroid uses Rust's `zeroize::Zeroize` crate to cryptographically wipe the passphrase bytes from RAM immediately after session initialization, ensuring sensitive credentials cannot linger in memory.
</details>

<details>
<summary><b>3. Can I open archives directly from other apps or the system download manager?</b></summary>
<br>

**Yes.** HatchDroid includes a built-in `ContentUriPathResolver` that intercepts system `content://` and `file://` intents. When you tap an archive in Chrome, Firefox, or a third-party file manager, HatchDroid resolves the URI directly to its underlying physical POSIX path on internal storage or physical SD cards and launches the pre-flight planning sheet.
</details>

<details>
<summary><b>4. Does in-place extraction cause excessive flash memory wear (UFS / eMMC)?</b></summary>
<br>

**No.** In fact, in-place extraction causes **equal or fewer total flash write cycles** than traditional extraction:
- Extracted files are written sequentially to disk exactly once in both methods.
- Truncating the archive tail via `ftruncate` only updates the file's metadata index in the filesystem inode / block allocation table—it does not overwrite the data sectors.
- In-place extraction prevents Android's memory manager from thrashing low-storage swap space during tight storage conditions.
</details>

<details>
<summary><b>5. How does tail truncation interact with Android's FUSE filesystem?</b></summary>
<br>

Android runs a userspace FUSE wrapper over `/storage/emulated/0` to manage Scoped Storage permissions. Some legacy extractors fail because Android's FUSE daemon returns `ENOSYS` or `EOPNOTSUPP` on POSIX advisory file locks (`flock`) or directory sync operations (`syncfs`). HatchDroid's modified platform driver handles these specific FUSE quirks by falling back gracefully, ensuring reliable tail truncation across internal storage and FAT32/exFAT SD cards.
</details>

<details>
<summary><b>6. What happens if the app crashes, battery dies, or Android kills the process?</b></summary>
<br>

**Your files remain completely safe.** HatchDroid maintains a two-file recovery journal (`archive.zip.hatchzip` and `archive.zip.hatchzip-tail`) holding the archive's central directory. Every file is completely extracted, verified against its CRC-32 checksum, and synced via `fsync` before the source archive is truncated. If extraction is interrupted at any moment, reopen HatchDroid and tap **Resume**—it will read the journal and pick up seamlessly from the exact byte where it left off.
</details>

<details>
<summary><b>7. Can I use HatchDroid as a normal extractor without altering the source archive?</b></summary>
<br>

**Yes.** If your device has sufficient storage, toggle **"Keep Archive"** in HatchDroid's settings. In this mode, HatchDroid operates as a traditional, non-destructive extractor: the source archive is untouched, and full extraction space is allocated as normal.
</details>

---

<a id="license--attribution"></a>
## 📄 License & Attribution

This project is dual-licensed under your choice of:
- **[Apache License, Version 2.0](LICENSE-APACHE)**
- **[MIT License](LICENSE-MIT)**

### Upstream Attribution & Acknowledgments
- **Original Engine & Algorithm (`hatchzip-core`):** Created and maintained by **[xevrion](https://github.com/xevrion)** at **[xevrion/hatchzip](https://github.com/xevrion/hatchzip)** ([hatchzip.dev](https://hatchzip.dev)).
- **HatchDroid (Android Port):** Created and maintained by **[@rsshir60](https://github.com/rsshir60)** as a community fork bringing `hatchzip-core` to Android.
- **UnRAR:** RAR format extraction uses the UnRAR library under its respective [freeware license](LICENSE-UNRAR).

---

<div align="center">
  <b>⭐ Star this repo if it saved your phone storage!</b><br>
  Check out the original upstream project at <a href="https://github.com/xevrion/hatchzip">xevrion/hatchzip</a>.
</div>
