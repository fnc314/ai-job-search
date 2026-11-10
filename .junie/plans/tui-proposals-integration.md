---
sessionId: session-261110-165204-1d8y
---

# Problem & Solution

### Problem
The `mise proposals approve -i` command spawns a separate TUI process (`--proposals` flag) for reviewing staged edits. This is disconnected from the main interactive TUI dashboard (`mise run tui`), which has no way to enter proposals review mode. Users must exit the dashboard, run a separate CLI command, and then restart the TUI — a clunky workflow.

### Current State
- **Proposals review mode** exists in `Main.kt` (`runProposalsMode`, lines 1701–1914) with full interactive features: toggle selection, inspect details, batch approve/reject.
- **Accessible only via CLI flag**: `--proposals` launches the TUI in proposals-only mode, bypassing the main dashboard entirely.
- **Main TUI `interactiveLoop`** (lines 2258–2370) has commands for filtering, inspecting apps, and even an `e` command that launches `EmailsApp.run()` inline — but **no `p` command for proposals**.
- **No proposal badge** in the dashboard header. The cache already tracks `pendingProposals` count (via `reader.py`), and `loadPendingProposals()` already works in the TUI.

### Solution
1. **Add `p` / `proposals` command** to the main TUI `interactiveLoop` that launches `runProposalsMode()` inline, mirroring how `e` launches `EmailsApp.run()`.
2. **Add pending proposal badge** to the dashboard header so users see at a glance when proposals need review.
3. **Refresh filters after returning** from proposals mode, so the dashboard reflects any status changes from approved proposals.

### Scope

**In Scope:**
- Add `p`/`proposals` command to `interactiveLoop` in `Main.kt`
- Add pending proposal count badge to the dashboard header/filter line
- Update help text to document the new command
- Update the existing test (`test_terminal_bin.py`) if needed

**Out of Scope:**
- Changes to the standalone `--proposals` CLI mode (it remains available for `mise run proposals approve -i`)
- Changes to `tracker.py` proposal logic
- UI redesign of the proposals review mode itself

# Technical Design

### Current Implementation

**File:** `frontend/apps/terminal/src/main/kotlin/dev/fnc314/tools/jobtracker/terminal/Main.kt`

- `runProposalsMode(autoYes: Boolean)` (line 1701) — standalone proposals review loop. Loads proposals via `loadPendingProposals()`, renders a table, and handles interactive commands (toggle, inspect, approve, reject). Outputs summary JSON to stdout.
- `interactiveLoop()` (line 2258) — main dashboard loop. Commands include `o` (open filter), `l` (leads), `s` (status), `/` (search), `i` (inspect), `b` (browse), `r` (recalc), `e` (emails → calls `EmailsApp.run()`), `h` (help), `q` (quit).
- `listApplications()` (line 2040) — renders the header via `renderHeader()` and the filter/stats line showing total/open/interviews/offers/leads counts.
- `loadPendingProposals()` (line 1616) — already loads proposals from Firestore, CLI fallback, or cache.

### Proposed Changes

**1. Add `p`/`proposals` command to `interactiveLoop`** (after the `e`/`emails` command, ~line 2345):

```kotlin
"p", "proposals" -> {
    TerminalApp.runProposalsMode()
    listApplications(openOnly, filterQuery, statusFilter, tagFilter, limit, sortCol)
}
```

This mirrors the existing `e`/`emails` pattern: launch the sub-mode, then refresh the application list when returning.

**2. Add pending proposal badge to the filter/stats line** in `listApplications()` (~line 2084):

After computing `leadCount`, add:
```kotlin
val pendingProposals = try { loadPendingProposals().size } catch (e: Exception) { 0 }
```

Then append to the stats line (after `leadCount`):
```kotlin
+ white(" | Pending Proposals: ") + 
    (if (pendingProposals > 0) yellow(bold(pendingProposals.toString())) else dim("0"))
```

**3. Update help text** (~line 2349):
Add `p, proposals       - Review staged inbox proposals (approve/reject)` to the available commands list.

**4. Update prompt** (~line 2283):
Add `p` to the prompt: `tracker [o:open, l:leads, s <status>, / <query>, i <id>, b <id>, r <id>, p:proposals, q:quit] > `

### Key Decisions
- **Reuse `runProposalsMode()` directly** rather than duplicating logic. The function already handles the full interactive cycle and returns control naturally.
- **Badge uses `loadPendingProposals()`** which already has Firestore → CLI → cache fallback chain. No new data path needed.
- **No change to `tracker.py`** — the Python side already wires `--proposals` correctly. This is purely a TUI-side integration.

# Delivery Steps

### ✓ Step 1: Add proposals command and badge to main TUI
Add `p`/`proposals` command to `interactiveLoop` and a pending proposal count badge to the dashboard header in `Main.kt`.

- Add `"p", "proposals"` case to the `interactiveLoop` when-block (~line 2345), calling `TerminalApp.runProposalsMode()` then refreshing the app list.
- Compute `pendingProposals` count in `listApplications()` (~line 2084) using the existing `loadPendingProposals()` and append it to the stats line.
- Update the prompt string (~line 2283) to include `p:proposals`.
- Update the help text (~line 2349) to document the new command.
- Build the terminal app (`mise frontend:build` or `./gradlew :frontend:apps-terminal:installDist`) to verify compilation.

### ✓ Step 2: Update tests and verify end-to-end
Ensure existing tests pass and add coverage for the new TUI proposals integration.

- Run existing terminal binary tests (`python3 -m pytest scripts/tests/test_terminal_bin.py`) to confirm no regressions.
- Verify the `--proposals` flag still works standalone (existing test `test_proposals_flag_emits_json_summary`).
- Verify the tracker CLI hand-off (`test_tracker_cli_proposals_approve_interactive`) still passes.
- Manually verify: launch `mise run tui`, type `p`, confirm proposals mode launches; type `q` to return to dashboard and verify the app list refreshes with the proposal badge visible.