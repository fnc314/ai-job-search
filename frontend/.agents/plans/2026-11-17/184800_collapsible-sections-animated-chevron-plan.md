# Architecture Plan: Collapsible Job Application Categorized Sections with Animated Chevron

**Status:** Completed  
**Date:** 2026-11-17  
**Target:** `frontend`  
**Harness:** Antigravity  
**Model:** gemini-3.8-flash  
**Session:** 1a905987-3182-436b-bce2-dcd93a2c0d45  
**Depends On:** [`./2026-11-16/004109_job-applications-categorized-sections-plan.md`](./2026-11-16/004109_job-applications-categorized-sections-plan.md)  

---

## 1. Executive Summary & Goals
Currently, `JobApplicationsListScreen` organizes applications into categorized sections (`Interviewing`, `In Draft`, `Applied`, `Rejected`) with sticky headers. However, section contents are always expanded, causing lists with many applied or rejected applications to require extensive vertical scrolling.

This plan introduces:
1. **Interactive Collapsible Sections**:
   - Each category section can be expanded and collapsed independently by tapping the header.
   - All sections default to expanded (`true`), preserving current behavior until toggled.
2. **Animated Chevron Icon**:
   - A directional chevron icon in each section header:
     - Facing right (`>`) / `0f` rotation when collapsed.
     - Facing down (`v`) / `90f` rotation when open.
   - Smooth rotation transition using Compose `animateFloatAsState`.
3. **Accessibility & Usability**:
   - Tap target spans the primary header row.
   - Appropriate content description ("Expand section" / "Collapse section").
   - Retains the section jump dropdown action without conflict.

---

## 2. Architecture & Design Principles

### State Management
- In `JobApplicationsListScreen`, introduce state tracking for collapsed section orders:
  `var collapsedSections by rememberSaveable { mutableStateOf(setOf<Int>()) }` (or an explicit expanded set: `var expandedSections by rememberSaveable { mutableStateOf(setOf(1, 2, 3, 4)) }`).
- When `sectionOrder` is in `collapsedSections`, the header renders with `isExpanded = false`, and the corresponding `items(apps)` call is conditionally skipped in `LazyColumn` and `LazyVerticalGrid`.
- State persists across configuration changes via `rememberSaveable`.

### Header Component Enhancement (`JobApplicationsSectionHeader`)
- Add properties:
  - `isExpanded: Boolean` (default `true`)
  - `onToggleExpand: () -> Unit`
- Layout:
  - Place the animated chevron at the leading edge (or next to the title/badge).
  - Animate rotation: `val chevronRotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)`.
  - Icon: `Icons.AutoMirrored.Filled.KeyboardArrowRight` rotated by `chevronRotation`.
    - At `0f`, it points right (`>`).
    - At `90f`, it points down (`v`).

---

## 3. Implementation Steps

1. **Update `JobApplicationsSectionHeader.kt`**:
   - Add `isExpanded: Boolean` and `onToggleExpand: () -> Unit` parameters.
   - Animate `chevronRotation` from `0f` (collapsed `>`) to `90f` (expanded `v`).
   - Wrap the title/badge/icon area in a clickable modifier triggering `onToggleExpand`.
2. **Update `JobApplicationsListScreen.kt`**:
   - Maintain `collapsedSections: Set<Int>` using `rememberSaveable`.
   - Pass `isExpanded = sectionOrder !in collapsedSections` and `onToggleExpand = { ... }` to `JobApplicationsSectionHeader`.
   - In Compact, Medium, and Expanded viewports, only render `items(apps)` when `sectionOrder !in collapsedSections`.
3. **Automated & Verification Tests**:
   - Run unit tests and verification checks across the frontend modules.

---

## 4. Bookkeeping
- [x] Plan persisted at `frontend/.agents/plans/2026-11-17/184800_collapsible-sections-animated-chevron-plan.md`
- [x] Registry `frontend/.agents/plans/README.md` updated
- [x] Implement and verify
- [x] Ready to commit
