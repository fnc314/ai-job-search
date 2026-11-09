# Module: `:frontend:core-widgets`

> [!NOTE]
> **Quick Facts for AI Agents & Developers**
> - **Subproject**: `:frontend:core-widgets`
> - **Package Root**: `dev.fnc314.tools.jobtracker.core.widgets`
> - **Primary Contributions**: Universal shared Compose components (`StatusBadge`, `FitBadge`, `NotesWidget`, `TooltipIconButton`), re-exports `core:design-system`
> - **Architectural Invariant**: Acts as the single shared UI entry point for feature modules; re-exports `projects.coreDesignSystem` via `api(...)`.
> - **Key Build/Verification Command**: `./gradlew :frontend:core-widgets:compileCommonMainKotlinMetadata`

---

## 1. Module Overview
`:frontend:core-widgets` houses reusable, theme-aware Compose presentation components that are shared across multiple feature modules. It serves as the universal shared UI layer, eliminating duplicate widget implementations and standardizing micro-interactions, badge styling, and accessibility across all screens.

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
│   ├── StatusBadge.kt          # Visual tonal pill displaying application funnel status
│   ├── FitBadge.kt             # Color-tiered fit rating pill (green / amber / neutral)
│   ├── NotesWidget.kt          # Expandable / scrollable multi-note inspector
│   └── TooltipIconButton.kt    # Accessible icon button with desktop hover & mobile tooltip
└── androidMain/kotlin/         # Android core KTX extensions
```

---

## 4. Public API Surface & Significant Contributions

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

### Key Dependencies
- `api(projects.coreDesignSystem)`: Exports tokens, themes, and responsive classes to feature consumers.
- `api(projects.coreData)`: Exports domain entities (`TrackerStatus`, etc.) needed by widgets.
