# Implementation Plan: Mise Task Orchestration, Root Config Consolidation & Subsystem Documentation Alignment

**Status:** Completed  
**Date:** 2026-11-09  
**Target:** repository-wide  
**Depends On:** [`.agents/plans/2026-11-09/164500_post_monorepo_quality_gates_and_cloud_run_deployment_plan.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.agents/plans/2026-11-09/164500_post_monorepo_quality_gates_and_cloud_run_deployment_plan.md)  
**Reconciles & Completes:** [`.agents/plans/2026-11-09/182733_consolidate-agent-directories.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.agents/plans/2026-11-09/182733_consolidate-agent-directories.md) (Junie Instruction Consolidation)  

---

## 1. Goal Description & Executive Summary

This plan unifies task orchestration, root tooling consolidation, and complete monorepo alignment across both code and AI instructions:
1. **Mise Task Orchestration over Custom Gradle Aggregators**: Rather than registering custom aggregator tasks (such as `checkAll`) inside Gradle build scripts, use file-based Mise tasks in `.config/mise/tasks/` to orchestrate multiple Gradle calls and task chains. Specifically, wire Konsist architectural testing into `mise run frontend:test` (alongside `core-data:jvmTest`) and introduce a unified `mise run dev:check` (alias `check`) validation task.
2. **Strict Root `mise.toml` Consolidation**: Eliminate all straggler `mise.toml` files in child subsystems (`records/mise.toml`, `backend/mise.toml`, `frontend/mise.toml`). Consolidate all tool declarations (`node`, `java`, `gradle`, `bun`, `python`, `usage`) into root `mise.toml`, remove the multi-root `[monorepo]` config block, and format via `tombi format`.
3. **Subsystem Active Documentation & Protocol Alignment**: Update all active `.md` files across `frontend/` and `records/` (including all 12 module READMEs, `frontend/AGENTS.md`, and `records/AGENTS.md`), root `README.md`, and Git hooks to replace stale `ui/kmp` references, outdated paths, and local build commands with canonical root-level `mise run <task>` and `./gradlew :<includedBuild>:<task>` invocations.
4. **Audit & Reconcile Junie's AI Instruction Consolidation**: Audit and resolve the code, tooling, security, and test drift left after Junie moved markdown files from `.claude/` to `.agents/`. Update Python scripts (`rank.py`, `scrape.py`, `fit_evaluator.py`, `lint_skills.py`), `.gitignore` personal data patterns, root `AGENTS.md`, and command test suites to reference the new canonical `.agents/` structure.

---

## 2. Audit of Junie's Work (`182733_consolidate-agent-directories.md`)

Our verification confirms Junie successfully completed the markdown file moves:
- **Skills Relocated**: `job-application-assistant/`, `job-scraper/`, `upskill/` moved from `.claude/skills/` to `.agents/skills/`.
- **Commands Relocated**: All 13 workflow commands moved from `.claude/commands/` to `.agents/commands/`.
- **Shared Agent Relocated**: `gemini-research-expert.md` moved to `.agents/agents/`.
- **Agent Pointers Updated**: `CLAUDE.md`, `GEMINI.md`, `OPENCODE.md`, `.gemini/GEMINI.md`, `.opencode/OPENCODE.md`, `.qwen/QWEN.md`, `.junie/JUNIE.md` updated with plan management rules and canonical paths.
- **Orphan Plans Removed**: `.junie/plans/` cleared.

### Identified Gaps (Drift in Code, Tooling & Security)
Because Junie was tasked with instruction drift rather than code edits, the following discrepancies remain:
1. **Python Script Hardcoded Paths**:
   - `scripts/rank.py` lines 21-22 hardcodes `.claude/skills/job-application-assistant/01-candidate-profile.md` (crashes `mise run search:rank`).
   - `scripts/scrape.py` line 28 hardcodes `.claude/skills/job-scraper/search-queries.md` (crashes `mise run search:scrape`).
   - `lib/fit_evaluator.py` & `lib/tracker/fit_evaluator.py` hardcode `.claude/skills/` docstrings and lookup paths.
   - `scripts/check_framework_version.py` & `scripts/check_upstream_updates.py` check `.claude/skills/`.
