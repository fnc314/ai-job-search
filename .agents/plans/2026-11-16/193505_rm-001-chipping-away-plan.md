# Implementation Plan: RM-001 Kotlin-Only Migration — Chipping Away at Remaining Scripts

**Status:** Completed  
**Date:** 2026-11-16  
**Target:** repository-wide (`frontend/apps/cli/`, `records/parser/`, `.config/mise/tasks/`, `scripts/`, `ROADMAP.md`)  
**Harness:** Antigravity  
**Model:** gemini-3.8-flash  
**Session:** 7093357f-5e5a-45e0-9093-a854e5ab458a  
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)  
**Depends On:** [`192641_rm-001-python-to-kotlin-migration-plan.md`](../2026-11-10/192641_rm-001-python-to-kotlin-migration-plan.md)  
**Related Items:** `RM-001` (Kotlin-Only Migration), `RM-003` (Backend Functionality Abstraction)  

---

## 1. Executive Summary & Goals

`RM-001` aims to eradicate all Python code from the repository, standardizing on a clean, strongly-typed Kotlin architecture:
$$\text{Agent / User} \longrightarrow \text{mise task} \longrightarrow \text{Kotlin CLI binary / JAR} \longrightarrow \text{Domain / Services}$$

With `RM-003` complete (which abstracted deterministic tasks like ranking, event chaining, LaTeX compilation, and portal scraping into modular `@backend` services), we can now systematically chip away at the remaining **41 Python scripts** in `scripts/` and the **38 `.py` mise tasks** in `.config/mise/tasks/`.

Rather than an unmanageable all-at-once rewrite, this plan tackles the migration in structured, low-risk, high-impact bites:

1. **Bite 1 (Immediate - Archival of One-Time / Obsolete Scripts):**
   Archive one-off data migration, bucket relocation, and legacy import scripts that have already fulfilled their purpose into `archive/scripts/`, immediately retiring ~10 scripts without code churn.
2. **Bite 2 (Proposals & Application Tracking Subcommands in Kotlin CLI):**
   Expand `frontend/apps/cli` with Clikt subcommands for proposal inspection/approval (`proposals list`, `proposals approve`, `proposals reject`) and tracker lifecycle tasks (`tracker export`, `tracker bulk`, `tracker edit`), replacing corresponding `.config/mise/tasks/proposals/*.py` with `.sh` wrappers invoking `:frontend:apps-cli:run`.
3. **Bite 3 (Salary & Utilities in Kotlin):**
   Migrate `scripts/salary_lookup.py` to a Kotlin domain parser / CLI command reading `data/salary_benchmarks.json`.
4. **Bite 4 (Dev Tooling & Housekeeping):**
   Audit developer checks (`scripts/check_framework_version.py`, `scripts/lint_skills.py`), porting them to Konsist architecture tests or lightweight POSIX tasks.
5. **Bite 5 (Roadmap & Documentation Tracking):**
   Update `ROADMAP.md` progress to reflect completed slices and increment completion percentage towards 85%+.

> [!NOTE]
> As progress is made through each section, live status and completed items will be continuously updated in this document and reported in the terminal.

---

## 2. Inventory & Triage of Remaining Python Scripts (41 Total)

