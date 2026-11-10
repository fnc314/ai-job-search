# Roadmap Terminal TUI

**Canonical Plan:** [`144900_roadmap-terminal-tui.md`](../.agents/plans/2026-11-10/144900_roadmap-terminal-tui.md) (Status: Completed)

This file tracks Junie execution progress for the roadmap terminal TUI implementation. The canonical architecture and implementation plan lives in `.agents/plans/`.

---

## Validation Approach
The implementation will be validated by:
1. Building the terminal app (`./gradlew :frontend:apps-terminal:installDist`) and confirming no compilation errors
2. Running `mise roadmap:edit` and verifying the interactive TUI launches
3. Running `mise roadmap:status` and verifying aggregate data output
4. Testing each interactive flow (list, detail, add, update, clear)

## Key Scenarios
1. **Launch roadmap TUI:** `mise roadmap:edit` → shows list of existing ROADMAP items
2. **Inspect item:** Select an item → shows detail view with linked plans, status, changelog
3. **Add item:** Press `+` → form appears → fill Name, Description, Criticality, Priority → short desc generated → accept/edit → Create → item appears in list
4. **Update criticality:** In detail view → change criticality → saved to ROADMAP.md with changelog entry
5. **Mark complete:** In detail view → mark complete → status changes, completion % set to 100
6. **Clear roadmap:** Type `clear` → 3 confirmations → reason (50+ chars) → all items frozen, archived
7. **Status command:** `mise roadmap:status` → shows counts by status, criticality distribution, oldest item age

## Edge Cases
- Empty ROADMAP.md (no items) → show empty state with option to add
- Malformed ROADMAP.md entry → skip with warning, don't crash
- ROADMAP.md exceeds size limit → warn user, suggest archival
- Clear with only 1 item → still require all speedbumps
- Reason too short (<50 chars) → reject, ask again
- Linked plans not found → show "No linked plans" gracefully
- Creation date missing (legacy items) → estimate or use current date with note

## Test Changes
- Add unit tests for `RoadmapParser` (parse valid/invalid markdown)
- Add unit tests for `RoadmapWriter` (write items, preserve format)
- Extend `TerminalAppTest.kt` with roadmap mode tests

---

## Delivery Steps

### ✓ Step 1: Rename TODO.md to ROADMAP.md and update format
Rename the file and add new fields to existing items.
- Rename `TODO.md` to `ROADMAP.md`
- Update frontmatter: change title, update scales to use Criticality alongside Priority
- Add `Short Desc`, `Creation Date`, `Completion %` fields to each existing item
- Generate short descriptions for existing items from their titles
- Set creation dates (estimate from context or use placeholder)
- Add frozen items section template at bottom
- Update Table of Contents to include new columns
- Update all references in `.agents/plans/README.md` and other files that reference TODO.md

### ✓ Step 2: Implement RoadmapParser and RoadmapWriter in Main.kt
Add parsing and writing logic for ROADMAP.md format.
- Add `RoadmapItem` and `ChangelogEntry` data classes to `Main.kt`
- Implement `RoadmapParser` object: parse ROADMAP.md into `List<RoadmapItem>`
  - Parse Table of Contents for quick overview
  - Parse each item block for full details
  - Scan `.agents/plans/README.md` for linked plans referencing each item ID
  - Handle frozen items separately
- Implement `RoadmapWriter` object: write `List<RoadmapItem>` back to ROADMAP.md
  - Preserve markdown format
  - Update Table of Contents
  - Append changelog entries
  - Handle frozen items section
- Add file size guard: warn if ROADMAP.md exceeds ~50KB

### ✓ Step 3: Implement RoadmapApp interactive TUI mode
Add the interactive roadmap management experience to the terminal app.
- Add `RoadmapApp` object in `Main.kt` with:
  - `run()` entry point (dispatched from `TerminalApp.run()` via `--roadmap` flag)
  - List view: table showing ID, Name, Short Desc, Criticality, Age (days)
  - Detail view: full item details, linked plans, completion %, changelog
  - Action buttons: update criticality, update priority, mark complete, mark cancelled
  - Add item form: Name, Description (long), Criticality, Priority inputs
  - Short description generation: template-based with interactive edit/accept/replace
  - Auto-generate ID (next available number) and creation date
  - Create/Cancel buttons (Create only enabled when form is valid and short desc is done)
  - Clear roadmap: 3 confirmations (CLEAR → CLEAR ROADMAP → CLEAR ALL) + reason (50+ chars) → freeze all items
  - Navigation: back to list from detail, quit
- Update `TerminalApp.run()` to dispatch `--roadmap` flag to `RoadmapApp.run()`
- Follow existing Mordant patterns: `table {}`, `Panel`, colors, interactive loop with `Scanner`

### ✓ Step 4: Add mise tasks for roadmap:edit and roadmap:status
Create mise task runner scripts for roadmap operations.
- Create `.config/mise/tasks/roadmap/edit.sh`:
  - `#MISE description="Open the interactive roadmap manager (Kotlin TUI)"`
  - Launches `mise run tui --roadmap`
- Create `.config/mise/tasks/roadmap/status.sh`:
  - `#MISE description="Display aggregate roadmap data"`
  - Parse ROADMAP.md and print: total items, counts by status, counts by criticality, oldest item age, completion summary
  - Use Python for parsing (consistent with other tracker tasks)
  - Include frozen items count separately
- Test both tasks via `mise roadmap:edit` and `mise roadmap:status`