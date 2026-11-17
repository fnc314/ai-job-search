## `search:scrape`

Search job portals and collect new listings (markdown output, save-seen to Firestore by default)


- **Usage:** `search:scrape [FLAGS]`
- **Aliases:** `scrape`

### Flags
- **`-q --query <query>`** — Explicit query (repeatable); overrides search-queries.md
- **`--limit <limit>`** — Max results per portal per query (default: 20)
- **`--priority <priority>`** — Priority categories to run, 0 = all (default: 3)
- **`--focus <focus>`** — Focus area to prioritize queries (e.g. Android)
- **`--no-save-seen`** — Skip writing seen postings to Firestore (default: saves)
- **`--json`** — Output as JSON instead of a Markdown table
- **`--summary`** — Only show summary stats, not individual results
- **`--health`** — Only check portal health (probe each portal)