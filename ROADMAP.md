# Roadmap

This file tracks outstanding work for the AI job search repository. Each entry is structured for AI agents to ingest quickly: every item includes priority, criticality, complexity, scope, dependencies, and acceptance criteria. Items are sorted from highest complexity to lowest.

**How to use this file:**

- Scan the Table of Contents for a quick overview of all items.
- Read the detailed block for any item you plan to work on.
- Use the Template at the bottom to add new entries.
- Update **Status:** to `IN_PROGRESS` when starting work, and `DONE` when all acceptance criteria are met.
- Manage items interactively via `mise roadmap:edit` or view aggregate status via `mise roadmap:status`.

## Complexity Scale

| Level | Label   | Description                                                             |
|-------|---------|-------------------------------------------------------------------------|
| 1     | Trivial | Single-file change, no dependencies, straightforward implementation     |
| 2     | Simple  | A few files, minimal risk, clear path to completion                     |
| 3     | Moderate| Multiple files or modules, some coordination, moderate testing needed   |
| 4     | Complex | Cross-cutting changes, architectural impact, significant testing needed |
| 5     | Critical| Repository-wide impact, long migration path, high risk of regression   |

## Priority Scale

| Level   | Description                                                                 |
|---------|-----------------------------------------------------------------------------|
| CRITICAL| Blocks core functionality or security; must be addressed immediately        |
| HIGH    | Important for roadmap; should be completed before lower-priority items      |
| MEDIUM  | Valuable improvement; schedule when capacity allows                         |
| LOW     | Nice-to-have; low urgency, may be deprioritized                             |
| NOT_SET | Priority not yet assessed                                                   |

## Criticality Scale

| Level    | Description                                                                 |
|----------|-----------------------------------------------------------------------------|
| PIVOTAL  | Core to the project mission; without it the roadmap has no direction        |
| HIGH     | Major architectural or workflow impact; enables other items                 |
| MEDIUM   | Important improvement but not blocking other work                           |
| LOW      | Nice-to-have; low strategic impact                                          |
| NOT_SET  | Criticality not yet assessed                                                |

## Table of Contents

| ID       | Title                                  | Short Desc                          | Criticality | Priority | Age    | Status |
|----------|----------------------------------------|-------------------------------------|-------------|----------|--------|--------|
| RM-001   | Kotlin-Only Migration                  | Migrate all Python scripts to Kotlin | PIVOTAL    | MEDIUM   | 70 d   | IN_PROGRESS |
| RM-002   | AI-Portable Pluginization              | Make agent contributions portable   | LOW         | LOW      | 70 d   | OPEN   |
| RM-003   | Backend Functionality Abstraction      | Expose deterministic tasks as services | HIGH       | HIGH     | 70 d   | IN_PROGRESS |
| RM-006   | Add Email Top-Level Destination for in-app analysis | Add Email evidence & rectification | MEDIUM      | MEDIUM   | 0 d    | DONE        |
| RM-004   | Firestore Events Migration Investigation | Evaluate top-level events collection | LOW        | LOW      | 35 d   | DONE   |
| RM-005   | LinkedIn Company Enrichment            | Enrich companies with LinkedIn data | HIGH        | HIGH     | 35 d   | DONE   |
| RM-007   | Fix KMP Inspector                      | Fix Room and Ktor integrations       | PIVOTAL    | CRITICAL | 0 d    | DONE        |

---

### [RM-001] Kotlin-Only Migration

**Short Desc:** Migrate all Python scripts to Kotlin
**Criticality:** PIVOTAL
**Priority:** MEDIUM
**Complexity:** 5 — Critical
**Status:** IN_PROGRESS
**Completion %:** 60
**Creation Date:** 2026-11-10
**Objective:** Incrementally migrate all Python functionality in `scripts/` (56 files) to Kotlin Multiplatform, fully eradicating Python from the codebase. The ideal workflow becomes `Agent` → `mise` → `some-locally-built.jar <ARGS>` where the JAR originates from this repo (most likely `frontend/apps/cli`).

**Scope:**

- In Scope: Migrating all 56 Python scripts to Kotlin Multiplatform, targeting KMP Compose where possible for UI layers. Existing Python is deprecated and closed to enhancement.
- Out of Scope: Big-bang rewrite. This migration must be incremental and TDD-driven.

