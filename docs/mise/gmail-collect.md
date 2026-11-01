## `gmail:collect`

Stage proposals from fetched emails to Firestore (set TRACKER_ALLOW_WRITES=1)


- **Usage:** `gmail:collect [FLAGS]`

### Flags
- **`--json <json>`** — Inbox payload path (default: generated/gmail-inbox.json)
- **`--labels <labels>`** — JSON inventory of Gmail labels discovered by this fetch
- **`--trigger <trigger>`** — Trigger source: manual | cron | application-updated
- **`--dry-run`** — Preview without writing to Firestore