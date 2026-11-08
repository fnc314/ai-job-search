#!/usr/bin/env sh
#MISE description="Install Android debug APK on connected device (:apps-android:installDebug)"
set -eu
cd ui/kmp && ./gradlew :apps-android:installDebug
