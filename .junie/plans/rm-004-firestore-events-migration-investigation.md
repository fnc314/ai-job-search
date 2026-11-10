---
sessionId: session-261110-162202-1cq7
---

# Plan

## Requirements

### Overview & Goals

Two related tasks:
1. **Execute RM-004 cleanup**: Remove the unused collection group index on `events` from `firestore.indexes.json` as recommended by the completed RM-004 investigation.
2. **Assess RM-006 (top-level `emails` collection)**: Determine whether the same no-op reasoning from RM-004 applies to RM-006's proposal for a top-level `emails` collection for in-app email analysis.

### Scope

**In Scope:**
- Removing the unused `events` collection group index from `firestore.indexes.json`
- Investigating the current email data structure and query patterns
- Producing an assessment of whether a top-level `emails` collection is warranted for RM-006
- Updating the plan document with findings

**Out of Scope:**
- Implementing the RM-006 email inbox UI (that is the RM-006 implementation itself)
- Changing the email fetch pipeline (`gmail_fetch.py`)

### Current Implementation: Email Data

**Current Structure — No Top-Level `emails` Collection Exists:**
- There is **no** `emails` collection in Firestore currently.
- Email references are stored as `emailMessageIds` (string array) on each **application document** — just IDs, not full email content.
- `gmail_fetch.py` fetches from Gmail API via Composio and writes to `generated/gmail-inbox.json` (local file cache only). It never writes to Firestore.
- The `pending-proposals` collection stores email-derived proposals awaiting review, but these are proposal documents, not email records.
- When a proposal is approved, the `emailMessageId` is appended to the application's `emailMessageIds` array and an event is recorded.

