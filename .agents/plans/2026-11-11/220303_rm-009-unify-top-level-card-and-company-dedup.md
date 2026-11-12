# Implementation Plan: RM-009 — Unify Top-Level Card Component & Company Deduplication

**Status:** Completed  
**Date:** 2026-11-11  
**Target:** `records`, `backend`, and `frontend` (cross-subsystem)  
**Harness:** Antigravity  
**Model:** gemini-3.1-pro  
**Session:** d6855c57-5b9c-43fa-a712-c814f7098513  
**Evaluates:** [`ROADMAP.md`](../../../ROADMAP.md)  
**Schema Authority:** [`tracker.schema.json`](../../../tracker.schema.json)  

---

## 1. Executive Summary & Goals

Implement **RM-009 (Unify Top-Level Card Component)** from [`ROADMAP.md`](../../../ROADMAP.md), along with its two Side Quests:

1. **Unify Top-Level Card Component (`TopLevelCard`)**: Extract a single reusable, responsive, `Grid`-based card skeleton in `:frontend:core-widgets` used across top-level list destinations (`JobApplicationCard`, `CompanyCard`, and `EditCard`), with `@file:OptIn(ExperimentalGridApi::class)` and all other `@OptIn` annotations declared at the `@file:` level, full KDoc coverage on all `public` declarations, and strict adherence to Kotlin `explicitApi()`.
2. **Side Quest 1 — Unify `CompanyLogoImage`**: Standardize predefined logo sizes across all use-sites, render a transparent background when a valid logo loads, display a dynamic loading indicator/shimmer during `AsyncImagePainter.State.Loading`, and surface a red top banner indicating the HTTP error code when loading fails (with detailed diagnostics logged via Coil's `EventListener` setup).
3. **Side Quest 2 — Trace, Fix, & Migrate Company Duplication (`Meta`, `Ford Motor Company`, etc.)**: Fix the divergent slug generators across `records/parser`, `backend/`, `frontend/apps/cli`, and `scripts/scrape.py`, provide a deterministic migration/deduplication task to merge duplicate Firestore `companies` documents, add defensive deduplication in the frontend repository, and update architecture documentation to prevent future regressions.

---

## 2. Architecture & Design Principles

### 2.1 Unified Responsive `Grid` Card Architecture (`TopLevelCard`)
- **Single `Grid` Layout Engine**: All top-level cards share a single `androidx.compose.foundation.layout.Grid`-based layout in `:frontend:core-widgets`, eliminating ad-hoc `Row`/`Column` forks across `JobApplicationCard`, `CompanyCard`, and `EditCard`.
- **File-Level Opt-Ins**: `@file:OptIn(ExperimentalGridApi::class)` (and any `ExperimentalMaterial3Api` opt-ins) are declared strictly at the `@file:` level.
- **Explicit API & KDoc Completeness**: Every `public` class, enum, property, and `@Composable` function is annotated with explicit visibility (`public`), explicit return types, and comprehensive KDoc.
- **Viewport-Aware Interaction Contract**:
  - **Compact Viewports with Hidden Content** (`isCompact && (expandedContent != null || wideBody != null)`): Card tap and chevron tap toggle an `AnimatedVisibility` container revealing `expandedContent` and the action bar.
  - **Wide Viewports or No Hidden Content** (`!isCompact || (expandedContent == null && wideBody == null)`): Chevron is omitted; the `Card` itself acts as a direct navigation target (`clickable(onClick = onClick)`).
- **Equalized Action Treatment & Surface Variations**:
  - Action buttons are modeled as `ImmutableList<TopLevelCardAction>` and rendered inside an equal-width (`1.fr`) `Grid` row with uniform height and `MaterialTheme.shapes.small`.
  - Surface and elevation treatments are standardized via `TopLevelCardSurfaceVariant` (`Surface`, `Elevated`, `FilledTonal`, `Highlighted`).

### 2.2 Standardized `CompanyLogoImage` State Machine
- **Predefined Size Tokens**: All logo instances use `CompanyLogoSize` (`Small` = `48.dp`, `Medium` = `64.dp`, `Large` = `72.dp`) backed by `MaterialTheme.componentSizes`.
- **Transparent Loaded Surface**: When `isValidLogoLoaded` is `true`, the container background transitions to `Color.Transparent` with no border so alpha-channel logos render without surface bleed.
- **Dynamic Loading & HTTP Error Banner**:
  - `AsyncImagePainter.State.Loading` renders an animated shimmer brush and subtle indeterminate `CircularProgressIndicator`.
  - `AsyncImagePainter.State.Error` renders the fallback monogram plus a top-aligned red error banner (`MaterialTheme.colorScheme.error` / `onError`) displaying the HTTP status code (`"HTTP 404"`, `"HTTP 500"`, or `"ERR"`), while `CoilEventListenerBridge` logs full diagnostics.

### 2.3 Root Cause Analysis & Single Source of Truth for Company Slugs (Side Quest 2)

Tracing company creation and lookup from the UI down to Firestore and the parser layer revealed **four distinct sources of duplication**:

```mermaid
flowchart TD
    subgraph Sources["Company Write Sources"]
        S1["scripts/scrape.py (_slugify)<br/>Naive regex: 'Ford Motor Company' -> 'ford-motor-company'<br/>'Meta Platforms' -> 'meta-platforms'"]
        S2["BackendService.kt (recalculateCompanyMetrics & addCompany)<br/>Naive regex fallback when companyId missing"]
        S3["AddApplicationCommand.kt (frontend/apps/cli)<br/>Writes applications WITHOUT 'companyId' field"]
        S4["DocumentIdGenerator.kt / contract.py<br/>Strips 'company', 'the', 'inc':<br/>'Ford Motor Company' -> 'ford-motor'"]
    end

    S3 -->|Missing companyId| S2
    S1 -->|Writes companies/ford-motor-company| FS[("Firestore: companies collection")]
    S2 -->|Writes companies/ford-motor-company| FS
    S4 -->|Writes companies/ford-motor| FS
    FS -->|Unfiltered getCollection('companies')| UI["CompaniesListScreen<br/>Shows duplicate cards for same employer"]
```

1. **Historical Slug Rule Change (Commit `437f5a07` on 2026-11-04)**:
   - Prior to Nov 4, `contract.slug("Ford Motor Company")` used a simple regex and produced `"ford-motor-company"`.
   - Commit `437f5a07` updated `contract.slug()` and `DocumentIdGenerator.kt` to strip `corpSuffixes` (including `"company"`), changing the derived slug for `"Ford Motor Company"` to `"ford-motor"`, leaving both documents in Firestore `companies`.
2. **Naive `_slugify` in `scripts/scrape.py`**:
   - `scrape.py` defined its own `_slugify(s)` instead of calling `contract.slug(company)`. When RM-005 added `_enrich_company_in_firestore(db, company_slug, company)`, every scraped posting with `"Company"`, `"Inc."`, `"The"`, or `"Platforms"` in the employer name created a second document in `companies/{naive-slug}`.
3. **Missing `companyId` in `AddApplicationCommand.kt` + Naive Fallback in `BackendService.kt`**:
   - `AddApplicationCommand` (`mise run tracker:add`) inserted application documents without setting `companyId`.
   - When `BackendService.recalculateCompanyMetrics()` grouped applications by `companyId`, it fell back to naive regex instead of `DocumentIdGenerator.slug()`.
4. **Unnormalized Employer Name Variants (`Meta` vs `Meta Platforms`, etc.)**:
   - Job portals frequently emit `"Meta Platforms"` or `"Meta Platforms, Inc."` while existing tracker records use `"Meta"`. Adding canonical alias normalization in `DocumentIdGenerator.slug()` and `contract.slug()` resolves `"Meta Platforms"` to `"meta"`.

---

## 3. Proposed Changes & Implementation Steps

### Step 1: Design System Tokens (`:frontend:core-design-system`)
- Update `Dimensions.kt` (`ComponentSizes`) with `companyLogoSmall` (`48.dp`), `companyLogoMedium` (`64.dp`), `companyLogoLarge` (`72.dp`), `cardElevationDefault` (`0.dp`), `cardElevationLow` (`1.dp`), `cardElevationMedium` (`3.dp`), `cardElevationHigh` (`6.dp`), and `chevronTouchWidth` (`40.dp`).

### Step 2: Core Widgets — Unified `TopLevelCard` & `CompanyLogoImage` (`:frontend:core-widgets`)
- Create `TopLevelCard.kt` with `@file:OptIn(ExperimentalGridApi::class)`, `TopLevelCardSurfaceVariant`, `TopLevelCardAction`, and `@Composable public fun TopLevelCard(...)`.
- Refactor `JobApplicationCard.kt` to delegate to `TopLevelCard` and use `CompanyLogoSize`.
- Update `CompanyLogoImage.kt` with `CompanyLogoSize`, transparent background when loaded, animated loading shimmer + progress indicator, and red HTTP status code banner on error.
- Update `CoreWidgetsModule.kt` to extract HTTP status codes from Coil `ErrorResult.throwable` (`HttpException`) and log detailed diagnostics.

### Step 3: Feature Card Adoptions & `@file:OptIn` Alignment (`:frontend:features:*`)
- Refactor `CompanyCard.kt` to delegate to `TopLevelCard` and use `CompanyLogoSize`.
- Refactor `EditCard` in `TrackerEditsScreen.kt` to delegate to `TopLevelCard` with `TopLevelCardAction` buttons and `@file:OptIn`.
- Update `CompanyDetailsScreen.kt` and `JobApplicationDetailsScreen.kt` (`CompanyProfileSection`) to use `CompanyLogoImage` with `CompanyLogoSize.Large` and `CompanyLogoSize.Small`, and move `@OptIn` annotations to `@file:OptIn`.

### Step 4: Side Quest 2 — Fix Company Duplication & Migration (`records`, `backend`, `frontend`, `scripts`)
- Update `DocumentIdGenerator.kt`, `CompanySectorNormalizer.kt`, and `scripts/contract.py` with canonical company slug aliases (`"meta-platforms"` -> `"meta"`, `"ford-motor-company"` -> `"ford-motor"`, etc.).
- Update `BackendService.kt` (`recalculateCompanyMetrics`, `addCompany`, and new `deduplicateCompanies(dryRun)`), `backend/Main.kt` (`--deduplicate-companies` and `POST /companies/tasks/deduplicate`), and `.config/mise/tasks/tracker/companies/deduplicate.sh`.
- Update `AddApplicationCommand.kt` to populate `companyId` via `DocumentIdGenerator.slug(company)`.
- Update `scripts/scrape.py` to use `contract.slug(company)` for company slugs.
- Add defensive deduplication in `RemoteCompaniesListDataSourceImpl.kt`.

### Step 5: Documentation & Plan Bookkeeping
- Update `frontend/core/widgets/README.md`, `frontend/core/design-system/README.md`, `frontend/features/companies-list/README.md`, `AGENTS.md`, and `ROADMAP.md`.

---

## 4. Verification Plan & Testing Checklist

- [x] **Records Parser Unit Tests**: `./gradlew -p records test detekt`
- [x] **Backend Compilation & Tests**: `./gradlew -p backend test detekt`
- [x] **Frontend Metadata, Unit Tests, Konsist, & Detekt**:
  - `./gradlew :frontend:compileCommonMainKotlinMetadata`
  - `mise run frontend:test`
  - `mise run frontend:detekt`
  - `mise run frontend:readmes`
- [x] **Firestore Company Deduplication Dry-Run & Execution**:
  - `mise run tracker:companies:deduplicate --dry-run`
- [x] **UI & Card Verification**:
  - `JobApplicationCard`, `CompanyCard`, and `EditCard` use `TopLevelCard` with `@file:OptIn(ExperimentalGridApi::class)`
  - `CompanyLogoImage` unified across all 4 use-sites with transparent loaded state, dynamic loading indicator, and HTTP error banner

---

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-11/220303_rm-009-unify-top-level-card-and-company-dedup.md`
- [x] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
