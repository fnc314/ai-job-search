---
sessionId: session-261111-200259-13nb
---

# Requirements

### Overview
Expand the `mise roadmap:edit` interactive TUI to support:
1. **Targeted editing** — jump directly to a specific item's detail view via command-line arguments (`i RM-XXX` or `e RM-XXX`)
2. **Keyboard arrow navigation** — use ↑/↓ arrow keys to navigate the item list instead of typing IDs

### Current Behavior
- `mise roadmap:edit` launches the Kotlin TUI (`RoadmapApp.run()` in `Main.kt`)
- The list view displays all items in a table; users must type the item ID (e.g., `RM-009`) to inspect it
- No keyboard arrow navigation exists
- The `edit.sh` script hardcodes `--roadmap` without passing through arguments

### Functional Requirements
1. **Command-line targeted editing**: `mise roadmap:edit i RM-009` or `mise roadmap:edit e RM-009` should open directly in the detail view for that item
2. **Arrow key navigation in list view**: Press ↑/↓ to highlight items in the table, press Enter to inspect the highlighted item
3. **Backwards compatibility**: Typing an item ID at the prompt should still work as before
4. **The `edit.sh` script** must pass through `"$@"` to the TUI binary so arguments reach `RoadmapApp.run()`

### Out of Scope
- Mouse support in the TUI
- Changing the detail view interaction model beyond adding targeted entry

# Technical Design

### Current Implementation
- **Entry point**: `edit.sh` → `apps-terminal --roadmap` (no argument passthrough)
- **Dispatch**: `TerminalApp.run()` (line 1367) checks `--roadmap` flag, calls `RoadmapApp.run()`
- **RoadmapApp** (`Main.kt:827-1317`):
  - `run()` — main loop with `currentView` state machine (`list`, `detail`, `add`, `exit`)
  - `renderListView()` — shows table of items, reads ID via `Scanner.nextLine()`
  - `renderDetailView()` — shows item details, field update commands (`c`, `p`, `d`, `x`, `b`)
  - Uses `java.util.Scanner` for input; Mordant `table {}` and `Panel` for rendering

### Key Decisions
1. **Arrow navigation via escape sequences**: Read raw terminal escape sequences (`ESC [ A` for up, `ESC [ B` for down, `\r` for Enter) instead of `Scanner.nextLine()`. This avoids adding a new dependency since Mordant doesn't expose a built-in arrow-key widget.
2. **Command-line arguments**: Pass `args` through `edit.sh` → `TerminalApp.run()` → `RoadmapApp.run(args)` so `i RM-XXX` / `e RM-XXX` can be parsed before entering the main loop.
3. **Highlighted item tracking**: Add `selectedIndex` state to `RoadmapApp` to track which row is highlighted in the list view.

### Proposed Changes

**File 1: `.config/mise/tasks/roadmap/edit.sh`**
- Change `exec "${TUI_BIN}" --roadmap` to `exec "${TUI_BIN}" --roadmap "$@"` to pass through arguments

**File 2: `frontend/apps/terminal/src/main/kotlin/.../Main.kt`**

1. **`TerminalApp.run()` (line 1367-1374)**: Pass `args` to `RoadmapApp.run(args)` instead of no-arg call

2. **`RoadmapApp.run()` (line 848)**: Accept `args: Array<String>`, parse `i RM-XXX` / `e RM-XXX` to jump directly to detail view

3. **`RoadmapApp.renderListView()` (line 889)**:
   - Add `selectedIndex` state variable
   - Replace `Scanner.nextLine()` with escape-sequence reader for ↑/↓/Enter
   - Re-render the table with the highlighted row (using bold/color on the selected row)
   - Keep typing an ID as a fallback

4. **New helper**: `readKey()` function to read single characters and escape sequences (`ESC [ A` = Up, `ESC [ B` = Down, `\r` = Enter)

### Architecture Diagram
```
mermaid
graph LR
  A[edit.sh] -->|passes args| B[apps-terminal --roadmap $@]
  B --> C[TerminalApp.run args]
  C -->|--roadmap + args| D[RoadmapApp.run args]
  D -->|parse i/e RM-XXX| E[Direct to DetailView]
  D -->|no args| F[ListView with arrow nav]
  F -->|↑/↓/Enter| G[DetailView]
  G -->|back| F
```

# Delivery Steps

