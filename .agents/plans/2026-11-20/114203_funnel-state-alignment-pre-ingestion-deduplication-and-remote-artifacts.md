# Architecture & Implementation Plan: Funnel Lifecycle State Alignment, Pre-Ingestion Deduplication Gate, Remote Artifact Binding & UI Refresh Controls

**Status:** Completed  
**Date:** 2026-11-20  
**Target:** repository-wide  
**Harness:** Antigravity  
**Model:** Gemini 3.8 Flash  
**Session:** abbfb0e8-5323-4b16-ab25-0ca4d047341d  
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)  
**Supersedes:** [`113038_funnel-state-alignment-pre-ingestion-deduplication-and-remote-artifacts.md`](./113038_funnel-state-alignment-pre-ingestion-deduplication-and-remote-artifacts.md)  

---

## 1. Executive Summary & Goals

### Problem Statement & Real-World Audit Findings
Through real-world execution of `/scrape`, `/rank`, and `/apply` against postings such as `https://stripe.com/careers/listing/android-engineer-identity/8311069?gh_src=73vnei`:
1. **Premature Funnel Progression:** The apply workflows (`ApplyCommand.kt`, `ApplyPipelineCoordinator.kt`, `.agents/commands/apply.md`) previously set new application records directly to `SUBMITTED` (or legacy `APPLIED`), completely skipping `DRAFTING` and `DRAFTED`.
2. **Missing Deduplication & Ambiguity Blocking Gate:** Ingestion logic risks inserting stub records into Firestore before full deduplication and multi-channel aggregation completes. When a new posting is similar to an existing active or terminal application, the system does not pause or route to the user, creating duplicate records.
3. **Artifact Isolation on Local Filesystem:** Generated LaTeX CVs, cover letters, `job-posting.md`, and `outcome.md` lived only in local directories (`job-applications/<company>/<role>/<date>/`) without being uploaded to remote object storage (GCS) or linked as typed `TrackerArtifact` entities in the Firestore `applications/{id}` document.
4. **Visibility Gap in `@frontend/apps`:** Because artifacts are not registered in the remote database or remote storage bucket upon drafting completion, the multiplatform frontend applications (Desktop Compose UI, Web Dashboard, and TUI) cannot access or display drafts.
5. **Missing UI Refresh Action in Companies List:** The `CompaniesListScreen` in `@frontend/features/companies-list/ui` has a defined string resource (`companies_refresh_tooltip`) and an existing `CompaniesListUiIntent.Refresh` intent in MVI, but lacks a visible `Refresh` button in its toolbar row, unlike `JobApplicationsListScreen`.

### Goals & Target State
1. **Gate 0 Enforcement:** Mandatory pre-ingestion deduplication and ambiguity routing. If a newly discovered posting is similar to an existing application, the pipeline **blocks** and prompts the user for clarification before any database mutation.
2. **Canonical State Progression:**
   - `/scrape` $\rightarrow$ `TRACKER_STATUS_UNSPECIFIED` (or `SCRAPED` if formalized in schema).
   - `/rank` $\rightarrow$ `RANKED` with `fitRating` recorded via append-only event.
   - `/apply` invocation $\rightarrow$ `DRAFTING`.
   - All edits and compilation finished $\rightarrow$ `DRAFTED`.
   - Explicit user confirmation OR "Thank you for applying" email reconciliation $\rightarrow$ `SUBMITTED`.
   - Post-submission funnel driven **almost exclusively via email scans** (`email:collect` / proposal reconciler).
3. **Mandatory Remote Artifact Association:** Upon transitioning to `DRAFTED`, all associated artifacts (`OUTCOME`, `POSTING`, `CV`, `COVER_LETTER` in both PDF and TeX formats) must be uploaded to Remote Object Storage (GCS) and strictly attached as `TrackerArtifact` references in the Firestore application document.
4. **Immediate Multiplatform Availability & UI Polish:**
   - `@frontend/apps` displays the attached artifacts and remote storage URLs directly on the application detail view.
   - Add a `Refresh` button to `CompaniesListScreen` toolbar in `@frontend/features/companies-list/ui` using `Icons.Default.Refresh` and `TooltipIconButton`.

---

## 2. Gap Analysis & Architecture Trace

