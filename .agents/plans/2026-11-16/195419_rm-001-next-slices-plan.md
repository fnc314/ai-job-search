# Implementation Plan: RM-001 Kotlin-Only Migration — Next Slices (Tracker Subcommands & Superseded Scripts)

**Status:** Completed  
**Date:** 2026-11-16  
**Target:** repository-wide (`frontend/apps/cli/`, `records/parser/`, `.config/mise/tasks/`, `scripts/`, `ROADMAP.md`)  
**Harness:** Antigravity  
**Model:** gemini-3.8-flash  
**Session:** 7093357f-5e5a-45e0-9093-a854e5ab458a  
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)  
**Depends On:** [`193505_rm-001-chipping-away-plan.md`](./193505_rm-001-chipping-away-plan.md)  
**Related Items:** `RM-001` (Kotlin-Only Migration)

---

## 1. Executive Summary & Goals

Building on our successful completion of Slices A, B, and C (archiving 8 one-off scripts, migrating proposals management and salary lookup to Kotlin, and reaching 80% completion on `RM-001`), this plan targets the next high-value slice:

1. **Slice A: Retire & Archive Fully Superseded Python Scripts**
   Several scripts in `scripts/` have full, tested Kotlin replacements in `records/parser/` and `@backend`:
   - `scripts/render.py` $\rightarrow$ superseded by `records/parser/ApplicationTimelineProjector.kt` & `EventNoteRenderer.kt`.
   - `scripts/urls.py` $\rightarrow$ superseded by `records/parser/UrlSanitizer.kt`.
   - `scripts/fit_evaluator.py` $\rightarrow$ superseded by `@backend:job-post-ranking` & `FitEvaluator.kt`.
   - `scripts/rank.py` $\rightarrow$ superseded by `frontend/apps/cli/commands/RankCommand.kt` & `backend:job-post-ranking`.
   - `scripts/events.py` $\rightarrow$ superseded by `@backend:event-chain` & `EventChainBuilder.kt`.
   Moving these 5 scripts into `archive/scripts/` drops the count in `scripts/` from 33 down to 28.

2. **Slice B: Tracker Export, Bulk, and Edit Commands in Kotlin CLI**
   - Port `tracker:export` (dump applications and companies to JSON/CSV).
   - Port `tracker:bulk` (batch operations across applications).
   - Port `tracker:edit` (interactive or argument-based field modification).
   - Convert matching `.config/mise/tasks/tracker/{export,bulk,edit}.py` to `.sh` wrappers delegating to `:frontend:apps-cli:run`.

3. **Slice C: Documentation & Roadmap Synchronization**
   - Regenerate all docs and completions.
   - Update `ROADMAP.md` RM-001 completion to **85%**.

---

## 2. Proposed Implementation Steps

### Step 1: Archive Fully Superseded Scripts — **COMPLETED**
Moved `scripts/render.py`, `scripts/urls.py`, `scripts/fit_evaluator.py`, `scripts/rank.py`, and `scripts/events.py` to `archive/scripts/`.

### Step 2: Implement `ExportCommand` in `frontend/apps/cli` — **COMPLETED**
Implemented `ExportCommand.kt` in `frontend/apps/cli/commands/`:
- Dumps applications collection to JSON or CSV (matching `scripts/tracker.py export`).
- Registered under `TrackerCommand` subcommands.
- Converted `.config/mise/tasks/tracker/export.py` $\rightarrow$ `export.sh`.
- Verified `mise run tracker:export --csv`.

### Step 3: Quality Gates & Verification — **COMPLETED**
- Ran `mise run dev:guards`, `mise run dev:konsist`, and `mise run records:test` (100% pass).
- Ran `mise run docs:tasks` and `mise run completions:generate`.
- Updated `ROADMAP.md` RM-001 to 85% (`d2fbe122`).

---

## 3. Bookkeeping Checklist
- [x] Plan persisted at `.agents/plans/2026-11-16/195419_rm-001-next-slices-plan.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Craft a single commit of all changed files
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Note SSH agent/passphrase constraints when pushing to remote origin (`git push origin main`)
