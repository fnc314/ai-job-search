# Agent Guidelines: Kotlin Multiplatform Workspace (`ui/kmp`)

This document defines architecture invariants, development workflows, and mandatory rules for AI coding assistants working in the `ui/kmp` multiplatform workspace.

---

## 1. Workspace Architecture Map

The project is structured into three clean layers plus the custom build convention toolchain:

```mermaid
graph TD
    Apps["apps/* (android, desktop, web, core)"]
    Features["features/* (sign-in, job-applications-list, job-applications-details, settings, add-job-application, tracker-edits)"]
    CoreWidgets["core:widgets (Universal UI Components & Badges)"]
    CoreDesignSystem["core:design-system (Tokens, Themes, Spacing, Shapes, Colors)"]
    CoreData["core:data (Entities, Enums, Factory, Repositories, Settings)"]
    CoreNetwork["core:network (Ktor Multiplatform Engines)"]
    Plugins["gradle/tools/project-plugins (Local Convention Plugins)"]

    Apps --> Features
    Apps --> CoreWidgets
    Apps --> CoreData
    Features --> CoreWidgets
    Features --> CoreData
    CoreWidgets --> CoreDesignSystem
    CoreWidgets --> CoreData
    CoreData --> CoreNetwork
    Plugins -. applies to .-> Apps
    Plugins -. applies to .-> Features
    Plugins -. applies to .-> CoreWidgets
    Plugins -. applies to .-> CoreDesignSystem
    Plugins -. applies to .-> CoreData
    Plugins -. applies to .-> CoreNetwork
```

### Module Breakdown
- **`core/data`**: Shared domain models (`JobApplication`, `TrackerEvent`, etc.), self-contained enums, centralized `Factory`, repository contracts, Firestore & Ktor storage implementations.
- **`core/design-system`**: Material 3 styling tokens, dynamic color schemes, seed colors, typography, shapes, spacing, and window size classes. **Contains ZERO composable widgets.**
- **`core/widgets`**: Universal, reusable UI components (`StatusBadge`, `FitBadge`, `NotesWidget`, `TooltipIconButton`). Exports `core:design-system` via `api(projects.coreDesignSystem)` to serve as the unified UI foundation for features.
- **`core/network`**: Multiplatform HTTP engine abstractions and preconfigured JSON Ktor clients.
- **`features/*`**: Consolidated domain, data, and UI feature submodules (`features/<name>/domain`, `features/<name>/data`, `features/<name>/ui`).
- **`apps/*`**: Platform deployment targets (`apps/android`, `apps/desktop`, `apps/web`) and the shared navigation/DI coordinator (`apps/core`).
- **`gradle/tools/project-plugins`**: Included Gradle build providing type-safe convention plugins (`local.kotlin.multiplatform`, `local.android.kmp.library`, `local.compose`, etc.).

---

## 2. Invariants & Rules for AI Agents (MANDATORY)

### Rule 1: Synchronize Module `README.md` Files
Every contributing module under `core/` and `features/` maintains a root `README.md` (e.g. `core/data/README.md`, `features/sign-in/README.md`).
- **Mandate**: Whenever you add, modify, or delete public APIs, use cases, navigation routes, dependencies, or architectural responsibilities in any module, you **MUST** update the corresponding module `README.md` within the same batch of changes.
- **Verification**: Run `mise run ui:kmp:docs:readmes` (or `bash scripts/check-kmp-readmes.sh`) to ensure all module documentation exists and is up to date.

### Rule 2: Strict Kotlin Explicit API Mode
All modules enforce explicit API mode (`explicitApi()` or `explicitApiWarning()`):
- **Visibility**: Every declaration (class, interface, fun, val, var) must be marked `public`, `internal`, or `private`.
- **Return Types**: Every function and property must explicitly specify its return type.
- **Least Privilege**: Only items intended to be consumed across module boundaries deserve `public` visibility. Screen components, internal dialogs, and platform helper classes must remain `internal`.

### Rule 3: Zero Rogue `Color()` Calls
- The `Color(...)` constructor must **NEVER** be called outside `core/design-system/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/designsystem/Color.kt`.
- Hex parsing (`String.toColorOrNull()`) and hex formatting (`Color.toHexString()`) are provided by `core/design-system`.
- In Compose UI, colors must be referenced through `MaterialTheme.colorScheme.*` or canonical `SeedColors` / `AppColors`.

### Rule 4: Clean UI Dependency Decoupling (Option A)
- `core/widgets` exports `core:design-system` via `api(projects.coreDesignSystem)`.
- Feature modules (`features/*/ui`) must depend on `projects.coreWidgets` and **not** directly on `projects.coreDesignSystem`.
- `core/design-system` must remain strictly token- and theme-only; no widgets may be introduced into it.

### Rule 5: Convention Plugins Standard
Build scripts must apply the local convention plugins declared in `gradle/libs.versions.toml`:
- Functional plugins: `local.kotlin.multiplatform`, `local.android.kmp.library`, `local.compose`.
- Concern plugins: `local.dependencies.compose`, `local.dependencies.koin`, `local.dependencies.ktor`, `local.dependencies.serialization`.
- Do not configure target SDKs, compiler options, or toolchains manually in module build scripts.

### Rule 6: Architecture & Design Plans in `.agents/plans/`
- All technical architecture, design, and migration plans oriented to `ui/kmp` must be housed in `.agents/plans/` and indexed in `ui/kmp/.agents/plans/README.md`.

### Rule 7: Zero-Warning Compilation & Clean Build Logs
- Build logs must remain clean of compiler warnings and deprecations.
- Always use non-deprecated Material 3 APIs (e.g. `menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)` and `TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above)`).
- Ensure all public interfaces, data classes, and properties in domain modules declare explicit visibility and explicit return types.
- Pre-packaged vendor native binaries that lack NDK symbols must be excluded from stripping via `packaging.jniLibs.keepDebugSymbols` rather than emitting packaging warnings.

---

## 3. Key Gradle & Mise Execution Commands

> [!IMPORTANT]
> The workspace relies on a custom Java SDK managed via Mise (`26.0.2+101`). Always execute Gradle commands with `BypassSandbox: true` when running in agent sandboxes to ensure access to the Mise runtime.

| Command | Purpose |
| :--- | :--- |
| `./gradlew compileCommonMainKotlinMetadata` | Fast compiler check across all KMP modules verifying explicit API mode and metadata |
| `mise run ui:kmp:android:assemble` | Assembles Android debug APK (`:apps-android:assembleDebug`) |
| `mise run ui:kmp:desktop:run` | Runs the Compose Desktop JVM application |
| `mise run ui:kmp:web:wasm:dev` | Runs Wasm development server in browser |
| `mise run ui:kmp:docs:readmes` | Verifies presence and completeness of all module `README.md` files |
