# Implementation Plan: Apply Assistant Android

**Status:** Proposed  
**Date:** 2026-11-18  
**Target:** `frontend`, `backend`, and `records` (cross-subsystem)  
**Harness:** Antigravity  
**Model:** gemini-2.5-pro  
**Evaluates:** [`apply-assistant-android.md`](../../.junie/plans/apply-assistant-android.md), [`session-261118-102429-10p9`](/Users/fnc314/.junie/sessions/session-261118-102429-10p9/transcript.md)  
**Schema Authority:** [`tracker.schema.json`](../tracker.schema.json)  

---

## 1. Executive Summary & Goals

Transform the in-app `Edits` destination (represented by the active/filled `Pencil` icon on the bottom navigation bar and navigation rail) into a unified **Assistant & Proposal Hub**. Users can evaluate job postings, generate tailored application artifacts, and manage incoming email proposals entirely within this screen—**with all floating action buttons (`FAB`s) completely eliminated across the entire application**.

### Lifecycle & Interaction Flow
1. **No FABs Anywhere:** Remove floating action buttons from `JobApplicationsListScreen`, `JobApplicationDetailsScreen`, and do not introduce one in `AppScaffold`. Navigation to and creation of workflows happens directly in the UI.
2. **Edits Tab as the Hub (`JobApplicationEditsRoute`):**
   - Top-level destination remains in the bottom navigation / navigation rail using `Icons.Filled.Edit` (active) / `Icons.Outlined.Edit` (unselected).
   - The screen provides tabbed or segmented access:
     - **Apply Assistant Tab:** Full chat-style / interactive assistant workflow for URL or raw Job Description input, fit evaluation, and artifact generation.
     - **Pending Edits Tab:** Review and approve/reject email proposal diffs.
3. **Status Funnel Progression & Tag Ordering:**
   - **`DRAFTING`** (wireTag = 11, wireKey = `"DRAFTING"`): Async artifact drafting is actively underway (LaTeX generation and PDF compilation in flight).
   - **`DRAFTED`** (wireTag = 12, wireKey = `"DRAFTED"`): Artifact generation has completed successfully, or application draft is prepared; pending submission.
   - **`RANKED`** (wireTag = 13, wireKey = `"RANKED"`): Job posting evaluated by `RankScorer`, outcome and fit score recorded, awaiting decision to generate assets.
   - **Funnel Progression:**
     1. Instant evaluation via `RankScorer` ($0 budget) $\rightarrow$ records tracked as **`RANKED`**.
     2. "Generate assets?" = Yes $\rightarrow$ triggers async job, transitions record to **`DRAFTING`**.
     3. Backend completes LaTeX + PDF compilation $\rightarrow$ transitions record to **`DRAFTED`** (artifacts attached).
     4. "Did you submit the application?" = Yes $\rightarrow$ transitions record to **`SUBMITTED`** (canonical wireTag = 2, wireKey = `"SUBMITTED"`). If No $\rightarrow$ remains **`DRAFTED`**.

---

## 2. Architectural Invariants & Codebase Compliance

### Invariant 1: Single Abstract Method (SAM) & Functional Interfaces
Per repository rule and `SingleAbstractMethodArchitectureTest.kt`:
- Every domain use case with a single abstract method MUST be declared as `fun interface`:
  ```kotlin
  public fun interface EvaluateJobUseCase {
      public suspend operator fun invoke(input: EvaluateJobInput): Result<RankResult>
  }
  ```
- All callers must invoke instances directly via `invoke` operator syntax (`evaluateJobUseCase(input)`).

### Invariant 2: Interface-First Naming (Zero Vendor Leaks)
Per repository rule and `docs/interface-naming-baseline.txt`:
- All public interfaces must use abstraction vocabulary:
  - `NetworkApplyAssistantDataSource` (raw HTTP to backend Ktor)
  - `RemoteApplyAssistantDataSource` (Firestore listener and mutations)
  - `LocalApplyAssistantDataSource` (Room 3 database)
- Backend module for AI drafting MUST be named `backend/ai-client` (or `agent-client`), exposing `ArtifactDraftingService` as a clean facade. No tool or vendor names in public declarations.

### Invariant 3: Multiplatform Room 3 Database Schema
`AppDatabase` in `frontend/core/data/.../AppDatabase.kt` is the central KMP database. To allow `ApplySessionEntity` and `ApplyMessageEntity` to be queried reactively and survive process death:
- Register `ApplySessionEntity` and `ApplyMessageEntity` in `AppDatabase` (incrementing database version).
- Expose `ApplyAssistantDao` from `AppDatabase`.
- `features/apply-assistant/data` leverages `AppDatabase` provided via Koin.

### Invariant 4: Complete Elimination of Floating Action Buttons (FABs)
- Remove `FloatingActionButton` from `JobApplicationsListScreen.kt` and `JobApplicationDetailsScreen.kt`.
- Replace "Add Application" and "Add Lead" FAB triggers with actions in the top app bar or top-level controls on their respective screens.
- Remove `MaterialTheme.spacing.fabBottomPadding` workarounds and list cut-off hacks.

---

