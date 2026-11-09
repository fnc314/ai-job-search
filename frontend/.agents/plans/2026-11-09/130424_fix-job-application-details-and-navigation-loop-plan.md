# Architecture & Bugfix Plan: Fix Job Application Details Loading & Navigation Flow

**Status:** Completed  
**Date:** 2026-11-09  
**Target:** `ui/kmp` (`features:job-application-details`, `apps:core`, `core:navigation`, `features:job-applications-list`)  

---

## 1. Goal Description & Problem Analysis

### 1.1 The Problems
1. **Details Page Glaring Red Error Box**:
   - When navigating to `DetailRoute(applicationId)`, the screen immediately displays an `ErrorBannerWidget` with `"Error: Application not found"`.
   - **Root Cause**:
     1. In `ui/kmp/features/job-application-details/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationdetails/data/source/impl/RemoteJobApplicationDetailsDataSourceImpl.kt`:
        ```kotlin
        override suspend fun getJobApplication(id: String): JobApplication? = null
        override suspend fun getCompanies(): List<Company> = emptyList()
        ```
        `RemoteJobApplicationDetailsDataSourceImpl` was left as a stub returning `null` for `getJobApplication(id)` and `emptyList()` for `getCompanies()`.
     2. In `JobApplicationDetailsDataModule.kt`, dependencies were declared with `singleOf(::RemoteJobApplicationDetailsDataSourceImpl)` and `singleOf(::LocalJobApplicationDetailsDataSourceImpl)`. In `ui/kmp/core/data/di/CoreDataModule.kt`, the remote client is registered as `RemoteDataSource` (not nullable) and the database container is `DatabaseHolder` (not nullable). Because constructor parameters were defined as nullable without explicit DI injection (`get()`), Koin resolved them with null values, so neither `RemoteDataSource` nor `DatabaseHolder` was supplied to the data sources!
     3. When an application is clicked from the list, `JobApplicationDetailsRepositoryImpl` checks `localDataSource.getJobApplication(id)` (which looks in Room SQLite DAO or memory cache). If the application hasn't been cached locally yet, it queries `remoteDataSource.getJobApplication(id)` which returned `null`, causing `JobApplicationDetailViewModel.handleLoad(id)` to immediately fail with `"Application not found"`.

2. **Navigation Stuck in a Loop / Unable to Leave Broken Details Page**:
   - In `JobApplicationsListViewModel`, processing `SelectApplication(id)` sets `_uiState.value = JobApplicationsListUiState.NavigateToDetail(applicationId = id)`.
   - In `JobApplicationsListScreen.kt`:
     ```kotlin
     is JobApplicationsListUiState.NavigateToDetail -> {
         LaunchedEffect(s) {
             onApplicationClick(s.applicationId)
         }
         ...
     }
     ```
     `JobApplicationsListViewModel` never reset or shifted its `_uiState` back to `Content`!
     When the user on the Details screen presses the top bar Back arrow (`navigator.popBackStack()`) or the hardware back button, the top entry `DetailRoute` is popped off `Navigator.backStack`, returning to `ListRoute`.
     Because `JobApplicationsListViewModel.uiState` is STILL `JobApplicationsListUiState.NavigateToDetail`, as soon as `JobApplicationsListScreen` recomposes upon returning, its `LaunchedEffect(s)` re-executes and immediately calls `onApplicationClick(s.applicationId)`, navigating straight back into `DetailRoute`! This creates the inescapable navigation loop.
   - Additionally:
     - `JobApplicationDetailsScreen.kt` has an `ErrorBannerWidget` when in `Error` state, but provides no option or button to go back directly from the error banner (only "Retry", which immediately retries and fails again).
     - Hardware back handling (`BackHandler`) in `JobApplicationDetailsScreen`: `BackHandler` is available in `core:navigation` but was not wired into `JobApplicationDetailsScreen` to dispatch back navigation on Android.

---

## 2. Architecture & Design Principles

