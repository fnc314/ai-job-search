# Kotlin Multiplatform Workspace Architecture & Design Plans

This directory contains design and implementation plans for the `ui/kmp` project.

> [!NOTE]
> For repository-wide cross-subsystem plans that span `records` and `ui/kmp` (such as Dokka documentation aggregation and Detekt/Konsist linting), refer to the root architectural plans directory at [`.agents/plans/`](../../.agents/plans/README.md).

---

## Plan Invariants (MANDATORY)
1. **Target-Derived Location**: Subsystem plans specific to `ui/kmp` live here. Cross-subsystem plans belong in root `.agents/plans/`.
2. **Date Subdirectory**: Plans must be saved in a `YYYY-MM-DD/` folder matching creation date.
3. **24-Hour Timestamp Prefix**: Filenames must be formatted as `HHMMSS_<plan-name>.md` using a 24-hour clock (`00`-`23` for `HH`).
4. **Registry Synchronization**: Register the plan in the table below with its relative path (`./YYYY-MM-DD/HHMMSS_<plan-name>.md`), status, and scope.
5. **Mandatory Opening Frontmatter**: Start every plan from [`TEMPLATE.md`](../../../.agents/plans/TEMPLATE.md) (`Status`, `Date`, `Target` block); `Status` must match the registry row.

---

### UI/KMP Plans

| Plan | Status | Scope / Focus Area |
| :--- | :--- | :--- |
| [`add-job-application-url-and-records-dep.md`](./2026-11-03/232245_add-job-application-url-and-records-dep.md) | Completed | Job URL navigation support and records composite build integration |
| [`build-logs-and-compiler-warnings-triage-plan.md`](./2026-11-05/160417_build-logs-and-compiler-warnings-triage-plan.md) | Completed | Build logs and compiler warnings triage & resolution |
| [`clean-architecture-datasources-repositories-plan.md`](./2026-11-05/164709_clean-architecture-datasources-repositories-plan.md) | Completed | Clean Architecture layer decoupling: DataSources in core, Repositories in features |
| [`compose-resources-migration-plan.md`](./2026-11-05/160417_compose-resources-migration-plan.md) | Completed | Migration of all UI literals to `composeResources` |
| [`create-core-modules-and-notes-widget.md`](./2026-11-03/205720_create-core-modules-and-notes-widget.md) | Completed | Core widget extraction (`NotesWidget`, `StatusBadge`, `FitBadge`) |
| [`custom-gradle-plugins-architecture-plan.md`](./2026-11-05/164709_custom-gradle-plugins-architecture-plan.md) | Completed | Custom convention plugins in `gradle/tools/project-plugins` |
| [`firebase-android-and-multiplatform-integration.md`](./2026-11-03/230050_firebase-android-and-multiplatform-integration.md) | Completed | GitLive Firebase SDK multiplatform data store implementation |
| [`fix-android-defects.md`](./2026-11-03/205720_fix-android-defects.md) | Completed | Android runtime, build, and edge-to-edge UI defect fixes |
| [`integrate-records-fix-icons.md`](./2026-11-04/064700_integrate-records-fix-icons.md) | Completed | Protobuf records integration and Material icon fixes |
| [`kmp-mobile-tracker-ui.md`](./2026-11-02/221655_kmp-mobile-tracker-ui.md) | Completed | Foundation KMP tracker architecture, models, and initial UI screens |
| [`m3-theming-adaptive-layout-and-widgets.md`](./2026-11-04/122940_m3-theming-adaptive-layout-and-widgets.md) | Completed | Material 3 token system, dynamic colors & adaptive layouts |
| [`module-readmes-plan.md`](./2026-11-05/164709_module-readmes-plan.md) | Completed | Module `README.md` documentation standard & verification script |
| [`unified-remote-datasource-and-firebase-generics.md`](./2026-11-06/190000_unified-remote-datasource-and-firebase-generics.md) | Draft / Proposed | Unified RemoteDataSource and Feature-specific Firebase generics mapping |
| [`mvi-architecture-and-ui-model-separation-plan.md`](./2026-11-05/160417_mvi-architecture-and-ui-model-separation-plan.md) | Completed | MVI state/intent architecture & UI model decoupling |
| [`platform-agnosticism-renaming-plan.md`](./2026-11-05/160417_platform-agnosticism-renaming-plan.md) | Completed | Platform-agnostic module renaming (`apps/core`, `features/*`) |
| [`repackaging-shared-and-job-applications-modularization.md`](./2026-11-04/195448_repackaging-shared-and-job-applications-modularization.md) | Completed | Feature packaging and modularization (`features/job-applications-*`) |
| [`room3-platform-support-and-caching-plan.md`](./2026-11-05/163042_room3-platform-support-and-caching-plan.md) | Completed | Room 3 multiplatform platform support, Uuid7 & caching |
| [`root-scaffold-and-navigation-enhancement-plan.md`](./2026-11-05/160417_root-scaffold-and-navigation-enhancement-plan.md) | Completed | Navigation backstack & adaptive scaffold enhancement |
| [`testing-bundles-plan.md`](./2026-11-05/164709_testing-bundles-plan.md) | Completed | Multiplatform testing catalog bundles & convention plugin |
| [`thinning-apps-core-and-core-navigation.md`](./2026-11-05/160417_thinning-apps-core-and-core-navigation.md) | Completed | Decoupling navigation routes to feature modules |
| [`ui-flow-improvements-and-modularization.md`](./2026-11-04/133245_ui-flow-improvements-and-modularization.md) | Completed | UI screen flows and widget extractions |
| [`unit-tests-coverage-plan.md`](./2026-11-05/164709_unit-tests-coverage-plan.md) | Completed | 5:1 Unit test coverage across platform-agnostic business logic |