| Script | Status / Triage | Target Destination |
|---|---|---|
| **One-Time / Obsolete (Archive Immediately)** | | |
| `scripts/importer.py` | One-off CSV import | Move to `archive/scripts/importer.py` |
| `scripts/reconcile_submissions.py` | One-off submission reconciler | Move to `archive/scripts/reconcile_submissions.py` |
| `scripts/resume_migration.py` | One-off migration resume | Move to `archive/scripts/resume_migration.py` |
| `scripts/blob_migrate.py` | One-off GCS path relocation | Move to `archive/scripts/blob_migrate.py` |
| `scripts/blob_batch_relocate.py` | One-off GCS batch relocation | Move to `archive/scripts/blob_batch_relocate.py` |
| `scripts/blob_relocate.py` | One-off GCS relocation | Move to `archive/scripts/blob_relocate.py` |
| `scripts/blob_relocate_role_slugs.py` | One-off GCS slug restructure | Move to `archive/scripts/blob_relocate_role_slugs.py` |
| `scripts/blob_purge_legacy.py` | One-off legacy blob purger | Move to `archive/scripts/blob_purge_legacy.py` |
| `scripts/mock_data.py` | Mock data generator | Retain or move to test fixtures |
| **Already Implemented in Kotlin / Superseded by Backend** | | |
| `scripts/render.py` | Migrated to Kotlin `records/parser/` | Mark fully superseded / remove |
| `scripts/urls.py` | Migrated to Kotlin `UrlSanitizer.kt` | Mark fully superseded / remove |
| `scripts/events.py` | Migrated to `@backend:event-chain` | Mark fully superseded |
| `scripts/rank.py` | Migrated to `@backend:job-post-ranking` & `RankCommand.kt` | Mark fully superseded |
| `scripts/fit_evaluator.py` | Migrated to `FitEvaluator.kt` | Mark fully superseded |
| `scripts/alignment.py` | Migrated to `AlignmentEngine.kt` & `AlignCommand.kt` | Deprecated |
| **Proposals & Tracking CLI (Focus of Bite 2)** | | |
| `scripts/proposals.py` | Staging, listing, approving proposals | Port to `frontend/apps/cli/commands/ProposalCommand.kt` |
| `scripts/tracker.py` | Remaining subcommands (export, bulk, edit) | Add to `frontend/apps/cli/commands/` |
| **Utilities & Domain Services (Focus of Bite 3)** | | |
| `scripts/salary_lookup.py` | Lookup salary benchmarks | `records/parser/SalaryLookup.kt` + CLI command |
| `scripts/convert_salary_excel.py` | Convert xlsx to json | One-time / Kotlin utility |
| `scripts/artifacts.py` | Artifact hashing & GCS paths | `records/parser/ArtifactUtils.kt` |
| **Email & Orchestrators (External CLI / Agent Integrations)** | | |
| `scripts/email_fetch.py` | Fetch emails via Composio CLI | Subprocess wrapper (keep isolated until native Ktor client) |
| `scripts/scrape.py` | Portal scrapers orchestrator | Wrapped by `backend:job-post-scraping` |
| `scripts/apply.py` | LaTeX CV/letter generation | Agent workflow command |

---

## 3. Proposed Slices

### Slice A: Archival of One-Time Scripts (Bite 1) — **COMPLETED**
- Moved verified historical one-off scripts to `archive/scripts/`:
  - `importer.py`
  - `reconcile_submissions.py`
  - `resume_migration.py`
  - `blob_migrate.py`
  - `blob_batch_relocate.py`
  - `blob_relocate.py`
  - `blob_relocate_role_slugs.py`
  - `blob_purge_legacy.py`
- Updated task docs and completions via `docs:tasks` and `completions:generate`.
- Updated `ROADMAP.md` RM-001 completion to 75% and committed (`132968d2`).

### Slice B: Proposals Management in Kotlin CLI (Bite 2) — **COMPLETED**
- Created `ProposalCommand.kt` in `frontend/apps/cli/commands/` implementing subcommands: `list`, `show`, `approve`, `reject`, `expire`.
- Registered `ProposalCommand` in `JobSearchCli` and `TrackerCommand` entry points.
- Fixed `RemotePlatform.jvm.kt` fallback to target `dev-fnc314-ai-job-tracker`.
- Resolved Konsist Architecture Test rule on design system color tokens (`BrandPalette`).
- Converted all 5 proposal tasks in `.config/mise/tasks/proposals/` from `.py` to `.sh` wrappers.
- Verified `mise run proposals:list` and `mise run proposals:show <id>`.
- Regenerated documentation and shell completions.
- Committed changes (`bf5e1d04`).

