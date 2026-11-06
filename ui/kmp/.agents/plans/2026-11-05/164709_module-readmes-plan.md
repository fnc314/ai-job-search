# Implementation Plan: Module README.md Documentation Standard

**Status:** Completed  
**Date:** 2026-11-05  
**Target:** ui/kmp:core, ui/kmp:features  

---

## Goal Description
Establish clear, comprehensive, and consistent `README.md` documentation for all contributing modules under `core/` and `features/` within the `ui/kmp` project. These documents will explain the architectural purpose of each module, how it integrates into the overarching multiplatform application, its public API surface, deep-link capabilities, and significant technical implementation details.

---

## Scope & Target Locations

Per requirements, documentation must exist at the project level for each module under `core/` and `features/`. Multi-project features (which have `domain`, `data`, and `ui` subprojects) will have a single consolidated `README.md` at the feature root (e.g. `features/sign-in/README.md`) rather than fragmented readmes inside each subproject:

| Category | Target Path | Subprojects Covered |
| :--- | :--- | :--- |
| **Core** | `core/data/README.md` | Single module (`:core-data`) |
| **Core** | `core/design-system/README.md` | Single module (`:core-design-system`) |
| **Core** | `core/network/README.md` | Single module (`:core-network`) |
| **Core** | `core/widgets/README.md` | Single module (`:core-widgets`) |
| **Feature** | `features/sign-in/README.md` | `:features-sign-in-domain`, `:features-sign-in-data`, `:features-sign-in-ui` |
| **Feature** | `features/job-applications-list/README.md` | `:features-job-applications-list-domain`, `:features-job-applications-list-data`, `:features-job-applications-list-ui` |
| **Feature** | `features/job-applications-details/README.md` | `:features-job-applications-details-domain`, `:features-job-applications-details-data`, `:features-job-applications-details-ui` |
| **Feature** | `features/settings/README.md` | `:features-settings-domain`, `:features-settings-data`, `:features-settings-ui` |
| **Feature** | `features/add-job-application/README.md` | `:features-add-job-application-domain`, `:features-add-job-application-data`, `:features-add-job-application-ui` |
| **Feature** | `features/tracker-edits/README.md` | `:features-tracker-edits-domain`, `:features-tracker-edits-data`, `:features-tracker-edits-ui` |

---

## Standardized README Structure

To ensure consistency, readability, and immediate utility for both developers and future AI agents, each `README.md` will strictly follow this layout:

1. **Quick Facts for AI Agents & Developers** (Metadata card):
   - **Subproject(s)**: e.g. `:features-job-applications-list-domain`, `:features-job-applications-list-data`, `:features-job-applications-list-ui`
   - **Namespace / Package Root**: e.g. `dev.fnc314.tools.jobtracker.features.jobapplicationslist`
   - **Public Entry Point(s)**: Screen composables, Koin module, primary Use Cases
   - **Key Test/Build Command**: e.g. `./gradlew :features-job-applications-list-ui:compileCommonMainKotlinMetadata`
2. **Title & Module Overview**: High-level summary of what lives in the module.
3. **Contribution to the Larger Application**: Architectural role within the KMP application, upstream consumers, and downstream dependencies.
4. **Module Architecture & Subprojects** (primarily for features): Breakdown of the clean-architecture tiers (`domain`, `data`, `ui`) or functional packages.
5. **Public API Surface & Significant Contributions**:
   - Explicit `public` classes, composables, use cases, models, and Koin modules.
   - Internal invariants (strict API mode compliance, encapsulation of implementation details).
6. **Key Gradle & Mise Tasks**:
   - Specific compilation and test tasks for the module.
   - Root Gradle and Mise tasks that execute or assemble the module.
7. **Deep-Link & Navigation Integration**:
   - Navigation routes supported (e.g., `DetailRoute`, `ListRoute`, `SettingsRoute`).
   - Deep-linking capabilities (URI patterns, arguments, back-stack behavior, external URL handling).
8. **Convention Plugins & Dependencies**:
   - Local Gradle convention plugins applied (`local.kotlin.multiplatform`, `local.android.kmp.library`, `local.compose`, etc.).
   - Key runtime dependencies (Compose M3, Koin, Ktor, kotlinx-serialization).

---

## Design System Invariants & Dependency Obscuration (Option A Confirmed)

To uphold a clean design system architecture and eliminate fragmented styling:
1. **Zero Rogue `Color()` Calls**:
   - Absolutely no `Color(...)` constructor invocations are permitted anywhere outside `core/design-system/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/designsystem/Color.kt`.
   - Centralize hex parsing (`String.toColorOrNull()`) and hex serialization (`Color.toHexString()`) into `Color.kt`.
   - Expose canonical seed colors and default hex constants in `SeedColors` / `AppColors`.
   - Remove ad-hoc hex parsing from `apps/core/App.kt` and `features/settings/ui/SettingsScreen.kt`.
