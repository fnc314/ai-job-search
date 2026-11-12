# Bug Fix Plan: Roadmap TUI Ghostty Terminal Compatibility

**Status:** Completed
**Date:** 2026-11-11
**Target:** `frontend/apps/terminal/src/main/kotlin/.../Main.kt` (RoadmapApp)

---

## Problem

Running `mise roadmap:edit` in Ghostty terminal (v1.4.0) fails with:
```
Exception in thread "main" com.sun.jna.LastErrorException: [19] Operation not supported by device
        at com.github.ajalt.mordant.terminal.terminalinterface.jna.TerminalInterfaceJnaMacos.getStdinTermios(...)
        at com.github.ajalt.mordant.terminal.terminalinterface.TerminalInterfacePosix.enterRawMode(...)
```

Error 19 is `ENODEV` (Operation not supported by device) — the JNA `tcgetattr` syscall fails because Ghostty doesn't support traditional POSIX terminal control via JNA/FFM.

## Root Cause

Ghostty uses a custom terminal emulator backend that doesn't support `tcgetattr`/`tcsetattr` syscalls through JNA or FFM (Foreign Function Memory). Both of Mordant's native terminal interface providers fail, making raw mode impossible through the library alone.

## Solution

Wrap `t.enterRawMode()` in a try-catch. On failure, fall back to:
1. **`stty` command** to set raw mode: `stty -f /dev/tty -echo -icanon -isig min 0 time 0`
2. **Manual escape sequence parsing** via `readKeyFallback()` instead of Mordant's `KeyboardEvent`
3. **Terminal restore** on exit: `stty -f /dev/tty echo icanon isig`

## Changes Made

- **`Main.kt`**: Added `useFallbackRawMode` flag and `fallbackSttyRestore` variable to `RoadmapApp`
- **`Main.kt`**: Wrapped `t.enterRawMode()` in try-catch; on failure, run `stty` to set raw mode
- **`Main.kt`**: Added `readKeyFallback()` — manual escape sequence parser (↑/↓/←/→, Tab, Backspace, Delete, Ctrl+D, Shift/Option modifiers)
- **`Main.kt`**: Added `readRawByte()` helper for blocking `System.in.read()`
- **`Main.kt`**: Updated `readKey()` to delegate to `readKeyFallback()` when `useFallbackRawMode` is true
- **`Main.kt`**: Added terminal restore in `finally` block when using fallback mode
- **`build.gradle.kts`**: Fixed `projects.core.data` → `project(":core-data")` (typesafe accessor not available)
- **`build.gradle.kts`**: Updated comment to note Ghostty fallback

## Verification

- `./gradlew :frontend:apps-terminal:installDist` — BUILD SUCCESSFUL
- New binary timestamped Nov 11 21:07
