# Migration Plan: Consolidate Agent Directories

**Status:** Draft / Proposed  
**Date:** 2026-11-09  
**Target:** repository-wide (agent dot-directories, `.agents/` canonical structure)  
**Session:** session-261109-181449-c6ql  
**Supersedes:** [`consolidate-agent-directories.md`](../../.junie/plans/consolidate-agent-directories.md) (session-specific draft in `.junie/plans/`)  

---

## 1. Executive Summary & Goals

Consolidate all agent-specific dot-directories (`.qwen`, `.junie`, `.gemini`, `.opencode`, `.claude`) to fully align with the canonical `.agents/` directory. This migration moves substantive content out of `.claude/` (skills, commands, agents) into `.agents/`, removes orphaned plans from `.junie/plans/`, and updates all cross-references across the repository so every agent points to the canonical locations.

**Key outcomes:**
- All skills reside in `.agents/skills/` (canonical)
- All workflow commands reside in `.agents/commands/` (new canonical location)
- Shared agent definitions reside in `.agents/agents/` (new canonical location)
- `.junie/plans/` is emptied; all plans live in `.agents/plans/` per invariants
- Routing stubs at old locations preserve backward compatibility
- All agent pointer files and root-level docs reference canonical paths

**Out of Scope:**
- `.claude/settings.json` — Claude Code runtime config, stays in `.claude/`
- `.agents/plans/` — already canonical, no migration needed
- `.agents/skills/` portal-search CLIs — already canonical, no migration needed
- Content changes to skills or commands (only path updates)

## 2. Architecture & Design Principles

### Current State

The project has a shared `.agents/` directory with skills and plans, plus agent-specific dot-directories that are supposed to be thin pointers. However:

- **`.claude/skills/`** contains 4 substantive skill directories (`job-application-assistant` with 8 reference files, `job-scraper` with SKILL.md + search-queries.md, `job-search` router, `upskill`) — these should live in `.agents/skills/`
- **`.claude/commands/`** contains 13 command files — these should live in `.agents/commands/`
- **`.claude/agents/gemini-research-expert.md`** — a shared agent definition (despite the name, it uses the `gemini -p` CLI, not Gemini-specific). It should move to `.agents/agents/`
- **`.junie/plans/`** contains 7 files (including the superseded draft of this plan): orphaned plans that should be removed
- **`.opencode/skills/job-search/`** is a duplicate of `.agents/skills/job-search/`
- Agent pointer files and dozens of docs reference `.claude/commands/` and `.claude/skills/` paths

### Plan Status Verification

**`.agents/plans/README.md`** (repository-wide): 1 Draft, 1 Superseded, 1 Canonical, 28 Completed — statuses accurate  
**`frontend/.agents/plans/README.md`** (frontend): 6 Draft/Proposed, 24 Completed — statuses accurate  
**`records/.agents/plans/README.md`** (records): Empty registry — appropriate for current state

### Key Design Decisions

1. **`.agents/skills/` is canonical for all skills** — `job-application-assistant`, `job-scraper`, `upskill`, and `job-search` router all belong here. The existing `.agents/skills/job-search/SKILL.md` is the canonical version (has `Artifact Placement` section the `.claude` copy lacks).

2. **`.agents/commands/` is canonical for all workflow commands** — new directory for this migration.

3. **`.agents/agents/` is canonical for shared agent definitions** — `gemini-research-expert.md` moves here as a shared resource any agent can use.

4. **`.junie/plans/` files are removed entirely** — not replaced with pointers. `.junie/JUNIE.md` will contain explicit instructions to NEVER commit plans there and ONLY use `.agents/plans/`.

5. **Routing stubs preserve backward compatibility** — each old location in `.claude/skills/`, `.claude/commands/`, and `.claude/agents/` gets a short stub pointing to the new location.

6. **`.claude/` directory remains** — it still owns `.claude/CLAUDE.md` (pointer), `.claude/settings.json` (runtime config), and routing stubs.

7. **All agent directories get plan management instructions** — all agent pointer files will direct to `.agents/plans/` as the only plan location.

## 3. Proposed Changes & Implementation Steps

### Stage 1: Migrate Skills

**Moves:**
- `.claude/skills/job-application-assistant/` → `.agents/skills/job-application-assistant/`
- `.claude/skills/job-scraper/` → `.agents/skills/job-scraper/`
- `.claude/skills/job-search/` → merge into existing `.agents/skills/job-search/` (canonical version wins)
- `.claude/skills/upskill/` → `.agents/skills/upskill/`

