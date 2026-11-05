# Kotlin Multiplatform Workspace Architecture & Design Plans

This directory contains design and implementation plans for the `ui/kmp` project.

| Plan | Status | Scope / Focus Area |
| :--- | :--- | :--- |
| [`compose-resources-migration-plan.md`](./compose-resources-migration-plan.md) | Completed | Migration of all UI literals to `composeResources` |
| [`mvi-architecture-and-ui-model-separation-plan.md`](./mvi-architecture-and-ui-model-separation-plan.md) | Completed | MVI state/intent architecture & UI model decoupling |
| [`platform-agnosticism-renaming-plan.md`](./platform-agnosticism-renaming-plan.md) | Completed | Platform-agnostic module renaming (`apps/core`, `features/*`) |
| [`thinning-apps-core-and-core-navigation.md`](./thinning-apps-core-and-core-navigation.md) | Completed | Decoupling navigation routes to feature modules |
| [`root-scaffold-and-navigation-enhancement-plan.md`](./root-scaffold-and-navigation-enhancement-plan.md) | Completed | Navigation backstack & adaptive scaffold enhancement |
| [`repackaging-shared-and-job-applications-modularization.md`](./repackaging-shared-and-job-applications-modularization.md) | Completed | Feature packaging and modularization |
| [`m3-theming-adaptive-layout-and-widgets.md`](./m3-theming-adaptive-layout-and-widgets.md) | Completed | Material 3 token system & adaptive layouts |
| [`ui-flow-improvements-and-modularization.md`](./ui-flow-improvements-and-modularization.md) | Completed | UI screen flows and widget extractions |
| [`build-logs-and-compiler-warnings-triage-plan.md`](./build-logs-and-compiler-warnings-triage-plan.md) | Completed | Build logs and compiler warnings triage & resolution |
