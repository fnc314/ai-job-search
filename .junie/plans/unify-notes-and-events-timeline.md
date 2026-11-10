# Implementation Plan: Unify NOTES and Events Timeline in Job Application Details

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** `frontend` (`features/job-application-details`, `core/data/model`)  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Schema Authority:** [`tracker.schema.json`](../tracker.schema.json)

---

## 1. Executive Summary & Goals

The NOTES section and Events Timeline in the job application details screen (`features/job-application-details`) are completely disconnected. The NOTES section displays a pre-rendered markdown string (`notesRendered`) from the Firestore document, while the Events Timeline shows at most 2 synthetic fallback events because real events are never loaded from Firestore. This plan unifies them so that events are the single source of truth: every NOTES entry is an event, the NOTES section is removed, and the Events Timeline becomes the sole display of activity history.

**Goals:**
- Load real events from the `applications/{id}/events` subcollection on screen load.
- Fix `recordEvent()` to write to the correct subcollection (per schema).
- Add structured `scheduledAt` date-time to `TrackerEvent` for interview scheduling.
- Remove the redundant NOTES section; the Events Timeline replaces it.
- Keep `notesRendered` on the backend for backward compatibility (reports, sheet sync).

**User Stories:**
- As a user, I want every activity note I add to appear in the Events Timeline so I have a structured, chronological history.
- As a user, I want to schedule an interview with a date-time and see that date-time in the timeline.
- As a user, I want the Events Timeline to show all real events from the database, not just synthetic fallback entries.

**Scope:**

| In Scope | Out of Scope |
|----------|--------------|
| Frontend: event loading infrastructure, ViewModel changes, UI changes, `TrackerEvent` model extension, `RecordEventDialog` date-time picker, `EventsTimeline` display of `scheduledAt` | Backend `notesRendered` rendering logic (already correct) |
| Schema: add `scheduledAt` to event schema in `tracker.schema.json` | Migration of existing events from top-level `events` collection |
| Tests: update `JobApplicationDetailViewModelTest.kt` | Other features or screens outside `job-application-details` |

## 2. Architecture & Design Principles

### Current Implementation

**NOTES section** (`JobApplicationDetailsScreen.kt` lines 240-252, 299-313):
- Displays `model.notesRendered` — a raw markdown string stored on the `JobApplication` Firestore document.
- The backend (`scripts/render.py::renderNotes()` and `records/parser/EventNoteRenderer.kt::renderNotes()`) correctly derives this string FROM events.
- The schema (`tracker.schema.json` line 746) declares: `"DERIVED, read-only. Rendered from events. No writer may set this directly."`
- The UI shows it as plain `Text` in an `OutlinedCard`.

**Events Timeline** (`EventsTimeline.kt`):
- Renders `model.timelineEvents` — a `List<TrackerEvent>`.
- The ViewModel's `handleLoad()` sets `currentEvents = emptyList()` — **events are never loaded from Firestore**.
- `deriveTimelineEvents()` creates synthetic fallback events from `createdAt`/`updatedAt` only when `events.isEmpty()`.
- Result: the timeline shows at most 2 synthetic events instead of the real event log.

**Current Architecture:**
```
JobApplicationDetailsScreen
  ├── NOTES section → model.notesRendered (raw String from Firestore doc)
  └── EventsTimeline → model.timelineEvents (empty list, synthetic fallback)

JobApplicationDetailViewModel.handleLoad()
  ├── loadApplicationUseCase → JobApplication (includes notesRendered string)
  ├── currentEvents = emptyList()  ← PROBLEM: never fetches events
  └── createJobApplicationDetailUiModel() → deriveTimelineEvents() → synthetic events

RemoteJobApplicationDetailsDataSource
  ├── getJobApplication() ✓
  ├── recordEvent() ✓ (writes to top-level "events" collection)
  ├── getCompanies() ✓
  └── getEvents() ✗  ← MISSING
```

### Key Design Decisions

1. **Events loaded from `applications/{id}/events` subcollection** — per schema `tracker.schema.json` line 72. The frontend `recordEvent` currently writes to a top-level `events` collection — this needs to change to the subcollection pattern.

