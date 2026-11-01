# HatchDroid: Open-Source Release & Distribution Guide

Comprehensive operational manual for packaging, signing, and releasing HatchDroid across open-source Android platforms and repositories.

---

## 1. Release Architecture & Distribution Channels

```text
                                 [ git tag vX.Y.Z ]
                                         │
                        ┌────────────────┴────────────────┐
                        ▼                                 ▼
             [ GitHub Actions CI ]              [ F-Droid Build Server ]
            (release-android.yml)                   (from source)
                        │                                 │
           ┌────────────┼────────────┐                    │
           ▼            ▼            ▼                    ▼
     [ GitHub ]   [ Obtainium ] [ IzzyOnDroid ]    [ Official F-Droid ]
     (Releases)   (Direct Pull)  (Auto-sync)        (Main Repository)
           │                         │                    │
           └─────────────────────────┼────────────────────┘
                                     ▼
                        [ User Android Devices ]
```

| Channel | Model | Update Speed | Build Origin | Requirements |
|---|---|---|---|---|
| **GitHub Releases** | Direct APK / AAB | Immediate (< 5 min) | GitHub Actions CI | Signed release APK |
| **Obtainium** | Direct Auto-updater | Immediate (< 1 hr) | GitHub Releases | GitHub Repo URL |
| **IzzyOnDroid** | F-Droid Repo | Fast (1–2 days) | GitHub Releases | Tagged APKs + GitLab issue |
| **Official F-Droid** | Libre Repository | Standard (1–2 weeks) | Headless F-Droid build | Pure FLOSS from source |
| **Accrescent** | Modern FLOSS Store | Fast (1–3 days) | Developer Signed AAB/APK | Accrescent Console |
| **Google Play** | Mainstream Store | 2–5 days review | Developer Signed AAB | Closed test & All Files form |

---

## 2. Production Keystore & Secret Management

Release builds require a cryptographic Android Signing Key (`.jks`).

> [!CAUTION]
> Never commit `release-keystore.jks` or private passwords to git. It is automatically ignored by `.gitignore`. Keep an offline backup of your `.jks` in a secure password manager or encrypted physical drive. If lost, existing users cannot receive app updates.

### Step 2.1: Generate Production Keystore
Run the following command using `keytool` (included with Android Studio / JDK):

```bash
keytool -genkeypair -v \
  -keystore hatchdroid-release.jks \
  -alias hatchdroid \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=HatchDroid, OU=Mobile, O=HatchZip, L=OpenSource, C=US"
```

### Step 2.2: Export Keystore to Base64
For GitHub Actions CI, convert the `.jks` file into a Base64 string:

**On Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("hatchdroid-release.jks")) | Set-Clipboard
```

**On Linux / macOS:**
```bash
base64 -w 0 hatchdroid-release.jks | pbcopy # or xclip -selection clipboard
```

### Step 2.3: Configure GitHub Repository Secrets
Navigate to your repository:
`Settings` ➔ `Secrets and variables` ➔ `Actions` ➔ `New repository secret`

Add the following four secrets:
- `KEYSTORE_BASE64`: The base64-encoded string from Step 2.2.
- `KEYSTORE_PASSWORD`: Keystore password chosen during keytool generation.
- `KEY_ALIAS`: `hatchdroid` (or your chosen alias).
- `KEY_PASSWORD`: Key password chosen during keytool generation.

---

## 3. Local Release Build & 16 KB Alignment Verification

Before pushing a public release, you can run the automated build and verification pipeline locally:

**On Windows:**
```powershell
.\android-app\scripts\build-release.ps1
```

**On Linux / macOS:**
```bash
# 1. Compile Rust engine with 16 KB ELF alignment
./scripts/build-android.sh --release

# 2. Build signed release APK and AAB
cd android-app
./gradlew assembleRelease bundleRelease
```

### Verify 16 KB Page Alignment & Signatures
```bash
# Verify ELF max-page-size 16384 (0x4000)
llvm-readelf -l android-app/app/src/main/jniLibs/arm64-v8a/libhatchzip_android.so | grep -E "LOAD|Align"

