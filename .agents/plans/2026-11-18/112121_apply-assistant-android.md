# Implementation Plan: Apply Assistant Android (Dual-Rank, CLI-Aligned, Edits Destination Replacement & Zero FAB)

**Status:** Completed
**Date:** 2026-11-18
**Target:** `frontend`, `backend`, and `records` (cross-subsystem)
**Harness:** Antigravity
**Model:** gemini-2.5-pro
**Evaluates:** [`apply-assistant-android.md`](../../.junie/plans/apply-assistant-android.md), [`session-261118-102429-10p9`](/Users/fnc314/.junie/sessions/session-261118-102429-10p9/transcript.md)
**Schema Authority:** [`tracker.schema.json`](../tracker.schema.json)

---

## 1. Executive Summary & Goals

Unify the job evaluation and application drafting pipeline so that **CLI invocations, `mise` tasks, and Android UI all drive the exact same underlying services**. Regardless of whether `/apply` is triggered via the agent CLI, `mise run search:apply <target>`, or the Android app, changes are persisted as real-time append-only events in Firebase/Firestore where statuses seamlessly transition across each step.

### Dual-Expose Ranking Architecture (Instant Deterministic + Animated AI Evaluation)
Expose the backend AI client module as `backend:ai-client` (`dev.fnc314.tools.jobtracker.backend:ai-client:1.0.0` or `backend/ai/client/`), supporting a two-tier evaluation model:
1. **Tier 1 (Instant & Free — Deterministic):** `RankScorer` evaluates instantly (<1s, $0 cost) via `POST /applications/apply` (or `POST /jobs/rank`). The UI renders the deterministic baseline score immediately.
2. **Tier 2 (AI Semantic Evaluation — Optional / Asynchronous):** `POST /ai/rank` invokes the AI model to perform deep semantic evaluation, nuance detection, and calibrated compensation benchmarking.
3. **Animated UI Transition:** In the Android UI, the evaluation card loads instantaneously with the Tier 1 deterministic score. When the Tier 2 `ai/rank` result resolves asynchronously, the score counter and breakdown fluidly animate to the refined AI score.

Simultaneously, transform the in-app `Edits` destination (`Pencil` icon) directly into the **Apply Assistant** (disabling the existing proposal edits flow for now; single dedicated view, no tabs), and **eliminate all floating action buttons (`FAB`s)** across the entire app while keeping the top app bar strictly clean (only title, `Refresh`, and `Settings`).

### Lifecycle & Interaction Flow
1. **Unified Pipeline via CLI / Service Invocations:**
   - The same pipeline driving `/apply` drives Android and `mise`:
     - CLI command: `mise run search:apply <target>` or `apps-cli apply <target>`
     - Backend routes: `POST /applications/apply` (deterministic), `POST /ai/rank` (AI-refined), and `POST /artifacts/create` (async drafting)
   - Real-time Firebase visibility: As steps execute (evaluation $\rightarrow$ asset drafting $\rightarrow$ compilation $\rightarrow$ submission), status updates write directly to Firestore so anyone inspecting Firebase sees the application progress live.
2. **Zero FABs & Clean Top Bar:**
   - Remove floating action buttons completely from `JobApplicationsListScreen` and `JobApplicationDetailsScreen`. Do not introduce any FAB in `AppScaffold`.
   - Top bar remains strictly clean: standard navigation/title, plus only `Refresh` and `Settings`. No manual creation actions in the top bar.
3. **Edits Destination Hosts Apply Assistant via Separate Feature (`frontend/features/ai-apply/{domain,data,ui}` / `AiApplyRoute`):**
   - Retain the top-level destination in bottom navigation / navigation rail using `Icons.Filled.Edit` (active) / `Icons.Outlined.Edit` (unselected).
   - Renders the full-screen **Apply Assistant** experience (no tabs, proposal edits temporarily disabled).
   - In-app interface provides:
     - Prominent dual input controls: "Job Posting URL" (text field with fetch button) and "Job Description" (expandable markdown text area).
     - Interactive chat-style evaluation stream with animated score transition from deterministic to AI score.
     - Confirmation prompts and PDF artifact download/view links.
