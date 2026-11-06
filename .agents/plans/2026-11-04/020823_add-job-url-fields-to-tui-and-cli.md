# Implementation Plan: Job URL Fields Integration in TUI, CLI & Schema

**Status:** Completed  
**Date:** 2026-11-04  
**Target:** ui/terminal and tracker (CLI & Schema)  

---

# Requirements

### Overview
Enable `jobUrl` (job posting URL) and `jobApplicationUrl` (ATS application URL) to be displayed and edited through the TUI detail panel, add an interactive `tracker.py edit` CLI command, and make the application `id` visible in both the TUI list and KMP detail views.

The records proto (`application.proto`) and KMP data model already support these fields. The KMP Add form and Detail screen already render them. This plan focuses on the gaps: TUI display/edit, `tracker.py` support, `reader.py` projection, and ID visibility.

### Scope

**In Scope:**
- Display `jobUrl` and `jobApplicationUrl` in the TUI detail panel (`load_detail_lines`)
- Inline editing of these fields in the TUI via a curses text input (key `e` on a URL line)
- New `tracker.py edit` interactive CLI command for editing application fields
- `reader.py` projection of `jobUrl` and `jobApplicationUrl` to the legacy row shape
- `tracker.py` `buildApplication` / `mergeApplication` support for `jobUrl` and `jobApplicationUrl`
- `tracker.schema.json` update to include `jobUrl` and `jobApplicationUrl`
- Application `id` column in the TUI main list view
- Application `id` displayed prominently in the KMP detail view

**Out of Scope:**
- KMP Add form or Detail screen changes (already done)
- Records proto changes (already done)
- KMP list row changes beyond ID visibility
- Google Sheet sync changes

### User Stories
1. As a user, I want to see `jobUrl` and `jobApplicationUrl` in the TUI detail panel so I can reference them while browsing.
2. As a user, I want to edit these URLs from the TUI detail panel so I can quickly update them without leaving the terminal.
3. As a user, I want an interactive `tracker.py edit` CLI flow so I can update application fields from the command line.
4. As a user, I want the application `id` visible in the TUI list and KMP detail view so I can reference it for CLI commands.

# Technical Design

### Current Implementation

**TUI Detail Panel** (`ui/terminal/tracker_tui.py`, `load_detail_lines`, lines 789–900):
- Renders `COMPANY`, `ROLE`, `DATE`, `STATUS`, `NOTES`, and a single `URL` field (from `row.get('source')`).
- No `jobUrl` or `jobApplicationUrl` displayed.
- No inline editing capability — the detail view is read-only with keyboard navigation.

**TUI List View** (`tracker_tui.py`, `DEFAULT_CONFIG`, lines 108–118):
- Columns: `date`, `company`, `status`, `fit_rating`, `role`. No `id` column.

**reader.py** (`scripts/reader.py`, `rowFromStore`, lines 120–150):
- Projects Firestore documents to a legacy row shape. Includes `source` but not `jobUrl` or `jobApplicationUrl`.

**tracker.py** (`scripts/tracker.py`):
- `buildApplication` (line 554): Creates the Firestore document from a payload. No `jobUrl` or `jobApplicationUrl` in the document (line 587–610).
- `mergeApplication` (line 630): Merges payloads for upsert. No `jobUrl` or `jobApplicationUrl`.
- `_interactive_application_flow` (line 989): Prompts for `source` URL but not `jobUrl`/`jobApplicationUrl`.
- No `edit` subcommand exists.

**tracker.schema.json**: No `jobUrl` or `jobApplicationUrl` defined.

**KMP Detail** (`ui/kmp/shared/.../ui/detail/JobApplicationDetailScreen.kt`, lines 249–263):
- Already renders `jobUrl` and `jobApplicationUrl` in `ApplicationSummarySection`.
- Application `id` is not displayed.

### Key Decisions

1. **`jobUrl` vs `source`**: The existing `source` field in the legacy row is the job posting URL. `jobUrl` in the proto is the same concept. We'll map `jobUrl` → `source` in `reader.py` for backward compatibility, while also exposing `jobUrl` as a separate key. `jobApplicationUrl` gets its own key.

2. **TUI inline editing**: Press `e` when a URL line is selected in the detail panel to open a curses `textpad`-style input. On confirm, the TUI calls `tracker.py upsert` (via subprocess) to persist the change.

3. **`tracker.py edit` command**: Interactive flow similar to `_interactive_application_flow` but for editing an existing application by ID. Prompts for fields to update, then calls `mergeApplication` + Firestore update.

4. **ID visibility**: Add `id` as a column in the TUI `DEFAULT_CONFIG` and display it in the KMP `ApplicationSummarySection`.

### Proposed Changes

#### 1. Schema Update
- **`tracker.schema.json`**: Add `jobUrl` and `jobApplicationUrl` as optional string properties to the application definition. Bump `schemaVersion` to 7.

