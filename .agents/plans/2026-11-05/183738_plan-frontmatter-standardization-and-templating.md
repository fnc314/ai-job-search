# Implementation Plan: Universal Markdown Plan Frontmatter Standardization & Templating

**Status:** Completed  
**Date:** 2026-11-05  
**Target:** repository-wide (root, `records`, `ui/kmp` plan directories & agent rules)  
**Schema Source:** [`175500_canonical-enum-mapping-and-wire-contracts-plan.md`](./175500_canonical-enum-mapping-and-wire-contracts-plan.md)  

---

## Overview & Goal
Standardize the opening frontmatter across all `.md` architecture and implementation plan files repository-wide to match the canonical format established in [`.agents/plans/2026-11-05/175500_canonical-enum-mapping-and-wire-contracts-plan.md`](./175500_canonical-enum-mapping-and-wire-contracts-plan.md). 

Furthermore, create a permanent `.agents/plans/TEMPLATE.md` and update **every** `AGENTS.md` and `.agents/plans/README.md` in the repository — root, `records/`, and `ui/kmp/` — so each one states the frontmatter invariant and links to the single canonical template.

### Scope Summary (revised per review)
| Area | Action |
| :--- | :--- |
| Root `.agents/plans/` (10 plans) | Apply canonical frontmatter |
| `ui/kmp/.agents/plans/` (22 plans) | Apply canonical frontmatter |
| `docs/tracker-migration-plan.md` | **Migrate** → `.agents/plans/2026-10-31/021052_tracker-migration-plan.md` (git add-date `2026-10-31 02:10:52`), apply conventions, fix all inbound links & registries |
| `ai-job-search-plan.md` | **Out of scope — untouched** |
| `AGENTS.md` ×3 (root, `records`, `ui/kmp`) | Add frontmatter rule + link to `TEMPLATE.md` |
| Plan registries ×3 (root, `records`, `ui/kmp`) | Add invariant #5 + link to `TEMPLATE.md` |

---

## The Canonical Frontmatter Standard

Derived from `175500_canonical-enum-mapping-and-wire-contracts-plan.md`:
```markdown
# <Plan Type>: <Title>

**Status:** <Status>  
**Date:** <YYYY-MM-DD>  
**Target:** <Subsystem / Target Scope>  
**Evaluates:** [<Optional Reference>](<relative-path>)  
**Schema Authority:** [<Optional Reference>](<relative-path>)  
**Depends On:** [<Optional Reference>](<relative-path>)  
**Supersedes:** [<Optional Reference>](<relative-path>)  

---
```

### Key Formatting Rules:
1. **Title H1**: `# <Type> Plan: <Title>` (e.g., `Architecture Plan`, `Implementation Plan`, `Migration Plan`, `Requirements & Architecture Plan`).
2. **Metadata Key-Values**: Bold key with colon (`**Key:** Value`), followed by two trailing spaces (`  \n`) for Markdown hard line breaks.
3. **Mandatory Fields**:
   - `**Status:**` One of `Completed`, `In Review`, `In Progress`, `Draft / Proposed`, `Superseded`, `Canonical`.
   - `**Date:**` ISO creation date (`YYYY-MM-DD`) matching the parent date folder.
   - `**Target:**` Clear subsystem indicator (e.g., `records`, `ui/kmp`, `cross-subsystem`, `cli`, `tracker`, `root`).
4. **Contextual / Optional Links**:
   - `**Evaluates:**`, `**Schema Authority:**`, `**Depends On:**`, `**Supersedes:**`, `**Author:**`.
5. **Horizontal Divider**: Standard `---` separating frontmatter from the document body.
6. **Legacy Cleanup**: Complete elimination of obsolete session blocks (`--- \n sessionId: ... \n ---`).

---

## User Review Required

