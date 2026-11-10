# Implementation Plan: Seamless KMP Inspector Integration (Debug vs Release)

**Status:** Completed  
**Date:** 2026-11-06  
**Target:** `ui/kmp`  

---

## 1. Executive Summary & Goals

Integrate `kmp-inspector` across Ktor network clients (`core:network`), Room databases (`core:data`), and Android application entry points (`apps/android`) with zero production overhead (using `kmp-inspector-noop` in release builds and full inspection tools in debug builds).

---

## 2. Architecture & Design Principles

- **Zero Release Overhead**: Release builds compile against `kmp-inspector-noop` and strip out inspector UI/interceptors.
- **Ktor & Room Integration**: Attach `kmp-inspector-ktor` interceptors to Ktor clients and `kmp-inspector-room` to Room database builders when building in debug mode.
- **Convention Plugin Automation**: Convention plugins (`AndroidApplicationConventionPlugin`) handle artifact substitution between debug and release variants.

---

## 3. Proposed Changes & Implementation Steps

### [MODIFY] [NetworkClient.kt](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/core/network/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/network/NetworkClient.kt)
- Conditionally install `KmpInspector` plugin into Ktor client.

### [MODIFY] [DatabaseBuilder.kt](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/DatabaseBuilder.kt)
- Attach `kmp-inspector-room` query inspector for database diagnostics.

### [MODIFY] [JobTrackerApplication.kt](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/apps/android/src/main/kotlin/dev/fnc314/tools/jobtracker/JobTrackerApplication.kt)
- Verify `KmpInspector.install(this)` invocation.

---

## 4. Verification Plan & Testing Checklist

- [ ] Assemble debug APK: `mise run ui:kmp:android:assemble`
- [ ] Run KMP metadata compile: `./gradlew compileCommonMainKotlinMetadata`
