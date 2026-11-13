# Implementation Plan: Deterministic Fit Score Unification & Backend Multi-Module Migration

**Status:** Completed  
**Date:** 2026-11-12  
**Target:** `backend`, `records`, `frontend` (cross-subsystem)  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Supersedes:** previous draft of deterministic-fit-score-unification  

---

## 1. Executive Summary & Goals

The `/rank` workflow currently dispatches AI agents to score job postings. The user wants a **fully deterministic, code-based Fit Score** that requires no AI entity, with the ranking logic isolated into its own Gradle module (`job-post-ranking`) under `backend/`. Additionally, the current `backend/` (a single-module Ktor service) will be reworked into a multi-module Gradle project with the existing service code moving to a `service` sub-module.

**Key objectives:**
1. **Unify & enhance** `RankScorer` with FitEvaluator rules (absorb, then **DELETE** FitEvaluator — no deprecation)
2. **Eradicate manual fit editing** — remove the ability to hand-edit the `fit` value, replace with a **dynamic weights** mechanism so the user can "weigh in"
3. **Isolate ranking logic** into `backend/job-post-ranking/` — a standalone, testable, JAR-exportable module
4. **Rework `backend/`** into multi-module Gradle: existing code → `backend/service/`, new `backend/settings.gradle.kts` with sub-module includes
5. **Add backend convention plugins** via `gradle/tools/project-plugins` for cross-sub-module consistency
6. **Update `/rank` AI command** to use deterministic scorer

### Current State

| Component | Location | Status | Called By |
|-----------|----------|--------|-----------|  
| `RankScorer.kt` | `records/parser/` | Active | `RankCommand.kt` (CLI) |
| `FitEvaluator.kt` | `records/parser/` | Dead in production (tests only) | `FitEvaluatorTest.kt` only |
| `rank.py` | `scripts/` | Deprecated (RM-001) | None |
| `/rank` AI command | `.agents/commands/rank.md` | Active | AI agents during `/scrape` flow |
| `UpdateFitScoreDialog` | `frontend/.../DetailDialogs.kt` | Active | User manual fit editing |
| `UpdateFitScoreUseCase` | `frontend/.../domain/` | Active | ViewModel → BackendService |
| `backend/` | Single-module Gradle | Active | Cloud Run, local CLI tasks |

### Key Gap

`RankScorer` uses keyword matching across 4 dimensions but lacks:
- **Role-level detection** (principal/staff/senior bonuses from FitEvaluator)
- **Sector alignment** (FinTech/FinServ bonus)
- **Location scoring** (remote/Pittsburgh bonus, NYC/DC neutral)
- **Calibration signals** (Reddit cap at 75, known low-conversion company caps)
- **Deal-breaker penalties** (hybrid-only -30, consulting -25, people management -20)
- **Dynamic weights** — user cannot influence scoring without manually overriding the fit value

`FitEvaluator` has role/sector/location/calibration rules but is dead production code — only tests call it.

### Scope

**In Scope:**
- Absorb FitEvaluator's rules into RankScorer, then **DELETE** FitEvaluator (no deprecation)
- Isolate all ranking logic into `backend/job-post-ranking/` module
- Rework `backend/` into multi-module Gradle (`service/` + `job-post-ranking/`)
- Eradicate manual fit editing (`UpdateFitScoreDialog`, `UpdateFitScoreUseCase`)
- Add dynamic `ScoringWeights` so users can "weigh in" on the scoring process
- Add backend convention plugin via `gradle/tools/project-plugins`
- Update `/rank` AI command to use deterministic RankScorer
- Update proto, schema, and Kotlin models for `ScoringWeights`
- Ensure all tests pass

**Out of Scope:**
- Removing `rank.py` (already tracked by RM-001 Phase 3)
- Modifying the `/apply` workflow (it re-evaluates independently)


## 2. Architecture & Design Principles

### Backend Multi-Module Structure (Target)

```
backend/
  settings.gradle.kts          (rewritten: includes :service, :job-post-ranking)
  build.gradle.kts             (new: root build, applies conventions)
  service/
    build.gradle.kts           (existing backend build.gradle.kts moved here)
    src/                       (existing backend/src/ moved here)
  job-post-ranking/
    build.gradle.kts           (new: pure Kotlin JVM library)
    src/main/kotlin/           (RankScorer, ScoringConfig, etc.)
    src/test/kotlin/           (RankScorerTest, ScoringWeightsTest)
```

