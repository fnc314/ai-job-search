---
sessionId: session-261110-153958-1i4a
---

# Roadmap Review

Review `ROADMAP.md` and ensure all roadmap items have up-to-date links to their related implementation plans in `.agents/plans/`.

# Delivery Steps

### ✓ Step 1: Add missing plan links to RM-001, RM-002, RM-003, and RM-005
Update the "Linked Plans" sections in ROADMAP.md for each roadmap item to include all relevant completed plans.

- **RM-001** (Kotlin-Only Migration): Add `hunter-logo-integration.md`, `fix-email-collect-contract-error.md`, and `fix-event-type-and-email-pipeline-docs.md` which migrated Python scripts to Kotlin.
- **RM-002** (AI-Portable Pluginization): Add `plan-relocation-and-index-alignment.md`, `plan-files-consolidation-and-gitignore-alignment.md`, `plan-frontmatter-standardization-and-templating.md`, and `mark-detekt-and-konsist-plan-completed.md` which established the `.agents/` canonical structure.
- **RM-003** (Backend Functionality Abstraction): Add `160200_retroactive-company-enrichment.md` (backend batch enrichment endpoints) and `164500_post_monorepo_quality_gates_and_cloud_run_deployment_plan.md` (Cloud Run deployment).
- **RM-005** (LinkedIn Company Enrichment): Add `160200_retroactive-company-enrichment.md` (retroactive backfill) and `hunter-logo-integration.md` (logo metadata).

### ✓ Step 2: Update RM-005 changelog and verify consistency
Add a changelog entry to RM-005 for the retroactive enrichment completion and verify all status values match.

- Add changelog entry: "2026-11-10 | Progress update | Retroactive batch enrichment of existing company records, frontend LinkedIn URL display, and sector to industry mapping fix".
- Verify that RM-005 Status is DONE and Completion % is 100 (already correct).
- Verify Table of Contents row matches the detailed block.