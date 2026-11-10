# Implementation Plan: Companies Navigation & Company Details Adaptive Feature

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** repository-wide (`frontend/features/companies-list/*`, `frontend/features/company-details/*`, `frontend/core/widgets`, `frontend/core/navigation`, `frontend/apps/core`, `records/proto`, `records/parser`, `backend/`, `tracker.schema.json`)  
**Harness:** AGY  
**Model:** Gemini 3.8 Flash (High)  
**Session:** 67494617-dcfc-49c3-92fd-28e963ce3a2c  
**Depends On:** `features/job-applications-list`, `features/job-application-details`, `core/navigation`, `core/widgets`, `tracker.schema.json`, `125800_kotlin_cloud_run_remote_tasks_and_tui_plan.md`  

---

Introduce a new primary top-level navigation destination called **Companies** positioned in the navigation rail and bottom navigation bar immediately **after Applications and before Pending Edits**.

The feature is paired with a dedicated **Company Details** screen. Together, `features/companies-list` and `features/company-details` leverage the canonical Material 3 **List-Detail Layout** on wide viewports, while adapting cleanly to single-pane navigation on compact mobile screens. Tapping a `JobApplicationCard` from within `features/company-details` launches `features/job-application-details`.

To ensure maximum performance, zero client-side battery drain, and cross-subsystem consistency, this plan integrates the Screaming Monorepo architecture ([`125800_kotlin_cloud_run_remote_tasks_and_tui_plan.md`](./../2026-11-09/125800_kotlin_cloud_run_remote_tasks_and_tui_plan.md)) with a **Local-First Verification Workflow**:
- **`@records`**: Defines `CompanyMetrics` proto3 contract and parser mappers.
- **`@backend` (Local Execution & Testing)**: Implements the Kotlin calculation engine in `BackendService.kt`. The entire data flow and all calculated insights are executed and verified locally via dedicated tasks and automated unit tests before any cloud deployment.
- **`@backend` (Initial Cloud Run Deploy)**: Provides complete, automated step-by-step deployment instructions for Google Cloud Run ($0 always-free tier) and Cloud Scheduler routine sweep.
- **`@frontend`**: Consumes pre-aggregated metrics in $O(1)$ time across Android, Desktop, and Web.

---

## User Review Required

> [!IMPORTANT]
> ### 1. Local Solution to Google Cloud Run Logic & Data Flow Verification
> Before pushing any services to live Google Cloud Run, the entire data flow and calculated insights will be verified locally:
> - **Local CLI Recalculation Task (`mise run tracker:companies:recalculate`)**:
>   - Directly runs `BackendService.recalculateCompanyMetrics()` locally using local credentials (`TRACKER_SA_KEY` or `~/.config/job-tracker/sa.json` or ADC).
>   - Outputs a formatted terminal table showing all aggregated metrics (counts, percentages, average FIT score, salary range, primary channel, locations).
> - **Local Ktor Netty Server (`mise run remote:local:run` or `./gradlew -p backend run`)**:
>   - Launches the backend server locally on `http://127.0.0.1:8080`.
>   - Supports local testing of `/health`, `/tasks/companies-recalculate-metrics`, and `/tasks/sweep` via `curl`.
> - **Local Automated Test Suite (`:backend:test`)**:
>   - Comprehensive unit tests in `BackendServiceTest.kt` verifying counts, float percentages (100% total invariant), average fit score calculation, salary range string formatting, and primary channel mode detection.

