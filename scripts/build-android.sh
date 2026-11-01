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