**Routing stubs in `.claude/skills/`:**
- `job-application-assistant/SKILL.md` → pointer to `.agents/skills/job-application-assistant/SKILL.md`
- `job-scraper/SKILL.md` → pointer to `.agents/skills/job-scraper/SKILL.md`
- `job-search/SKILL.md` → pointer to `.agents/skills/job-search/SKILL.md`
- `upskill/SKILL.md` → pointer to `.agents/skills/upskill/SKILL.md`

**Internal path updates (within migrated files):**
- `.agents/skills/job-scraper/SKILL.md`: `search-queries.md` reference → `.agents/skills/job-scraper/search-queries.md`
- `.agents/skills/upskill/SKILL.md`: `01-candidate-profile.md` references → `.agents/skills/job-application-assistant/`

**Duplicate removal:**
- Replace `.opencode/skills/job-search/SKILL.md` with routing pointer to `.agents/skills/job-search/SKILL.md`

### Stage 2: Migrate Commands

**Moves (13 files):**
- `.claude/commands/{add-portal,add-template,apply,expand,gmail-sync,html-report,interview,notion-sync,outcome,rank,reset,scrape,setup}.md` → `.agents/commands/`

**Routing stubs in `.claude/commands/`:**
- Each file replaced with `<!-- Relocated to .agents/commands/<file>.md -->`

**Internal path updates (within migrated command files):**
- `.agents/commands/setup.md`: `.claude/skills/job-application-assistant/` → `.agents/skills/job-application-assistant/` (12 references)
- `.agents/commands/setup.md`: `.claude/skills/job-scraper/search-queries.md` → `.agents/skills/job-scraper/search-queries.md`
- `.agents/commands/apply.md`: `.claude/skills/job-application-assistant/` → `.agents/skills/job-application-assistant/`
- `.agents/commands/rank.md`: `.claude/skills/job-application-assistant/` → `.agents/skills/job-application-assistant/`
- `.agents/commands/interview.md`: `.claude/skills/job-application-assistant/` → `.agents/skills/job-application-assistant/`
- `.agents/commands/expand.md`: `.claude/skills/job-application-assistant/` → `.agents/skills/job-application-assistant/`
- `.agents/commands/reset.md`: `.claude/skills/` → `.agents/skills/`
- `.agents/commands/scrape.md`: `.claude/skills/job-scraper/` → `.agents/skills/job-scraper/`
- `.agents/commands/add-portal.md`: verify `.agents/skills/` references (already correct)

**Router updates:**
- `.agents/skills/job-search/SKILL.md`: `.claude/commands/` → `.agents/commands/`

### Stage 3: Migrate Agents and Remove `.junie/plans/`

**Agent migration:**
- Create `.agents/agents/` directory
- Move `.claude/agents/gemini-research-expert.md` → `.agents/agents/gemini-research-expert.md`
- Create `.claude/agents/README.md` routing stub pointing to `.agents/agents/`

**.junie/plans/ cleanup (remove all files):**
- Delete all 7 files in `.junie/plans/` (including the superseded `consolidate-agent-directories.md`)
- Not replaced with pointers — removed entirely
- Update `.junie/JUNIE.md` with explicit instructions:
  - "NEVER commit plans in `.junie/`"
  - "ONLY use `.agents/plans/` following the invariants in `.agents/plans/README.md`"
  - Plans must use `YYYY-MM-DD/HHMMSS_plan-name.md` format
  - Register every plan in `.agents/plans/README.md`

### Stage 4: Update All Agent Pointer Files and Cross-References

**Agent pointer file updates (all 5 files):**
- `.qwen/QWEN.md`: `.claude/commands/` → `.agents/commands/`, add plan management instructions
- `.junie/JUNIE.md`: `.claude/commands/` → `.agents/commands/`, enhance plan management instructions
- `.gemini/GEMINI.md`: `.claude/commands/` → `.agents/commands/`, add plan management instructions
- `.opencode/OPENCODE.md`: `.claude/commands/` → `.agents/commands/`, add plan management instructions
- `.claude/CLAUDE.md`: `.claude/commands/` → `.agents/commands/`, `.claude/skills/` → `.agents/skills/`

