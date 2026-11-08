# Architecture Plan: Platform-Agnostic Module Renaming

**Status:** Completed  
**Date:** 2026-11-05  
**Target:** ui/kmp  

---

## Goal Description
Audit and rename all files, classes, and conventions across the multiplatform workspace (`ui/kmp`) that reference specific technology vendors, libraries, or framework solutions by name (e.g., `Firebase` / `Firestore` $\to$ `Remote`, `Ktor` $\to$ `Network`, `Koin` $\to$ `DI`, `SignInWithGoogle` $\to$ `SignIn`).

Adhere strictly to:
- **Exceptions**: `ViewModel` and `Nav` are preserved.
- **Screen Naming**: All screen files end in `Screen` (zero `Compose` or `Composable` suffixes).
- **Core Widgets**: `NotesWidget` is retained. Enhance `NotesWidget` to use `ImmutableList<String>` (from `kotlinx.collections.immutable`) for `initialNotes` and callbacks to prevent unnecessary Compose recompositions.
- **Option A Pattern**: Multiplatform file suffix convention for platform-specific implementations (`<Name>.<platform>.kt`).
- **Complete Dependency Scrubbing**: Eliminate library interfaces (e.g. `KoinComponent`) from ViewModels in favor of constructor injection; rename convention plugins (`local.dependencies.koin` $\to$ `local.dependencies.di`).
- **Delete Unneeded Assets**: Purge dead template graphics (`compose-multiplatform.xml`).
- **Unit Test Grounding**: Update and adjust all unit tests to test the agnostic classes, interfaces, and methods.
- **Module `README.md` Invariant**: Synchronize all module `README.md` files in the same batch.
- **Code Standards**: Strict Kotlin explicit API mode, zero wildcard imports, zero inline FQNames.

---

## User Decisions Incorporated

1. **Retain `NotesWidget`**: Kept `NotesWidget.kt` and `fun NotesWidget`. Optimized its signature from `List<String>` to `ImmutableList<String>` (`persistentListOf()`) to ensure stable Compose inputs and prevent unnecessary recomposition.
2. **Option A Multiplatform Suffixes**: Use `RemoteJobApplicationRepository.android.kt`, `RemoteJobApplicationRepository.jvm.kt`, and `RemoteJobApplicationRepository.kt`.
3. **Delete Dead Assets**: Remove `apps/core/src/commonMain/composeResources/drawable/compose-multiplatform.xml`.
4. **Scrub Explicit Dependencies**:
   - Scrub `KoinComponent` from ViewModels (`JobApplicationDetailViewModel`, `JobApplicationsListViewModel`) by using clean constructor parameter injection.
   - Rename convention plugin `KoinDependenciesConventionPlugin.kt` $\to$ `DIDependenciesConventionPlugin.kt` and plugin ID `local.dependencies.di`.
   - Rename `RemoteJVM.kt` $\to$ `RemotePlatform.jvm.kt` (object `RemotePlatform`).
   - Rename `AndroidApplicationContextHolder.kt` $\to$ `ApplicationContextHolder.android.kt` (object `ApplicationContextHolder`).
5. **Adjust Unit Tests**: Update test suites in `core:data`, `features:sign-in`, and `features:tracker-edits` to test the new agnostic names and signatures.

---

## Proposed Changes

### Master Summary of Renamings & Deletions

