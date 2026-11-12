# Repository Architectural Plans

This directory holds architecture, system design, and migration plans for the repository.

### Subsystem Plan Directories
- **Frontend Multiplatform Workspace:** [`frontend/.agents/plans/`](../../frontend/.agents/plans/README.md) houses all KMP UI, MVI, design system, and multiplatform feature architecture plans.
- **Records Subsystem Workspace:** [`records/.agents/plans/`](../../records/.agents/plans/README.md) houses all protobuf contract, parsing, and interop plans specific to `records`.

---

## Plan Invariants (MANDATORY)
Every architecture plan created by any agent must strictly adhere to these paradigms:
1. **Target-Derived Location**:
   - Multi-module, cross-subsystem, or repository-wide plans -> `.agents/plans/`
   - Subsystem plans -> `<subsystem>/.agents/plans/` (e.g. `frontend/.agents/plans/`, `records/.agents/plans/`)
2. **Date Subdirectory**: Plans must be saved in a `YYYY-MM-DD/` folder matching the creation date.
3. **24-Hour Timestamp Prefix**: Filenames must be formatted as `HHMMSS_<plan-name>.md` using a 24-hour clock (`00`-`23` for `HH`).
4. **Registry Synchronization**: Register the plan in the table below with its relative path (`./YYYY-MM-DD/HHMMSS_<plan-name>.md`), status, and scope.
5. **Mandatory Opening Frontmatter**: Start every plan from [`TEMPLATE.md`](./TEMPLATE.md) (`Status`, `Date`, `Target` block); `Status` must match the registry row. The template is shared by `records/` and `frontend/` plan directories.

---

### Repository-Wide Plans

