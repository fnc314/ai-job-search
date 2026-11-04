# UI Flow Enhancements, Navigation, Formatting & Modularization Plan

**Status:** In Progress  
**Author:** Antigravity  
**Date:** 2026-11-04  

## Objective
Implement a suite of user experience, design system, and navigation upgrades to the KMP Job Tracker:
0. Architect & migrate UI flows from `shared` into dedicated `features/<Flow Name>/{domain,data,ui}` Gradle modules.
1. Replace `ColumnHeader`s with a layered dropdown/menu approach (Column selection + Ascending/Descending toggle, Mac-toolbar style).
2. Semantic timestamp formatting with strict precedence: `updatedAt` -> `createdAt` -> nothing.
   - `< 24 hours`: X hours ago
   - `< 7 days`: X days ago
   - `< 4 weeks`: X weeks and Y days ago
   - `< 2 months`: X months, Y weeks, and Z days ago
   - Thereafter: `YYYY-MM-DD (X months ago)`
3. Drop custom IconButton colors and add M3 `TooltipBox` hover/focus state labels.
4. Add leading icons to the FAB menu items.
5. Implement M3 `NavigationBar` (Bottom Navigation) with state-aware outlined/filled icons:
   - Primary: `Job Applications`
   - Secondary: `Pending Edits`
   - Tertiary: `Settings`
6. Surface actionable links for stored PDFs and Markdown artifacts (PDF -> default handler, Markdown -> default handler or in-app viewer via setting).
7. Remove the awkward expanding/collapsible card wrapper around Company Name in Details.
8. Populate and enrich the Events Timeline with custom event-type graphics, synthetic lifecycle fallback, and an interactive event detail inspection sheet/dialog.

---

## Plan Checklist

- [ ] **Step 1: Semantic Timestamp Formatter with Precedence (`updatedAt` -> `createdAt`)**
- [ ] **Step 2: Layered Mac-Toolbar Style Sort Dropdown (Dropping ColumnHeader)**
- [ ] **Step 3: M3 NavigationBar (Bottom Navigation with Filled/Outlined Icons)**
- [ ] **Step 4: Standardize IconButtons with M3 Tooltips & Add FAB Menu Icons**
- [ ] **Step 5: Details Page Cleanup: Remove Collapsible Company Wrapper & Enrich Artifact Links**
- [ ] **Step 6: Rich Graphic Events Timeline & Event Details Inspection**
- [ ] **Step 7: Scaffold Feature Modules (`features/<flow>/{domain,data,ui}`) & Code Migration**
- [ ] **Step 8: Verification & End-to-End Review**
