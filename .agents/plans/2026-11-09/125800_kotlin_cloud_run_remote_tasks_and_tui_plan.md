# Architecture & Implementation Plan: Screaming Monorepo, Root Composite Build, Kotlin Cloud Run Backend & Unified Frontend

**Status:** Completed  
**Date:** 2026-11-09  
**Target:** repository-wide  
**Model:** gemini-3.8-flash  
**Session:** 3e3f750b-aea3-46d6-8014-70592ca54e5d  
**Schema Authority:** [`tracker.schema.json`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/tracker.schema.json)  

---

## 1. Executive Summary & Goals

The AI Job Search repository is evolving into a full **Screaming Monorepo** where the top-level directory structure unambiguously reflects system domains rather than framework leftovers:
1. **`records/`**: The immutable data contract and wire protocol layer (Protobuf v3, serialization, parsers).
2. **`backend/`**: Headless Cloud Run execution service (Kotlin Ktor) executing scheduled routine sweeps (4x/day), Composio/email scanning, staging inbox payloads, and linking matched email IDs to job application records.
3. **`frontend/`**: Flattened, unified user application workspace (renamed from `ui/kmp`), housing:
   - `frontend/apps/android`
   - `frontend/apps/desktop`
   - `frontend/apps/web` (Compose Wasm/JS replacing legacy static HTML dashboard `ui/web`)
   - `frontend/apps/terminal` (Kotlin TUI replacing Python `ui/terminal/tracker_tui.py`)
   - `frontend/apps/cli` (Kotlin CLI fat JAR replacing Python `scripts/tracker.py` for `mise`)
4. **`gradle/` at Root**: Root-level Gradle architecture housing wrapper, version-catalogs included build, and shared convention settings applied across all included builds.

---

## 2. Architecture & Root Composite Build

```mermaid
flowchart TD
    subgraph RootMonorepo ["Root Monorepo Architecture"]
        RootSettings["settings.gradle.kts (Composite Root)"]
        SharedSettings["gradle/conventions/subsystem.settings.gradle.kts (Shared DRY Settings)"]
        CatBuild["gradle/tools/version-catalogs (Included Build)"]
    end

    subgraph RecordsDomain ["records/ Subsystem"]
        Proto["records/proto (proto3)"]
        Parser["records/parser (parsers)"]
    end

    subgraph BackendDomain ["backend/ Subsystem (Cloud Run - Always Free Tier)"]
        Ktor["backend (Ktor JVM Service - min instances: 0)"]
        Scheduler["Cloud Scheduler (Configurable CRON - default: 0 */6 * * *)"]
    end

    subgraph FrontendDomain ["frontend/ Subsystem (All Clients)"]
        Android["frontend/apps/android"]
        Desktop["frontend/apps/desktop"]
        Web["frontend/apps/web (Compose Wasm/JS)"]
        Terminal["frontend/apps/terminal (Kotlin TUI)"]
        Cli["frontend/apps/cli (Pre-compiled JAR for Mise)"]
        CoreFeat["frontend/core & frontend/features"]
    end

    CatBuild -->|dev.fnc314.tools.jobtracker.gradle.tools:version-catalogs| SharedSettings
    SharedSettings -->|apply from common script| RecordsDomain
    SharedSettings -->|apply from common script| BackendDomain
    SharedSettings -->|apply from common script| FrontendDomain

    RecordsDomain -->|libs.records.proto (Typed Accessor)| BackendDomain
    RecordsDomain -->|libs.records.parser (Typed Accessor)| FrontendDomain
    Scheduler -->|OIDC Auth /tasks/sweep| Ktor
    Cli -->|Invoked by mise tracker:* & remote:*| Ktor
```