| Current Path | Proposed Path | Action / Convention |
| :--- | :--- | :--- |
| **`core/widgets/.../NotesWidget.kt`** | **`core/widgets/.../NotesWidget.kt`** | *Retained*. Optimize to `ImmutableList<String>` |
| **`core/data/.../FirestoreJobApplicationRepository.kt`** | **`core/data/.../RemoteJobApplicationRepository.kt`** | `Firebase` / `Firestore` $\to$ `Remote` |
| **`core/data/.../AndroidFirestoreJobApplicationRepository.kt`** | **`core/data/.../RemoteJobApplicationRepository.android.kt`** | Option A: `Remote` + platform file suffix |
| **`core/data/.../JVMJobApplicationRepository.kt`** | **`core/data/.../RemoteJobApplicationRepository.jvm.kt`** | Option A: `Remote` + platform file suffix |
| **`core/data/.../KtorStorageRepository.kt`** | **`core/data/.../NetworkStorageRepository.kt`** | `Ktor` $\to$ `Network` |
| **`core/data/.../platform/RemoteJVM.kt`** | **`core/data/.../platform/RemotePlatform.jvm.kt`** | Platform suffix convention (object `RemotePlatform`) |
| **`core/data/.../AndroidApplicationContextHolder.kt`** | **`core/data/.../ApplicationContextHolder.android.kt`** | Platform suffix convention (object `ApplicationContextHolder`) |
| **`features/tracker-edits/.../FirestoreTrackerEditsRepository.kt`** | **`features/tracker-edits/.../RemoteTrackerEditsRepository.kt`** | `Firestore` $\to$ `Remote` |
| **`apps/core/.../di/KoinModule.kt`** | **`apps/core/.../di/AppDI.kt`** | `Koin` $\to$ `DI` (val `appDIModule`) |
| **`features/sign-in/.../SignInWithGoogleUseCase.kt`** | **`features/sign-in/.../SignInUseCase.kt`** | Provider-agnostic use case |
| **`features/sign-in/.../SignInWithGoogleUseCaseImpl.kt`** | **`features/sign-in/.../SignInUseCaseImpl.kt`** | Provider-agnostic use case implementation |
| **`gradle/tools/.../KoinDependenciesConventionPlugin.kt`** | **`gradle/tools/.../DIDependenciesConventionPlugin.kt`** | `Koin` $\to$ `DI` convention plugin (`local.dependencies.di`) |
| **`apps/core/.../drawable/compose-multiplatform.xml`** | *[DELETE]* | Purge unneeded template graphic |

---

### Component 1: `core:widgets` Stability Optimization

#### [MODIFY] `core/widgets/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/widgets/NotesWidget.kt`
- Keep file and composable name `NotesWidget`.
- Upgrade parameters from unstable `List<String>` to `ImmutableList<String>` from `kotlinx.collections.immutable`:

```kotlin
package dev.fnc314.tools.jobtracker.core.widgets

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun NotesWidget(
    onNotesChanged: (ImmutableList<String>) -> Unit,
    modifier: Modifier = Modifier,
    initialNotes: ImmutableList<String> = persistentListOf(),
    label: String = "Notes",
) {
    // Retain internal state, emit toImmutableList() on update to prevent recomposition churn
}
```

#### [MODIFY] Call sites of `NotesWidget`:
- `features/add-job-application/ui/.../AddJobApplicationScreen.kt`: Pass and handle `ImmutableList<String>`.
- `features/add-job-application/ui/.../AddLeadScreen.kt`: Pass and handle `ImmutableList<String>`.
- `features/job-application-details/ui/.../JobApplicationsDetailsScreen.kt`: Pass and handle `ImmutableList<String>`.

---

### Component 2: `core:data`

#### [DELETE] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/repository/FirestoreJobApplicationRepository.kt`
#### [NEW] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/repository/RemoteJobApplicationRepository.kt`
- Rename class `FirestoreJobApplicationRepository` $\to$ `RemoteJobApplicationRepository`.

#### [DELETE] `core/data/src/androidMain/kotlin/dev/fnc314/tools/jobtracker/core/data/repository/AndroidFirestoreJobApplicationRepository.kt`
#### [NEW] `core/data/src/androidMain/kotlin/dev/fnc314/tools/jobtracker/core/data/repository/RemoteJobApplicationRepository.android.kt`
- Rename class `AndroidFirestoreJobApplicationRepository` $\to$ `AndroidRemoteJobApplicationRepository`.

#### [DELETE] `core/data/src/jvmMain/kotlin/dev/fnc314/tools/jobtracker/core/data/repository/JVMJobApplicationRepository.kt`
#### [NEW] `core/data/src/jvmMain/kotlin/dev/fnc314/tools/jobtracker/core/data/repository/RemoteJobApplicationRepository.jvm.kt`
- Rename class `JVMJobApplicationRepository` $\to$ `JvmRemoteJobApplicationRepository`.
- Reference `RemotePlatform.initialize()`.

