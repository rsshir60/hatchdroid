<#
.SYNOPSIS
    Automated, fail-fast release compilation, packaging, and 16 KB alignment verification pipeline for HatchZip Android.
#>

[CmdletBinding()]
param(
    [string]$SdkRoot = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$NdkVersion = "27.2.12479018",
    [string]$BuildToolsVersion = "36.0.0"
)

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$NdkPath = Join-Path $SdkRoot "ndk\$NdkVersion"
$BuildToolsPath = Join-Path $SdkRoot "build-tools\$BuildToolsVersion"
$ClangBin = Join-Path $NdkPath "toolchains\llvm\prebuilt\windows-x86_64\bin"

Write-Host "==> [1/5] Checking Toolchain Prerequisites..." -ForegroundColor Cyan
if (-not (Test-Path $NdkPath)) { throw "NDK $NdkVersion not found at $NdkPath" }
if (-not (Test-Path $BuildToolsPath)) { throw "Build-tools $BuildToolsVersion not found at $BuildToolsPath" }

$env:PATH = "$env:USERPROFILE\.cargo\bin;$env:PATH"
$env:CC_aarch64_linux_android = Join-Path $ClangBin "aarch64-linux-android35-clang.cmd"
$env:CXX_aarch64_linux_android = Join-Path $ClangBin "aarch64-linux-android35-clang++.cmd"
$env:AR_aarch64_linux_android = Join-Path $ClangBin "llvm-ar.exe"
$env:CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER = Join-Path $ClangBin "aarch64-linux-android35-clang.cmd"

Write-Host "==> [2/5] Building Stripped Rust Native Library (aarch64-linux-android --release)..." -ForegroundColor Cyan
Push-Location $RepoRoot
try {
    cargo build -p hatchzip-android --target aarch64-linux-android --release
    if ($LASTEXITCODE -ne 0) { throw "Cargo build failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

$ReleaseSo = Join-Path $RepoRoot "target\aarch64-linux-android\release\libhatchzip_android.so"
$JniDest = Join-Path $RepoRoot "android-app\app\src\main\jniLibs\arm64-v8a\libhatchzip_android.so"

Write-Host "==> [3/5] Verifying 16 KB ELF Alignment..." -ForegroundColor Cyan
$ReadElf = Join-Path $ClangBin "llvm-readelf.exe"
$AlignmentOutput = & $ReadElf -l $ReleaseSo | Select-String "LOAD|Align"
$AlignmentOutput | ForEach-Object { Write-Host "    $_" }

Copy-Item $ReleaseSo $JniDest -Force
Write-Host "    Copied $([math]::Round((Get-Item $ReleaseSo).Length / 1MB, 2)) MB binary to $JniDest" -ForegroundColor Green

Write-Host "==> [4/5] Building Signed Release APK & AAB..." -ForegroundColor Cyan
Push-Location (Join-Path $RepoRoot "android-app")
try {
    .\gradlew.bat assembleRelease bundleRelease
    if ($LASTEXITCODE -ne 0) { throw "Gradle release build failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

Write-Host "==> [5/5] Verifying 16 KB Zip-Alignment & Signature..." -ForegroundColor Cyan
$ReleaseApk = Join-Path $RepoRoot "android-app\app\build\outputs\apk\release\app-release.apk"
$ZipAlign = Join-Path $BuildToolsPath "zipalign.exe"
$ApkSigner = Join-Path $BuildToolsPath "apksigner.bat"

& $ZipAlign -c -P 16 -v 4 $ReleaseApk | Select-String "lib/.*\.so|Verification successful"
if ($LASTEXITCODE -ne 0) { throw "16 KB zipalign verification failed" }

& $ApkSigner verify --verbose $ReleaseApk
if ($LASTEXITCODE -ne 0) { throw "apksigner verification failed" }

Write-Host "`nSUCCESS: Production release artifacts built and verified at:" -ForegroundColor Green
Write-Host "APK: $ReleaseApk ($([math]::Round((Get-Item $ReleaseApk).Length / 1MB, 2)) MB)"
Write-Host "AAB: $(Join-Path $RepoRoot "android-app\app\build\outputs\bundle\release\app-release.aab")"