### 2.1 Shared DRY `settings.gradle.kts` via Convention Script
To ensure absolute DRY configuration across all included builds without duplicated boilerplate, root provides:
`gradle/conventions/subsystem.settings.gradle.kts`:
```kotlin
// gradle/conventions/subsystem.settings.gradle.kts
pluginManagement {
    includeBuild("${rootDir}/../gradle/tools/version-catalogs")
    includeBuild("${rootDir}/../gradle/tools/project-plugins")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from("dev.fnc314.tools.jobtracker.gradle.tools:version-catalogs:1.0.0")
        }
    }
}
```
In `records/settings.gradle.kts`, `backend/settings.gradle.kts`, and `frontend/settings.gradle.kts`:
```kotlin
apply(from = "../gradle/conventions/subsystem.settings.gradle.kts")
```

### 2.2 Strictly Typed Project Accessors, Version Catalog References & TOML Formatting
**ZERO string-based dependencies in build scripts.**
In `gradle/tools/version-catalogs` (and `libs.versions.toml`), all entries are strictly formatted using `tombi` on `$PATH` where keys inside tables are indented by 2 spaces:

```toml
[versions]
  kotlin = "2.4.21"
  clikt = "5.0.3"
  mordant = "3.0.2"
  ktor = "3.6.1"

[libraries]
  records-proto = { module = "dev.fnc314.tools.jobsearch.records:proto" }
  records-parser = { module = "dev.fnc314.tools.jobsearch.records:parser" }
  ktor-server-netty = { module = "io.ktor:ktor-server-netty", version.ref = "ktor" }
  clikt = { module = "com.github.ajalt.clikt:clikt", version.ref = "clikt" }
  mordant = { module = "com.github.ajalt.mordant:mordant", version.ref = "mordant" }
```

In `backend/build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.records.proto)
    implementation(libs.records.parser)
    implementation(libs.ktor.server.netty)
    implementation(libs.google.cloud.firestore)
}
```
In `frontend/apps/cli/build.gradle.kts` and `frontend/apps/terminal/build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.records.proto)
    implementation(libs.records.parser)
    implementation(projects.core)
    implementation(libs.clikt)
    implementation(libs.mordant)
}
```

---

## 3. Top-Level Structural Alignment & Flattening

| Current Path | Target Path | Changes / Actions |
| :--- | :--- | :--- |
| `records/` | `records/` | Keep wire contracts; point version catalog to `version-catalogs` via shared settings convention. |
| *(None)* | `backend/` | **[NEW]** Ktor JVM Cloud Run service implementing atomic task endpoints, scheduled sweeps, Composio inbox polling, and email message ID association. |
| `ui/kmp/` | `frontend/` | Flatten directory name to reflect client application suite. |
| `ui/terminal/tracker_tui.py` | `frontend/apps/terminal/` | Port Python curses TUI to Kotlin TUI (using Mordant / Lanterna / Clikt) sharing `records` and `frontend/core`. |
| `scripts/tracker.py` | `frontend/apps/cli/` | Implement unified Kotlin CLI fat JAR (`jobsearch-cli`) handling all local tracker commands. |
| `ui/web/` | `frontend/apps/web/` | Legacy static HTML dashboard superseded by Compose Multiplatform Web (Wasm/JS). |
| `ui/kotlin-toolchain/` | *(Delete / Archive)* | Stale template directory removed to avoid confusion. |

---

## 4. Email ID Association & Retroactive Historical Backfill Plan

### 4.1 Schema Contract
- `JobApplication` maintains `repeated string emailMessageIds = 23;` representing all linked email correspondence.
- `TrackerEvent` carries `emailMessageId = 7`.

### 4.2 Retroactive Historical Backfill Plan
1. **Source Discovery**:
   - Extract processed message IDs from `gmail-sync/state.json` (200+ historical message IDs).
   - Read all existing timeline events in Firestore `applications/{id}/events` that have non-null `emailMessageId` fields.
2. **Correlator Tool (`mise run tracker:backfill:emails`)**:
   - A dedicated migration command in `frontend/apps/cli` queries all application records and events.
   - For every event with `emailMessageId`, ensure the parent application document's `emailMessageIds` set includes that ID.
   - For historical messages in `gmail-sync/state.json` without an event, perform deterministic matching against company name and job title to link orphan message IDs to their corresponding application.
   - Writes are atomic and idempotent via Firestore transactions.
