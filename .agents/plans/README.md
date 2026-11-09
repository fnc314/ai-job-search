# Repository Architectural Plans

This directory holds architecture, system design, and migration plans for the repository.

### Subsystem Plan Directories
- **Kotlin Multiplatform UI Workspace:** [`ui/kmp/.agents/plans/`](../../ui/kmp/.agents/plans/README.md) houses all KMP UI, MVI, design system, and multiplatform feature architecture plans.
- **Records Subsystem Workspace:** [`records/.agents/plans/`](../../records/.agents/plans/README.md) houses all protobuf contract, parsing, and interop plans specific to `records`.

---

## Plan Invariants (MANDATORY)
Every architecture plan created by any agent must strictly adhere to these paradigms:
1. **Target-Derived Location**:
   - Multi-module, cross-subsystem, or repository-wide plans -> `.agents/plans/`
   - Subsystem plans -> `<subsystem>/.agents/plans/` (e.g. `ui/kmp/.agents/plans/`, `records/.agents/plans/`)
2. **Date Subdirectory**: Plans must be saved in a `YYYY-MM-DD/` folder matching the creation date.
3. **24-Hour Timestamp Prefix**: Filenames must be formatted as `HHMMSS_<plan-name>.md` using a 24-hour clock (`00`-`23` for `HH`).
4. **Registry Synchronization**: Register the plan in the table below with its relative path (`./YYYY-MM-DD/HHMMSS_<plan-name>.md`), status, and scope.
5. **Mandatory Opening Frontmatter**: Start every plan from [`TEMPLATE.md`](./TEMPLATE.md) (`Status`, `Date`, `Target` block); `Status` must match the registry row. The template is shared by `records/` and `ui/kmp/` plan directories.

---

### Repository-Wide Plans

