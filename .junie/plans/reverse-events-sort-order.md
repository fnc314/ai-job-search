---
sessionId: session-261110-193849-p5f8
---

# Summary

### Root Cause Found: Events have `DATA MISSING` dates and sort order is broken

**Problem:** The `mapRecordToTrackerEvent()` function in `RemoteJobApplicationDetailsDataSourceImpl.kt` reads `record.fields["createdAt"]` to parse the event timestamp. However, the Firestore event schema uses `ts` (write timestamp) and `date` (event date) — there is no `createdAt` field on event documents. As a result, every event gets `Instant.DISTANT_PAST` as its `createdAt`, which:
1. Causes `formatRelativeInstant()` to return `null`, displaying `DATA MISSING` in the UI
2. Makes `sortedByDescending { it.createdAt }` a no-op since all events have the same sort key

**Fix:** Update `mapRecordToTrackerEvent()` to read `ts` (preferred) or `date` from the Firestore event document, matching the schema at `tracker.schema.json` lines 840-844.

# Delivery Steps

### ✓ Step 1: Reverse event sort order
Already implemented and verified. Events sorted by createdAt descending in deriveTimelineEvents().

### ✓ Step 2: Fix event createdAt mapping
Updated `mapRecordToTrackerEvent()` to read `ts` (preferred) or `date` from Firestore event documents instead of the non-existent `createdAt` field.

### ✓ Step 3: Verify build and run tests
`mise run frontend:build` — BUILD SUCCESSFUL
`mise run frontend:test` — BUILD SUCCESSFUL (all tests passed)