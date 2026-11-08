#!/usr/bin/env sh
#MISE description="Assemble Android debug APK (:apps-android:assembleDebug)"
set -eu
cd ui/kmp && ./gradlew :apps-android:assembleDebug
