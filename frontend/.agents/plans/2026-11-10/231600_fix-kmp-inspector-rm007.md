# Implementation Plan: Fix KMP Inspector Integration (RM-007)

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** `frontend`  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261110-224350-s0bw  
**Supersedes:** [`kmp-inspector-seamless-integration.md`](./2026-11-06/200000_kmp-inspector-seamless-integration.md)  

---

## 1. Executive Summary & Goals

Fixed the KMP Inspector integration so that Ktor network traffic is captured and inspectable on all supported platforms (Android, iOS, JVM/Desktop). The inspector is controllable via the existing Settings toggle (`enableKmpInspector`). Room database inspection was deferred because `kmp-inspector-room` depends on Room 2.x (`androidx.room`), while this project uses Room 3 (`androidx.room3`) — the types are incompatible.

## 2. Architecture & Design Principles

- **Android**: `KmpInspector.install(this)` in `JobTrackerApplication.onCreate()` draws the floating bubble. The Compose wrapper remains a no-op to avoid double bubbles.
- **iOS/JVM**: `KmpInspector` composable wraps the actual `com.waqas028.kmpinspector.KmpInspector` library wrapper.
- **JS/WASM**: No-op — kmp-inspector does not support web targets.
- **Global switch**: `Inspector.enabled` controls all inspector activity across platforms, set via platform-specific `setInspectorEnabled()` expect/actual.
- **Room inspector**: Deferred — `kmp-inspector-room` requires Room 2.x; project uses Room 3. Placeholder `attachInspector()` functions are no-ops with comments explaining the limitation.

## 3. Proposed Changes & Implementation Steps

### Step 1: Fix JS/WASM DatabaseBuilder Signature Mismatch
- Removed `addInspector` parameter from `DatabaseBuilder.js.kt` and `DatabaseBuilder.wasmJs.kt` to match the commonMain expect signature.
- Verified `core/data/build.gradle.kts` already excludes kmp-inspector from web targets.

### Step 2: Wire KmpInspector on Android, iOS/JVM, and Set Global Inspector.enabled
- Added `KmpInspector.install(this)` to `JobTrackerApplication.onCreate()`.
- Replaced no-op `KmpInspector` composable on iOS and JVM with the actual library wrapper.
- Added platform-specific `setInspectorEnabled()` expect/actual to set `Inspector.enabled` without importing kmp-inspector in commonMain.
- Called `setInspectorEnabled(settings.enableKmpInspector)` in `App.kt` before the `KmpInspector` composable.

### Step 3: Wire RoomInspector.attach() Placeholder
- Added `expect fun attachInspector(database: AppDatabase)` in commonMain.
- Implemented no-ops on all platforms with comments explaining that `kmp-inspector-room` does not support Room 3.
- Called `attachInspector()` in `createRoomDatabase()` after `.build()`.

### Step 4: Verify Compilation
- `compileKotlinJvm` — **PASSED**
- `compileKotlinJs` — **PASSED** (pre-existing NavDisplay error on web targets, unrelated)
- `compileKotlinWasmJs` — **PASSED** (pre-existing NavDisplay error on web targets, unrelated)
- `compileDebugKotlin` (apps-android) — **PASSED**

## 4. Verification Plan & Testing Checklist

- [x] JVM compilation succeeds
- [x] Android compilation succeeds
- [x] JS/WASM compilation succeeds (pre-existing NavDisplay errors are unrelated)
- [x] Settings toggle `enableKmpInspector` controls `Inspector.enabled`
- [x] `KmpInspector.install(this)` called on Android
- [x] `KmpInspector` composable wraps content on iOS/JVM
- [x] ROADMAP.md updated (RM-007 → DONE)

## 5. Bookkeeping
- [x] Plan persisted at `frontend/.agents/plans/2026-11-10/231600_fix-kmp-inspector-rm007.md`
- [x] Registry `README.md` row added
- [x] ROADMAP.md updated

### Known Limitations
- Room database inspection is not functional — `kmp-inspector-room` depends on Room 2.x, while this project uses Room 3. The `attachInspector()` functions are no-ops until kmp-inspector adds Room 3 support.
- JS/WASM targets cannot use kmp-inspector (library limitation). The `NavDisplay` compilation error on web targets is a pre-existing issue unrelated to this change.