> [!IMPORTANT]
> **Repository-Wide Scope & Document Taxonomy**:
> The repository contains several categories of `.md` files. We classify them as follows:
> 1. **Plan Documents (Direct Alignment - 33 files)**: All `.md` plans in `.agents/plans/` (10) and `ui/kmp/.agents/plans/` (22), plus the migrated `tracker-migration-plan.md` (1). `records/.agents/plans/` has no plans yet. `ai-job-search-plan.md` is explicitly excluded.
> 2. **Plan Registry Index Files (`README.md` in plans directories - 3 files)**: Updated to document the mandatory frontmatter invariant and reference `TEMPLATE.md`.
> 3. **Non-Plan Markdown Files**:
>    - *Module READMEs* (`core/*/README.md`, `features/*/README.md`): Keep standard module-level documentation headers.
>    - *Outcome Records* (`job-applications/**/outcome.md`): Maintain their existing domain-specific header (`Company`, `Role`, `Evaluation Date`, `Overall Fit Rating`).
>    - *Auto-Generated Docs* (`docs/mise/*.md`): Auto-generated from `scripts/specs/*.usage.kdl` by `mise run mise:docs:tasks`.
>    - *Agent Rules & Framework Wrappers* (`AGENTS.md`, `ui/kmp/AGENTS.md`): Updated to encode this frontmatter standard.

---

## Proposed Changes

### Phase 1: Reusable Template & Agent Rules Codification

#### [NEW] [`.agents/plans/TEMPLATE.md`](../TEMPLATE.md)
Canonical template containing standard frontmatter and structural sections:
```markdown
# [Architecture | Implementation | Migration] Plan: <Plan Title>

**Status:** [Draft / Proposed | In Review | In Progress | Completed | Superseded]  
**Date:** YYYY-MM-DD  
**Target:** [`records` | `ui/kmp` | `cross-subsystem` | `tracker` | `cli` | `root`]  
**Evaluates:** [`<draft-name>.md`](<relative-path>) <!-- Optional -->  
**Schema Authority:** [`tracker.schema.json`](<relative-path>) <!-- Optional -->  
**Depends On:** [`<plan-name>.md`](<relative-path>) <!-- Optional -->  
**Supersedes:** [`<plan-name>.md`](<relative-path>) <!-- Optional -->  

---

## 1. Executive Summary & Goals

## 2. Architecture & Design Principles

## 3. Proposed Changes & Implementation Steps

## 4. Verification Plan & Testing Checklist
```

#### [MODIFY] AGENTS.md ×3 — [root](../../../AGENTS.md), [records](../../../records/AGENTS.md), [ui/kmp](../../../ui/kmp/AGENTS.md)
Add to each plan-rules section (root: "Architecture & Design Plans"; `records`: Rule 1; `ui/kmp`: Rule 6):
```markdown
- **Mandatory Opening Frontmatter**: Every plan must begin by copying the canonical template
  [`.agents/plans/TEMPLATE.md`](<relative>/.agents/plans/TEMPLATE.md): an H1 `# <Type> Plan: <Title>`,
  then `**Status:**`, `**Date:**`, `**Target:**` (plus optional `Evaluates` / `Schema Authority` /
  `Depends On` / `Supersedes`), then `---`. No YAML `sessionId` blocks.
```
Relative links: root `./.agents/plans/TEMPLATE.md`; `records` `../.agents/plans/TEMPLATE.md`; `ui/kmp` `../../.agents/plans/TEMPLATE.md` (single template, no copies).

#### [MODIFY] Plan registries ×3 — [root](../README.md), [records](../../../records/.agents/plans/README.md), [ui/kmp](../../../ui/kmp/.agents/plans/README.md)
Append invariant to `## Plan Invariants (MANDATORY)` (#5 root/ui-kmp, #4 records):
- **Mandatory Opening Frontmatter**: Start from [`TEMPLATE.md`](…); `Status` must match this registry row.

Root registry: remove the `docs/tracker-migration-plan.md` row; add `tracker-migration-plan.md` → `./2026-10-31/021052_tracker-migration-plan.md` (Completed); add a "Template" line pointing to `./TEMPLATE.md`. Leave the `ai-job-search-plan.md` row alone.

---

### Phase 2: Root Plan Files Alignment (`.agents/plans/` & `docs/`)

