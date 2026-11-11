# Implementation Plan: App Navigator Refactoring

**Status:** Completed  
**Date:** 2026-11-11  
**Target:** `frontend`  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261111-111905-1bv5  
**Supersedes:** [`root-scaffold-and-navigation-enhancement-plan.md`](./2026-11-05/160417_root-scaffold-and-navigation-enhancement-plan.md)  

---

## 1. Executive Summary & Goals

Refactor the navigation layer to replace the callback-heavy `FeatureNavigationHook` pattern with a clean `AppNavigator` interface. Feature screens navigate declaratively via `CompositionLocal` injection rather than receiving navigation callbacks through composable parameters. Type-safe navigation IDs (`@JvmInline` value classes), minimized route input parameters (IDs only), consistent `JobApplication*` route naming, and enum-based bottom nav selection complete the refactoring.

### Goals
- Eliminate deep callback chains through composable parameters
- Provide type-safe navigation via `AppNavigator` interface
- Prevent wrong ID types via `ApplicationId` and `CompanyId` value classes
- Decouple navigation from display data by minimizing route params to IDs only
- Use finite `TopLevelDestinationEnum` for type-safe bottom nav selection
- Ensure all `*Impl` classes satisfy Konsist architecture tests

## 2. Architecture & Design Principles

### Current State (Before)
`App.kt` used `entryProvider` with 8 `*NavHook` functions. Each hook factory returned a `FeatureNavigationHook` (fun interface) that registered entries and received navigation callbacks (e.g., `onApplicationClick`, `onCompanyClick`, `onNavigateBack`) as lambda parameters, creating a deep callback chain through all composables.

### Key Decisions
1. **AppNavigator as public interface** — Defined in `core/navigation`, implemented as `internal class AppNavigatorImpl` in `apps/core/impl/` to satisfy Konsist rules. Provided via `CompositionLocal` (`LocalAppNavigator`) because it depends on `Navigator` which is composable-scoped.
2. **FeatureEntryRegistry replaces FeatureNavigationHook** — `core/navigation` defines the interface with `register(scope: EntryProviderScope)`. Each feature implements and registers as a Koin `single`. `App.kt` collects all registries from Koin.
3. **JvmInline value classes** — `ApplicationId` and `CompanyId` provide type safety. Only these two IDs are used in navigation routes; other IDs (e.g., `ContactId`, `EmailId`) exist in the data layer but never cross navigation boundaries.
4. **Route minimal input principle** — Each route accepts only the minimal ID. ViewModels load display data from repositories:
   - `CompanyDetailsRoute`: Only `companyId` (removed `companyName`, `website`)
   - `DetailRoute`: Only `applicationId` (removed `roleTitle`)
5. **TopLevelDestinationEnum** — Simple finite enum (Applications, Companies, Edits, Settings) without icons/resources since core can't depend on feature modules.
6. **Consistent route naming** — All app routes prefixed with `JobApplication*`; `SignInRoute` remains unchanged as a generic auth destination.

## 3. Proposed Changes & Implementation Steps

### Step 1: core/navigation — New Types
- Created `AppNavigator.kt` — navigation interface with `navigateToApplicationDetails`, `navigateToCompanyDetails`, `navigateTo`, `navigateToSettings`, `login`, `logout`, `popBackStack`, and generic `navigate(NavKey)`
- Created `NavigationIds.kt` — `@JvmInline value class ApplicationId` and `CompanyId`
- Created `FeatureEntryRegistry.kt` — interface replacing `FeatureNavigationHook`
- Created `TopLevelDestinationEnum.kt` — simple enum (Applications, Companies, Edits, Settings)
- Deleted `FeatureNavigationHook.kt`

### Step 2: apps/core — AppNavigator Implementation
- Created `apps/core/impl/AppNavigatorImpl.kt` (internal, in `.impl` package per Konsist rules)
- Created `LocalAppNavigator.kt` — CompositionLocal for providing AppNavigator to all descendant composables

### Step 3: All 8 Features Refactored
- Renamed all routes with `JobApplication*` prefix
- Simplified `CompanyDetailsRoute` to accept only `companyId`
- Simplified `DetailRoute` to accept only `applicationId`
- Updated `CompanyCard.onCompanyClick` callback to accept only `companyId`
- Created `*EntryRegistry` for each feature replacing `*NavHook`
- Updated all 8 feature DI modules to register `single<FeatureEntryRegistry>`

| Current | New | Notes |
|---------|-----|-------|
| `ListRoute` | `JobApplicationListRoute` | Object route |
| `CompaniesListRoute` | `JobApplicationCompaniesRoute` | Object route |
| `CompanyDetailsRoute(companyId, companyName, website)` | `JobApplicationCompanyDetailsRoute(companyId)` | Removed `companyName`, `website` |
| `DetailRoute(applicationId, roleTitle)` | `JobApplicationDetailRoute(applicationId)` | Removed `roleTitle` |
| `SettingsRoute` | `JobApplicationSettingsRoute` | Object route |
| `EditsRoute` | `JobApplicationEditsRoute` | Object route |
| `AddApplicationRoute` | `JobApplicationAddApplicationRoute` | Object route |
| `AddLeadRoute` | `JobApplicationAddLeadRoute` | Object route |
| `SignInRoute` | `SignInRoute` | Unchanged |

### Step 4: App.kt and AppScaffold Rewrite
- `App.kt`: replaced all `*NavHook` calls with `koinInject<List<FeatureEntryRegistry>>()` iteration, updated `SavedStateConfiguration` with renamed routes
- `AppScaffold.kt`: replaced `is ListRoute` type checks with `TopLevelDestinationEnum` matching, uses `LocalAppNavigator.current` for navigation
- Deleted duplicate `SettingsUiModule.kt`

## 4. Verification Plan & Testing Checklist

- [ ] `frontend:build` compiles successfully after all changes
- [ ] `frontend:detekt` passes with no new violations
- [ ] `frontend:konsist` passes (especially `*Impl` in `.impl` package rule)
- [ ] Navigation flow: Application List → Application Details → Company Details → back → Application Details
- [ ] Bottom nav bar selection state uses `TopLevelDestinationEnum` correctly
- [ ] `login()` and `logout()` clear the backstack and switch roots correctly
- [ ] `AppNavigator` is injectable in feature screens via `LocalAppNavigator.current`
- [ ] `CompanyDetailsScreen` loads company data correctly with `companyId`-only route
- [ ] `SavedStateConfiguration` uses renamed route class names
- [ ] `frontend:test` passes — existing ViewModel tests pass without changes

### Edge Cases
- Back navigation from SignInRoute should not return to app screens (backstack cleared on login/logout)
- Company details with blank `companyName` should show generic title
- `AppNavigator.popBackStack()` should be a no-op when backstack size is 1
- Top-level navigation to current destination should be a no-op

### Risks
- **Backstack serialization:** Renaming routes may break saved backstack state. Users may see a navigation reset.
- **CompanyDetailsRoute title:** Removing `companyName` means `AppScaffold` shows generic "Company Details" until the ViewModel loads.
- **FeatureEntryRegistry ordering:** Entry registration order may matter if routes conflict.

## 5. Bookkeeping
- [x] Plan persisted at `frontend/.agents/plans/2026-11-11/114200_app-navigator-refactoring-plan.md`
- [x] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