> [!IMPORTANT]
> ### 2. Step-by-Step Initial Cloud Run & Scheduler Deployment
> Handled directly within this plan once local data flow and insights are verified:
> 1. **Container Build & Deploy**:
>    - Local Docker: `mise run remote:deploy:service --local`
>    - Cloud Build (fallback if local Docker is absent): `mise run remote:deploy:service --cloud`
> 2. **Scale-to-Zero Free Tier Guarantee**:
>    - `--min-instances=0 --max-instances=1 --memory=512Mi --cpu=1` ensures $0 monthly cost when idle.
> 3. **Cloud Scheduler Job**:
>    - `mise run remote:deploy:scheduler` provisions 4x/day routine sweep (`0 */6 * * *`) targeting `/tasks/sweep`.
> 4. **Live Verification & Smoke Test**:
>    - Health: `curl -f https://<SERVICE_URL>/health`
>    - Remote recalculation: `curl -f -X POST https://<SERVICE_URL>/tasks/companies-recalculate-metrics`
>    - Scheduled sweep trigger: `mise run remote:sweep`
>    - Rollback/Teardown: `mise run remote:destroy`

> [!IMPORTANT]
> ### 3. Impact on `@records` (Protobuf v3 & Wire Protocol)
> - **`records/proto/src/main/proto/tracker/v1/company.proto`**:
>   - Define `message CompanyMetrics` containing `counts`, `percentages`, `average_fit_score`, `salary_range`, `primary_channel`, `locations`, and `last_calculated_at`.
>   - Add field `CompanyMetrics metrics = 15;` to `message Company`.
> - **`records/parser/`**:
>   - Add mapper functions for `CompanyMetrics` between proto3 messages and Firestore document maps.
> - **`tracker.schema.json`**:
>   - Add schema contract definition for `company.metrics`.

> [!IMPORTANT]
> ### 4. Canonical Material 3 List-Detail Layout Flow
> - **Wide Viewports (Tablets / Desktop)**:
>   - `features/companies-list` and `features/company-details` operate together as a canonical **List-Detail Layout** (`ListDetailSceneStrategy` / two-pane split).
>   - Left pane: Companies array of `CompanyCard`s.
>   - Right pane: `CompanyDetailsScreen` (overview metrics card and job application cards).
> - **Compact Viewports (Phones)**:
>   - Tapping a `CompanyCard` pushes `CompanyDetailsScreen` with back navigation.
> - **Drill-down to Job Application Details**:
>   - Tapping any `JobApplicationCard` within `CompanyDetailsScreen` navigates into `JobApplicationDetailsScreen` (`features/job-application-details`).

> [!IMPORTANT]
> ### 5. Widget Naming: `<WidgetName>Card` and `<WidgetName>CardUiModel`
> Presentation components in `:core-widgets` and features use `<WidgetName>Card` conventions:
> - `JobApplicationCard` bound to `JobApplicationCardUiModel` (in `:core-widgets`)
> - `CompanyCard` bound to `CompanyCardUiModel` (in `:features-companies-list-ui`)
> - `CompanyDetailsScreen` bound to `CompanyDetailsUiModel` (in `:features-company-details-ui`)

> [!IMPORTANT]
> ### 6. Dynamic Centered Top Bar Titles & Navigation Ordering
> - **Navigation Order**: `1. Applications` -> `2. Companies` -> `3. Pending Edits` -> `4. Settings`.
> - **Centered Title in `AppScaffold.kt`**:
>   - `CompanyDetailsRoute`: Dynamically displays **Company Name** (e.g., `"PNC Financial Services"`).
>   - `DetailRoute`: Dynamically displays **Role Title** (e.g., `"Principal Android Software Engineer"`).
> - **Top-Right Action**:
>   - Globe icon / tooltip button (`Icons.Default.Public` from Compose Material 3) on `CompanyDetailsRoute` launching company website via `PlatformFileOpener().openUrl(website)`.

---

## Monorepo Architecture Flow

