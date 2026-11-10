# Architecture Plan: Proposal Approval Flow Review & Terminal Experience Migration

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** `frontend/apps/terminal`, `scripts/tracker.py`, `.config/mise/tasks/tracker/`, `.config/mise/tasks/proposals/`, `.config/mise/tasks/ui/terminal/`  
**Harness:** Antigravity  
**Model:** Gemini 3.8 Flash (Low)  
**Session:** `67494617-dcfc-49c3-92fd-28e963ce3a2c`  

---

## 1. Executive Summary & Problem Diagnosis

### Context
A user noted: *"Review the mise tracker actions and the interactive approval flow for proposals. It seems to now be automatic approval."*

Following an in-depth audit across the pipeline codebase (`frontend/apps/terminal`, `scripts/tracker.py`, `scripts/proposals.py`, `.config/mise/tasks/`), we investigated both the Firestore records and the approval execution pathways.

### The Root Causes of "Automatic Approval"

1. **Dangerous Default: All Proposals Pre-Selected (`enabled = true`)**
   In `frontend/apps/terminal/src/main/kotlin/dev/fnc314/tools/jobtracker/terminal/Main.kt`:
   ```kotlin
   public data class ProposalRecord(
       val proposalId: String,
       val createdAt: String,
       val state: String,
       val changesCount: Int,
       val unmatchedCount: Int,
       var enabled: Boolean = true,  // <--- Pre-selects EVERY proposal
       ...
   )
   ```
   When the interactive proposal review mode starts (`apps-terminal --proposals`), **every pending proposal in the database is automatically checked (`[x]`)**.

2. **Zero-Confirmation Batch Execution on `a` (Approve)**
   In `Main.kt` lines 526-536:
   ```kotlin
   "a", "approve" -> {
       val toApprove = if (parts.size > 1) {
           proposals.filter { it.proposalId.startsWith(parts[1]) }
       } else {
           proposals.filter { it.enabled }
       }
       ...
   ```
   If a user types `a` (or `approve`) without an ID prefix, it executes `proposals.filter { it.enabled }`. Because all proposals default to `enabled = true`, **typing `a` immediately approves and applies every staged proposal in Firestore in a single batch with NO confirmation prompt**.

3. **Mismatched UI Instructions vs Console Input Handling**
   The UI banner states:
   > *"Review staged inbox proposals • Press Space to toggle selection, [a] approve, [r] reject, [q] quit"*
   
   However, the console input is handled by `java.util.Scanner(System.in).nextLine()`, which is line-buffered. Pressing `Space` does not toggle selection; pressing Enter or typing `a` triggers batch approval, creating the impression that proposals are being approved automatically upon entering the mode or hitting a key.

4. **CLI Argument Fallthrough Bug in `scripts/tracker.py`**
   In `scripts/tracker.py` lines 2534-2545:
   ```python
   if args.proposalsCommand == "approve":
       if getattr(args, "interactive", False) and not args.id:
           return _interactive_proposal_approve(db, note=args.note)
       client.assertWritesAllowed("approved proposal changes")
       ...
       emit({"ok": True, **proposalsModule.approve(db, args.id, ...)})
   ```
   When a user runs `mise run proposals:approve` or `mise tracker proposals approve` without passing `<id>` and without passing `-i`, `args.id` is `None`. Instead of defaulting to interactive mode or prompting for input, it calls `proposalsModule.approve(db, None)` and crashes with `contract error: no such proposal: None`.

5. **Terminal Script Fragmentation & Residual Python Terminal Logic**
   The legacy `ui/terminal/tracker_tui.py` Python script was previously retired in favor of `frontend/apps/terminal` (Kotlin + Mordant), but lingering wrappers, subprocess calls in Python scripts, and documentation references still point back to Python terminal scripts. All terminal interactions and interactive review workflows must be fully owned by `frontend/apps/terminal`, referenced through clean `mise` tasks with a freshly generated jar and distribution.

---

## 2. Strategic Objectives & User Alignments

