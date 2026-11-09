# Implementation Plan: Koin Annotations Bootstrap & Build Logs Resolution

**Status:** Completed  
**Date:** 2026-11-08  
**Target:** `ui/kmp`  
**Model:** Gemini 3.8 Flash  
**Session:** `7119f97b-15f3-4a20-8cf2-b66898a0dd22`  

---

## 1. Executive Summary & Goals
Executing `:ui:kmp:android:assemble` produces build logs consisting of:
1. Compose Material 3 icon deprecation warning in `JobApplicationRow.kt:150:106` (`Icons.Default.KeyboardArrowRight` -> `Icons.AutoMirrored.Filled.KeyboardArrowRight`).
2. Kotzilla marketing CTA link in `App.kt:54:7` (`[Koin] → Fix with AI: set up Kotzilla MCP...`).
3. Koin compiler diagnostic warning `KOIN-W013` in `App.kt:54:7` indicating that compile-time graph validation for `koinConfiguration` could not be executed because the entry module declares no DSL definitions of its own.

This plan migrates the existing application DI bootstrap to **Koin Annotations (`@KoinApplication`, `@Module`)** while resolving all build logs cleanly without suppressions.

## 2. Architecture & Design Principles
- **Type-Safe Compiler DSL Bootstrap:** `AppDI.kt` declares `@Module class AppModule` and `@KoinApplication(modules = [AppModule::class]) class MainApplication`. `App.kt` invokes `org.koin.plugin.module.dsl.koinConfiguration<MainApplication>`, enabling the Koin Compiler Plugin to resolve the entry point module structure while retaining `compileSafety = true`.
- **Zero Suppressions Invariant:** All warnings are resolved at the root API level without `@Suppress(...)`.

## 3. Proposed Changes & Implementation Steps
1. **`AppDI.kt`**: Add `@Module public class AppModule` and `@KoinApplication(modules = [AppModule::class]) public class MainApplication`.
2. **`App.kt`**: Migrate from `org.koin.dsl.koinConfiguration` to `org.koin.plugin.module.dsl.koinConfiguration<MainApplication>`.
3. **`KoinDependenciesConventionPlugin.kt`**: Set `aiAssist.set(false)` to disable promotional marketing log lines while retaining `compileSafety.set(true)` and `strictSafety.set(true)`.
4. **`JobApplicationRow.kt`**: Replace deprecated `Icons.Default.KeyboardArrowRight` with `Icons.AutoMirrored.Filled.KeyboardArrowRight`.

## 4. Verification Plan & Testing Checklist
- [x] `mise run :ui:kmp:android:assemble` completes with 0 compiler warnings and 0 compiler problems.
- [x] `JobApplicationRow.kt` Compose `Grid` track sizing (`1.fr` member property on `GridConfigurationScope`) and `@Composable` `MaterialTheme.spacing.small` extraction verified.
- [x] `SortJobApplicationsUseCase.kt` non-nullable `Instant` comparator verified with 0 warnings.

## 5. Bookkeeping
- [x] Plan persisted at `ui/kmp/.agents/plans/2026-11-08/193000_koin-annotations-entry-point-and-build-logs-resolution.md`
- [x] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