3. **Application Details Presentation**:
   - Both `frontend/apps/terminal` and `frontend/features/job-application-details` render a **"Related Emails"** card listing all linked message IDs, subjects, and timestamps.

---

## 5. UI Visual Indicators (Scoped: Visuals Only, No Push Notifications)

Per requirements, push notifications are out of scope. Visual cues and treatments in user interfaces are the core mechanism for communicating automated background activity:
1. **Application List Badges**:
   - Records updated or created via automated cloud sweeps display a subtle indicator badge (e.g. `[AUTO-SYNCED]` or a cloud glyph with relative timestamp like `2h ago`).
2. **Application Detail Status Tag**:
   - The status change header displays the actor (e.g. `Actor: Cloud Scheduler / Sweep` vs `Actor: User`).
3. **Staging / Inbox Queue Treatment**:
   - A distinct counter or tab badge for unreviewed `analysis_ready_payloads` and `proposals`.

---

## 6. Cost-Control Architecture & Deployment via `mise` ($0 / Near-Zero Free Tier)

All remote deployment and scheduled sweep operations must strictly remain **$0 or negligible fractions of a cent per month** using Google Cloud Free Tier limits:

### 6.1 Google Cloud Free Tier Guarantees
- **Cloud Run**:
  - Free tier includes **2 million requests/month**, **360,000 vCPU-seconds**, and **180,000 GiB-seconds** of memory.
  - Scale-to-zero is enforced (`--min-instances=0 --max-instances=1`). When no sweep is running, compute allocation is $0.
  - Sized at `1 vCPU, 512MiB RAM`, a 15-second sweep 4x/day uses ~1,800 seconds/month (less than 0.5% of the free quota).
- **Cloud Scheduler**:
  - Google Cloud provides **3 free jobs per billing account per month**. Our single routine sweep job uses 1 of 3 free jobs.
- **Firestore & Cloud Build / Artifact Registry**:
  - 50k reads, 20k writes/day free tier. Cloud Build includes 120 free build-minutes/day. Artifact Registry provides 0.5GB/month free storage.

### 6.2 Plainly Configurable via Environment (`.env.example` & `.env.local`)
Update `.env.example` to document all remote deployment settings:
```bash
# ==============================================================================
# Remote Cloud Run & Scheduler Settings (Optional / Remote Offload)
# ==============================================================================
# The GCP Project and Region for Cloud Run & Cloud Scheduler. Defaults to TRACKER_PROJECT.
REMOTE_GCP_PROJECT=
REMOTE_GCP_REGION=us-east1

# Cadence for automated routine sweeps. Default: 4x/day ("0 */6 * * *").
REMOTE_SWEEP_CRON="0 */6 * * *"

# Sizing and scaling limits to guarantee $0 always-free tier operation.
REMOTE_MIN_INSTANCES=0
REMOTE_MAX_INSTANCES=1
REMOTE_MEMORY=512Mi
REMOTE_CPU=1

# Container build mode: "local" (uses local Docker daemon) or "cloud" (uses Cloud Build).
REMOTE_BUILD_MODE=local
```

### 6.3 Dual-Mode Container Builds & Deployment Tasks via `mise`
Developers can build containers **either locally with Docker** (ideal for local testing or avoiding Cloud Build quotas) **or remotely via Cloud Build**:
1. **Local Build & Push Mode (`REMOTE_BUILD_MODE=local` or `--local`)**:
   - `./gradlew :backend:installDist` builds the JRE distribution locally.
   - `docker build -t ${IMAGE_URI} backend/` builds the container locally.
   - `docker push ${IMAGE_URI}` pushes directly to Google Artifact Registry.
   - Deploys image to Cloud Run using `gcloud run deploy --image ${IMAGE_URI}`.
