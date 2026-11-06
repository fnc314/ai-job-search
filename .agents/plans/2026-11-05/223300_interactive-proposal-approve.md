# Implementation Plan: Interactive Proposal Approve (TUI)

**Status:** Completed  
**Date:** 2026-11-05  
**Target:** tracker and ui/terminal  
**Supersedes:** [`interactive-proposal-approve.md`](./223300_interactive-proposal-approve.md) (readline-based version)

---

## 1. Executive Summary & Goals

The `mise run tracker proposals approve -i` command originally used a readline-based loop that was illegible, hid keyboard commands until prompts, and provided no way to investigate proposal details. This plan replaces it with a curses-based TUI mode inside the existing `ui/terminal/tracker_tui.py` that provides:

1. **Always-visible keyboard commands** — a footer bar shows bindings at all times
2. **Proposal detail view** — pressing Enter reveals all changes and unmatched emails in a scrollable pane
3. **Checkbox-style enable/disable** — each proposal row has `[ ]` / `[x]` toggle for batch selection
4. **Legible layout** — proper curses rendering with color pairs, scrollable panes, and structured output

## 2. Architecture & Design Principles

### Current Implementation
- `scripts/tracker.py` — `_interactive_proposal_approve()` (readline-based) uses `_read_line()` prompts with `sys.stderr.write`. Iterates proposals sequentially, showing all changes then prompting for action.
- `ui/terminal/tracker_tui.py` — Full curses-based TUI with established patterns: list/detail views, split mode, settings view, edit view. Uses `safe_addstr()`, color pairs, scrollable panes, and a `while True` main loop.
- `scripts/proposals.py` — `listing(db, "proposal:pending")` returns full proposal documents; `approve()` and `reject()` apply changes.

### Key Decisions
- **Add proposals mode to the TUI's main loop** — A new `in_proposals` boolean flag is added to the existing `main()` loop, mirroring the existing `in_settings`, `in_edit`, and `in_detail` patterns. Reuses all rendering infrastructure.
- **Entry from `tracker proposals approve -i`** — The CLI command spawns `tracker_tui.py --proposals`, which loads pending proposals directly and enters the proposals mode. No changes to `proposals.py`.
- **Checkbox selection model** — Each proposal has an `enabled` boolean. `[Space]` toggles. `[a]` approves all enabled, `[r]` rejects all enabled. Individual proposals can also be approved/rejected from the detail view.
- **Detail view reuses `load_detail_lines` pattern** — `load_proposal_detail_lines(proposal, max_width)` generates `(text, file_path)` tuples for the detail pane.

## 3. Proposed Changes & Implementation Steps

### 3.1. `ui/terminal/tracker_tui.py` — `--proposals` CLI flag and entry point
- Added `--proposals` argument to `__main__` block that skips application data loading
- When `--proposals` is set, calls `proposalsModule.listing(db, "proposal:pending")` and enters proposals mode
- Skips the Textual loading screen and goes straight to curses

### 3.2. `ui/terminal/tracker_tui.py` — `load_proposal_detail_lines(proposal, max_width)`
- Returns list of `(text, file_path)` tuples
- Shows: proposal ID, created date, state
- Each change: index, company/application ID, event type, status transition, confidence, email subject
- Unmatched emails: subject, reason
- Follows the same format as `load_detail_lines()`

### 3.3. `ui/terminal/tracker_tui.py` — `main_proposals(stdscr)` curses loop
- Full curses loop with list view and detail view
- List view: header with pending/enabled counts, checkbox columns, proposal ID, date, change count, unmatched count
- Detail view: full-screen scrollable view of all changes with transitions and email subjects
- Key bindings: `[Space]` toggle enabled, `[j/k]` navigate, `[Enter]` detail, `[a]` approve selected (batch), `[r]` reject selected (batch), `[q]`/`[ESC]` quit
- Detail view bindings: `[a]` approve this proposal, `[r]` reject this proposal, `[b]`/`[ESC]` back to list
- Always-visible footer bar with keyboard commands
- Approve/reject actions use `subprocess.run` to call `tracker.py proposals approve/reject`, ensuring `TRACKER_ALLOW_WRITES=1` is enforced
- Emits summary JSON on stdout so `tracker.py` can parse it

### 3.4. `ui/terminal/tracker_tui.py` — `_run_proposal_action(action, proposal_id, note)`
- Runs approve or reject via subprocess, returning `(ok, message)`
- Same pattern as the edit view for write permission enforcement

### 3.5. `scripts/tracker.py` — Refactored `_interactive_proposal_approve()`
- Instead of readline loop, spawns `tracker_tui.py --proposals` via `subprocess.run`
- Parses the TUI's summary JSON output
- Kept TTY check: if no TTY, falls back to error message
- Added `os` and `subprocess` top-level imports

### 3.6. `scripts/specs/tracker.usage.kdl` and `mise.toml`
- No changes needed — existing `-i` flag description is sufficient

## 4. Verification Plan & Testing Checklist
- [x] `mise run tracker proposals approve --help` shows `-i` flag and optional `[id]`
- [x] TTY check: piping to `proposals approve -i` correctly rejects with "interactive mode requires a terminal"
- [x] Both files pass Python syntax validation
- [x] 14 pending proposals exist in Firestore for live testing
- [x] Existing non-interactive `proposals approve <id>` path unchanged

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-05/223300_interactive-proposal-approve.md`
- [x] Registry `README.md` row added / status synchronized
