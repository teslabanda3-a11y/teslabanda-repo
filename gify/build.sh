#!/bin/bash

echo "Building the app (64-bit)..."

# Make gradlew executable if it exists
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew assembleDebug
else
    echo "gradlew not found."
    echo "→ Open the project in Android Studio first (it will generate gradlew)."
    echo "→ Or install Gradle and run: gradle assembleDebug"
    exit 1
fi

echo ""
echo "✅ Build finished!"
echo "APK location:"
echo "app/build/outputs/apk/debug/app-debug.apk"
