#!/usr/bin/env sh
#MISE description="Install Android debug APK on connected device (:apps-android:installDebug)"
set -eu
./gradlew :frontend:apps-android:installDebug
