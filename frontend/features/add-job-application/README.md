# Feature: Add Job Application & Lead

> [!NOTE]
> **Quick Facts**
> - **Gradle Project IDs**: `:frontend:features-add-job-application-domain`, `:frontend:features-add-job-application-data`, `:frontend:features-add-job-application-ui`
> - **Module Tier**: Feature / User Experience (Clean Architecture: Domain, Data, UI)
> - **Package Base**: `dev.fnc314.tools.jobtracker.features.addjobapplication`
> - **Target Platforms**: Android (`androidTarget`), Desktop JVM (`jvm`), iOS (`iosX64`, `iosArm64`, `iosSimulatorArm64`), Web (`js`, `wasmJs`)
> - **Internal Project Dependencies**:
>   - Domain: `:frontend:core-data`
>   - Data: `:frontend:features-add-job-application-domain`, `:frontend:core-data`
>   - UI: `:frontend:features-add-job-application-domain`, `:frontend:features-add-job-application-data`, `:frontend:core-widgets`, `:frontend:core-navigation`, `:frontend:core-data`

---

## Overview

The `features/add-job-application` module provides intake workflows for candidate opportunities. It supports two entry modalities:
1. **Full Job Application Entry**: Capturing company, role, sector, role type, work mode (`REMOTE`, `HYBRID`, `ONSITE`), initial lifecycle status, compensation ranges, posting URLs, and recruiter contacts.
2. **Quick Lead Creation**: Lightweight capture for early-stage discovery, recruiter cold outreach, informal networking connections, and prospective leads.

---

## Architectural Breakdown & Subprojects

The feature is organized across three Clean Architecture subprojects:

```mermaid
graph TD
    UI[":features-add-job-application-ui"] --> Domain[":features-add-job-application-domain"]
    UI --> Data[":features-add-job-application-data"]
    UI --> Widgets[":core-widgets"]
    Data --> Domain
    Domain --> CoreData[":core-data"]
    Data --> CoreData
```

### 1. `:features-add-job-application-domain`
Defines intake business contracts:
- [`AddJobApplicationRepository`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/domain/repository/AddJobApplicationRepository.kt): Domain repository interface contract defining operations for inserting validated job applications and unstructured prospective leads.
- [`CreateJobApplicationUseCase`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/domain/usecase/CreateJobApplicationUseCase.kt): Saves a validated `JobApplication` entity into storage via `AddJobApplicationRepository` and returns the generated document ID.
- [`CreateLeadUseCase`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/domain/usecase/CreateLeadUseCase.kt): Transforms unstructured lead data into a `DRAFTING` application record with initial notes and channel metadata via `AddJobApplicationRepository`.

### 2. `:features-add-job-application-data`
Provides concrete data sources and repository implementations:
- [`AddJobApplicationRepositoryImpl`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/data/repository/AddJobApplicationRepositoryImpl.kt): `internal` repository implementation coordinating `RemoteAddJobApplicationDataSource` and `LocalAddJobApplicationDataSource`.
- [`RemoteAddJobApplicationDataSource`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/data/source/RemoteAddJobApplicationDataSource.kt): Remote data source interface and `internal class RemoteAddJobApplicationDataSourceImpl` backed by `RemoteDataSource` / Firestore.
- [`LocalAddJobApplicationDataSource`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/data/source/LocalAddJobApplicationDataSource.kt): Local data source interface and `internal class LocalAddJobApplicationDataSourceImpl` backed by Room 3 SQLite database (`DatabaseHolder` / `JobApplicationDao`) with in-memory fallback.
- [`CreateJobApplicationUseCaseImpl`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/data/CreateJobApplicationUseCaseImpl.kt): Queries `AddJobApplicationRepository`.
- [`CreateLeadUseCaseImpl`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/data/CreateLeadUseCaseImpl.kt): Queries `AddJobApplicationRepository`.
- [`addJobApplicationDataModule`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/data/AddJobApplicationDataModule.kt): Koin DI declarations registering data sources and repository.