2. **Cloud Build Mode (`REMOTE_BUILD_MODE=cloud` or `--cloud`)**:
   - `gcloud builds submit --tag ${IMAGE_URI} backend/` builds in GCP and pushes directly.
   - Deploys image to Cloud Run.

Tasks hidden behind `mise`:
- `mise run remote:deploy:service [--local|--cloud]`: Builds container (local Docker or Cloud Build) and deploys to Cloud Run with scale-to-zero settings.
- `mise run remote:deploy:scheduler`: Creates or updates the Cloud Scheduler CRON job pointing to `/tasks/sweep` using the configured `REMOTE_SWEEP_CRON`.
- `mise run remote:deploy:all`: Runs service and scheduler deployments sequentially.
- `mise run remote:destroy`: Tears down the Cloud Run service and scheduler job to instantly prevent any remote execution and guarantee zero cost.

### 6.4 Documentation Updates in `SETUP.md`
`SETUP.md` will be updated with a dedicated section:
- **Prerequisites**: Installing `gcloud` CLI and authenticating (`gcloud auth login`, `gcloud auth application-default login`, `gcloud config set project <project-id>`).
- **Enabling APIs**: Single `gcloud services enable run.googleapis.com cloudscheduler.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com` command.
- **Artifact Registry**: Creating repository `job-tracker-backend`.
- **Deploying via Mise**: Running `mise run remote:deploy:all` to achieve 100% automated background sweeps.

---

## 7. Mise Task Integration (All Operations Hidden Behind `mise`)

All local, build, format, and remote invocations remain strictly hidden behind `mise` tasks:
1. **Local Commands (`mise run tracker:*`)**:
   - Thin wrappers invoking `frontend/apps/cli/build/libs/jobsearch-cli.jar tracker <subcommand> "$@"`.
   - `mise` task lifecycle ensures the CLI JAR is built/fresh before running.
2. **Terminal UI (`mise run tui`)**:
   - Thin wrapper invoking `java -jar frontend/apps/terminal/build/libs/jobsearch-terminal.jar`.
3. **Build, Format & Lint (`mise run dev:*`)**:
   - `mise run dev:build` -> `./gradlew build`
   - `mise run dev:format` -> runs `tombi format` across all `*.toml` files ensuring 2-space table indentations.
   - `mise run dev:lint` -> unified Konsist & Detekt
4. **Remote Execution Commands (`mise run remote:*`)**:
   - `mise run remote:email:read-inbox`
   - `mise run remote:email:collect`
   - `mise run remote:proposals:expire`
   - `mise run remote:tracker:sync-due`
   - `mise run remote:sweep` (Triggers on-demand full scheduled sequence)
   - `mise run remote:status` (Queries Cloud Run health and queue metrics)
5. **Remote Deployment Commands (`mise run remote:deploy:*`)**:
   - `mise run remote:deploy:service [--local|--cloud]`
   - `mise run remote:deploy:scheduler`
   - `mise run remote:deploy:all`
   - `mise run remote:destroy`

---

## 8. Phased Implementation Strategy

### Phase 1: Root Composite Build & Shared Settings Convention
- Set up root `settings.gradle.kts` and `build.gradle.kts`.
- Create `gradle/tools/version-catalogs` included build with typed library accessors (`libs.records.proto`, `libs.records.parser`).
- Format all TOML files using `tombi format` with 2-space table entry indentation.
- Create `gradle/conventions/subsystem.settings.gradle.kts`.
- Update `records/settings.gradle.kts` using `apply(from = "../gradle/conventions/subsystem.settings.gradle.kts")` and verify via `mise run dev:test`.

### Phase 2: Frontend Flattening (`ui/kmp` -> `frontend`) & CLI Modules
- Move `ui/kmp` to `frontend/`.
- Wire `frontend/settings.gradle.kts` to consume shared settings convention.
- Add `frontend/apps/cli` module (Clikt + shared records typed accessors) and wire `mise run tracker:*` tasks.
- Add `frontend/apps/terminal` module (Mordant/Lanterna TUI) and wire `mise run tui`.
- Remove legacy `ui/web` and `ui/kotlin-toolchain`.

