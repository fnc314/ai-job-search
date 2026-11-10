#!/usr/bin/env python3
"""Display aggregate roadmap data from ROADMAP.md."""

import os
import re
import sys
from datetime import datetime, date

def main():
    repo_root = os.environ.get("REPO_ROOT", os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))))
    roadmap_path = os.path.join(repo_root, "ROADMAP.md")

    if not os.path.exists(roadmap_path):
        print("ROADMAP.md not found.")
        sys.exit(1)

    with open(roadmap_path, "r") as f:
        content = f.read()

    # Check for frozen section
    frozen_match = re.search(r"## Frozen Items", content)
    active_content = content[:frozen_match.start()] if frozen_match else content
    frozen_content = content[frozen_match.start():] if frozen_match else ""

    # Parse active items
    items = parse_items(active_content)
    frozen_items = parse_items(frozen_content) if frozen_content else []

    if not items and not frozen_items:
        print("No roadmap items found.")
        return

    # Aggregate data
    today = date.today()
    status_counts = {}
    criticality_counts = {}
    priority_counts = {}
    oldest_age = 0

    for item in items:
        status = item.get("status", "UNKNOWN")
        status_counts[status] = status_counts.get(status, 0) + 1

        crit = item.get("criticality", "NOT_SET")
        criticality_counts[crit] = criticality_counts.get(crit, 0) + 1

        prio = item.get("priority", "NOT_SET")
        priority_counts[prio] = priority_counts.get(prio, 0) + 1

        creation = item.get("creation_date", "")
        if creation:
            try:
                created = datetime.strptime(creation, "%Y-%m-%d").date()
                age = (today - created).days
                oldest_age = max(oldest_age, age)
            except ValueError:
                pass

    # Print summary
    print("=" * 60)
    print("  ROADMAP STATUS")
    print("=" * 60)
    print()

    print(f"  Active Items:    {len(items)}")
    print(f"  Frozen Items:    {len(frozen_items)}")
    print(f"  Oldest Item:     {oldest_age} days")
    print()

    print("  Status Distribution:")
    for status, count in sorted(status_counts.items()):
        print(f"    {status:15s} {count}")
    print()

    print("  Criticality Distribution:")
    for crit, count in sorted(criticality_counts.items()):
        print(f"    {crit:15s} {count}")
    print()

    print("  Priority Distribution:")
    for prio, count in sorted(priority_counts.items()):
        print(f"    {prio:15s} {count}")
    print()

    # Completion summary
    completions = [int(item.get("completion_percent", 0)) for item in items]
    if completions:
        avg_completion = sum(completions) / len(completions)
        done_count = sum(1 for c in completions if c >= 100)
        print(f"  Completion:      {done_count}/{len(completions)} items done (avg {avg_completion:.0f}%)")
    print()

    # Frozen reason
    if frozen_items:
        frozen_reason_match = re.search(r"\*\*Reason:\*\*\s*(.+)", frozen_content)
        frozen_date_match = re.search(r"\*\*Frozen on:\*\*\s*(.+)", frozen_content)
        if frozen_date_match:
            print(f"  Last Frozen:       {frozen_date_match.group(1).strip()}")
        if frozen_reason_match:
            reason = frozen_reason_match.group(1).strip()
            if len(reason) > 60:
                reason = reason[:57] + "..."
            print(f"  Reason:            {reason}")
    print()

    # Item list
    if items:
        print("  Active Items:")
        print("  " + "-" * 56)
        for item in items:
            item_id = item.get("id", "?")
            title = item.get("title", "?")[:30]
            crit = item.get("criticality", "?")
            status = item.get("status", "?")
            age = "?"
            creation = item.get("creation_date", "")
            if creation:
                try:
                    created = datetime.strptime(creation, "%Y-%m-%d").date()
                    age = f"{(today - created).days}d"
                except ValueError:
                    pass
            print(f"    {item_id}  {title:30s}  {crit:8s}  {status:10s}  {age}")
        print()

    print("=" * 60)


def parse_items(content):
    """Parse ROADMAP items from markdown content."""
    items = []
    item_blocks = re.split(r"(?=### \[RM-\d+\])", content)

    for block in item_blocks:
        if "### [RM-" not in block:
            continue

        item = {}

        # ID and title
        id_match = re.search(r"### \[(RM-\d+)\]\s+(.+)", block)
        if id_match:
            item["id"] = id_match.group(1)
            item["title"] = id_match.group(2).strip()

        # Fields
        for field, key in [
            (r"\*\*Short Desc:\*\*\s+(.+)", "short_desc"),
            (r"\*\*Criticality:\*\*\s+(.+)", "criticality"),
            (r"\*\*Priority:\*\*\s+(.+)", "priority"),
            (r"\*\*Status:\*\*\s+(.+)", "status"),
            (r"\*\*Completion %:\*\*\s+(\d+)", "completion_percent"),
            (r"\*\*Creation Date:\*\*\s+(\d{4}-\d{2}-\d{2})", "creation_date"),
        ]:
            match = re.search(field, block)
            if match:
                item[key] = match.group(1).strip()

        if item.get("id"):
            items.append(item)

    return items


if __name__ == "__main__":
    main()