#### [DELETE] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/repository/KtorStorageRepository.kt`
#### [NEW] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/repository/NetworkStorageRepository.kt`
- Rename class `KtorStorageRepository` $\to$ `NetworkStorageRepository`.

#### [DELETE] `core/data/src/jvmMain/kotlin/dev/fnc314/tools/jobtracker/core/data/platform/RemoteJVM.kt`
#### [NEW] `core/data/src/jvmMain/kotlin/dev/fnc314/tools/jobtracker/core/data/platform/RemotePlatform.jvm.kt`
- Rename object `RemoteJVM` $\to$ `RemotePlatform`.

#### [DELETE] `core/data/src/androidMain/kotlin/dev/fnc314/tools/jobtracker/core/data/AndroidApplicationContextHolder.kt`
#### [NEW] `core/data/src/androidMain/kotlin/dev/fnc314/tools/jobtracker/core/data/ApplicationContextHolder.android.kt`
- Rename object `AndroidApplicationContextHolder` $\to$ `ApplicationContextHolder`.

---

### Component 3: `features:tracker-edits`

#### [DELETE] `features/tracker-edits/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/trackeredits/data/FirestoreTrackerEditsRepository.kt`
#### [NEW] `features/tracker-edits/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/trackeredits/data/RemoteTrackerEditsRepository.kt`
- Rename class `FirestoreTrackerEditsRepository` $\to$ `RemoteTrackerEditsRepository`.

#### [MODIFY] `features/tracker-edits/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/trackeredits/data/TrackerEditsDataModule.kt`
- Update binding: `single<TrackerEditsRepository> { RemoteTrackerEditsRepository() }`.

#### [MODIFY] `features/tracker-edits/data/src/commonTest/kotlin/.../TrackerEditsUseCasesTest.kt`
- Ensure tests verify `TrackerEditsRepository` contracts and mock/test implementations.

---

### Component 4: `features:sign-in`

#### [DELETE] `features/sign-in/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/domain/usecase/SignInWithGoogleUseCase.kt`
#### [NEW] `features/sign-in/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/domain/usecase/SignInUseCase.kt`
- Rename interface `SignInWithGoogleUseCase` $\to$ `SignInUseCase`.

#### [DELETE] `features/sign-in/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/data/SignInWithGoogleUseCaseImpl.kt`
#### [NEW] `features/sign-in/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/data/SignInUseCaseImpl.kt`
- Rename class `SignInWithGoogleUseCaseImpl` $\to$ `SignInUseCaseImpl`.

#### [MODIFY] `features/sign-in/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/data/SignInDataModule.kt`
- Bind `factoryOf(::SignInUseCaseImpl) bind SignInUseCase::class`.

#### [MODIFY] `features/sign-in/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/data/DefaultAuthRepository.kt`
- Expose `suspend fun signIn(): Result<UserAccount>` (renamed from `signInWithGoogle()`).

#### [MODIFY] `features/sign-in/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/ui/SignInViewModel.kt`
- Inject `signInUseCase: SignInUseCase`.
- Expose `public fun signIn()` (renamed from `signInWithGoogle()`).

#### [MODIFY] `features/sign-in/domain/src/commonTest/kotlin/.../AuthUseCasesTest.kt`
- Adjust tests to target `SignInUseCase` and assert expected `Result<UserAccount>` behavior:
```kotlin
@Test
fun testSignInUseCase_success() = runTest {
    val fakeRepo = FakeAuthRepository(UserAccount(uid = "user-123", email = "test@example.com"))
    val useCase = SignInUseCaseImpl(fakeRepo)
    val result = useCase()
    assertTrue(result.isSuccess)
    assertEquals("user-123", result.getOrNull()?.uid)
}
```

---

### Component 5: Decouple DI from ViewModels

#### [MODIFY] `features/job-applications-list/ui/src/commonMain/kotlin/.../JobApplicationsListViewModel.kt`
- Remove unused `KoinComponent` interface implementation.