```mermaid
flowchart TD
    subgraph RecordsDomain ["records/ Subsystem (Proto3 & Contract Authority)"]
        Proto["records/proto (tracker/v1/company.proto: CompanyMetrics)"]
        Parser["records/parser (CompanyMetricsParser & JSON/Map Serializers)"]
        Schema["tracker.schema.json (Single Source of Truth)"]
    end

    subgraph LocalBackend ["Local Execution & Verification (Local First)"]
        LocalCLI["mise run tracker:companies:recalculate"]
        LocalServer["mise run remote:local:run (http://localhost:8080)"]
        UnitTests[":backend:test (BackendServiceTest.kt)"]
        LocalEngine["BackendService.recalculateCompanyMetrics()"]
        LocalCLI --> LocalEngine
        LocalServer --> LocalEngine
    end

    subgraph CloudRunBackend ["backend/ Subsystem (Google Cloud Run - $0 Free Tier)"]
        DeployService["mise run remote:deploy:service [--local|--cloud]"]
        DeployScheduler["mise run remote:deploy:scheduler"]
        Ktor["Main.kt (POST /tasks/companies-recalculate-metrics)"]
        CloudEngine["BackendService.recalculateCompanyMetrics()"]
        Sweep["POST /tasks/sweep (Cloud Scheduler 4x/day Routine Sweep)"]
        Sweep --> CloudEngine
        Ktor --> CloudEngine
    end

    subgraph Database ["Google Cloud Firestore"]
        AppsColl["applications/{id}"]
        CompColl["companies/{companyId}.metrics"]
        LocalEngine -->|Local Run: Reads| AppsColl
        LocalEngine -->|Local Run: Writes Atomically| CompColl
        CloudEngine -->|Remote Run: Reads| AppsColl
        CloudEngine -->|Remote Run: Writes Atomically| CompColl
    end

    subgraph FrontendDomain ["frontend/ Subsystem (All Clients)"]
        CL["features/companies-list (CompaniesListScreen - List Pane)"]
        CD["features/company-details (CompanyDetailsScreen - Detail Pane)"]
        Widgets["core/widgets (JobApplicationCard, CompanyCard, CompanyMetricsTextRow)"]
        CompColl -->|Reads O(1)| CL
        CompColl -->|Reads O(1)| CD
        CL -->|ListDetailSceneStrategy| CD
        CD -->|Select JobApplicationCard| JAD["features/job-application-details"]
    end

    Proto --> Parser
    Parser --> LocalBackend
    Parser --> CloudRunBackend
    Parser --> FrontendDomain
    Schema --> Proto
    LocalBackend -->|Verified Flawlessly| DeployService
    DeployService --> CloudRunBackend
    DeployScheduler --> CloudRunBackend
```

---

## Phased Implementation Sequence

### Phase 1: `@records` Subsystem (Contract & Parser)
1. **`records/proto/src/main/proto/tracker/v1/company.proto`**:
   - Define `CompanyMetrics` and add `CompanyMetrics metrics = 15;` to `Company`:
     ```protobuf
     message CompanyMetrics {
       message Counts {
         int32 draft = 1;
         int32 submitted = 2;
         int32 interviewing = 3;
         int32 offer = 4;
         int32 rejected = 5;
         int32 total = 6;
       }
       message Percentages {
         float draft = 1;
         float submitted = 2;
         float interviewing = 3;
         float offer = 4;
         float rejected = 5;
       }
       message SalaryRange {
         optional double min = 1;
         optional double max = 2;
         string currency = 3;
         string display = 4;
       }
       Counts counts = 1;
       Percentages percentages = 2;
       optional int32 average_fit_score = 3;
       SalaryRange salary_range = 4;
       ApplicationChannel primary_channel = 5;
       repeated string locations = 6;
       google.protobuf.Timestamp last_calculated_at = 7;
     }
     ```
2. **`records/parser/`**:
   - Implement `CompanyMetricsParser.kt` to map between proto3 messages, JSON, and Firestore maps.
3. **`tracker.schema.json`**:
   - Update JSON schema contract with `metrics` map under `company`.
4. **Verification**:
   - Run `./gradlew :records:test` to confirm compilation and serialization.

---