**Dependencies:** None

**Acceptance Criteria:**

- All Python scripts in `scripts/` have Kotlin equivalents or are archived
- Python dependencies are removed from the project
- `mise` tasks invoke Kotlin JARs instead of Python scripts
- Existing functionality is preserved — no behavioral regression
- Migration follows TDD practices: tests written or ported before implementation

**Notes:**

- Migration should be incremental, not a one-time pass
- Python is deprecated; new functionality is Kotlin-only
- Target Kotlin Multiplatform architecture with Compose for UI layers
- The `records/parser/` module contains pure Kotlin domain logic; `frontend/apps/cli/` contains Clikt-based CLI commands
- Complex orchestrators (`scrape.py`, `apply.py`) and external tool wrappers (`gmail_fetch.py`) are skipped — no meaningful Kotlin equivalent
- One-time scripts (`importer.py`, `migrate_gcs.py`, `purge_gcs_legacy.py`, `reconcile_submissions.py`) are archived
- Phase 8 (final cleanup) is blocked on remaining Python scripts without Kotlin equivalents

**Linked Plans:**

- `.agents/plans/2026-11-10/192641_rm-001-python-to-kotlin-migration-plan.md` (canonical, supersedes all)
- `.agents/plans/2026-11-10/154500_pure_kotlin_migration_render_py.md`
- `.agents/plans/2026-11-10/144500_centralized_event_cryptography_and_projections.md`
- `.agents/plans/2026-11-08/171648_hunter-logo-integration.md`
- `.agents/plans/2026-11-08/214000_fix-email-collect-contract-error.md`
- `.agents/plans/2026-11-08/215200_fix-event-type-and-email-pipeline-docs.md`
- `.agents/plans/2026-11-08/180500_python-to-mise-tasks-migration-plan.md`

**Changelog:**

| Date       | Change            | Details                              |
|------------|-------------------|--------------------------------------|
| 2026-11-10 | Progress update   | Created 10 Kotlin modules (DocumentIdGenerator, UuidGenerator, EventBuilder, FitEvaluator, RankScorer, ProposalClassifier, ArtifactUtils, CacheUtils, ContractError, EventError) with 117 tests in records/parser |
| 2026-11-10 | Progress update   | Added RankCommand.kt CLI command and registered in Main.kt |
| 2026-11-10 | Progress update   | Converted 13 Python mise tasks to shell wrappers (tracker/check, add, list, get, status, score, event, align, seed, default, search/rank, dev/test, ui/web/server) |
| 2026-11-10 | Progress update   | Added DEPRECATED/ARCHIVED markers to all 34 remaining Python scripts |
| 2026-11-10 | Progress update   | Scrubbed Python script references from AI-significant .md files (README, SKILL.md, SETUP.md, docs/), redirecting to mise tasks |
| 2026-11-10 | Progress update   | Updated README.md diagrams to reflect Kotlin-first architecture |
| 2026-11-10 | Progress update   | Aligned migration plan with TEMPLATE.md, created canonical plan at .agents/plans/2026-11-10/ |
| 2026-11-10 | Progress update   | Migrated clean_url and URL query sanitization to pure Kotlin (records/parser UrlSanitizer.kt & JPypeBridge.cleanUrl), marked scripts/urls.py deprecated |
| 2026-11-10 | Progress update   | Migrated renderNotes and event projection logic to pure Kotlin (records/parser), marked render.py deprecated |
| 2026-11-10 | Progress update   | Implemented pure Kotlin EventChainBuilder and ApplicationTimelineProjector in records/parser |
| 2026-11-10 | Item created      | Renamed from TODO-001 to RM-001      |

---

### [RM-002] AI-Portable Pluginization

**Short Desc:** Make agent contributions portable
**Criticality:** LOW
**Priority:** LOW
**Complexity:** 4 — Complex
**Status:** OPEN
**Completion %:** 0
**Creation Date:** 2026-11-10
**Objective:** Enforce `.agents/*` as the canonical and EXCLUSIVE home for all AI contributions from this repository. Encapsulate key operations (tool calls, database knowledge, workflows) into consumable, self-contained plugins for maximum AI-agent portability.

**Scope:**

