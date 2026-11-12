# Module: `:frontend:core-widgets`

> [!NOTE]
> **Quick Facts for AI Agents & Developers**
> - **Subproject**: `:frontend:core-widgets`
> - **Package Root**: `dev.fnc314.tools.jobtracker.core.widgets`
> - **Primary Contributions**: Universal shared Compose components (`TopLevelCard`, `JobApplicationCard`, `CompanyLogoImage`, `StatusBadge`, `FitBadge`, `NotesWidget`, `TooltipIconButton`), Coil image loading subsystem (`JobApplicationTrackerImage`, `ProvideImageLoader`), re-exports `core:design-system`
> - **Architectural Invariant**: Acts as the single shared UI entry point for feature modules; re-exports `projects.coreDesignSystem` via `api(...)`. All image loading flows through `JobApplicationTrackerImage`.
> - **DI Module**: `coreWidgetsModule` registers `ImageLoadEventListener` for Coil observability.
> - **Key Build/Verification Command**: `./gradlew :frontend:core-widgets:compileCommonMainKotlinMetadata`

---

## 1. Module Overview
`:frontend:core-widgets` houses reusable, theme-aware Compose presentation components that are shared across multiple feature modules. It serves as the universal shared UI layer, eliminating duplicate widget implementations and standardizing micro-interactions, badge styling, and accessibility across all screens. It also owns the Coil image loading subsystem, providing a centralized `ImageLoader` configuration with observability hooks and a single wrapper composable (`JobApplicationTrackerImage`) for all image loading.

---

## 2. Contribution to the Larger Application
This module is the primary UI foundation consumed by all feature screens:
- **Upstream Consumers**: `features:job-applications-list`, `features:job-applications-details`, `features:settings`, `features:add-job-application`, `features:tracker-edits`, `features:sign-in`, and `apps:core`.
- **Downstream Dependencies**: `core:design-system` (exported via `api`), `core:data` (exported via `api`), Material 3.
- **Architectural Role (Option A)**: By exporting `core:design-system` as an `api` dependency, `:core-widgets` allows feature modules to depend **only** on `:core-widgets` while still accessing design tokens (`spacing`, `componentSizes`, `iconSizes`) without needing direct `core:design-system` dependencies in their build files.

---

## 3. Package & Subproject Structure

```
core/widgets/src/
├── commonMain/kotlin/dev/fnc314/tools/jobtracker/core/widgets/
│   ├── TopLevelCard.kt                 # Unified Grid-based adaptive card skeleton with actions & surface variants
│   ├── JobApplicationCard.kt           # Job application card built on TopLevelCard
│   ├── StatusBadge.kt                  # Visual tonal pill displaying application funnel status
│   ├── FitBadge.kt                     # Color-tiered fit rating pill (green / amber / neutral)
│   ├── NotesWidget.kt                  # Expandable / scrollable multi-note inspector
│   ├── TooltipIconButton.kt            # Accessible icon button with desktop hover & mobile tooltip
│   ├── CompanyLogoImage.kt             # Unified company logo with CompanyLogoSize, shimmer loading, & HTTP error banner
│   ├── JobApplicationTrackerImage.kt   # Centralized image loading wrapper (Coil)
│   ├── CoilImageLoaderProvider.kt      # App-level Coil singleton configurator
│   └── di/
│       └── CoreWidgetsModule.kt        # Koin DI module: ImageLoadEventListener + Coil HTTP error logging
└── androidMain/kotlin/                 # Android core KTX extensions
```

---

## 4. Public API Surface & Significant Contributions

- `TopLevelCard(title, modifier, onClick, contentDescription, onClickLabel, variant, isExpanded, onToggleExpand, showActionsWhenNotCollapsible, thumbnail, subtitle, compactSummary, wideTrailingHeader, wideBody, expandedContent, actions)`:
  - Unified responsive `Grid`-based card skeleton (`@file:OptIn(ExperimentalGridApi::class)`) used across `JobApplicationCard`, `CompanyCard`, and `EditCard`.
  - Automatically toggles collapsible `expandedContent` on compact viewports when hidden content exists, and acts as a direct navigation target when no content is hidden.
  - Gives equal-width (`1.fr`) treatment to action buttons via `TopLevelCardAction` and `TopLevelCardActionsGrid`, and supports surface/elevation variations via `TopLevelCardSurfaceVariant` (`Surface`, `Elevated`, `FilledTonal`, `Highlighted`).