### Phase 2: `@backend` Local Solution & Test Suite
1. **`backend/src/main/kotlin/dev/fnc314/tools/jobtracker/backend/BackendService.kt`**:
   - Implement `recalculateCompanyMetrics(targetCompanyId: String? = null): Map<String, Any>`:
     - Queries `applications` where `companyId == targetCompanyId` (or all if null).
     - Aggregates status counts: `draft`, `submitted`, `interviewing`, `offer`, `rejected`, `total`.
     - Calculates float percentages ($count / total$).
     - Computes average FIT score (filtering valid non-negative integer scores).
     - Calculates salary range (min, max, formatted display string e.g. `"$150k - $185k"`).
     - Identifies primary sourcing channel (mode of `channel` or defaults to `DIRECT`).
     - Collects and deduplicates application locations.
     - Atomically writes updated map to `companies/{companyId}.metrics` and sets `lastCalculatedAt`.
   - Update `performRoutineSweep()` to invoke `recalculateCompanyMetrics()` as part of the scheduled sweep.
2. **`backend/src/main/kotlin/dev/fnc314/tools/jobtracker/backend/Main.kt`**:
   - Register route: `post("/tasks/companies-recalculate-metrics")`.
3. **Local Mise Runner Tasks**:
   - Create `.config/mise/tasks/remote/local/run.sh`:
     - Starts Ktor server locally via `${MISE_PROJECT_ROOT}/gradlew -p backend run`.
   - Create `.config/mise/tasks/tracker/companies/recalculate.sh`:
     - CLI task allowing local execution: `mise run tracker:companies:recalculate [--company-id <id>]`.
4. **Automated Backend Unit Tests**:
   - Create `backend/src/test/kotlin/dev/fnc314/tools/jobtracker/backend/BackendServiceTest.kt`:
     - Test metric calculations with mock application fixtures.
     - Verify percentage invariants ($100\%$ sum), null/empty salary handling, and channel resolution.
   - Run `./gradlew -p backend test`.

---

### Phase 3: Local Data Flow & Insight Verification
1. Run local recalculation task:
   ```bash
   mise run tracker:companies:recalculate
   ```
2. Inspect terminal output to verify:
   - All company metrics reflect actual database contents.
   - Salary ranges, FIT scores, and location lists are properly formatted.
3. Verify local Ktor server:
   ```bash
   mise run remote:local:run
   # In another terminal:
   curl -f http://127.0.0.1:8080/health
   curl -f -X POST http://127.0.0.1:8080/tasks/companies-recalculate-metrics
   ```
4. Verify updated Firestore documents in `companies/{id}.metrics`.

---

### Phase 4: Initial Cloud Run & Scheduler Deployment

#### Execution Timing & Lifecycle Breakdown ("When Do We Do This?")
1. **Initial Baseline Deployment (Feature Rollout — COMPLETED & VERIFIED)**:
   - **When:** Executed immediately following Phase 3, once the local engine, Proto contracts, `BackendServiceTest` (100% pass), and local Firestore recalculation (`mise run tracker:companies:recalculate`) were verified locally.
   - **Purpose:** Provisions the scale-to-zero container service (`job-tracker-backend`) and Cloud Scheduler routine sweep job in Google Cloud Platform.
   - **Verification Result:** Deployed to `https://job-tracker-backend-399944190604.us-east1.run.app` and verified via `mise run remote:sweep`.
2. **Autonomous Routine Execution (Continuous / Hands-Free)**:
   - **When:** Runs automatically every 6 hours (`0 */6 * * *`) via Google Cloud Scheduler.
   - **Action:** Cloud Scheduler calls `${SERVICE_URL}/tasks/sweep`, which triggers `BackendService.performRoutineSweep()` to continuously recalculate and update all company metrics in Firestore without manual developer intervention.
3. **Ad-Hoc / On-Demand Refresh**:
   - **When:** Whenever a user or agent modifies applications in Firestore and desires an immediate remote database update without waiting for the 6-hour scheduler window.
   - **Action:** Run `mise run remote:sweep` (or locally: `mise run tracker:companies:recalculate`).
