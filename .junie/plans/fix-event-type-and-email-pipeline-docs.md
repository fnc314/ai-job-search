---
sessionId: session-261108-214734-13ti
---

# Requirements

### Overview
Fix the `mise run align` crash caused by non-canonical event type strings passed to `buildEvent()`, and add a clear end-to-end workflow document for the email-to-Firebase pipeline.

### Problem
- `mise run align` crashes with `events.EventError: unknown event type 'event:note-added'` because `lib/alignment.py` (and several call sites in `scripts/tracker.py`) pass `event:*` prefixed strings to `buildEvent()`, but the schema (`tracker.schema.json` → `eventTypeValue`) only accepts unprefixed enum names like `ADD_NOTE`, `CORRECT_STATUS`, `RECALCULATE_FIT`, `ADD_ARTIFACT`.
- The email-to-Firebase workflow (`email:read-inbox` → `email:collect` → proposal review) lacks a single clear reference document explaining the full pipeline.

### Scope
**In Scope:**
- Fix all `buildEvent()` call sites that pass `event:*` prefixed event types to use canonical schema enum names.
- Sync identical copies in `scripts/alignment.py` and `lib/tracker/alignment.py`.
- Update test expectations that reference the old event type format.
- Create `docs/mise/email-pipeline.md` with the full email→Firestore workflow.

**Out of Scope:**
- Read-side lookups (`NON_STATUS_EVENTS`, `COUNTER_RULES`, `renderNote` defaults) — these correctly use `event:*` to match legacy Firestore records and must remain unchanged.
- Schema changes — the schema already defines the correct enum names.
- Migrating existing Firestore records — legacy records with `event:*` types remain valid; only new writes use canonical names.

### Acceptance Criteria
- `mise run align` completes without `EventError`.
- `mise run tracker add --note "..."` works without error.
- `mise run tracker edit`, `tracker status`, `tracker score`, `tracker artifact` all work without `EventError`.
- Existing tests pass with updated event type expectations.
- `docs/mise/email-pipeline.md` exists and documents the full pipeline.

# Technical Design

### Current Implementation
The schema (`tracker.schema.json` → `eventTypeValue`) defines canonical event types: `ADD_NOTE`, `CORRECT_STATUS`, `RECALCULATE_FIT`, `ADD_ARTIFACT`, etc. The `buildEvent()` function in `lib/events.py:127` validates `eventType` against `contract.enums("eventType")`, which reads from this schema.

However, several call sites pass `event:*` prefixed strings (e.g., `"event:note-added"`, `"event:status-corrected"`, `"event:fit-recalculated"`, `"event:artifact-added"`) which are not in the schema enum, causing `EventError`.

The read-side code (`NON_STATUS_EVENTS` set, `COUNTER_RULES` dict, `renderNote` default) uses `event:*` strings to match legacy Firestore records — this is correct and must not change.

### Key Decisions
- **Only fix write-side calls**: `buildEvent()` is the write path. Read-side lookups stay as-is to handle legacy data.
- **Sync all copies**: `lib/alignment.py` is the active module (via `PYTHONPATH`), but `scripts/alignment.py` and `lib/tracker/alignment.py` are exact copies that must stay in sync.
- **No schema migration needed**: legacy records with `event:*` types remain valid; only new writes use canonical names.

### Proposed Changes

#### Write-side event type fixes
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

#### Test updates
- `scripts/tests/test_tracker_align.py:33` — `notesRendered` expected value contains `event:note-added` → `ADD_NOTE`.

#### Email pipeline documentation
- New file: `docs/mise/email-pipeline.md` — end-to-end workflow guide covering `email:read-inbox` → `email:collect` → proposal review, with command table and data flow explanation.

# Delivery Steps

### ✓ Step 1: Fix event type strings in tracker.py and alignment.py
Replace all `event:*` prefixed strings passed to `buildEvent()` with canonical schema enum names so `mise run align` and related commands no longer crash.

- `lib/alignment.py:350` — `"event:note-added"` → `"ADD_NOTE"`
- `scripts/tracker.py:770` — `"event:note-added"` → `"ADD_NOTE"` (commandAdd, --note flag)
- `scripts/tracker.py:1167` — `"event:note-added"` → `"ADD_NOTE"` (commandUpsert, notes from payload)
- `scripts/tracker.py:1634` — `"event:status-corrected"` → `"CORRECT_STATUS"` and `"event:note-added"` → `"ADD_NOTE"` (commandEdit)
- `scripts/tracker.py:1736` — `"event:status-corrected"` → `"CORRECT_STATUS"` (commandStatus)
- `scripts/tracker.py:1853` — `"event:fit-recalculated"` → `"RECALCULATE_FIT"` (commandScore)
- `scripts/tracker.py:1966` — `"event:artifact-added"` → `"ADD_ARTIFACT"` (commandArtifact)
- Sync identical copies in `scripts/alignment.py` and `lib/tracker/alignment.py` (exact duplicates)
- Update test expectations in `scripts/tests/test_tracker_align.py:33` where `notesRendered` contains `event:note-added` → `ADD_NOTE`

### ✓ Step 2: Add email-to-Firebase pipeline workflow doc
Create a clear end-to-end workflow guide at `docs/mise/email-pipeline.md` that documents the full email→Firestore pipeline so users know exactly which commands to run and in what order.

- Document the 3-step pipeline: `mise run email:read-inbox` → `mise run email:collect` → proposal review (`mise run proposals:list` / `proposals:approve`)
- Explain what each step produces (inbox payload JSON, staged proposals in Firestore, applied changes)
- Include the optional `mise run email:sync-labels` step for label caching
- Add a quick-reference command table with descriptions and typical flags
- Update `docs/mise/` index if one exists, or ensure the doc is discoverable via `mise run mise:docs:tasks`