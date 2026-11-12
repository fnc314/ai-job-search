---
sessionId: session-261111-212905-1dwa
---

# Requirements

### Overview
The `mise tracker list` CLI command (and `mise tracker get`) renders tables that don't use the full terminal width, making them hard to read. The table columns are sized to fit content only, and the terminal width defaults to 72 columns on JVM.

### Root Causes
1. **Mordant defaults to 72 columns** on JVM when the `COLS` environment variable is not set (which most terminals don't). The table is constrained to this narrow width.
2. **No column expansion** — none of the columns have `expandWeight` set, so even if the terminal width were detected correctly, the table would still only be as wide as the content requires.

### Functional Requirements
- `mise tracker list` table should expand to fill the full terminal width.
- `mise tracker get <id>` events table should also expand to fill the full terminal width.
- The `Company` and `Role` columns (in list) and `Notes` column (in get) should expand to consume available space.
- Short columns (`ID`, `Status`, `Fit`, `Emails`, `Date/Time`, `Event Type`, `Actor`, `Email Message ID`) should remain content-sized.

### Scope
- **In Scope:** `ListApplicationsCommand.kt` and `GetApplicationCommand.kt` table rendering.
- **Out of Scope:** Other CLI commands, TUI, or web dashboard.

# Technical Design

### Current Implementation
- `ListApplicationsCommand.kt` (line 60–87) and `GetApplicationCommand.kt` (line 54–69) use mordant's `table {}` DSL without any column width configuration.
- Mordant 3.0.2 is used (see `gradle/libs.versions.toml`).
- The `Terminal()` instance is created without calling `updateTerminalSize()`, so on JVM the width defaults to 72 columns.

### Key Decisions
1. **Call `t.info.updateTerminalSize()`** before each table render to detect the actual terminal width. This is the recommended fix from the mordant library author.
2. **Set `expandWeight` on wide columns** using `ColumnWidth(priority = 0, expandWeight = 1f)` so they fill remaining terminal space.

### Proposed Changes

**`ListApplicationsCommand.kt`:**
- Add `import com.github.ajalt.mordant.table.ColumnWidth`
- Add `t.info.updateTerminalSize()` before the `table {}` call
- Add `column(1)` (Company) and `column(2)` (Role) with `width = ColumnWidth(priority = 0, expandWeight = 1f)`

**`GetApplicationCommand.kt`:**
- Add `import com.github.ajalt.mordant.table.ColumnWidth`
- Add `t.info.updateTerminalSize()` before the `table {}` call
- Add `column(4)` (Notes) with `width = ColumnWidth(priority = 0, expandWeight = 1f)`

# Delivery Steps

### ✓ Step 1: Fix ListApplicationsCommand table width
Fix `ListApplicationsCommand.kt` to render tables at full terminal width.

- The `ColumnWidth(priority = 0, expandWeight = 1f)` constructor was incorrect for Mordant 3.x — the proper API is `ColumnWidth.Expand(1f)` (priority 1) for expanding columns and `ColumnWidth.Fixed(n)` for fixed-width columns.
- Set the ID column (column 0) to `ColumnWidth.Fixed(40)` so it always displays without consuming all available width.
- Set Company (column 1) and Role (column 2) to `ColumnWidth.Expand(1f)` so they fill remaining terminal space.
- Pass `nonInteractiveWidth = 200` to the `Terminal` constructor so the table renders at a reasonable width when stdout is piped (e.g., through Gradle), instead of defaulting to 79 columns.
- Import `ColumnWidth` from `com.github.ajalt.mordant.table`.

### ✓ Step 2: Fix GetApplicationCommand table width
Apply the same fix to `GetApplicationCommand.kt`'s events table.

- Pass `nonInteractiveWidth = 200` to the `Terminal` constructor.
- Set the Date/Time column (column 0) to `ColumnWidth.Fixed(25)` so it displays timestamps without consuming all available width.
- Set the Notes column (column 4) to `ColumnWidth.Expand(1f)` so it fills remaining terminal space.
- Import `ColumnWidth` from `com.github.ajalt.mordant.table`.
- Verify both commands compile and render correctly.