### 2.1 Full Details Data Pipeline Resolution (No Nulls In DI)
Connect `features/job-application-details/data` to live `RemoteDataSource` and `DatabaseHolder`:
1. **Eliminate Null Injection & Enforce Live Dependencies**:
   - `RemoteJobApplicationDetailsDataSourceImpl`:
     Change constructor from `(remoteDataSource: RemoteDataSource? = null)` to non-null `(remoteDataSource: RemoteDataSource)`.
   - `LocalJobApplicationDetailsDataSourceImpl`:
     Change constructor to accept `(databaseHolder: DatabaseHolder)` directly.
   - `JobApplicationDetailsDataModule`:
     Register with explicit dependencies:
     ```kotlin
     public val jobApplicationDetailsDataModule: Module = module {
         single<RemoteJobApplicationDetailsDataSource> {
             RemoteJobApplicationDetailsDataSourceImpl(remoteDataSource = get())
         }
         single<LocalJobApplicationDetailsDataSource> {
             LocalJobApplicationDetailsDataSourceImpl(databaseHolder = get())
         }
         single<NetworkJobApplicationDetailsDataSource> {
             NetworkJobApplicationDetailsDataSourceImpl(networkDataSource = get())
         }
         single<JobApplicationDetailsRepository> {
             JobApplicationDetailsRepositoryImpl(
                 remoteDataSource = get(),
                 networkDataSource = get(),
                 localDataSource = get(),
             )
         }
     }
     ```
2. **`RemoteJobApplicationDetailsDataSourceImpl` Implementation**:
   - `getJobApplication(id: String): JobApplication?`:
     - Fetch document from Firestore: `remoteDataSource.getDocument("applications", id)`.
     - If not found directly by ID, fall back to checking if `id` is a slug or UUID within `remoteDataSource.getCollection("applications")`.
     - Map `RemoteRecord` into `JobApplication` with all fields (role, company, status, channel, compensation, location, timeline, fitRating, fitEvaluation, notesRendered, timestamps).
   - `getCompanies(): List<Company>`:
     - Query `remoteDataSource.getCollection("companies")`.
     - Map records to `Company` domain objects (name, aliases, website, industry, logoUrl, etc.).
   - Implement real updates in `updateStatus`, `recordEvent`, `updateFitScore` via `remoteDataSource.updateDocument` / `setDocument`.
3. **Repository Fallback & Cache Sync**:
   - `JobApplicationDetailsRepositoryImpl.getJobApplication(id)`:
     - Query `localDataSource.getJobApplication(id)`.
     - If not found or empty, fetch from `remoteDataSource.getJobApplication(id)` and save to `localDataSource.saveJobApplication(remote)`.
   - Ensure `getCompanies()` also populates local cache.

### 2.2 Navigation Loop Fix & State Transition
1. **Shift State Upon Seeing Navigation Intent / Event**:
   - As soon as navigation is triggered (or seen by the screen), the UI state shifts back to what it should be if/when someone returns to this screen (`lastContent`).
   - In `JobApplicationsListScreen.kt`, for all navigation states (`NavigateToDetail`, `NavigateToAddApplication`, `NavigateToAddLead`, `NavigateToEdits`, `NavigateToSettings`), when `LaunchedEffect(s)` fires:
     ```kotlin
     is JobApplicationsListUiState.NavigateToDetail -> {
         LaunchedEffect(s) {
             onApplicationClick(s.applicationId)
             onIntent(JobApplicationsListUiIntent.ResetNavigation)
         }
         ...
     }
     ```
     `JobApplicationsListViewModel` handles `ResetNavigation` by setting `_uiState.value = lastContentState`. When the user pops back from details, the list screen is sitting happily in `Content` and does NOT loop.
2. **Scaffolding Invariant (Single Chrome Layer)**:
   - Verify that `JobApplicationDetailsScreen.kt` does **NOT** instantiate its own `Scaffold` or duplicate `TopAppBar`.
   - `AppScaffold` in `apps:core` already provides the outer responsive scaffold, top bar, and back arrow (`<-`) when `currentDestination` is `DetailRoute`.
   - `JobApplicationDetailsScreen.kt` stays a clean content composable (`Box` / `Column`) without nested scaffolds or duplicate headers.
3. **Details Page Back & Hardware Navigation**:
   - In `JobApplicationDetailsScreen.kt`:
     - Add `BackHandler(enabled = true) { onNavigateBack() }` from `core:navigation` so Android hardware back button and back gesture always pop `DetailRoute`.
     - In `JobApplicationDetailUiState.Error`: Provide a secondary "Go Back" action button (`onBack = onNavigateBack` / `onIntent(JobApplicationDetailUiIntent.ClickBack)`), ensuring the user is never trapped in an error box.
