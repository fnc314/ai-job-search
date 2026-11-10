# Implementation Plan: Roadmap Terminal TUI

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** `frontend` and tracker (cross-subsystem)  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261110-142347-qlx2  
**Depends On:** [`141800_reformat-todo-md.md`](./2026-11-10/141800_reformat-todo-md.md)  

---

## 1. Executive Summary & Goals

Replace the static `TODO.md` file with an interactive roadmap management experience backed by `ROADMAP.md`. The goal is to provide two `mise` tasks:

- `mise roadmap:edit` — launches a Kotlin Mordant-based interactive TUI for managing roadmap items (list, detail, add, update, clear).
- `mise roadmap:status` — displays aggregate roadmap data (counts by status/criticality/priority, oldest item age, completion summary).

The ROADMAP.md file remains the system of record, supported by cross-references to `.agents/plans/` entries. A file size guard (~50KB) warns when the file grows too large for low-end model contexts.

### Goals
- Rename TODO.md to ROADMAP.md and enrich the format with Criticality, Short Desc, Creation Date, Completion %, changelog, and linked plans
- Implement a parser and writer in Kotlin for the ROADMAP.md markdown format
- Build an interactive TUI mode within `frontend/apps/terminal` for roadmap management
- Provide nuclear "clear roadmap" functionality with multiple speedbumps (3 confirmations + 50-char reason + archival)
- Expose both features via the `mise` task runner

## 2. Architecture & Design Principles

### ROADMAP.md Format
- Markdown-based, human-readable, AI-agent-ingestible
- Each item has: ID (RM-XXX), Title, Short Desc, Criticality, Priority, Complexity (1-5), Status, Completion %, Creation Date, Objective, Scope, Dependencies, Acceptance Criteria, Notes, Linked Plans, Changelog
- Table of Contents at the top for quick scanning
- Frozen Items section at the bottom for archived entries
- File size guard: warn at ~50KB

### Data Models (Kotlin)
- `RoadmapItem` — full item data including changelog and linked plans
- `RoadmapChangelogEntry` — date, change, details

### Parser & Writer
- `RoadmapParser` — parses ROADMAP.md into `List<RoadmapItem>`, scans `.agents/plans/README.md` for linked plans, handles frozen items separately
- `RoadmapWriter` — writes items back to ROADMAP.md, preserves format, updates Table of Contents, appends changelog entries, handles freeze-all

### TUI Architecture
- `RoadmapApp` object in `Main.kt` with a `run()` entry point
- Dispatched from `TerminalApp.run()` via `--roadmap` CLI flag
- Views: List (table), Detail (full item), Add (form), Clear (confirmation flow)
- Follows existing Mordant patterns: `table {}`, `Panel`, colors, interactive loop with `Scanner`

### Short Description Generation
- AI-assisted generation from Name + Description (long), with fallback to template-based generation
- Interactive edit/accept/replace flow before creating the item

### Clear Roadmap Speedbumps
- 3 sequential confirmations: type "CLEAR" → "CLEAR ROADMAP" → "CLEAR ALL"
- Reason required (minimum 50 characters)
- All active items frozen and moved to Frozen Items section
- Frozen items archived and excluded from active views

## 3. Proposed Changes & Implementation Steps

### ✓ Step 1: Rename TODO.md to ROADMAP.md and update format
- Renamed `TODO.md` to `ROADMAP.md`
- Updated frontmatter: title changed to "Roadmap", added Criticality scale alongside Priority
- Added `Short Desc`, `Creation Date`, `Completion %` fields to each existing item (RM-001 through RM-005)
- Generated short descriptions for existing items from their titles
- Set creation dates (estimated from context: 2026-09-01 for RM-001/002/003, 2026-10-06 for RM-004/005)
- Added frozen items section template at bottom
- Updated Table of Contents to include Short Desc, Criticality, and Age columns
- Updated template for new entries with all new fields
- Updated all references in `.agents/plans/README.md`

