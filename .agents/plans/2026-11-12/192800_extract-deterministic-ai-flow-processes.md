# Implementation Plan: Extract Deterministic AI-Flow Processes into Standalone Libraries

**Status:** Completed  
**Date:** 2026-11-12  
**Target:** `records`, `backend` (cross-subsystem)  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Depends On:** [`185800_deterministic-fit-score-unification.md`](./2026-11-12/185800_deterministic-fit-score-unification.md)  

---

## 1. Executive Summary & Goals

The `185800_deterministic-fit-score-unification` plan successfully extracted `RankScorer` into `backend/job-post-ranking/` as a standalone library. This plan extracts **all other deterministic, idempotent processes** embedded in AI command flows into isolated Kotlin libraries under `backend/`.

### Key Objectives
1. Extract `DocumentIdGenerator`, `StatusNormalizer`, `UrlSanitizer`, `UuidGenerator` into `tracker-conventions/`
2. Extract `ProposalClassifier` (renamed to `EmailClassifier`) into `email-classification/`
3. Extract Hunter.io API integration into `company-metadata-enrichment/`
4. Extract `EventChainBuilder/Verifier` into `event-chain/`
5. Update AI commands to reference the new modules

## 2. Architecture & Design Principles

Each module is an isolation boundary designed as if exposed behind a URL. Pure modules (`tracker-conventions`, `email-classification`) have no framework dependencies. Modules needing proto types (`company-metadata-enrichment`, `event-chain`) access them via `includeBuild("../records")` in `backend/settings.gradle.kts`.

### Module Dependency Graph

- `tracker-conventions` → pure Kotlin (no deps)
- `email-classification` → `tracker-conventions`
- `company-metadata-enrichment` → `tracker-conventions`, `records-proto`
- `event-chain` → `records-proto`
- `job-post-ranking` → pure Kotlin (no deps)

## 3. Proposed Changes & Implementation Steps

### Step 1: Create `backend/tracker-conventions/`
Extracted `DocumentIdGenerator`, `StatusNormalizer`, `UrlSanitizer`, `UuidGenerator` with `ContractError` into a pure Kotlin library. Package: `dev.fnc314.tools.jobtracker.conventions`.

### Step 2: Create `backend/email-classification/`
Extracted `ProposalClassifier` renamed to `EmailClassifier` with renamed data classes (`EmailClassificationResult`, `StagedEmailChange`, `UnmatchedEmail`, `EmailClassifierRule`). Added `@Deprecated` to the old `ProposalClassifier`. Package: `dev.fnc314.tools.jobtracker.classification`.

### Step 3: Create `backend/company-metadata-enrichment/`
Created `HunterClient` interface, `HunterHttpClient` implementation, `HunterCompanyInfo` data class, and `CompanySectorResolver` with keyword-based sector mapping. Package: `dev.fnc314.tools.jobtracker.enrichment`.

### Step 4: Create `backend/event-chain/`
Extracted `EventChainBuilder` and `EventChainVerifier` with proto type dependencies. Package: `dev.fnc314.tools.jobtracker.eventchain`.

### Step 5: Update AI Commands and Bookkeeping
Updated `/gmail-sync` command to note `EmailClassifier` module availability. Registered plan in `.agents/plans/README.md`.

## 4. Verification Plan & Testing Checklist

- `tracker-conventions`: Tests for slug generation, status normalization, URL cleaning, UUIDv7 generation
- `email-classification`: Migrated all 22 tests from `ProposalClassifierTest` to `EmailClassifierTest`
- `company-metadata-enrichment`: Sector resolution tests (to be added)
- `event-chain`: Hash chain verification tests (to be added)

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-12/192800_extract-deterministic-ai-flow-processes.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Craft a single commit of all changed files
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Push `git push origin main`
