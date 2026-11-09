# Implementation Plan: Expandable Job Application Tiles on Compact Widths & Adaptive Wider Layouts

**Status:** Draft / Proposed
**Date:** 2026-11-08
**Target:** `ui/kmp` (`features/job-applications-list/ui`)

---

## 1. Executive Summary & Goals

Implement responsive job application tiles supporting both compact and wider screen widths according to `prompt.md`:
- **Compact Screen Widths**:
  - 90x90 company logo square at START position.
  - Job Title at top-end of logo (`titleMedium`/`titleLarge`), Company name at bottom-end of logo (`bodyMedium`/`bodyLarge`).
  - 90x90 Chevron space at END position (pointing right when collapsed, rotating/pointing down when expanded).
  - "Created on YYYY-MM-DD" aligning top-start of chevron; `StatusTag` aligning bottom-start of chevron in collapsed state.
  - Expanded state: 3x3 grid body containing:
    - Row 1: "Fit Score", "Applied on", "Updated"
    - Row 2: Numerical FIT score (using `Text`, not `FitTag`), submission date (YYYY-MM-DD), semantic updated date
    - Row 3: Primary Button to launch `features/job-application-details`.
- **Wider Screen Widths**:
  - Remove collapsing; surface all data directly on the card.
  - Tapping the card launches `features/job-application-details`.
  - Data alignment order:
    - START of "Applied on YYYY-MM-DD" aligns with END of "Created on" text.
    - START of `FitTag` aligns with END of `StatusTag`.
    - START of "Updated ..." aligns with END of "Applied on YYYY-MM-DD" text.
    - Space content out evenly as space permits.

## 2. Architecture & Design Principles

- **Adaptive Window Size Class Integration**: Uses window size class / layout constraints to switch between compact expandable card behavior and wider non-collapsing rich layout.
- **Clean UI & Reusability**: Robust MVI state and clean Compose components.

## 3. Proposed Changes & Implementation Steps

### [MODIFY] [JobApplicationItemUiModel.kt](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/JobApplicationItemUiModel.kt)
- Add fields for created date (YYYY-MM-DD), applied date (YYYY-MM-DD), updated date, and expansion state.

### [MODIFY] [JobApplicationRow.kt](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/component/JobApplicationRow.kt)
- Implement adaptive row/tile layout for compact and wider screens.

## 4. Verification Plan & Testing Checklist

### Automated Tests
- Run Gradle KMP metadata compilation and Android assemble.