2. **NOTES section removed as a separate section** — the Events Timeline already displays each event's type, note, and timestamp. The schema's intent is that `notesRendered` is a derived projection of events — the timeline IS that projection, just structured. Removing the redundant NOTES section eliminates the dual-source problem entirely.

3. **Structured `scheduledAt` field on TrackerEvent** — the `TrackerEvent` model gains an optional `scheduledAt: Instant?` field. The `RecordEventDialog` gains a date-time picker that appears when `SCHEDULE_INTERVIEW` or `VISIT_ONSITE` is selected.

4. **Frontend uses `EventNoteRenderer` logic** — the UI model renders notes from events using the same rules the backend uses, keeping frontend consistent with backend rendering.

### Architecture Diagram

``` mermaid
graph LR
  Screen[JobApplicationDetailsScreen] --> VM[JobApplicationDetailViewModel]
  VM -->|getEvents| GetEventsUC[GetEventsUseCase]
  VM -->|recordEvent| RecordEventUC[RecordEventUseCase]
  GetEventsUC --> Repo[JobApplicationDetailsRepository]
  RecordEventUC --> Repo
  Repo --> DataSource[RemoteJobApplicationDetailsDataSourceImpl]
  DataSource -->|read| Subcollection[applications/id/events]
  DataSource -->|write| Subcollection
  Screen --> Timeline[EventsTimeline]
  Timeline -->|renders| Events[List of TrackerEvent]
```

## 3. Proposed Changes & Implementation Steps

### ✓ Step 1: Add event loading infrastructure
Add `getEvents(applicationId)` across data and domain layers so the UI can fetch real events from Firestore.

- Add `getEvents(applicationId: String): List<TrackerEvent>` to `RemoteJobApplicationDetailsDataSource` interface.
- Implement in `RemoteJobApplicationDetailsDataSourceImpl`: query `applications/{id}/events` subcollection, map to `TrackerEvent` domain model.
- Add `GetEventsUseCase` interface and implementation in domain layer.
- Add `getEvents(applicationId: String)` to `JobApplicationDetailsRepository` interface and `JobApplicationDetailsRepositoryImpl`.
- Wire in DI modules (`JobApplicationDetailsDataModule`, `JobApplicationsDetailsDomainModule`).

### ✓ Step 2: Fix event storage and add `scheduledAt` field
Events are written to the correct subcollection and gain a structured date-time for interview scheduling.

- Change `recordEvent()` in `RemoteJobApplicationDetailsDataSourceImpl` to write to `applications/{applicationId}/events` subcollection instead of top-level `events`.
- Add `scheduledAt: Instant?` field to `TrackerEvent` data class (`core/data/model/TrackerEvent.kt`).
- Add `scheduledAt` parameter to `Factory.createTrackerEvent()`.
- Map `scheduledAt` in `recordEvent()` Firestore write and event reading.
- Update `tracker.schema.json` event schema to include `scheduledAt`.

### ✓ Step 3: Load events in ViewModel and update RecordEventDialog
The ViewModel fetches real events on load; the Record Event dialog captures scheduled date-times.

- Update `handleLoad()` in `JobApplicationDetailViewModel` to call `getEventsUseCase()` and pass real events to `createJobApplicationDetailUiModel()`.
- Update `handleRecordEvent()` to accept and persist `scheduledAt`.
- Add `scheduledAt` to `JobApplicationDetailUiIntent.RecordEvent`.
- Add date-time picker to `RecordEventDialog` that appears when event type is `SCHEDULE_INTERVIEW` or `VISIT_ONSITE`.
- Display `scheduledAt` in `EventsTimeline` event cards.

### ✓ Step 4: Remove NOTES section and clean up UI model
Eliminate the redundant NOTES section; the Events Timeline becomes the single source of truth.

