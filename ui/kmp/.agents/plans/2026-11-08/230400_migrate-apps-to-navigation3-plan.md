# Implementation Plan: Migrate `ui/kmp` Navigation to Navigation 3

**Status:** Completed  
**Date:** 2026-11-08  
**Target:** `ui/kmp`  
**Model:** Gemini 3.8 Flash  
**Session:** 7119f97b-15f3-4a20-8cf2-b66898a0dd22  
**Depends On:** [`160417_thinning-apps-core-and-core-navigation.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/.agents/plans/2026-11-05/160417_thinning-apps-core-and-core-navigation.md)  

---

## 1. Goal Description & Background Context

The current `ui/kmp` navigation implementation relies on legacy Jetpack Navigation 2 (`androidx.navigation:navigation-compose`, `NavHostController`, `NavHost`, and `composable<T>`).

With Android's release of **Jetpack Navigation 3** (and its support in Compose Multiplatform), navigation moves from a black-box, library-managed controller with string/reflection-based route graphs to:
1. **User-owned Back Stack**: Explicit, snapshot-state-driven `NavBackStack<NavKey>` (created and remembered via `rememberNavBackStack`), providing full control over history, deep links, multi-stack management, and transitions.
2. **First-class Compose Display (`NavDisplay`)**: Replaces `NavHost` with `NavDisplay`, taking the active `backStack`, an `onBack` handler, and an `EntryProvider` (`entryProvider { ... }`).
3. **Decoupled Route & Feature Entry Contracts**: Each feature defines its destination keys (`@Serializable class/object : NavKey`) and registers an `entry<Key> { key -> ... }` into the `EntryProviderScope`.

Using the `android` CLI documentation and Android knowledge base (`kb://android/guide/navigation/navigation-3/migration-guide`, `kb://JetBrains/kotlin-multiplatform-dev-docs/topics/compose/compose-navigation-3`, and Navigation 3 skill recipes), this plan defines the complete migration of `ui/kmp/apps/*` and `ui/kmp/core/navigation` to **Navigation 3**.

---

## 2. Architecture & Design Principles

### 2.1 Navigation 3 Multiplatform Route Invariant
- In Navigation 3, all routes must implement `androidx.navigation3.runtime.NavKey` and be annotated with `@Serializable`.
- In `core/navigation`, we redefine [`NavKey.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/core/navigation/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/navigation/NavKey.kt):
  ```kotlin
  package dev.fnc314.tools.jobtracker.core.navigation

  import androidx.navigation3.runtime.NavKey as AndroidxNavKey
  import org.jetbrains.compose.resources.StringResource

  /**
   * App-wide typed destination route contract extending Navigation 3 NavKey.
   */
  public interface NavKey : AndroidxNavKey {
      public val titleRes: StringResource
  }
  ```
- Because all existing route objects and data classes (`ListRoute`, `DetailRoute`, `AddApplicationRoute`, `AddLeadRoute`, `EditsRoute`, `SettingsRoute`, `SignInRoute`) already implement `dev.fnc314.tools.jobtracker.core.navigation.NavKey` and have `@Serializable`, they immediately conform to Navigation 3 destination keys!

### 2.2 Decoupled Feature Navigation Hooks
- Replace legacy `FeatureNavigationHook` (which took `NavGraphBuilder` and `NavHostController`) with an `EntryProviderScope` contribution:
  ```kotlin
  package dev.fnc314.tools.jobtracker.core.navigation

  import androidx.navigation3.runtime.EntryProviderScope

  public fun interface FeatureNavigationHook {
      public operator fun invoke(
          scope: EntryProviderScope<NavKey>,
          navigator: Navigator,
      )
  }
  ```
- In each feature UI module (`features/<feature>/ui`), `entry<Route> { ... }` registers into the `EntryProviderScope` directly.

### 2.3 `Navigator` & App State Management
- Define a lightweight, clean `Navigator` class in `core/navigation` wrapping `NavBackStack<NavKey>`:
  ```kotlin
  class Navigator(
      public val backStack: NavBackStack<NavKey>,
  ) {
      public fun navigate(key: NavKey) {
          backStack.add(key)
      }

      public fun popBackStack(): Boolean {
          if (backStack.size > 1) {
              backStack.removeLastOrNull()
              return true
          }
          return false
      }

      public fun popToRoot() {
          if (backStack.size > 1) {
              val root = backStack.first()
              backStack.clear()
              backStack.add(root)
          }
      }

      public fun switchRoot(key: NavKey) {
          backStack.clear()
          backStack.add(key)
      }
  }
  ```
- In `AppScaffold.kt`:
  - Determine `currentDestination` by observing `navigator.backStack.lastOrNull()`.
  - `canNavigateBack` is simply `navigator.backStack.size > 1 && currentDestination != ListRoute`.
  - Active tab selection in bottom bar and nav rail matches `currentDestination == destination.route`.
  - Back navigation from TopAppBar or back gesture invokes `navigator.popBackStack()`.
- In `App.kt`:
  - `val backStack = rememberNavBackStack(if (isAuthenticated) ListRoute else SignInRoute)`
  - `val navigator = remember(backStack) { Navigator(backStack) }`
  - Replaces `NavHost` with `NavDisplay(backStack = navigator.backStack, onBack = { navigator.popBackStack() }, entryProvider = entryProvider { ... })`.

### 2.4 Dependency & Convention Plugin Updates
- Update `libs.versions.toml`:
  - Ensure `navigation3-runtime` and `navigation3-ui` dependencies are bundled in `navigation3`.
  - Update `NavigationDependenciesConventionPlugin.kt` to depend on `CatalogTypes.Bundles.NAVIGATION3` instead of `NAVIGATION_COMPOSE`.
  - Remove all legacy `androidx.navigation:navigation-compose` dependencies once migration is complete.

---

## 3. User Review Required

> [!IMPORTANT]
> **Complete Atomic Migration of Navigation 2 to Navigation 3**:
> This migrates all 6 UI feature navigation hooks and the root app scaffold simultaneously to ensure zero mixed-mode runtime state, matching the Google Navigation 3 migration guide prerequisites.

---

## 4. Proposed Changes

### Component 1: Convention Plugins & Dependency Catalog (`gradle/tools/project-plugins`, `libs.versions.toml`)

#### [MODIFY] [`libs.versions.toml`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/gradle/libs.versions.toml)
- Verify `navigation3-runtime` and `navigation3-ui` artifacts in `navigation3` bundle.

#### [MODIFY] [`NavigationDependenciesConventionPlugin.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/gradle/tools/project-plugins/src/main/kotlin/dev/fnc314/tools/jobtracker/kmp/gradle/plugins/config/NavigationDependenciesConventionPlugin.kt)
- Replace `CatalogTypes.Libraries.NAVIGATION_COMPOSE(project)` with `CatalogTypes.Bundles.NAVIGATION3(project)`.

---

### Component 2: Core Navigation Contracts (`core:navigation`)

#### [MODIFY] [`NavKey.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/core/navigation/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/navigation/NavKey.kt)
- Inherit from `androidx.navigation3.runtime.NavKey`.

#### [NEW] [`Navigator.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/core/navigation/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/navigation/Navigator.kt)
- Provide type-safe `Navigator` managing `NavBackStack<NavKey>`.

#### [MODIFY] [`FeatureNavigationHook.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/core/navigation/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/navigation/FeatureNavigationHook.kt)
- Define `FeatureNavigationHook` as `fun interface FeatureNavigationHook { operator fun invoke(scope: EntryProviderScope<NavKey>, navigator: Navigator) }`.

---

### Component 3: Feature Navigation Hooks (`features/*/ui`)

#### [MODIFY] [`JobApplicationsListNavigation.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/JobApplicationsListNavigation.kt)
- Use `entry<ListRoute> { ... }`.

#### [MODIFY] [`JobApplicationDetailsNavigation.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-application-details/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationdetails/ui/JobApplicationDetailsNavigation.kt)
- Use `entry<DetailRoute> { key -> JobApplicationDetailsScreen(applicationId = key.applicationId, ...) }`.

#### [MODIFY] [`AddJobApplicationNavigation.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/add-job-application/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/ui/AddJobApplicationNavigation.kt)
- Use `entry<AddApplicationRoute>` and `entry<AddLeadRoute>`.

#### [MODIFY] [`SettingsNavigation.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/settings/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/settings/ui/SettingsNavigation.kt)
- Use `entry<SettingsRoute>`.

#### [MODIFY] [`SignInNavigation.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/sign-in/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/signin/ui/SignInNavigation.kt)
- Use `entry<SignInRoute>`.

#### [MODIFY] [`TrackerEditsNavigation.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/tracker-edits/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/trackeredits/ui/TrackerEditsNavigation.kt)
- Use `entry<EditsRoute>`.

---

### Component 4: App Root & Scaffold (`apps:core`)

#### [MODIFY] [`AppScaffold.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/ui/AppScaffold.kt)
- Replace `NavHostController` with `Navigator`.
- Observe current active destination directly from `navigator.backStack.lastOrNull()`.
- Bottom navigation and navigation rail click triggers `navigator.navigate(destination.route)`.
- Top bar title directly reads `currentDestination.titleRes`.

#### [MODIFY] [`App.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/App.kt)
- Replace `rememberNavController()` and `NavHost` with `rememberNavBackStack` and `NavDisplay`.
- Assemble all feature navigation hooks using `entryProvider { ... }`.
- Handle auth state transitions via `navigator.switchRoot(ListRoute)` or `navigator.switchRoot(SignInRoute)`.

---

## 5. Verification Plan

### Automated Tests
1. **Unit & Konsist Architecture Tests**:
   ```bash
   mise run :ui:kmp:test
   ```
   Verify all unit tests pass, Konsist checks pass, and view model / navigation invariants hold.
2. **Detekt Static Analysis**:
   ```bash
   mise run :ui:kmp:detekt
   ```
   Verify 0 static analysis errors or warnings.
3. **Android Debug Build**:
   ```bash
   mise run :ui:kmp:android:assemble
   ```
   Verify `BUILD SUCCESSFUL` with 0 warnings.
4. **Android CLI Inspection**:
   Use `android layout` or `android screen capture` if a device/emulator is running to inspect the rendered Navigation 3 tree.

### Manual Verification
- Verify navigation between Applications, Pending Edits, and Settings tabs in bottom navigation bar.
- Verify pushing into Application Details (`DetailRoute`), Add Application, and Add Lead.
- Verify back button navigation (both TopAppBar back arrow and system back button).
