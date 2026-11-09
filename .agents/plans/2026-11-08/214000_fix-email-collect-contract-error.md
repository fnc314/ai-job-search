# Implementation Plan: Fix Email Collect Contract Error

**Status:** Completed
**Date:** 2026-11-08
**Target:** tracker
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)

---

## 1. Executive Summary & Goals

`scripts/proposals.py` produces proposal documents with **namespaced enum values** that don't match the canonical enum names defined in `tracker.schema.json`. The `contract.validate("proposal", proposal)` call catches the mismatch, causing `mise run //:email:collect` to fail.

**Goal:** Align all enum values in `scripts/proposals.py`, `lib/proposals.py`, and `scripts/tracker.py` with the canonical enum names from `tracker.schema.json`.

## 2. Root Cause Analysis

### Mismatches Found

| Field | Code Produces | Schema Expects |
|---|---|---|
| `kind` | `proposal:gmail-sync` | `EMAIL_SYNC` |
| `state` | `proposal:pending` | `PENDING` |
| `trigger` | `trigger:manual` | `MANUAL` |
| `eventType` (rejection) | `event:rejection` | `REJECT` |
| `eventType` (assessment) | `event:assessment` | `ASSESS` |
| `eventType` (offer) | `event:offer` | `OFFER` |
| `eventType` (interview) | `event:interview-scheduled` | `SCHEDULE_INTERVIEW` |
| `eventType` (acknowledged) | `event:acknowledged` | `ACKNOWLEDGE` |
| `eventType` (outreach) | `event:recruiter-outreach` | `RECRUITER_OUTREACH` |
| `statusTo` (rejected) | `status:rejected` | `REJECTED` |
| `statusTo` (offer) | `status:offer` | `OFFER_EXPECTED` |
| `statusTo` (interview) | `status:interview` | `INTERVIEW` |
| `confidence` | `high` / `medium` / `low` | `HIGH_CONFIDENCE` / `MEDIUM_CONFIDENCE` / `LOW_CONFIDENCE` |

### Additional issues in `approve()` / `reject()` / `expire()`

- `approve()` sets `state` to `proposal:approved` (should be `APPROVED`)
- `reject()` sets `state` to `proposal:rejected` (should be `REJECTED`)
- `expire()` sets `state` to `proposal:expired` (should be `EXPIRED`)
- `approve()` uses `actor:gmail-sync` and `derivedFrom:gmail` — these are NOT in the schema enums (`actorValue` has `EMAIL_SYNC`, `derivedFromValue` has `EMAIL`)
- `approve()` lead records use `status:lead` (should be `LEAD`), `channel:outreach:email` (should be `OUTREACH_EMAIL`)
- `commandCollect` default trigger is `trigger:manual` (should be `MANUAL`)
- `commandSyncDue` passes `trigger:cron` (should be `CRON`)

## 3. Proposed Changes & Implementation Steps

### Files Modified

1. **`scripts/proposals.py`** — primary fix location
2. **`lib/proposals.py`** — virtualenv import copy (runtime import path)
3. **`scripts/tracker.py`** — fix default trigger values

### Changes in `proposals.py` (both `scripts/` and `lib/`)

**CLASSIFIERS table:** Replaced all namespaced values with canonical enum names:
- `event:rejection` → `REJECT`, `status:rejected` → `REJECTED`, `high` → `HIGH_CONFIDENCE`
- `event:offer` → `OFFER`, `status:offer` → `OFFER_EXPECTED`, `high` → `HIGH_CONFIDENCE`
- `event:interview-scheduled` → `SCHEDULE_INTERVIEW`, `status:interview` → `INTERVIEW`, `medium` → `MEDIUM_CONFIDENCE`
- `event:assessment` → `ASSESS`, `medium` → `MEDIUM_CONFIDENCE`
- `event:acknowledged` → `ACKNOWLEDGE`, `low` → `LOW_CONFIDENCE`
- `event:recruiter-outreach` → `RECRUITER_OUTREACH`, `medium` → `MEDIUM_CONFIDENCE`

**Constants & functions:**
- `PENDING` constant: `proposal:pending` → `PENDING`
- `buildProposal()`: `kind: proposal:gmail-sync` → `kind: EMAIL_SYNC`, default trigger `trigger:manual` → `MANUAL`
- `approve()`: `actor:gmail-sync` → `EMAIL_SYNC`, `derivedFrom:gmail` → `EMAIL`, `status:lead` → `LEAD`, `channel:outreach:email` → `OUTREACH_EMAIL`, `state: proposal:approved` → `APPROVED`
- `reject()`: `proposal:rejected` → `REJECTED`
- `expire()`: `proposal:expired` → `EXPIRED`

### Changes in `scripts/tracker.py`

- `commandCollect` default trigger: `trigger:manual` → `MANUAL`
- `commandSyncDue` trigger: `trigger:cron` → `CRON`

## 4. Verification Plan & Testing Checklist

- [x] Run `mise run //:email:collect --dry-run` to verify the proposal validates without contract errors
- [x] Confirm proposal output uses `kind: EMAIL_SYNC`, `state: PENDING`, `trigger: MANUAL`, and canonical event types and confidence values

## 5. Bookkeeping

- [x] Plan persisted at `.agents/plans/2026-11-08/214000_fix-email-collect-contract-error.md`
- [x] Registry `README.md` row added / status synchronized
