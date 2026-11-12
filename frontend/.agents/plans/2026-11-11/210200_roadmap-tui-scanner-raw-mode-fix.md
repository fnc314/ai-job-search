# Bug Fix Plan: Roadmap TUI Scanner Raw Mode Conflict

**Status:** Completed
**Date:** 2026-11-11
**Target:** `frontend/apps/terminal/src/main/kotlin/.../Main.kt` (RoadmapApp)

---

## Problem

Arrow keys (↑/↓) did not work in the `mise roadmap:edit` TUI percentage editor or any other input flow. Instead of changing the value, raw escape sequences (`^[[A^[[B`) were echoed to the terminal.

## Root Cause

`RoadmapApp` created a `Scanner(System.in)` at class initialization time. Java's `Scanner` wraps `System.in` with an internal buffered reader. When Mordant's `enterRawMode()` was called, it used `System.in.read()` directly via the JNA `TerminalInterface`. However, the Scanner's buffer had already consumed bytes from the underlying stream, so Mordant's `readRawByte()` either got stale data or the terminal driver was still in cooked mode for the Scanner's file descriptor.

The fix was twofold:
1. Close the Scanner before entering raw mode to release its hold on `System.in`
2. Replace all `scanner.nextLine()` calls in RoadmapApp with `readSingleLine()` which uses `readKey()` via Mordant's raw mode API

## Changes Made

- **`Main.kt`**: Added `scanner.close()` before `t.enterRawMode()` in `RoadmapApp.run()`
- **`Main.kt`**: Replaced `scanner.nextLine()` with `readSingleLine()` in:
  - `renderListView()` — empty list prompt
  - `renderDetailView()` — frozen item prompt, edit command prompt
  - `renderAddForm()` — all field inputs (name, description, criticality, priority, short desc choice, confirm)
  - `handleClearRoadmap()` — all confirmation prompts and reason input
- **`Main.kt`**: Fixed unused expression warnings (`key == "q"` and `key == "+"` branches now use `return`)

## Verification

- `./gradlew :frontend:apps-terminal:installDist` — BUILD SUCCESSFUL, zero warnings
- New binary timestamped Nov 11 21:02
