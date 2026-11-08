## `tracker:status`

Record a funnel status change


- **Usage:** `tracker:status [FLAGS] <id> <status>`

### Arguments
- **`<id>`** — Application ID
- **`<status>`** — Target status

### Flags
- **`--date <date>`** — ISO date the change happened
- **`--note <note>`** — What happened
- **`--correction`** — Emit status-corrected event
- **`--dry-run`** — Preview without writing