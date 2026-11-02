---
name: job-search
description: AI job search assistant — evaluate jobs, tailor CVs, write cover letters, prep interviews
arguments: mode
user_invocable: true
---

# job-search -- Router

## Mode Routing
| Input | Action |
|-------|--------|
| (empty) | Show command menu |
| `setup` | `.claude/commands/setup.md` |
| `apply <url>` | `.claude/commands/apply.md` |
| `interview` | `.claude/commands/interview.md` |
| `rank` | `.claude/commands/rank.md` |
| `scrape` | `.claude/commands/scrape.md` |
| `upskill` | `.claude/commands/upskill.md` |
| `outcome` | `.claude/commands/outcome.md` |
| `expand` | `.claude/commands/expand.md` |
| `html-report` | `.claude/commands/html-report.md` |
| `add-portal` | `.claude/commands/add-portal.md` |
| `add-template` | `.claude/commands/add-template.md` |
| `gmail-sync` | `.claude/commands/gmail-sync.md` |
| `notion-sync` | `.claude/commands/notion-sync.md` |
| `reset` | `.claude/commands/reset.md` |

## Context Loading
Read `AGENTS.md` for candidate profile and shared workflow rules, then read and execute the requested mode file in `.claude/commands/`.

## Artifact Placement

All application artifacts (CV, cover letter, outcome.md) are stored under:

```
job-applications/<Company>/<Role>/<YYYY-MM-DD>/
```

- **Company and role** use Title-Case with underscores for spaces (e.g., `Snap_Inc/Software_Engineer_Android_Level_5/2026-10-30/`)
- **Date subdirectory** allows multiple applications to the same role over time
- Each dated folder contains:
  - `main_<company>_<role>.tex` — CV source
  - `main_<company>_<role>.pdf` — Compiled CV
  - `cover_<company>_<role>.tex` — Cover letter source
  - `cover_<company>_<role>.pdf` — Compiled cover letter
  - `outcome.md` — Fit evaluation and result
  - `job_posting.md` — Original posting text (archived by `/outcome`)

See `documents/README.md` for the full convention.