### 3. `:features-add-job-application-ui`
Supplies Compose screens, MVI state models, and unidirectional data flow:
- [`AddJobApplicationFormUiModel`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddJobApplicationFormUiModel.kt): `@Immutable` data model representing application intake form fields, notes, and validation errors.
- [`AddJobApplicationUiState`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddJobApplicationUiState.kt): Sealed interface modeling UI states (`Editing`, `Submitting`, `Success`, `Error`, `NavigateBack`).
- [`AddJobApplicationUiIntent`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddJobApplicationUiIntent.kt): Sealed interface representing unidirectional user intents for updating fields, submitting, and navigating.
- [`AddJobApplicationViewModel`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddJobApplicationViewModel.kt): MVI ViewModel exposing `uiState: StateFlow<AddJobApplicationUiState>` and processing intents via `onIntent(intent: AddJobApplicationUiIntent)`.
- [`AddJobApplicationScreen`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddJobApplicationScreen.kt): Multi-field intake form with dropdown selectors for work mode, channel, and status, with dynamic note entry via `NotesWidget`, dispatching intents to `AddJobApplicationViewModel`.
- [`AddLeadFormUiModel`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddLeadFormUiModel.kt): `@Immutable` data model representing fast lead intake form fields, notes, and validation errors.
- [`AddLeadUiState`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddLeadUiState.kt): Sealed interface modeling lead UI states (`Editing`, `Submitting`, `Success`, `Error`, `NavigateBack`).
- [`AddLeadUiIntent`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddLeadUiIntent.kt): Sealed interface representing unidirectional lead creation intents.
- [`AddLeadViewModel`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddLeadViewModel.kt): MVI ViewModel exposing `uiState: StateFlow<AddLeadUiState>` and processing intents via `onIntent(intent: AddLeadUiIntent)`.
- [`AddLeadScreen`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddLeadScreen.kt): Fast-entry screen optimized for rapid note-taking and lead logging, dispatching intents to `AddLeadViewModel`.
- [`AddApplicationRoute`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddJobApplicationNavigation.kt): Type-safe destination key implementing `NavKeyRoute`.
- [`AddLeadRoute`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddJobApplicationNavigation.kt): Type-safe destination key implementing `NavKeyRoute`.
- [`addJobApplicationNavHook`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddJobApplicationNavigation.kt): `FeatureNavigationHook` registering `composable<AddApplicationRoute>` and `composable<AddLeadRoute>` in the root `NavGraphBuilder`.
- [`addJobApplicationUiModule`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddJobApplicationUiModule.kt): Koin factories for both ViewModels.
- [`addJobApplicationFeatureModule`](file:///Users/fnc314/Work/Search/ai-job-search/frontend/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddJobApplicationUiModule.kt): Umbrella DI module.

---

## Deep-Link Support

The intake destinations are registered with distinct type-safe navigation routes:

| Route Type | URI Pattern | Description |
| :--- | :--- | :--- |
| `AddApplicationRoute` | `jobtracker://applications/new` | Opens the full application intake form. |
| `AddLeadRoute` | `jobtracker://leads/new` | Opens the fast lead intake form. |

Both routes support back-navigation (`onNavigateBack`) popping back to the previous screen or returning to `ListRoute`.

---

## Key Gradle Tasks

Run the following commands from the repository root:

```bash
# Compile CommonMain Kotlin metadata across all 3 subprojects
./gradlew :frontend:features-add-job-application-domain:compileCommonMainKotlinMetadata \
          :frontend:features-add-job-application-data:compileCommonMainKotlinMetadata \
          :frontend:features-add-job-application-ui:compileCommonMainKotlinMetadata

# Run unit tests
./gradlew :frontend:features-add-job-application-domain:allTests \
          :frontend:features-add-job-application-data:allTests \
          :frontend:features-add-job-application-ui:allTests

# Assemble Android debug variant for UI module
./gradlew :frontend:features-add-job-application-ui:assembleDebug
```