2. **Skill Linter Failures**:
   - `scripts/lint_skills.py` scans `.claude/skills/` and `.claude/commands/`. Because Junie replaced those with redirection comments (not `# /<name>` or YAML frontmatter), `lint_skills.py` fails with 17 errors.
3. **Security & Gitignore Vulnerability**:
   - `.gitignore` lines 178-182 still ignore `.claude/skills/...` instead of `.agents/skills/...`. Candidate profiles in `.agents/skills/job-application-assistant/` risk being committed.
   - `.gitignore` line 185 still references `ui/kmp/androidApp/google-services.json` instead of `frontend/apps/android/google-services.json`.
4. **Root Instructions (`AGENTS.md`)**:
   - `AGENTS.md` lines 20-21, 53, 78, 80 still point to `.claude/skills/` and `.claude/commands/`.
   - `.agents/README.md` line 8 still says `ui/kmp/.agents/plans/`.
5. **Command Tests**:
   - `scripts/tests/test_html_report_command.py`, `test_notion_sync_command.py`, `test_outcome_followup.py` test `.claude/commands/` instead of `.agents/commands/`.

---

## 3. Proposed Changes

### Component 1: Root Mise Configuration Consolidation

Consolidate toolchain definitions into root `mise.toml` and remove subsystem stragglers.

#### [DELETE] `records/mise.toml`
#### [DELETE] `backend/mise.toml`
#### [DELETE] `frontend/mise.toml`

#### [MODIFY] `mise.toml`
Add `node = "26.13.0"` under `[tools]`, remove `[monorepo]` block (`config_roots`), format via `tombi format`.

```toml
monorepo_root = true

[tools]
  python = "3.15.2"
  bun = "1.4.4"
  usage = { version = "7.3.0", workflow = "publish-cli.yml" }
  gradle = "9.9.0"
  java = "26.0.2+101"
  node = "26.13.0"

[env]
  WORKSPACE_ROOT = "{{config_root}}"
  _ = { file = ".env.local", python = { venv = { path = ".venv", create = false } } }
  PYTHONPATH = "{{config_root}}/lib:{{config_root}}/scripts:{{config_root}}"
  TRACKER_ALLOW_WRITES = "1"
  TRACKER_SOURCE = "store"
  MISE_TASK = "1"
  TUI_LOG_RETENTION_DAYS = "7"
  TUI_LOG_COOLDOWN_HOURS = "24"
  ANDROID_USER_HOME = "/Users/fnc314/.android"
  ANDROID_HOME = "/Users/fnc314/.android/sdk"
  JAVA_HOME = "/Users/fnc314/.local/share/mise/installs/java/26.0.2+101/Contents/Home"

[hooks]
  cd = "bash scripts/cleanup-tui-logs.sh"
```

---

### Component 2: Mise Orchestration for Konsist & Full Monorepo Validation

#### [MODIFY] `.config/mise/tasks/frontend/test.sh`
Orchestrate `:frontend:core-data:jvmTest` AND `:frontend:apps-core:jvmTest` (Konsist architectural tests):

```bash
#!/usr/bin/env bash
#MISE description="Run frontend unit and architectural tests (core-data, apps-core Konsist)"
set -euo pipefail

cd "${MISE_PROJECT_ROOT}"
exec "${MISE_PROJECT_ROOT}/gradlew" :frontend:core-data:jvmTest :frontend:apps-core:jvmTest "$@"
```

#### [MODIFY] `.config/mise/tasks/dev/konsist.sh`
Standardize working directory and execution parameters:

```bash
#!/usr/bin/env bash
#MISE description="Run Konsist architectural test suites across records and frontend"
#MISE alias="konsist"
set -euo pipefail

cd "${MISE_PROJECT_ROOT}"
exec "${MISE_PROJECT_ROOT}/gradlew" :records:records-parser:test :frontend:apps-core:jvmTest "$@"
```

#### [NEW] `.config/mise/tasks/dev/check.sh`
Orchestrate a unified full-validation workflow across static analysis, architecture rules, unit tests, and security guards:

```bash
#!/usr/bin/env bash
#MISE description="Run full monorepo validation across static analysis, Konsist, and unit test suites"
#MISE alias="check"
#MISE depends=["dev:lint", "records:test", "frontend:test", "dev:test"]

echo "✅ Full monorepo validation passed: Detekt, Konsist, records tests, frontend tests, and python tests."
```

#### [MODIFY] `.config/mise/tasks/frontend/readmes.sh`
Update path resolution to use `${MISE_PROJECT_ROOT}/frontend`.

#### [MODIFY] `.git/hooks/pre-commit`
Fix stale task invocation on line 15:
```sh
# Before: exec mise run ui:kmp:docs:readmes
# After:
exec mise run frontend:readmes
```

---

### Component 3: Subsystem Active Documentation Alignment

Align active documentation across `frontend/`, `records/`, and root `README.md`:

#### [MODIFY] `frontend/AGENTS.md`
- Update title and overview from `ui/kmp` to `frontend`.
- Update Rule 1 verification command to `mise run frontend:readmes`.
- Update Key Commands table to `frontend:*` tasks (`android:assemble`, `desktop:run`, `web:wasm:dev`, `readmes`, `test`, `build`).

#### [MODIFY] `frontend/README.md`
- Replace `ui/kmp/` ASCII hierarchy with `frontend/`.
- Update Mise tasks table to `frontend:*` commands.
- Update platform execution snippets to repo-root `./gradlew :frontend:...` commands.

#### [MODIFY] `frontend/core/*/README.md`
- `frontend/core/data/README.md`: Update commands to `./gradlew :frontend:core-data:...` and `mise run frontend:test`.
- `frontend/core/design-system/README.md`: Update commands to `./gradlew :frontend:core-design-system:...` and `mise run frontend:build`.
- `frontend/core/network/README.md`: Update commands to `./gradlew :frontend:core-network:...` and `mise run frontend:build`.
- `frontend/core/widgets/README.md`: Update commands to `./gradlew :frontend:core-widgets:...` and `mise run frontend:build`.
- `frontend/core/navigation/README.md`: Update commands to `./gradlew :frontend:core-navigation:...`.

#### [MODIFY] `frontend/features/*/README.md`
Update all 6 feature READMEs (`add-job-application`, `job-application-details`, `job-applications-list`, `settings`, `sign-in`, `tracker-edits`):
- Change `"Run the following commands from the ui/kmp directory:"` to `"Run the following commands from the repository root:"`.
- Update Gradle task targets to `:frontend:features-<name>-<layer>:...`.

#### [MODIFY] `frontend/apps/core/src/jvmTest/kotlin/.../KonsistArchitectureTest.kt`
Update test class docstring from `ui/kmp` to `frontend`.

#### [MODIFY] `records/AGENTS.md` & `records/.agents/plans/README.md`
Update `records + ui/kmp` references to `records + frontend`.

#### [MODIFY] `records/proto/src/main/proto/tracker/v1/README.md`
Update build instructions from `cd records && ./gradlew ...` to repo-root `./gradlew :records:...` and `mise run records:build`.

#### [MODIFY] `README.md`
Update layout tree to replace `ui/kmp/` with `frontend/`.

#### [DELETE & REGENERATE] Stale Task Documentation
- Purge stale markdown files in `docs/mise/` (`detekt.md`, `konsist.md`, `lint.md`, `ui-terminal-run.py.md`, etc.).
- Run `mise run docs:tasks` to regenerate clean documentation.
- Run `mise run completions:generate` to regenerate Zsh completions.

