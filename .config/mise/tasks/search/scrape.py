#!/usr/bin/env python3
#MISE description="Search job portals and collect new listings (markdown output, save-seen to Firestore by default)"
#MISE alias="scrape"
#USAGE flag "-q --query <query>" help="Explicit query (repeatable); overrides search-queries.md" var=#true
#USAGE flag "--limit <limit>" help="Max results per portal per query (default: 20)"
#USAGE flag "--priority <priority>" help="Priority categories to run, 0 = all (default: 3)"
#USAGE flag "--focus <focus>" help="Focus area to prioritize queries (e.g. Android)"
#USAGE flag "--no-save-seen" help="Skip writing seen postings to Firestore (default: saves)"
#USAGE flag "--json" help="Output as JSON instead of a Markdown table"
#USAGE flag "--summary" help="Only show summary stats, not individual results"
#USAGE flag "--health" help="Only check portal health (probe each portal)"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    scrape_script = root / "scripts" / "scrape.py"
    import runpy
    sys.argv[0] = str(scrape_script)
    runpy.run_path(str(scrape_script), run_name="__main__")

if __name__ == "__main__":
    main()
