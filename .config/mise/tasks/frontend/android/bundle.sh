#!/usr/bin/env sh
#MISE description="Bundle Android release AAB (:apps-android:bundleRelease)"
set -eu
./gradlew :frontend:apps-android:bundleRelease