4. **Status Funnel Progression & Tag Ordering:**
   - **`DRAFTING`** (wireTag = 11): Async artifact drafting is actively underway (LaTeX generation and PDF compilation in flight).
   - **`DRAFTED`** (wireTag = 12): Artifact generation has completed successfully; draft application prepared and pending submission.
   - **`RANKED`** (wireTag = 13): Job posting evaluated, outcome and fit score recorded, awaiting user decision to generate assets.
   - **Enum Sizing & Wire Representation:**
     - Drop `wireKey` in favor of standard enum `name`. `name` is the single canonical string across Protobuf, Firestore, Room, and CLI.
     - User-facing display labels are localized via Compose resources (`strings.xml`), completely decoupled from enum code names.
   - **Funnel Progression:**
     1. Evaluation $\rightarrow$ record tracked in Firestore as **`RANKED`**.
     2. "Generate assets?" = Yes $\rightarrow$ triggers async backend job, transitions record in Firestore to **`DRAFTING`**.
     3. Backend completes LaTeX + PDF compilation $\rightarrow$ transitions record in Firestore to **`DRAFTED`** (artifacts attached).
     4. "Did you submit the application?" = Yes $\rightarrow$ transitions record in Firestore to **`SUBMITTED`** (canonical wireTag = 2, `name = "SUBMITTED"`). If No $\rightarrow$ remains **`DRAFTED`**.

---

## 2. Architectural Invariants & Codebase Compliance

### Invariant 1: Single Abstract Method (SAM) & Functional Interfaces
Per repository rule and `SingleAbstractMethodArchitectureTest.kt`:
- Every domain use case with a single abstract method MUST be declared as `fun interface`:
  ```kotlin
  public fun interface EvaluateJobUseCase {
      public suspend operator fun invoke(input: EvaluateJobInput): Result<RankResult>
  }

  public fun interface EvaluateJobWithAiUseCase {
      public suspend operator fun invoke(input: EvaluateJobInput): Result<RankResult>
  }
  ```
- All callers must invoke instances directly via `invoke` operator syntax (`evaluateJobUseCase(input)`).

### Invariant 2: Interface-First Naming & `ai/client` Module
Per repository rule and `../../../docs/interface-naming-baseline.md`:
- All public interfaces must use abstraction vocabulary:
  - `NetworkApplyAssistantDataSource` (raw HTTP to backend Ktor)
  - `RemoteApplyAssistantDataSource` (Firestore listener and mutations)
  - `LocalApplyAssistantDataSource` (Room 3 database)
- Backend module for AI drafting and ranking MUST be exposed under `backend/ai/client/` (Gradle project `:ai:client` or `:ai-client`), exposing clean public facades:
  - `ArtifactDraftingService` (SAM interface for LaTeX drafting)
  - `AiRankingService` (SAM interface for AI-refined evaluation)
- Internal implementations reside in `impl/` packages hiding vendor SDK details.

### Invariant 3: Multiplatform Room 3 Database Schema & Version Bump
- Register `ApplySessionEntity` and `ApplyMessageEntity` in `AppDatabase`.
- **Room Database Version Bump:** Increment database version from `1` to `2` in `AppDatabase.kt`.
- Expose `ApplyAssistantDao` from `AppDatabase`.
- `features/apply-assistant/data` leverages `AppDatabase` provided via Koin.

### Invariant 4: Zero FABs & Uncluttered Top Bar
- Remove `FloatingActionButton` from `JobApplicationsListScreen.kt` and `JobApplicationDetailsScreen.kt`.
- Do NOT introduce any FAB in `AppScaffold`.
- Top app bar only contains `Refresh` and `Settings` (no new action buttons).
- Remove `MaterialTheme.spacing.fabBottomPadding` workarounds.

---

## 3. Data Models & Wire Contracts

### 3.1 Protobuf & Tracker Schema (Un-versioned package `tracker`)
- Remove `/v1/` path segment and package versioning: Protos live under `tracker/enums.proto` with package `tracker`:
  ```protobuf
  syntax = "proto3";

  package tracker;

  option java_package = "dev.fnc314.aijobtracker.records";
  option java_multiple_files = true;

  enum TrackerStatus {
    TRACKER_STATUS_UNSPECIFIED = 0;

    // Terminal inactive states (ranks 0-3)
    NO_RESPONSE = 1;
    OFFER_DECLINED = 2;
    APPLICATION_WITHDRAWN = 3;
    REJECTED = 4;

    // Active funnel states
    DRAFTING = 11;               // Async artifact generation / active drafting in progress
    DRAFTED = 12;                // Application draft and artifacts prepared; pending submission
    RANKED = 13;                 // Posting evaluated, fit score recorded, not yet drafted
    SUBMITTED = 2;               // Application formally submitted to company/portal
    RECRUITER_SCREENED = 7;
    INTERVIEWING = 8;
    NEGOTIATING_PRE_OFFER = 9;
    OFFER = 10;
    NEGOTIATING_POST_OFFER = 11;

    // Terminal success
    HIRED = 5;
  }
  ```