**Query Patterns — All Per-Application:**
- `emailMessageIds` is read as part of the application document (no separate query needed).
- `frontend/apps/terminal/Main.kt` — displays email count badge and linked email IDs within the application detail view.
- `frontend/apps/cli/` — shows email count in list and detail commands.
- `frontend/features/job-application-details/` — shows `ApplicationEmailsSection` with linked email IDs.
- `scripts/render.py` — renders email message IDs into notes.
- No code queries emails independently of an application context.
- No code fetches email content by ID from Firestore (because email content isn't stored in Firestore).

**RM-006 Objective:**
- Introduce a UI inbox showing emails used to determine application state.
- Pull email data (from, subject, body, labels, date received) tied to existing companies and applications.
- Surface the evidence behind automated decisions.

### Key Differences from RM-004 (events)

| Aspect | events (RM-004) | emails (RM-006) |
|--------|-----------------|-----------------|
| Current storage | Subcollection per application | IDs only, embedded in application document |
| Content in Firestore | Full event documents | None — only message IDs as references |
| RM-006 goal | N/A (investigation only) | New UI inbox with full email content |
| Cross-app queries | None exist | **Needed**: inbox must show emails across all applications |
| Data sharing | Per-application only | One email may relate to multiple applications (same company) |
| Query pattern | Always per-application | Inbox needs cross-application browsing |

## Technical Design

### Current Implementation

**firestore.indexes.json (lines 36-43):**
The `events` collection group index is defined but never used:
```json
{
  "collectionGroup": "events",
  "queryScope": "COLLECTION_GROUP",
  "fields": [
    { "fieldPath": "type", "order": "ASCENDING" },
    { "fieldPath": "ts", "order": "DESCENDING" }
  ]
}
```
Additionally, two field overrides exist for `events` (lines 115-123) that disable indexing on `note` and `metadata` fields.

**Email data flow:**
1. `gmail_fetch.py` → `generated/gmail-inbox.json` (local cache, Gmail API data)
2. `scripts/tracker.py collect` → reads inbox JSON, creates proposals in `pending-proposals`
3. Proposal approved → `emailMessageId` appended to application's `emailMessageIds` array
4. Frontend reads `emailMessageIds` from application document, displays as badges/links

### Key Decisions

**RM-006 Assessment: Top-level `emails` collection IS warranted (unlike events).**

The no-op recommendation from RM-004 does **not** apply to RM-006 because:

1. **Cross-application queries are the primary use case**: RM-006's inbox UI must browse emails across all applications — the opposite of events where all queries are per-application.
2. **Email content is not in Firestore at all**: Currently only message IDs are stored. RM-006 requires storing full email content (from, subject, body, labels, date) in Firestore for the first time.
3. **One email may relate to multiple applications**: A single email from a company may be relevant to multiple open applications at that company. A top-level collection with back-references to applications is the natural structure.
4. **No migration needed**: Since no `emails` collection exists, there's no migration cost — this is greenfield design.
5. **Deduplication benefit**: Storing emails at the top level avoids duplicating the same email content across multiple application documents.

### Proposed Changes

**Step 1: Remove unused events index**
- Remove the `events` collection group index from `firestore.indexes.json` (lines 36-43).
- Remove the `events` field overrides from `firestore.indexes.json` (lines 115-123) since they only matter for collection group queries.

**Step 2: RM-006 assessment document**
- Document the findings that a top-level `emails` collection is warranted for RM-006.
- Contrast with RM-004 reasoning to show why the assessment differs.
- Recommend proceeding with RM-006 planning.

### File Structure

| File | Action |
|------|--------|
| `firestore.indexes.json` | Modify: remove events collection group index and field overrides |
| `.junie/plans/rm-004-firestore-events-migration-investigation.md` | Update: add RM-006 assessment |

## Testing

### Validation Approach
- Verify `firestore.indexes.json` is valid JSON after removal.
- Confirm no code references the removed index (already confirmed: zero `collection_group()` calls for `events`).
- Verify the RM-006 assessment correctly contrasts with RM-004 findings.

### Key Scenarios
- The `events` index removal doesn't break any existing query (confirmed: all event queries use subcollection path, not collection group).
- The RM-006 assessment correctly identifies that cross-application email queries are the primary use case, unlike events.

### Edge Cases
- The `events` field overrides (disabling `note` and `metadata` indexing) are only relevant for collection group queries; removing them is safe since the collection group index itself is removed.

# Delivery Steps

### ✓ Step 1: Produce investigation document and recommendation
Create the RM-004 investigation document analyzing the events migration question and delivering a recommendation.

The investigation will:
- Document the current events structure (subcollection under each application, 116 apps, 147 events total, avg 1.3 per app)
- Catalog all code paths that read/write events (scripts/events.py, cache.py, alignment.py, proposals.py, reconcile_submissions.py, backend/BackendService.kt)
- Confirm that zero cross-application event queries exist — the collection group index on `events` is deployed but unused
- Analyze query impact: top-level events would require `applicationId` field + composite index for per-app queries, adding complexity without benefit since all reads are already per-application
- Evaluate metrics: top-level would increase document reads (no change), add index maintenance cost for applicationId field, and complicate hash chain verification which is inherently per-application
- Assess migration cost: 147 documents to move, plus updating all write paths (appendEvent, proposals.approve, BackendService.appendEventTransactional) and read paths (readEvents, cache loading)
- Compare against the companies migration precedent: companies benefited from top-level because attributes are shared; events are per-application append-only logs with no sharing benefit
- Deliver clear recommendation with documented reasoning
- Save the investigation document to `.agents/plans/` following the plan naming conventions, and update the ROADMAP.md status for RM-004

### ✓ Step 2: Execute RM-004 cleanup and RM-006 assessment
Remove the unused events collection group index and produce the RM-006 assessment.

- Remove the `events` collection group index from `firestore.indexes.json` (lines 36-43).
- Remove the `events` field overrides from `firestore.indexes.json` (lines 115-123).
- Verify `firestore.indexes.json` remains valid JSON.
- Produce the RM-006 assessment documenting that a top-level `emails` collection **is** warranted (unlike events), with reasoning:
  - Cross-application email queries are the primary RM-006 use case (inbox UI).
  - Email content is not currently in Firestore — this is greenfield, no migration cost.
  - One email may relate to multiple applications, making top-level storage with back-references the natural design.
  - Dedupulation benefit: same email content stored once, not per-application.
- Update this plan file with the final assessment.