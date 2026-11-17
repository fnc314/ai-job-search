## `email:collect`

Stage proposals from fetched email messages to Firestore (set TRACKER_ALLOW_WRITES=1)


- **Usage:** `email:collect [FLAGS]`
- **Aliases:** `gmail:collect`

### Flags
- **`--json <json>`** — Inbox payload path (e.g. from Composio email fetch)
- **`--labels <labels>`** — JSON inventory of email provider labels/folders
- **`--trigger <trigger>`** — Trigger source: manual | cron | application-updated
- **`--dry-run`** — Preview without writing