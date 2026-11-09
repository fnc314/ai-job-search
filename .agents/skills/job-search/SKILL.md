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
| `setup` | `.agents/commands/setup.md` |
| `apply <url>` | `.agents/commands/apply.md` |
| `interview` | `.agents/commands/interview.md` |
| `rank` | `.agents/commands/rank.md` |
| `scrape` | `.agents/commands/scrape.md` |
| `upskill` | `.agents/commands/upskill.md` |
| `outcome` | `.agents/commands/outcome.md` |
| `expand` | `.agents/commands/expand.md` |
| `html-report` | `.agents/commands/html-report.md` |
| `add-portal` | `.agents/commands/add-portal.md` |
| `add-template` | `.agents/commands/add-template.md` |
| `gmail-sync` | `.agents/commands/gmail-sync.md` |
| `notion-sync` | `.agents/commands/notion-sync.md` |
| `reset` | `.agents/commands/reset.md` |

## Context Loading
Read `AGENTS.md` for candidate profile and shared workflow rules, then read and execute the requested mode file in `.agents/commands/`.

## Artifact Placement

All application artifacts (CV, cover letter, outcome.md) are stored under:

```
job-applications/<company>/<role>/<YYYY-MM-DD>/
```

- **Company and role** use lower-kebab-case, stripping stop words ('the', 'a', 'an', 'of', 'and') and corporate suffixes ('inc', 'llc', etc.) (e.g., `snap/software-engineer-android-level-5/2026-10-30/`)
- **Date subdirectory** allows multiple applications to the same role over time
- Each dated folder contains:
  - `main_<company>_<role>.tex` — CV source
  - `main_<company>_<role>.pdf` — Compiled CV
  - `cover_<company>_<role>.tex` — Cover letter source
  - `cover_<company>_<role>.pdf` — Compiled cover letter
  - `outcome.md` — Fit evaluation and result
  - `job_posting.md` — Original posting text (archived by `/outcome`)

See `documents/README.md` for the full convention.
