## `gmail:collect`

Stage proposals from fetched emails to Firestore (set TRACKER_ALLOW_WRITES=1)


- **Usage:** `gmail:collect [FLAGS]`

### Flags
- **`--json`** — Inbox payload path (default: generated/gmail-inbox.json)
- **`--labels`** — JSON inventory of Gmail labels discovered by this fetch
- **`--trigger`** — Trigger source: manual | cron | application-updated
- **`--dry-run`** — Preview without writing to Firestore