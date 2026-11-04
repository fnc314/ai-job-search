# Material 3 Theming, Adaptive Layouts, Persistence & Widget Enhancements

**Status:** In Progress  
**Author:** Antigravity  
**Date:** 2026-11-04  

## Objective
Implement comprehensive Material Design 3 enhancements across the KMP Job Tracker shared UI:
1. Persistent configuration choices regarding UI Mode (Light/Dark/System) and/or Seed color.
2. Full-fledged Material Theme key tokens around Typography, Spacing, and Shapes in `:core:design-system`.
3. Explicit screen-size reactivity across screen size classes (Compact, Medium, Expanded) and unique treatments per breakpoint.
4. Thorough application of `m3.material.io` guidelines and standard widgets in proper places (M3 icons, tonal badges, scrollable chips, cleanup).
5. Fix content obscured by the FAB at the bottom of the main applications list.
6. Fix focus movement in `NotesWidget` so that when a new note is added, focus immediately moves to the newly added empty input.

---

## Plan Checklist

- [ ] **Step 1: Core Design System Tokens (Typography, Spacing, Shapes, Color Schemes)**
- [ ] **Step 2: Persistent Settings Configuration (UiMode & Seed Color)**
- [ ] **Step 3: Screen-Size Reactivity & Breakpoints (WindowSizeClass & Layout Treatments)**
- [ ] **Step 4: Material 3 Widget Polish (M3 Icons, Tonal Badges, Scrollable Chips, Cleanups)**
- [ ] **Step 5: Fix FAB Covering Content at Bottom of List**
- [ ] **Step 6: NotesWidget Focus Movement & Note Creation Fix**
- [ ] **Step 7: Verification and Final Polish**

---

## Detailed Step Breakdown

### Step 1: Core Design System Tokens
- Create comprehensive `Typography.kt` in `:core:design-system` covering all 15 M3 typography roles (`display*`, `headline*`, `title*`, `body*`, `label*`) respecting fixed-width monospace headings and sans-serif body text.
- Expand `Spacing.kt` with `none`, `hairline`, `extraExtraLarge`, and `fabBottomPadding`.
- Define `Shapes.kt` with standard M3 corner tokens (`extraSmall = 4.dp`, `small = 8.dp`, `medium = 12.dp`, `large = 16.dp`, `extraLarge = 28.dp`, `full = CircleShape`).
- Enhance `Theme.kt` with harmonious M3 light and dark color schemes (tonal surfaces, containers, outline, error, primary/secondary/tertiary).

### Step 2: Persistent Settings Configuration
- Define `UiMode` enum (`SYSTEM`, `LIGHT`, `DARK`) in `AppSettings`.
- Implement `SettingsStorage` multiplatform abstraction (`expect`/`actual`) using:
  - Android: `SharedPreferences` via `AndroidApplicationContextHolder`
  - JVM: `java.util.prefs.Preferences`
  - iOS: `NSUserDefaults`
  - JS / WasmJS: `window.localStorage`
- Connect `SettingsStorage` to `AppSettingsRepository` / `App.kt` so theme, seed color, and settings persist across restarts.
- Enhance `SettingsDialog.kt` with curated M3 color palette swatches and `UiMode` selector.

### Step 3: Screen-Size Reactivity & Breakpoints
- Implement `WindowSizeClass` (`Compact`, `Medium`, `Expanded`) in `:core:design-system`.
- Provide responsive layout treatments:
  - **Compact (<600dp):** Single-column list, horizontal chip scrolling, standard top bar.
  - **Medium (600-840dp):** 2-column grid/table layout, paired form fields.
  - **Expanded (>=840dp):** Responsive two-pane / wide layout, centered max-width form containers (max 720-800dp).

### Step 4: Material 3 Widget Polish
- Replace raw emojis (`⏳`, `🔄`) in `TrackerEditsScreen.kt` with standard M3 vector icons.
- Replace `"▲"` and `"▼"` text in `ColumnHeader.kt` with M3 vector arrow icons.
- Remove hardcoded debug red borders in `JobApplicationRow.kt`.
- Upgrade `StatusBadge.kt` to an M3 tonal badge container with `Surface` and matching container/content colors.
- Make filter chips row in `JobApplicationsListScreen.kt` horizontally scrollable.

### Step 5: Fix FAB Covering Content at Bottom of List
- Adjust `LazyColumn` `contentPadding` in `JobApplicationsListScreen.kt` with bottom padding (88.dp) to guarantee the last items scroll completely clear of the FAB.

### Step 6: NotesWidget Focus Movement & Note Creation Fix
- Add explicit "Add note" action in the `NotesWidget` header.
- Fix focus requester attachment lifecycle in `NoteRow` using frame-aware retry so focus attaches reliably to the newly spawned input.
- Support Enter key and `+` button in any note to commit and advance focus to the empty note.

### Step 7: Verification and Final Polish
- Ensure all modules compile cleanly across targets.
- Verify end-to-end functionality of theme persistence, responsive breakpoints, list padding, and notes focus.