### ✓ Step 1: Update edit.sh to pass through arguments
Modify `edit.sh` so command-line arguments reach the TUI binary.
- Change `exec "${TUI_BIN}" --roadmap` to `exec "${TUI_BIN}" --roadmap "$@"` in `.config/mise/tasks/roadmap/edit.sh`
- Update `TerminalApp.run()` in `Main.kt` (line 1372) to pass `args` to `RoadmapApp.run(args)` instead of no-arg call

### ✓ Step 2: Add targeted editing (i/e RM-XXX) and arrow navigation to RoadmapApp
Implement both targeted entry and keyboard navigation in `RoadmapApp` within `Main.kt`.
- Update `RoadmapApp.run()` signature to accept `args: Array<String>`; parse `i RM-XXX` / `e RM-XXX` to skip list view and jump to `renderDetailView`
- Add `selectedIndex` state variable to `RoadmapApp` (line 831)
- Add `readKey()` helper: read escape sequences for ↑ (`ESC [ A`), ↓ (`ESC [ B`), Enter (`\r`), and fallback to `Scanner.nextLine()` for typed IDs
- Update `renderListView()` (line 889): re-render the table with the `selectedIndex` row highlighted (bold + color), handle ↑/↓ to change index, Enter to select
- Keep typing an item ID at the prompt as a fallback for backwards compatibility

### ✓ Step 3: Expand editable fields in detail view
Make all fields except `id` and `title` editable from the detail view.
- Add edit commands for: `s` (shortDesc), `o` (objective), `g` (scope), `a` (acceptanceCriteria), `n` (notes), `k` (complexity), `l` (dependencies)
- Add `m` command for status with a menu: OPEN, IN_PROGRESS, DONE, CANCELLED — allow moving backwards (e.g., DONE → IN_PROGRESS)
- Add `t` command for completion % (0-100); setting 100 auto-marks status as DONE; setting < 100 allows moving status backwards
- Block all edits on FROZEN items (show red banner, only `b` back command allowed)
- Update `renderDetailView()` prompt to show all available commands
- Update `RoadmapWriter.updateField()` to handle multi-line fields (objective, scope, notes, acceptanceCriteria) — rewrite the entire field block rather than regex-replacing a single line

### ✓ Step 4: Support multi-line input for large content blocks
Enable pasting or typing large chunks of content (e.g., copied `.md` file content).
- Add `readMultiLine()` helper that reads from `Scanner` until a delimiter line (e.g., `END`) is encountered
- Use `readMultiLine()` for multi-line fields: objective, scope, notes, acceptanceCriteria, dependencies
- Keep single-line `Scanner.nextLine()` for short fields: shortDesc, criticality, priority, complexity, status, completion %
- Set a reasonable buffer limit (e.g., 64KB) to prevent memory issues

### ✓ Step 5: Support file references for content import
Allow referencing a file path so its content is copied into the field instead of manual pasting.
- When editing a multi-line field, accept `@<path>` as the first line to read content from a file
- Resolve paths relative to the repo root, with absolute path fallback
- Validate the file exists and is readable before importing
- Show a confirmation with the file path and line count before applying
- Update `renderDetailView()` to mention `@file` syntax in the prompt for multi-line fields

### ✓ Step 6: Enhance input handling (case-insensitive ID, enum tab-completion, form validation)
Improve the input experience with better validation and completion.
- Case-insensitive ID input is already implemented (line 1117) — verify and ensure consistency
- Add Tab key completion for enum fields: when editing criticality/priority/status, pressing Tab cycles through valid options
- Add Tab key completion for item IDs in the list view: pressing Tab after partial input auto-completes to the first matching ID
- Enhance form validation: show available options inline, highlight invalid input in red, re-prompt until valid
- Add `readKey()` support for Tab (`ESC [ Z` or `\t`) in the key reader

### ✓ Step 7: Two-dimensional keyboard navigation for multiline content
Enable cursor-based navigation within multiline field editing.
- Replace `Scanner.nextLine()` for multiline fields with a raw terminal editor using `readKey()`
- Support left/right arrow keys to move the cursor horizontally within a line
- Support up/down arrow keys to move between lines
- Support Backspace/Delete for character removal
- Support Enter for new lines, Escape to cancel, Ctrl+D (or a dedicated command) to finish
- Display the current line with a cursor indicator (`>` at cursor position)
- Show line numbers and a mini-map of surrounding context (3 lines above/below)
- This is a significant undertaking — implement a simple line buffer with cursor position tracking

