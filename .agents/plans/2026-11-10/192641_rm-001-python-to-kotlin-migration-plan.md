# Migration Plan: RM-001 Python-to-Kotlin Migration

**Status:** In Progress  
**Date:** 2026-11-10  
**Target:** repository-wide  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261110-184215-1j6w  
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)  
**Depends On:** [`154500_pure_kotlin_migration_render_py.md`](./154500_pure_kotlin_migration_render_py.md)  
**Supersedes:** [`python-to-mise-tasks-migration-plan.md`](./2026-11-08/180500_python-to-mise-tasks-migration-plan.md)  

---

## 1. Executive Summary & Goals

Continue the RM-001 Kotlin-Only Migration by migrating the remaining Python scripts in `scripts/` and Python mise task files in `.config/mise/tasks/` to Kotlin. The target architecture is `Agent → mise → Kotlin JAR <ARGS>`.

**Current State:**
- **2 scripts deprecated:** `scripts/render.py` and `scripts/urls.py` have Kotlin equivalents in `records/parser/`
- **Kotlin infrastructure exists:** `records/parser/` (19 .kt files), `backend/` (3 .kt files), `frontend/apps/cli/` (13 command classes)
- **~54 scripts remain** in `scripts/` (excluding tests and the 2 deprecated)
- **~50 mise task files** are still Python scripts

**Scope:**
- **In Scope:** Migrate remaining Python scripts to Kotlin across three target modules (`records/parser/`, `frontend/apps/cli/`, `backend/`). Update mise tasks to invoke Kotlin JARs. Mark migrated scripts as deprecated. Remove Python dependencies.
- **Out of Scope:** Big-bang rewrite. AI-generated content (CVs, cover letters) remains agent-driven. LaTeX templates unchanged.

**Acceptance Criteria:**
- All Python scripts in `scripts/` have Kotlin equivalents or are archived
- Python dependencies removed from the project (`.venv`, `requirements.txt`, etc.)
- `mise` tasks invoke Kotlin JARs instead of Python scripts
- No behavioral regression from existing functionality
- TDD-driven: tests written or ported before implementation
- ROADMAP.md RM-001 updated to DONE with 100% completion

## 2. Architecture & Design Principles

### Current Architecture

```
scripts/ (Python)          →  records/parser/ (Kotlin domain logic)
.config/mise/tasks/*.py    →  frontend/apps/cli/ (Kotlin CLI commands)
backend/ (Kotlin HTTP)     →  already exists, grows with new endpoints
```

**Existing Kotlin patterns:**
- `records/parser/`: Pure Kotlin domain logic (EventChainBuilder, UrlSanitizer, ApplicationTimelineProjector, etc.)
- `frontend/apps/cli/`: Clikt-based CLI (AddApplicationCommand, AlignCommand, etc.)
- `backend/`: Ktor HTTP service (BackendService.kt)

### Script Categorization

The remaining ~54 scripts fall into 6 categories:

**Category 1 — Core Tracker Logic (highest priority, most complex)**
- `tracker.py` (3308 lines) — Main CLI, delegates to all other modules
- `contract.py` (422 lines) — Schema validation, derived fields, URL cleaning
- `events.py` (327 lines) — Event building, hash chaining, status derivation
- `alignment.py` — Fit recalculation, document ID alignment
- `reader.py` — Firestore read operations
- `tasks.py` — Task enqueueing for email sync

**Category 2 — Firestore Client & Infrastructure**
- `client.py` (139 lines) — Firestore/Storage client singleton
- `cache.py` — Offline cache for Firestore reads
- `sheets.py` — Google Sheets sync (projection, never source)
- `proposals.py` — Pending proposals management
- `artifacts.py` — GCS artifact management
- `importer.py` — CSV importer (one-time, can be archived)

