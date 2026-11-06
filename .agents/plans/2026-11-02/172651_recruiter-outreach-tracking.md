# Implementation Plan: Recruiter Outreach Data Models & Pipeline Tracking

**Status:** Completed  
**Date:** 2026-11-02  
**Target:** records and tracker (CLI & Schema)  

---

# Requirements

### Overview & Goals

Support tracking recruiter-initiated outreach (e.g., a LinkedIn message from a Capital One recruiter) as first-class records in the job tracker. This enables capturing leads before a concrete job posting exists, tracking conversations outside the automated `/scrape` pipeline, and managing the full lifecycle from initial contact through application.

### User Stories

1. As a candidate, I want to quickly record a recruiter outreach via CLI so I don't lose track of conversations.
2. As a candidate, I want to see leads alongside applications in the TUI and web dashboard so I have a single view of all ongoing processes.
3. As a candidate, I want to filter by status (leads, rejected, etc.) in the TUI and web dashboard so I can focus on specific subsets.
4. As a candidate, I want to transition a lead to an application when a concrete role emerges.

### Scope

**In Scope:**
- New `status:lead` status in the funnel
- Namespaced channel values (`channel:outreach:linkedin`, `channel:outreach:email`, etc.)
- New `process:recruiter:outreach` tag
- New `tracker.py lead` CLI subcommand
- TUI quick-filter keybindings for status-based views
- TUI showing all entries (leads + applications) in a unified list
- Web dashboard showing all entries with lead status support and quick-filter buttons

**Out of Scope:**
- `add --interactive` / `-i` flag (planned for a follow-up)
- Automated outreach record creation (all outreach proposals require human approval)

### Functional Requirements

1. `tracker.py lead` creates a minimal application record with `status:lead`, the specified company, channel, and optional contact/note.
2. Leads are filterable via `tracker.py list --status lead` and the TUI quick-filter.
3. Leads transition to `status:drafting` or `status:applied` via `tracker.py status <id> drafting`.
4. TUI shows all entries sorted newest-first, with quick-filter keys to narrow by status category.
5. Web dashboard shows all entries sorted newest-first, with quick-filter buttons and a lead status card.
6. Gmail `collect` detects recruiter outreach emails (no matching application) and stages them as lead proposals for human review.
7. `proposals approve` creates a lead record from an approved outreach proposal.
8. TUI and web dashboard show a "pending proposals" badge when outreach proposals await review.

# Technical Design

### Current Implementation

- **Schema** (`tracker.schema.json`): Closed enums for `statusValue` (9 values), `channelValue` (8 values), and `tagValue` (16 values). Schema version 4.
- **CLI** (`scripts/tracker.py`): Subcommands `add`, `upsert`, `status`, `event`, `score`, `artifact`, `list`, `get`, etc. All writes require `TRACKER_ALLOW_WRITES=1`.
- **Contract** (`scripts/contract.py`): Single loader for schema enums; `normalizeStatus()`, `deriveStatusFields()`, and JSON Schema validation.
- **TUI** (`ui/terminal/tracker_tui.py`): curses-based table with configurable columns. Uses `scripts/reader.py` for data.

### Key Decisions

1. **Channel namespace `channel:{direction}:{source}`** — e.g., `channel:outreach:linkedin`, `channel:outreach:email`. Groups by direction first. Existing flat channels remain for backward compatibility.

2. **`status:lead` as pre-funnel** — Rank 0 (before `status:drafting` which moves to rank 1). Non-terminal.

3. **`process:recruiter:outreach` tag** — Marks records from recruiter-initiated contact.

4. **Schema version bump to 5** — Adding enum values requires a schemaVersion bump per convention.

### Proposed Changes

**Schema (`tracker.schema.json`):**
- Bump `schemaVersion` from 4 to 5
- Add `status:lead` to `statusValue` enum with rank 0; shift existing ranks up by 1
- Add `channel:outreach:linkedin`, `channel:outreach:email`, `channel:outreach:referral` to `channelValue`
- Add `process:recruiter:outreach` to `tagValue`
- Add `event:recruiter-outreach` to `eventTypeValue` enum
- Add `proposal:lead` to `proposalKindValue` enum for lead-type proposals
- Update `application.schemaVersion` const from 4 to 5
- Add revision log entry for v5

**CLI (`scripts/tracker.py`):**
- New `lead` subcommand: `--company` (required), `--channel` (default `outreach:linkedin`), `--contact`, `--note`, `--dry-run`
- `commandLead()` handler: builds minimal payload with `status:lead`, calls `buildApplication()`, writes to Firestore
- Update `normalizeChannel()` to accept namespaced values
- No changes to `collect` flow: it continues to call `proposalsModule.buildProposal()` which now handles outreach detection