### ✓ Step 8: Full-screen terminal mode
Transform the TUI from a prompt-based interface to a full-screen application.
- Detect terminal dimensions on startup using `System.console()?.reader().read()` or environment variables (`LINES`, `COLUMNS`)
- Clear the screen at the start using ANSI escape codes (`\033[2J\033[H`)
- Render all views (list, detail, add) within the terminal bounds, using the full width
- Add a status bar at the bottom showing current mode, help hints, and item count
- Handle terminal resize signals by re-reading dimensions and re-rendering
- Use Mordant `Panel` widgets to create bordered sections that fill the available space
- Add a header bar with the app title and current view indicator

### ✓ Step 9: Bash/Zsh tab completion for roadmap IDs
Enable shell-level tab completion for `mise roadmap:edit i RM-XXX`.
- Add `#USAGE` spec to `edit.sh` so `mise completion zsh` generates completions
- The completion should list `i` and `e` as subcommands, then complete RM-XXX IDs from ROADMAP.md
- Create a custom completion function `_roadmap_edit` that parses ROADMAP.md for IDs
- Update `completions/generate.sh` or create a separate roadmap completion file
- Test with `mise roadmap:edit i <TAB>` to verify ID completion

### ✓ Step 10: Form validation, read-only fields, and percentage arrow-key editing
Add robust form validation, lock "pure" fields, and enable arrow-key percentage editing.
- **Form field validation**: All edit flows must validate input before accepting:
  - Percentage complete: reject non-numeric input, reject values outside 0-100, reject letters
  - Complexity: reject non-numeric input, reject values outside 0-5
  - Enum fields (criticality, priority, status): reject values not in the valid set
  - Short description: reject empty strings
  - Multi-line fields: reject blank content
- **Read-only "pure" fields**: The following fields must NOT be editable from the detail view:
  - `id` — immutable identifier
  - `title` — immutable name
  - `creationDate` — tracks when the item was created, must remain "pure"
  - `age` — computed from creationDate, inherently read-only
  - `linkedPlans` — auto-tracked from `.agents/plans/README.md`, not manually editable
  - `changelog` — append-only audit trail, not directly editable
- **Percentage arrow-key editing**: Replace the text-input-based percentage editor with a key-based editor:
  - Up arrow: increment by 1
  - Down arrow: decrement by 1
  - SHIFT+Up: increment by 10
  - SHIFT+Down: decrement by 10
  - OPTION+Up: increment by 5
  - OPTION+Down: decrement by 5
  - Enter: confirm and save
  - Escape: cancel
  - Direct numeric input still allowed for setting exact values
  - Display current value prominently, re-render on each key press