### Key Decisions

1. **DELETE, not deprecate.** FitEvaluator will be deleted entirely. Its rules are absorbed into the unified scorer in `job-post-ranking`. No `@Deprecated` annotation — the file will be removed.
2. **Dynamic weights instead of manual fit.** Replace the `UpdateFitScoreDialog` slider with a `ScoringWeights` configuration object. Users can adjust dimension weights (e.g. Technical 30% → 40%) via a new dialog. The score is always computed deterministically from weights + posting text.
3. **`job-post-ranking` as a pure library.** No framework dependencies (no Ktor, no Firebase). Just Kotlin + kotlinx.serialization. Exportable as a JAR via `maven-publish`. Consumable by `backend/service`, CLI, and any future client.
4. **Backend convention plugin.** A new convention plugin (`backend-service`) in `gradle/tools/project-plugins` will apply shared configuration (Ktor deps, Firebase deps, JVM toolchain, testing) across `backend` sub-modules.
5. **Fit score is computed, not stored as a user override.** The `fitRating` field in Firestore will be populated only by the deterministic scorer. The `UpdateFitScoreUseCase` and `UpdateFitScoreDialog` will be removed. User influence flows through `ScoringWeights` only.
6. **ScoringWeights as a first-class config.** `data class ScoringWeights(val technical: Double = 0.30, val experience: Double = 0.25, val behavioral: Double = 0.15, val career: Double = 0.30, val roleLevelMultiplier: Double = 1.0, val sectorMultiplier: Double = 1.0, val locationMultiplier: Double = 1.0)` — serializable, storable in Firestore `metadata`, and passable to `evaluate()`.

### Architecture Diagram

```mermaid
graph LR
    A[Job Posting Text] --> B[job-post-ranking: RankScorer.evaluate]
    B --> C[Keyword Dimension Scoring
    Technical/Experience/Behavioral/Career]
    C --> D[Role-Level Detection
    +15/+10/+5 bonus]
    D --> E[Sector Alignment
    +5 FinTech/FinServ]
    E --> F[Location Scoring
    +5 remote/Pittsburgh]
    F --> G[Calibration Caps
    Reddit≤75, etc.]
    G --> H[Deal-Breaker Penalties
    -30/-25/-20]
    H --> I[Verdict Band
    Strong/Good/Moderate/Weak/Poor]
    I --> J[RankResult]
    K[ScoringWeights] --> B
    L[backend/service] --> B
    M[RankCommand CLI] --> B
    N[/rank AI Command] --> M
```

### Risks

- **Over-scoring:** Adding FitEvaluator's adjustments on top of keyword scoring could push scores too high. Mitigation: keyword dimensions remain the primary signal (weighted 0-100), adjustments are bounded (±15 max per category, total score clamped to 0-100).
- **Calibration staleness:** Company-specific calibration rules will become outdated. Mitigation: encode them as a configurable map with a comment pointing to `04-job-evaluation.md` as the source of truth.
- **Backend migration risk:** Moving `backend/src/` to `backend/service/src/` could break Cloud Run deployment. Mitigation: `MainKt` class fully qualified name remains the same; Dockerfile entry point is unchanged.


## 3. Steps

### ✓ Step 1: Create `backend/job-post-ranking/` Module
Create the new pure Kotlin JVM library module with all ranking logic.

### ✓ Step 2: Move `backend/` Existing Code to `backend/service/`
Move existing backend code to service sub-module.

### ✓ Step 3: Rewrite `backend/settings.gradle.kts`
Include `:service` and `:job-post-ranking` sub-modules.

### ✓ Step 4: Add Backend Convention Plugin
New convention plugin in `gradle/tools/project-plugins/`.

### ✓ Step 5: DELETE FitEvaluator
Delete `FitEvaluator.kt` and `FitEvaluatorTest.kt` from `records/parser/`.

### ✓ Step 6: Eradicate Manual Fit Editing
Remove `UpdateFitScoreDialog`, `UpdateFitScoreUseCase`, and related code.

### ✓ Step 7: Update Proto & Schema
Add `ScoringWeights` message to proto and schema files.

### ✓ Step 8: Update `/rank` AI Command
Use deterministic `RankScorer.evaluate()` via CLI.