- In Scope: Establishing `.agents/*` as the exclusive home for AI contributions. Any `.<AGENT>/*` directories contain only simple router files pointing to `.agents/*` equivalents. Grouping the minimum necessary functionality into portable units so any agent can conditionally consume them.
- Out of Scope: Fundamentally changing how the project integrates across harnesses and models. This expands agent-agnostic integrations, not replaces them.

**Dependencies:** None

**Acceptance Criteria:**

- `.agents/*` is documented as the canonical and exclusive home for AI contributions
- Any `.<AGENT>/*` directories contain only router/pointer files to `.agents/*`
- Key operations are encapsulated as self-contained, consumable plugins
- Plugins are portable across different AI agent frameworks
- Dependencies between plugins are minimal and explicitly documented

**Notes:**

- Some AI agents accept `.<AGENT>/plugins/*` as an alternative abstraction akin to skills
- Operations that don't make sense out of context should be grouped with the least amount of other functions necessary to provide a complete picture
- Example: `/scrape`, `/apply`, `/outcome` command sequence should be encapsulated as a single portable unit

**Linked Plans:**

- `.agents/plans/2026-11-05/173850_plan-relocation-and-index-alignment.md`
- `.agents/plans/2026-11-05/164709_plan-files-consolidation-and-gitignore-alignment.md`
- `.agents/plans/2026-11-05/183738_plan-frontmatter-standardization-and-templating.md`
- `.agents/plans/2026-11-05/181232_mark-detekt-and-konsist-plan-completed.md`
- `.agents/plans/2026-11-09/182733_consolidate-agent-directories.md`

**Changelog:**

| Date       | Change            | Details                              |
|------------|-------------------|--------------------------------------|
| 2026-11-10 | Item created      | Renamed from TODO-002 to RM-002      |

---

### [RM-003] Backend Functionality Abstraction

**Short Desc:** Expose deterministic tasks as services
**Criticality:** HIGH
**Priority:** HIGH
**Complexity:** 3 — Moderate
**Status:** IN_PROGRESS
**Completion %:** 75
**Creation Date:** 2026-11-10
**Objective:** Migrate existing deterministic Python functionalities and expose them to both `backend/` and `frontend/apps` projects. This sets the high-level requirement for any backend functionality created — deterministic tasks that don't require AI oversight should be offloaded to regular computation environments or automated processes.

**Scope:**

- In Scope: Identifying deterministic functionalities currently in Python scripts (e.g., cryptographic event chain alignment), migrating them to Kotlin, and exposing them via `backend/` and `frontend/apps`. Migration order: NEWEST functionality first.
- Out of Scope: Treating existing implementations as enhancements. This is about refactoring existing deterministic logic, not adding new features.

**Dependencies:** RM-001 (Kotlin migration provides the foundation)

**Acceptance Criteria:**

- Deterministic Python functionalities are identified and cataloged
- Each identified functionality is migrated to Kotlin and exposed to `backend/` and `frontend/apps`
- Migration follows NEWEST-first order
- Exposed functionalities are consumable by both backend services and frontend applications
- Existing behavior is preserved — no functional regression

**Notes:**

- Cryptographic event chain alignment (`scripts/events.py`) is an example of deterministic logic
- This encapsulates a high-level requirement for any "backend" functionality going forward
- The `backend/` directory already exists as a small Kotlin service

**Linked Plans:**

- `.agents/plans/2026-11-10/160200_retroactive-company-enrichment.md`
- `.agents/plans/2026-11-10/154500_pure_kotlin_migration_render_py.md`
- `.agents/plans/2026-11-09/164500_post_monorepo_quality_gates_and_cloud_run_deployment_plan.md`
- `.agents/plans/2026-11-10/144500_centralized_event_cryptography_and_projections.md`
- `.agents/plans/2026-11-09/125800_kotlin_cloud_run_remote_tasks_and_tui_plan.md`

**Changelog:**

| Date       | Change            | Details                              |
|------------|-------------------|--------------------------------------|
| 2026-11-10 | Status update     | Implemented application alignment route and service logic |
| 2026-11-10 | Status update     | Implemented event chaining, projections, and backend service append |
| 2026-11-10 | Item created      | Renamed from TODO-003 to RM-003      |

---