- Remove the NOTES section from `JobApplicationDetailsScreen.kt` in both wide (2-column) and compact (1-column) layouts.
- Remove `notesList` and `notesRendered` from `JobApplicationDetailUiModel` (deprecate, keeping for backward compat if needed).
- Remove `parseNotesList()` helper from `JobApplicationDetailUiModel.kt`.
- Update `createJobApplicationDetailUiModel()` to no longer pass notes fields.
- Update `deriveTimelineEvents()` to keep synthetic fallback only for records with genuinely zero events.
- Update tests (`JobApplicationDetailViewModelTest.kt`) to reflect the new event-loading behavior.

### File Structure

| File | Change |
|------|--------|
| `frontend/core/data/model/TrackerEvent.kt` | Add `scheduledAt` field |
| `frontend/core/data/factory/Factory.kt` | Add `scheduledAt` to `createTrackerEvent()` |
| `frontend/features/job-application-details/data/source/RemoteJobApplicationDetailsDataSource.kt` | Add `getEvents()` interface method |
| `frontend/features/job-application-details/data/source/impl/RemoteJobApplicationDetailsDataSourceImpl.kt` | Implement `getEvents()`, fix `recordEvent()` path, map `scheduledAt` |
| `frontend/features/job-application-details/domain/repository/JobApplicationDetailsRepository.kt` | Add `getEvents()` interface method |
| `frontend/features/job-application-details/data/repository/impl/JobApplicationDetailsRepositoryImpl.kt` | Implement `getEvents()` |
| `frontend/features/job-application-details/domain/usecase/` | Add `GetEventsUseCase` (interface + impl) |
| `frontend/features/job-application-details/domain/di/JobApplicationsDetailsDomainModule.kt` | Register `GetEventsUseCase` |
| `frontend/features/job-application-details/data/di/JobApplicationDetailsDataModule.kt` | Wire `GetEventsUseCase` dependencies |
| `frontend/features/job-application-details/ui/JobApplicationDetailViewModel.kt` | Load events, pass `scheduledAt` |
| `frontend/features/job-application-details/ui/JobApplicationDetailUiIntent.kt` | Add `scheduledAt` to `RecordEvent` intent |
| `frontend/features/job-application-details/ui/JobApplicationDetailUiModel.kt` | Remove `notesList`/`notesRendered`, keep `timelineEvents` |
| `frontend/features/job-application-details/ui/JobApplicationDetailsScreen.kt` | Remove NOTES section |
| `frontend/features/job-application-details/ui/DetailDialogs.kt` | Add date-time picker to `RecordEventDialog` |
| `frontend/features/job-application-details/ui/component/EventsTimeline.kt` | Display `scheduledAt` in event cards |
| `tracker.schema.json` | Add `scheduledAt` to event schema |
| `frontend/features/job-application-details/ui/JobApplicationDetailViewModelTest.kt` | Update tests for event loading |

## 4. Verification Plan & Testing Checklist

### Validation Approach

Validate that the Events Timeline displays real events loaded from Firestore and that the NOTES section is no longer present. Verify that recording an event with a scheduled date-time persists and displays correctly.

### Key Scenarios

1. **Screen load with existing events** — open a job application with events in the `applications/{id}/events` subcollection; timeline shows those events.
2. **Screen load with no events** — open a job application with no events; timeline shows synthetic fallback events.
3. **Record event with scheduled date-time** — record a `SCHEDULE_INTERVIEW` event with a date-time; event appears in timeline with the scheduled date-time displayed.
4. **Record event without scheduled date-time** — record a `NOTE_ADDED` event; event appears in timeline without a scheduled date-time.
5. **NOTES section removed** — the screen no longer shows a separate NOTES section.

### Edge Cases

1. **Events in wrong collection** — existing events written to the top-level `events` collection are not loaded; only events in the subcollection are shown. This is acceptable as a migration boundary.
2. **Missing `scheduledAt` on old events** — events without `scheduledAt` render normally without the date-time badge.
3. **Zero events** — synthetic fallback events are shown to avoid an empty timeline.

### Test Changes

- Update `JobApplicationDetailViewModelTest.kt` to verify event loading behavior.
- Add tests for `GetEventsUseCase`.
- Add tests for `RecordEvent` with `scheduledAt`.

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-10/173600_unify_notes_and_events_timeline.md`
- [x] Registry `.agents/plans/README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`