4. **Codebase Updates & CI/CD Redeployment**:
   - **When:** Re-run `mise run remote:deploy:service` ONLY when code inside `backend/` or data schema definitions (`records/proto` or `tracker.schema.json`) are modified. In automated CI/CD environments, this task executes on merges to `main` touching `backend/**`.

#### Deployment Steps:
1. **Pre-flight Check**:
   - Ensure `TRACKER_PROJECT` or `REMOTE_GCP_PROJECT` is set:
     ```bash
     export TRACKER_PROJECT="job-search-fnc314-dev"
     export REMOTE_GCP_REGION="us-east1"
     ```
   - Ensure Artifact Registry repository exists:
     ```bash
     gcloud artifacts repositories describe job-tracker-backend --location=us-east1 --project="${TRACKER_PROJECT}" || \
     gcloud artifacts repositories create job-tracker-backend --repository-format=docker --location=us-east1 --project="${TRACKER_PROJECT}"
     ```
2. **Deploy Cloud Run Service**:
   - Run via local Docker (or Cloud Build fallback):
     ```bash
     mise run remote:deploy:service --local
     # Fallback if local Docker daemon is not running:
     mise run remote:deploy:service --cloud
     ```
   - Confirms Cloud Run deployment with scale-to-zero settings:
     - Service name: `job-tracker-backend`
     - CPU: 1, Memory: 512Mi, Min instances: 0, Max instances: 1 ($0 free tier).
3. **Deploy Cloud Scheduler Sweep**:
   - Run:
     ```bash
     mise run remote:deploy:scheduler
     ```
   - Creates or updates `job-tracker-routine-sweep` on CRON `0 */6 * * *` targeting `${SERVICE_URL}/tasks/sweep`.
4. **Remote Smoke Test**:
   - Retrieve service URL:
     ```bash
     SERVICE_URL=$(gcloud run services describe job-tracker-backend --region=us-east1 --project="${TRACKER_PROJECT}" --format="value(status.url)")
     curl -f "${SERVICE_URL}/health"
     curl -f -X POST "${SERVICE_URL}/tasks/companies-recalculate-metrics"
     ```
   - Trigger routine sweep:
     ```bash
     mise run remote:sweep
     ```

---

### Phase 5: `frontend/core/widgets` Extraction
1. **[NEW / EXTRACT] `JobApplicationCard.kt` & `JobApplicationCardUiModel.kt`**:
   - Promoted from `features/job-applications-list/ui` into `core.widgets`.
   - Adheres to `<WidgetName>Card` and `<WidgetName>CardUiModel` conventions.
   - Used by both `job-applications-list` and `company-details`.
2. **[NEW] `CompanyMetricsTextRow.kt`**:
   - Renders colored text metrics using `TrackerStatus.contentColor()`:
     - **Draft**: count & % in subtle text (`DRAFT`)
     - **Submitted**: count & % in tertiary color (`SUBMITTED`)
     - **Interviewing**: count & % in secondary color (`INTERVIEWING`)
     - **Offer**: count & % in primary color (`OFFER`)
     - **Rejected**: count & % in error color (`REJECTED`)
3. **[NEW] `CompanyLogoImage.kt`**:
   - Shared logo rendering using Coil `AsyncImage` with placeholder and error fallbacks.

---

### Phase 6: `frontend/features/companies-list`
1. **Modules**: Create `frontend/features/companies-list/{data,domain,ui}` with standard Gradle build files and README.md.
2. **`CompanyCardUiModel.kt`**:
   - Holds company id, name, logoUrl, website, industry, primaryChannel, salaryRange, locations, averageFitScore, counts, and percentages.
3. **`component/CompanyCard.kt`**:
   - Uses Compose `Grid` with `@file:OptIn(ExperimentalGridApi::class)`.
   - **Compact Viewport**:
     - *Collapsed*: Logo, submitted count, total count, average FIT score, industry, animated chevron.
     - *Expanded*: Adds website, `CompanyMetricsTextRow`, salary range, primary source, locations, and view details button.
   - **Medium / Expanded Viewport**:
     - Surfaces all expanded information directly on the card in the responsive grid array.
