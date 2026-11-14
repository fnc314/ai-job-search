# Implementation Plan: MCP Cleanup and Consolidation

**Status:** Done
**Date:** 2026-11-13  
**Target:** repository-wide  
**Harness:** Junie  
**Session:** session-261113-234017-1wsp

---

## 1. Executive Summary & Goals

The project-level `.agents/mcp/mcp.json` defines a `mise` MCP server that duplicates what's already available in global agent configs (`~/.junie/mcp/mcp.json` and `~/.agents/mcp/mcp.json`). This creates conflicts on the owner's machine and couples a personal tool to a specific repository. Additionally, `~/.junie/mcp/mcp.json` contains duplicate IDE servers and broken entries that should be cleaned up.

**Goals:**
1. Disable the project-level `.agents/mcp/mcp.json` by renaming it to `.agents/mcp/mcp.json.disabled` so it no longer loads automatically, but remains as a reference template.
2. Create `.agents/mcp/README.md` documenting all MCP servers available for agents working in this repo, with setup instructions so new users can self-configure.
3. Add optional MCP onboarding instructions to `SETUP.md` and `AGENTS.md`.
4. Clean up `~/.junie/mcp/mcp.json` to remove duplicate and broken servers.

## 2. Architecture & Design Principles

### MCP Config Inventory

| Config File | Scope | Servers | Action |
|---|---|---|---|
| `~/.junie/mcp/mcp.json` | Junie global | 6 (idea, Composio, mise, studio, KOIN, JetBrains IDE) | Clean up: remove duplicates & broken |
| `~/.agents/mcp/mcp.json` | Shared agents | 2 (mise, hunter-remote-mcp) | Keep as-is |
| `.agents/mcp/mcp.json` | Project-local | 1 (mise) | **Disable** (rename to `.disabled`) |
| `~/.cursor/mcp.json` | Cursor IDE | 1 (studio) | Cursor-only, no change |
| `~/.lmstudio/mcp.json` | LM Studio | 0 (empty) | No change |
| `~/.gemini/config/plugins/google-cloud-developer/mcp.json` | Gemini plugin | 1 (developer-knowledge) | Plugin-managed, no change |

### Problems in `~/.junie/mcp/mcp.json`

1. **Duplicate IDE servers**: `idea`, `studio`, and `JetBrains IDE` all point to `http://127.0.0.1:64342` (same local IDE). Three entries for one service.
2. **Broken Composio server**: Returns 401 — stale `ck_...` consumer key.
3. **KOIN server**: Returns 405 on plain HTTP — needs SSE transport, unverified utility.

### Key Decisions

- **`.agents/mcp/mcp.json` is disabled, not deleted** — renamed to `.agents/mcp/mcp.json.disabled` so it serves as a reference template for new users, but doesn't load automatically or conflict with global configs.
- **Personal connections go in global configs** — Composio, Hunter.io, etc. should be configured in `~/.junie/mcp/mcp.json` or `~/.agents/mcp/mcp.json`, not committed to the repo.
- **Documentation enables self-service** — `.agents/mcp/README.md` provides a full catalog so agents and collaborators can discover and configure MCP tools without project-level configs.

## 3. Proposed Changes & Implementation Steps

### ✓ Step 1: Disable project-level `.agents/mcp/mcp.json`

- Rename `.agents/mcp/mcp.json` to `.agents/mcp/mcp.json.disabled`.
- This prevents agents from loading it as an active MCP config while preserving it as a reference template.

### ✓ Step 2: Create `.agents/mcp/README.md`

A documentation file describing each MCP server that may be useful for agents working in this repo:

- **mise** — Task runner MCP. Provides `list_commands`, `run_task`, and `install_tool`. Transport: stdio. Command: `mise mcp`. No auth needed.
- **JetBrains IDE** — Local IDE integration. Provides file edit, terminal, and navigation tools. Transport: HTTP SSE. URL: `http://127.0.0.1:64342/sse`. Requires an IDE running with MCP plugin.
- **Composio** — API platform for Gmail, GitHub, etc. Transport: HTTP SSE. URL: `https://connect.composio.dev/mcp`. Requires `x-consumer-api-key` header with a fresh `ck_...` key from [Composio Dashboard](https://dashboard.composio.dev/~/org/connect/apps).
- **hunter-remote-mcp** — Hunter.io email finder and company search. Transport: stdio via `npx mcp-remote`. Requires `HUNTER_IO_API_KEY` env var. Connects to `https://mcp.hunter.io/mcp`.
- **KOIN** — Kotlin dependency injection MCP (optional). Transport: HTTP SSE. URL: `https://mcp.kotzilla.io/mcp`.

Each entry will include:
- What the server does
- Transport type (stdio vs HTTP SSE)
- Configuration example (JSON snippet)
- Auth requirements
- Setup instructions

### ✓ Step 3: Add MCP onboarding section to `SETUP.md`

Add a new section (after the existing prerequisites) titled "### Optional: MCP Server Setup" explaining:
- What MCP is and why it's useful for agents in this repo
- Where to find MCP server documentation (`.agents/mcp/README.md`)
- How to configure MCP for your agent (point to the agent's global MCP config, e.g. `~/.junie/mcp/mcp.json`)
- Which servers are recommended vs optional
- Note that personal connections (Composio, Hunter.io) should be configured in the user's global MCP config, not committed to the repo

### ✓ Step 4: Add MCP reference to `AGENTS.md`

Add a brief note in the `## Repo Structure` section of `AGENTS.md` pointing to `.agents/mcp/README.md` for MCP server documentation. This ensures agents discover MCP capabilities when reading the repo guidelines.

### ✓ Step 5: Clean up `~/.junie/mcp/mcp.json`

Remove duplicate and broken entries from the global Junie MCP config:
- Remove `idea`, `studio` (duplicates of JetBrains IDE)
- Remove `Composio` (401 auth failure)
- Remove `KOIN` (405, unverified) — **later restored** for `frontend/apps/android` Koin DI work
- Keep `JetBrains IDE`, `mise`, and `KOIN`

### ✓ Step 6: Restore KOIN for Android frontend work

The user confirmed KOIN MCP (Kotzilla) is set up for `frontend/apps/android` Koin dependency injection observability. Re-add KOIN to `~/.junie/mcp/mcp.json`:
- Server: `KOIN` at `https://mcp.kotzilla.io/mcp` (HTTP SSE transport)
- Provides Koin DI performance monitoring, issue diagnosis, and fix guidance for Android/KMP apps

## 4. Verification Plan & Testing Checklist

- [ ] `.agents/mcp/mcp.json` no longer exists (renamed to `.agents/mcp/mcp.json.disabled`)
- [ ] `.agents/mcp/README.md` exists and contains documentation for all 5 MCP servers
- [ ] `SETUP.md` contains MCP onboarding section
- [ ] `AGENTS.md` references `.agents/mcp/README.md`
- [ ] `~/.junie/mcp/mcp.json` contains `JetBrains IDE`, `mise`, and `KOIN` servers
- [ ] `~/.junie/mcp/mcp.json` is valid JSON
- [ ] No agent complaints about missing MCP servers after cleanup

## 5. Bookkeeping
- [ ] Plan persisted at `.agents/plans/YYYY-MM-DD/HHMMSS_mcp-cleanup-and-consolidation.md`
- [ ] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`