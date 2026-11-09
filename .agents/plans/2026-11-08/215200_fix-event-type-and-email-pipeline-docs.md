# Implementation Plan: Fix Event Type Strings and Email Pipeline Docs

**Status:** Completed
**Date:** 2026-11-08
**Target:** tracker
**Model:** Qwen/Qwen3.6-27B-FP8
**Session:** session-261108-214734-13ti
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)

---

## 1. Executive Summary & Goals

Fix the `mise run align` crash caused by non-canonical event type strings (`event:*` prefixed) passed to `buildEvent()`, and create a clear end-to-end workflow document for the email-to-Firebase pipeline.

### Goals
- Replace all `event:*` prefixed event type strings in write-side `buildEvent()` calls with canonical schema enum names (`ADD_NOTE`, `CORRECT_STATUS`, `RECALCULATE_FIT`, `ADD_ARTIFACT`).
- Sync identical copies across `lib/alignment.py`, `scripts/alignment.py`, and `lib/tracker/alignment.py`.
- Update test expectations to match canonical event type names.
- Document the full email-to-Firebase pipeline in `docs/mise/email-pipeline.md`.

## 2. Architecture & Design Principles

### Problem
- `mise run align` crashes with `events.EventError: unknown event type 'event:note-added'` because call sites pass `event:*` prefixed strings to `buildEvent()`, but `tracker.schema.json` → `eventTypeValue` accepts only unprefixed enum names.
- The email pipeline (`email:read-inbox` → `email:collect` → proposal review) lacked a single reference document.

### Key Decisions
- **Write-side only**: Only `buildEvent()` call sites (the write path) are fixed. Read-side lookups (`NON_STATUS_EVENTS`, `COUNTER_RULES`, `renderNote` defaults) intentionally retain `event:*` format to match legacy Firestore records.
- **Sync all copies**: `lib/alignment.py` is the active module (via `PYTHONPATH`), but `scripts/alignment.py` and `lib/tracker/alignment.py` are exact duplicates that must stay in sync.
- **No schema migration**: Legacy Firestore records with `event:*` types remain valid; only new writes use canonical names.

## 3. Proposed Changes & Implementation Steps

### Step 1: Fix event type strings in tracker.py and alignment.py (Completed)

Replaced all `event:*` prefixed strings passed to `buildEvent()` with canonical schema enum names:

| File | Line | Current | Fixed |
|---|---|---|---|
| `lib/alignment.py` | 350 | `"event:note-added"` | `"ADD_NOTE"` |
| `scripts/tracker.py` | 770 | `"event:note-added"` | `"ADD_NOTE"` |
| `scripts/tracker.py` | 1167 | `"event:note-added"` | `"ADD_NOTE"` |
| `scripts/tracker.py` | 1634 | `"event:status-corrected"` / `"event:note-added"` | `"CORRECT_STATUS"` / `"ADD_NOTE"` |
| `scripts/tracker.py` | 1736 | `"event:status-corrected"` | `"CORRECT_STATUS"` |
| `scripts/tracker.py` | 1853 | `"event:fit-recalculated"` | `"RECALCULATE_FIT"` |
| `scripts/tracker.py` | 1966 | `"event:artifact-added"` | `"ADD_ARTIFACT"` |
| `scripts/alignment.py` | 350 | `"event:note-added"` | `"ADD_NOTE"` |
| `lib/tracker/alignment.py` | 350 | `"event:note-added"` | `"ADD_NOTE"` |

Updated test expectations in `scripts/tests/test_tracker_align.py:33` where `notesRendered` contains `event:note-added` → `ADD_NOTE`.

### Step 2: Add email-to-Firebase pipeline workflow doc (Completed)

Created `docs/mise/email-pipeline.md` documenting:
- The 3-step pipeline: `mise run email:read-inbox` → `mise run email:collect` → proposal review
- What each step produces (inbox payload JSON, staged proposals in Firestore, applied changes)
- Optional `mise run email:sync-labels` step for label caching
- Quick-reference command table with descriptions and typical flags
- Data flow diagram and troubleshooting section

## 4. Verification Plan & Testing Checklist

- [x] All event type values match `tracker.schema.json` `eventTypeValue` enum exactly
- [x] Three identical copies of `alignment.py` synced with the same fix
- [x] Test expectations updated to match canonical names
- [x] `docs/mise/email-pipeline.md` created with complete pipeline documentation
- [ ] Run `mise run align` to verify no `EventError` (pending live Firestore access)
- [ ] Run `mise run dev:test` to verify test pass status

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-08/215200_fix-event-type-and-email-pipeline-docs.md`
- [x] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
