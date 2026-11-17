# Implementation Plan: RM-001 Kotlin-Only Migration — Archiving Superseded Domain Scripts & Porting Bulk/Lead Commands

**Status:** Proposed  
**Date:** 2026-11-16  
**Target:** repository-wide (`frontend/apps/cli/`, `records/parser/`, `.config/mise/tasks/`, `scripts/`, `ROADMAP.md`)  
**Harness:** Antigravity  
**Model:** gemini-3.8-flash  
**Session:** 7093357f-5e5a-45e0-9093-a854e5ab458a  
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)  
**Depends On:** [`195419_rm-001-next-slices-plan.md`](./195419_rm-001-next-slices-plan.md)  
**Related Items:** `RM-001` (Kotlin-Only Migration)

---

## 1. Executive Summary & Goals

Continuing the incremental drive to 100% Kotlin on `RM-001`:

1. **Slice A: Archive Domain Scripts Fully Superseded by Existing Kotlin Modules**
   - `scripts/alignment.py` (493 lines) $\rightarrow$ fully superseded by `records/parser/ApplicationTimelineProjector.kt`, `EventChainBuilder.kt`, and `frontend/apps/cli/commands/AlignCommand.kt`.
   - `scripts/salary_lookup.py` (430 lines) $\rightarrow$ fully superseded by `records/parser/SalaryBenchmarkEngine.kt` and `frontend/apps/cli/commands/SalaryCommand.kt`.
   - `scripts/proposals.py` (625 lines) $\rightarrow$ fully superseded by `backend:email-classification` and `frontend/apps/cli/commands/ProposalCommand.kt`.
   Moving these 3 scripts into `archive/scripts/` drops the count in `scripts/` from 28 down to **25**.

2. **Slice B: Port Tracker Lead Command & Task to Kotlin**
   - `tracker:lead` allows creating an initial lead/prospect application record with `status=LEAD` from recruiter outreach or early contact.
   - Implement `LeadCommand.kt` in `frontend/apps/cli/commands/`.
   - Convert `.config/mise/tasks/tracker/lead.py` $\rightarrow$ `lead.sh`.

3. **Slice C: Quality Gates, Docs, and Roadmap Bump to 90%**
   - Regenerate all docs and completions.
   - Run quality gates (`mise run dev:guards`, `mise run dev:konsist`, `mise run records:test`).
   - Bump `ROADMAP.md` RM-001 completion to **90%**.

---

## 2. Proposed Implementation Steps

### Step 1: Archive Superseded Domain Scripts
Move `scripts/alignment.py`, `scripts/salary_lookup.py`, and `scripts/proposals.py` to `archive/scripts/`.

### Step 2: Implement `LeadCommand` in `frontend/apps/cli`
Implement `LeadCommand.kt` in `frontend/apps/cli/commands/`:
- Creates a new application with status `LEAD`, channels (`OUTREACH_EMAIL`, etc.), and appends the initial `LEAD` event.
- Register under `TrackerCommand`.
- Convert `.config/mise/tasks/tracker/lead.py` to `lead.sh`.

### Step 3: Quality Gates & Verification
- Verify `mise run tracker:lead --help`.
- Run `mise run dev:guards`, `mise run dev:konsist`, and `mise run records:test`.
- Run `mise run docs:tasks` and `mise run completions:generate`.
- Update `ROADMAP.md` RM-001 to 90%.

---

## 3. Bookkeeping Checklist
- [ ] Plan persisted at `.agents/plans/2026-11-16/200053_rm-001-domain-scripts-and-lead-plan.md`
- [ ] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