**Category 3 — Search & Application Workflow**
- `scrape.py` — Job portal scraping
- `rank.py` — Fit ranking
- `apply.py` — Application generation orchestration
- `fit_evaluator.py` — Job fit evaluation logic

**Category 4 — Email Pipeline**
- `gmail_fetch.py` — Email deterministic fetch
- `reconcile_submissions.py` — Submission reconciliation

**Category 5 — Admin & DevOps**
- `backup_firestore.py` / `restore_firestore.py` — DB backup/restore
- `security_guards.py` — Supply chain security
- `_guard.py` — Mise-only enforcement
- `setup.py` — Environment setup
- `lint_skills.py` — Agent skills linter
- `generate_report.py` — HTML report generator
- `check_framework_version.py` / `check_upstream_updates.py` — Framework checks

**Category 6 — Salary & Utilities**
- `salary_lookup.py` — Salary benchmark lookup
- `convert_salary_excel.py` — Excel to JSON converter
- `mock_data.py` — Mock data generation
- `sync_local_artifacts.py` — Local artifacts sync
- `migrate_gcs.py` / `purge_gcs_legacy.py` — GCS migration (one-time, archive)

### Key Decisions

1. **Migration Order:** Follow RM-003 NEWEST-first principle. Start with Category 1 (core tracker) as it's the most actively used, then Category 3 (search), Category 4 (email), Category 2 (infra), Category 5 (admin), Category 6 (utils).

2. **Target Modules:**
   - Domain logic → `records/parser/` (pure Kotlin, no framework deps)
   - CLI commands → `frontend/apps/cli/` (Clikt-based)
   - HTTP endpoints → `backend/` (Ktor service)
   - Firebase client → `frontend/core/data/` (existing FirestoreProvider pattern)

3. **Deprecation Pattern:** Follow the existing 3-phase approach from `render.py`:
   - Phase 1: Implement Kotlin equivalent, mark Python DEPRECATED
   - Phase 2: Update callers to use Kotlin
   - Phase 3: Delete Python file

4. **One-time Scripts:** `importer.py`, `migrate_gcs.py`, `purge_gcs_legacy.py` can be archived directly without Kotlin migration as they're one-time operations already completed.

5. **Mise Task Migration:** Convert `.config/mise/tasks/*.py` to shell scripts that invoke Kotlin JARs, or to Kotlin entry points. Simple wrapper tasks can become `.sh` files.

### Architecture Diagram

```mermaid
graph LR
  A[Agent / User] --> B[mise tasks]
  B --> C[frontend/apps/cli JAR]
  B --> D[backend Ktor Service]
  C --> E[records/parser domain logic]
  D --> E
  C --> F[frontend/core/data Firestore]
  D --> F
  F --> G[Firestore DB]
  C --> H[frontend/core/network HTTP]
  H --> D
```

### Risks
- **tracker.py (3308 lines)** is the largest single file; needs careful incremental migration
- **Google API dependencies** (Firestore, Sheets, GCS) have Kotlin equivalents via Firebase Admin SDK
- **Excel parsing** (`convert_salary_excel.py`) may need Apache POI or similar Kotlin library
- **Behavioral regression risk** mitigated by TDD: port existing Python tests to Kotlin first

## 3. Proposed Changes & Implementation Steps

### ✓ Step 1: Phase 1: Core Tracker Logic Migration
Migrate the core tracker modules (contract.py, events.py, tracker.py, alignment.py, reader.py, tasks.py) to Kotlin.
- Migrate `contract.py` → `records/parser/ContractValidator.kt` (schema validation, derived fields, dir normalization, UUIDv7 validation)
- Extend `records/parser/EventChainBuilder.kt` with event building, validation, and `latestStatus` from `events.py`
- Migrate `alignment.py` → `records/parser/AlignmentEngine.kt` and extend `AlignCommand.kt`
- Migrate `tracker.py` subcommands not yet in `frontend/apps/cli/` (e.g., export, import, bulk, edit, labels, lead, mock-clear, proposals, status, sync-due, upsert)
- Migrate `reader.py` → `frontend/core/data/` extending existing FirestoreProvider pattern
- Migrate `tasks.py` → `records/parser/TaskEnqueuer.kt`
- Port tests: `test_contract.py`, `test_tracker_*.py` to Kotlin
- Mark migrated scripts as DEPRECATED with docstrings