2. **Zero Widgets in `core/design-system`**:
   - `core/design-system` contains strictly tokens, themes, typography, shapes, colors, spacing, dimensions, and window size classes.
   - It contains NO composable widgets.
3. **Universal Widget Layering via `core/widgets` (Option A)**:
   - `core/widgets/build.gradle.kts` uses `api(projects.coreDesignSystem)`.
   - Under this pattern, `core:widgets` acts as the single universal UI entry point for all feature modules.
   - All feature modules (`features/*/ui`) depend **only** on `projects.coreWidgets`, completely removing direct `projects.coreDesignSystem` declarations from their `build.gradle.kts`.
   - Only `core/widgets` and `apps/core` (for the top-level application `JobTrackerTheme` wrapper) directly reference `projects.coreDesignSystem`.

---

## Root `ui/kmp/AGENTS.md` & Pre-Commit Hook Invariant

To ensure future agents have instant access to critical information upon assessment and to mandate README maintenance:

### 1. `ui/kmp/AGENTS.md` (NEW)
Create [`ui/kmp/AGENTS.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/AGENTS.md) as the canonical AI guidelines document for the Kotlin Multiplatform workspace, detailing:
- **Workspace Architecture Map**: Overview of `core/`, `features/`, `apps/`, and `gradle/tools/project-plugins`.
- **Mandatory Maintenance Rule**: Whenever changes are made to a module's public API surface, internal routing, dependencies, or domain logic, the agent MUST update the corresponding module `README.md`.
- **Design System Invariant**: No direct `Color()` constructor calls outside `core/design-system`. All widgets live in `core/widgets`.
- **Gradle Execution Rules**: Always invoke Gradle with Mise context or `./gradlew` using `BypassSandbox: true` due to the custom Java SDK path (`26.0.2+101`).
- **Explicit API Mode Rule**: All Kotlin code enforces explicit API mode (`explicitApi()`), requiring explicit `public`/`internal` visibility and explicit return types on all declarations.
- **Convention Plugins Invariant**: All subprojects must apply plugins via `libs.plugins.local.*` and never define ad-hoc target configurations.

### 2. Mise Pre-Commit Hook Integration
- Add task `"ui:kmp:docs:readmes"` to `mise.toml` running `bash scripts/check-kmp-readmes.sh`.
- Update `.git/hooks/pre-commit` (configured via mise) to execute `mise run ui:kmp:docs:readmes` alongside `mise run mise:docs:tasks`, ensuring that whenever files in `core/` or `features/` are committed, documentation validity and presence is strictly validated before commit.

---

## Detailed Content Breakdown per Module

### 1. `core/data/README.md`
- **Contents**: Domain entities (`JobApplication`, `TrackerEvent`, `TrackerArtifact`, `TrackerEdit`, `Company`), 11 encapsulated enums (`TrackerStatus`, `ApplicationChannel`, etc.), repository interfaces (`JobApplicationRepository`, `StorageRepository`), Firestore multiplatform repository implementations, settings persistence models, and timestamp formatters.
- **Contribution**: The foundational data and domain layer contract for the entire application. Provides type-safe models, Firestore serializations, and repository contracts consumed by all feature domain modules and the core app runner.
- **Significant Contributions**:
  - `Factory`: Centralized creation factory for applications, events, and artifacts.
  - `JobApplicationRepository`, `StorageRepository`: Core storage interfaces.
  - Encapsulated enums with self-contained styling (`containerColor()`, `contentColor()`), display strings (`displayName()`), and Firestore transformations (`toFirestore()`, `fromFirestore()`).
  - `PlatformFileOpener`: Platform expect/actual for launching system PDF/file viewers.
- **Deep-Link Support**: None (data-only foundation).

### 2. `core/design-system/README.md`
- **Contents**: Material 3 theme infrastructure, dynamic color palette (`SeedColors`, `LightColorScheme`, `DarkColorScheme`), typography specifications, responsive window size classification (`WindowSizeClass`, `WindowWidthSizeClass`), and adaptive dimension/spacing tokens.
- **Contribution**: Provides standard styling, theming, accessibility high-contrast support, and multiplatform window-size adaptations to all UI modules.
- **Significant Contributions**:
  - `JobTrackerTheme`: Master theme composable handling system dark mode, high contrast, and dynamic palette generation.
  - `ProvideWindowSizeClass` & `LocalWindowSizeClass`: Adaptive breakpoint infrastructure.
  - `AppSpacing`, `AppDimensions`, `AppShapes`, `AppTypography`.
- **Deep-Link Support**: None (theming and design foundation).

### 3. `core/network/README.md`
- **Contents**: Multiplatform Ktor client configurations and platform engine bindings (OkHttp on Android, CIO on JVM, Darwin on iOS, Js/Wasm on Web).
- **Contribution**: Encapsulates low-level HTTP networking, JSON serialization negotiation, logging, and engine dispatching.
- **Significant Contributions**:
  - `createPlatformHttpClient`: Platform-specific Ktor `HttpClient` engine builder.
  - `createJsonHttpClient`: Pre-configured JSON content-negotiated client.
- **Deep-Link Support**: None (networking foundation).

### 4. `core/widgets/README.md`
- **Contents**: Shared Compose presentation components that don't belong to a single feature.
- **Contribution**: Reusable, theme-aware UI widgets ensuring visual and interaction consistency across feature screens.
- **Significant Contributions**:
  - `StatusBadge`: Material 3 tonal badge rendering status color and label.
  - `FitBadge`: Algorithmic fit score indicator with color tiers (green/amber/red).
  - `NotesWidget`: Expandable notes view with semantic labels.
  - `TooltipIconButton`: Accessible action button with desktop hover / mobile long-press tooltip support.
- **Deep-Link Support**: None (widget library).

### 5. `features/sign-in/README.md`
- **Contents**: Sign-in presentation, state management, and authentication use-case definitions across `domain`, `data`, and `ui`.
- **Contribution**: Handles user authentication gate, session initialization, and unauthenticated redirects.
- **Significant Contributions**:
  - `signInFeatureModule` (combines `signInUiModule` and `signInDataModule`).
  - `SignInScreen`: Composable presentation for credentials/login.
  - `SignInViewModel`: Reactive authentication state flow.
- **Deep-Link Support**:
  - Route: `SignInRoute`.
  - Behavior: Fallback destination when unauthenticated; deep links to authenticated screens queue navigation after `SignInRoute` completes.

### 6. `features/job-applications-list/README.md`
- **Contents**: Primary application list dashboard, search, layered filtering, and column sorting across `domain`, `data`, and `ui`.
- **Contribution**: The default authenticated home screen allowing users to browse, search, and navigate into tracked job applications.
- **Significant Contributions**:
  - `jobApplicationsListFeatureModule`.
  - `JobApplicationsListScreen`: Responsive master list UI adapting across Compact, Medium, and Expanded window sizes.
  - `JobApplicationsListViewModel`: Manages search query, filter criteria, and sort order.
  - Domain Use Cases: `LoadAllJobApplicationsUseCase`, `FilterJobApplicationsUseCase`, `SortJobApplicationsUseCase`.
  - Internal components: `JobApplicationRow`, `FilterMenu`, `SortMenu`.
- **Deep-Link Support**:
  - Route: `ListRoute`.
  - URI pattern: `jobtracker://applications` or `/applications`.
  - Supports query filters (e.g. `?status=APPLIED&age=RECENT`).

### 7. `features/job-applications-details/README.md`
- **Contents**: Detailed application inspector, event timeline, generated artifact browser, status mutation dialogs, and company intelligence profiles across `domain`, `data`, and `ui`.
- **Contribution**: High-density detail screen for an individual job application, enabling interview prep, status progression, and artifact review.
- **Significant Contributions**:
  - `jobApplicationsDetailsFeatureModule`.
  - `JobApplicationsDetailsScreen`: Detail view taking `applicationId`.
  - `JobApplicationDetailViewModel`: Coordinates status updates, manual event recording, and fit score adjustments.
  - Domain Use Cases: `LoadJobApplicationUseCase`, `UpdateApplicationStatusUseCase`, `RecordEventUseCase`, `UpdateFitScoreUseCase`, `GetArtifactSignedUrlUseCase`, `LoadCompaniesUseCase`.
  - Internal components: `ArtifactsList`, `EventsTimeline`, `UpdateStatusDialog`, `RecordEventDialog`, `UpdateFitScoreDialog`, `MarkdownPreviewDialog`.
- **Deep-Link Support**:
  - Route: `DetailRoute(val applicationId: String)`.
  - URI pattern: `jobtracker://applications/{applicationId}` or `/applications/{applicationId}`.
  - Navigates directly to the target application with automatic back-stack fallback to `ListRoute`.

### 8. `features/settings/README.md`
- **Contents**: Application configuration, theme preferences, contrast switches, artifact storage backend selection, and developer options across `domain`, `data`, and `ui`.
- **Contribution**: Manages persistent user preferences reflected globally through `AppSettings`.
- **Significant Contributions**:
  - `settingsFeatureModule`.
  - `SettingsScreen`: Settings UI categorized into Appearance, Application Defaults, and Storage.
  - `SettingsViewModel`: StateFlow binding for reactive settings updates.
  - Domain Use Cases: `ObserveSettingsUseCase`, `UpdateSettingsUseCase`.
  - `SettingsRepository`: Interface and implementation for local key-value persistence.
- **Deep-Link Support**:
  - Route: `SettingsRoute`.
  - URI pattern: `jobtracker://settings` or `/settings`.

### 9. `features/add-job-application/README.md`
- **Contents**: Application creation forms (full application creation and fast lead intake) across `domain`, `data`, and `ui`.
- **Contribution**: Entry portal for recording new outbound applications or capturing inbound recruiter leads.
- **Significant Contributions**:
  - `addJobApplicationFeatureModule`.
  - `AddJobApplicationScreen` & `AddJobApplicationViewModel`: Full multi-field application entry form with validation.
  - `AddLeadScreen` & `AddLeadViewModel`: Quick-intake form for inbound contacts/leads.
  - Domain Use Cases: `CreateJobApplicationUseCase`, `CreateLeadUseCase`.
- **Deep-Link Support**:
  - Routes: `AddApplicationRoute`, `AddLeadRoute`.
  - URI patterns: `jobtracker://applications/new`, `jobtracker://leads/new`.

### 10. `features/tracker-edits/README.md`
- **Contents**: Governance and review screen for automated edits proposed by scraper bots or Gmail sync tasks across `domain`, `data`, and `ui`.
- **Contribution**: Safeguards the database by requiring human approval or rejection of AI-extracted application edits before applying them.
- **Significant Contributions**:
  - `trackerEditsFeatureModule`.
  - `TrackerEditsScreen`: Approval queue showing pending diffs, confidence scores, and source triggers.
  - `TrackerEditsViewModel`: Approval, rejection, and staleness expiration actions.
  - Domain Use Cases: `LoadPendingEditsUseCase`, `ApproveEditUseCase`, `RejectEditUseCase`, `ExpireStaleEditsUseCase`.
- **Deep-Link Support**:
  - Route: `EditsRoute`.
  - URI pattern: `jobtracker://edits` or `/edits`.

---

## User Review Required

> [!IMPORTANT]
> **Consolidation of Features vs. Subproject READMEs**:
> We are adhering to your instruction to place a single `README.md` at each feature's root (e.g. `features/sign-in/README.md`), covering its `domain`, `data`, and `ui` submodules in unified sections, rather than creating separate files in `features/sign-in/ui/`, etc.
>
> **Deep Linking Documentation**:
> We will document both the current type-safe Navigation Compose routes (`NavRoutes.kt`) and the standard URI patterns (`jobtracker://...`) supported or targeted for platform deep-linking and browser routing.

---

## Proposed Changes

### Documentation & Invariants
- [NEW] `ui/kmp/AGENTS.md`: Canonical rules for AI agents working in `ui/kmp`, outlining workspace layout, explicit API rules, key build tasks, and the README synchronization requirement.
- [NEW] `scripts/check-kmp-readmes.sh`: Script to verify that all modules under `core/` and `features/` have `README.md` files and validate documentation presence.
- [MODIFY] `mise.toml`: Add `ui:kmp:docs:readmes` task to run `scripts/check-kmp-readmes.sh`.
- [MODIFY] `.git/hooks/pre-commit`: Wire `mise run ui:kmp:docs:readmes` into pre-commit.

### Design System & Dependency Architecture Refinements
- [MODIFY] `core/design-system/.../Color.kt`: Centralize `String.toColorOrNull()` and `Color.toHexString()`; expose canonical seed hex constants.
- [MODIFY] `apps/core/.../App.kt`: Remove local `toColorOrNull()` implementation; import from `core/design-system`.
- [MODIFY] `features/settings/ui/.../SettingsScreen.kt`: Remove local `toHexString()` implementation; import from `core/design-system`.
- [MODIFY] `features/sign-in/ui/build.gradle.kts`: Replace direct `coreDesignSystem` with `coreWidgets`.
- [MODIFY] `features/*/ui/build.gradle.kts`: Remove redundant direct `coreDesignSystem` dependencies in favor of `coreWidgets` (`api(projects.coreDesignSystem)`).

### Core Modules Documentation
- [NEW] `core/data/README.md`
- [NEW] `core/design-system/README.md`
- [NEW] `core/network/README.md`
- [NEW] `core/widgets/README.md`

### Feature Modules Documentation
- [NEW] `features/sign-in/README.md`
- [NEW] `features/job-applications-list/README.md`
- [NEW] `features/job-applications-details/README.md`
- [NEW] `features/settings/README.md`
- [NEW] `features/add-job-application/README.md`
- [NEW] `features/tracker-edits/README.md`

---

## Verification Plan

### Automated Checks
1. Validate file creation and locations:
   ```zsh
   ls -la core/*/README.md features/*/README.md
   ```
2. Verify Gradle build and metadata compilation remain completely unaffected:
   ```zsh
   ./gradlew compileCommonMainKotlinMetadata
   ```
3. Verify Markdown links and references resolve properly against the codebase.