- `CompanyLogoImage(companyName, modifier, logoUrl, companyInitial, logoSize, size, shape, contentScale, textStyle)`:
  - Standardized across all use-sites via `CompanyLogoSize` (`Small` = `48.dp`, `Medium` = `64.dp`, `Large` = `72.dp`).
  - Uses `Color.Transparent` background when a valid logo is loaded, renders an animated shimmer + progress indicator while loading, and displays a top red HTTP status code banner (`HTTP 404`, `ERR`) on load errors.
- `StatusBadge(status: TrackerStatus, modifier: Modifier)`:
  - Renders a Material 3 tonal badge using `status.containerColor()` and `status.contentColor()`.
  - Accessible semantics announce the status label.
- `FitBadge(score: Int, modifier: Modifier, textAlign: TextAlign, maxLines: Int)`:
  - Renders an algorithmic fit rating pill with 3-tier color coding:
    - `>= 80`: Strong fit (`primaryContainer`)
    - `>= 60`: Moderate fit (`secondaryContainer`)
    - `< 60`: Low / stretch fit (`surfaceVariant`)
- `NotesWidget(onNotesChanged: (ImmutableList<String>) -> Unit, modifier: Modifier, initialNotes: ImmutableList<String>, label: String)`:
  - Renders an interactive, reorderable, multi-note editor inside an elevated surface with semantic content descriptions. Accepts and emits stable `ImmutableList<String>` to prevent unnecessary Compose recomposition churn.
- `TooltipIconButton(tooltip: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier, enabled: Boolean)`:
  - Cross-platform tooltip button wrapping M3 `TooltipBox` and `IconButton`.
- `JobApplicationTrackerImage(model: Any?, contentDescription: String?, modifier: Modifier, contentScale: ContentScale, onState: (AsyncImagePainter.State) -> Unit)`:
  - Single public image loading composable. Delegates to Coil's `AsyncImage` using the application-configured `ImageLoader` from `ProvideImageLoader`. All image loading in the app should flow through this component.
- `ProvideImageLoader(content: @Composable () -> Unit)`:
  - App-level wrapper that configures Coil's singleton `ImageLoader` with `DebugLogger`, Ktor3 network fetcher, and observability hooks. Wrap app content with this inside `JobTrackerTheme`.
- `ImageLoadEventListener` interface + `DefaultImageLoadEventListener`:
  - Framework-agnostic observability interface for image load lifecycle events (start, success, error, cancel). Registered as a Koin singleton via `coreWidgetsModule`. Composed from 4 `fun interface` callbacks: `OnImageLoadStart`, `OnImageLoadSuccess`, `OnImageLoadError`, `OnImageLoadCancel`.

---

## 5. Key Gradle & Mise Tasks

| Task | Scope | Purpose |
| :--- | :--- | :--- |
| `./gradlew :frontend:core-widgets:compileCommonMainKotlinMetadata` | Module | Validates explicit API compliance and Compose widget compilation |
| `./gradlew :frontend:core-widgets:assemble` | Module | Compiles multiplatform library targets |
| `mise run frontend:build` | Workspace | Builds full application hierarchy |

---

## 6. Deep-Link & Navigation Integration
`:core-widgets` is a component library. It does not declare navigation destinations or handle deep links directly, but its widgets are rendered inside screens navigated via deep links.

---

## 7. Convention Plugins & Dependencies

### Applied Convention Plugins
- `local.kotlin.multiplatform`: Multi-target toolchain.
- `local.android.kmp.library`: AGP Multiplatform Library setup.
- `local.compose`: Compose compiler plugin.
- `local.dependencies.compose`: Compose UI, Material 3, and Foundation libraries.
- `local.dependencies.di`: Koin dependency injection.

### Key Dependencies
- `api(projects.coreDesignSystem)`: Exports tokens, themes, and responsive classes to feature consumers.
- `api(projects.coreData)`: Exports domain entities (`TrackerStatus`, etc.) needed by widgets.
- `implementation(libs.bundles.coil)`: Coil 3 image loading (coil-compose, coil-core, coil-network-ktor3).