| File | Target Frontmatter Header |
| :--- | :--- |
| **`2026-10-31/135442_firestore-data-optimization.md`** | `# Architecture Plan: Firestore Data Optimization & GCS Artifact Migration`<br/>`**Status:** Completed`<br/>`**Date:** 2026-10-31`<br/>`**Target:** tracker (backend, storage, Firestore)` |
| **`2026-11-01/174503_tui-log-rotation-and-cleanup.md`** | `# Implementation Plan: TUI Session Log Rotation & Disk Cleanup`<br/>`**Status:** Completed`<br/>`**Date:** 2026-11-01`<br/>`**Target:** ui/terminal (Curses TUI)` |
| **`2026-11-02/061702_kmp-proto-contracts.md`** | `# Architecture Plan: KMP Protobuf Type Contracts & Records Restructuring`<br/>`**Status:** Completed`<br/>`**Date:** 2026-11-02`<br/>`**Target:** records and ui/kmp (cross-subsystem)` |
| **`2026-11-02/172651_recruiter-outreach-tracking.md`** | `# Implementation Plan: Recruiter Outreach Data Models & Pipeline Tracking`<br/>`**Status:** Completed`<br/>`**Date:** 2026-11-02`<br/>`**Target:** records and tracker (CLI & Schema)` |
| **`2026-11-04/020823_add-job-url-fields-to-tui-and-cli.md`** | `# Implementation Plan: Job URL Fields Integration in TUI, CLI & Schema`<br/>`**Status:** Completed`<br/>`**Date:** 2026-11-04`<br/>`**Target:** ui/terminal and tracker (CLI & Schema)` |
| **`2026-11-05/163042_dokka-and-comprehensive-kdocs-plan.md`** | `# Implementation Plan: Dokka Integration & Comprehensive Workspace KDocs`<br/>`**Status:** Completed`<br/>`**Date:** 2026-11-05`<br/>`**Target:** records and ui/kmp (cross-subsystem)` |
| **`2026-11-05/164709_plan-files-consolidation-and-gitignore-alignment.md`** | `# Implementation Plan: Plan Files Consolidation, Sorting & Repository Gitignore Alignment`<br/>`**Status:** Completed`<br/>`**Date:** 2026-11-05`<br/>`**Target:** repository-wide (root .agents/plans, .gitignore)` |
| **`2026-11-05/172809_detekt-and-konsist-integration-plan.md`** | `# Implementation Plan: Detekt & Konsist Static Analysis & Architectural Linting`<br/>`**Status:** Completed`<br/>`**Date:** 2026-11-05`<br/>`**Target:** records and ui/kmp (cross-subsystem)` |
| **`2026-11-05/173850_plan-relocation-and-index-alignment.md`** | `# Implementation Plan: Relocate Dokka Plan & Date/Timestamp Reorganization of .agents/plans/`<br/>`**Status:** Completed`<br/>`**Date:** 2026-11-05`<br/>`**Target:** repository-wide (root .agents/plans, ui/kmp/.agents/plans)` |
| **`2026-11-05/181232_mark-detekt-and-konsist-plan-completed.md`** | `# Implementation Plan: In-Repo Plan Relocation & Registry Status Synchronization`<br/>`**Status:** Completed`<br/>`**Date:** 2026-11-05`<br/>`**Target:** repository-wide (root .agents/plans)` |

### Phase 2b: Migrate `docs/tracker-migration-plan.md` → `.agents/plans/`

#### [DELETE → NEW] `git mv docs/tracker-migration-plan.md .agents/plans/2026-10-31/021052_tracker-migration-plan.md`
Preserves history. Existing bespoke header fields are folded into the canonical block (no info lost):
```diff
-# Tracker Migration Plan — CSV → Firestore
-
-**Status:** complete — Phases 0–8, 9a, 9b, and 10 landed. Firestore holds the record …
-**Owner:** Franco N. Colaizzi
-**Supersedes:** `job_search_tracker.schema.json` v1 (CSV contract), `scripts/export_tracker_json.py`
-**Last updated:** 2026-10-31 (post-migration setup/mock-data follow-on)
-**Update rule:** this file and `docs/tracker-migration.md` are revised at the end of **every phase** …
+# Migration Plan: Tracker Migration — CSV → Firestore
+
+**Status:** Completed  
+**Date:** 2026-10-31  
+**Target:** tracker (CLI, schema, Firestore, Sheet/dashboard views)  
+**Schema Authority:** [`tracker.schema.json`](../../../tracker.schema.json)  
+**Supersedes:** `job_search_tracker.schema.json` v1 (CSV contract), `scripts/export_tracker_json.py`  
+**Companion:** [`docs/tracker-migration.md`](../../../docs/tracker-migration.md)  
+**Owner:** Franco N. Colaizzi  
+**Last updated:** 2026-10-31 (post-migration setup/mock-data follow-on)  
+
+> Completion detail: Phases 0–8, 9a, 9b, and 10 landed. Firestore holds the record … *(original status sentence preserved verbatim)*
+> **Update rule:** this file and `docs/tracker-migration.md` are revised at the end of **every phase** … *(preserved verbatim)*
+
+---
```
Any relative links inside the body are re-based from `docs/` to `.agents/plans/2026-10-31/` (prefix `../../../`).