Per explicit user direction:
- **Dynamic Harness, Model & Session Metadata in `TEMPLATE.md`**:
  Updated `.agents/plans/TEMPLATE.md` with explicit instructions for *any* running AI agent across harnesses (Antigravity, Junie, Claude Code, OpenCode, Codex, etc.) to dynamically populate the frontmatter with data reported by the active harness environment:
  - `**Harness:** <current-running-harness-name>` (e.g., `Antigravity`, `Junie`, `Claude Code`, `OpenCode`)
  - `**Model:** <current-running-model-name>` (e.g., `gemini-3.8-flash`, `claude-3-7-sonnet`)
  - `**Session:** <current-session-id>` (the active harness conversation/session ID)
- **Pure Kotlin Terminal Ownership**: All terminal experiences must be strictly handled by `frontend/apps/terminal` (Kotlin Mordant TUI) with regenerated distribution binary (`gradlew :frontend:apps-terminal:installDist`), completely migrating away from Python terminal scripts.
- **Opt-In Selection**: Staged proposals must default to unselected (`[ ]`, `enabled = false`).
- **Batch Confirmation & Scripting Override**: All interactive batch actions require explicit confirmation (`[y/N]`), with support for `-y` / `--yes` for automated scripting.
- **TTY Auto-Launch**: When `proposals:approve` is invoked without `<id>` in an interactive terminal (TTY attached), it automatically routes directly to `apps-terminal --proposals`.

---

## 3. Architecture & Design Principles

### Core Architecture Specifications

#### A. Interactive Proposal Review Hardening (`frontend/apps/terminal`)
1. **Opt-In Selection Default**:
   ```kotlin
   public data class ProposalRecord(
       val proposalId: String,
       val createdAt: String,
       val state: String,
       val changesCount: Int,
       val unmatchedCount: Int,
       var enabled: Boolean = false, // Strictly false: opt-in required
       val changes: List<Map<String, Any?>> = emptyList(),
       val unmatched: List<Map<String, Any?>> = emptyList()
   )
   ```
2. **Explicit Interactive Confirmation**:
   - `a` (batch approve):
     - If count == 0: prints warning `"No proposals selected. Use 't <id>' or 'all' to select proposals to approve."` and resumes loop without executing.
     - If count > 0: prompts `Approve $count proposal(s) with total $totalChanges changes? [y/N] > `. Only proceeds if user answers `y` or `yes`.
   - `r` (batch reject):
     - If count == 0: prints warning and resumes.
     - If count > 0: prompts `Reject $count proposal(s)? [y/N] > `.
   - Single target commands (`a <id>`, `r <id>`):
     - Prompts `Approve proposal $id? [y/N] > ` before executing.
3. **Command Vocabulary**:
   - `t <id>`: Toggle selection.
   - `all`: Select all pending proposals.
   - `none`: Deselect all proposals.
   - `i <id>`: Inspect proposed state changes and unmatched message reasons.
   - `a` / `a <id>`: Approve selected or specific proposal.
   - `r` / `r <id>`: Reject selected or specific proposal.
   - `q`: Quit without taking action.
4. **Scripting Non-Interactive Override (`--yes`, `-y`)**:
   - Support `--yes` / `-y` flag passed to `apps-terminal` or `proposals approve` for automated verification scripts and headless pipelines, bypassing interactive confirmation prompts when explicitly instructed.

#### B. Complete Migration Away from Python Terminal Scripts
1. **Single Terminal Binary Ownership**:
   - `frontend/apps/terminal` is the authoritative and exclusive implementation for all interactive terminal applications (pipeline tracker dashboard and proposal approval review).
   - Python `tracker.py` remains strictly a headless backend business logic and Firestore write-engine tool; it will contain no curses code, no terminal formatting UI, and delegates all interactive loops to `apps-terminal`.
2. **Mise Task Wiring**:
   - `.config/mise/tasks/ui/terminal/run.sh` (`mise run tui`) builds and executes `frontend/apps/terminal/build/install/apps-terminal/bin/apps-terminal`.
   - `.config/mise/tasks/proposals/approve.py`: Updated to detect interactive TTY and invoke `apps-terminal --proposals`.
   - Any remaining documentation or comments referencing `ui/terminal/tracker_tui.py` or python curses will be cleaned up and aligned with `frontend/apps/terminal`.

