# Implementation Plan: TUI Triage & Fix

**Status:** Completed
**Date:** 2026-11-06
**Target:** ui/terminal and repository-wide (Firestore remediation)

---

## 1. Executive Summary & Goals

This plan documents the root-cause analysis, triage, and resolution of three reported issues in the Curses TUI (`ui/terminal/tracker_tui.py`) and the Firestore `applications` collection:

| Issue | Root Cause | Resolution | Verification |
| :--- | :--- | :--- | :--- |
| **1. Screen Flashes on Sort / Redraw** | `stdscr.clear()` in the curses event loop invokes `wclear()`, blanking the physical terminal screen before re-rendering. | Changed to `stdscr.erase()`, which only resets curses's internal memory buffer and allows differential redraws via `doupdate()`. | Verified code in `main()` and `main_proposals()` in `tracker_tui.py`. |
| **2. Up/Down Arrow Navigation Blocked** | `stdscr.keypad(True)` was missing on initial screen setup, causing arrow keys to be received as multi-byte escape sequences (`\x1b[A`) rather than curses constants `curses.KEY_UP` / `curses.KEY_DOWN`. | Added `stdscr.keypad(True)` upon screen initialization in `main()` and `main_proposals()`. Added column 6 (`ord('6')`) keyboard sorting. | Keypad handling enabled; syntax validated. |
| **3. Applications Erroneously Marked `WITHDRAWN`** | An automated bulk scraping script generated `status:withdrawn` for scraped listings that failed location or were expired, which were then imported into Firestore. | **Option D**: eBay application restored to `DRAFTING` with registered artifacts; 9 disqualified scraped postings deleted from `applications`. Backed up Firestore prior to remediation. | `mise run tracker list --status APPLICATION_WITHDRAWN` returns 0 results; eBay record verified in `DRAFTING`. |

## 2. Architecture & Design Principles

### Curses Rendering Best Practice
- `stdscr.erase()` clears only the internal character matrix in memory, allowing `doupdate()` to perform differential rendering (only transmitting changed characters).
- `stdscr.clear()` calls `wclear()`, which physically blanks the entire terminal screen and forces a full redraw from row 0, causing visible flash.
- `stdscr.keypad(True)` is required to decode ANSI escape sequences (`^[[A`, `^[[B`) into curses constants (`KEY_UP`, `KEY_DOWN`).

### Firestore Data Integrity
- Pre-mutation backups are mandatory before any database fix (`generated/backup_v7_20261106_100805.json`).
- Scraped postings that fail triage criteria should remain in portal cache or triage output, never inserted into `applications` as `WITHDRAWN`.
- The `buildApplication()` function in `scripts/tracker.py` defaults to `DRAFTING` (`status:drafting`); bulk imports must not override this for location-vetoed postings.

## 3. Proposed Changes & Implementation Steps

### 3.1. TUI Fixes (`ui/terminal/tracker_tui.py`)
1. Enabled `stdscr.keypad(True)` in both `main()` and `main_proposals()`.
2. Replaced `stdscr.clear()` with `stdscr.erase()` in the rendering loops of `main()` and `main_proposals()`.
3. Added support for `ord('6')` key handling so that all visible columns (including column 6 / `role`) can be sorted via keyboard shortcuts.

### 3.2. Firestore Remediation
1. **Full Backup Created:** Pre-mutation snapshot saved to `generated/backup_v7_20261106_100805.json` (SHA-256: `6bab6ff5d564fa7848e8cf1689b09a06f3661eb5e7baec6b8fb7b247f070b80f`).
2. **eBay Application Reconciled:** `ebay--staff-android-engineer-mts-2` updated to `DRAFTING` (`statusRank: 1`, `isOpen: true`) with 3 registered artifacts (CV, Cover Letter, Outcome) and a `TAILOR_DOCUMENT` audit event.
3. **9 Erroneous Records Removed:** Permanently deleted from Firestore:
   - `whoop--staff-android-engineer`
   - `echostar--staff-android-engineer`
   - `echostar--staff-software-engineer-android`
   - `qualcomm-technologies-inc--staff-engineer-camera-system-software-android`
   - `rakuten-rewards--staff-android-engineer`
   - `real--staff-android-engineer`
   - `andpad-vietnam-co-ltd--staff-android-engineer-platform`
   - `movilges--senior-staff-android-lead`
   - `termius--staff-android-engineer`

## 4. Verification Plan & Testing Checklist

1. **Withdrawn count is zero:**
   ```bash
   mise run tracker list --status APPLICATION_WITHDRAWN
   # Output: No applications found. (count = 0)
   ```
2. **eBay record is `DRAFTING` with artifacts:**
   ```bash
   mise run tracker get ebay--staff-android-engineer-mts-2
   # Output: status: DRAFTING, isOpen: true, 3 artifacts registered (CV, Cover Letter, Outcome)
   ```
3. **TUI navigation and rendering:**
   ```bash
   mise run tui
   ```
   Arrow keys and `j`/`k` navigate smoothly; sorting with `1`-`6` re-renders without screen flashing.

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-06/052500_tui-triage-and-fix.md`
- [ ] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
