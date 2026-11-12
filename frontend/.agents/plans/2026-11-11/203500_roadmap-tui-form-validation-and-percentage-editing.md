# Implementation Plan: Roadmap TUI Form Validation, Read-Only Fields, and Percentage Arrow Editing

**Status:** Completed  
**Date:** 2026-11-11  
**Target:** `frontend` (apps-terminal TUI)  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261111-200259-13nb  

---

## 1. Executive Summary & Goals

Enhance the `mise roadmap:edit` interactive TUI (Kotlin, `Main.kt`) with three improvements:

1. **Form field validation** — all editing flows reject invalid input (e.g., letters in percentage fields, out-of-range values, empty strings where disallowed).
2. **Read-only "pure" fields** — fields that track immutable data (`id`, `title`, `creationDate`, `age`, `linkedPlans`, `changelog`) are marked `[read-only]` and have no edit command.
3. **Percentage arrow-key editing** — the completion percentage editor supports ↑/↓ (±1%), Shift+↑/↓ (±10%), and Option+↑/↓ (±5%), with direct numeric input as a fallback.

## 2. Architecture & Design Principles

- All input validation happens at the editor level, before any file write.
- Read-only fields are visually indicated in the detail view and excluded from the edit command list.
- The `readKey()` function is extended to parse macOS modifier escape sequences (`ESC [ 1 ; 2 A` = Shift+Up, `ESC [ 1 ; 3 A` = Option+Up).
- A new `readNumericLine()` helper rejects non-digit characters as they are typed, providing immediate feedback.

## 3. Proposed Changes & Implementation Steps

### File: `frontend/apps/terminal/src/main/kotlin/.../Main.kt`

**1. Enhanced `readKey()` function** (line ~1019)
- Added support for macOS modifier escape sequences: `ESC [ 1 ; <modifier> <arrow>`
- Modifier codes: 2=Shift, 3=Option, 4=Ctrl, 5=Shift+Option
- Returns prefixed strings like `"shift_up"`, `"opt_down"`, etc.
- Properly handles the conflict between `ESC [ 1 ~` (Delete) and `ESC [ 1 ; 2 A` (Shift+Up) by checking for `;` after `1`.

**2. Rewrote `updateCompletion()` function** (line ~1475)
- Replaced text-input-based editor with a key-based interactive editor
- Arrow keys: ↑/↓ increment/decrement by 1%
- Shift+↑/↓: increment/decrement by 10%
- Option+↑/↓: increment/decrement by 5%
- Direct numeric input via `readNumericLine()` for exact values
- Non-numeric characters rejected immediately with a red error message
- Values clamped to 0-100 range
- Enter saves, Escape cancels
- Extracted `saveCompletion()` helper for the file write logic

**3. Added `readNumericLine()` helper** (line ~1665)
- Reads input character by character using `readKey()`
- Accepts only digits (0-9), Backspace, Enter, and Escape
- Rejects non-digit characters immediately with inline error feedback
- Returns the typed digit string, "escape" on cancel, or empty on EOF

**4. Enhanced `updateComplexity()` validation** (line ~1594)
- Added explicit letter rejection: typing a letter shows "Invalid: letters not allowed. Must be a digit 0-5."
- Non-digit, non-letter characters show the generic "Invalid complexity. Must be 0-5."

**5. Detail view read-only indicators** (line ~1260)
- Added `gray(" [read-only]")` suffix to ID, Title, Created, and Age fields
- Added ID field display (was previously hidden)
- Edit command list now shows `(ID, Title, Created, Age are read-only)` in gray
- Completion % command hint updated to show arrow key shortcuts

**6. Status bar hint update** (line ~1011)
- Detail view status bar now mentions `t: ↑/↓ ±1% Shift±10% Opt±5%`

### No Changes Required
- `updateCriticality()`, `updatePriority()`, `updateStatus()` — already have enum validation via `updateEnumField()` / `updateEnumFieldWithAlias()` with re-prompt on invalid input.
- `updateShortDesc()` — already validates non-empty strings with re-prompt.
- `updateMultiLineField()` — already validates non-blank content.
- Frozen items — already block all edits except `b` (back).

## 4. Verification Plan & Testing Checklist

- [x] Compilation: all errors are pre-existing proto-module issues — zero new errors introduced
- [ ] Manual test: open `mise roadmap:edit`, navigate to an item, press `t` for completion
  - Verify ↑/↓ change by ±1%
  - Verify Shift+↑/↓ change by ±10%
  - Verify Option+↑/↓ change by ±5%
  - Verify typing a letter shows rejection message
  - Verify typing a number and Enter sets exact value
  - Verify Enter saves, Escape cancels
- [ ] Manual test: press `k` for complexity, verify letter rejection
- [ ] Manual test: verify ID, Title, Created, Age show `[read-only]` in detail view
- [ ] Manual test: verify no edit command exists for ID, Title, Created, or Age

## 5. Bookkeeping
- [x] Plan persisted at `frontend/.agents/plans/2026-11-11/203500_roadmap-tui-form-validation-and-percentage-editing.md`
- [x] Registry `README.md` row added and status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message
- [ ] Push `git push origin main`
