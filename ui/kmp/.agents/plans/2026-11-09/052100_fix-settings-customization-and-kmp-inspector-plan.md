# Implementation Plan: Fix Settings Customization and KmpInspector Parity

**Status:** Completed  
**Date:** 2026-11-09  
**Target:** `ui/kmp/features/settings`, `ui/kmp/apps/core`  
**Depends On:** `050600_squash-tracker-edits-navigation-and-title-plan.md`  

---

## 1. Problem Statement
1. **Settings Customizations Do Not Take Effect**:
   Selecting theme modes (Light/Dark/System) or custom accent seeds (e.g. `#386a20`) does not update the active application theme. The root composable in [`App.kt`](../../apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/App.kt#L107-L108) observes `AppSettingsRepository.settings`, while [`SettingsViewModel`](../../features/settings/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/settings/ui/viewmodel/SettingsViewModel.kt#L45-L48) observes and updates `SettingsRepositoryImpl` via use cases. These two repositories maintain completely separate `MutableStateFlow<AppSettings>` instances in memory. As a result, mutations made in Settings never propagate to the active root `JobTrackerTheme` in [`App.kt`](../../apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/App.kt#L111).

2. **Checkbox Controller Does Not Disable KmpInspector**:
   Even when the checkbox indicates "KMP Inspector is disabled", the floating circular inspector button remains visible and interactive on screen. In [`App.kt`](../../apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/App.kt#L131-L135), the code conditionally executes:
   ```kotlin
   if (settings.enableKmpInspector) {
     KmpInspector(enabled = true, content = appContent)
   } else {
     appContent()
   }
   ```
   Because `KmpInspector` is omitted rather than passed `enabled = false`, the underlying library's internal window/bubble teardown logic (`com.waqas028.kmpinspector.KmpInspector(enabled = false)`) is never invoked, leaving the active floating bubble orphaned on the window. Furthermore, because of problem #1, `settings.enableKmpInspector` never changed inside `App.kt` in the first place.

---

## 2. Proposed Changes

### 2.1 Unify Settings Repositories (`features:settings:data`)
- Update [`SettingsRepositoryImpl`](../../features/settings/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/settings/data/repository/SettingsRepositoryImpl.kt) to delegate to [`AppSettingsRepository`](../../core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/repository/AppSettingsRepository.kt), ensuring a single unified `StateFlow<AppSettings>` across the entire Koin container.
- Update [`SettingsDataModule.kt`](../../features/settings/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/settings/data/di/SettingsDataModule.kt) to inject `AppSettingsRepository` into `SettingsRepositoryImpl`.
- Ensure settings updates persisted to multiplatform-settings are reflected synchronously in the shared `_settings` flow, updating both the Settings UI and the root `App.kt` theme/inspector observers.

### 2.2 Fix KmpInspector Invocation (`apps:core`)
- In [`App.kt`](../../apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/App.kt#L131-L135), replace the `if (settings.enableKmpInspector)` branch with:
  ```kotlin
  KmpInspector(enabled = settings.enableKmpInspector, content = appContent)
  ```
- This ensures `KmpInspector` remains in the composition tree when disabled, allowing `KmpInspector(content = content, enabled = false)` on Android to properly hide/dismiss the floating bubble overlay via the library's teardown handler.

### 2.3 Update & Add Tests
- Update [`SettingsRepositoryTest.kt`](../../features/settings/data/src/jvmTest/kotlin/dev/fnc314/tools/jobtracker/features/settings/data/repository/SettingsRepositoryTest.kt) to verify reactive delegation and synchronization between `SettingsRepository` and `AppSettingsRepository`.
- Verify `:features-settings-domain:jvmTest`, `:features-settings-data:jvmTest`, and `:features-settings-ui:jvmTest`.

---

## 3. Verification Plan
1. **Automated Unit Tests**:
   - `./gradlew :features-settings-data:jvmTest :features-settings-ui:jvmTest`
   - `mise run dev:konsist`
   - `mise run dev:detekt`
2. **Build Verification**:
   - `./gradlew :apps-android:assembleDebug`
3. **Behavioral Testing**:
   - Verify that updating theme mode immediately updates the root theme.
   - Verify that updating the custom seed hex updates the Material 3 color scheme dynamically.
   - Verify that unchecking "Enable KMP Inspector" hides/dismisses the floating bubble, and checking it restores it.
