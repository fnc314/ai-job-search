# Repository Architectural Plans

This directory holds architecture, system design, and migration plans for the repository.

### Subsystem Plan Directories
- **Kotlin Multiplatform UI Workspace:** [`ui/kmp/.agents/plans/`](../../ui/kmp/.agents/plans/README.md) houses all KMP UI, MVI, design system, and multiplatform feature architecture plans.

---

### Repository-Wide Plans

| Plan | Status | Scope / Focus Area |
| :--- | :--- | :--- |
| [`detekt-and-konsist-integration-plan.md`](./detekt-and-konsist-integration-plan.md) | In Review | Integration of Detekt 1.23.8 & Konsist 0.18.1 across `records` & `ui/kmp` |
| [`plan-files-consolidation-and-gitignore-alignment.md`](./plan-files-consolidation-and-gitignore-alignment.md) | Completed | Plan files reorganization, duplicate cleanup, and universal `.gitignore` alignment |
| [`kmp-proto-contracts.md`](./kmp-proto-contracts.md) | Completed | Top-level repo restructuring, Protobuf type contracts, and shared parsing module |
| [`firestore-data-optimization.md`](./firestore-data-optimization.md) | Completed | Firestore schema optimization, projection queries, and index alignment |
| [`recruiter-outreach-tracking.md`](./recruiter-outreach-tracking.md) | Completed | Recruiter outreach data models, CLI subcommands, and pipeline tracking |
| [`add-job-url-fields-to-tui-and-cli.md`](./add-job-url-fields-to-tui-and-cli.md) | Completed | Job URL fields in tracker schema, Curses TUI, and Python CLI tools |
| [`tui-log-rotation-and-cleanup.md`](./tui-log-rotation-and-cleanup.md) | Completed | Curses TUI session log rotation, retention limit, and disk cleanup |
| [`docs/tracker-migration-plan.md`](../../docs/tracker-migration-plan.md) | Completed | 10-phase authoritative migration from CSV tracker to Firestore store |
| [`ai-job-search-plan.md`](../../ai-job-search-plan.md) | Canonical | Universal multi-agent architecture and single source of truth specifications |
