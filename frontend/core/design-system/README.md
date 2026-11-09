# Module: `:core-design-system`

> [!NOTE]
> **Quick Facts for AI Agents & Developers**
> - **Subproject**: `:core-design-system`
> - **Package Root**: `dev.fnc314.tools.jobtracker.core.designsystem`
> - **Primary Contributions**: Material 3 theme engine (`JobTrackerTheme`), dynamic color schemes, `SeedColors`, typography tokens, window size classification, spacing, shapes, and dimensions
> - **Architectural Invariant**: **Contains ZERO composable widgets.** Exclusive location for `Color(...)` constructor invocations in the repository.
> - **Key Build/Verification Command**: `./gradlew :core-design-system:compileCommonMainKotlinMetadata`

---

## 1. Module Overview
`:core-design-system` provides the foundational visual styling and responsive layout infrastructure for the Job Tracker application. It defines the Material 3 design tokens, theme engine, high-contrast accessible color generation, adaptive window size classes, and the exclusive color parsing utilities for the entire workspace.

---

## 2. Contribution to the Larger Application
This module establishes the single source of truth for all styling rules across the app:
- **Upstream Consumers**: `core:widgets` (which exports this module via `api(projects.coreDesignSystem)`), and `apps:core` (which hosts the top-level `JobTrackerTheme` and `ProvideWindowSizeClass` root composables).
- **Design Boundary**: Feature modules depend on `core:widgets` rather than declaring direct dependencies on `core:design-system`.
- **Zero Rogue `Color()` Policy**: No module other than `:core-design-system` may invoke `Color(...)`. All hex string conversions (`toColorOrNull()`) and serializations (`toHexString()`) reside here.

---

## 3. Package & Subproject Structure

```
core/design-system/src/
├── commonMain/kotlin/dev/fnc314/tools/jobtracker/core/designsystem/
│   ├── Color.kt                  # Seed colors, M3 dynamic scheme builders, toColorOrNull(), toHexString()
│   ├── Dimensions.kt             # Component sizes, icon sizes, elevation tokens
│   ├── Shapes.kt                 # Material 3 shape scale (extraSmall, small, medium, large, full)
│   ├── Spacing.kt                # Spacing scale tokens and LocalSpacing composition local
│   ├── Theme.kt                  # JobTrackerTheme composable, ThemeMode, ColorContrast
│   ├── Typography.kt             # Material 3 typographic hierarchy
│   └── WindowSizeClass.kt        # Multiplatform responsive window width/height breakpoints
├── androidMain/kotlin/           # Android dynamic color (Monet) platform integration
├── jvmMain/kotlin/               # Desktop theme overrides
├── iosMain/kotlin/               # iOS UIKit appearance integration
└── webMain/kotlin/               # Web CSS / canvas color bindings
```

---

## 4. Public API Surface & Significant Contributions

### Theming & Responsive Breakpoints
- `JobTrackerTheme`: Master application theme wrapper supporting `ThemeMode` (System, Light, Dark), `ColorContrast` (Standard, Medium, High), and dynamic seed colors.
- `WindowSizeClass`: Responsive window classification providing `isCompact`, `isMedium`, and `isExpanded` dimensions across phone, tablet, and desktop viewports.
- `ProvideWindowSizeClass`: Composition local provider for adaptive UI hierarchies.

### Design Tokens
- `MaterialTheme.spacing`: Semantic spacing scale (`none`, `compact`, `extraSmall`, `small`, `subMedium`, `medium`, `large`, `extraLarge`).
- `MaterialTheme.componentSizes`: Standardized component heights, card padding, and avatar sizes.
- `MaterialTheme.iconSizes`: Consistent icon dimensions (`tiny`, `small`, `medium`, `large`).
- `SeedColors`: Canonical theme seed colors (`ProfessionalBlue`, `MaterialPurple`, `ForestTeal`, `SlateIndigo`, `NordicSage`, `WarmAmber`) and `DEFAULT_SEED_HEX`.

### Centralized Color Conversions
- `String.toColorOrNull()`: Safely parses hex strings into Compose `Color` instances without throwing exceptions.
- `Color.toHexString()`: Formats a Compose `Color` to standard lowercase 6-digit hex format.

---

## 5. Key Gradle & Mise Tasks

| Task | Scope | Purpose |
| :--- | :--- | :--- |
| `./gradlew :core-design-system:compileCommonMainKotlinMetadata` | Module | Checks explicit API mode and verifies token compilation |
| `./gradlew :core-design-system:assemble` | Module | Builds all multiplatform library binaries |
| `mise run ui:kmp:build` | Workspace | Builds all modules across the application |

---

## 6. Deep-Link & Navigation Integration
`:core-design-system` is a pure styling and token library. It does not provide navigation destinations or handle deep links.

---

## 7. Convention Plugins & Dependencies

### Applied Convention Plugins
- `local.kotlin.multiplatform`: Multiplatform target configuration.
- `local.android.kmp.library`: AGP Multiplatform Library setup.
- `local.compose`: Compose Multiplatform compiler plugin.
- `local.dependencies.compose`: Compose Material 3 and UI foundations.