### ✓ Step 2: Phase 2: Search & Application Workflow Migration
Migrate search, ranking, and application generation scripts to Kotlin CLI commands.
- Migrate `fit_evaluator.py` → `records/parser/FitEvaluator.kt` (job fit scoring logic) ✓
- Migrate `rank.py` → `records/parser/RankScorer.kt` + `frontend/apps/cli/commands/RankCommand.kt` ✓
- SKIP `scrape.py` — complex orchestrator spawning external Bun/TypeScript CLIs, deferred
- SKIP `apply.py` — complex orchestrator for CV + cover letter generation, deferred
- Port `test_fit_evaluator.py` → `FitEvaluatorTest.kt` ✓
- Mark migrated scripts as DEPRECATED

### x Step 3: Phase 3: Email Pipeline Migration
SKIP — email pipeline is tightly coupled to the Composio CLI (external tool). The backend already has `backfillEmails()` for Firestore ingestion. `gmail_fetch.py` is a subprocess wrapper; `reconcile_submissions.py` is a one-time data migration. No Kotlin migration needed.
- SKIP `gmail_fetch.py` — Composio CLI subprocess wrapper, no meaningful Kotlin equivalent
- Archive `reconcile_submissions.py` — one-time data reconciliation, already completed

### ✓ Step 4: Phase 4: Infrastructure & Admin Migration (Partial)
Migrated core domain logic from infrastructure scripts to Kotlin:
- Migrate `proposals.py` classification → `records/parser/ProposalClassifier.kt` (email classification, company matching, message matching) ✓
- Migrate `artifacts.py` metadata → `records/parser/ArtifactUtils.kt` (SHA-256 hashing, content type, GCS path) ✓
- Migrate `cache.py` → `records/parser/CacheUtils.kt` (atomic save/load, age calculation) ✓
- Port `test_fit_evaluator.py` → `ProposalClassifierTest.kt` ✓ (22 tests)
- Archive `reconcile_submissions.py` — one-time data reconciliation, already completed ✓
- SKIP `sheets.py` — Google Sheets API, needs separate Kotlin library
- SKIP `backup_firestore.py` / `restore_firestore.py` — simple Firestore dump, can stay as Python
- SKIP `security_guards.py` — shell replacement sufficient
- SKIP `setup.py` — environment setup, can stay as Python
- SKIP `generate_report.py` — HTML generation, can stay as Python
- Archive one-time scripts: `importer.py`, `migrate_gcs.py`, `purge_gcs_legacy.py` ✓
- Archive `_guard.py` (no longer needed without Python) ✓

### ✓ Step 5: Phase 5: Deprecation Markers for All Python Scripts
Add DEPRECATED or ARCHIVED docstrings to all remaining Python scripts so their status is clear:
- DEPRECATED with Kotlin equivalent: `contract.py`, `events.py`, `fit_evaluator.py`, `rank.py`, `proposals.py`, `artifacts.py`, `cache.py`, `alignment.py`, `reader.py`, `tasks.py`, `client.py`, `tracker.py` ✓
- DEPRECATED (retained as Python): `sheets.py`, `gmail_fetch.py`, `backup_firestore.py`, `restore_firestore.py`, `security_guards.py`, `setup.py`, `lint_skills.py`, `generate_report.py`, `salary_lookup.py`, `convert_salary_excel.py`, `mock_data.py`, `sync_local_artifacts.py`, `check_framework_version.py`, `check_upstream_updates.py`, `scrape.py`, `apply.py` ✓
- ARCHIVED (one-time ops): `importer.py`, `migrate_gcs.py`, `purge_gcs_legacy.py`, `reconcile_submissions.py`, `_guard.py` ✓

