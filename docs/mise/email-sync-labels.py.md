## `email:sync-labels`

Fetch and cache email folder/labels (originates from Gmail labels, managed via Composio integration to enable swapping email providers)


- **Usage:** `email:sync-labels [FLAGS]`
- **Aliases:** `gmail:sync:labels`

### Flags
- **`-v --verbose`** — Print detailed progress and timing to stderr
- **`--label <label>`** — Fetch only this label/folder (validated against email provider labels)
- **`--since <since>`** — Lookback start date, YYYY-MM-DD
- **`--timeout <timeout>`** — Timeout in seconds per Composio email API call (default: 60)