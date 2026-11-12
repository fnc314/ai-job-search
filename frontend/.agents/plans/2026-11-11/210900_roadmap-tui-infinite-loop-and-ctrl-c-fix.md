# Bug Fix Plan: Roadmap TUI Infinite Loop and Ctrl+C Fix

**Status:** Completed
**Date:** 2026-11-11
**Target:** `frontend/apps/terminal/src/main/kotlin/.../Main.kt` (RoadmapApp)

---

## Problem

Running `mise roadmap:edit` causes an infinite screen flash loop in the detail view. The screen rapidly redraws the first item's details, and Ctrl+C cannot break out of the loop.

## Root Cause

`readKey()` treated Ctrl+C as `"enter"` (`if (event.isCtrlC) return "enter"`). In the detail view, pressing Ctrl+C caused `readSingleLine()` to return an empty string, which didn't match any command, so `renderDetailView` returned `"detail"`. The main loop stayed in detail view, cleared the screen, and called `renderDetailView` again — creating an infinite flash loop.

Additionally, `readKeyFallback()` (used when Mordant's native interfaces fail, e.g., on Ghostty) didn't handle Ctrl+C (byte 3) at all.

## Solution

1. **`readKey()`**: Return `"ctrl_c"` instead of `"enter"` for Ctrl+C
2. **`readKeyFallback()`**: Handle byte 3 as `"ctrl_c"`
3. **`readSingleLine()`**: Return `"exit"` on Ctrl+C
4. **`readNumericLine()`**: Return `"exit"` on Ctrl+C
5. **`readInputLineWithTab()`**: Return `"exit"` on Ctrl+C
6. **All callers**: Handle `"exit"` to either exit the TUI or return from the editor
7. **Main loop**: Handle `"exit"` from `renderListView` and `renderDetailView` to cleanly exit

## Changes Made

- **`Main.kt` — `readKey()`**: Changed `if (event.isCtrlC) return "enter"` to `return "ctrl_c"`
- **`Main.kt` — `readKeyFallback()`**: Added `if (c1 == 3) return "ctrl_c"`
- **`Main.kt` — `readSingleLine()`**: Added `key == "ctrl_c" -> return "exit"`
- **`Main.kt` — `readNumericLine()`**: Added `key == "ctrl_c" -> return "exit"`
- **`Main.kt` — `readInputLineWithTab()`**: Added `key == "ctrl_c" -> return "exit"`
- **`Main.kt` — `renderDetailView()`**: Changed `line == "exit"` to return `"exit"` (was `"back"`)
- **`Main.kt` — `renderListView()`**: Added `key == "ctrl_c" -> return "exit"`
- **`Main.kt` — main loop**: Handle `"exit"` from list and detail views to cleanly exit
- **`Main.kt` — `updateShortDesc()`**: Handle `value == "exit"`
- **`Main.kt` — `updateComplexity()`**: Handle `key == "ctrl_c"`
- **`Main.kt` — `updateCompletion()`**: Handle `key == "ctrl_c"` and `rest == "exit"`
- **`Main.kt` — `updateEnumField()`**: Handle `input == "exit"`
- **`Main.kt` — `updateEnumFieldWithAlias()`**: Handle `input == "exit"`

## Verification

- `./gradlew :frontend:apps-terminal:installDist` — BUILD SUCCESSFUL
- New binary timestamped Nov 11 21:09

---

## Follow-up: Refactor Main.kt into Subpackages

After the Ctrl+C fix, the monolithic `Main.kt` (3550+ lines) was refactored into focused subpackages:

### Changes

- **`roadmap/RoadmapApp.kt`** (new, 2047 lines): Contains `RoadmapItem`, `RoadmapChangelogEntry`, `RoadmapParser`, `RoadmapWriter`, and `RoadmapApp` object — all roadmap TUI code
- **`tracker/TrackerApp.kt`** (new, 1652 lines): Contains `ApplicationRecord`, `TimelineEventRecord`, `ProposalRecord`, `EmailRecord`, `EmailsApp`, and `TerminalApp` — all job tracker TUI code
- **`Main.kt`** (reduced to 16 lines): Minimal dispatcher that imports `TerminalApp` and calls `TerminalApp.run(args)`
- **`TerminalAppTest.kt`**: Updated imports to reference `dev.fnc314.tools.jobtracker.terminal.tracker` package

### Key Decisions

- `TerminalApp.run()` in `tracker/TrackerApp.kt` dispatches to `RoadmapApp.run()` when `--roadmap` flag is present, keeping the dispatch logic centralized
- Data classes remain `public` so tests and other modules can access them
- Package structure: `dev.fnc314.tools.jobtracker.terminal.roadmap` and `dev.fnc314.tools.jobtracker.terminal.tracker`

### Verification

- `./gradlew :frontend:apps-terminal:installDist` — BUILD SUCCESSFUL
- New binary timestamped Nov 11 21:15
