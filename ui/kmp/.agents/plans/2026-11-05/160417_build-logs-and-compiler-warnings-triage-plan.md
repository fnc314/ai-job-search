# Implementation Plan: Triage & Resolve Build Warnings in apps-android:assembleDebug

**Status:** Completed  
**Date:** 2026-11-05  
**Target:** ui/kmp  

---

## Goal Description
During `:apps-android:assembleDebug` builds, multiple non-standard warnings and deprecation logs are emitted across Gradle configuration, native library packaging, Compose Material 3 UI widgets, and Kotlin Explicit API compiler checks. 

This plan categorizes each warning, diagnoses the root cause, and specifies the exact code and configuration changes required to **solve the root problem cleanly** (zero suppressions) while maintaining forward compatibility with Gradle 10/11 and latest Jetpack Compose M3 standards.

---

## Warning Inventory & Root Cause Analysis

### Category 1: Gradle Build & Dependency Deprecations (External / Ignorable)
1. **`The Configuration.setVisible(boolean) method has been deprecated. (Gradle 11 removal)`**
   - **Origin**: `:records:records-proto` via `com.google.protobuf` Gradle plugin 0.9.4.
   - **Root Cause**: Internal to Google's `protobuf-gradle-plugin` 0.9.4 when configuring tasks.
   - **Triage Decision**: Marked as **External / Ignorable Dependency Warning**. Upgrades will be monitored in upstream protobuf plugin releases without hacking plugin internals.

2. **`Declaring dependencies using multi-string notation has been deprecated. (Gradle 10 removal)`**
   - **Origin**: `:records:records-proto` internal protoc artifact resolution.
   - **Root Cause**: Internal to Google's `protobuf-gradle-plugin` coordinate resolution (`:osx-aarch_64@exe`).
   - **Triage Decision**: Marked as **External / Ignorable Dependency Warning**. Upstream plugin responsibility.

---

### Category 2: Android Native Packaging (`stripDebugDebugSymbols`)
1. **`Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libdatastore_shared_counter.so, libsqliteJni.so.`**
   - **Origin**: `:apps-android:stripDebugDebugSymbols`.
   - **Root Cause**: AGP attempts to strip symbols from pre-compiled AAR shared libraries (`androidx.graphics.path`, `datastore`, `androidx.sqlite`). Because these vendored native binaries are already stripped or lack NDK debug symbols, AGP emits a warning.
   - **Solution**: Configure `packaging.jniLibs.keepDebugSymbols += listOf("**/libandroidx.graphics.path.so", "**/libdatastore_shared_counter.so", "**/libsqliteJni.so")` in `:apps-android` build script (or the `local.android.kmp.library` / `local.android.application` convention plugin) instructing AGP to package them directly without attempting redundant stripping.

---

### Category 3: Kotlin Explicit API Mode Violations
1. **`NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING` (15 occurrences)**
   - **Origin**: Domain use case interfaces and classes in `features/job-application-details/domain` and `features/settings/domain`:
     - `GetArtifactSignedUrlUseCase.kt` (lines 5, 6)
     - `LoadCompaniesUseCase.kt` (lines 6, 7)
     - `LoadJobApplicationUseCase.kt` (lines 6, 7)
     - `RecordEventUseCase.kt` (lines 6, 7)
     - `UpdateApplicationStatusUseCase.kt` (lines 7, 8)
     - `UpdateFitScoreUseCase.kt` (lines 6, 7)
     - `ObserveSettingsUseCase.kt` (lines 7, 10)
     - `UpdateSettingsUseCase.kt` (line 6)
   - **Root Cause**: Declarations lack explicit `public` visibility modifier in modules with `explicitApiWarning()`.
   - **Solution**: Add explicit `public` to all declarations and nested parameter classes/properties.

2. **`NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING` (1 occurrence)**
   - **Origin**: `SortJobApplicationsUseCase.kt:17` in `features/job-applications-list/domain`.
   - **Root Cause**: `val DATE = DATE_CREATED` omitted explicit return type `SortColumn`.
   - **Solution**: Change to `public val DATE: SortColumn = DATE_CREATED`.

---