#### 2. tracker.py Changes
- **`buildApplication`**: Add `jobUrl` and `jobApplicationUrl` to the document dict (lines 587–610).
- **`mergeApplication`**: Add `jobUrl` and `jobApplicationUrl` to the merge logic (line 655–662).
- **`_interactive_application_flow`**: Add prompts for `jobUrl` and `jobApplicationUrl` after the `source` URL prompt.
- **New `edit` command**: Add `commandEdit(args)` with an interactive flow that loads the existing application, prompts for fields to change, then writes via `upsert`.
- **`mise.toml`**: Add `cmd "edit"` to the tracker task Usage DSL.
- **`scripts/specs/tracker.usage.kdl`**: Add the `edit` command spec.

#### 3. reader.py Changes
- **`rowFromStore`**: Add `jobUrl` and `jobApplicationUrl` to the returned dict (after `source`).

#### 4. TUI Changes
- **`DEFAULT_CONFIG`**: Add `{"key": "id", "label": "ID", "width": 12, "visible": True}` as the first column.
- **`load_detail_lines`**: Replace the single `URL: {source}` line with separate `JOB URL:` and `APPLICATION URL:` lines. Mark them as editable (attach a special marker so the main loop knows they're editable fields).
- **Main loop**: Add `e` key handler in the detail view (and split view) that opens a curses text input for the selected URL line. On confirm, spawn `tracker.py upsert` with the updated field.

#### 5. KMP Changes
- **`JobApplicationDetailScreen.kt`**: Display `application.id` in `ApplicationSummarySection` (e.g., below the company name).

### File Structure
```
tracker.schema.json                          # MODIFIED: add jobUrl, jobApplicationUrl; bump schemaVersion
scripts/tracker.py                           # MODIFIED: buildApplication, mergeApplication, interactive flow, new edit command
scripts/reader.py                            # MODIFIED: rowFromStore projection
scripts/specs/tracker.usage.kdl              # MODIFIED: add edit command spec
mise.toml                                    # MODIFIED: add edit to tracker Usage DSL
ui/terminal/tracker_tui.py                   # MODIFIED: DEFAULT_CONFIG, load_detail_lines, main loop (inline edit)
ui/kmp/shared/.../ui/detail/JobApplicationDetailScreen.kt  # MODIFIED: display application id
```

# Delivery Steps

###   Step 1: Add jobUrl and jobApplicationUrl to schema and tracker.py
Wire `jobUrl` and `jobApplicationUrl` through the tracker CLI so they can be stored in Firestore.
- Add `jobUrl` and `jobApplicationUrl` as optional string properties to `tracker.schema.json`; bump `schemaVersion` to 7.
- Add `jobUrl` and `jobApplicationUrl` to `buildApplication()` document dict in `scripts/tracker.py`.
- Add `jobUrl` and `jobApplicationUrl` to `mergeApplication()` merge logic.
- Add prompts for `jobUrl` and `jobApplicationUrl` in `_interactive_application_flow()` after the source URL prompt.
- Regenerate docs via `mise run mise:docs:tasks`.

###   Step 2: Project fields through reader.py and display in TUI
Make `jobUrl` and `jobApplicationUrl` visible in the TUI detail panel and add the `id` column to the list.
- Add `jobUrl` and `jobApplicationUrl` to the dict returned by `rowFromStore()` in `scripts/reader.py`.
- Update `load_detail_lines()` in `ui/terminal/tracker_tui.py` to render `JOB URL:` and `APPLICATION URL:` lines (replacing the single `URL: {source}` line). Mark these lines with an editable flag.
- Add `{"key": "id", "label": "ID", "width": 12, "visible": True}` to `DEFAULT_CONFIG` columns in `tracker_tui.py` as the first column.
- Verify the TUI displays the new fields and column correctly.

###   Step 3: Add inline URL editing to TUI detail panel
Enable inline editing of `jobUrl` and `jobApplicationUrl` in the TUI detail panel via a curses text input.
- Add an `e` key handler in the TUI main loop (detail view and split view) that triggers when a URL line is selected.
- Implement a curses-based text input dialog (using `curses.newwin` + `curs_set(1)`) for editing the URL value.
- On confirm, spawn `tracker.py upsert --json -` with a minimal payload containing the application `id` and the updated field (`jobUrl` or `jobApplicationUrl`).
- Invalidate the `detail_cache` for the affected row so the updated value is re-rendered.
- On cancel, return to the detail view without changes.

###   Step 4: Add interactive tracker.py edit command
Add an interactive `tracker.py edit <id>` CLI command for editing application fields.
- Add `commandEdit(args)` to `scripts/tracker.py` that loads the existing application by ID, prompts for fields to update (company, role, jobUrl, jobApplicationUrl, status, fitRating, notes, tags), then writes via `mergeApplication` + Firestore update.
- Add `cmd "edit"` to the tracker task Usage DSL in `mise.toml` with the `id` argument and `--dry-run` flag.
- Add the `edit` command spec to `scripts/specs/tracker.usage.kdl`.
- Run `mise run mise:docs:tasks` and `mise run completions:generate` to regenerate docs and completions.

###   Step 5: Display application ID in KMP detail view
Make the application `id` visible in the KMP detail screen for easy reference.
- Display `application.id` in `ApplicationSummarySection` of `JobApplicationDetailScreen.kt` (e.g., below the company name, styled as `bodySmall` with a label like "ID:").
- Verify the KMP project compiles and the ID renders correctly on the detail screen.