### Slice C: Salary Benchmark Lookup in Kotlin (Bite 3) — **COMPLETED**
- Created `SalaryBenchmarkEngine.kt` in `records/parser/` providing `normalizeCompanyName()`, fuzzy search, and JSON serialization.
- Authored and verified unit tests in `SalaryBenchmarkEngineTest.kt` (100% pass).
- Created `SalaryCommand.kt` in `frontend/apps/cli/commands/` supporting `--city`, `--json`, `--list-all`, and `--validate`.
- Registered `SalaryCommand` in `JobSearchCli` and `TrackerCommand`.
- Converted `.config/mise/tasks/salary/lookup.py` to `.sh` wrapper.
- Verified `mise run salary --validate`.
- Regenerated documentation and shell completions.
- Updated `ROADMAP.md` RM-001 completion to 80% and committed (`078d35f4`).

### Slice D: Final Verification & Plan Alignment — **COMPLETED**
- Verified filename adheres strictly to `.agents/plans/TEMPLATE.md`: `.agents/plans/2026-11-16/193505_rm-001-chipping-away-plan.md` (lowercase kebab-case prefix with 24-hr timestamp).
- Verified frontmatter adheres strictly to TEMPLATE.md (Status, Date, Target, Harness, Model, Session, Schema Authority, Depends On, Related Items).
- Verified all quality gates pass: `mise run dev:guards`, `mise run dev:konsist`, and `mise run records:test`.
- Synced `ROADMAP.md` RM-001 progress to 80% completion.

---

## 5. Bookkeeping Checklist
- [x] Plan persisted at `.agents/plans/2026-11-16/193505_rm-001-chipping-away-plan.md` (kebab-case title)
- [x] Registry `.agents/plans/README.md` row added / status synchronized
- [x] Craft a single commit of all changed files
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Note SSH agent/passphrase constraints when pushing to remote origin (`git push origin main`)
1. **Add `ProposalCommand` to `frontend/apps/cli`**:
   - Subcommands:
     - `proposals list [--state <state>]`
     - `proposals show <id>`
     - `proposals approve <id> [--yes] [--note <text>]`
     - `proposals reject <id> [--yes] [--reason <text>]`
2. **Convert Mise Tasks in `.config/mise/tasks/proposals/`**:
   - `list.py` $\rightarrow$ `list.sh`
   - `show.py` $\rightarrow$ `show.sh`
   - `approve.py` $\rightarrow$ `approve.sh`
   - `reject.py` $\rightarrow$ `reject.sh`
3. **Register `ProposalCommand` in `JobSearchCli` (`frontend/apps/cli/src/main/kotlin/.../Main.kt`)**.

### Slice C: Salary Benchmark Lookup in Kotlin (Bite 3)
1. Implement `SalaryLookupService.kt` in `records/parser/` to parse and query `data/salary_benchmarks.json`.
2. Add `SalaryCommand` (`salary lookup <role> <location>`) to `frontend/apps/cli`.
3. Convert `.config/mise/tasks/salary/lookup.py` to `lookup.sh`.

---

## 4. Verification Plan

### Automated Tests
1. **CLI Tests**: Run `:frontend:apps-cli:test` to verify argument parsing and subcommand delegation.
2. **Domain Tests**: Run `:records:parser:test` and `:records:test` to verify proposal approval logic and salary lookup computations.
3. **Architecture & Naming Tests**: Run `mise run dev:konsist` and `mise run dev:guards` to guarantee zero interface-naming baseline regressions.

### Manual Verification
1. Run `mise run proposals:list` using the new Kotlin runner and confirm identical output format.
2. Run `mise run salary:lookup --role "Senior Android Engineer" --location "Pittsburgh, PA"` and verify output matches the Python behavior.
3. Verify `archive/scripts/` contains the retired one-off scripts and git status is clean.

---

## 5. Documentation Regeneration & Final Commit Protocol

> [!IMPORTANT]
> **Stage/Slice Change Invariant:**
> At each slice transition and stage change, all documentation must be regenerated:
> - `mise run docs:all` (or `mise run docs:tasks`, `mise run records:docs`, `mise run frontend:docs`, `mise run backend:docs`)
> - `mise run completions:generate`
>
> **Final Commit Protocol:**
> - Ensure all files are tracked, code is verified via tests/guards, and commit changes cleanly with descriptive conventional commit messages.
> - Note SSH agent/passphrase constraints when pushing to remote origin.

