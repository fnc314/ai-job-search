# Implementation Plan: Integrate Kermit Logger with KMP Inspector

**Status:** Completed  
**Date:** 2026-11-12  
**Target:** `frontend`  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261112-024023-9wsc  
**Depends On:** [`kmp-inspector-seamless-integration.md`](./2026-11-06/200000_kmp-inspector-seamless-integration.md)  

---

## 1. Executive Summary & Goals

Bridge Kermit 2.2.0 logging to KMP Inspector so that all application logs automatically appear in the Inspector's Logs panel. This uses the ["Feeding it data"](https://github.com/waqas028/kmp-inspector#feeding-it-data) APIs (`InspectorLog`) to route Kermit `LogWriter` output into the inspector overlay.

**In Scope:**
- Create `KmpInspectorLogWriter` implementing Kermit's `LogWriter` to route logs to `InspectorLog`
- Expect/actual pattern: real implementation on android, ios, jvm; no-op on js, wasmJs
- Wire `KmpInspectorLogWriter` into Kermit's logger configuration in `App.kt`
- Add Kermit 2.2.0 dependency to the version catalog and `apps/core`

**Out of Scope:**
- Manual `Inspector.network()` calls (already handled by `KmpInspectorPlugin` in Ktor)
- Manual `Inspector.custom()` calls (can be added ad hoc when needed)
- Database inspection via `kmp-inspector-room` (Room 3 not supported yet)
- Creating the `core/log` module (that is a separate pending plan)

## 2. Architecture & Design Principles

**Current State:**
KMP Inspector is already integrated for network traffic (`KmpInspectorPlugin` in Ktor on android, ios, jvm), UI overlay (`KmpInspector` composable wrapper), and database stubs. The "Feeding it data" APIs (`InspectorLog.v/d/i/w/e`) are not yet used.

**Key Decisions:**
- **`KmpInspectorLogWriter` in `apps/core`** — placed where KMP Inspector dependencies already exist, rather than in `core/log` which doesn't exist yet. Can be moved later.
- **Expect/actual pattern** — `commonMain` declares `expect class KmpInspectorLogWriter : LogWriter()`. Real implementations on android, ios, jvm route to `InspectorLog`. No-op on js, wasmJs.
- **Severity mapping** — Kermit `Severity` maps to `InspectorLog` methods: `VERBOSE`→`v`, `DEBUG`→`d`, `INFO`→`i`, `WARN`→`w`, `ERROR`→`e`, `ASSERT`→`e`.
- **Registered via `Logger.setLogWriters()`** — logs flow to both platform loggers AND the inspector simultaneously.

**Architecture:**
```
Application Code → Kermit Logger → platformLogWriter → Logcat/OSLog/console
                            → KmpInspectorLogWriter → InspectorLog → KMP Inspector Logs Panel
```

## 3. Proposed Changes & Implementation Steps

### 3.1. Version catalog (`gradle/libs.versions.toml`)
- Add `kermit = "2.2.0"` to `[versions]`
- Add `kermit = { module = "co.touchlab:kermit", version.ref = "kermit" }` to `[libraries]`

### 3.2. `KmpInspectorLogWriter` expect/actual in `apps/core`
- `commonMain`: `expect class KmpInspectorLogWriter : LogWriter()`
- `androidMain`, `iosMain`, `jvmMain`: actual class mapping `Severity` to `InspectorLog` calls
- `jsMain`, `wasmJsMain`: actual no-op class

### 3.3. Wire in `App.kt`
- Call `Logger.setLogWriters(listOf(platformLogWriter(), KmpInspectorLogWriter()))` at the start of `App()`

### 3.4. Add Kermit dependency to `apps/core`
- `implementation(libs.kermit)` in `commonMain` dependencies

## 4. Verification Plan & Testing Checklist

- [x] `./gradlew :frontend:apps-core:compileAndroidMain` — Android compiles
- [x] `./gradlew :frontend:apps-core:compileKotlinJvm` — JVM compiles
- [x] JS/WasmJs compile (pre-existing core/widgets errors unrelated to this change)
- [ ] Run Android app and verify logs appear in KMP Inspector overlay

## 5. Bookkeeping
- [x] Plan persisted at `frontend/.agents/plans/2026-11-12/030100_integrate-kermit-logger.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Craft a single commit of all changed files
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Push `git push origin main`