### [RM-004] Firestore Events Migration Investigation

**Short Desc:** Evaluate top-level events collection
**Criticality:** LOW
**Priority:** LOW
**Complexity:** 2 — Simple
**Status:** DONE
**Completion %:** 100
**Creation Date:** 2026-11-10
**Completed Date:** 2026-11-10
**Objective:** Investigate migrating `events` to a top-level collection aligned with `applications` and evaluate the value, query impact, and metrics implications. A no-op outcome is acceptable if the investigation shows no benefit.

**Scope:**

- In Scope: Analyzing the current `events` structure, evaluating the impact of migrating to a top-level collection, assessing query performance and metrics changes, and documenting findings.
- Out of Scope: Actually performing the migration unless the investigation clearly shows significant benefit.

**Dependencies:** None

**Acceptance Criteria:**

- Investigation document produced with findings
- Query impact analysis completed (performance comparison before/after)
- Metrics implications evaluated
- Clear recommendation: proceed with migration or no-op
- If no-op, reasoning is documented

**Notes:**

- Current events are nested; migration would align with `applications` at the top level
- A no-op outcome is perfectly acceptable
- Focus on whether the change improves or worsens queries and metrics

**Linked Plans:**

- `.agents/plans/2026-11-10/162700_firestore_events_migration_investigation.md`

**Changelog:**

| Date       | Change            | Details                              |
|------------|-------------------|--------------------------------------|
| 2026-11-10 | Completed         | Investigation complete: no-op recommendation — all event queries are per-application, no cross-app queries exist |
| 2026-11-10 | Item created      | Renamed from TODO-004 to RM-004      |

---

### [RM-005] LinkedIn Company Enrichment

**Short Desc:** Enrich companies with LinkedIn data
**Criticality:** HIGH
**Priority:** HIGH
**Complexity:** 1 — Trivial
**Status:** DONE
**Completion %:** 100
**Creation Date:** 2026-11-10
**Completed Date:** 2026-11-10
**Objective:** Integrate LinkedIn to enrich the `companies` collection with `industry` tags and `logo` metadata during `/scrape` for roles that are surfaced to `/apply`.

**Scope:**

- In Scope: Adding company metadata enrichment (industry tags, logo URLs) during the scrape workflow. Targeting the `companies` top-level collection.
- Out of Scope: Enriching other collections or adding metadata beyond industry and logo.

**Dependencies:** None

**Acceptance Criteria:**

- During `/scrape`, company metadata is fetched and enriched
- `companies` collection includes `industry` tags
- `companies` collection includes `logo` metadata (URL)
- Enrichment is non-blocking — scrape still succeeds even if enrichment fails
- Enriched data is available when `/apply` processes the job posting

**Notes:**

- LinkedIn search skill already exists at `.agents/skills/linkedin-search/`
- Enrichment should happen during `/scrape` to ensure data is ready for `/apply`
- Keep volume low per LinkedIn ToS

**Linked Plans:**

- `.agents/plans/2026-11-10/160200_retroactive-company-enrichment.md`
- `.agents/plans/2026-11-10/152500_linkedin_company_enrichment_plan.md`
- `.agents/plans/2026-11-08/171648_hunter-logo-integration.md`

**Changelog:**

| Date       | Change            | Details                              |
|------------|-------------------|--------------------------------------|
| 2026-11-10 | Progress update   | Retroactive batch enrichment of existing company records, frontend LinkedIn URL display, and sector to industry mapping fix |
| 2026-11-10 | Completed         | Enriched company metadata during /scrape and implemented backend enrichment service |
| 2026-11-10 | Status update     | Started implementation per approved plan 152500 |
| 2026-11-10 | Item created      | Renamed from TODO-005 to RM-005      |

---

### [RM-006] Add Email Top-Level Destination for in-app analysis

**Short Desc:** Add Email evidence & rectification
**Criticality:** MEDIUM
**Priority:** MEDIUM
**Complexity:** 3 — Moderate
**Status:** DONE
**Completion %:** 100
**Creation Date:** 2026-11-10
**Completed Date:** 2026-11-10
**Objective:** Surface the evidence used behind automated decisions to records. Introduce a top-level `emails` collection in Firestore, a backend backfill & rectification service, an interactive TUI rectification mode, and in-context email evidence viewing on application detail records.

