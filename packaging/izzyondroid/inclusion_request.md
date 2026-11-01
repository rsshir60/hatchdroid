# IzzyOnDroid App Inclusion Request: HatchDroid

Submit this request at: **https://codeberg.org/IzzyOnDroid/repodata/issues**
*(Click **New Issue** -> select **App Inclusion Request**)*

---

### App Name
HatchDroid

### Package Name
`dev.hatchzip.android`

### Repository URL
https://github.com/rsshir60/hatchdroid

### Releases URL
https://github.com/rsshir60/hatchdroid/releases

### Binary Type
Signed APK attached to tagged GitHub Releases:
`HatchDroid-v*-arm64-v8a.apk`

### License
MIT OR Apache-2.0 (Dual licensed)

### Short Description
Unpack huge ZIP, 7z, RAR & TAR files in place without needing double disk space.

### Full Description / Fastlane Location
Fastlane structure included in repository:
`android-app/fastlane/metadata/android/en-US/`

### Category
System / Utilities / Archive

### Anti-Features Declaration
- **Ads:** None
- **Tracking / Analytics:** None (100% offline, zero network requests, no telemetry)
- **Non-Free Network Services:** None
- **Non-Free Dependencies:** None (All dependencies are Apache 2.0 / MIT / standard AndroidX)

### Why HatchDroid?
Traditional unzippers fail with "Out of space" errors when extracting archives on storage-constrained Android devices because they require 2x disk space (archive + uncompressed contents simultaneously). Powered by the native `hatchzip-core` Rust engine, HatchDroid progressively shrinks the source archive in place as files are extracted to disk, eliminating the 2x storage overhead.

### Technical Highlights
- 16 KB ELF memory page alignment for Android 15+.
- Foreground Service with live dual-progress notifications and crash-safe resumption.
- Memory-safe Rust JNI core with Zeroize password scrubbing.
