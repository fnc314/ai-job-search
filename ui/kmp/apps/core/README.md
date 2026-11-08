# Module: Apps Core (`apps/core`)

> [!NOTE]
> **Quick Facts**
> - **Gradle Project ID**: `:apps-core`
> - **Module Tier**: Application Orchestrator & Assembler
> - **Package Base**: `dev.fnc314.tools.jobtracker`
> - **Target Platforms**: Android (`androidTarget`), Desktop JVM (`jvm`), iOS (`iosX64`, `iosArm64`, `iosSimulatorArm64`), Web (`js`, `wasmJs`)
> - **Dependencies**:
>   - Core: `:core-navigation`, `:core-data`, `:core-network`, `:core-widgets`
>   - Features: `:features-sign-in-ui`, `:features-job-applications-list-ui`, `:features-job-applications-details-ui`, `:features-settings-ui`, `:features-add-job-application-ui`, `:features-tracker-edits-ui`

---

## Overview

The `apps/core` module is the **thin assembler** of the multiplatform client application. It contains **zero** domain logic, **zero** repository implementations, and **zero** direct backend data sources.

Its sole responsibilities are:
1. **Application Shell & Scaffold (`AppScaffold.kt`)**: Material 3 root scaffolding with dynamic `CenterAlignedTopAppBar` (back navigation arrow, dynamic title, search & calendar action buttons) and adaptive navigation (compact `NavigationBar` vs medium/expanded `NavigationRail`).
2. **Top-Level Navigation Host (`App.kt`)**: Assembling feature navigation hooks (`FeatureNavigationHook`) into the root `NavHost` using `NavKeyRoute` entries.
3. **Dependency Injection Graph (`AppDI.kt`)**: Uniting foundational modules (`coreDataModule`, `coreNetworkModule`) and feature modules (`signInFeatureModule`, `jobApplicationsListFeatureModule`, `jobApplicationDetailsFeatureModule`, `settingsFeatureModule`, `addJobApplicationFeatureModule`, `trackerEditsFeatureModule`).
4. **Platform Bridge (`MainViewController.kt`)**: Providing the iOS entry point `ComposeUIViewController`.

---

## Structure

```
apps/core/src/
├── commonMain/
│   └── kotlin/dev/fnc314/tools/jobtracker/
│       ├── App.kt                      # Root NavHost and feature hook orchestrator
│       ├── di/
│       │   └── AppDI.kt                # Centralized DI module aggregation (appModule)
│       └── ui/
│           └── AppScaffold.kt          # Material 3 responsive Scaffold, TopAppBar & NavRail
└── iosMain/
    └── kotlin/dev/fnc314/tools/jobtracker/
        └── MainViewController.kt      # iOS UIViewController factory
```