### ✓ Step 2: Implement RoadmapParser and RoadmapWriter in Main.kt
- Added `RoadmapItem` and `RoadmapChangelogEntry` data classes to `Main.kt`
- Implemented `RoadmapParser` object:
  - Parses ROADMAP.md into `List<RoadmapItem>`
  - Parses each item block for full details
  - Scans `.agents/plans/README.md` for linked plans referencing each item ID
  - Handles frozen items separately
- Implemented `RoadmapWriter` object:
  - Writes items back to ROADMAP.md preserving markdown format
  - Updates Table of Contents
  - Appends changelog entries
  - Handles frozen items section
  - Supports field updates (criticality, priority, status, completion %)
- Added file size guard: warns if ROADMAP.md exceeds ~50KB

### ✓ Step 3: Implement RoadmapApp interactive TUI mode
- Added `RoadmapApp` object in `Main.kt` with:
  - `run()` entry point dispatched from `TerminalApp.run()` via `--roadmap` flag
  - List view: table showing ID, Name, Short Desc, Criticality, Age (days)
  - Detail view: full item details, linked plans, completion %, changelog
  - Action buttons: update criticality, update priority, mark complete, mark cancelled
  - Add item form: Name, Description (long), Criticality, Priority inputs
  - Short description generation: template-based with interactive edit/accept/replace
  - Auto-generate ID (next available number) and creation date
  - Create/Cancel buttons (Create only enabled when form is valid and short desc is done)
  - Clear roadmap: 3 confirmations (CLEAR → CLEAR ROADMAP → CLEAR ALL) + reason (50+ chars) → freeze all items
  - Navigation: back to list from detail, quit
- Updated `TerminalApp.run()` to dispatch `--roadmap` flag to `RoadmapApp.run()`
- Followed existing Mordant patterns: `table {}`, `Panel`, colors, interactive loop with `Scanner`

### ✓ Step 4: Add mise tasks for roadmap:edit and roadmap:status
- Created `.config/mise/tasks/roadmap/edit.sh`:
  - `#MISE description="Open the interactive roadmap manager (Kotlin TUI)"`
  - Launches `mise run tui --roadmap`
- Created `.config/mise/tasks/roadmap/status.py`:
  - `#MISE description="Display aggregate roadmap data"`
  - Parses ROADMAP.md and prints: total items, counts by status, counts by criticality, oldest item age, completion summary
  - Uses Python for parsing (consistent with other tracker tasks)
  - Includes frozen items count separately

## 4. Verification Plan & Testing Checklist

### Build Verification
- [x] `./gradlew :frontend:apps-terminal:installDist` compiles without errors
- [x] BUILD SUCCESSFUL confirmed

### Functional Verification
- [x] `mise roadmap:status` displays aggregate data correctly (5 active items, 0 frozen, criticality/priority distribution)
- [x] TUI launches via `--roadmap` flag, displays list view with all 5 items
- [x] TUI exits cleanly on `q` command

### Key Scenarios
1. **Launch roadmap TUI:** `mise roadmap:edit` → shows list of existing ROADMAP items ✓
2. **Inspect item:** Select an item → shows detail view with linked plans, status, changelog
3. **Add item:** Press `+` → form appears → fill fields → short desc generated → accept/edit → Create → item appears
4. **Update criticality:** In detail view → change criticality → saved to ROADMAP.md with changelog entry
5. **Mark complete:** In detail view → mark complete → status changes, completion % set to 100
6. **Clear roadmap:** Type `clear` → 3 confirmations → reason (50+ chars) → all items frozen, archived
7. **Status command:** `mise roadmap:status` → shows counts by status, criticality distribution, oldest item age ✓

### Edge Cases
- Empty ROADMAP.md → show empty state with option to add
- Malformed ROADMAP.md entry → skip with warning, don't crash
- ROADMAP.md exceeds size limit → warn user, suggest archival
- Clear with only 1 item → still require all speedbumps
- Reason too short (<50 chars) → reject, ask again
- Linked plans not found → show "No linked plans" gracefully
- Creation date missing (legacy items) → estimate or use current date with note

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-10/144900_roadmap-terminal-tui.md`
- [x] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
