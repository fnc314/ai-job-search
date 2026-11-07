# Implementation Plan: Refactor Settings Feature and Add Firebase Remote Config

**Status:** Draft / Proposed  
**Date:** 2026-11-06  
**Target:** `ui/kmp`  

---

## 1. Executive Summary & Goals

This plan outlines two key architectural initiatives in `ui/kmp`:
1. **Firebase Remote Config Integration**: Declare Remote Config dependencies in `gradle/libs.versions.toml` using the existing Firebase BOM and configure `build.gradle.kts` files without mutating application code yet.
2. **Settings Feature Refactoring (`features/settings`)**: Fully refactor `features/settings` (`domain`, `data`, `ui`) to mirror `features/job-applications-list` in dependency ordering, DI module naming (`settingsFeatureModule` including domain, data, and ui modules), packaging, explicit API visibility, and MVI state/intent patterns.
3. **Settings Functionality**:
   - Device-based default theme, manual Light/Dark/System toggle.
   - Customizable M3 Expressive seed color with random seed suggestions.
   - Direct RRGGBB hex control input field with validation.
   - Sign out action.
4. **Testing & Documentation**: Align unit tests, document public entities with KDoc, and update `features/settings/README.md`.

---

## 2. Architecture & Design Principles

- **Clean Architecture & MVI**: Strict separation of Domain use cases/repositories, Data persistence/sources, and UI ViewModels (`SettingsViewModel`), UI States (`SettingsUiState`), and UI Intents (`SettingsUiIntent`).
- **DI Module Standardization**: Umbrella `settingsFeatureModule` including `settingsDataModule`, `settingsDomainModule`, and `settingsUiModule`.
- **Explicit API Mode**: Explicit visibility (`public`, `internal`, `private`) and explicit return types across all declarations.

---

## 3. Proposed Changes & Implementation Steps

### Step 1: Firebase Remote Config Version Catalog & Gradle Setup
#### [MODIFY] [gradle/libs.versions.toml](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/gradle/libs.versions.toml)
- Add `firebase-remote-config` library pointing to `com.google.firebase:firebase-config-ktx` or BOM equivalent.
#### [MODIFY] [core/data/build.gradle.kts](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/core/data/build.gradle.kts) & related build files
- Add Remote Config dependencies to appropriate build files without runtime logic changes.

### Step 2: Refactor `features/settings` Domain, Data, and UI Layers
#### [MODIFY] Domain Module (`features/settings/domain`)
- Ensure clean repository contracts and use cases (`ObserveSettingsUseCase`, `UpdateSettingsUseCase`, etc.) with explicit API visibility.
- Create `settingsDomainModule` in `features/settings/domain/src/commonMain/kotlin/.../di/SettingsDomainModule.kt`.

#### [MODIFY] Data Module (`features/settings/data`)
- Refactor `SettingsRepositoryImpl` and `LocalSettingsDataSource` adhering to explicit visibility.
- Create `settingsDataModule` in `features/settings/data/src/commonMain/kotlin/.../di/SettingsDataModule.kt`.

#### [MODIFY] UI Module (`features/settings/ui`)
- Refactor `SettingsViewModel`, `SettingsUiState`, `SettingsUiIntent`, and `SettingsScreen` to follow MVI architecture mirroring `features/job-applications-list`.
- Fix nesting and top bar issues (properly embedded within `AppScaffold` or feature screen hierarchy).
- Support Theme (Light/Dark/System), seed color selection, random seed persistence, RRGGBB hex formatting input, and sign out action.
- Update `SettingsUiModule.kt` to define `settingsUiModule` and umbrella `settingsFeatureModule` including data, domain, and UI modules.

### Step 3: Unit Tests & Documentation
- Align and update unit tests (`SettingsViewModelTest`, `SettingsRepositoryTest`).
- Add KDoc for public entities and update `features/settings/README.md`.

---

## 4. Verification Plan & Testing Checklist

- [ ] Compile KMP metadata: `./gradlew compileCommonMainKotlinMetadata`
- [ ] Run unit tests for settings feature: `./gradlew :features-settings-domain:allTests :features-settings-data:allTests :features-settings-ui:allTests`
- [ ] Assemble Android debug APK: `mise run ui:kmp:android:assemble`
