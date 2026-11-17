# Migration Plan: RM-001 Final Sprint (Python Eradication & Kotlin Conversion)

**Status:** COMPLETED
**Date:** 2026-11-16
**Target:** Finalize RM-001, migrate/archive remaining Python scripts, convert `.config/mise/tasks` to POSIX shell / Kotlin, and update `ROADMAP.md` to 100% DONE.
**Depends On:** `.agents/plans/2026-11-16/200053_rm-001-domain-scripts-and-lead-plan.md`

---

## 1. Context & Motivation
RM-001 represents the repository-wide migration from legacy Python scripts to Kotlin Multiplatform and CLI tools. As of commit `4cd68155` and `a959e8a2`, RM-001 stands at 90% completion. The goal of this final sprint plan is to systematically migrate, wrap, or archive the remaining 24 Python scripts and 31 Python mise tasks, ultimately achieving 100% completion on RM-001.

## 2. Inventory of Remaining Python Surfaces

### A. Subsystems & Tasks
1. **`tracker/` subcommands:**
   - `tracker:edit` → already implemented in Kotlin `EditCommand.kt`. Convert `.config/mise/tasks/tracker/edit.py` → `edit.sh`.
   - `tracker:proposals` → already implemented in Kotlin `ProposalCommand.kt`. Convert `.config/mise/tasks/tracker/proposals.py` → `proposals.sh`.
   - `tracker:upsert` & `tracker:bulk` → Implement `UpsertCommand.kt` and `BulkCommand.kt` in `frontend/apps/cli/commands/`, convert tasks to `.sh`.
   - `tracker:labels` → Implement `LabelsCommand.kt` in `frontend/apps/cli/commands/`, convert task to `.sh`.
   - `tracker:mock-clear` & `tracker:sync-due` → Implement `MockClearCommand.kt` and `SyncDueCommand.kt`, convert tasks to `.sh`.
   - `tracker:sheet` & `tracker:import` → Archive legacy one-shot / auxiliary scripts (`sheets.py`, `importer.py` in scripts/), convert task to shell wrapper or archive.

2. **`email/` tasks (`collect`, `discover`, `read-inbox`, `sync-labels`):**
   - Orchestrated via Composio CLI and Kotlin backend ingestion service.
   - Convert tasks to `.sh` wrappers invoking Composio CLI / `:backend:email-ingestion` or `:frontend:apps-cli`.
   - Archive `email_fetch.py` and `discover.py`.

3. **`artifacts/` tasks (`sync`, `add`, `backfill`, `migrate`, `purge-legacy`):**
   - Implemented via Kotlin GCS utilities or backend artifact service.
   - Archive `scripts/artifacts.py` and `scripts/sync_local_artifacts.py`.

4. **`admin/` & `dev/` tasks:**
   - `roadmap:status` → Convert `.config/mise/tasks/roadmap/status.py` to Kotlin CLI subcommand `roadmap status` or lightweight POSIX shell/awk script, convert to `.sh`.
   - `dev:guards` & `dev:verify-pdf` → Integrate into Gradle/Konsist/POSIX shell tasks.
   - Archive remaining root scripts: `security_guards.py`, `check_framework_version.py`, `check_upstream_updates.py`, `setup.py`, `remote_backup.py`, `remote_restore.py`, `build_docs_site.py`, `lint_skills.py`, `convert_salary_excel.py`, `generate_report.py`, `mock_data.py`.
   - Archive remaining core files: `scripts/contract.py`, `scripts/client.py`, `scripts/reader.py`, `scripts/tasks.py`, `scripts/tracker.py`, `scripts/cache.py`, `scripts/scrape.py`, `scripts/apply.py`.

## 3. Execution Slices

### Slice 1: Tracker Task Wrapping & Porting (Edit, Proposals, Upsert, Labels)
- Convert `tracker/edit.py` and `tracker/proposals.py` to `.sh` wrappers.
- Implement Kotlin CLI subcommands for `labels`, `upsert`, and `bulk` if needed, and convert mise tasks to `.sh`.
- Test and verify with `:frontend:apps-cli:run`.

### Slice 2: Roadmap Status & Shell Migration
- Port `roadmap:status` to native Kotlin subcommand `RoadmapStatusCommand` in `frontend/apps/cli/commands/` or `.sh`.
- Convert `roadmap/status.py` to `status.sh`.
- Verify `mise run roadmap:status`.

### Slice 3: Archive Deprecated Python Scripts & Clean scripts/
- Relocate remaining Python scripts from `scripts/` to `archive/scripts/`.
- Convert remaining Python mise tasks in `admin/`, `artifacts/`, `dev/`, `email/`, `search/` to shell wrappers.
- Verify `find scripts -name "*.py"` is empty or contains zero runnable entrypoints.

### Slice 4: Update Documentation, Completions, and Close RM-001
- Update `ROADMAP.md` setting RM-001 status to `DONE` with 100% completion.
- Re-run `mise run docs:tasks` and `mise run completions:generate`.
- Run `mise run dev:check` and verify all tests pass.
- Commit all changes following protocol.
