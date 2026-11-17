# Implementation Plan: Pure Kotlin Migration of Event Projection & History Rendering (`scripts/render.py` & `scripts/events.py`)

**Status:** Proposed  
**Date:** 2026-11-10  
**Target:** `records/parser`, `backend/`, `frontend/apps/cli/`, `scripts/render.py`, `scripts/events.py`  
**Evaluates:** `RM-001` (Kotlin-Only Migration) & `RM-003` (Backend Functionality Abstraction) in [ROADMAP.md](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ROADMAP.md)  
**Schema Authority:** `tracker.schema.json` v10 (`$defs/event`, `$defs/timeline`, `$defs/counters`, `$defs/application`)

---

## Goal Description
Following the migration order specified in `RM-003` (migrating deterministic Python logic to Kotlin in NEWEST-first order) and `RM-001` (incremental migration of all 56 Python scripts), the next high-impact deterministic module for complete migration is **`scripts/render.py`** and the event building/projection logic in **`scripts/events.py`**.

Currently:
1. `EventChainBuilder` and `ApplicationTimelineProjector` in `records/parser` already handle cryptographic hash chaining and timeline/submissionConfirmation derivations.
2. However, **note history rendering** (`renderNotes`, `renderNote`), **funnel status derivations** (`latestStatus`, `deriveStatusFields`), **counter calculations** (`counters`), and **event building & validation** (`buildEvent`) are still duplicated or trapped in `scripts/render.py` and `scripts/events.py`.
3. Calls across `scripts/tracker.py`, `scripts/proposals.py`, and `scripts/reconcile_submissions.py` still invoke `render.project(app, events)` and `eventsModule.buildEvent(...)`.

This plan ports all projection and event-rendering logic from `scripts/render.py` and `scripts/events.py` to pure Kotlin in `records/parser` (`ApplicationEventProjector.kt` and `EventNoteRenderer.kt`), exposes unified projection via `backend/` and `frontend/apps/cli`, and bridges/replaces the Python calls with direct Kotlin-backed derivations.

---

## User Review Required
> [!IMPORTANT]
> **Diminishing Lifecycle & Retirement Roadmap for `scripts/render.py`:**
> `scripts/render.py` (and `lib/render.py`) will undergo a structured, 3-phase decommissioning:
> 1. **Phase 1 (Current Plan - Kotlin Authority):** Implement all projection, counter calculations, and markdown note rendering in Kotlin (`records/parser`). Mark `scripts/render.py` as formally **DEPRECATED**. Add deprecation docstring and log warning.
> 2. **Phase 2 (Immediate Follow-up - Direct Consumer Migration):** Update Python callers (`scripts/proposals.py`, `scripts/tracker.py`, `scripts/reconcile_submissions.py`) to bypass `render.py` by querying the centralized backend service (`POST /applications/{id}/align` or local Kotlin runner).
> 3. **Phase 3 (Final Removal - Zero References):** Once all callers are routed to Kotlin/backend endpoints, delete `scripts/render.py` and `lib/render.py`, along with their legacy test suite in `scripts/tests/test_reader.py`. Update `scripts/` inventory count in `ROADMAP.md` (RM-001 decrement).

> [!NOTE]
> **Notes Rendering Format Contract:**
> `renderNotes` generates human-readable history formatted as `<date> · <type> — <note>`, with the special legacy exemption that `event:imported` displays prose only without decorating prefixes. Our Kotlin implementation will adhere strictly to this contract.

---

## Proposed Changes

### 1. `records/parser/` (Pure Kotlin Multiplatform / JVM)
#### [NEW] `EventNoteRenderer.kt`
- Implements note formatting matching `scripts/render.py`:
  - `renderNote(event: TrackerEvent): String`
    - Returns note prose directly if event type is `IMPORT` / `event:imported`.
    - Otherwise formats as `"<date> · <type> — <note>"`.
  - `renderNotes(events: List<TrackerEvent>): String?`
    - Chronologically sorts events using sequence numbers/timestamps.
    - Joins non-empty rendered lines with `\n`. Returns `null` if empty.

#### [MODIFY] `ApplicationTimelineProjector.kt`
- Expand `ApplicationTimelineProjector` to become `ApplicationEventProjector`:
  - Add `projectNotes(events: List<TrackerEvent>): String?` delegating to `EventNoteRenderer.renderNotes`.
  - Add `projectLatestStatus(events: List<TrackerEvent>): TrackerStatus?` to determine the latest funnel status moved by an event.
  - Implement full projection method `project(existingApp: JobApplication?, events: List<TrackerEvent>): ApplicationProjectionResult` returning:
    - `timeline: Timeline`
    - `submissionConfirmation: SubmissionConfirmation`
    - `counters: Map<String, Int>`
    - `notesRendered: String?`
    - `impliedStatus: TrackerStatus?`
    - `isOpen: Boolean`
    - `statusRank: Int`

#### [NEW] `ApplicationEventProjectorTest.kt`
- Add comprehensive unit tests covering:
  - `renderNote` formatting with and without notes.
  - `event:imported` note prefix omission.
  - Chronological ordering and line joining in `renderNotes`.
  - Full `project` roundtrip comparing counters, notes, timeline, and status derivations against test fixtures.

---

### 2. `backend/` (Kotlin Backend Service)
#### [MODIFY] `BackendService.kt`
- Update `appendEventTransactional` and application ingestion to use `EventNoteRenderer.renderNotes` and full projection:
  - Persists `notesRendered` alongside `timeline`, `submissionConfirmation`, and `counters` on the parent application document.

#### [MODIFY] `Main.kt`
- Add route `POST /applications/{id}/align`:
  - Consistent with the repository's task and CLI naming (`mise run tracker:align`, `AlignCommand`), aligns the parent application document with its events subcollection: recalculates derived `notesRendered` via `EventNoteRenderer`, recomputes `timeline`, `counters`, and `submissionConfirmation`, and merges the update atomically into Firestore.
  - Supports query flag `?dryRun=true` to preview the aligned payload without mutating the database.

---

### 3. `scripts/` (Python Deprecation & Shimming)
#### [MODIFY] `scripts/render.py` & `lib/render.py`
- Update docstring noting deprecation in favor of Kotlin `records/parser` `ApplicationTimelineProjector` / `EventNoteRenderer`.
- Ensure fallback behavior matches byte-for-byte with Kotlin unit test expectations.

---

### 4. `ROADMAP.md`
#### [MODIFY] `ROADMAP.md`
- Update `RM-001` completion percentage from 25% to 35% with changelog entry.
- Update `RM-003` completion percentage from 60% to 75% with changelog entry.
- Link this plan in `Linked Plans`.

---

## Verification Plan

### Automated Tests
1. **Records Subsystem Build & Tests:**
   ```bash
   ./gradlew -p records check
   ```
2. **Backend Service Tests:**
   ```bash
   ./gradlew -p backend test
   ```
3. **Frontend CLI Build:**
   ```bash
   ./gradlew -p frontend :apps-cli:assemble
   ```
4. **Tracker & Schema Check:**
   ```bash
   mise run tracker:check
   mise roadmap:status
   ```

### Manual Verification
1. Inspect generated `notesRendered` in Firestore or via `./gradlew -p backend test` verifying that dates, separators (` · `), and dashes (` — `) match schema and existing records exactly.