### Phase 3: Retroactive Historical Email Backfill & Ingestion
- Implement `JobApplication.emailMessageIds` schema integration.
- Implement `mise run tracker:backfill:emails` command to correlate and backfill historical email IDs from `gmail-sync/state.json` and timeline events.
- Surface linked email list on application detail views in `terminal` and Compose UI.

### Phase 4: Backend Cloud Run Service (`backend/`) & Deployment Tasks
- Create `backend/` Ktor JVM module in root composite build using `apply(from = "../gradle/conventions/subsystem.settings.gradle.kts")`.
- Implement atomic task handlers:
  - Email inbox scanning via Composio API credentials (stored in Secret Manager).
  - Staging inbox messages into `analysis_ready_payloads` Firestore collection.
  - Linking `emailMessageId` to application timeline events and details.
  - Deterministic proposal staging (`proposals` collection) & expiration.
- Add Dockerfile (Eclipse Temurin JRE Alpine) with scale-to-zero configuration (`--min-instances=0 --max-instances=1`).
- Add `mise run remote:deploy:*` tasks supporting dual-mode `--local` and `--cloud` builds.
- Update `.env.example` and `SETUP.md` with prerequisite `gcloud` and environment variable instructions.

### Phase 5: UI Visual Treatments
- Update `frontend/core` and `frontend/apps/*` to render visual indicator badges for auto-updated records.

---

## 9. Verification Plan

### Automated Verification
```bash
# Build all composite subsystems
mise run dev:build

# Verify TOML formatting with tombi
mise run dev:format

# Run unit tests across records, backend, and frontend
mise run dev:test

# Run architectural Konsist & Detekt checks
mise run dev:lint
```

### Manual Verification
1. **CLI Execution**: Run `mise run tracker:list` and confirm instant response from `frontend/apps/cli`.
2. **Terminal TUI**: Run `mise run tui` and verify interactive keyboard navigation, email ID detail display, and auto-sync badge rendering.
3. **Retroactive Email Backfill**: Run `mise run tracker:backfill:emails` and confirm historical application records reflect linked email IDs.
4. **Cloud Run Remote Trigger**: Run `mise run remote:sweep` and inspect Firestore `analysis_ready_payloads`, `proposals`, and linked email message IDs.
5. **Deployment & Cost Check**: Run `mise run remote:deploy:all --local`, verify Cloud Run instance count is 0 when idle, and confirm Cloud Scheduler job uses 1 of 3 free jobs.
6. **Documentation & Template Check**: Inspect `.env.example` and `SETUP.md` for complete configuration coverage.

---

## 10. Bookkeeping & Completion
- [x] Plan persisted at `.agents/plans/2026-11-09/125800_kotlin_cloud_run_remote_tasks_and_tui_plan.md`
- [x] Registry `README.md` row updated
- [x] Root composite build and shared convention settings established
- [x] Reorganized monorepo structure (`records/`, `backend/`, `frontend/`)
- [x] Added `repeated string emailMessageIds = 23;` to proto, schema, domain, and entities
- [x] Implemented unified Kotlin CLI (`frontend/apps/cli`) and `mise run tracker:backfill:emails`
- [x] Verified and applied retroactive email backfill to Firestore
- [x] Implemented Kotlin TUI (`frontend/apps/terminal`) with Related Emails card & `[AUTO-SYNCED]` badges
- [x] Implemented Ktor backend Cloud Run service (`backend/`) with routine sweep handlers
- [x] Documented remote deployment and scale-to-zero free tier setup in `SETUP.md`
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and commit using exact message: `125800_kotlin_cloud_run_remote_tasks_and_tui_plan.md`
- [ ] Push to `origin main` (`git push origin main`)