| Plan | Status | Scope / Focus Area |
| :--- | :--- | :--- |
| [`125800_kotlin_cloud_run_remote_tasks_and_tui_plan.md`](./2026-11-09/125800_kotlin_cloud_run_remote_tasks_and_tui_plan.md) | Completed | Architecture & implementation plan for Kotlin Cloud Run service, shared proto3 models, remote task offloading, and Kotlin TUI migration |
| [`python-to-mise-tasks-migration-plan.md`](./2026-11-08/180500_python-to-mise-tasks-migration-plan.md) | Completed | Migrate away from direct Python invocations to file-based Mise tasks with environment/virtualenv auto-binding |
| [`tui-triage-and-fix.md`](./2026-11-06/052500_tui-triage-and-fix.md) | Completed | TUI screen flash and navigation fixes, Firestore erroneously withdrawn applications remediation |
| [`mise-task-changes.md`](./2026-11-05/230500_mise-task-changes.md) | Completed | Inverted `mise scrape` defaults (markdown output, save-seen), added `mise apply` and `mise rank` tasks |
| [`migration-execution-and-rollback-plan.md`](./2026-11-05/224500_migration-execution-and-rollback-plan.md) | Completed | Execution and rollback procedure for live Firestore migration to Enum.name matches (Schema v8) |
| [`recorded-values-to-enum-name-migration-plan.md`](./2026-11-05/220500_recorded-values-to-enum-name-migration-plan.md) | Completed | Strategy and roadmap for migrating recorded values to exact Enum.name matches (Schema v8) |
| [`interactive-proposal-approve.md`](./2026-11-05/223300_interactive-proposal-approve.md) | Completed | Curses TUI proposals review mode replacing readline-based interactive loop |
| [`firestore-scraped-postings-collection.md`](./2026-11-05/215800_firestore-scraped-postings-collection.md) | Completed | Firestore collection for scraped postings, replacing static seen_jobs.json |
| [`commit-reconciliation-and-frontmatter-completion-plan.md`](./2026-11-05/185500_commit-reconciliation-and-frontmatter-completion-plan.md) | Completed | Split bundled commit into per-plan commits, finish frontmatter templating, remove draft plan, and log commit SHAs |
| [`plan-frontmatter-standardization-and-templating.md`](./2026-11-05/183738_plan-frontmatter-standardization-and-templating.md) | Completed | Universal Markdown plan frontmatter standardization, `TEMPLATE.md`, and tracker migration plan relocation |
| [`mark-detekt-and-konsist-plan-completed.md`](./2026-11-05/181232_mark-detekt-and-konsist-plan-completed.md) | Completed | In-repo plan relocation, registry status synchronization, and bookkeeping enforcement |
| [`canonical-enum-mapping-and-wire-contracts-plan.md`](./2026-11-05/175500_canonical-enum-mapping-and-wire-contracts-plan.md) | Completed | Two-tier canonical enum mapping, self-contained wire contracts, O(1) lookups, and Room converter modernization |
| [`dokka-and-comprehensive-kdocs-plan.md`](./2026-11-05/163042_dokka-and-comprehensive-kdocs-plan.md) | Completed | Dokka 2.3.0 unified multi-project setup & exhaustive KDocs across records & ui/kmp |
| [`plan-relocation-and-index-alignment.md`](./2026-11-05/173850_plan-relocation-and-index-alignment.md) | Completed | Relocation of cross-subsystem plans to .agents/plans, timestamp hierarchy & registry synchronization |
| [`detekt-and-konsist-integration-plan.md`](./2026-11-05/172809_detekt-and-konsist-integration-plan.md) | Completed | Integration of Detekt 1.23.8 & Konsist 0.18.1 across `records` & `ui/kmp` |
| [`plan-files-consolidation-and-gitignore-alignment.md`](./2026-11-05/164709_plan-files-consolidation-and-gitignore-alignment.md) | Completed | Plan files reorganization, duplicate cleanup, and universal `.gitignore` alignment |
| [`add-job-url-fields-to-tui-and-cli.md`](./2026-11-04/020823_add-job-url-fields-to-tui-and-cli.md) | Completed | Job URL fields in tracker schema, Curses TUI, and Python CLI tools |
| [`recruiter-outreach-tracking.md`](./2026-11-02/172651_recruiter-outreach-tracking.md) | Completed | Recruiter outreach data models, CLI subcommands, and pipeline tracking |
| [`kmp-proto-contracts.md`](./2026-11-02/061702_kmp-proto-contracts.md) | Completed | Top-level repo restructuring, Protobuf type contracts, and shared parsing module |
| [`tui-log-rotation-and-cleanup.md`](./2026-11-01/174503_tui-log-rotation-and-cleanup.md) | Completed | Curses TUI session log rotation, retention limit, and disk cleanup |
| [`firestore-data-optimization.md`](./2026-10-31/135442_firestore-data-optimization.md) | Completed | Firestore schema optimization, projection queries, and index alignment |
| [`tracker-migration-plan.md`](./2026-10-31/021052_tracker-migration-plan.md) | Completed | 10-phase authoritative migration from CSV tracker to Firestore store |
| [`hunter-logo-integration.md`](./2026-11-08/171648_hunter-logo-integration.md) | Completed | Hunter.io Logo API integration: Company logo metadata in Firestore, `tracker logo-fetch` CLI, KMP & TUI logo wiring |
| [`fix-email-collect-contract-error.md`](./2026-11-08/214000_fix-email-collect-contract-error.md) | Completed | Align namespaced enum values in `proposals.py` and `tracker.py` with canonical schema enum names |
| [`fix-event-type-and-email-pipeline-docs.md`](./2026-11-08/215200_fix-event-type-and-email-pipeline-docs.md) | Completed | Fix `event:*` prefixed event type strings in `buildEvent()` calls and create email-to-Firebase pipeline documentation |
| [`ai-job-search-plan.md`](../../ai-job-search-plan.md) | Canonical | Universal multi-agent architecture and single source of truth specifications |