- **Records Version Bump:** Bump `records` version from `1.0.0` to `1.1.0` in `gradle/libs.versions.toml` and `records/build.gradle.kts`.

### 3.2 Frontend `TrackerStatus` (Eliminate `wireKey`, Use `name`)
```kotlin
public enum class Known(
    public val wireTag: Int = -1,
) : TrackerStatus {
    NONE,
    UNSPECIFIED(wireTag = 0),
    LEAD(wireTag = 10),
    DRAFTING(wireTag = 11),
    DRAFTED(wireTag = 12),
    RANKED(wireTag = 13),
    SUBMITTED(wireTag = 2),
    INTERVIEW(wireTag = 3),
    OFFER_EXPECTED(wireTag = 4),
    HIRED(wireTag = 5),
    REJECTED(wireTag = 6),
    NO_RESPONSE(wireTag = 7),
    OFFER_DECLINED(wireTag = 8),
    APPLICATION_WITHDRAWN(wireTag = 9);

    override fun toRemote(): String = name
}
```
- User-facing display labels are localized via Compose resources in `strings.xml`.

### 3.3 Backend HTTP & CLI API Contracts

#### `POST /applications/apply` (Tier 1: Deterministic `RankScorer`)
Instant response (<1s, $0 cost).
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
  "isAiEvaluated": false,
  "strengths": ["Kotlin", "Jetpack Compose", "Android SDK"],
  "gaps": ["GraphQL"],
  "dealBreakers": [],
  "salaryEstimate": "$170k - $190k"
}
```

#### `POST /ai/rank` (Tier 2: AI-Refined Semantic Evaluation)
Asynchronous or secondary response with deeper reasoning.
```json
// Request Body:
{
  "postingText": "Raw job description text...",
  "deterministicResult": { ... }
}