### ✓ Bug Fix: Scanner input buffering conflict with Mordant raw mode
Fixed arrow keys not working in percentage editor (and all other TUI input).
- **Root cause**: `RoadmapApp` created `Scanner(System.in)` which buffered input, conflicting with Mordant's `enterRawMode()` that uses `System.in.read()` directly. The Scanner's buffer consumed bytes before Mordant's `readRawByte()` could read them, causing raw escape sequences to be echoed instead of parsed.
- **Fix**: Close `scanner` before entering raw mode, replace all `scanner.nextLine()` calls in RoadmapApp with `readSingleLine()` (which uses `readKey()` via Mordant's raw mode API)
- **Files changed**: `Main.kt` — added `scanner.close()` before `enterRawMode()`, replaced Scanner usage in `renderListView()`, `renderDetailView()`, `renderAddForm()`, and `handleClearRoadmap()`

### ✓ Bug Fix: FFM terminal interface fails on macOS
Fixed `RuntimeException: failed to read terminal settings` when launching `mise roadmap:edit`.
- **Root cause**: Mordant's ServiceLoader selects terminal interface providers in order: FFM first, then JNA. The FFM provider (`mordant-jvm-ffm-jvm`) requires JVM 22+ with `--enable-native-access=ALL-UNNAMED`. On older JVMs or without the flag, FFM's `load()` succeeds but `getStdinTermios()` throws `RuntimeException` at runtime.
- **Fix**: Exclude `mordant-jvm-ffm-jvm` from all transitive dependencies in `build.gradle.kts` using `configurations.all { exclude(...) }` so the JNA provider is used instead.
- **Files changed**: `frontend/apps/terminal/build.gradle.kts` — added `configurations.all { exclude(group = "com.github.ajalt.mordant", module = "mordant-jvm-ffm-jvm") }`

### ✓ Bug Fix: JNA terminal interface fails on Ghostty
Fixed `LastErrorException: [19] Operation not supported by device` when running `mise roadmap:edit` in Ghostty terminal.
- **Root cause**: Ghostty (and some other modern terminal emulators) don't support traditional POSIX `tcgetattr`/`tcsetattr` syscalls via JNA/FFM. Both Mordant's native terminal interfaces fail, making raw mode impossible through the library.
- **Fix**: Wrap `t.enterRawMode()` in try-catch. On failure, fall back to `stty` command (`stty -f /dev/tty -echo -icanon -isig min 0 time 0`) to set raw mode, and use manual escape sequence parsing (`readKeyFallback()`) instead of Mordant's `KeyboardEvent`. Terminal is restored on exit via `stty -f /dev/tty echo icanon isig`.
- **Files changed**: `Main.kt` — added `useFallbackRawMode` flag, `fallbackSttyRestore`, try-catch around `enterRawMode()`, `readKeyFallback()` for manual escape sequence parsing, `readRawByte()` helper, and cleanup in `finally` block

### ✓ Bug Fix: Infinite loop and Ctrl+C not working in detail view
Fixed infinite screen flash loop in detail view where Ctrl+C could not exit the TUI.
- **Root cause**: `readKey()` treated Ctrl+C as `"enter"` (line 1077: `if (event.isCtrlC) return "enter"`). In the detail view, pressing Ctrl+C (or pressing Enter with empty input) returned an empty string from `readSingleLine()`, which didn't match any command, causing `renderDetailView` to return `"detail"`. The main loop then stayed in detail view, cleared the screen, and called `renderDetailView` again — creating an infinite flash loop. The fallback `readKeyFallback()` didn't handle Ctrl+C (byte 3) at all.
- **Fix**: `readKey()` now returns `"ctrl_c"` for Ctrl+C. `readSingleLine()`, `readNumericLine()`, and `readInputLineWithTab()` propagate `"exit"` on Ctrl+C. All callers (`renderDetailView`, `renderListView`, `updateShortDesc`, `updateComplexity`, `updateCompletion`, `updateEnumField`, `updateEnumFieldWithAlias`) handle `"exit"` to either exit the TUI or return from the editor. Main loop handles `"exit"` from `renderListView` and `renderDetailView` to cleanly exit.
- **Files changed**: `Main.kt` — `readKey()` returns `"ctrl_c"`, `readKeyFallback()` handles byte 3, `readSingleLine()` returns `"exit"`, `readNumericLine()` returns `"exit"`, `readInputLineWithTab()` returns `"exit"`, all callers handle `"exit"`, main loop exits on `"exit"` from list/detail views

### ✓ Refactor: Split Main.kt into subpackages
Refactored the monolithic `Main.kt` (3550+ lines) into focused subpackages for maintainability.
- **`roadmap/RoadmapApp.kt`** (2047 lines): `RoadmapItem`, `RoadmapChangelogEntry`, `RoadmapParser`, `RoadmapWriter`, `RoadmapApp`
- **`tracker/TrackerApp.kt`** (1652 lines): `ApplicationRecord`, `TimelineEventRecord`, `ProposalRecord`, `EmailRecord`, `EmailsApp`, `TerminalApp`
- **`Main.kt`** (16 lines): Minimal dispatcher — `main()` calls `TerminalApp.run(args)`
- **`TerminalAppTest.kt`**: Updated imports to use `dev.fnc314.tools.jobtracker.terminal.tracker` package
- **Build**: `./gradlew :frontend:apps-terminal:installDist` — BUILD SUCCESSFUL, binary Nov 11 21:15

### ✓ Bug Fix: stty non-blocking read causes infinite loop on Ghostty
Fixed infinite screen flash loop and Ctrl+C not working when using `stty` fallback raw mode on Ghostty terminal.
- **Root cause**: `stty min 0 time 0` makes `System.in.read()` return `-1` immediately when no data is available. The `readRawByte()` function caught this as an exception and returned `null`, causing `readKeyFallback()` to return `"enter"`. This made `readSingleLine()` return an empty string instantly without waiting for input, creating a tight loop where the detail view re-rendered continuously.
- **Fix**: Changed `stty` settings from `min 0 time 0` to `min 1 time 0` so `read()` blocks until at least one byte is available. Updated `readRawByte()` to poll with a 20ms sleep and 1-second timeout as a safety net for edge cases.
- **Files changed**: `roadmap/RoadmapApp.kt` — `stty` command uses `min 1`, `readRawByte()` polls on `-1` with timeout