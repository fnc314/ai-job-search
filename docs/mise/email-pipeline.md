# Email-to-Firebase Pipeline

End-to-end workflow for pulling job-related email updates and publishing them to Firestore.

## Overview

The email pipeline automates the flow of recruiter responses, interview invitations, and other job-related emails into the Firestore job tracker. It works in three stages:

1. **Fetch** — read emails from your inbox via the Composio integration
2. **Stage** — convert fetched emails into proposals staged in Firestore for review
3. **Review & Apply** — approve or reject proposals to apply changes to application records

```
Email Provider → email:read-inbox → email:collect → proposals:list → proposals:approve
   (Gmail)            (fetch)           (stage)         (review)        (apply)
```

## Prerequisites

- `TRACKER_ALLOW_WRITES=1` environment variable set for steps 2 and 3 (writes to Firestore)
- Composio email service configured and connected (see `.env.local`)
- Firebase project authenticated via `firebase login`

## Step-by-Step Pipeline

### Step 1 — Fetch: `mise run email:read-inbox`

Fetches job-related messages from your email inbox and writes a local JSON payload.

**What it does:**
- Connects to your email provider via Composio (Gmail by default)
- Filters for job-related messages (based on labels or keywords)
- Writes the fetched messages to `generated/gmail-inbox.json`

**Output:**
- `generated/gmail-inbox.json` — raw inbox payload used by the next step
- `generated/gmail-labels.json` — cached label/folder inventory

**Typical usage:**
```bash
# Fetch all job-related emails
mise run email:read-inbox

# Fetch only emails from the last 7 days
mise run email:read-inbox --since 2025-01-01

# Fetch only a specific label
mise run email:read-inbox --label "Job Applications"

# Dry run — read but write nothing
mise run email:read-inbox --dry-run
```

**Flags:**
| Flag | Description |
|---|---|
| `--since <date>` | Lookback start date (YYYY-MM-DD) |
| `--label <label>` | Fetch only this label/folder |
| `--max <n>` | Total messages to collect (fetched 50 at a time) |
| `--timeout <s>` | Timeout per API call (default: 60) |
| `--dry-run` | Read email and compare labels, but write nothing |
| `-v / --verbose` | Print detailed progress to stderr |

### Step 2 — Stage: `mise run email:collect`

Processes the fetched inbox payload and stages proposals in Firestore.

**What it does:**
- Reads the inbox payload from `generated/gmail-inbox.json`
- Matches emails to existing Firestore application records
- Creates proposals for new or updated information (status changes, new contacts, etc.)
- Proposals are stored in Firestore under the `proposals` collection with state `PENDING`

**Output:**
- Staged proposals in Firestore (`proposals` collection)
- Updated label cache in Firestore (`meta/sync` document)

**Typical usage:**
```bash
# Process the default inbox payload
TRACKER_ALLOW_WRITES=1 mise run email:collect

# Process a specific payload file
TRACKER_ALLOW_WRITES=1 mise run email:collect --json generated/gmail-inbox.json

# Dry run — preview proposals without writing
mise run email:collect --dry-run
```

**Flags:**
| Flag | Description |
|---|---|
| `--json <path>` | Inbox payload path (default: `generated/gmail-inbox.json`) |
| `--labels <path>` | JSON inventory of email labels |
| `--trigger <type>` | Trigger source: `manual`, `cron`, or `application-updated` |
| `--dry-run` | Preview proposals without writing |

### Step 3 — Review & Apply: `mise run proposals:list` / `proposals:approve`

Review staged proposals and apply approved changes to application records.

**What it does:**
- Lists all pending proposals with their proposed changes
- Allows interactive or batch approval/rejection
- On approval, writes the changes to Firestore application records as append-only events

**Typical usage:**
```bash
# List all pending proposals
mise run proposals:list

# List proposals filtered by state
mise run proposals:list --state proposal:pending

# Approve proposals interactively (walk through each one)
TRACKER_ALLOW_WRITES=1 mise run proposals:approve --interactive

# Approve a specific proposal by ID
TRACKER_ALLOW_WRITES=1 mise run proposals:approve <proposal-id>

# Reject a proposal
TRACKER_ALLOW_WRITES=1 mise run proposals:reject <proposal-id>

# Show details of a specific proposal
mise run proposals:show <proposal-id>
```

**Flags for `proposals:approve`:**
| Flag | Description |
|---|---|
| `[id]` | Proposal ID (optional with `--interactive`) |
| `--interactive` | Walk through all pending proposals one by one |
| `--note <text>` | Resolution text attached to the approval |
| `--only <indexes>` | Comma-separated change indexes to approve (partial approval) |

## Optional: Label Cache Sync

Before fetching, you can refresh the label/folder cache to ensure the pipeline uses the latest email provider labels.

```bash
mise run email:sync-labels
```

This runs `email:read-inbox` in dry-run mode, updating only the label inventory without fetching messages.

## Quick Reference

| Command | Purpose | Requires `TRACKER_ALLOW_WRITES` |
|---|---|---|
| `mise run email:sync-labels` | Refresh label cache | No |
| `mise run email:read-inbox` | Fetch emails from inbox | No |
| `mise run email:collect` | Stage proposals in Firestore | Yes |
| `mise run proposals:list` | List pending proposals | No |
| `mise run proposals:show <id>` | View proposal details | No |
| `mise run proposals:approve [-i]` | Approve and apply proposals | Yes |
| `mise run proposals:reject <id>` | Reject a proposal | Yes |
| `mise run proposals:expire` | Expire stale proposals | Yes |

## Data Flow

```
┌─────────────────┐
│  Email Provider  │  (Gmail via Composio)
└────────┬────────┘
         │ email:read-inbox
         ▼
┌─────────────────────────┐
│  generated/gmail-       │  (local JSON payload)
│  inbox.json             │
└────────┬────────────────┘
         │ email:collect
         ▼
┌─────────────────────────┐
│  Firestore:              │  (staged proposals)
│  proposals collection    │  state: PENDING
└────────┬────────────────┘
         │ proposals:approve
         ▼
┌─────────────────────────┐
│  Firestore:              │  (applied changes)
│  applications collection │  append-only events
└─────────────────────────┘
```

## Troubleshooting

- **No emails fetched:** Run `mise run email:sync-labels` first to ensure labels are cached, then re-run `email:read-inbox`.
- **No proposals staged:** Check that the inbox payload exists at `generated/gmail-inbox.json` and contains messages. Use `--dry-run` on `email:collect` to preview.
- **Write denied:** Ensure `TRACKER_ALLOW_WRITES=1` is set in your environment before running `email:collect` or `proposals:approve`.
- **Composio connection error:** Verify `.env.local` has valid Composio credentials and the email service is connected.