| Plan | Status | Scope / Focus Area |
| :--- | :--- | :--- |
| [`023100_fix-company-logo-rendering-compose-ui.md`](./2026-11-12/023100_fix-company-logo-rendering-compose-ui.md) | Completed | Fix inconsistent company logo rendering by removing aspect ratio check, deleting fallback URL generation, and adding diagnostic logging in Compose UI |
| [`021900_hunter-io-company-enrichment-migration.md`](./2026-11-12/021900_hunter-io-company-enrichment-migration.md) | Completed | Migrate company enrichment to Hunter.io Company API (`/v2/companies/find`), extract 20+ metadata fields, credit-aware batching, schema v12, and deprecate hardcoded LinkedIn slugs |
| [`225300_company-slug-deduplication-migration.md`](./2026-11-11/225300_company-slug-deduplication-migration.md) | Completed | Complete migration template: Firestore document ID renaming, GCS blob relocation, local folder migration, email reference updates — demonstrated with `ford-motor` → `ford` |
| [`220303_rm-009-unify-top-level-card-and-company-dedup.md`](./2026-11-11/220303_rm-009-unify-top-level-card-and-company-dedup.md) | Completed | RM-009: Unified Grid-based TopLevelCard, CompanyLogoImage size/loading/HTTP-error unification, and canonical company slug deduplication across records, backend, frontend, and scripts |
| [`183900_proposal_inspect_table_and_interview_state_machine.md`](./2026-11-11/183900_proposal_inspect_table_and_interview_state_machine.md) | Completed | Status enum lifecycle reordering, interview rounds Proto3 message & sealed hierarchy, and inline proposal inspect table |
| [`190100_markdown_rendering_in_artifacts_preview.md`](./2026-11-10/190100_markdown_rendering_in_artifacts_preview.md) | Completed | Replace plain text display in MarkdownPreviewDialog with proper markdown rendering using `mikepenz/multiplatform-markdown-renderer` (M3) |
| [`173600_unify_notes_and_events_timeline.md`](./2026-11-10/173600_unify_notes_and_events_timeline.md) | Completed | Single source of truth for application activity, elimination of duplicate NOTES section, subcollection event querying, and interview scheduling (`scheduledAt`) |
| [`172200_interactive_artifacts_presentation_and_in_app_viewer_plan.md`](./2026-11-10/172200_interactive_artifacts_presentation_and_in_app_viewer_plan.md) | Completed | Interactive Artifact Presentation in KMC frontend/apps: query Firestore artifacts, launch dedicated apps (PDF, URL, external), in-app text/markdown viewer setting & preview modal, local artifact sync, **PlatformFileReader** for local-only artifacts |
| [`164050_email_evidence_taxonomy_and_tui_rectification.md`](./2026-11-10/164050_email_evidence_taxonomy_and_tui_rectification.md) | Completed | RM-006: Top-level emails collection, EmailStatus taxonomy, backend backfill & rectification service, TUI rectification mode, and in-context GUI email evidence display |
| [`163500_rm-006-email-top-level-collection-assessment.md`](./2026-11-10/163500_rm-006-email-top-level-collection-assessment.md) | Completed | Assessment that top-level `emails` collection IS warranted for RM-006 (unlike RM-004 events) — cross-app queries needed, greenfield design, deduplication benefit |
| [`162700_firestore_events_migration_investigation.md`](./2026-11-10/162700_firestore_events_migration_investigation.md) | Completed | Investigation of migrating events to top-level collection: no-op recommendation — all queries are per-application, no cross-app queries exist (RM-004) |
| [`170538_company_sector_enum_and_heuristic_enrichment.md`](./2026-11-10/170538_company_sector_enum_and_heuristic_enrichment.md) | Completed | Define canonical CompanySector proto enum, Kotlin normalizer in records/parser, and execute backfill across 94 Firestore companies |
| [`165146_public_linkedin_company_enrichment_plan.md`](./2026-11-10/165146_public_linkedin_company_enrichment_plan.md) | Superseded | Public LinkedIn guest API scraping plan; superseded by 170538_company_sector_enum_and_heuristic_enrichment.md |
| [`164749_heuristic_company_enrichment_backfill.md`](./2026-11-10/164749_heuristic_company_enrichment_backfill.md) | Superseded | Option 2 implementation plan; superseded by 165146_public_linkedin_company_enrichment_plan.md |
| [`164458_rm005_company_enrichment_backfill_execution.md`](./2026-11-10/164458_rm005_company_enrichment_backfill_execution.md) | Proposed | Execution plan for RM-005 company enrichment backfill across 94 existing Firestore company records |
| [`160845_pure-kotlin-migration-url-cleaning-plan.md`](./2026-11-10/160845_pure-kotlin-migration-url-cleaning-plan.md) | Completed | Pure Kotlin migration of URL sanitization & tracking parameter cleaning (scripts/urls.py to records/parser UrlSanitizer.kt, RM-001 & RM-003) |
| [`160200_retroactive-company-enrichment.md`](./2026-11-10/160200_retroactive-company-enrichment.md) | Completed | Retroactive batch enrichment of existing Firestore company records (sector, linkedinUrl), mise task, frontend display, and sector→industry mapping fix |
| [`154500_pure_kotlin_migration_render_py.md`](./2026-11-10/154500_pure_kotlin_migration_render_py.md) | Completed | Pure Kotlin migration of event projection & history rendering (scripts/render.py & scripts/events.py to records/parser) |
| [`152500_linkedin_company_enrichment_plan.md`](./2026-11-10/152500_linkedin_company_enrichment_plan.md) | Completed | Enrich companies collection with LinkedIn industry tags and logo metadata during scrape (RM-005) |
| [`145600_fix-roadmap-edit-usage-spec-plan.md`](./2026-11-10/145600_fix-roadmap-edit-usage-spec-plan.md) | Completed | Fix invalid #USAGE spec key in .config/mise/tasks/roadmap/edit.sh and align task formatting |
| [`144900_roadmap-terminal-tui.md`](./2026-11-10/144900_roadmap-terminal-tui.md) | Completed | Interactive roadmap manager TUI in `frontend/apps/terminal` with `RoadmapParser`, `RoadmapWriter`, `RoadmapApp`, and mise tasks (`roadmap:edit`, `roadmap:status`) for ROADMAP.md management |
| [`144500_centralized_event_cryptography_and_projections.md`](./2026-11-10/144500_centralized_event_cryptography_and_projections.md) | Completed | Idempotent HTTP Event-Appending endpoint in backend/ (Cloud Run), client integration in frontend/ (Android, Web, Desktop, CLI), and pure Kotlin cryptographic hash chaining & timeline projection in records/parser (TODO-001 & TODO-003) |
| [`141800_reformat-todo-md.md`](./2026-11-10/141800_reformat-todo-md.md) | Completed | Reformat TODO.md (now ROADMAP.md) with structured blocks, complexity/priority/criticality scales, table of contents, and copy-pasteable template for AI agent ingestibility |
| [`130500_proposal_approval_flow_and_tracker_actions_review_plan.md`](./2026-11-10/130500_proposal_approval_flow_and_tracker_actions_review_plan.md) | Completed | Audit mise tracker proposal actions, eliminate auto-approval defaults, add interactive review confirmation gates, and align task taxonomy |
| [`130500_official-submission-single-source-of-truth.md`](./2026-11-10/130500_official-submission-single-source-of-truth.md) | Proposed | Single source of truth for officially submitted job applications across @records, @backend, tracker.schema.json, and Firestore |
| [`025642_companies-navigation-and-company-details-adaptive-feature-plan.md`](./2026-11-10/025642_companies-navigation-and-company-details-adaptive-feature-plan.md) | Completed | Companies top-level navigation, responsive grid tiles, and Company Details adaptive Supporting Pane experience |
| [`192641_rm-001-python-to-kotlin-migration-plan.md`](./2026-11-10/192641_rm-001-python-to-kotlin-migration-plan.md) | In Progress | RM-001: Migrate remaining Python scripts in `scripts/` and Python mise tasks to Kotlin across `records/parser/`, `frontend/apps/cli/`, and `backend/` modules |
| [`195200_replace-legacy-ui-with-frontend-apps-parity-plan.md`](./2026-11-09/195200_replace-legacy-ui-with-frontend-apps-parity-plan.md) | Completed | Replace legacy `ui/` (terminal & web) with `frontend/apps/terminal` and `frontend/apps/web`, achieving full terminal parity and retiring `ui/` |
| [`182733_consolidate-agent-directories.md`](./2026-11-09/182733_consolidate-agent-directories.md) | Completed | Consolidate all agent dot-directories (.claude, .junie, .gemini, .opencode, .qwen) to align with canonical .agents/ structure |
| [`181300_mise_task_orchestration_and_documentation_alignment_plan.md`](./2026-11-09/181300_mise_task_orchestration_and_documentation_alignment_plan.md) | Completed | Mise task orchestration for Konsist/validation, root-only mise.toml consolidation, and monorepo documentation alignment |
| [`164500_post_monorepo_quality_gates_and_cloud_run_deployment_plan.md`](./2026-11-09/164500_post_monorepo_quality_gates_and_cloud_run_deployment_plan.md) | Completed | Unified roadmap & implementation plan: tooling centralization under gradle/tools/, kebab-case catalog normalization, subsystem gradle/ removal, and local data enrichment |
| [`170500_align_declared_versions_to_kebab_case_plan.md`](./2026-11-09/170500_align_declared_versions_to_kebab_case_plan.md) | Superseded | Adopted and superseded by 164500_post_monorepo_quality_gates_and_cloud_run_deployment_plan.md |
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
| [`161900_email-job-application-discovery.md`](./2026-11-12/161900_email-job-application-discovery.md) | In Progress | Discovery classifier for untracked job applications from email evidence: DISCOVER event type, BackendService discovery functions, email:discover mise task, proposals.py DISCOVER approval, and gmail-sync.md workflow update |
| [`ai-job-search-plan.md`](../../ai-job-search-plan.md) | Canonical | Universal multi-agent architecture and single source of truth specifications |
