## `gmail:read:inbox`

Fetch job-related emails from Gmail and write inbox payload


- **Usage:** `gmail:read:inbox [FLAGS]`

### Flags
- **`-v --verbose`** — Print detailed progress and timing to stderr
- **`--label`** — Fetch only this label (validated against Gmail labels)
- **`--since`** — Lookback start date, YYYY-MM-DD
- **`--max`** — Total messages to collect, fetched 50 at a time
- **`--timeout`** — Timeout in seconds per Composio API call (default: 60)
- **`--dry-run`** — Read Gmail and compare labels, but write nothing