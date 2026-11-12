# Implementation Plan: Email Job Application Discovery

**Status:** In Progress  
**Date:** 2026-11-12  
**Target:** backend and tracker (cross-subsystem)  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261112-160440-x5xy  

---

## 1. Executive Summary & Goals

When emails reference job applications that aren't tracked in Firestore (e.g., Fabletics rejection in `JOB/Fabletics`), the system drops them into `unmatched` and never creates the application record. This feature adds a **Discovery Classifier** that generates proposals for untracked applications from email evidence, so the user can review and create them.

## 2. Architecture & Design Principles

- Discovery lives in `BackendService.kt` (Kotlin) and `scripts/discover.py` (Python) per RM-003
- Discovery proposals use the existing `pending-proposals` queue with `kind: "DISCOVER"`
- New event type `DISCOVER = 22` in `TrackerEventType` proto enum
- Role title extraction from email body with user validation before commit
- `email-inbox.json` replaces `gmail-inbox.json` as the primary inbox file name

## 3. Proposed Changes & Implementation Steps

### Completed
- [x] Added `DISCOVER = 22` to `TrackerEventType` proto enum
- [x] Implemented `discoverUntrackedApplications()`, `findRelatedEmails()`, `stageDiscoveryProposals()` in `BackendService.kt`
- [x] Created `scripts/discover.py` with Python discovery classifier
- [x] Created `.config/mise/tasks/email/discover.py` mise task
- [x] Updated `scripts/proposals.py` to handle `kind: "DISCOVER"` in `approve()`
- [x] Updated `scripts/tracker.py` to use `email-inbox.json` as primary
- [x] Updated `BackendService.kt` `backfillEmails()` to use `email-inbox.json` as primary
- [x] Rebuilt proto via `mise run records:build`

### Bug Fixes (Post-Implementation)
- [x] Fixed `discover.py`: added `os` and `re` imports (missing `os` caused runtime error)
- [x] Fixed `discover.py`: `re.IGNORE_CASE` → `re.IGNORECASE` (Python 3.15 compatibility)
- [x] Fixed `discover.py`: `_extract_company` only used subject patterns, never extracted company from sender domain — added `_extract_company_from_sender()` function
- [x] Fixed `discover.py`: NOISE emails were skipped entirely — added `_has_job_signals()` re-classification so false-positive NOISE emails (e.g., Fabletics misclassified because "order" matched "border" in HTML/CSS) are re-evaluated
- [x] Fixed `discover.py`: added job signal gate before company extraction — without it, every uncorrelated email (personal Gmail replies, Venmo, etc.) was processed as a candidate
- [x] Fixed `discover.py`: sender domain extraction didn't handle "Display Name <email@domain>" format — `rsplit("@")` on the full sender string gave wrong results
- [x] Fixed `discover.py`: personal domain rejection checked after signal matching, so Gmail replies with "assessment" in the snippet passed through — moved to the top of `_has_job_signals()`
- [x] Fixed `discover.py`: reject patterns checked after ATS weak signal check, so "New Job Opportunities" job alerts passed — moved reject patterns before ATS check
- [x] Fixed `discover.py`: added `_company_matches_tracked()` fuzzy matching so "Vanguardhr" matches tracked "Vanguard"
- [x] Fixed `discover.py`: added `_email_references_tracked()` to check email content for tracked company names (e.g., "capitalone" in sender matches tracked "Capital One")
- [x] Fixed `BackendService.kt`: noise keyword "order" matched "border" in HTML/CSS from ATS emails — removed "order" from noise keywords and added job signal override that prevents ATS emails from being classified as NOISE
- [x] Fixed `BackendService.kt`: updated `backfillEmails()` to check job signals before noise classification
- [x] Fixed `proposals.py`: `statusRank(status_to)` passed a positional argument to `contract.statusRank()` which takes zero arguments — changed to `contract.statusRank().get(status_to, 4)`
- [x] Fixed `TrackerApp.kt`: `t all` in proposals TUI treated "all" as a proposal ID prefix — added "all"/"none" special cases in the toggle handler

### In Progress
- [x] Updated `.agents/commands/gmail-sync.md`: relaxed line 51, added Step 3e, extended Step 4
- [ ] Run `mise run email:discover --all` for catch-up (requires Firebase credentials)
- [x] Write formal plan artifact
- [ ] Update `.agents/plans/README.md`

## 4. Verification Plan & Testing Checklist

- [x] Run `python3 scripts/discover.py --all --dry-run` — surfaces 4 untracked applications:
  - **Fabletics** (REJECTED, from `fabletics@myworkday.com`)
  - **Fmr** (SUBMITTED, from `fmr@myworkday.com`, likely Fidelity/FMR)
  - **Snapchat** (SUBMITTED, from `snapchat@myworkday.com`)
  - **Washpost** (SUBMITTED, from `washpost@myworkday.com`, Washington Post)
- [ ] Verify discovery proposals appear with `kind: "DISCOVER"` (requires `TRACKER_ALLOW_WRITES=1`)
- [ ] Verify `proposals approve` creates application records correctly
- [x] Verify no false positives from noise emails — personal Gmail replies, Venmo, LinkedIn alerts, job alerts, talent network emails all correctly filtered

## 5. Bookkeeping

- [x] Plan persisted at `.agents/plans/2026-11-12/161900_email-job-application-discovery.md`
- [ ] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message
- [ ] Push `git push origin main`
