# Investigation Plan: Firestore Events Migration — No-Op Recommendation

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** tracker  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261110-162202-1cq7  

---

## 1. Executive Summary & Recommendation

**Recommendation: No-Op — Do not migrate `events` to a top-level collection.**

Migrating `events` from `applications/{id}/events/{eventId}` (subcollection) to `events/{eventId}` (top-level collection) provides no measurable benefit and introduces unnecessary complexity. All event queries in the codebase are scoped to a single application, the hash chain algorithm is inherently per-application, and no cross-application event queries exist or are planned.

## 2. Current State Analysis

### Data Volume

| Metric | Value |
|--------|-------|
| Total applications | 116 |
| Total events | 147 |
| Average events per application | 1.3 |
| Max events in a single application | 5 |

### Current Structure

Events live in `applications/{applicationId}/events/{eventId}` as a subcollection of each application document. Each event document contains:

- `eventId` — unique identifier
- `sequenceNumber` — ordinal position within the application's event chain
- `prevEventHash` — SHA-256 hash of the previous event (cryptographic chain)
- `eventHash` — SHA-256 hash of the current event
- `type` — event type enum (APPLY, ACKNOWLEDGE, etc.)
- `statusFrom`, `statusTo` — application status transition
- `actor` — who/what triggered the event (USER, EMAIL_SYNC, etc.)
- `derivedFrom`, `confidence`, `note` — metadata about the event source
- `emailMessageId` — Gmail message ID if email-derived
- `metadata` — additional structured data
- `ts`, `date` — timestamps

### Query Patterns — Exhaustive Catalog

All event queries in the codebase are **per-application**:

| File | Function | Operation | Scope |
|------|----------|-----------|-------|
| `scripts/events.py` | `readEvents()` | Read all events | Single application subcollection via `.stream()` |
| `scripts/events.py` | `appendEvent()` | Write one event | Single application subcollection |
| `scripts/cache.py` | Cache loading | Read events | Iterates applications, then reads each application's events subcollection |
| `scripts/alignment.py` | Alignment | Read/write events | Single application at a time |
| `scripts/proposals.py` | Proposal approval | Read/write events | Single application at a time |
| `scripts/reconcile_submissions.py` | Reconciliation | Read events | Single application at a time |
| `backend/BackendService.kt` | `appendEventTransactional()` | Transactional write | Single application's events subcollection |
| `frontend/` | — | No direct queries | Uses backend service or cached data |

### Key Finding: Zero Cross-Application Event Queries

- The collection group index on `events` (defined in `firestore.indexes.json`, lines 37-43) is **deployed but never used**.
- No `collection_group()` call exists anywhere in the codebase.
- There is no dashboard, report, or analytics feature that queries events across all applications.
- All event reads are scoped to a single application's subcollection.

## 3. Query Impact Analysis

### Current: Subcollection Queries

Reading events for one application:
```python
db.collection('applications').document(app_id).collection('events').stream()
```
- **Cost:** 1 read per event document
- **Index:** Uses the default `__name__` index (no custom index needed)
- **Complexity:** O(n) where n = events for that application

### Hypothetical: Top-Level Collection Queries

Reading events for one application would require:
```python
db.collection('events').where('applicationId', '==', app_id).stream()
```
- **Cost:** 1 read per event document (same)
- **Index:** Requires a composite index on `applicationId`
- **Complexity:** O(n) where n = events for that application (same)
- **Additional cost:** Every event document must carry an `applicationId` field (redundant with the current path-based scoping)

### Comparison

| Aspect | Subcollection (Current) | Top-Level (Hypothetical) |
|--------|------------------------|-------------------------|
| Read cost per query | 1 read/event | 1 read/event |
| Index requirement | None (default `__name__`) | Composite index on `applicationId` |
| Document size | As-is | +`applicationId` field per document |
| Path-based scoping | Natural (parent path) | Explicit field required |
| Cross-app query support | Not supported | Supported (but unused) |

**Conclusion:** Top-level events add index maintenance cost and document storage overhead without improving any existing query. The only theoretical benefit — cross-application queries — is not used.

## 4. Metrics & Cost Implications

### Read/Write Costs

- **Document reads:** No change. All queries read the same number of documents regardless of structure.
- **Document writes:** No change. Event append operations write one document regardless of structure.
- **Index maintenance:** **Increase.** A top-level collection would require a composite index on `applicationId`, adding index storage and write amplification.

### Hash Chain Verification

The hash chain algorithm (sequenceNumber, prevEventHash, eventHash) is inherently per-application. Moving to a top-level collection:

- Requires adding `applicationId` to every event document for chain scoping
- Complicates chain verification, which currently relies on the parent path to identify the application scope
- Adds no benefit since chains never span across applications

### Migration Cost

- **147 documents** to move
- **All write paths** must be updated: `appendEvent()`, `proposals.approve()`, `BackendService.appendEventTransactional()`
- **All read paths** must be updated: `readEvents()`, cache loading, alignment, reconciliation
- **Schema change** required: add `applicationId` field to all event documents
- **Index deployment** required: composite index on `applicationId`
- **Risk:** Migration window introduces potential for data inconsistency

## 5. Precedent Analysis: Companies Migration

The `companies` collection was successfully migrated to top-level (RM-005, completed). However, the rationale does not apply to events:

| Aspect | Companies | Events |
|--------|-----------|--------|
| Data sharing | Shared across applications (one company, many applications) | Per-application only (no sharing) |
| Query pattern | Looked up by company name/slug independently | Always queried within application context |
| Data characteristics | Static attributes (name, industry, logo) | Append-only log with cryptographic chain |
| Migration benefit | Eliminated duplication, enabled cross-app lookups | No duplication to eliminate, no cross-app queries |

**Conclusion:** The companies migration succeeded because company data is shared and static. Events are per-application append-only logs with no sharing benefit.

## 6. Recommendation

**No-Op: Keep events as a subcollection under each application.**

### Reasoning

1. **No query benefit:** All event queries are per-application. A top-level collection provides no performance improvement for the existing query patterns.

2. **Increased complexity:** Moving to top-level requires adding `applicationId` to every event document, deploying a composite index, and updating all read/write paths across the codebase.

3. **Hash chain alignment:** The cryptographic hash chain is inherently per-application. The subcollection structure naturally enforces this scoping without requiring an explicit `applicationId` field.

4. **Unused infrastructure:** The collection group index on `events` is deployed but never used. Removing it would simplify the index configuration, not adding a top-level collection.

5. **Migration risk vs. zero benefit:** Moving 147 documents with zero query improvement is a net loss. The migration effort (updating all code paths, schema changes, index deployment) carries risk with no compensating benefit.

6. **Data volume is low:** With 1.3 events per application on average and a maximum of 5, the subcollection structure is well-suited to the data volume. Firestore subcollections are designed for this exact pattern.

### Action Items

- **Remove the unused collection group index** on `events` from `firestore.indexes.json` to simplify index configuration.
- **Close RM-004** as completed with a no-op recommendation.

## 7. Bookkeeping

- [x] Investigation document produced with findings
- [x] Query impact analysis completed (performance comparison before/after)
- [x] Metrics implications evaluated (read/write costs, index costs)
- [x] Clear recommendation: no-op
- [x] No-op reasoning is documented
