# Implementation Plan: Interface-First Naming Policy

**Status:** Completed  
**Date:** 2026-11-14  
**Target:** repository-wide  
**Harness:** Antigravity  
**Model:** Claude Sonnet 5.5 (Low)  
**Session:** 8d551734-c2d7-4cc1-9332-9920dc74c273  

---

## 1. Executive Summary & Goals
Public surfaces are named for the interface (Remote / Local / Network / Blob / Email), never the tool. "Public" means *deployable*: anything that does work could ship to GCP. `sheet` is generic and allowed; `gmail` is being eradicated. Conversion pair: `toRemote()` / `fromRemote()`. Exemptions: KmpInspector, direct `koin` calls, `impl/`/internal adapters, external-contract identifiers.

## 2. Architecture & Design Principles
Forward-only enforcement with a shrink-only baseline ([`docs/interface-naming-baseline.txt`](../../../docs/interface-naming-baseline.txt)); existing violations are documented in [`docs/interface-naming-exceptions.md`](../../../docs/interface-naming-exceptions.md) and renamed in a follow-up plan.

## 3. Proposed Changes & Implementation Steps
- Policy: root `AGENTS.md`, `frontend/AGENTS.md` Rule 9, `records/AGENTS.md` Rule 4.
- Register: `docs/interface-naming-exceptions.md` and baseline.
- Enforcement: Konsist test, Detekt `ForbiddenClassName`, Kotlin + Python guard in `scripts/security_guards.py` (`mise run dev:guards`).

## 4. Verification Plan & Testing Checklist
- [x] `mise run dev:guards`
- [x] `mise run dev:konsist`
- [x] `mise run frontend:detekt` (verified zero `ForbiddenClassName` violations; preexisting detekt issues in untouched modules baseline-preserved)

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-14/022100_interface-first-naming-policy.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Craft a single commit of all changed files
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Push `git push origin main`
