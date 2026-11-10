# Investigation Plan: RM-006 Email Top-Level Collection Assessment

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** tracker  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261110-162202-1cq7  
**Depends On:** [`162700_firestore_events_migration_investigation.md`](./2026-11-10/162700_firestore_events_migration_investigation.md)  

---

## 1. Executive Summary & Recommendation

**Recommendation: Proceed — A top-level `emails` collection IS warranted for RM-006.**

Unlike the RM-004 no-op recommendation for events, a top-level `emails` collection is the correct design for RM-006's email inbox UI. The key differentiator is that RM-006 requires cross-application email queries (the inbox must browse emails across all applications), whereas all event queries are per-application. Additionally, no `emails` collection currently exists in Firestore — this is greenfield design with zero migration cost.

## 2. Current State Analysis

### Email Data Structure — No Top-Level Collection Exists

- There is **no** `emails` collection in Firestore currently.
- Email references are stored as `emailMessageIds` (string array) on each **application document** — just Gmail message IDs, not full email content.
- `gmail_fetch.py` fetches from Gmail API via Composio and writes to `generated/gmail-inbox.json` (local file cache only). It never writes to Firestore.
- The `pending-proposals` collection stores email-derived proposals awaiting review, but these are proposal documents, not email records.
- When a proposal is approved, the `emailMessageId` is appended to the application's `emailMessageIds` array and an event is recorded.

### Email Data Flow

1. `gmail_fetch.py` → `generated/gmail-inbox.json` (local cache, Gmail API data)
2. `scripts/tracker.py collect` → reads inbox JSON, creates proposals in `pending-proposals`
3. Proposal approved → `emailMessageId` appended to application's `emailMessageIds` array via `scripts/proposals.py`
4. `scripts/render.py` projects `emailMessageIds` from events into the application document
5. Frontend reads `emailMessageIds` from application document, displays as badges/links

### Current Query Patterns — All Per-Application

- `emailMessageIds` is read as part of the application document (no separate query needed).
- `frontend/apps/terminal/Main.kt` — displays email count badge and linked email IDs within the application detail view.
- `frontend/apps/cli/` — shows email count in list and detail commands.
- `frontend/features/job-application-details/` — shows `ApplicationEmailsSection` with linked email IDs.
- `scripts/render.py` — renders email message IDs into notes.
- No code queries emails independently of an application context.
- No code fetches email content by ID from Firestore (because email content isn't stored in Firestore).

### RM-006 Objective

- Introduce a UI inbox showing emails used to determine application state.
- Pull email data (from, subject, body, labels, date received) tied to existing companies and applications.
- Surface the evidence behind automated decisions.

## 3. Why RM-004 No-Op Reasoning Does NOT Apply

| Aspect | events (RM-004) | emails (RM-006) |
|--------|-----------------|-----------------|
| Current storage | Subcollection per application | IDs only, embedded in application document |
| Content in Firestore | Full event documents | None — only message IDs as references |
| RM-006 goal | N/A (investigation only) | New UI inbox with full email content |
| Cross-app queries | None exist | **Needed**: inbox must show emails across all applications |
| Data sharing | Per-application only | One email may relate to multiple applications (same company) |
| Query pattern | Always per-application | Inbox needs cross-application browsing |
| Migration cost | 147 documents to move | **Zero** — greenfield, no existing collection |

### Key Differences

1. **Cross-application queries are the primary use case**: RM-006's inbox UI must browse emails across all applications — the opposite of events where all queries are per-application.

2. **Email content is not in Firestore at all**: Currently only message IDs are stored. RM-006 requires storing full email content (from, subject, body, labels, date) in Firestore for the first time.

3. **One email may relate to multiple applications**: A single email from a company may be relevant to multiple open applications at that company. A top-level collection with back-references to applications is the natural structure.

4. **No migration needed**: Since no `emails` collection exists, there's no migration cost — this is greenfield design.

5. **Deduplication benefit**: Storing emails at the top level avoids duplicating the same email content across multiple application documents.

## 4. Proposed Top-Level Emails Collection Design

### Document Structure

```
emails/{messageId}
├── messageId: string (Gmail message ID, used as document ID)
├── from: string
├── to: string
├── subject: string
├── body: string (truncated or full text)
├── labels: string[] (Gmail labels)
├── dateReceived: timestamp
├── applicationIds: string[] (back-references to related applications)
├── companyId: string (company the email relates to)
├── syncedAt: timestamp
└── syncSource: string (e.g., "gmail_fetch")
```

### Required Indexes

- Composite index on `companyId` + `dateReceived` for company-scoped inbox views.
- Composite index on `applicationIds` (array) + `dateReceived` for application-scoped views.
- Collection group index on `dateReceived` DESCENDING for global inbox ordering.

### Write Path

- During `email:collect` or proposal approval, email content from `generated/gmail-inbox.json` is written to the top-level `emails` collection.
- The application's `emailMessageIds` array remains as a reference (no change to existing structure).
- The email document's `applicationIds` field provides the reverse lookup.

## 5. Recommendation

**Proceed with RM-006 planning for a top-level `emails` collection.**

### Reasoning

1. **Cross-application queries are the primary use case**: The inbox UI must browse emails across all applications, which requires a top-level collection.

2. **Greenfield design**: No existing `emails` collection means zero migration cost — this is purely additive.

3. **Natural data model**: One email can relate to multiple applications (same company). Top-level storage with back-references (`applicationIds` array) is the correct pattern.

4. **Deduplication**: Same email content stored once, not duplicated per-application.

5. **Content availability**: Currently only message IDs exist in Firestore. RM-006 needs full email content (from, subject, body, labels, date) for the first time.

### Next Steps

- RM-006 implementation should define the exact email document schema.
- The `email:collect` pipeline needs to be extended to write email content to Firestore.
- The inbox UI component needs to query the top-level `emails` collection with appropriate filters.

## 6. Bookkeeping

- [x] Assessment document produced with findings
- [x] RM-004 vs RM-006 contrast documented
- [x] Clear recommendation: proceed with top-level emails collection
- [x] Proposed document structure and indexes outlined
