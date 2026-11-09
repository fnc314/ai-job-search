# Feature: Sign In

> [!NOTE]
> **Quick Facts**
> - **Gradle Project IDs**: `:frontend:features-sign-in-domain`, `:frontend:features-sign-in-data`, `:frontend:features-sign-in-ui`
> - **Module Tier**: Feature / User Experience (Clean Architecture: Domain, Data, UI)
> - **Package Base**: `dev.fnc314.tools.jobtracker.features.signin`
> - **Target Platforms**: Android (`androidTarget`), Desktop JVM (`jvm`), iOS (`iosX64`, `iosArm64`, `iosSimulatorArm64`), Web (`js`, `wasmJs`)
> - **Internal Project Dependencies**:
>   - Domain: `:frontend:core-data`
>   - Data: `:frontend:features-sign-in-domain`, `:frontend:core-data`
>   - UI: `:frontend:features-sign-in-domain`, `:frontend:features-sign-in-data`, `:frontend:core-widgets`, `:frontend:core-navigation`, `:frontend:core-data`

---

## Overview

The `features/sign-in` module provides the candidate authentication and identity onboarding experience for the application. It orchestrates user authentication with Google Identity and Firebase Authentication across all supported Kotlin Multiplatform targets, exposes real-time authentication state flows, and provides a Material 3 login screen.

---

## Architectural Breakdown & Subprojects

The feature is decomposed across three subprojects adhering strictly to Clean Architecture:

```mermaid
graph TD
    UI[":features-sign-in-ui"] --> Domain[":features-sign-in-domain"]
    UI --> Data[":features-sign-in-data"]
    UI --> Widgets[":core-widgets"]
    Data --> Domain
    Domain --> CoreData[":core-data"]
    Data --> CoreData
```

### 1. `:features-sign-in-domain`
Defines pure Kotlin business contracts and use cases with no UI or platform framework dependencies:
- [`AuthRepository`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/domain/repository/AuthRepository.kt): Domain repository interface contract defining authentication lifecycle, session state observation, and sign-out.
- [`SignInUseCase`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/domain/usecase/SignInUseCase.kt): Triggers the provider authentication flow via `AuthRepository`.
- [`ObserveAuthStateUseCase`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/domain/usecase/ObserveAuthStateUseCase.kt): Streams the reactive `StateFlow<AuthState>` via `AuthRepository`.
- [`GetCurrentUserUseCase`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/domain/usecase/GetCurrentUserUseCase.kt): Retrieves the currently signed-in `UserAccount?` via `AuthRepository`.
- [`SignOutUseCase`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/domain/usecase/SignOutUseCase.kt): Signs the candidate out and invalidates cached credentials via `AuthRepository`.

### 2. `:features-sign-in-data`
Provides concrete data sources, platform handlers, and repository implementations:
- [`AuthRepositoryImpl`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/data/repository/AuthRepositoryImpl.kt): `internal` repository implementation coordinating `PlatformSignInHandler`, `RemoteAuthDataSource`, and `LocalAuthDataSource`.
- [`RemoteAuthDataSource`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/data/source/RemoteAuthDataSource.kt): Remote data source interface and `internal class RemoteAuthDataSourceImpl` managing remote auth token validation and profile sync.
- [`LocalAuthDataSource`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/data/source/LocalAuthDataSource.kt): Local data source interface and `internal class LocalAuthDataSourceImpl` caching credentials and session state.
- [`PlatformSignInHandler`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/data/PlatformSignInHandler.kt): `expect`/`actual` bridge managing native authentication (e.g. Credential Manager on Android, web popup, native desktop/iOS auth).
- [`SignInUseCaseImpl`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/data/SignInUseCaseImpl.kt): Use case implementation delegating to `AuthRepository`.
- [`signInDataModule`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/data/SignInDataModule.kt): Koin DI module registering data sources, repository, use case implementations, and platform auth bindings.

### 3. `:features-sign-in-ui`
Encapsulates the Jetpack Compose user interface, presentation models, and MVI flow:
- [`SignInRoute`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/ui/SignInNavigation.kt): Type-safe `NavKeyRoute` destination object.
- [`signInNavHook`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/ui/SignInNavigation.kt): `FeatureNavigationHook` registering `composable<SignInRoute>` into the application `NavHost`.
- [`UserAccountUiModel`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/ui/UserAccountUiModel.kt): Immutable presentation model representing the authenticated user (`email`, `displayName`, `photoUrl`), ensuring composables remain decoupled from domain models (`UserAccount`).
- [`SignInUiIntent`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/ui/SignInUiIntent.kt): Sealed interface representing UI interactions (`ClickSignIn`, `ClickSignOut`, `ClearError`) dispatched to `SignInViewModel.onIntent`.
- [`SignInUiState`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/ui/SignInUiState.kt): Sealed interface modeling UI states (`Unauthenticated`, `SigningIn`, `Authenticated(user)`, `Error(message)`).
- [`SignInViewModel`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/ui/SignInViewModel.kt): MVI coordinator managing state flow, exposing `uiState: StateFlow<SignInUiState>` and accepting intents via `onIntent(SignInUiIntent): Unit`.
- [`SignInScreen`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/ui/SignInScreen.kt): Composable screen providing responsive branding, Google Sign-In call to action, loading indicators, and error snackbar notifications driven by `SignInUiState` and dispatching `SignInUiIntent`.
- [`signInUiModule`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/ui/SignInUiModule.kt): Koin DI factory for `SignInViewModel`.
- [`signInFeatureModule`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/sign-in/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/ui/SignInUiModule.kt): Umbrella module including both `signInDataModule` and `signInUiModule`.

---

## Deep-Link Support

The sign-in destination is defined in the type-safe Compose Navigation graph:

| Route Type | URI Pattern | Description |
| :--- | :--- | :--- |
| `SignInRoute` | `jobtracker://login` | Navigates unauthenticated users to the identity sign-in screen. |

When launched with an unauthenticated session or when an active session expires, the root `NavHost` automatically routes to `SignInRoute`. Upon successful authentication, the reactive `AuthState` stream triggers navigation to the primary applications dashboard (`ListRoute`).

---

## Key Gradle Tasks

Run the following commands from the repository root:

```bash
# Compile CommonMain Kotlin metadata across all 3 subprojects
./gradlew :frontend:features-sign-in-domain:compileCommonMainKotlinMetadata \
          :frontend:features-sign-in-data:compileCommonMainKotlinMetadata \
          :frontend:features-sign-in-ui:compileCommonMainKotlinMetadata

# Run unit tests
./gradlew :frontend:features-sign-in-domain:allTests \
          :frontend:features-sign-in-data:allTests \
          :frontend:features-sign-in-ui:allTests

# Assemble Android debug variant for UI module
./gradlew :frontend:features-sign-in-ui:assembleDebug
```
