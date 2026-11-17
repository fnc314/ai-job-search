# Implementation Plan: Migrate mise Tasks to ZSH and Configure GitHub Actions

**Status:** COMPLETED
**Date:** 2026-11-17
**Target:** Migrate all task scripts across `.config/mise/tasks` to `.zsh` with `#!/usr/bin/env zsh`, standardize GitHub Actions CI/CD workflows to install and run steps with `shell: zsh {0}`, and update task document generators and completions.
**Depends On:** `.agents/plans/2026-11-16/201200_rm-001-final-sprint-plan.md`

---

## 1. Goal Description
The repository currently uses `.sh` scripts with bash/sh shebangs for its mise tasks. We are migrating all mise tasks and utility scripts to `.zsh` scripts using `#!/usr/bin/env zsh`, taking advantage of modern zsh capabilities and standardizing the shell environment across macOS (local development) and Linux (GitHub Actions runners), as demonstrated in [`fnc314.github.io/.github/workflows/gh-pages.yaml`](https://github.com/fnc314/fnc314.github.io/blob/main/.github/workflows/gh-pages.yaml#L14-L17).

## 2. Proposed Changes

### Component 1: Mise Tasks Migration (`.config/mise/tasks/`)
- Rename all 96 `.sh` files to `.zsh`.
- Replace shebangs: change `#!/usr/bin/env bash` or `#!/usr/bin/env sh` to `#!/usr/bin/env zsh`.
- Update `.config/mise/tasks/secrets/decrypt` and `encrypt` with `#!/usr/bin/env zsh`.
- Ensure strict error handling (`set -euo pipefail`) executes properly under zsh semantics.

### Component 2: Git Hooks & Task Generators
- Update `.githooks/pre-commit` to use `#!/usr/bin/env zsh`.
- Update task generators (`docs/tasks.zsh`) to document the `.zsh` tasks.

### Component 3: GitHub Actions Workflows (`.github/workflows/`)
Following the pattern from `fnc314.github.io`:
```yaml
      - name: Install Zsh
        run: |
          sudo apt-get update
          sudo apt-get install -y zsh
```
and setting `shell: zsh {0}` on all steps invoking `mise run`.
- **`ci.yml`**: Add Zsh installation to Ubuntu runners and set `shell: zsh {0}`.
- **`pages.yml`**: Add Zsh installation to documentation deployment.
- **`deploy-backend.yml`**: Ensure consistency.

### Component 4: Documentation & Completions
- Re-run `mise run docs:tasks` to regenerate task documentation for all `.zsh` scripts in `docs/mise/`.
- Re-run `mise run completions:generate` to regenerate Zsh completions for all tasks.

---

## 3. Verification Plan

### Automated Tests
1. **Shell Syntax Check**:
   - Run `zsh -n` across all `.config/mise/tasks/**/*.zsh`.
2. **Task Validation**:
   - `mise run roadmap:status`
   - `mise run dev:test`
   - `mise run dev:konsist`
   - `mise run dev:guards`
   - `mise run tracker:check`
3. **Docs & Completions Generation**:
   - `mise run docs:tasks`
   - `mise run completions:generate`