**Root-level file updates:**
- `AGENTS.md`: `.claude/skills/job-application-assistant/` → `.agents/skills/job-application-assistant/` (lines 20, 53, 82); `.claude/commands/` → `.agents/commands/` (lines 21, 80, 83)
- `CLAUDE.md`: `.claude/commands/` → `.agents/commands/`, `.claude/skills/` → `.agents/skills/`
- `.agents/README.md`: `.claude/commands/` → `.agents/commands/` (lines 9-11, 17), add `.agents/agents/` reference
- `ai-job-search-plan.md`: `.claude/commands/` → `.agents/commands/`, `.claude/skills/` → `.agents/skills/`
- `GEMINI.md` (root): `.claude/commands/` → `.agents/commands/`, `.claude/skills/` → `.agents/skills/`
- `OPENCODE.md` (root): `.claude/commands/` → `.agents/commands/`, `.claude/skills/` → `.agents/skills/`
- `CODEX.md`: verify no `.claude/` references (uses skill names only)
- `DREAMS.md`: check for `.claude/` references
- `.claude/settings.json`: check for path references

**Final verification:**
- Grep for remaining `.claude/skills/` and `.claude/commands/` references to ensure all are either updated or are legitimate routing stubs

### Target File Structure

```
.agents/
├── README.md (updated)
├── skills/
│   ├── job-application-assistant/ (moved from .claude/skills/)
│   │   ├── SKILL.md, 01-candidate-profile.md, 02-behavioral-profile.md,
│   │   ├── 03-writing-style.md, 04-job-evaluation.md, 05-cv-templates.md,
│   │   ├── 06-cover-letter-templates.md, 07-interview-prep.md,
│   │   ├── 08-application-forms.md, outreach_protocols.json
│   ├── job-scraper/ (moved from .claude/skills/)
│   │   ├── SKILL.md, search-queries.md
│   ├── job-search/ (canonical, already existed, updated router paths)
│   │   └── SKILL.md
│   ├── upskill/ (moved from .claude/skills/)
│   │   └── SKILL.md
│   └── (existing portal-search skills...)
├── commands/ (NEW)
│   └── {add-portal,add-template,apply,expand,gmail-sync,html-report,
│        interview,notion-sync,outcome,rank,reset,scrape,setup}.md
├── agents/ (NEW)
│   └── gemini-research-expert.md (moved from .claude/agents/)
└── plans/ (unchanged)

.claude/
├── CLAUDE.md (updated)
├── settings.json (unchanged)
├── agents/README.md (routing stub to .agents/agents/)
├── skills/ (4 routing stubs)
└── commands/ (13 routing stubs)

.junie/
├── JUNIE.md (updated with plan management rules)
└── plans/ (removed entirely)

.opencode/
├── OPENCODE.md (updated)
└── skills/job-search/SKILL.md (routing stub)
```

## 4. Verification Plan & Testing Checklist

### Automated Verification
1. **File migration verification:**
   - Verify all skill directories exist in `.agents/skills/` with correct file counts
   - Verify all 13 command files exist in `.agents/commands/`
   - Verify `gemini-research-expert.md` exists in `.agents/agents/`
   - Verify `.junie/plans/` is empty (no `.md` files)
2. **Routing stub verification:**
   - Verify `.claude/skills/` contains only SKILL.md stubs (4 directories)
   - Verify `.claude/commands/` contains only routing stub files (13 files)
   - Verify `.claude/agents/README.md` exists as routing stub
   - Verify `.opencode/skills/job-search/SKILL.md` is a routing stub
3. **Path reference verification:**
   - Grep for `.claude/skills/` references — only routing stubs should remain
   - Grep for `.claude/commands/` references — only routing stubs should remain
   - Grep for `.claude/agents/` references — only routing stub should remain
4. **Link verification:**
   - Verify all links in `.agents/README.md` resolve to existing files
   - Verify all links in agent pointer files resolve to existing files

### Manual Verification
- Confirm `.junie/JUNIE.md` contains explicit "NEVER commit plans" instructions
- Confirm all agent pointer files reference `.agents/commands/` and `.agents/skills/`
- Confirm `.agents/plans/README.md` is updated with this plan's entry

## 5. Bookkeeping
- [ ] Plan persisted at `.agents/plans/2026-11-09/182733_consolidate-agent-directories.md`
- [ ] Registry `.agents/plans/README.md` row added with status "Draft / Proposed"
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`