---

### Component 4: Reconcile Code, Security, and Tooling Drift from Instruction Consolidation

Reconcile underlying scripts and tooling with Junie's canonical `.agents/` layout:

#### [MODIFY] `scripts/rank.py` & `scripts/scrape.py`
Update hardcoded paths to point to `.agents/skills/`:
- `PROFILE_FILE = REPO_ROOT / ".agents" / "skills" / "job-application-assistant" / "01-candidate-profile.md"`
- `EVAL_FILE = REPO_ROOT / ".agents" / "skills" / "job-application-assistant" / "04-job-evaluation.md"`
- `QUERIES_FILE = REPO_ROOT / ".agents" / "skills" / "job-scraper" / "search-queries.md"`

#### [MODIFY] `lib/fit_evaluator.py`, `lib/tracker/fit_evaluator.py`, `scripts/check_framework_version.py`, `scripts/check_upstream_updates.py`
Update references from `.claude/skills/` to `.agents/skills/`.

#### [MODIFY] `scripts/lint_skills.py`
Update scanning targets to canonical `.agents/` directories:
```python
skills = sorted(ROOT.glob(".agents/skills/*/SKILL.md"))
commands = sorted((ROOT / ".agents" / "commands").glob("*.md"))
```

#### [MODIFY] `.gitignore`
Update personal data exclusion rules:
```gitignore
# Personal candidate outreach & profile disclosures (populated by /setup)
.agents/skills/job-application-assistant/01-candidate-profile.md
.agents/skills/job-application-assistant/02-behavioral-profile.md
.agents/skills/job-application-assistant/04-job-evaluation.md
.agents/skills/job-application-assistant/07-interview-prep.md
.agents/skills/job-scraper/search-queries.md

frontend/apps/android/google-services.json
```

#### [MODIFY] `AGENTS.md` & `.agents/README.md`
- In `AGENTS.md`: Update lines 20-22, 53, 78, 80 to `.agents/skills/` and `.agents/commands/`.
- In `.agents/README.md`: Update line 8 from `ui/kmp/.agents/plans/` to `frontend/.agents/plans/`.

#### [MODIFY] `scripts/tests/test_html_report_command.py`, `test_notion_sync_command.py`, `test_outcome_followup.py`
Update command paths from `.claude/commands/` to `.agents/commands/`.

---

## 4. Verification Plan

### Automated Tests
1. **Mise Config Validation**:
   ```bash
   test ! -f records/mise.toml && test ! -f backend/mise.toml && test ! -f frontend/mise.toml
   mise doctor
   tombi format mise.toml
   ```
2. **Skill Linter & Security Guards**:
   ```bash
   python3 scripts/lint_skills.py
   python3 scripts/security_guards.py
   ```
3. **Frontend Test Suite (including Konsist)**:
   ```bash
   mise run frontend:test
   ```
4. **Konsist Architecture Tests**:
   ```bash
   mise run dev:konsist
   ```
5. **Detekt Quality Gate**:
   ```bash
   mise run dev:detekt
   ```
6. **Unified Linting (Detekt + Konsist)**:
   ```bash
   mise run dev:lint
   ```
7. **Full Monorepo Validation Gate**:
   ```bash
   mise run dev:check
   ```
8. **Module README Verification**:
   ```bash
   mise run frontend:readmes
   ```
9. **Task Documentation & Completions**:
   ```bash
   mise run docs:tasks
   mise run completions:generate
   ```
10. **Zero Stale References in Modified Surfaces**:
    ```bash
    git grep -n -E "(ui/kmp|ui:kmp)" frontend/ records/ README.md
    ```

### Manual Verification
1. Inspect `git status` and `git diff` to verify only intended files were modified.
2. Confirm pre-commit hook runs cleanly without errors.