### Category 4: Compose Material 3 API Deprecations
1. **`Modifier.menuAnchor()` without parameters is deprecated (5 occurrences)**
   - **Origin**: 
     - `features/add-job-application/ui/.../AddJobApplicationScreen.kt:328, 372, 416`
     - `features/job-application-details/ui/.../DetailDialogs.kt:112, 193`
   - **Root Cause**: Material 3 deprecated bare `Modifier.menuAnchor()` in favor of `Modifier.menuAnchor(type: ExposedDropdownMenuAnchorType, enabled: Boolean)`.
   - **Solution**: For read-only dropdown fields, update to `Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)`.

2. **`rememberPlainTooltipPositionProvider()` & `rememberTooltipPositionProvider()` without positioning parameter (3 occurrences)**
   - **Origin**:
     - `core/widgets/.../TooltipIconButton.kt:24`
     - `features/job-applications-list/ui/.../FilterMenu.kt:78`
     - `features/job-applications-list/ui/.../SortMenu.kt:88`
   - **Root Cause**: `rememberPlainTooltipPositionProvider()` was deprecated in favor of `rememberTooltipPositionProvider()`. In turn, `rememberTooltipPositionProvider()` with default positioning was deprecated in favor of explicitly providing `TooltipAnchorPosition.Above` (or `positioning = TooltipAnchorPosition.Above`).
   - **Solution**: Pass `positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above)`.

3. **`rememberSwipeToDismissBoxState(confirmValueChange = ...)` is deprecated (1 occurrence)**
   - **Origin**: `core/widgets/.../NotesWidget.kt:289`.
   - **Root Cause**: Material 3 deprecated `confirmValueChange` in `rememberSwipeToDismissBoxState` without replacement because state change vetoes should be governed by the anchor set or positional threshold rather than intercepting callback predicates.
   - **Solution**: Use `rememberSwipeToDismissBoxState()` without `confirmValueChange`. React to item dismissal cleanly via `LaunchedEffect(dismissState.targetValue)` or when `dismissState.currentValue != SwipeToDismissBoxValue.Settled`, setting `isVisible = false` and invoking the delete callback upon animation completion.

---

## User Review Required

> [!IMPORTANT]
> **Zero Suppressions Invariant**: None of these logs will be addressed with `@Suppress(...)` or `@SuppressWarnings`. Every single warning will be resolved by upgrading dependencies, configuring packaging filters, specifying explicit visibilities/types, and migrating to canonical Material 3 replacement APIs.

> [!NOTE]
> **In-Repo Plan Storage**: This plan will be committed directly to `ui/kmp/.agents/plans/build-logs-and-compiler-warnings-triage-plan.md` and registered in `ui/kmp/.agents/plans/README.md`.

---

## Proposed Changes

### Component 1: `apps:android` Packaging Configuration

#### [MODIFY] `apps/android/build.gradle.kts`
Configure `packaging.jniLibs.keepDebugSymbols` so AGP skips stripping pre-packaged vendor binaries:
```kotlin
android {
    ...
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            keepDebugSymbols += listOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so",
                "**/libsqliteJni.so"
            )
        }
    }
}
```

---

### Component 2: `core:widgets` Compose M3 Deprecation Fixes

#### [MODIFY] `core/widgets/.../TooltipIconButton.kt`
Replace deprecated `rememberPlainTooltipPositionProvider()`:
```kotlin
@Composable
public fun TooltipIconButton(
    tooltip: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
): Unit {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above
        ),
        tooltip = {
            PlainTooltip {
                Text(tooltip)
            }
        },
        state = rememberTooltipState(),
    ) { ... }
}
```

#### [MODIFY] `core/widgets/.../NotesWidget.kt`
Replace deprecated `rememberSwipeToDismissBoxState(confirmValueChange = ...)`:
```kotlin
val dismissState = rememberSwipeToDismissBoxState()

LaunchedEffect(dismissState.targetValue) {
    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
        isVisible = false
    }
}
```

---

### Component 3: Domain Use Cases Explicit API Fixes

#### [MODIFY] `features/job-application-details/domain/.../*.kt`
Add explicit `public` visibility to use case interfaces and `Params` data classes:
- `GetArtifactSignedUrlUseCase.kt`:
  ```kotlin
  public interface GetArtifactSignedUrlUseCase : UseCase<GetArtifactSignedUrlUseCase.Params, String> {
      public data class Params(public val path: String)
  }
  ```