#### Inbound link updates
| File | Change |
| :--- | :--- |
| `.agents/plans/README.md` | registry row (above) |
| `docs/tracker-migration.md` (L7, L12) | `docs/tracker-migration-plan.md` → `.agents/plans/2026-10-31/021052_tracker-migration-plan.md` |
| `.agents/skills/job-tracker/SKILL.md` (L15) | same path update |
| `.agents/plans/2026-11-05/173850_plan-relocation-and-index-alignment.md` (L120) | update link target |
| `164709_plan-files-consolidation…md` (L70-71) | historical narrative — left as-is (describes past state) |
| `memory/**` | historical corpus — untouched |

---

### Phase 3: Subsystem Plan Files Alignment (`ui/kmp/.agents/plans/`)

Standardize all 22 plans in `ui/kmp/.agents/plans/` using the registry index data:

1. **`2026-11-02/221655_kmp-mobile-tracker-ui.md`**:
   - `# Architecture Plan: KMP Mobile Tracker UI & Data Foundation`
   - `**Status:** Completed` | `**Date:** 2026-11-02` | `**Target:** ui/kmp`
2. **`2026-11-03/205720_create-core-modules-and-notes-widget.md`**:
   - `# Implementation Plan: Core Module Extraction & NotesWidget Architecture`
   - `**Status:** Completed` | `**Date:** 2026-11-03` | `**Target:** ui/kmp:core:widgets, ui/kmp:core:design-system`
3. **`2026-11-03/205720_fix-android-defects.md`**:
   - `# Implementation Plan: Android Runtime, Build & Edge-to-Edge Defect Fixes`
   - `**Status:** Completed` | `**Date:** 2026-11-03` | `**Target:** ui/kmp:apps:android`
4. **`2026-11-03/230050_firebase-android-and-multiplatform-integration.md`**:
   - `# Implementation Plan: GitLive Firebase SDK Multiplatform Integration`
   - `**Status:** Completed` | `**Date:** 2026-11-03` | `**Target:** ui/kmp:core:data`
5. **`2026-11-03/232245_add-job-application-url-and-records-dep.md`**:
   - `# Implementation Plan: Job Application URL Flow & Records Composite Dependency`
   - `**Status:** Completed` | `**Date:** 2026-11-03` | `**Target:** ui/kmp`
6. **`2026-11-04/064700_integrate-records-fix-icons.md`**:
   - `# Implementation Plan: Protobuf Records Integration & Material Icon Modernization`
   - `**Status:** Completed` | `**Date:** 2026-11-04` | `**Target:** ui/kmp`
7. **`2026-11-04/122940_m3-theming-adaptive-layout-and-widgets.md`**:
   - `# Architecture Plan: Material 3 Theming, Adaptive Layouts & Widget Enhancements`
   - `**Status:** Completed` | `**Date:** 2026-11-04` | `**Target:** ui/kmp:core:design-system, ui/kmp:core:widgets`
8. **`2026-11-04/133245_ui-flow-improvements-and-modularization.md`**:
   - `# Architecture Plan: UI Flow Enhancements, Navigation & Modularization`
   - `**Status:** Completed` | `**Date:** 2026-11-04` | `**Target:** ui/kmp:features`
9. **`2026-11-04/195448_repackaging-shared-and-job-applications-modularization.md`**:
   - `# Architecture Plan: Repackaging shared into apps/core & Job Applications Modularization`
   - `**Status:** Completed` | `**Date:** 2026-11-04` | `**Target:** ui/kmp:apps:core, ui/kmp:features`
10. **`2026-11-05/160417_build-logs-and-compiler-warnings-triage-plan.md`**:
    - `# Implementation Plan: Triage & Resolve Build Warnings in apps-android:assembleDebug`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp`
11. **`2026-11-05/160417_compose-resources-migration-plan.md`**:
    - `# Architecture Plan: Complete Compose Resources Migration`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp:core:design-system, ui/kmp:features`
