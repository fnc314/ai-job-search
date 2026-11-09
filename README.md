<p align="center">
  <img src="assets/mascot/pip_flight_loop.gif" alt="Pip, the courier bird" width="200">
</p>

# AI Job Search

*The job search that runs on your machine.*

<p align="center">
  <a href="https://trendshift.io/repositories/43622?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-43622" target="_blank" rel="noopener noreferrer"><img src="https://trendshift.io/api/badge/trendshift/repositories/43622/daily" alt="MadsLorentzen%2Fai-job-search | Trendshift" width="250" height="55"/></a>
</p>

[![CI](https://github.com/MadsLorentzen/ai-job-search/actions/workflows/ci.yml/badge.svg)](https://github.com/MadsLorentzen/ai-job-search/actions/workflows/ci.yml)

An AI-powered job application and tracking framework built for **all major AI CLIs** (Claude Code, Google Gemini / Antigravity, OpenCode, Codex). Fork it, fill in your profile, and let your AI agent evaluate job postings, tailor your CV, write cover letters, prepare you for interviews, and manage your full application pipeline across terminal, web, and native mobile/desktop apps.

> **Note**: This is an independent open-source project and is not affiliated with, endorsed by, sponsored by, or maintained by Anthropic or Google. Product names are referenced only to describe supported toolchains.
>
> This project has **no affiliated cryptocurrency, token, or paid sponsorship program**. Anything claiming otherwise is unauthorized and should be treated as a scam. The only ways to support the project are the Ko-fi link below and contributing on GitHub.

## Does it actually work?

*From the original author (Mads Lorentzen):*

I'm a geophysicist by training. When my position was cut in late 2025, I built this framework to run my own job search — the same `/scrape`, `/apply`, and `/interview` workflow in this repo, used weekly, on my own career. I was upfront about it with every employer I spoke to, and instead of counting against me, it usually sparked a genuine technical conversation.

Sixty-nine tailored applications, twenty first interviews, and one signed contract later, I started as an AI engineer in June 2026. People kept asking whether this actually works. It got me hired. Now it's yours.

*The longer version, including the full application funnel, is on [LinkedIn](https://www.linkedin.com/in/mads-lorentzen/).*

<p align="center">
  <i>Did this save you a Sunday of cover-letter writing? Consider a coffee.<br>
  Did it land you the job? Maybe two.</i> ☕
</p>

<p align="center">
  <a href="https://ko-fi.com/madslorentzen">
    <img src="https://storage.ko-fi.com/cdn/kofi3.png?v=6" alt="Buy me a coffee at ko-fi.com" height="40">
  </a>
</p>

---

## What this is

A structured, agentic workflow that turns your AI CLI into a full-stack job application assistant and pipeline tracker. The core workflow (self-profiling, fit evaluation, drafter-reviewer generation, and interview preparation) is **language- and country-agnostic**.

```
/setup          /scrape              /apply <url>            /interview
  |                |                     |                       |
  v                v                     v                       v
Fill in        Search job           Evaluate fit            Prepare STAR
your profile   portals              Score & recommend       talking points,
  |                |                     |                  company intel,
  v                v                     v                  mock interviews
Profile        Present matches      Draft CV + Letter            |
files ready    with fit ratings     (Tailored LaTeX)             v
                   |                     |                  Track funnel
                   v                     v                  in Firestore
               Pick match           Reviewer agent          & Dashboards
               -> /apply            -> PDF inspect -> Done
```

---

## Current Project Layout

```
ai-job-search/
├── .agents/                           # Universal Agent Index & Portable Skills
│   ├── README.md                      # Canonical index of all skills and CLI entry points
│   ├── commands/                      # Slash command definitions (/apply, /outcome, /setup, etc.)
│   └── skills/                        # Portable Agent Skills (CLIs and workflows)
│       ├── job-application-assistant/ # Methodology guides and candidate profile
│       ├── job-tracker/               # System-of-record invariants, commands, & schemas
│       ├── job-search/                # Orchestration skill for setup, apply, and interview prep
│       ├── freehire-search/           # Tech job aggregator search CLI
│       ├── linkedin-search/           # Country-agnostic LinkedIn job query CLI
│       ├── composio/                  # Composio tooling integration
│       └── jobindex-search/ ...       # Regional job search portals (Jobnet, Jobbank, etc.)
├── templates/                         # Master Application Templates
│   ├── cv/main_example.tex            # Master LaTeX CV (Inconsolata headings, Sans-serif body)
│   └── cover-letters/                 # Custom LaTeX cover letter class (cover.cls) & fonts
├── scripts/                           # Underlying Python implementation tools
│   ├── tracker.py                     # Central system-of-record CLI for Firestore mutations
│   ├── reader.py                      # Multi-source read projection for UIs (live store / cache / csv)
│   ├── generate_report.py             # Generates standalone SVG/HTML application dashboard
│   ├── salary_lookup.py               # Salary benchmark querying tool
│   ├── gmail_fetch.py                 # Gmail collector via Composio CLI
│   └── specs/                         # CLI argument specifications in Usage KDL format
├── ui/                                # Multi-Interface Application Tracking Suite
│   ├── terminal/                      # Interactive Terminal UI (TUI)
│   │   ├── tracker_tui.py             # Keyboard-driven curses application tracker
│   │   └── tui_config.json            # Layout and theme configurations
│   ├── web/                           # Standalone Web Dashboard
│   │   ├── application-dashboard.html # Interactive HTML/SVG pipeline report
│   │   └── application-dashboard-server.py # Lightweight localhost preview server
├── frontend/                          # Native Kotlin Multiplatform Suite (Android, Desktop, Web, iOS)
│   ├── apps/                          # Platform targets: android, desktop, web (Wasm/JS), ios
│   ├── core/                          # core-design-system (Material 3), core-widgets, core-data, core-network, core-navigation
│   └── features/                      # Modular features (sign-in, job-applications-list, etc.)
├── records/                           # Protobuf contracts and parser bridge
│   ├── proto/                         # tracker/v1/application.proto definitions
│   └── parser/                        # Java/Kotlin data bridge
├── documents/                         # Candidate career materials (CVs, LinkedIn exports, diplomas)
├── job-applications/                  # Generated per-job application archives (LaTeX + compiled PDFs)
├── tracker.schema.json                # Single typed contract for all Firestore records (Schema v7)
├── archive/                           # Frozen pre-migration CSV snapshot (read-only historical audit)
├── generated/                         # Disposable caches and Gmail fetch payloads (gitignored)
├── docs/mise/                         # Auto-generated documentation for all mise tasks
├── mise.toml                          # Task runner configuration and tool version management
├── AGENTS.md                          # Universal Agent instructions (Single Source of Truth)
├── GEMINI.md                          # Google Gemini / Antigravity CLI entry point
└── CLAUDE.md                          # Claude Code CLI entry point
```

---

## Universal Functionality Colocation via `.agents`

This workspace implements the **Universal Agent Architecture**. All autonomous behaviors, search skills, and database invariants are colocated inside the [`.agents/`](.agents/) directory:

- **Single Source of Truth:** Core skills live in [`.agents/skills/`](.agents/skills/) using the standardized Agent Skills format. Every CLI pointer (`AGENTS.md`, `GEMINI.md`, `CLAUDE.md`, `OPENCODE.md`) references these canonical definitions.
- **Portability Across AI Tools:** Whether executing via **Claude Code**, **Google Gemini / Antigravity**, **OpenCode**, or **Codex**, the AI uses the exact same operational rules, verification checklists, and CLI toolchains.
- **Self-Contained CLIs:** Scraper and search skills (e.g. `linkedin-search`, `freehire-search`) live directly with their runtime dependencies, enabling instant execution without modifying global packages.

---

## Database Architecture & Interaction Interfaces

Cloud Firestore is the **single authoritative system of record** (`job-search-fnc314-dev`). The historical CSV in `archive/` is retired and never read as a mutable data source.

### Invariants & Schema Contract
1. **Typed Boundary:** Every document adheres strictly to [`tracker.schema.json`](tracker.schema.json) (Schema Version 7).
2. **Append-Only History:** Funnel movements, status changes, and notes are never destructively overwritten; they are logged as immutable events in the `applications/{id}/events` subcollection. Derived fields (`notesRendered`, `isOpen`, `counters`, `timeline`) are reprojected from the event log.
3. **Write Safety:** Mutations to Firestore are opt-in and require `TRACKER_ALLOW_WRITES=1`.

### Interaction Interfaces

The workspace provides multiple specialized interfaces to interact with the database depending on your workflow:

```
                                  ┌──────────────────────────────┐
                                  │   Cloud Firestore (nam5)     │
                                  │   System of Record (Schema 7)│
                                  └──────────────▲───────────────┘
                                                 │
            ┌────────────────────────────────────┼────────────────────────────────────┐
            │                                    │                                    │
            ▼                                    ▼                                    ▼
┌───────────────────────┐            ┌───────────────────────┐            ┌───────────────────────┐
│     mise / CLI        │            │   Terminal UI (TUI)   │            │ Kotlin Multiplatform  │
│  `mise run tracker`   │            │    `mise run tui`     │            │ `mise run frontend:*` │
│ Scripts, automation,  │            │ Keyboard-driven       │            │ Android, Desktop JVM, │
│ bulk edits, auditing  │            │ real-time curses view │            │ Web Wasm/JS, and iOS  │
└───────────────────────┘            └───────────────────────┘            └───────────────────────┘
            │                                    │                                    │
            ▼                                    ▼                                    ▼
┌───────────────────────┐            ┌───────────────────────┐            ┌───────────────────────┐
│     Web Dashboard     │            │   Google Sheet Sync   │            │   Notion Live View    │
│  `mise run dashboard` │            │ `tracker sheet --sync`│            │     `/notion-sync`    │
│ Local SVG/HTML report │            │ Regenerated sheet     │            │ Mobile-friendly live  │
│ served on :8000       │            │ mirror view           │            │ dashboard view        │
└───────────────────────┘            └───────────────────────┘            └───────────────────────┘
```

1. **The CLI (`mise run tracker <command>`)**:
   Full programmatic control for listing, inspecting, updating, status changes, manual event appending, and scoring.
   ```bash
   mise run tracker list --open
   mise run tracker get capital-one
   mise run tracker edit capital-one --interactive
   mise run tracker status capital-one interview --note "Technical screen scheduled"
   ```
2. **Interactive Terminal UI (`mise run tui`)**:
   Fast, keyboard-driven curses tracker. Supports sorting, interactive filtering, viewing notes history, and jumping to local CV/cover letter PDFs.
3. **Web Dashboard Server (`mise run dashboard`)**:
   Lightweight Python server serving [`ui/web/application-dashboard.html`](ui/web/application-dashboard.html) at `http://127.0.0.1:8000` with interactive pipeline metrics, sector distributions, and status charts.
4. **Native Multiplatform Apps (`frontend`)**:
   Native applications built with Compose Multiplatform:
   - **Desktop (JVM):** Full native desktop experience (`mise run frontend:desktop:run`).
   - **Android:** Mobile tracker with Material 3 design and offline-first Firestore sync (`mise run frontend:android:install`).
   - **Web:** Browser clients compiled to WebAssembly and JavaScript (`mise run frontend:web:wasm:dev`).
5. **Synchronized Views (Google Sheets & Notion)**:
   - Synchronize an external Google Sheet mirror: `mise run tracker sheet --sync`.
   - Publish a read-only live briefing to Notion: `/notion-sync`.

---

## The `mise` Toolkit

Task execution across the entire repository is standardized using [`mise`](https://mise.jdx.dev/). Each task's arguments and usage are documented under [`docs/mise/`](docs/mise/).

### Application & Search Tasks
- **`mise run tracker <command>`** — Execute tracker CLI operations (e.g., `check`, `list`, `get`, `add`, `edit`, `status`, `score`, `event`, `artifact`, `proposals`).
- **`mise run align`** — Align Firebase records to canonical IDs, recalculate fit ratings, and refresh `outcome.md`.
- **`mise run scrape`** — Search portals and collect new job listings deduplicated against the tracker.
- **`mise run salary [company]`** — Benchmark role compensation using local salary data.
- **`mise run tui`** — Launch the interactive curses Terminal UI.
- **`mise run dashboard`** — Serve the HTML dashboard locally on port 8000.
- **`mise run report`** — Regenerate static HTML application report.
- **`mise run setup`** — Run first-time setup, validate environment, and seed schema.

### Gmail Ingestion & Automation Tasks
- **`mise run gmail:sync:labels`** — Discover and cache user Gmail labels via Composio CLI.
- **`mise run gmail:read:inbox`** — Fetch job-related emails and write inbox payload.
- **`mise run gmail:collect`** — Stage proposals from fetched emails to Firestore for human review.

### Kotlin Multiplatform (`frontend`) Tasks
- **`mise run frontend:build`** — Build all KMP targets and modules.
- **`mise run frontend:clean`** — Clean Gradle build cache and build directories.
- **`mise run frontend:test`** — Run KMP unit tests across shared modules.
- **`mise run frontend:desktop:run`** — Launch the Compose Desktop application.
- **`mise run frontend:desktop:package`** — Package native desktop distribution (DMG/MSI/DEB).
- **`mise run frontend:android:assemble`** — Build Android debug APK.
- **`mise run frontend:android:bundle`** — Build Android release App Bundle.
- **`mise run frontend:android:install`** — Install debug APK to connected device/emulator.
- **`mise run frontend:web:wasm:dev`** — Run Compose Web dev server (Wasm target).
- **`mise run frontend:web:wasm:dist`** — Build production Web Wasm bundle.
- **`mise run frontend:web:js:dev`** — Run Compose Web dev server (JS target).
- **`mise run frontend:web:js:dist`** — Build production Web JS bundle.

### Documentation & Shell Completions
- **`mise run mise:docs:tasks`** — Lint Usage specs, sync Protobuf, and regenerate Markdown documentation in `docs/mise/`.
- **`mise run completions:generate`** — Generate fast Zsh autocompletions for all CLI specs.

---

## Prerequisites & Installation

1. **AI CLI**: OpenCode, Claude Code, Gemini CLI, Antigravity, or Codex.
2. **Task Runner**: [mise](https://mise.jdx.dev/) (`curl https://mise.run | sh` or `brew install mise`).
3. **Runtimes**: Python 3.10+, [Bun](https://bun.sh) (for portal search CLIs), JDK 17+ (for KMP and Protobuf builds).
4. **LaTeX Distribution**: MacTeX, TeX Live, or MiKTeX with `lualatex` (for CV) and `xelatex` (for cover letter).
5. **Optional ATS Tooling**: `pdftotext` from `poppler` (`brew install poppler` on macOS).

### Quick Setup

```bash
# 1. Install toolchains and dependencies
mise install

# 2. Run initial environment validation
mise run setup --check

# 3. Launch your AI assistant
antigravity # or claude, opencode, gemini
```

Inside your AI chat, run:
```bash
/setup
```

---

## How `/apply` Works

The `/apply` command executes a rigorous **drafter-reviewer workflow** with automated PDF layout verification:

1. **Parse & Evaluate:** Extracts posting requirements and scores fit against your verified candidate profile.
2. **Draft LaTeX:** Generates tailored CV (`cv/main_example.tex`) and Cover Letter (`cover_letters/cover.cls`).
3. **Reviewer Audit:** An independent reviewer agent critiques the drafts for keyword alignment, tone, and ATS scanability.
4. **Compile & Inspect:** Compiles with `lualatex` (CV) and `xelatex` (Cover Letter). Enforces strict layout rules (CV strictly 2 pages; Cover Letter strictly 1 page).
5. **ATS Verification:** Extracts embedded text using `pdftotext` to ensure zero symbol corruption, valid reading order, and genuine keyword grounding (zero fabrication).

---

## Contributing & Upstream Synchronization

- Upstream releases are tagged in GitHub releases. Use `mise run dev:check-upstream` to preview upstream updates against your personalized files.
- Refer to [CONTRIBUTING.md](CONTRIBUTING.md) for PR guidelines and [SECURITY.md](SECURITY.md) for untrusted data handling rules.

## Acknowledgements

- **Original Creator:** [Mads Lorentzen](https://github.com/MadsLorentzen) ([ai-job-search](https://github.com/MadsLorentzen/ai-job-search))
- **Portal Search CLIs:** [Mikkel Krogholm](https://github.com/mikkelkrogsholm)

## License

MIT