- `LoadCompaniesUseCase.kt`:
  ```kotlin
  public interface LoadCompaniesUseCase : UseCase<LoadCompaniesUseCase.Params, List<String>> {
      public class Params
  }
  ```
- `LoadJobApplicationUseCase.kt`:
  ```kotlin
  public interface LoadJobApplicationUseCase : UseCase<LoadJobApplicationUseCase.Params, JobApplication?> {
      public data class Params(public val id: String)
  }
  ```
- `RecordEventUseCase.kt`:
  ```kotlin
  public interface RecordEventUseCase : UseCase<RecordEventUseCase.Params, Unit> {
      public data class Params(public val applicationId: String, public val event: TrackerEvent)
  }
  ```
- `UpdateApplicationStatusUseCase.kt`:
  ```kotlin
  public interface UpdateApplicationStatusUseCase : UseCase<UpdateApplicationStatusUseCase.Params, Unit> {
      public data class Params(
          public val applicationId: String,
          public val status: TrackerStatus,
          public val effectiveDate: String? = null,
          public val note: String? = null,
      )
  }
  ```
- `UpdateFitScoreUseCase.kt`:
  ```kotlin
  public interface UpdateFitScoreUseCase : UseCase<UpdateFitScoreUseCase.Params, Unit> {
      public data class Params(public val applicationId: String, public val score: Int)
  }
  ```

#### [MODIFY] `features/settings/domain/.../*.kt`
- `ObserveSettingsUseCase.kt`:
  ```kotlin
  public class ObserveSettingsUseCase(
      private val repository: SettingsRepository,
  ) {
      public operator fun invoke(): StateFlow<AppSettings> = repository.settings
  }
  ```
- `UpdateSettingsUseCase.kt`:
  ```kotlin
  public class UpdateSettingsUseCase(
      private val repository: SettingsRepository,
  ) {
      public operator fun invoke(settings: AppSettings): Unit {
          repository.updateSettings(settings)
      }
  }
  ```

#### [MODIFY] `features/job-applications-list/domain/.../SortJobApplicationsUseCase.kt`
- Explicit return type on companion property:
  ```kotlin
  public companion object {
      public val DATE: SortColumn = DATE_CREATED
  }
  ```

---

### Component 4: Feature UI Compose M3 Deprecation Fixes

#### [MODIFY] `features/add-job-application/ui/.../AddJobApplicationScreen.kt`
Update 3 occurrences of `menuAnchor()`:
```kotlin
Modifier
    .fillMaxWidth()
    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
```

#### [MODIFY] `features/job-application-details/ui/.../DetailDialogs.kt`
Update 2 occurrences of `menuAnchor()`:
```kotlin
Modifier
    .fillMaxWidth()
    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
```

#### [MODIFY] `features/job-applications-list/ui/.../FilterMenu.kt` & `SortMenu.kt`
Update `rememberTooltipPositionProvider()`:
```kotlin
positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
    positioning = TooltipAnchorPosition.Above
)
```

---

### Component 5: Documentation & Invariant Updates

#### [MODIFY] `ui/kmp/AGENTS.md`
Add Invariant Rule 7 documenting strict adherence to zero-warning compilation, explicit API requirements in domain modules, and proper M3 position provider parameters.

#### [NEW] `ui/kmp/.agents/plans/build-logs-and-compiler-warnings-triage-plan.md`
Commit this plan into `ui/kmp/.agents/plans/`.

#### [MODIFY] `ui/kmp/.agents/plans/README.md`
Register this plan in the index.

---

## Verification Plan

### Automated Build Verification
1. Run full Android debug assembly with all warnings enabled:
   ```bash
   ./gradlew :apps-android:assembleDebug --warning-mode all
   ```
2. Verify output produces **ZERO** warnings:
   - Zero `Kotlin compiler warning`
   - Zero `DEPRECATION`
   - Zero `NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING`
   - Zero `NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING`
   - Zero `Unable to strip the following libraries`
3. Run unit tests across all affected modules:
   ```bash
   ./gradlew :core-widgets:jvmTest :features-job-applications-list-domain:jvmTest :features-job-applications-details-domain:jvmTest :features-settings-domain:jvmTest :features-add-job-application-ui:jvmTest
   ```