12. **`2026-11-05/160417_mvi-architecture-and-ui-model-separation-plan.md`**:
    - `# Architecture Plan: DataSource Abstraction, UI Model Separation, & Strict MVI`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp:features, ui/kmp:core:data`
13. **`2026-11-05/160417_platform-agnosticism-renaming-plan.md`**:
    - `# Architecture Plan: Platform-Agnostic Module Renaming`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp`
14. **`2026-11-05/160417_root-scaffold-and-navigation-enhancement-plan.md`**:
    - `# Architecture Plan: Root Scaffold & Navigation Backstack Enhancement`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp:apps:core, ui/kmp:features`
15. **`2026-11-05/160417_thinning-apps-core-and-core-navigation.md`**:
    - `# Architecture Plan: Thinning apps/core & Modular Navigation Decoupling`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp:apps:core, ui/kmp:core:navigation`
16. **`2026-11-05/163042_room3-platform-support-and-caching-plan.md`**:
    - `# Implementation Plan: Room 3 Multiplatform Support & Offline-First Caching Policy`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp:core:data`
17. **`2026-11-05/164709_clean-architecture-datasources-repositories-plan.md`**:
    - `# Implementation Plan: Clean Architecture DataSources & Repositories Realignment`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp:core:data, ui/kmp:features`
18. **`2026-11-05/164709_custom-gradle-plugins-architecture-plan.md`**:
    - `# Architecture Plan: Custom Gradle Convention Plugins (gradle/tools/project-plugins)`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp:gradle/tools/project-plugins`
19. **`2026-11-05/164709_module-readmes-plan.md`**:
    - `# Implementation Plan: Module README.md Documentation Standard`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp:core, ui/kmp:features`
20. **`2026-11-05/164709_testing-bundles-plan.md`**:
    - `# Implementation Plan: Testing Dependency Bundles & Multiplatform Testing Plugin`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp:gradle`
21. **`2026-11-05/164709_unit-tests-coverage-plan.md`**:
    - `# Implementation Plan: Comprehensive Unit Tests for Platform-Agnostic Flows`
    - `**Status:** Completed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp`
22. **`2026-11-05/172809_DRAFT-code-refactor.md`**:
    - `# Architecture Plan: Canonical Enum Mapping & Self-Contained Relationships (Refactor Proposal)`
    - `**Status:** Draft / Proposed` | `**Date:** 2026-11-05` | `**Target:** ui/kmp (and records)`

---

### Phase 4: Bookkeeping, Live Progress & Commit
- [x] Persist this plan in-repo as `.agents/plans/2026-11-05/183738_plan-frontmatter-standardization-and-templating.md` (itself using the new frontmatter) and register it in the root registry.
- [x] **Live progress list**: tick off all items in `frontmatter-progress.md`.
- [x] Stage **only** the touched paths (`git add <paths>`).
- [x] `git commit -m "Uniform frontmatter and templates for \`.agents/plans\`"` → `git push origin main`.

---

## Verification Plan

### Automated Checks:
1. Verify no orphan `sessionId:` blocks remain across `.agents/plans/`:
   ```bash
   git grep "sessionId:" -- ".agents/plans/" "ui/kmp/.agents/plans/"
   ```
2. Verify no stale links: `git grep -n "docs/tracker-migration-plan" -- . ':!memory' ':!.agents/plans/2026-11-05/164709_*'` returns nothing.
3. Verify all plan files contain `**Status:**`, `**Date:**`, and `**Target:**` on their initial lines:
   ```bash
   for f in $(find .agents/plans ui/kmp/.agents/plans -name "*.md" -not -name "README.md" -not -name "TEMPLATE.md"); do
       head -n 8 "$f" | grep -q "\*\*Status:\*\*" || echo "Missing status in $f"
       head -n 8 "$f" | grep -q "\*\*Date:\*\*" || echo "Missing date in $f"
       head -n 8 "$f" | grep -q "\*\*Target:\*\*" || echo "Missing target in $f"
   done
   ```
4. Run workspace linters and checks to ensure zero regression:
   ```bash
   mise run lint
   mise run ui:kmp:docs:readmes
   ```

### Manual Verification:
- Verify Markdown rendering of headings and metadata blocks in both `.agents/plans/` and `ui/kmp/.agents/plans/`.