4. **Scaffold & In-UI Up Navigation**:
   - `AppScaffold` top bar Back button invokes `navigator.popBackStack()`. When `DetailRoute` is active, `canNavigateBack` evaluates to `true`. With the state loop resolved, `popBackStack()` smoothly pops to `ListRoute`.

---

## 3. Proposed Changes

### Component 1: `features:job-application-details:data`

#### [MODIFY] `JobApplicationDetailsDataModule.kt`
- Explicitly wire dependencies `get()` for `RemoteDataSource`, `DatabaseHolder`, and `NetworkDataSource`. Remove null injection.

#### [MODIFY] `RemoteJobApplicationDetailsDataSourceImpl.kt`
- Take non-null `RemoteDataSource`.
- Implement `getJobApplication(id)` querying `remoteDataSource.getDocument("applications", id)`.
- Map `RemoteRecord` to `JobApplication`.
- Implement `getCompanies()` querying `remoteDataSource.getCollection("companies")`.
- Implement status update, fit score update, and event recording.

#### [MODIFY] `LocalJobApplicationDetailsDataSourceImpl.kt`
- Take non-null `DatabaseHolder` and query Room DAO or fallback to memory cache.

---

### Component 2: `features:job-applications-list:ui`

#### [MODIFY] `JobApplicationsListUiIntent.kt`
- Add `public data object ResetNavigation : JobApplicationsListUiIntent`.

#### [MODIFY] `JobApplicationsListViewModel.kt`
- Handle `ResetNavigation` by updating `_uiState.value` to the cached `Content` state.

#### [MODIFY] `JobApplicationsListScreen.kt`
- In `LaunchedEffect(s)` for `NavigateToDetail` and other navigation states, invoke `onIntent(JobApplicationsListUiIntent.ResetNavigation)` immediately after dispatching the navigation callback.

---

### Component 3: `features:job-application-details:ui`

#### [MODIFY] `JobApplicationDetailsScreen.kt`
- Verify no nested `Scaffold` or duplicate toolbar exists.
- Add `BackHandler(enabled = true) { onNavigateBack() }`.
- In `JobApplicationDetailUiState.Error`, add a "Go Back" button alongside "Retry" using `onNavigateBack()`.

---

### Component 4: `mise` Task Automation

#### [MODIFY] `.config/mise/tasks/ui/kmp/test.sh`
- Expose parameterized test execution by forwarding CLI arguments (`"$@"`) to Gradle:
  ```sh
  #!/usr/bin/env sh
  #MISE description="Run KMP unit tests across shared modules"
  set -eu
  cd ui/kmp
  if [ "$#" -gt 0 ]; then
    ./gradlew "$@"
  else
    ./gradlew jvmTest
  fi
  ```

---

## 4. Verification Plan

### Automated Tests
Run via `mise`:
1. **KMP Test Suite**:
   ```bash
   mise run ui:kmp:test
   # Or targeting individual modules:
   mise run ui:kmp:test -- :features:job-application-details:ui:jvmTest
   mise run ui:kmp:test -- :features:job-application-details:data:jvmTest
   mise run ui:kmp:test -- :features:job-application-details:domain:jvmTest
   mise run ui:kmp:test -- :features:job-applications-list:ui:jvmTest
   ```
2. **Konsist Architecture Invariants**:
   ```bash
   mise run dev:konsist
   ```
3. **Detekt Static Analysis**:
   ```bash
   mise run ui:kmp:detekt
   ```
4. **Android Build**:
   ```bash
   mise run ui:kmp:android:assemble
   ```

### Manual Verification
1. **Details Screen Load**:
   - Open app -> Tap any application in the list.
   - Verify that the full details load (Company, Role, Status, Fit Score, Timeline, Artifacts) instead of the red error box.
2. **In-UI Up Navigation & Backstack**:
   - Tap top-bar Back arrow (`<-`). Verify it navigates back to the list cleanly and does not loop back into details.
3. **Hardware Back Button**:
   - Tap an application, then press device Back (or Android back gesture). Verify it returns to the list screen.
4. **No Double Scaffolding**:
   - Verify there is exactly one top app bar with one title and back arrow.
5. **Error Banner Recovery**:
   - If an error state occurs, verify that "Go Back" is present and returns the user to safety.