### ✓ Step 9: Update CLI
`RankCommand.kt` depends on `job-post-ranking`.

---

## 4. Proposed Changes & Implementation Steps

### 3.1. Create `backend/job-post-ranking/` Module

New `backend/job-post-ranking/` as a pure Kotlin JVM library (no Ktor, no Firebase). Contains:
- `RankScorer.kt` — moved from `records/parser/`, enhanced with FitEvaluator rules and dynamic `ScoringWeights`
- `ScoringWeights.kt` — `data class` for dimension weights and multiplier toggles
- `CalibrationRule.kt` — company-specific calibration (caps, overrides)
- `RankResult.kt` — enhanced with `location_verdict` field
- `JobInfo.kt` — extracted job metadata
- `RankScorerTest.kt` — migrated + new tests
- `ScoringWeightsTest.kt` — weight validation tests

### 3.2. Move `backend/` Existing Code to `backend/service/`

- Move `backend/src/` → `backend/service/src/`
- Move `backend/build.gradle.kts` → `backend/service/build.gradle.kts`
- Package stays `dev.fnc314.tools.jobtracker.backend`
- Add `implementation(projects.jobPostRanking)` to `service/build.gradle.kts`

### 3.3. Rewrite `backend/settings.gradle.kts`

Include `:service` and `:job-post-ranking` sub-modules plus `includeBuild("../records")`.

### 3.4. Add Backend Convention Plugin

New `BackendServiceConventionPlugin.kt` in `gradle/tools/project-plugins/` — applies Ktor, Firebase, serialization, JVM 21 toolchain. Registered in `project-plugins/build.gradle.kts`.

### 3.5. DELETE FitEvaluator

Delete `FitEvaluator.kt` and `FitEvaluatorTest.kt` from `records/parser/`. All rules absorbed into `job-post-ranking/RankScorer.kt`.

### 3.6. Eradicate Manual Fit Editing

**Remove:** `UpdateFitScoreDialog`, `UpdateFitScoreUseCase`, `handleUpdateFitScore`, `UpdateFitScore` intent, fit score button, related string resources.

**Replace with:** `AdjustScoringWeightsDialog` (sliders for dimension weights), `UpdateScoringWeightsUseCase` (persists to Firestore `metadata`), `AdjustScoringWeights` intent in ViewModel.

### 3.7. Update Proto & Schema

Add `ScoringWeights` message to `fit_evaluation.proto`, add `ScoringWeights weights = 4` to `TrackerFit`, update `tracker.schema.json` and `TrackerFit.kt`.

### 3.8. Update `/rank` AI Command

Use deterministic `RankScorer.evaluate()` via CLI. Remove AI scoring rubric. Keep aggregation/ranking/state steps.

### 3.9. Update CLI

`RankCommand.kt` depends on `job-post-ranking`. Add `--weights` option for custom `ScoringWeights` as JSON.

## 4. Verification Plan & Testing Checklist

### Key Scenarios

**RankScorerTest.kt — in `job-post-ranking/`:**
1. Role-level detection: principal/staff+Android +15, senior+Android +10, mobile +5
2. Sector alignment: FinTech/FinServ +5
3. Location scoring: remote/Pittsburgh +5, NYC/DC neutral
4. Calibration: Reddit cap 75, low-conversion cap 75, Capital One override 95
5. Deal-breaker penalties: hybrid-only -30, consulting -25, people management -20
6. Dynamic weights: custom weights produce different scores than DEFAULT
7. Score bounds: clamped to 0-100 after all adjustments
8. Empty text with structured fields → baseline from role/sector/location
9. All deal-breakers → score ≥ 0
10. Calibration override vs cap → override wins

**ScoringWeightsTest.kt:**
1. DEFAULT weights sum to 1.0
2. Invalid weights (sum ≠ 1.0) → validation error
3. Serialization round-trip

### Regression Checks
- `records:test` still passes (FitEvaluator files are gone, no broken imports)
- `backend/service` builds and tests pass
- `frontend` builds without `UpdateFitScoreUseCase` references
- `RankCommand` CLI still works

### Edge Cases
- Weight sum validation: weights must sum to 1.0 (or normalize automatically)
- Zero-weight dimension: allowed, contributes 0 to overall score
- Negative weights: rejected by validation

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-12/185800_deterministic-fit-score-unification.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Craft a single commit of all changed files
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Push `git push origin main`

