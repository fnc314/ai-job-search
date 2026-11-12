## `email:discover`

Scan cached emails for untracked job applications and stage discovery proposals (set TRACKER_ALLOW_WRITES=1)


- **Usage:** `email:discover [FLAGS]`

### Flags
- **`--all`** — Scan all cached emails, not just recent ones
- **`--since <date>`** — Only scan emails since this date (YYYY-MM-DD)
- **`--dry-run`** — Preview discovery candidates without writing proposals