| Phase | Intended Architecture | Current Divergence in Codebase |
| :--- | :--- | :--- |
| **0. Pre-Ingestion** | Deduplicate against existing applications. If similar role exists, **halt and query user**. | Scrapers and ad-hoc scripts often write stubs directly or parse without interactive ambiguity checks. |
| **1. Scraping** | Record stub status is `TRACKER_STATUS_UNSPECIFIED`. | `ApplyPipelineCoordinator.kt` and `AddApplicationCommand.kt` default initial status to `"APPLIED"` or `"SUBMITTED"`. |
| **2. Ranking** | Record state is `RANKED`; score attached via `event:fit-recalculated`. | `/rank` scores in markdown tables (`scrape-ranked-unique-*.md`) but frequently does not persist a `RANKED` status document to Firestore unless explicitly seeded. |
| **3. Apply Kickoff** | Application status set to `DRAFTING`. | Neither `apply.md` nor `ApplyCommand.kt` transitions status to `DRAFTING` during asset generation. |
| **4. Drafting & Compilation** | Artifacts compiled locally, then immediately uploaded to GCS (`artifacts/<appId>/...`) and registered in Firestore `applications/{id}.artifacts`. | Artifacts remain on disk in `job-applications/` only; remote upload command (`ArtifactsAddCommand` / `ArtifactsSyncCommand`) was previously manual. |
| **5. Ready to Submit** | Record transitions to `DRAFTED`. Live in `@frontend/apps`. | Record was erroneously set to `SUBMITTED` prematurely. |
| **6. Submission** | Record transitions to `SUBMITTED` **only** upon user directive or confirmation email detection. | Previously marked `SUBMITTED` before candidate applied. |
| **7. Post-Submission** | Status transitions (`RECRUITER_SCREENED`, `INTERVIEWING`, `OFFER`, `REJECTED`) driven via `email:collect`. | Manual commands bypassed email proposal reconciliation engine. |
| **8. UI Controls** | `CompaniesListScreen` toolbar allows instant data refresh. | `CompaniesListScreen.kt` omitted `TooltipIconButton` with `Icons.Default.Refresh` next to the sort button. |

---

## 3. Proposed Changes & Implementation Steps