#### [MODIFY] `features/job-application-details/ui/src/commonMain/kotlin/.../JobApplicationDetailViewModel.kt`
- Convert property injection `by inject()` into constructor parameters:
```kotlin
public class JobApplicationDetailViewModel(
    private val loadApplicationUseCase: LoadJobApplicationUseCase,
    private val loadCompaniesUseCase: LoadCompaniesUseCase,
    private val updateStatusUseCase: UpdateApplicationStatusUseCase,
    private val recordEventUseCase: RecordEventUseCase,
    private val updateFitScoreUseCase: UpdateFitScoreUseCase,
    private val getSignedUrlUseCase: GetArtifactSignedUrlUseCase,
) : ViewModel() { ... }
```
- Completely remove `KoinComponent` and `import org.koin...` from the file.

---

### Component 6: `apps:core` & Entrypoints

#### [DELETE] `../../../apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/di`
#### [NEW] `../../../apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/di`
- Rename val `appKoinModule` $\to$ `appDIModule`.

#### [DELETE] `apps/core/src/commonMain/composeResources/drawable/compose-multiplatform.xml`
- Delete dead template graphic per user decision.

#### [MODIFY] Application Entrypoints:
- `../../../apps/android/src/main/kotlin/dev/fnc314/tools/jobtracker/app/android/MainActivity.kt`: Reference `appDIModule`.
- `../../../apps/desktop/src/main/kotlin/dev/fnc314/tools/jobtracker/app/desktop/main.kt`: Reference `appDIModule`.
- `apps/web/src/webMain/kotlin/dev/fnc314/tools/jobtracker/main.kt`: Reference `appDIModule`.

---

### Component 7: Convention Plugins & Version Catalog

#### [DELETE] `gradle/tools/project-plugins/src/main/kotlin/.../config/KoinDependenciesConventionPlugin.kt`
#### [NEW] `gradle/tools/project-plugins/src/main/kotlin/.../config/DIDependenciesConventionPlugin.kt`
- Class `DIDependenciesConventionPlugin : Plugin<Project>, BaseConventionPlugin`.

#### [MODIFY] `gradle/tools/project-plugins/build.gradle.kts`:
- Register `dev.fnc314.tools.jobtracker.dependencies.di` with ID `local.dependencies.di` (and retain `local.dependencies.koin` as alias).

#### [MODIFY] `gradle/libs.versions.toml`:
- Add `local-dependencies-di = { id = "local.dependencies.di" }`.

#### [MODIFY] `build.gradle.kts` in feature and app modules:
- Update `alias(libs.plugins.local.dependencies.koin)` $\to$ `alias(libs.plugins.local.dependencies.di)`.

---

### Component 8: Documentation Synchronization

Update `README.md` across affected modules:
- `apps/core/README.md`: Update references from `KoinModule.kt` to `AppDI.kt` (`appDIModule`).
- `core/data/README.md`: Update architecture tree and table to reflect `RemoteJobApplicationRepository` (with `.android.kt` and `.jvm.kt`), `NetworkStorageRepository`, `RemotePlatform.jvm.kt`, and `ApplicationContextHolder.android.kt`.
- `core/widgets/README.md`: Document `NotesWidget` and its `ImmutableList<String>` stability contract.
- `features/tracker-edits/README.md`: Update references to `RemoteTrackerEditsRepository`.
- `features/sign-in/README.md`: Update use case contracts to `SignInUseCase`.

---

## Verification Plan

### Automated Tests
1. **Metadata Compilation Check**:
   ```bash
   mise exec -- ./gradlew compileCommonMainKotlinMetadata
   ```
2. **Desktop Application Assemble**:
   ```bash
   mise exec -- ./gradlew :apps-desktop:assemble
   ```
3. **Android Debug APK Assemble**:
   ```bash
   mise exec -- ./gradlew :apps-android:assembleDebug
   ```
4. **Unit Tests**:
   ```bash
   mise exec -- ./gradlew :core-data:jvmTest :features-sign-in-domain:allTests :features-tracker-edits-data:allTests :features-job-applications-details-data:allTests :features-job-applications-list-domain:allTests
   ```
5. **Agnostic Naming Audit**:
   Verify zero source files match `*Firebase*`, `*Firestore*`, `*Koin*`, `*Ktor*`, `*Room*`, or `*Composable*.kt`.