// Response (200 OK):
{
  "score": 92,
  "verdict": "STRONG_MATCH",
  "isAiEvaluated": true,
  "aiRationale": "Extensive mobile architecture and GenAI tooling experience strongly aligns with Walmart+ platform initiatives.",
  "strengths": ["Kotlin", "Jetpack Compose", "Android SDK", "GenAI Tooling"],
  "gaps": ["GraphQL"],
  "dealBreakers": [],
  "salaryEstimate": "$180k - $210k"
}
```

#### `POST /artifacts/create` (Async Job)
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

---

## 4. Implementation Steps

### Phase 1: Records & Proto Harmonization (Drop v1 & Bump Version)
1. Drop `v1` pathing/package in `records/proto/src/main/proto/tracker/`. Update package to `tracker`.
2. Add `DRAFTING = 11`, `DRAFTED = 12`, `RANKED = 13` to proto `TrackerStatus`.
3. Update `tracker.schema.json` to include `"DRAFTING"`, `"DRAFTED"`, `"RANKED"`.
4. Bump `records` version to `1.1.0` in `gradle/libs.versions.toml` and `records/build.gradle.kts`.
5. Update `frontend/core/data/.../TrackerStatus.kt` to drop redundant `wireKey` in favor of enum `name`.
6. Update `EnumLogicTest.kt` to cover `DRAFTING`, `DRAFTED`, and `RANKED`.
7. Run `mise run records:build` and verify proto code generation.

### Phase 2: Dual Ranking & Backend `ai/client` Module
1. In `backend/`:
   - Create module `backend/ai/client` (registered in `backend/settings.gradle.kts` as `:ai:client` or `:ai-client`).
   - Define public SAM `ArtifactDraftingService`:
     ```kotlin
     public fun interface ArtifactDraftingService {
         public suspend operator fun invoke(request: DraftingRequest): Result<DraftingResponse>
     }
     ```
   - Define public SAM `AiRankingService`:
     ```kotlin
     public fun interface AiRankingService {
         public suspend operator fun invoke(request: AiRankingRequest): Result<RankResult>
     }
     ```
   - Internal implementations use Google GenAI SDK (`gemini-2.5-flash` or `gemini-2.0-flash-lite`).
2. In `backend/service/.../ApplicationRoutes.kt`:
   - Route `POST /applications/apply` executes instant deterministic `RankScorer.evaluate()`.
   - Route `POST /ai/rank` executes `AiRankingService`.
3. In `frontend/apps/cli`:
   - Add `ApplyCommand` to `frontend:apps-cli` so running `mise run search:apply <target>` or CLI `apply` invokes the exact same evaluation and artifact generation pipeline.
4. In `backend/service/.../ArtifactRoutes.kt`:
   - Implement `POST /artifacts/create` returning HTTP 202.
   - Background job generates LaTeX CV/Cover Letter, compiles to PDF via `DocumentCompiler`, updates Firestore document `artifact_jobs/{jobId}` with status `SUCCESS` and artifact URLs, and updates application document status from `DRAFTING` to `DRAFTED`.

### Phase 3: Frontend Core Data & Database Updates (Version Bump)
1. Add `ApplySessionEntity`, `ApplyMessageEntity`, and `ApplyAssistantDao` in `frontend/core/data/src/commonMain/kotlin/.../database/`.
2. Update `AppDatabase.kt` to include the new entities and DAO method. **Bump database version to 2**.
3. Expose repository abstractions:
   - `LocalApplyAssistantDataSource`
   - `NetworkApplyAssistantDataSource`
   - `RemoteApplyAssistantDataSource`
4. Register components in `coreDataModule`.

### Phase 4: Edits Destination Replacement, Zero FAB & Animated Dual-Score UI
1. **Remove all FABs:**
   - Remove `FloatingActionButton` from `JobApplicationsListScreen.kt`.
   - Remove `FloatingActionButton` from `JobApplicationDetailsScreen.kt`.
   - Remove `MaterialTheme.spacing.fabBottomPadding` list padding workarounds.
   - Verify top app bar strictly retains `Refresh` and `Settings` (no extra action buttons).
2. **Create `frontend/features/ai-apply/{domain,data,ui}` & Wire `TopLevelDestinationEnum.Edits` to `AiApplyRoute`:**
   - Retain the `Pencil` icon on the bottom navigation bar and navigation rail (`Icons.Filled.Edit` / `Icons.Outlined.Edit`).
   - Restore `frontend/features/tracker-edits` to its original state and swap `apps/core` (`build.gradle.kts`, `AppDI.kt`, `App.kt`) to point `TopLevelDestinationEnum.Edits` to `AiApplyRoute` / `AiApplyEntryRegistry` (`AiApplyScreen`):
     - Single dedicated view (no tabs, proposal edits disabled).
     - Prominent dual input controls: "Job Posting URL" (text field with fetch button) and "Job Description" (expandable markdown text area).
     - `EvaluationResultCard` with animated score counter (`animateIntAsState`):
       - Immediately presents the deterministic Tier 1 score.
       - Dispatches background `POST /ai/rank`. When response returns, smoothly animates the score badge and updates the rationale/details.
     - Confirmation prompts and artifact link cards.
     - Resumes active sessions from Room DB and reconnects to Firestore listeners for in-flight `DRAFTING` jobs.

---

## 5. Verification & Testing Checklist

- [x] **Protobuf & Status Test:** `EnumLogicTest` passes verifying `DRAFTING` (11), `DRAFTED` (12), and `RANKED` (13) mapping and ordering.
- [x] **Architecture Test:** `SingleAbstractMethodArchitectureTest` passes across all new use cases and services (`EvaluateJobUseCase`, `EvaluateJobWithAiUseCase`, `ArtifactDraftingService`, `AiRankingService`).
- [x] **Interface Naming:** `mise run dev:guards` and Detekt pass with zero forbidden class name errors.
- [x] **Zero FAB & Clean Top Bar Audit:**
  - Codebase search confirms 0 `FloatingActionButton` occurrences in screens.
  - Top app bar only shows title, back arrow (when applicable), `Refresh`, and `Settings`.
- [x] **Dual-Rank Evaluation & Animation Verification:**
  - `POST /applications/apply` instantly returns deterministic score.
  - `POST /ai/rank` returns AI-calibrated score and rationale.
  - UI renders instant baseline, then animates cleanly to the AI score upon arrival.
- [x] **CLI & Firebase Invariant Verification:**
  - Executing `mise run search:apply <target>` or CLI `apps-cli apply <target>` executes the same flow.
  - Inspecting Firestore console shows live status progression: `RANKED` $\rightarrow$ `DRAFTING` $\rightarrow$ `DRAFTED` $\rightarrow$ `SUBMITTED`.
- [x] **Edits Tab Experience:**
  - Navigating to the `Pencil` (Edits) destination directly renders Apply Assistant.
  - Submitting a job posting URL or description triggers evaluation $\rightarrow$ creates a `RANKED` record.
  - Tapping "Generate assets?" transitions record to `DRAFTING`, launches backend job, and updates to `DRAFTED` with artifact links upon completion.
  - Tapping "Did you submit?" = Yes transitions record to `SUBMITTED`.
- [x] **Lifecycle & Resilience Verification:** Backgrounding or killing the app during `DRAFTING` re-opens cleanly with the state recovered from Room and Firestore listeners.
- [x] **Full Monorepo Verification:** Run `mise run dev:test` to confirm zero regressions across all targets.
