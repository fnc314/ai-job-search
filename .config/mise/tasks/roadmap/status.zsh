#!/usr/bin/env zsh
#MISE description="Display aggregate roadmap data from ROADMAP.md"

set -euo pipefail

REPO_ROOT="${MISE_PROJECT_ROOT:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
ROADMAP="${REPO_ROOT}/ROADMAP.md"

if [[ ! -f "$ROADMAP" ]]; then
    echo "ROADMAP.md not found at $ROADMAP" >&2
    exit 1
fi

awk '
/^### \[RM-[0-9]+\]/ {
    match($0, /RM-[0-9]+/)
    id = substr($0, RSTART, RLENGTH)
    sub(/^### \[RM-[0-9]+\] */, "")
    title[id] = $0
    curr = id
    order[++count] = id
}
/^\*\*Criticality:\*\*/ {
    sub(/^\*\*Criticality:\*\* */, "")
    crit[curr] = $0
}
/^\*\*Priority:\*\*/ {
    sub(/^\*\*Priority:\*\* */, "")
    prio[curr] = $0
}
/^\*\*Status:\*\*/ {
    sub(/^\*\*Status:\*\* */, "")
    status[curr] = $0
    status_counts[$0]++
}
/^\*\*Completion %:\*\*/ {
    sub(/^\*\*Completion %:\*\* */, "")
    done[curr] = $0
}
END {
    print "============================================================"
    print "  ROADMAP STATUS"
    print "============================================================"
    print ""
    printf "  Active Items: %d\n", count
    print ""
    print "  Status Distribution:"
    for (s in status_counts) {
        printf "    %-15s %d\n", s, status_counts[s]
    }
    print ""
    print "  Active Items:"
    print "  --------------------------------------------------------------------------------"
    printf "  %-8s  %-36s  %-8s  %-12s  %4s\n", "ID", "Title", "Crit", "Status", "Done"
    print "  --------------------------------------------------------------------------------"
    for (i = 1; i <= count; i++) {
        id = order[i]
        c = (id in crit) ? crit[id] : "-"
        st = (id in status) ? status[id] : "UNKNOWN"
        dn = (id in done) ? done[id] "%" : "0%"
        printf "  %-8s  %-36s  %-8s  %-12s  %4s\n", id, substr(title[id], 1, 36), substr(c, 1, 8), st, dn
    }
    print "============================================================"
}
' "$ROADMAP"
