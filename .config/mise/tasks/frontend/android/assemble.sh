#!/usr/bin/env sh
#MISE description="Assemble Android debug APK (:apps-android:assembleDebug)"
set -eu
./gradlew :frontend:apps-android:assembleDebug