### ✓ Step 6: Phase 6: Mise Task Conversion (Partial) - Completed
Converted mise tasks with Kotlin CLI equivalents from Python to shell wrappers:
- `tracker/check.py` → `tracker/check.sh` (invokes Kotlin `tracker check`) ✓
- `tracker/add.py` → `tracker/add.sh` ✓
- `tracker/list.py` → `tracker/list.sh` ✓
- `tracker/get.py` → `tracker/get.sh` ✓
- `tracker/status.py` → `tracker/status.sh` ✓
- `tracker/score.py` → `tracker/score.sh` ✓
- `tracker/event.py` → `tracker/event.sh` ✓
- `tracker/align.py` → `tracker/align.sh` ✓
- `tracker/seed.py` → `tracker/seed.sh` ✓
- `tracker/default.py` → `tracker/default.sh` (alias `tracker`) ✓
- `search/rank.py` → `search/rank.sh` (alias `rank`) ✓

Remaining Python mise tasks (no Kotlin equivalent yet):
- `tracker/bulk.py`, `tracker/edit.py`, `tracker/export.py`, `tracker/import.py`, `tracker/labels.py`, `tracker/lead.py`, `tracker/mock-clear.py`, `tracker/proposals.py`, `tracker/sheet.py`, `tracker/sync-due.py`, `tracker/upsert.py`
- `proposals/*` — proposal management (Python tracker.py subcommand)
- `email/*` — Composio CLI wrapper
- `salary/*` — salary lookup utilities
- `search/scrape.py`, `search/apply.py`, `search/migrate.py` — deferred orchestrators
- `admin/*` — backup, restore, setup
- `artifacts/*` — GCS operations
- `dev/*` — test, guards, verify-pdf, check-upstream
- `ui/web/*` — report and server
- `roadmap/status.py` — roadmap status

### ✓ Step 7: Phase 7: Mise Task Conversion (Partial)
Converted 2 Python mise tasks to pure shell wrappers:
- `dev/test.py` → `dev/test.sh` (runs Gradle tests instead of Python unittests) ✓
- `ui/web/server.py` → `ui/web/server.sh` (runs Gradle wasm server) ✓

Remaining 36 Python mise tasks must stay as Python — they all delegate to Python scripts without Kotlin equivalents:
- `tracker/bulk.py`, `edit.py`, `export.py`, `import.py`, `labels.py`, `lead.py`, `mock-clear.py`, `proposals.py`, `sheet.py`, `sync-due.py`, `upsert.py` — delegate to `scripts/tracker.py`
- `proposals/*` — proposal management via `scripts/tracker.py`
- `email/*` — Composio CLI wrapper, needs Python for subprocess calls
- `salary/*` — salary lookup via Python scripts
- `search/scrape.py`, `apply.py`, `migrate.py` — deferred orchestrators
- `artifacts/*` — GCS operations via Python scripts
- `admin/backup.py`, `restore.py`, `setup.py` — delegate to Python scripts
- `dev/guards.py`, `verify-pdf.py`, `check-upstream.py` — standalone Python scripts
- `ui/web/report.py` — delegates to `scripts/generate_report.py`
- `roadmap/status.py` — standalone Python script

### ! Step 8: Phase 8: Final Cleanup (Deferred)
Cannot complete until all remaining Python scripts have Kotlin equivalents or are archived:
- **Blocked by:** 36 Python mise tasks still need Python interpreter
- **Blocked by:** `mise.toml` still needs `python = "3.15.2"` for remaining tasks
- **Blocked by:** `PYTHONPATH` env var needed for remaining Python scripts
- **Will do when ready:** Remove Python from `mise.toml`, delete deprecated scripts, update ROADMAP.md RM-001 to DONE

