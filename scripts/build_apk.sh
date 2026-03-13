#!/usr/bin/env bash
set -euo pipefail

echo "Building debug APK (requires Android SDK + Gradle configured locally)..."
gradle assembleDebug

echo
echo "APK generated at: app/build/outputs/apk/debug/app-debug.apk"
