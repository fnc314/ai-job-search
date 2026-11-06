# Implementation Plan: Interactive Proposal Approve

**Status:** Completed  
**Date:** 2026-11-05  
**Target:** tracker  

---

## 1. Executive Summary & Goals

The `mise run tracker proposals approve` command requires a proposal ID as a positional argument, forcing users to `proposals list`, copy an ID, then run `proposals approve <id>` for each change. This plan adds an `--interactive` (`-i`) flag so users can iterate through all pending proposals one-by-one, reviewing and acting on each without manual ID handling.

## 2. Architecture & Design Principles

### Current Implementation
- `scripts/tracker.py` — `commandProposals()` handles all proposals subcommands. The `approve` branch requires a positional `id` argument and calls `proposalsModule.approve()` with optional `--note` and `--only`.
- `scripts/proposals.py` — `approve()` applies changes in a batch; `listing()` queries by state; `load()` fetches a single proposal.
- The interactive pattern is established in `_read_line()`, `_select_application_inline()`, and `interactiveEdit()`. All use `sys.stderr.write` for prompts and `sys.stdin.readline` for input.
- The `edit` subcommand's `--interactive` flag makes the positional `id` optional (`nargs="?"`).

### Key Decisions
- **Add `--interactive` to existing `approve` parser** — The positional `id` becomes optional (`nargs="?"`). When `-i` is set without an ID, the interactive loop runs. This mirrors the `edit` command pattern.
- **Iterate proposals, not individual changes** — Each proposal is a batch from a single Gmail fetch. The user reviews at the proposal level (approve all / reject / skip / select changes), matching the existing `--only` semantics.
- **Reuse `proposalsModule.approve()` and `proposalsModule.reject()`** — No changes needed to `proposals.py`. The interactive loop in `tracker.py` calls the existing functions.

## 3. Proposed Changes & Implementation Steps

### 3.1. `scripts/tracker.py` — Argument parser
- Change `proposalApprove.add_argument("id")` to `proposalApprove.add_argument("id", nargs="?")`
- Add `proposalApprove.add_argument("-i", "--interactive", action="store_true")`

### 3.2. `scripts/tracker.py` — New function `_interactive_proposal_approve(db, note)`
- Fetch all pending proposals via `proposalsModule.listing(db, "proposal:pending")`
- For each proposal, display a formatted summary (changes with event type, status transition, confidence, email subject; unmatched emails)
- Prompt with `[a]pprove / [r]eject / [s]kip / [c]hoose changes / [q]uit`
- On `a`: call `proposalsModule.approve(db, proposalId, note=note)`
- On `r`: call `proposalsModule.reject(db, proposalId)`
- On `s`: continue to next
- On `c`: prompt for comma-separated indexes, call `proposalsModule.approve(db, proposalId, note=note, only={indexes})`
- On `q`: break loop
- After loop, emit a summary JSON with counts of approved/rejected/skipped
- TTY check via `sys.stdin.isatty()` refusing non-interactive stdin

### 3.3. `scripts/tracker.py` — `commandProposals()` update
- In the `approve` branch, check `args.interactive`. If set and no `id`, call `_interactive_proposal_approve()`. Otherwise existing behavior is unchanged.

### 3.4. `scripts/specs/tracker.usage.kdl`
- Make `approve` `<id>` optional (`[id]`) and add `-i --interactive` flag

### 3.5. `mise.toml`
- Update the tracker task's Usage DSL to include the `-i` flag on the `approve` subcommand

## 4. Verification Plan & Testing Checklist
- [x] `mise run tracker proposals approve --help` shows new `-i` flag and optional `[id]`
- [x] TTY check: piping to `proposals approve -i` correctly rejects with "interactive mode requires a terminal"
- [x] Generated docs (`docs/mise/tracker.md`) updated via `mise run mise:docs:tasks`
- [x] Zsh completions updated via `mise run completions:generate`
- [x] Existing non-interactive `proposals approve <id>` path unchanged

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-05/223300_interactive-proposal-approve.md`
- [x] Registry `README.md` row added / status synchronized
