param(
    [string]$TargetAbi = "all",
    [switch]$Release
)

$ErrorActionPreference = "Stop"

$SdkDir = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$NdkDir = Get-ChildItem -Path "$SdkDir\ndk" -Directory | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
if (-not $NdkDir) {
    Write-Error "Android NDK not found in $SdkDir\ndk. Please install NDK r27+."
    exit 1
}

$NdkBin = "$NdkDir\toolchains\llvm\prebuilt\windows-x86_64\bin"
$env:PATH = "$env:USERPROFILE\.cargo\bin;$NdkBin;$env:PATH"
if (-not $env:JAVA_HOME -and (Test-Path "C:\Users\SUPRIYA\AppData\Local\Programs\jdk-21.0.6+7")) {
    $env:JAVA_HOME = "C:\Users\SUPRIYA\AppData\Local\Programs\jdk-21.0.6+7"
}

# Dynamic toolchain configuration for Cargo
$env:CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER = "$NdkBin\aarch64-linux-android35-clang.cmd"
$env:CARGO_TARGET_AARCH64_LINUX_ANDROID_AR = "$NdkBin\llvm-ar.exe"
$env:CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER = "$NdkBin\x86_64-linux-android35-clang.cmd"
$env:CARGO_TARGET_X86_64_LINUX_ANDROID_AR = "$NdkBin\llvm-ar.exe"

Write-Host "Using Android NDK at: $NdkDir"

$RepoRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $RepoRoot

$BuildMode = if ($Release) { "--release" } else { "" }
$SubDir = if ($Release) { "release" } else { "debug" }

$Targets = @()
if ($TargetAbi -eq "all" -or $TargetAbi -eq "arm64-v8a") {
    $Targets += @{ RustTarget = "aarch64-linux-android"; Abi = "arm64-v8a" }
}
if ($TargetAbi -eq "all" -or $TargetAbi -eq "x86_64") {
    $Targets += @{ RustTarget = "x86_64-linux-android"; Abi = "x86_64" }
}

foreach ($t in $Targets) {
    Write-Host "Building libhatchzip_android for $($t.RustTarget) with 16 KB ELF alignment..."
    cargo build -p hatchzip-android --target $($t.RustTarget) $BuildMode

    $OutDir = "$RepoRoot\android-app\app\src\main\jniLibs\$($t.Abi)"
    if (-not (Test-Path $OutDir)) {
        New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
    }

    $BuiltSo = "$RepoRoot\target\$($t.RustTarget)\$SubDir\libhatchzip_android.so"
    if (Test-Path $BuiltSo) {
        Copy-Item -Path $BuiltSo -Destination "$OutDir\libhatchzip_android.so" -Force
        Write-Host "Copied libhatchzip_android.so to $OutDir"
    } else {
        Write-Warning "Output library not found at $BuiltSo"
    }
}

Write-Host "Generating UniFFI Kotlin bindings..."
cargo run -p uniffi-bindgen-runner --bin uniffi-bindgen -- generate `
    --library "$RepoRoot\target\aarch64-linux-android\$SubDir\libhatchzip_android.so" `
    --language kotlin `
    --out-dir "$RepoRoot\android-app\app\src\main\java"

Write-Host "Android native build completed successfully."