---

## 4. Implementation Steps

### Phase 1: Kotlin Terminal App Update (`frontend/apps/terminal`)
- [x] In `frontend/apps/terminal/src/main/kotlin/dev/fnc314/tools/jobtracker/terminal/Main.kt`:
  - Change `ProposalRecord.enabled` default value from `true` to `false`.
  - Add `all` and `none` commands in `runProposalsMode()`.
  - Add helper function `confirmAction(prompt: String): Boolean`.
  - Implement confirmation checks for batch `approve`, batch `reject`, and single target approvals.
  - Update banner text: replace misleading `Press Space to toggle selection` with `Commands: [t <id>] toggle, [all] select all, [none] clear, [i <id>] inspect, [a] approve, [r] reject, [q] quit`.
- [x] Rebuild and reinstall terminal distribution:
  ```bash
  ./gradlew :frontend:apps-terminal:installDist
  ```
- [x] Add unit tests in `frontend/apps/terminal/src/test/kotlin/dev/fnc314/tools/jobtracker/terminal/TerminalAppTest.kt`:
  - Test `ProposalRecord` defaults to `enabled == false`.
  - Test command parsing for `all`, `none`, and confirmation flows.

### Phase 2: CLI Routing & TTY Auto-Launch (`scripts/tracker.py` & Mise Tasks)
- [x] In `scripts/tracker.py`:
  - In `commandProposals(args)`:
    - Add support for `-y` / `--yes` flag to bypass confirmation in CLI mode.
    - If `approve` is invoked without `<id>`:
      - If `sys.stdin.isatty()` or `args.interactive`: launch Kotlin TUI binary (`_interactive_proposal_approve`).
      - If not a TTY and no `<id>`: raise `contract.ContractError("Proposal ID is required, or use -i/--interactive in an interactive terminal.")`.
- [x] In `.config/mise/tasks/proposals/approve.py`:
  - Add `#USAGE flag "-y --yes" help="Skip interactive confirmation prompts"`
  - Forward `-y` / `--yes` to `scripts/tracker.py`.
- [x] In `.config/mise/tasks/tracker/`:
  - Ensure `tracker:proposals` or aliases map directly so both `mise run tracker:proposals:*` and `mise run proposals:*` function consistently.

### Phase 3: Documentation, Tests & Cleanup
- [x] Migrate terminal binary test suite from legacy Python `scripts/tests/test_terminal_bin.py` directly into Kotlin `frontend/apps/terminal/src/test/kotlin/dev/fnc314/tools/jobtracker/terminal/TerminalAppTest.kt` and delete the Python test file.
- [x] Update `docs/mise/` via `mise run docs:tasks`.
- [x] Run `mise run dev:konsist` to verify codebase architecture and consistency rules.

---

## 5. Verification & Testing Plan

1. **Automated Unit & Contract Tests**:
   - `./gradlew :frontend:apps-terminal:test`
   - `mise run dev:konsist`
2. **Interactive Manual Smoke Verification**:
   - Launch `mise run proposals:approve` (or `mise tui --proposals`) with test/cache data.
   - Verify all proposals start unselected (`[ ]`).
   - Type `a` -> verify system warns that 0 proposals are selected and takes no action.
   - Type `t <id>` -> verify that proposal toggles to `[x]`.
   - Type `a` -> verify system prompts `Approve 1 proposal(s) with total X changes? [y/N] > `.
   - Type `n` -> verify operation is aborted with zero mutations.
   - Type `all` -> verify all proposals toggle to `[x]`.
   - Type `none` -> verify all proposals revert to `[ ]`.
3. **Non-Interactive & Scripting Verification**:
   - Run `python3 scripts/tracker.py proposals approve <id> -y` -> verify single proposal approves immediately without interactive prompt.
   - Run in non-TTY without `<id>` -> verify clean error message without stack trace.
