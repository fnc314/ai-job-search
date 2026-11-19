# Implementation Plan: Canonical Status Unification (`APPLIED` vs `SUBMITTED`)

**Status:** Completed  
**Date:** 2026-11-18  
**Target:** repository-wide (`records`, `frontend`, `backend`, `data`)  
**Harness:** Antigravity  
**Model:** gemini-3.8-flash  
**Session:** 7d41c538-a103-4dd2-87e5-83b2d8bf74d7  
**Schema Authority:** [`tracker.schema.json`](file:///Users/fnc314/Work/Search/ai-job-tracker/tracker.schema.json)  

---

## 1. Executive Summary & Goals

### The Problem
A dual-vocabulary status discrepancy exists across subsystems:
1. **The Protobuf & Backend Tier** defines `TrackerStatus.SUBMITTED = 6` (`records/proto/src/main/proto/tracker/v1/enums.proto`) and normalizes inbound status to `"SUBMITTED"`.
2. **The JSON Schema Contract** (`tracker.schema.json`) declares `"SUBMITTED"` as the canonical status enum and maps `"applied"` $\rightarrow$ `"SUBMITTED"`.
3. **The Frontend CMP Tier** (`frontend/core/data/.../TrackerStatus.kt`) defines `Known.APPLIED` (wire key `"APPLIED"`). It maps `"submitted"` into `APPLIED`, but when serializing via `.toRemote()` it writes `"APPLIED"`.
4. **The Database (Firestore)** currently stores `"APPLIED"` on ~82 documents and `"SUBMITTED"` on ~4 documents.
5. **Timeline Events**: The event type is `APPLY` (wire key `APPLY` or legacy `event:applied`), but it produces `statusTo = "SUBMITTED"` in some code paths and `statusTo = "APPLIED"` in others.

This ambiguity causes subtle discrepancies:
- Firestore queries filtering by `status == "APPLIED"` miss `"SUBMITTED"` records and vice versa.
- UI cards, filters, and count badges show varying labels ("Applied" vs "Submitted") across the Terminal TUI, Web Dashboard, and Android app.
- Status rank in `tracker.schema.json` assigns rank 5 to `SUBMITTED` and rank 2 to `APPLIED`.

### The Goal
Rectify and standardize this status across the entire codebase—from Protobuf definitions, Schema, Parsers, Frontend Domain/Data/UI, CLI, and Firestore records—establishing a single canonical status wire key and presentation label with explicit legacy compatibility.

---

## 2. Architecture & Design Decisions

### Primary Design Choice: `SUBMITTED` as Canonical Funnel State
We propose adopting **`SUBMITTED`** as the single canonical wire key and enum identifier across all layers:
- **Grammatical Consistency**: All other active and terminal milestone states are past-participles (`DRAFTED`, `SUBMITTED`, `RECRUITER_SCREENED`, `INTERVIEWING`, `OFFER_EXPECTED` / `OFFER`, `HIRED`, `REJECTED`, `NO_RESPONSE`, `OFFER_DECLINED`, `APPLICATION_WITHDRAWN`).
- **Protobuf Authority**: `enums.proto` already defines `TrackerStatus.SUBMITTED = 6`.
- **JSON Schema Authority**: `tracker.schema.json` defines `"SUBMITTED"` in its closed `statusValue` enum.
- **Event vs State Disambiguation**: The *action/event* is `APPLY` (or `event:applied`), which transitions the entity into the *state* of `SUBMITTED`.
- **User-Facing UI Display**: The user-facing display label can remain **"Applied"** or **"Submitted"** (configurable, defaulting to "Applied" or "Submitted" consistently across CMP, Android, TUI, and Web).

---

## 3. Proposed Changes

### Component 1: Protobuf & Records Layer (`records/`)
- **`records/proto/src/main/proto/tracker/v1/enums.proto`**:
  - Retain `TrackerStatus.SUBMITTED = 6`. Add deprecated alias comment for `APPLIED`.
- **`records/parser/src/main/kotlin/dev/fnc314/aijobtracker/records/parser/JobApplicationStatus.kt`**:
  - Ensure canonical object is `SUBMITTED` (wire name `"SUBMITTED"`).
- **`records/parser/src/main/kotlin/dev/fnc314/aijobtracker/records/parser/JobApplicationEventBuilder.kt`**:
  - Ensure `TrackerEventType.APPLY` maps to `TrackerStatus.SUBMITTED`.
- **`records/parser/src/main/kotlin/dev/fnc314/aijobtracker/records/parser/StatusNormalizer.kt`**:
  - Normalize both `"applied"` and `"submitted"` $\rightarrow$ `"SUBMITTED"`.

### Component 2: Frontend Core Data & Models (`frontend/core/data/`)
- **`frontend/core/data/src/commonMain/kotlin/dev/fnc314/aijobtracker/frontend/core/data/model/TrackerStatus.kt`**:
  - Rename `Known.APPLIED` to `Known.SUBMITTED` with `wireKey = "SUBMITTED"`.
  - Provide `@Deprecated` typealias/val `Known.APPLIED` pointing to `Known.SUBMITTED` for backward compatibility.
  - In `lookup` map: map `"applied"`, `"status:applied"`, `"submitted"`, `"status:submitted"` to `Known.SUBMITTED`.
  - `.toRemote()` returns `"SUBMITTED"`.
  - Update `sectionOrder`, `containerColor()`, and `contentColor()` for `Known.SUBMITTED`.
- **`frontend/core/data/src/commonTest/kotlin/dev/fnc314/aijobtracker/frontend/core/data/EnumLogicTest.kt`**:
  - Update unit tests to verify `TrackerStatus.fromRemote("APPLIED") == TrackerStatus.SUBMITTED` and `TrackerStatus.fromRemote("SUBMITTED") == TrackerStatus.SUBMITTED`.

### Component 3: Frontend Feature Modules & UI (`frontend/features/`, `frontend/apps/`)
- **`frontend/features/job-applications-list/`**:
  - Update filter menus and summary counts from `TrackerStatus.APPLIED` to `TrackerStatus.SUBMITTED`.
- **`frontend/features/job-application-details/`**:
  - Update status chip, action dialogs, and detail models to reference `TrackerStatus.SUBMITTED`.
- **`frontend/apps/terminal/` (TUI)**:
  - In `TrackerApp.kt`: standardize status coloring and filter handling for `SUBMITTED`.
- **`ui/web/application-dashboard.html`**:
  - Ensure dashboard stats treat `SUBMITTED` as canonical.

### Component 4: CLI Commands & Backend Integration
- **`frontend/apps/cli/src/main/kotlin/dev/fnc314/aijobtracker/frontend/cli/commands/`**:
  - `UpdateStatusCommand.kt`: normalize input status to `SUBMITTED`.
  - `AddApplicationCommand.kt` & `AddEventCommand.kt`: ensure applied state writes `SUBMITTED`.
  - `EmailCommand.kt`: staging changes for submission confirmations stages `statusTo = "SUBMITTED"`.
  - `ProposalCommand.kt`: applying changes updates parent status to `SUBMITTED`.

### Component 5: Database Migration & Schema (`meta/sync` and `applications`)
- **`tracker.schema.json`**:
  - Ensure `statusMap` and `statusValue` define `"SUBMITTED"` as rank 5, with `"applied"` mapped to `"SUBMITTED"`.
- **Firestore Data Migration Script**:
  - Migrate all existing documents in Firestore `applications` having `status == "APPLIED"` to `status: "SUBMITTED"` and `statusRank: 5`.
  - Update any event subcollection documents where `statusTo == "APPLIED"` to `statusTo: "SUBMITTED"`.

---

## 4. Verification Plan

### Automated Tests
1. `./gradlew -q :records:records-parser:test` — Verify timeline projections, status normalizers, and event builders.
2. `./gradlew -q :backend:email-classification:test` — Verify email classifier output and status transitions.
3. `./gradlew -q :frontend:core-data:test` — Verify `TrackerStatus` parsing and backward compatibility.
4. `./gradlew -q :frontend:features-job-applications-list-domain:test :frontend:features-job-applications-list-ui:test` — Verify list filters and view models.
5. `mise run dev:test` — Full monorepo verification suite across all 42 modules.

### Manual Verification
1. Run `./gradlew -q :frontend:apps-cli:run --args="tracker list --json"` and verify no unrecognized status errors occur.
2. Run `mise run tracker get walmart--staff-software-engineer-android-ios` and verify status renders cleanly.
3. Launch Android app (`@frontend/apps/android`) or Desktop CMP app to verify the status badges, filters, and detail screen display as expected.

## 5. Bookkeeping
- [ ] Plan persisted at `.agents/plans/2026-11-18/111300_canonical_status_unification_applied_vs_submitted.md`
- [ ] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Push to main