### Phase 0: Baseline Snapshot & State Persistence
- **0.1 Push Local State to Remote:** Verify and flush all live tracker states to Firestore.
- **0.2 Export Canonical Snapshot:** Capture `generated/snapshot-2026-11-20.ndjson` (146 documents) using `TRACKER_ALLOW_WRITES=1 ./gradlew -q :frontend:apps-cli:run --args="tracker export --ndjson"`.
- **0.3 Purge Legacy Dumps:** Remove older dump files (`generated/backup_v7_*.json`) to enforce the single active snapshot policy.
- **0.4 Document in Root Authority:** Author [`SNAPSHOTS.md`](file:///Users/fnc314/Work/Search/ai-job-tracker/SNAPSHOTS.md) outlining export formats, dry-run checks (`mise run admin:restore <file> --dry-run`), live restores, and pre-migration backup protocols.

### Phase 1: Deduplication Gate & Disambiguation Routing (Gate 0)
- **1.1 Ambiguity Detection:** Enhance deduplication logic (in `DocumentIdGenerator.kt`, `scripts/contract.py`, and `search:rank`/`scrape`) to compute similarity between discovered postings and existing `applications` (matching on `companyId`, sanitized title tokens, and job URLs).
- **1.2 Interactive Clarification Prompt:** If title/company similarity exceeds 75% or matches an active/terminal record:
  - Halt automated ingestion.
  - Output structured diff showing the discovered posting vs. existing record(s).
  - Await explicit user instruction: `[M]erge as re-post / update URL`, `[N]ew distinct opening`, or `[S]kip duplicate`.

### Phase 2: Funnel State Progression & Lifecycle Fixes
- **2.1 Schema & Enums Alignment:**
  - Verify `TrackerStatus.TRACKER_STATUS_UNSPECIFIED`, `RANKED`, `DRAFTING`, `DRAFTED`, `SUBMITTED` in `records/proto/src/main/proto/tracker/enums.proto` and `records/parser/src/main/kotlin/.../JobApplicationStatus.kt`.
  - Update `StatusNormalizer.kt` and `tracker.schema.json` to reject premature jumps directly to `SUBMITTED` during apply initialization.
- **2.2 Apply Pipeline Execution (`ApplyPipelineCoordinator.kt` & `ApplyCommand.kt`):**
  - On `/apply` start: update application status to `DRAFTING`.
  - On artifact completion & verification: update status to `DRAFTED`.
  - Disallow `--submitted` flag during draft runs; enforce that `SUBMITTED` is an explicit downstream action.
- **2.3 Workflow Command Specification (`.agents/commands/apply.md` & `scrape.md`):**
  - Update workflow markdown instructions to mandate the `DRAFTING` $\rightarrow$ `DRAFTED` sequence.

### Phase 3: Remote Object Storage (GCS) Upload & Record Association
- **3.1 Remote Artifact Ingestion:**
  - Standardize artifact kinds in `records/proto/src/main/proto/tracker/enums.proto`: `CV`, `COVER_LETTER`, `OUTCOME`, `POSTING`.
  - Wire GCS upload directly into `ApplyPipelineCoordinator.kt` / `DefaultArtifactDraftingService.kt`:
    - Upload `job-posting.md` $\rightarrow$ `gs://$BUCKET/artifacts/<id>/job-posting.md` (`POSTING`).
    - Upload `outcome.md` $\rightarrow$ `gs://$BUCKET/artifacts/<id>/outcome.md` (`OUTCOME`).
    - Upload `FrancoNColaizzi_Resume.pdf` and `.tex` $\rightarrow$ `gs://$BUCKET/artifacts/<id>/...` (`CV`).
    - Upload `cover_*.pdf` and `.tex` $\rightarrow$ `gs://$BUCKET/artifacts/<id>/...` (`COVER_LETTER`).
- **3.2 Record Linkage:**
  - Atomically update the Firestore `applications/{id}` document with the populated `artifacts` array containing `artifactId`, `kind`, `filename`, `remoteUrl`, `sha256`, and `sizeBytes`.

### Phase 4: `@frontend/features/companies-list/ui` Refresh Button & Visibility
- **4.1 Companies List Screen Toolbar Update (`CompaniesListScreen.kt`):**
  - In `CompaniesListContent` toolbar row 1, add a `Row` container next to `CompanyFilterMenu` holding:
    - Sort Menu dropdown (`Icons.AutoMirrored.Filled.List`)
    - **Refresh Button (`Icons.Default.Refresh`)**:
      ```kotlin
      TooltipIconButton(
          tooltip = stringResource(Res.string.companies_refresh_tooltip),
          icon = Icons.Default.Refresh,
          onClick = { onIntent(CompaniesListUiIntent.Refresh) },
      )
      ```
- **4.2 Resource & Import Verification:**
  - Import `androidx.compose.material.icons.filled.Refresh`.
  - Use existing resource `Res.string.companies_refresh_tooltip`.

### Phase 5: Submission & Email Reconciliation Loop
- **5.1 Submission State Trigger:**
  - Transition from `DRAFTED` to `SUBMITTED` occurs *only* when:
    - User executes explicit status change (e.g. `mise run tracker status <id> SUBMITTED`), or
    - `email:collect` discovers an application acknowledgment / submission confirmation email.
- **5.2 Downstream Funnel Control:**
  - Subsequent transitions (`RECRUITER_SCREENED`, `INTERVIEWING`, `OFFER`, `REJECTED`) are driven via email scan proposals.
  - Manual status modification is reserved for candidate-initiated withdrawal (`APPLICATION_WITHDRAWN`).

---

## 4. Verification Plan & Testing Checklist

- [ ] **Unit & Architectural Tests:**
  - Run `mise run dev:konsist` to verify interface naming and Single Abstract Method invariants.
  - Run `./gradlew :frontend:features:companies-list:ui:test` to verify UI state and intent handling.
  - Run `./gradlew :records:parser:test` to verify status hierarchy transitions in `JobApplicationStatusTest` and `StatusNormalizerTest`.
  - Run `./gradlew :backend:service:test` to verify `ApplyAndArtifactRoutesTest`.
- [ ] **Frontend UI Verification:**
  - Run `mise run frontend:desktop:run` or `./gradlew :frontend:apps:desktop:run`.
  - Open the `Companies` tab.
  - Verify the `Refresh` icon button appears beside the sort icon in the toolbar.
  - Click `Refresh` and confirm company list triggers `CompaniesListUiIntent.Refresh` without UI flicker or regressions.
- [ ] **Deduplication Verification:**
  - Run dry-run deduplication check on mock duplicates to ensure ambiguous listings halt and prompt user.
- [ ] **Artifact Association Verification:**
  - Execute `/apply` in test mode; verify files upload to test GCS bucket and Firestore document contains valid `artifacts` references.
  - Verify `tracker get <id>` lists all 4+ attached remote artifacts.

---

## 5. Bookkeeping

- [x] Plan persisted at `.agents/plans/2026-11-20/114203_funnel-state-alignment-pre-ingestion-deduplication-and-remote-artifacts.md`
- [x] Registry `README.md` row added / status synchronized
- [ ] Stage changed files and craft commit