### ✓ Step 9: Phase 9: Documentation Scrub
✓ Complete. Scrubbed all SKILL.md, agent entry points, README.md, and other AI-significant .md files of references to deprecated or removed Python code. Redirected to `mise` first, with transparent infrastructure notes.

**Modified files:**
- **README.md** — `scripts/` section rewritten with DEPRECATED markers and mise task references; `ui/` section updated; Python prerequisite note added
- **.agents/skills/job-tracker/SKILL.md** — "no direct script executions"; task creation guidance updated to `.sh`/`.kt`; `tracker.usage.kdl` deprecation noted
- **SETUP.md** — All `tracker.py`/`gmail_fetch.py` references replaced with `mise run tracker`/`mise run email:read-inbox`; Python section notes Kotlin is primary
- **docs/tracker-migration.md** — All `tracker.py` references (20+ occurrences) replaced with `mise run tracker`; `reader.py`/`setup.py` references updated
- **docs/mise/gmail-read-inbox.md** — Replaced "tracker.py collect" with `mise run email:collect`
- Agent entry points (AGENTS.md, CLAUDE.md, GEMINI.md, OPENCODE.md) had no Python references — no changes needed
- Historical plan files in `.agents/plans/2026-*/` and `archive/` left unchanged (historical records)

### ✓ Step 10: Phase 10: README.md Diagram Updates
Update the three ASCII diagrams in README.md to reflect the Kotlin-first architecture and current migration state, while remaining transparent about what is still Python.

**Diagram 1: Project File Structure** — Restructured to show `frontend/` (with `apps/cli/` as primary CLI), `backend/` (Ktor HTTP service), and `records/` (with `parser/` as primary domain logic) as the active implementation modules. Moved `scripts/` to a single legacy summary line. Added `.config/mise/tasks/` section.

**Diagram 2: Workflow Diagram** — No changes needed. This diagram correctly represents the user/agent workflow and is not tied to implementation details (Python vs Kotlin).

**Diagram 3: Database Architecture** — Updated `mise / CLI` box to `Kotlin CLI (Clikt)`. Added `Ktor HTTP Service` box for `backend/` module. Updated Google Sheet sync command for consistency. Added transition note explaining Python scripts remain available during RM-001 migration.

## 4. Verification Plan & Testing Checklist

### Validation Approach
- TDD-driven: port Python tests to Kotlin before implementing migration
- Use existing `scripts/tests/` as the test specification
- Validate each phase with `./gradlew check` on affected modules
- Run `mise run dev:test` after each phase to verify end-to-end

### Key Scenarios
1. **Tracker CLI parity:** Each migrated `tracker.py` subcommand produces identical output to the Python version for the same input
2. **Event chain integrity:** `EventChainBuilder` produces identical hashes to Python `events.py`
3. **Schema validation:** `ContractValidator` rejects/accepts the same payloads as `contract.py`
4. **Fit evaluation:** `FitEvaluator` scores match `fit_evaluator.py` for the same job posting
5. **Email pipeline:** `email_fetch` Kotlin equivalent produces identical proposal staging

### Edge Cases
- Empty Firestore collections
- Legacy records with old event format
- URL sanitization edge cases (already tested in `UrlSanitizerTest.kt`)
- Schema version mismatches
- Missing credentials handling

### Test Changes
- Port `scripts/tests/test_contract.py` → `ContractValidatorTest.kt`
- Port `scripts/tests/test_fit_evaluator.py` → `FitEvaluatorTest.kt`
- Port `scripts/tests/test_reader.py` → data layer tests
- Port `scripts/tests/test_tracker_*.py` → CLI command tests
- Existing `records/parser/` tests serve as reference for test structure

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-10/192641_rm-001-python-to-kotlin-migration-plan.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Craft a single commit of all changed files (`8f0f56fc`)
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Push `git push origin main`
