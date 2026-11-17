# Implementation Plan: Status Enum, Interview State Machine, and Proposal Inspect Table

**Status:** Completed  
**Date:** 2026-11-11  
**Target:** `records` and `frontend` (cross-subsystem)  
**Harness:** Gemini  
**Model:** gemini-2.5-pro  
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)  

---

## 1. Executive Summary & Goals

Three interconnected architectural and operational issues need addressing across the job application tracker stack:

1. **Inline Proposal Inspect Table**: When running `mise tracker:proposals approve -i` and issuing the inspect command `i <id>`, the TUI should display a structured table of proposed field modifications (application ID, status transition, confidence, email subject). Currently it delegates to `tracker.py proposals show` via subprocess, and the in-memory `ProposalRecord` in `Main.kt` fails to populate its `changes` and `unmatched` collections.
2. **Lifecycle-Ordered Status Enum with Uniform Tense**: The legacy status enum (`LEAD`, `DRAFTING`, `APPLIED`, `INTERVIEW`, `OFFER_EXPECTED`, `HIRED`, `REJECTED`, `NO_RESPONSE`, `OFFER_DECLINED`, `APPLICATION_WITHDRAWN`) mixes terminal and active states, lacks explicit pre/post-offer negotiation stages, and mixes tenses inconsistently (`DRAFTING` vs. `HIRED`). The contract must be rebuilt around a production-grade lifecycle sequence: terminal states first (ranks 0–3), active funnel states chronologically (ranks 4–10), and terminal success (rank 11).
3. **Flexible Interview Round Data Contract**: When an application moves into multi-round interview loops (e.g. `vanguard--mobile-architect` entering Round 1 of 3), the schema has only a single flat `INTERVIEW` status. There is no typed schema to track current round index, total expected rounds, interview format classification, or round timestamps.

---

## 2. Architecture & Design Principles

### Design Principles
1. **Production-First Schema ("Build the Future Template")**:
   Avoid polluting the Proto3 enum with alias tags (`DRAFTING = 13`, `APPLIED = 14`). Instead, define the canonical 12 lifecycle states cleanly with tags 1–12. Backward compatibility for legacy Firestore documents is handled strictly at the read/ingest boundary inside `StatusNormalizer` and `contract.py`.
2. **Consistent Tense & Terminology**:
   - Outcome and completed milestone states use resultative past-participles or conditions: `DRAFTED`, `SUBMITTED`, `RECRUITER_SCREENED`, `HIRED`, `REJECTED`, `OFFER_DECLINED`, `APPLICATION_WITHDRAWN`, `NO_RESPONSE`.
   - Continuous multi-step stages use present participles: `INTERVIEWING`, `NEGOTIATING_PRE_OFFER`, `NEGOTIATING_POST_OFFER`.
   - Formal offer uses the clear noun milestone: `OFFER`.
3. **Three-Timestamp Temporal Contract for Interview Rounds**:
   - `interview_timestamp`: The actual point-in-time when the interview session occurs.
   - `scheduled_at`: When the interview slot was confirmed or locked in via email/calendar.
   - `completed_at`: When the interview concluded and debrief/feedback was captured.
4. **Compile-Time Exhaustive Kotlin Sealed Hierarchy**:
   `JobApplicationStatus` separates `Terminal` outcomes from `Active` funnel stages. `Active.Interviewing` is a `data class` holding `InterviewProgress`, while all other states are singleton `data object`s, guaranteeing null-safe pattern matching.

---

## 3. Proposed Changes & Implementation Steps

### Step 1: Proposal Inspect Inline Table (`Main.kt`)
- In `frontend/apps/terminal/src/main/kotlin/dev/fnc314/tools/jobtracker/frontend/terminal/Main.kt`:
  - In `loadPendingProposals()`: pass `changes` and `unmatched` JSON arrays directly from the proposal payload into the `ProposalRecord` constructor (both for primary and cache fallback).
  - In `inspectProposal()`: replace the `ProcessBuilder` subprocess call with an inline Mordant table rendering columns `#`, `Application ID`, `Status Transition`, `Confidence`, and `Email Subject`, plus a second table for unmatched emails.
- Verify via `./gradlew :frontend:apps-terminal:test` and install the distribution.

