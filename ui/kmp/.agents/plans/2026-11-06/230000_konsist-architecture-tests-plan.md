# Architecture Plan: Konsist Architecture Tests & Structural Enforcement

**Status:** Draft / Proposed  
**Date:** 2026-11-06  
**Target:** `ui/kmp`  

---

## 1. Executive Summary & Goals

Now that `features/job-applications-list` and `features/settings` have established the canonical architectural template for Clean Architecture layers (`domain`, `data`, `ui`), DI modules (`*FeatureModule`, `*DataModule`, `*DomainModule`, `*UiModule`), and `.impl.` internal packaging, we need to enforce these invariants automatically using **Konsist** tests.

This plan details:
1. New Konsist test rules in `KonsistArchitectureTest.kt` enforcing:
   - **Clean Architecture Layer Boundaries**: Domain code must not import data or ui packages.
   - **`.impl.` Package Convention**: `internal` implementation classes (`*Impl`) must reside within `.impl` sub-packages.
   - **DI Module Naming**: Feature DI modules must follow standard naming (`*DomainModule`, `*DataModule`, `*UiModule`, `*FeatureModule`).
2. A refactoring plan to bring all other feature modules (`features/sign-in`, `features/add-job-application`, `features/job-applications-details`, `features/tracker-edits`) into compliance with these Konsist rules.

---

## 2. Architecture & Design Principles

- **Automated Guardrails**: Architecture rules enforced at compile time/test time rather than through manual code reviews.
- **Strict Clean Layering**: Unidirectional dependency flow (`UI` -> `Domain` & `Data`, `Data` -> `Domain`, `Domain` -> pure domain/core-data).
- **Encapsulation**: Internal implementations hidden behind clean public interfaces using `.impl.` sub-packages.

---

## 3. Proposed Changes & Implementation Steps

### Step 1: Add Konsist Architectural Rules
#### [MODIFY] [KonsistArchitectureTest.kt](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/apps/core/src/jvmTest/kotlin/dev/fnc314/tools/jobtracker/architecture/KonsistArchitectureTest.kt)
- Add test: `internal implementations end with Impl and reside in impl package`.
- Add test: `feature domain modules must not reference ui or data packages`.
- Add test: `DI modules follow naming conventions`.

### Step 2: Audit and Refactor Other Feature Modules
- Inspect `features/sign-in`, `features/add-job-application`, `features/job-applications-details`, and `features/tracker-edits`.
- Apply `.impl.` housing pattern where `Impl` classes are outside `impl` sub-packages.
- Standardize DI module names (`*FeatureModule`, `*DomainModule`, `*DataModule`, `*UiModule`).

### Step 3: Verification & Execution
- Run JVM tests (`./gradlew :apps-core:jvmTest` or konsist test suite) to verify all structural rules pass cleanly.

---

## 4. Verification Plan & Testing Checklist

- [ ] Run Konsist architecture tests: `./gradlew :apps-core:jvmTest --tests dev.fnc314.tools.jobtracker.architecture.KonsistArchitectureTest`
- [ ] Compile all KMP modules: `./gradlew compileCommonMainKotlinMetadata`
- [ ] Assemble Android debug APK: `mise run ui:kmp:android:assemble`