## 3. Data Models & Wire Contracts

### 3.1 Protobuf & Tracker Schema
- `records/proto/src/main/proto/tracker/v1/enums.proto`:
  ```protobuf
  enum TrackerStatus {
    TRACKER_STATUS_UNSPECIFIED = 0;

    // Terminal inactive states (ranks 0-3)
    NO_RESPONSE = 1;
    OFFER_DECLINED = 2;
    APPLICATION_WITHDRAWN = 3;
    REJECTED = 4;

    // Active funnel states (ranks 4-10)
    DRAFTING = 11;               // Async artifact generation / active drafting in progress
    DRAFTED = 12;                // Application draft and artifacts prepared; pending submission
    RANKED = 13;                 // Posting evaluated, fit score recorded, not yet drafted
    SUBMITTED = 6;               // Application formally submitted to company/portal
    RECRUITER_SCREENED = 7;
    INTERVIEWING = 8;
    NEGOTIATING_PRE_OFFER = 9;
    OFFER = 10;
    NEGOTIATING_POST_OFFER = 11;

    // Terminal success (rank 11)
    HIRED = 12;
  }
  ```
- `frontend/core/data/.../TrackerStatus.kt`:
  ```kotlin
  DRAFTING(displayName = "Drafting", wireKey = "DRAFTING", wireTag = 11),
  DRAFTED(displayName = "Drafted", wireKey = "DRAFTED", wireTag = 12),
  RANKED(displayName = "Ranked", wireKey = "RANKED", wireTag = 13),
  SUBMITTED(displayName = "Submitted", wireKey = "SUBMITTED", wireTag = 2),
  ```
  - `DRAFTING`, `DRAFTED`, `RANKED` are active (`isActive = true`, `isTerminal = false`, `sectionOrder = 2`).

### 3.2 Backend HTTP API Contracts

#### `POST /applications/apply`
Replaces the existing hardcoded stub in `backend/service/src/main/kotlin/.../ApplicationRoutes.kt`:
```json
// Request Body:
{
  "url": "https://example.com/jobs/123",
  "text": "Raw job description text...",
  "company": "Optional Company",
  "title": "Optional Title"
}

// Response (200 OK):
{
  "score": 85,
  "verdict": "STRONG_MATCH",
  "strengths": ["Kotlin", "Jetpack Compose", "Android SDK"],
  "gaps": ["GraphQL"],
  "dealBreakers": [],
  "salaryEstimate": "$170k - $190k"
}
```

#### `POST /artifacts/create`
Async artifact generation in `backend/service/src/main/kotlin/.../ArtifactRoutes.kt`:
```json
// Request Body:
{
  "applicationId": "app_20261118_walmart_lead",
  "company": "Walmart",
  "role": "Principal Android Engineer",
  "postingText": "Job posting content..."
}

// Response (202 Accepted):
{
  "jobId": "job_art_abc123",
  "status": "ACCEPTED"
}
```

#### Firestore Collection: `artifact_jobs/{jobId}`
```json
{
  "jobId": "job_art_abc123",
  "applicationId": "app_20261118_walmart_lead",
  "status": "PENDING | IN_PROGRESS | SUCCESS | FAILED",
  "cvArtifactId": "walmart_cv_20261118",
  "cvPdfUrl": "https://storage.googleapis.com/.../cv.pdf",
  "coverLetterArtifactId": "walmart_cover_letter_20261118",
  "coverLetterPdfUrl": "https://storage.googleapis.com/.../cover_letter.pdf",
  "error": null,
  "updatedAt": "2026-11-18T16:30:00Z"
}
```

### 3.3 Room Database Schema (`frontend/core/data`)
- `ApplySessionEntity`:
  - `id`: String (UUID primary key)
  - `applicationId`: String? (associated `JobApplication` id)
  - `postingUrl`: String?
  - `postingText`: String?
  - `company`: String
  - `role`: String
  - `rankResultJson`: String?
  - `artifactJobId`: String?
  - `artifactJobStatus`: String? (`PENDING`, `SUCCESS`, `FAILED`)
  - `cvPdfUrl`: String?
  - `coverLetterPdfUrl`: String?
  - `currentStep`: String (`AWAITING_INPUT`, `EVALUATING`, `AWAITING_GENERATE_CONFIRMATION`, `DRAFTING`, `DRAFTED`, `AWAITING_SUBMISSION_CONFIRMATION`, `SUBMITTED`)
  - `updatedAt`: Long
- `ApplyMessageEntity`:
  - `id`: String (UUID primary key)
  - `sessionId`: String (foreign key / index)
  - `sender`: String (`USER`, `ASSISTANT`, `SYSTEM`)
  - `messageType`: String (`TEXT`, `EVALUATION_CARD`, `YES_NO_PROMPT`, `ARTIFACTS_CARD`)
  - `payloadJson`: String?
  - `timestamp`: Long

---

## 4. Implementation Steps

### Phase 1: Status Alignment & Records Subsystem
1. Update `records/proto/src/main/proto/tracker/v1/enums.proto`:
   - Set `DRAFTING = 11`, `DRAFTED = 12`, `RANKED = 13`.
