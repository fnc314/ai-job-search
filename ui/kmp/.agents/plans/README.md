# Kotlin Multiplatform Workspace Architecture & Design Plans

This directory contains design and implementation plans for the `ui/kmp` project.

| Plan | Status | Scope / Focus Area |
| :--- | :--- | :--- |
| [`add-job-application-url-and-records-dep.md`](./add-job-application-url-and-records-dep.md) | Completed | Job URL navigation support and records composite build integration |
| [`build-logs-and-compiler-warnings-triage-plan.md`](./build-logs-and-compiler-warnings-triage-plan.md) | Completed | Build logs and compiler warnings triage & resolution |
| [`clean-architecture-datasources-repositories-plan.md`](./clean-architecture-datasources-repositories-plan.md) | Completed | Clean Architecture layer decoupling: DataSources in core, Repositories in features |
| [`compose-resources-migration-plan.md`](./compose-resources-migration-plan.md) | Completed | Migration of all UI literals to `composeResources` |
| [`create-core-modules-and-notes-widget.md`](./create-core-modules-and-notes-widget.md) | Completed | Core widget extraction (`NotesWidget`, `StatusBadge`, `FitBadge`) |
| [`custom-gradle-plugins-architecture-plan.md`](./custom-gradle-plugins-architecture-plan.md) | Completed | Custom convention plugins in `gradle/tools/project-plugins` |
| [`dokka-and-comprehensive-kdocs-plan.md`](./dokka-and-comprehensive-kdocs-plan.md) | Completed | Dokka 2.3.0 unified multi-project setup & exhaustive KDocs |
| [`DRAFT-code-refactor.md`](./DRAFT-code-refactor.md) | Draft | Self-contained Enum relationships & companion lookup map refactor proposal |
| [`firebase-android-and-multiplatform-integration.md`](./firebase-android-and-multiplatform-integration.md) | Completed | GitLive Firebase SDK multiplatform data store implementation |
| [`fix-android-defects.md`](./fix-android-defects.md) | Completed | Android runtime, build, and edge-to-edge UI defect fixes |
| [`integrate-records-fix-icons.md`](./integrate-records-fix-icons.md) | Completed | Protobuf records integration and Material icon fixes |
| [`kmp-mobile-tracker-ui.md`](./kmp-mobile-tracker-ui.md) | Completed | Foundation KMP tracker architecture, models, and initial UI screens |
| [`m3-theming-adaptive-layout-and-widgets.md`](./m3-theming-adaptive-layout-and-widgets.md) | Completed | Material 3 token system, dynamic colors & adaptive layouts |
| [`module-readmes-plan.md`](./module-readmes-plan.md) | Completed | Module `README.md` documentation standard & verification script |
| [`mvi-architecture-and-ui-model-separation-plan.md`](./mvi-architecture-and-ui-model-separation-plan.md) | Completed | MVI state/intent architecture & UI model decoupling |
| [`platform-agnosticism-renaming-plan.md`](./platform-agnosticism-renaming-plan.md) | Completed | Platform-agnostic module renaming (`apps/core`, `features/*`) |
| [`repackaging-shared-and-job-applications-modularization.md`](./repackaging-shared-and-job-applications-modularization.md) | Completed | Feature packaging and modularization (`features/job-applications-*`) |
| [`room3-platform-support-and-caching-plan.md`](./room3-platform-support-and-caching-plan.md) | Completed | Room 3 multiplatform platform support, Uuid7 & caching |
| [`root-scaffold-and-navigation-enhancement-plan.md`](./root-scaffold-and-navigation-enhancement-plan.md) | Completed | Navigation backstack & adaptive scaffold enhancement |
| [`testing-bundles-plan.md`](./testing-bundles-plan.md) | Completed | Multiplatform testing catalog bundles & convention plugin |
| [`thinning-apps-core-and-core-navigation.md`](./thinning-apps-core-and-core-navigation.md) | Completed | Decoupling navigation routes to feature modules |
| [`ui-flow-improvements-and-modularization.md`](./ui-flow-improvements-and-modularization.md) | Completed | UI screen flows and widget extractions |
| [`unit-tests-coverage-plan.md`](./unit-tests-coverage-plan.md) | Completed | 5:1 Unit test coverage across platform-agnostic business logic |
