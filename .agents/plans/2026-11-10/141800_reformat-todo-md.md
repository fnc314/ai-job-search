# Implementation Plan: Reformat TODO.md for AI Agent Ingestibility

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** repository-wide  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261110-135319-dinw  

---

## 1. Executive Summary & Goals

Reformat `TODO.md` from a flat 5-item checklist into a structured, AI-agent-friendly document that includes complexity and priority scales, a table of contents, structured blocks per item, and a copy-pasteable template for new entries. Items are sorted from highest complexity to lowest.

## 2. Architecture & Design Principles

- **AI-agent ingestibility:** Every TODO item follows a consistent structure with Priority, Complexity, Status, Objective, Scope, Dependencies, Acceptance Criteria, and Notes.
- **Complexity scale (1–5):** Trivial → Simple → Moderate → Complex → Critical, helping agents gauge effort.
- **Priority scale:** CRITICAL, HIGH, MEDIUM, LOW, NOT_SET, helping agents triage work.
- **Sorted ordering:** Items sorted from highest complexity to lowest for natural reading order.
- **Template-driven:** Copy-pasteable template ensures future entries follow the same structure.

## 3. Proposed Changes & Implementation Steps

### Step 1: Reformat TODO.md with structured blocks, TOC, and complexity/priority scales

Rewrote TODO.md (7 → 214 lines) with the following structure:

- **Header:** Title, purpose statement, and instructions for agents on how to use the file.
- **Complexity Scale:** 5-level table (1-Trivial to 5-Critical) with descriptions.
- **Priority Scale:** 5-level table (CRITICAL to NOT_SET) with descriptions.
- **Table of Contents:** Quick-reference table with ID, Title, Priority, Complexity, Status, and Dependencies.
- **Detailed Blocks** (sorted Complexity 5→1):

1. **[TODO-001]** Kotlin-Only Migration (Complexity: 5, Priority: MEDIUM) — Incremental TDD-driven migration of 56 Python scripts in `scripts/` to Kotlin Multiplatform. Existing Python is deprecated.
2. **[TODO-002]** AI-Portable Pluginization (Complexity: 4, Priority: LOW) — Enforce `.agents/*` as the canonical and EXCLUSIVE home for all AI contributions. Any `.<AGENT>/*` directories contain only simple router files pointing to `.agents/*` equivalents.
3. **[TODO-003]** Backend Functionality Abstraction (Complexity: 3, Priority: HIGH) — Migrate existing deterministic Python functionalities and expose them to both `backend/` and `frontend/apps` projects. Migration order: NEWEST functionality first.
4. **[TODO-004]** Firestore Events Migration Investigation (Complexity: 2, Priority: LOW) — Investigate migrating `events` to a top-level collection aligned with `applications` and evaluate the value, query impact, and metrics implications. A no-op outcome is acceptable.
5. **[TODO-005]** LinkedIn Company Enrichment (Complexity: 1, Priority: HIGH) — Integrate LinkedIn to enrich `companies` collection with `industry` tags and `logo` metadata during `/scrape`.

### Step 2: Add copy-paste template for new TODO entries

Appended a clearly marked template section at the end of TODO.md with all structured fields (ID, Title, Priority, Complexity, Status, Objective, Scope, Dependencies, Acceptance Criteria, Notes) with placeholder values and brief instructions.

## 4. Verification Plan & Testing Checklist

- [x] TODO.md contains complexity and priority scales
- [x] Table of contents includes all 5 items with correct metadata
- [x] All 5 TODO blocks follow the structured format
- [x] Items are sorted from highest complexity (5) to lowest (1)
- [x] Copy-pasteable template is present at the bottom
- [x] Priorities reflect user clarifications (TODO-002: LOW, TODO-004: LOW, TODO-003: HIGH, TODO-005: HIGH)

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-10/141800_reformat-todo-md.md`
- [x] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