**Dependencies:** None

**Linked Plans:**

- `.agents/plans/2026-11-10/164050_email_evidence_taxonomy_and_tui_rectification.md`
- `.agents/plans/2026-11-10/163500_rm-006-email-top-level-collection-assessment.md`

**Changelog:**

| Date       | Change            | Details                              |
|------------|-------------------|--------------------------------------|
| 2026-11-10 | Completed         | Backfilled 200 emails to Firestore, implemented backend rectification endpoints, TUI rectification mode, and GUI in-context evidence display |
| 2026-11-10 | Status update     | Started implementation per approved plan 164050 |
| 2026-11-10 | Item created      | Initial entry                        |


---

### [RM-007] Fix KMP Inspector

**Short Desc:** Fix KMP Inspector to address Room and Ktor integrations on all platforms
**Criticality:** PIVOTAL
**Priority:** CRITICAL
**Complexity:** 0 — Unknown
**Status:** OPEN
**Completion %:** 0
**Creation Date:** 2026-11-10
**Objective:** Fix the integration of KMP Inspector so Room and Ktor traffic is captured on all (supported) platforms

**Dependencies:** None

**Linked Plans:**
- None

**Changelog:**
| Date       | Change            | Details                              |
|------------|-------------------|--------------------------------------|
| 2026-11-10 | Item created      | Initial entry                        |

---


---

### [RM-008] Add MaterialKolor Library

**Short Desc:** Add MaterialKolor Library
**Criticality:** HIGH
**Priority:** MEDIUM
**Complexity:** 0 — Unknown
**Status:** DONE
**Completion %:** 100
**Creation Date:** 2026-11-11
**Completed Date:** 2026-11-11
**Objective:** Integrate MaterialKolor library to replace manual color scheme derivation with Google's official material-color-utilities engine

**Dependencies:** None

**Linked Plans:**
- `frontend/.agents/plans/2026-11-11/010500_rm-008-materialkolor-integration.md`

**Changelog:**
| Date       | Change            | Details                              |
|------------|-------------------|--------------------------------------|
| 2026-11-11 | Completed         | Integrated MaterialKolor 6.0.0, replaced manual color derivation with DynamicMaterialExpressiveTheme, removed buildLightColorScheme and buildDarkColorScheme |
| 2026-11-11 | Item created      | Initial entry                        |
## Frozen Items

Items in this section have been frozen via the nuclear clear roadmap option. They are archived and should not be considered active work items.

*(No frozen items)*

---

## Template for New Entries

To add a new roadmap item, use `mise roadmap:edit` for the interactive experience, or copy the block below:

```markdown
### [RM-XXX] Title

**Short Desc:** — one-line summary
**Criticality:** PIVOTAL | HIGH | MEDIUM | LOW | NOT_SET
**Priority:** CRITICAL | HIGH | MEDIUM | LOW | NOT_SET
**Complexity:** 1-5 (see Complexity Scale)
**Status:** OPEN
**Completion %:** 0
**Creation Date:** YYYY-MM-DD
**Objective:** — clear statement of what this item achieves

**Scope:**
- In Scope: what this item covers
- Out of Scope: what is explicitly excluded

**Dependencies:** None | RM-XXX, RM-YYY

**Acceptance Criteria:**
- [ ] Concrete condition that defines "done"
- [ ] Another measurable condition
- [ ] Verification step

**Notes:**
- Additional context, constraints, or implementation hints

**Linked Plans:**
- None

**Changelog:**
| Date       | Change            | Details                              |
|------------|-------------------|--------------------------------------|
| YYYY-MM-DD | Item created      | Initial entry                        |
```

**Instructions:**

1. Replace `XXX` with the next available number (e.g., 006).
2. Choose a Criticality from the Criticality Scale.
3. Choose a Priority from the Priority Scale.
4. Assign a Complexity level (1-5) from the Complexity Scale.
5. Set Status to `OPEN`; change to `IN_PROGRESS` when starting, `DONE` when complete.
6. List any dependencies on other roadmap items.
7. Write acceptance criteria as concrete, testable conditions.
8. Insert the new block in complexity-sorted order (highest complexity first).
9. Add a row to the Table of Contents.
