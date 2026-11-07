# Implementation Plan: Internalize Feature Data Sources & Enforce via Konsist

**Status:** Draft / Proposed  
**Date:** 2026-11-06  
**Target:** `ui/kmp`  

---

## 1. Executive Summary & Goals

To strengthen encapsulation and adhere to the principle of least privilege, feature data sources (`*[Remote|Network|Local]DataSource*` interfaces and their `*Impl` implementations) must be strictly **`internal`** to their respective `:features:<name>:data` Gradle modules.

- **Previous State**: Data source interfaces were `public` in the data module.
- **New Architecture Invariant**: Data source interfaces and `*Impl` classes must be marked `internal`. Only the `*Repository` interface (in domain) and `*RepositoryImpl` (in data, bound to the domain interface) cross module boundaries.
- **Konsist Enforcement**: Add a Konsist test rule asserting that all data source interfaces and classes are `internal`.
- **Template Refactoring**: Refactor reference templates (`features/job-applications-list` and `features/settings`) to internalize their data sources.

---

## 2. Architecture & Design Principles

- **Strict Encapsulation**: Feature data modules encapsulate all `DataSource` logic internally. External callers interact exclusively with `Repository` contracts in domain.
- **Konsist Gatekeeping**: Automated unit tests prevent any future data source interfaces from leaking `public` visibility outside the data module.

---

## 3. Proposed Changes & Implementation Steps

### Step 1: Add Konsist Rule for Data Source Internal Visibility
#### [MODIFY] [KonsistArchitectureTest.kt](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/apps/core/src/jvmTest/kotlin/dev/fnc314/tools/jobtracker/architecture/KonsistArchitectureTest.kt)
- Add test: `all datasource interfaces and implementations are internal`.

### Step 2: Refactor Template Data Modules (`features/settings/data`, `features/job-applications-list/data`)
- Change `public interface *DataSource` to `internal interface *DataSource` and ensure `internal class *Impl` is internal.

---

## 4. Verification Plan & Testing Checklist

- [ ] Run Konsist architecture tests: `./gradlew :apps-core:jvmTest --tests dev.fnc314.tools.jobtracker.architecture.KonsistArchitectureTest`
- [ ] Compile KMP metadata: `./gradlew compileCommonMainKotlinMetadata`
- [ ] Assemble Android debug APK: `mise run ui:kmp:android:assemble`
