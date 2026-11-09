# Implementation Plan: Sorting Mechanism, Timestamp Formatting, and UI Role/Company Swap

**Status:** Draft / Proposed
**Date:** 2026-11-08
**Target:** `ui/kmp` (`features/job-applications-list`, `core/data`)

---

## 1. Executive Summary & Goals

Investigate and fix user-reported issues and requests:
1. **Sorting**: Fix timestamp parsing so date-based sorting (`DATE_CREATED`, `DATE_UPDATED`) functions properly instead of falling back to company name tie-breaker due to default current timestamps.
2. **Timestamps ("updated just now")**: Extract `createdAt` and `updatedAt` in `RemoteJobApplicationsListDataSourceImpl` from Firestore record fields rather than defaulting to `Clock.System.now()`.
3. **Role & Company Swap in UI**: Swap the display positioning and typography styles in `JobApplicationRow`:
   - Job Role (Title) becomes the prominent top-left header (using `titleMedium` style).
   - Employer (Company name) moves to the bottom-left subtitle position (using `bodyMedium` style).

## 2. Architecture & Design Principles

- **Clean Architecture & Domain Purity**: Ensure data sources correctly map raw Firestore records to domain models.
- **Visual Hierarchy**: Job role is the primary focus (top-left, larger/bold font), while company name is the secondary context (bottom-left subtitle).
- **Testability**: Enhance UI unit tests for model mapping and presentation states.

## 3. Proposed Changes & Implementation Steps

### [MODIFY] [RemoteJobApplicationsListDataSourceImpl.kt](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/data/source/impl/RemoteJobApplicationsListDataSourceImpl.kt)
- Parse `createdAt` and `updatedAt` from record fields and pass to `Factory.createJobApplication`.

### [MODIFY] [JobApplicationRow.kt](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/component/JobApplicationRow.kt)
- Swap row layout: top-left shows Job Role (`titleMedium`), bottom-left shows Company Name (`bodyMedium`).

### [MODIFY] [JobApplicationsListViewModelTest.kt](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonTest/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/JobApplicationsListViewModelTest.kt)
- Enhance test assertions for UI model mapping (role, company, timestamps).

## 4. Verification Plan & Testing Checklist

### Automated Tests
- Run Gradle unit tests (`:features-job-applications-list-domain:allTests`, `:features-job-applications-list-data:allTests`, `:features-job-applications-list-ui:allTests`).
- Run `compileCommonMainKotlinMetadata`.