# Verify APK zip alignment (16 KB)
zipalign -c -P 16 -v 4 android-app/app/build/outputs/apk/release/app-release.apk

# Verify cryptographic APK signature (v2/v3)
apksigner verify --verbose android-app/app/build/outputs/apk/release/app-release.apk
```

---

## 4. Triggering an Automated GitHub Release

When you are ready to publish a new version:

1. Update `versionCode` and `versionName` in `android-app/app/build.gradle.kts`:
   ```kotlin
   versionCode = 1
   versionName = "0.1.0"
   ```
2. Add the corresponding changelog file at:
   `android-app/fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
3. Commit and tag the release:
   ```bash
   git add android-app/app/build.gradle.kts android-app/fastlane/
   git commit -m "chore(release): prepare v0.1.0"
   git tag -a v0.1.0 -m "HatchDroid v0.1.0"
   git push origin main
   git push origin v0.1.0
   ```
4. GitHub Actions workflow `.github/workflows/android-release.yml` will automatically:
   - Build the 16 KB aligned native library.
   - Assemble and sign the release APK and AAB.
   - Run verification checks.
   - Generate `SHA256SUMS.txt`.
   - Publish the assets under GitHub Release `v0.1.0`.

---

## 5. Publishing to IzzyOnDroid (F-Droid Staging)

IzzyOnDroid is the most popular third-party F-Droid repository, acting as the primary launchpad for open-source Android apps.

1. Once your first GitHub Release (e.g. `v0.1.0`) is published with the APK asset attached, open:
   **https://gitlab.com/IzzyOnDroid/repo/-/issues/new?issuable_template=App%20Inclusion**
2. Copy and paste the pre-filled template from:
   `packaging/izzyondroid/inclusion_request.md`
3. Submit the issue.
4. IzzySoft's automation checks the release, verifies the metadata from `fastlane/metadata/android/en-US/`, and adds HatchDroid to the repo. All future tagged releases will be automatically detected and distributed without filing new requests!

---

## 6. Publishing to Official F-Droid Main Repository

F-Droid builds all applications from source code on their own headless Debian infrastructure.

1. Fork the official metadata repository:
   **https://gitlab.com/fdroid/fdroiddata**
2. Copy the recipe from:
   `packaging/fdroid/dev.hatchzip.android.yml`
   Into:
   `metadata/dev.hatchzip.android.yml` in your forked `fdroiddata` repo.
3. Test locally using the F-Droid build server tool (optional):
   ```bash
   fdroid checkupdates dev.hatchzip.android
   fdroid lint dev.hatchzip.android
   ```
4. Open a Merge Request against `fdroid/fdroiddata:master`.
5. The F-Droid automated test bot will trigger a pipeline build. Once approved by F-Droid maintainers, HatchDroid is indexed in the global F-Droid directory.

---

## 7. Configuring Obtainium for End Users

Obtainium allows users to get immediate, automated updates directly from your GitHub Releases:

1. Install **Obtainium** from [GitHub](https://github.com/ImranR98/Obtainium) or F-Droid.
2. Tap **Add App** (+).
3. Paste: `https://github.com/rsshir60/hatchdroid`
4. Tap **Add**. Obtainium will automatically parse the release tags, detect `HatchDroid-v*-arm64-v8a.apk`, and handle installation and updates.
5. Alternatively, distribute `packaging/obtainium/app_config.json` for one-tap import.

---

## 8. Checklist Before Each Release

- [ ] Version bumped in `android-app/app/build.gradle.kts`.
- [ ] Release notes added to `android-app/fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
- [ ] 16 KB ELF alignment verified via `readelf -l`.
- [ ] Local build passes: `assembleRelease bundleRelease`.
- [ ] Git tag pushed: `git push origin vX.Y.Z`.
- [ ] GitHub Actions release workflow finishes green.
- [ ] Release assets verified on GitHub (`.apk`, `.aab`, `SHA256SUMS.txt`).
- [ ] IzzyOnDroid sync verified (auto-updates within 24h).