### Step 2: Proto3 Schema Clean Rebuild (`records/proto`)
- In `records/proto/src/main/proto/tracker/v1/enums.proto`:
  - Rewrite `TrackerStatus` with canonical tags 0–12 (`NO_RESPONSE` = 1 through `HIRED` = 12).
  - Define `InterviewType` enum (`RECRUITER_SCREEN`, `TECHNICAL_KNOWLEDGE`, `TECHNICAL_DEMONSTRATION`, `SYSTEMS_DESIGN`, `BEHAVIORAL`, `ON_SITE`, `FINAL_ROUND`).
- Create `records/proto/src/main/proto/tracker/v1/interview.proto`:
  - Define `InterviewRound` message (`current_round`, `total_rounds`, `type`, `title`, `interview_timestamp`, `scheduled_at`, `completed_at`, `notes`).
- In `records/proto/src/main/proto/tracker/v1/application.proto`:
  - Add `InterviewRound current_interview_round = 25;` to `JobApplication`.
- In `records/proto/src/main/proto/tracker/v1/event.proto`:
  - Add `InterviewRound interview_round = 16;` to `TrackerEvent`.
- Run `./gradlew :records:proto:compileKotlin` to generate Kotlin bindings.

### Step 3: JSON Schema & Python Layer Parity
- In `tracker.schema.json`:
  - Update `statusValue` to the 12 canonical values, update `x-status-rank` (0–11), and add `interviewRound` definition. Bump `schemaVersion` from 10 to 11.
- In `scripts/contract.py`:
  - Update `normalizeStatus()` to map legacy strings to canonical values on read.
- In `scripts/events.py`:
  - Update `EVENT_IMPLIED_STATUS` mapping.
- In `scripts/tracker.py`:
  - Add CLI flags `--interview-round <N>`, `--interview-total <N>`, and `--interview-type <TYPE>` to the `edit` command.

### Step 4: Kotlin Parser & Domain Model Layer (`records/parser`)
- In `records/parser/src/main/kotlin/dev/fnc314/tools/jobsearch/parser/StatusNormalizer.kt`:
  - Update status rank map (0–11), terminal statuses set, and legacy alias normalizer.
- In `records/parser/src/main/kotlin/dev/fnc314/tools/jobsearch/parser/JobApplicationStatus.kt`:
  - Implement `JobApplicationStatus` sealed interface with `Terminal` and `Active.Interviewing`.
- In `records/parser/src/main/kotlin/dev/fnc314/tools/jobsearch/parser/InterviewProgress.kt`:
  - Implement `InterviewProgress` data class and `InterviewProgressType` enum with Firestore converters.
- Rename `EventBuilder.kt` to `JobApplicationEventBuilder.kt` and add `interviewRound` building support.
- In `ApplicationTimelineProjector.kt`: verify projections with new statuses.
- Run `./gradlew :records:parser:test` and `mise run dev:konsist`.

### Step 5: TUI Integration & Verification
- In `Main.kt`:
  - Update `ApplicationRecord` to carry `interviewProgress: InterviewProgress?`.
  - In detail view: display `Interview: Round 1/3 (Technical Knowledge)` when status is `INTERVIEWING`.
  - In list view: display `INTERVIEWING (1/3)` in the status column.
- In `scripts/alignment.py`: read round metadata and display structured stage information.
- Run `./gradlew :frontend:apps-terminal:test` and reinstall terminal app.

---

## 4. Verification Plan & Testing Checklist

- [x] Unit tests pass for terminal app: `./gradlew :frontend:apps-terminal:test`
- [x] Protobuf compilation succeeds: `./gradlew :records:proto:compileKotlin`
- [x] Parser unit tests pass: `./gradlew :records:parser:test`
- [x] Konsist architecture quality gates pass: `mise run dev:konsist`
- [x] Interactive proposal inspect displays table with zero subprocess lag: `mise tracker:proposals approve -i` -> `i <id>`
- [x] `tracker edit` records interview round and displays `INTERVIEWING (1/3)` in TUI

---

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-11/183900_proposal_inspect_table_and_interview_state_machine.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Craft a single commit of all changed files
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Push `git push origin main`
