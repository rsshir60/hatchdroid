# HatchDroid: Google Play Store Listing & Console Declarations

Production-ready metadata and compliance documentation for **HatchDroid**, formatted to meet all Google Play Store character limits and developer policy requirements.

---

## 1. App Title (29 / 30 characters)

```text
HatchDroid: In-Place Unzipper
```

*Character count check:* 29 characters (Cap: 30). Follows the `Brand: Core Keyword` formula.

---

## 2. Short Description (78 / 80 characters)

```text
Unpack huge ZIP, 7z, RAR & TAR files in place without needing double disk space
```

*Character count check:* 78 characters (Cap: 80). Highlights the core technical value proposition and addresses search intent directly.

---

## 3. Full Description (~2,450 / 4,000 characters)

```text
Running out of phone storage when trying to unpack a massive archive? Traditional archive extractors require double the free space—enough for both the compressed archive and all the extracted files at the same time.

HatchDroid solves the low-storage extraction problem on Android. Powered by the open-source Rust hatchzip-core engine, HatchDroid extracts archives IN PLACE by progressively shrinking the source archive from the end as your files hatch out.

If you have a 30 GB archive on a phone with only 33 GB of free space, standard unzippers fail with a "Not enough storage" error. HatchDroid calculates the exact peak storage margin beforehand and safely unpacks your files while reclaiming the archive space on the fly.

KEY FEATURES

• True In-Place Extraction: Shrinks single-file ZIP, 7z, and TAR archives from the tail (via native POSIX ftruncate) or punches holes in forward-streamed TAR.GZ archives (via fallocate) as files are written to disk.
• Smart Pre-Flight Space Planner: Inspects every archive before modifying a single byte. View a clear Before vs. After storage bar and verify your extraction will fit while keeping a protected free-space safety buffer (2 GB default) for Android system stability.
• Crash-Safe & Resumable: Battery died or switched apps mid-extraction? HatchDroid flushes each file to disk and records a recovery state index before truncating archive bytes. Simply reopen the app and tap Resume to pick up right where you left off.
• Multi-Volume & Split Archive Support: Unpacks split archives (.001, .part1.rar, .z01) and deletes each completed volume automatically as soon as its contents are extracted.
• Optional Pre-Verification Mode: Enable "Verify Checksums First" to stream and check every CRC-32 checksum or companion .sfv file before altering the source archive.
• Keep-Archive Mode: Have plenty of free storage? Toggle "Keep Archive" anytime to use HatchDroid as a fast, traditional non-destructive unarchiver.
• Full Local File & Archive Manager: Browse internal storage, filter for oversized archives, inspect compressed contents, manage file collisions (Skip, Replace, or Keep Both), and clean up leftover downloads.

SUPPORTED FORMATS
• ZIP & Split ZIP (.zip, .zip.001)
• 7-Zip (.7z, multi-block & solid archives)
• RAR (.rar, RAR4/RAR5, multi-part volumes)
• TAR & Compressed TAR (.tar, .tar.gz, .tgz)

BUILT FOR MODERN ANDROID (ANDROID 15+ READY)
• Native Rust core compiled with 16 KB memory page alignment for flagship Android 15+ devices.
• Runs long extractions inside a Foreground Service with live dual-bar progress notifications (watch your archive shrink while your output folder grows).
• Zero ads, zero telemetry, 100% offline operation.

OPEN SOURCE ATTRIBUTION
HatchDroid is built on the open-source hatchzip-core library (Apache-2.0 License).
```

---

## 4. Google Play Console `MANAGE_EXTERNAL_STORAGE` Declaration Form

When uploading your App Bundle (`.aab`) targeting Android 11+ (API 30–35+), Google Play prompts for the **All Files Access Declaration Form**.

### Selected Core Purpose
* Select: **File management / Archive management**

### Review Justification Text (Copy & Paste)

```text
HatchDroid is a full-featured local File and Archive Manager specializing in in-place archive extraction for low-storage devices. Its core native Rust engine (hatchzip-core) requires direct POSIX filesystem access (ftruncate and fallocate system calls) and adjacent recovery state files (.hatchzip / .hatchzip-tail) to progressively truncate multi-gigabyte .zip, .7z, .rar, and .tar archives in place across shared storage directories (/storage/emulated/0/...). Android's Storage Access Framework (content:// URIs) does not support in-place multi-volume truncation or crash-safe atomic file replacement next to the source archive.
```

---

## 5. Visual Asset Guidelines & Checklist

* **Master Store Icon:** `assets/icon/hatchdroid_store_icon.svg`
  * Size: `512 × 512 px`
  * Format: 32-bit sRGB PNG (or SVG exported without rounded corners or outer drop shadows; Google Play automatically masks to a 30% corner radius and applies the drop shadow).
* **Adaptive Android 15+ Launcher Icon:**
  * Background: `res/drawable/ic_launcher_background.xml` (`#0F172A` Obsidian Slate + `#1E293B` storage grid).
  * Foreground: `res/drawable/ic_launcher_foreground.xml` (Emerald `#10B981` top shell & extraction arrow, Light Slate zipper teeth, Rust `#F97316` bottom shell).
  * Safe Zone: Central `66 × 66 dp` (coordinates `21` to `87`).
  * Themed Icon: `res/drawable/ic_launcher_monochrome.xml` (Material You dynamic tinting on Android 13–15+).
