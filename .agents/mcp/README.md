# MCP Servers for AI Job Search

This directory documents MCP (Model Context Protocol) servers that are useful for agents working in this repository. The project-level MCP config (`.agents/mcp/mcp.json`) has been **disabled** to avoid conflicts with global agent configs. Instead, configure these servers in your agent's global MCP config file.

## Why no project-level MCP config?

MCP connections are personal — they tie to your local IDE, your API keys, and your accounts. Keeping them in global configs avoids committing personal credentials to the repository and prevents conflicts when multiple agents load overlapping server definitions.

**Where to configure:**

| Agent | Global MCP config location |
|---|---|
| Junie | `~/.junie/mcp/mcp.json` |
| Shared agents | `~/.agents/mcp/mcp.json` |
| Cursor | `~/.cursor/mcp.json` |
| Claude Desktop | `~/.claude/claude_desktop_config.json` |

---

## Server Catalog

### 1. mise — Task Runner MCP

**What it does:** Exposes `mise` task runner commands as MCP tools. Provides `list_commands`, `run_task`, and `install_tool` so agents can discover and execute project tasks without shell access.

**Transport:** stdio

**Configuration:**
```json
{
  "mcpServers": {
    "mise": {
      "command": "mise",
      "args": ["mcp"],
      "env": {}
    }
  }
}
```

**Auth:** None required.

**Setup:** Install `mise` (`curl https://mise.run | sh`) and ensure it's on your `PATH`. No additional configuration needed.

---

### 2. JetBrains IDE — Local IDE Integration

**What it does:** Connects to a running JetBrains IDE (IntelliJ, Android Studio, etc.) for file editing, terminal execution, navigation, and project inspection.

**Transport:** HTTP SSE

**Configuration:**
```json
{
  "mcpServers": {
    "JetBrains IDE": {
      "url": "http://127.0.0.1:64342/sse"
    }
  }
}
```

**Auth:** None (local connection only).

**Setup:**
1. Open your JetBrains IDE project.
2. Install the MCP plugin if not already installed.
3. The IDE exposes the MCP endpoint automatically on port `64342` (or whatever port is configured).
4. Verify the port matches your IDE's configuration.

**Note:** Only one IDE server entry is needed. Duplicate entries (`idea`, `studio`, etc.) pointing to the same URL are redundant.

---

### 3. Composio — API Platform (Gmail, GitHub, etc.)

**What it does:** API platform connecting AI agents to services like Gmail, GitHub, Slack, Notion, Calendar, and Linear. Essential for email synchronization workflows (`mise email:sync-labels`, `mise email:read-inbox`, `mise email:collect`).

**Transport:** HTTP SSE

**Configuration:**
```json
{
  "mcpServers": {
    "Composio": {
      "url": "https://connect.composio.dev/mcp",
      "headers": {
        "x-consumer-api-key": "ck_YOUR_FRESH_KEY_HERE"
      }
    }
  }
}
```

**Auth:** Requires `x-consumer-api-key` header with a valid `ck_...` consumer key.

**Setup:**
1. Go to [Composio Dashboard → For You → AI Clients](https://dashboard.composio.dev/~/org/connect/apps).
2. Generate a fresh consumer key (`ck_...`).
3. Paste the key into your global MCP config as shown above.
4. Reconnect any apps (Gmail, GitHub, etc.) you need access to.

**Important:** This is a personal connection — never commit the API key to the repository. The `mise.toml` file uses an encrypted version of this key for CLI tasks, which is separate from the MCP connection.

---

### 4. hunter-remote-mcp — Hunter.io Email Finder

**What it does:** Hunter.io email finder and company search. Provides 112+ tools for company search, email finding, email verification, sequences, and CRM integrations.

**Transport:** stdio (via `npx mcp-remote` proxy to HTTP SSE)

**Configuration:**
```json
{
  "mcpServers": {
    "hunter-remote-mcp": {
      "command": "npx",
      "args": ["mcp-remote", "https://mcp.hunter.io/mcp"],
      "env": {
        "HUNTER_IO_API_KEY": "your_hunter_io_api_key"
      }
    }
  }
}
```

**Auth:** Requires `HUNTER_IO_API_KEY` environment variable.

**Setup:**
1. Obtain an API key from [Hunter.io](https://hunter.io/api).
2. Set the `HUNTER_IO_API_KEY` environment variable in your config.
3. The `npx mcp-remote` proxy bridges stdio to the remote HTTP SSE endpoint.

**Note:** The stdio proxy can have timing issues where it shuts down before returning responses. Direct HTTP SSE requests to `https://mcp.hunter.io/mcp` work more reliably if your agent supports it.

---

### 5. KOIN — Kotlin Dependency Injection MCP (Optional)

**What it does:** Kotlin dependency injection MCP server. Provides tools related to KOIN DI framework.

**Transport:** HTTP SSE

**Configuration:**
```json
{
  "mcpServers": {
    "KOIN": {
      "url": "https://mcp.kotzilla.io/mcp"
    }
  }
}
```

**Auth:** None required.

**Setup:** No additional setup needed beyond adding the config entry.

**Note:** This server is optional and unverified for daily use. The endpoint requires SSE transport — plain HTTP requests will return 405.

---

## Recommended Setup

For agents working in this repo, the recommended minimum MCP configuration is:

| Server | Priority | Reason |
|---|---|---|
| **mise** | Required | Task runner access for all `mise run` commands |
| **JetBrains IDE** | Recommended | Local IDE integration for file operations |
| **Composio** | Recommended | Email sync pipeline (`email:sync-labels`, etc.) |
| **hunter-remote-mcp** | Optional | Email finding and company research |
| **KOIN** | Optional | Kotlin DI framework tools |

## Quick Start

1. Copy the configuration snippets above into your agent's global MCP config.
2. Replace any placeholder keys with your actual credentials.
3. Restart your agent to load the new MCP servers.
4. Verify connectivity by checking available tools.