4. **`CompaniesListScreen.kt` & `CompaniesListNavigation.kt`**:
   - Implements `CompaniesListRoute : NavKey` and MVI state handling.

---

### Phase 7: `frontend/features/company-details`
1. **Modules**: Create `frontend/features/company-details/{data,domain,ui}`.
2. **`CompanyDetailsUiModel.kt`**: Holds company overview details and `List<JobApplicationCardUiModel>`.
3. **`CompanyDetailsScreen.kt`**:
   - Top section: Company Overview metrics card.
   - Bottom section: Responsive array of job applications using `JobApplicationCard`.
   - Clicking an application card invokes `onNavigateToApplication(applicationId)` which routes to `features/job-application-details`.
4. **`CompanyDetailsNavigation.kt`**: `CompanyDetailsRoute(companyId, companyName, website) : NavKey`.

---

### Phase 8: Shell Navigation & AppScaffold Updates
1. **`AppScaffold.kt`**:
   - Update destination sequence:
     1. `ListRoute` (Applications)
     2. `CompaniesListRoute` (Companies) — Uses `Icons.Filled.Business` / `Icons.Outlined.Business`
     3. `EditsRoute` (Pending Edits)
     4. `SettingsRoute` (Settings)
   - Dynamic Centered Top Bar Title:
     - `CompanyDetailsRoute`: Displays `currentDestination.companyName`.
     - `DetailRoute`: Displays `currentDestination.roleTitle`.
   - Top-Right Action Button:
     - On `CompanyDetailsRoute`: Renders Globe icon button (`Icons.Filled.Public`) calling `PlatformFileOpener().openUrl(website)`.
2. **`App.kt`**:
   - Register routes in `SavedStateConfiguration`.
   - Wire canonical `ListDetailSceneStrategy` for `CompaniesListRoute` and `CompanyDetailsRoute`.
   - Route `JobApplicationCard` clicks to `DetailRoute(applicationId, roleTitle)`.

---

## Verification Plan

### Automated Tests
1. **Records Proto & Serialization**:
   - `./gradlew :records:test`
2. **Backend Engine & Endpoints**:
   - `./gradlew -p backend test`
3. **Frontend KMP Feature Unit Tests**:
   - `./gradlew :features-companies-list-domain:jvmTest :features-companies-list-ui:jvmTest`
   - `./gradlew :features-company-details-domain:jvmTest :features-company-details-ui:jvmTest`
4. **Konsist Architecture Guard**:
   - `mise run dev:konsist`
5. **Multiplatform Client Compilation**:
   - `./gradlew :apps-desktop:compileKotlinJvm`
   - `./gradlew :apps-android:assembleDebug`

### Manual Verification
1. **Local Recalculation Check**:
   - Run `mise run tracker:companies:recalculate` and verify computed stats match application documents.
2. **Local Server Smoke Test**:
   - Run `mise run remote:local:run`, hit `/health` and `/tasks/companies-recalculate-metrics`.
3. **Cloud Run Remote Check**:
   - Verify deployed Cloud Run service URL and scale-to-zero ($0 cost) behavior.
4. **Navigation & Dynamic Top Bar**:
   - Inspect navigation rail and bottom bar: `Applications` -> `Companies` -> `Pending Edits` -> `Settings`.
   - Verify dynamic Company Name on `CompanyDetailsScreen` and Role Title on `JobApplicationDetailsScreen`.
   - Verify Globe icon opens company website in default browser.
5. **List-Detail & Adaptive Layout**:
   - On Desktop/Tablet: Split pane with companies list on the left and selected company details on the right.
   - On Phone: Collapse/expand cards on list, push navigation into detail screen.
   - Tapping application card inside company details launches `job-application-details`.
