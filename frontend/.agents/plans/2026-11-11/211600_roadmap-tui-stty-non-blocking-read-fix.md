# Bug Fix Plan: Roadmap TUI stty non-blocking read infinite loop

**Status:** Completed
**Date:** 2026-11-11
**Target:** `frontend/apps/terminal/src/main/kotlin/dev/fnc314/tools/jobtracker/terminal/roadmap/RoadmapApp.kt`

---

## Problem

After refactoring `Main.kt` into subpackages, `mise roadmap:edit` still exhibited an infinite screen flash loop on Ghostty terminal, and Ctrl+C could not exit the TUI.

## Root Cause

The `stty` fallback raw mode used `min 0 time 0`, which makes `System.in.read()` return `-1` immediately when no data is available. The `readRawByte()` function treated this as an error and returned `null`, causing `readKeyFallback()` to return `"enter"`. This made every input function (`readSingleLine()`, `readNumericLine()`, etc.) return instantly without waiting for user input, creating a tight loop:

1. `readKeyFallback()` → `readRawByte()` returns `null` → returns `"enter"`
2. `readSingleLine()` sees `"enter"` → returns empty string `""`
3. `renderDetailView()` sees `""` → no matching command → returns `"detail"`
4. Main loop keeps `currentView = "detail"`, clears screen, calls `renderDetailView()` again
5. Repeat infinitely — screen flashes, Ctrl+C never reaches the handler

## Fix

1. **Changed `stty` settings**: `min 0 time 0` → `min 1 time 0` so `read()` blocks until at least one byte is available
2. **Updated `readRawByte()`**: Added polling loop with 20ms sleep and 1-second timeout as a safety net for edge cases where `-1` is still returned

## Files Changed

- `roadmap/RoadmapApp.kt` — `stty` command uses `min 1`, `readRawByte()` polls on `-1` with timeout

## Verification

- `./gradlew :frontend:apps-terminal:installDist` — BUILD SUCCESSFUL, binary Nov 11 21:16
- Manual testing required: run `mise roadmap:edit` in Ghostty to verify the TUI responds to input and Ctrl+C exits cleanly
