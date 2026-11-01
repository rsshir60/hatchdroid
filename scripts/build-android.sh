#!/usr/bin/env bash
set -euo pipefail

MIN_API=35
RELEASE_FLAG=""
SUBDIR="debug"

if [[ "${1:-}" == "--release" ]]; then
  RELEASE_FLAG="--release"
  SUBDIR="release"
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# Resolve Android NDK toolchain if available
NDK_ROOT="${ANDROID_NDK_ROOT:-}"
if [[ -z "${NDK_ROOT}" ]] && [[ -n "${ANDROID_HOME:-}" ]] && [[ -d "${ANDROID_HOME}/ndk" ]]; then
  NDK_ROOT="$(find "${ANDROID_HOME}/ndk" -maxdepth 1 -mindepth 1 -type d | sort -V | tail -n 1)"
fi

if [[ -n "${NDK_ROOT}" ]]; then
  # Determine prebuilt OS directory
  HOST_OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
  TOOLCHAIN="${NDK_ROOT}/toolchains/llvm/prebuilt/${HOST_OS}-x86_64/bin"
  if [[ -d "${TOOLCHAIN}" ]]; then
    export PATH="${TOOLCHAIN}:${PATH}"
    export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="${TOOLCHAIN}/aarch64-linux-android${MIN_API}-clang"
    export CARGO_TARGET_AARCH64_LINUX_ANDROID_AR="${TOOLCHAIN}/llvm-ar"
    export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="${TOOLCHAIN}/x86_64-linux-android${MIN_API}-clang"
    export CARGO_TARGET_X86_64_LINUX_ANDROID_AR="${TOOLCHAIN}/llvm-ar"
    echo "Configured Android NDK toolchain from: ${TOOLCHAIN}"
  fi
fi

# Ensure Android targets are added
rustup target add aarch64-linux-android x86_64-linux-android

TARGETS=("aarch64-linux-android:arm64-v8a" "x86_64-linux-android:x86_64")

for entry in "${TARGETS[@]}"; do
  RUST_TARGET="${entry%%:*}"
  ABI="${entry##*:}"
  
  echo "Building libhatchzip_android for ${RUST_TARGET} (16 KB page alignment)..."
  cargo build -p hatchzip-android --target "${RUST_TARGET}" ${RELEASE_FLAG}

  OUT_DIR="${REPO_ROOT}/android-app/app/src/main/jniLibs/${ABI}"
  mkdir -p "${OUT_DIR}"
  cp "${REPO_ROOT}/target/${RUST_TARGET}/${SUBDIR}/libhatchzip_android.so" "${OUT_DIR}/"
done

echo "Generating UniFFI Kotlin bindings..."
cargo run -p hatchzip-android --bin uniffi-bindgen -- generate \
  --library "${REPO_ROOT}/target/aarch64-linux-android/${SUBDIR}/libhatchzip_android.so" \
  --language kotlin \
  --out-dir "${REPO_ROOT}/android-app/app/src/main/java"

echo "Android native build completed successfully."
