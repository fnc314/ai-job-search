#!/usr/bin/env bash
# cleanup-tui-logs.sh - Clean up old TUI log directories based on retention policy.
#
# This script is designed to run via the mise `cd` hook. It checks for TUI log
# directories older than the configured retention period and prompts the user
# to delete them. A cooldown mechanism prevents repeated prompts.
#
# Environment variables (configured in mise.toml [env]):
#   TUI_LOG_RETENTION_DAYS  - Days to keep log directories (default: 7, 0 = always clean)
#   TUI_LOG_COOLDOWN_HOURS  - Hours between prompts (default: 24)
#
# Usage:
#   ./scripts/cleanup-tui-logs.sh

set -euo pipefail

# ─── Configuration ──────────────────────────────────────────────────────────────

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGS_DIR="$REPO_ROOT/.logs/tui"
STATE_DIR="$REPO_ROOT/.logs/tui/.state"
LAST_CLEANUP_FILE="$STATE_DIR/last_cleanup"
DONT_ASK_FILE="$STATE_DIR/dont_ask_until"

RETENTION_DAYS="${TUI_LOG_RETENTION_DAYS:-7}"
COOLDOWN_HOURS="${TUI_LOG_COOLDOWN_HOURS:-24}"

# ─── Helpers ────────────────────────────────────────────────────────────────────

now_epoch() {
    date +%s
}

hours_ago_epoch() {
    local hours="$1"
    local now
    now=$(now_epoch)
    echo $(( now - (hours * 3600) ))
}

days_ago_epoch() {
    local days="$1"
    local now
    now=$(now_epoch)
    echo $(( now - (days * 86400) ))
}

is_cooldown_active() {
    [[ -f "$DONT_ASK_FILE" ]] && {
        local dont_ask_until
        dont_ask_until=$(cat "$DONT_ASK_FILE" 2>/dev/null || echo 0)
        local now
        now=$(now_epoch)
        (( now < dont_ask_until ))
    }
}

set_cooldown() {
    mkdir -p "$STATE_DIR"
    local until_epoch
    until_epoch=$(hours_ago_epoch "-$COOLDOWN_HOURS")  # Negative hours = future
    echo "$until_epoch" > "$DONT_ASK_FILE"
}

# ─── Main Logic ─────────────────────────────────────────────────────────────────

# If logs directory doesn't exist, nothing to do
if [[ ! -d "$LOGS_DIR" ]]; then
    exit 0
fi

# If retention is 0, always clean without prompting
if [[ "$RETENTION_DAYS" -eq 0 ]]; then
    # Find and remove all date-based subdirectories
    find "$LOGS_DIR" -maxdepth 1 -mindepth 1 -type d -name '[0-9]*' -exec rm -rf {} + 2>/dev/null || true
    exit 0
fi

# Check cooldown - if active, silently skip
if is_cooldown_active; then
    exit 0
fi

# Find old log directories (older than RETENTION_DAYS)
CUTOFF_EPOCH=$(days_ago_epoch "$RETENTION_DAYS")
OLD_DIRS=()

while IFS= read -r dir; do
    [[ -z "$dir" ]] && continue
    # Get directory modification time
    dir_mtime=$(stat -f %m "$dir" 2>/dev/null || stat -c %Y "$dir" 2>/dev/null || echo 0)
    if (( dir_mtime < CUTOFF_EPOCH )); then
        OLD_DIRS+=("$(basename "$dir")")
    fi
done < <(find "$LOGS_DIR" -maxdepth 1 -mindepth 1 -type d -name '[0-9]*' 2>/dev/null)

# If no old directories, nothing to do
if [[ ${#OLD_DIRS[@]} -eq 0 ]]; then
    exit 0
fi

# Prompt the user
OLD_DIR_COUNT=${#OLD_DIRS[@]}
OLDEST="${OLD_DIRS[0]}"
NEWEST="${OLD_DIRS[$(( OLD_DIR_COUNT - 1 ))]}"

echo ""
echo "TUI Log Cleanup: Found $OLD_DIR_COUNT log directory(ies) older than $RETENTION_DAYS days"
echo "  Oldest: $OLDEST"
echo "  Newest: $NEWEST"
echo ""

# Offer options
echo "What would you like to do?"
echo "  1) Delete old logs"
echo "  2) Don't ask for $COOLDOWN_HOURS hours"
echo "  3) Skip this time"
echo ""

# Read user input (non-interactive fallback: skip)
if [[ ! -t 0 ]]; then
    # Not a terminal, skip cleanup
    exit 0
fi

read -r -p "Choice [1-3] (default: 3): " choice

case "$choice" in
    1)
        # Delete old logs
        for dir in "${OLD_DIRS[@]}"; do
            rm -rf "$LOGS_DIR/$dir"
        done
        echo "Deleted $OLD_DIR_COUNT old log directory(ies)."
        # Set cooldown after successful deletion
        set_cooldown
        ;;
    2)
        # Set cooldown
        set_cooldown
        echo "Won't ask again for $COOLDOWN_HOURS hours."
        ;;
    3| "")
        # Skip
        echo "Skipped."
        ;;
    *)
        echo "Invalid choice. Skipped."
        ;;
esac

# Update last cleanup timestamp
mkdir -p "$STATE_DIR"
now_epoch > "$LAST_CLEANUP_FILE"