**TUI (`ui/terminal/tracker_tui.py`):**
- Unified list: all entries sorted by `createdAt` descending
- Quick-filter keys: `l` leads, `o` open, `r` rejected, `a` all
- Visual distinction for `status:lead` entries

**Web Dashboard (`ui/web/application-dashboard.html`):**
- Add `--lead` CSS variable (e.g., `#a855f7` purple) for lead status styling
- Add lead summary card in the cards section (between Active and Interview)
- Add `.badge.lead` CSS class for lead status badges
- Add quick-filter buttons above the table: All, Leads, Open, Rejected
- Update funnel chart to include lead stage
- Ensure table sorts newest-first (already the case in current static data)
- Add JavaScript for quick-filter button handling to show/hide rows by status

**Proposals module (`scripts/proposals.py`):**
- Add `event:recruiter-outreach` classifier with patterns for recruiter outreach (e.g., "came across your profile", "we have opportunities", "would you be interested", "exciting role")
- When an outreach email has no matching application, create a `lead` change instead of `unmatched` entry
- The `lead` change includes: company (extracted from sender/subject), channel (`channel:outreach:email`), source email metadata
- On `proposals approve`, if a change has `eventType: event:recruiter-outreach` and no `applicationId`, create a new lead record instead of emitting an event

**New file (`scripts/specs/lead.usage.kdl`):**
- CLI spec for zsh completions

### Architecture Diagram

```mermaid
graph LR
    CLI[tracker.py lead] --> Build[buildApplication]
    Gmail[Outreach email] --> Fetch[gmail_fetch.py]
    Fetch --> Inbox[gmail-inbox.json]
    Inbox --> Collect[tracker.py collect]
    Collect --> Classify[proposals.py classify]
    Classify --> |outreach no match| LeadProp[lead proposal]
    Classify |existing app| ChangeProp[change proposal]
    LeadProp --> Stage[pending-proposals]
    ChangeProp --> Stage
    Stage --> Review[proposals list/show]
    Review --> Approve[proposals approve]
    Approve --> |lead| Build
    Approve --> |change| Emit[emit events]
    Build --> Contract[contract.py validation]
    Contract --> Schema[tracker.schema.json v5]
    Build --> Firestore[(Firestore)]
    Emit --> Firestore
    Firestore --> Reader[reader.py]
    Reader --> TUI[TUI unified list]
    TUI --> TFilter[TUI quick-filter keys]
    Reader --> WD[Web Dashboard]
    WD --> WFilter[Web quick-filter buttons]
```

### Files Modified

| File | Change |
|---|---|
| `tracker.schema.json` | Schema v5, new enums (lead status, outreach channels, outreach event, lead proposal kind) |
| `scripts/tracker.py` | New `lead` subcommand, updated `normalizeChannel` |
| `scripts/proposals.py` | New outreach classifier, lead proposal staging, lead creation on approve |
| `ui/terminal/tracker_tui.py` | Unified list, quick-filters, lead styling, proposals badge |
| `ui/web/application-dashboard.html` | Lead card, lead badge, quick-filter buttons, proposals badge |
| `scripts/specs/lead.usage.kdl` | New: CLI spec |

# Testing

### Key Scenarios

1. **Create a lead via CLI**: `tracker.py lead --company "Capital One" --channel outreach:linkedin --contact "Recruiter" --note "LinkedIn message"` → returns JSON with `status:lead`.
2. **Gmail detects outreach**: `gmail_fetch.py` fetches a recruiter email → `tracker.py collect` stages it as a lead proposal → `proposals list` shows the pending lead → `proposals approve` creates the lead record.
3. **List leads**: `tracker.py list --status lead` → shows only leads.
4. **Transition**: `tracker.py status <id> drafting` → moves to `status:drafting` with event.
5. **TUI unified view**: leads and applications mixed, sorted newest first.
6. **TUI quick-filter**: `l` shows leads only, `a` shows all.
7. **TUI proposals badge**: Badge shows count of pending proposals including lead proposals.
8. **Web dashboard lead card**: Dashboard shows a "Lead" card with count of lead records.
9. **Web dashboard quick-filter**: Click "Leads" button → only lead rows shown; click "All" → all shown.

### Edge Cases

