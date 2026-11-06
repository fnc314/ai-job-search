# Implementation Plan: TUI Session Log Rotation & Disk Cleanup

**Status:** Completed  
**Date:** 2026-11-01  
**Target:** ui/terminal (Curses TUI)  

---

# Technical Design

### ✓ done Step 1: Restructure TUI logging into date-based subdirectories
- Update `_setup_logging()` in `ui/terminal/tracker_tui.py` to create logs under `.logs/tui/YYYYMMDD/` instead of flat `.logs/tui/`
- Verify log files are created correctly with the new path structure

### ✓ done Step 2: Create cleanup script and mise hook
- Create `scripts/cleanup-tui-logs.sh` with retention, cooldown, and prompt logic
- Add `[env]` variables (`TUI_LOG_RETENTION_DAYS`, `TUI_LOG_COOLDOWN_HOURS`) to `mise.toml`
- Add `[hooks] cd` entry to `mise.toml`
- Test the script manually with various retention/cooldown settings
- Regenerate docs via `mise run mise:docs:tasks` (hook sources changed)