2. Update `tracker.schema.json`:
   - Add `"DRAFTING"`, `"DRAFTED"`, `"RANKED"` to allowed enum values and ranks.
3. Update `frontend/core/data/.../TrackerStatus.kt`:
   - Declare `DRAFTING` (wireTag = 11), `DRAFTED` (wireTag = 12), `RANKED` (wireTag = 13).
4. Update `EnumLogicTest.kt` to cover `DRAFTING`, `DRAFTED`, and `RANKED`.
5. Run `mise run records:build` and verify proto code generation.

### Phase 2: Backend Evaluation & Async Artifact Service
1. In `backend/service/.../ApplicationRoutes.kt`:
   - Replace hardcoded stub with invocation of `RankScorer.evaluate(postingText)`.
   - If a URL is provided, fetch HTML body (using sanitized network fetcher) and extract text.
2. Create `backend/ai-client`:
   - Define public SAM `ArtifactDraftingService`:
     ```kotlin
     public fun interface ArtifactDraftingService {
         public suspend operator fun invoke(request: DraftingRequest): Result<DraftingResponse>
     }
     ```
   - Internal implementation `impl/AiArtifactDraftingServiceImpl` using Google GenAI SDK (`gemini-2.5-flash` or `gemini-2.0-flash-lite`).
   - Ground prompts strictly against Candidate Profile (`AGENTS.md`) and verified LaTeX templates.
3. In `backend/service/.../ArtifactRoutes.kt`:
   - Implement `POST /artifacts/create` returning HTTP 202.
   - Launch background coroutine:
     - Generate tailored CV `.tex` and Cover Letter `.tex` via `ArtifactDraftingService`.
     - Compile `.tex` to `.pdf` via `DocumentCompiler`.
     - Upload artifacts to storage.
     - Update Firestore document `artifact_jobs/{jobId}` with status `SUCCESS` and artifact URLs, and transition target application from `DRAFTING` to `DRAFTED`.

### Phase 3: Frontend Core Data & Database Updates
1. Add `ApplySessionEntity`, `ApplyMessageEntity`, and `ApplyAssistantDao` in `frontend/core/data/src/commonMain/kotlin/.../database/`.
2. Update `AppDatabase.kt` to include the new entities and DAO method. Increment database version.
3. Expose repository abstractions:
   - `LocalApplyAssistantDataSource`
   - `NetworkApplyAssistantDataSource`
   - `RemoteApplyAssistantDataSource`
4. Register components in `coreDataModule`.

### Phase 4: Edits Experience Transformation (No FABs)
1. **Remove all FABs:**
   - Remove `FloatingActionButton` from `JobApplicationsListScreen.kt`.
   - Remove `FloatingActionButton` from `JobApplicationDetailsScreen.kt`.
   - In `JobApplicationsListScreen.kt`, add an action in `TopBarActions` or a header banner to add a manual application or lead.
2. **Transform `JobApplicationEditsRoute` into Assistant & Edits Hub:**
   - Retain the `Pencil` icon on the bottom navigation bar (`Icons.Filled.Edit` / `Icons.Outlined.Edit`).
   - Update `TrackerEditsScreen.kt` to feature a `PrimaryTabRow` / segmented switch:
     - **Tab 1: Apply Assistant** (The interactive `/apply` experience for URL or job description).
     - **Tab 2: Pending Edits** (The existing email proposal review list).
   - In the Apply Assistant tab:
     - Prominent dual input controls: "Job Posting URL" (text field with fetch button) and "Job Description" (expandable markdown text area).
     - Chat-style interactive log with `EvaluationResultCard`, `YesNoPromptCard`, and `ArtifactsResultCard`.
     - Seamlessly resumes active sessions from Room DB and reconnects to Firestore listeners for in-flight `DRAFTING` jobs.

---

## 5. Verification & Testing Checklist

- [ ] **Protobuf & Status Test:** `EnumLogicTest` passes verifying `DRAFTING` (11), `DRAFTED` (12), and `RANKED` (13) mapping and ordering.
- [ ] **Architecture Test:** `SingleAbstractMethodArchitectureTest` passes across all new use cases and services.
- [ ] **Interface Naming:** `mise run dev:guards` and Detekt pass with zero forbidden class name errors.
- [ ] **No FAB Audit:** Codebase search confirms zero `FloatingActionButton` occurrences in screens.
- [ ] **Edits Tab Experience:**
  - Launching app and navigating to the `Pencil` (Edits) tab shows the Apply Assistant and Pending Edits views.
  - Submitting a job posting URL or description triggers `RankScorer` evaluation $\rightarrow$ creates a `RANKED` record.
  - Tapping "Generate assets?" transitions record to `DRAFTING`, launches backend job, and updates to `DRAFTED` with artifact links upon completion.
  - Tapping "Did you submit?" = Yes transitions record to `SUBMITTED`.
- [ ] **Lifecycle & Resilience Verification:** Backgrounding or killing the app during `DRAFTING` re-opens cleanly with the state recovered from Room and Firestore listeners.
- [ ] **Full Monorepo Verification:** Run `mise run dev:test` to confirm zero regressions across all targets.