1. Lead without a role (company only) → ID is company slug.
2. Existing `schemaVersion: 4` records remain readable.
3. Existing flat channels (`channel:linkedin`) still work in `normalizeChannel()`.
4. Web dashboard quick-filter buttons should handle 0 leads gracefully (show card with 0 count).
5. Outreach email from a company that already has an application → classified as a normal change (not a lead), matched to existing application.
6. Outreach email with no identifiable company → staged as unmatched with reason "no company identified in outreach".
7. Multiple outreach emails from the same company before approval → each staged separately; user approves one, duplicates are noted.
8. Proposal approve for lead type creates the record with `actor:gmail-sync` and `derivedFrom:gmail` provenance.

# Delivery Steps

### ✓ Step 1: Update schema to v5 with lead status, namespaced channels, and outreach event
Add `status:lead`, namespaced channel values, outreach event type, and lead proposal kind to the tracker schema.
- Bump `schemaVersion` from 4 to 5 in `tracker.schema.json`.
- Add `status:lead` to `statusValue` enum with rank 0; shift all existing status ranks up by 1.
- Add `channel:outreach:linkedin`, `channel:outreach:email`, `channel:outreach:referral` to `channelValue`.
- Add `process:recruiter:outreach` to `tagValue`.
- Add `event:recruiter-outreach` to `eventTypeValue` enum.
- Add `proposal:lead` to `proposalKindValue` enum.
- Update `application.schemaVersion` const from 4 to 5.
- Add revision log entry for v5.

### ✓ Step 2: Implement tracker.py lead subcommand
Add the `lead` CLI subcommand to create minimal lead records from recruiter outreach.
- Add `lead` subparser to `buildParser()` in `scripts/tracker.py` with `--company`, `--channel`, `--contact`, `--note`, `--dry-run`.
- Implement `commandLead()`: builds payload with `status:lead`, calls `buildApplication()`, writes to Firestore.
- Update `normalizeChannel()` to accept namespaced values like `outreach:linkedin`.
- Create `scripts/specs/lead.usage.kdl` for completions.

### ✓ Step 3: Add Gmail outreach detection to proposals module
Enable `collect` to detect recruiter outreach emails and stage them as lead proposals for human review.
- Add `event:recruiter-outreach` classifier to `CLASSIFIERS` in `scripts/proposals.py` with patterns for outreach (e.g., "came across your profile", "we have opportunities", "would you be interested").
- In `buildProposal()`: when an outreach email has no matching application, create a `lead` change entry instead of `unmatched`.
- The `lead` change includes: company extracted from sender/subject, channel `channel:outreach:email`, source email metadata.
- In `approve()`: when a change has `eventType: event:recruiter-outreach` and no `applicationId`, create a new lead record (not just an event) with `actor:gmail-sync` provenance.
- Outreach emails from companies with existing applications continue to be matched normally (not treated as leads).

### ✓ Step 4: Update TUI for unified list, quick-filters, and proposals badge
Make the TUI show all entries with quick-filter keybindings and a pending proposals indicator.
- Update `tracker_tui.py` to fetch all applications and sort by `createdAt` descending.
- Add quick-filter keys: `l` leads, `o` open, `r` rejected, `a` all.
- Add visual distinction for `status:lead` entries (different color or "LEAD" prefix).
- Update status column rendering for the new `lead` status.

### ✓ Step 5: Update web dashboard for lead status, quick-filters, and proposals badge
Add equivalent lead support, quick-filter buttons, and proposals indicator to the web dashboard.
- Add `--lead` CSS variable (e.g., `#a855f7`) to `:root` in `application-dashboard.html`.
- Add `.card.lead` and `.badge.lead` CSS classes for lead styling.
- Add a "Lead" summary card in the cards section (between Active and Interview).
- Add quick-filter buttons above the table: All, Leads, Open, Rejected.
- Add JavaScript to handle quick-filter button clicks to show/hide table rows by status.
- Update funnel chart to include lead stage.
- Add a pending proposals badge or notification area.

### ✓ Step 6: Validate end-to-end workflow
Verify the complete lead workflow from CLI creation, Gmail detection, through TUI/web display.
- Run `tracker.py lead` and verify the Firestore record.
- Run `tracker.py list --status lead` and verify filtering.
- Run `tracker.py status <id> drafting` and verify the transition event.
- Test Gmail outreach detection: create a test inbox payload with an outreach email, run `collect`, verify the lead proposal is staged, approve it, and verify the lead record is created.
- Open TUI and verify unified list with quick-filters and proposals badge.
- Open web dashboard and verify lead card, lead badge styling, quick-filter buttons, and proposals badge.
- Regenerate mise docs via `mise run mise:docs:tasks`.