# Implementation Plan: RM-010 Separate out RoadmapApp.kt to internal/apps/terminal/roadmap

**Status:** Completed
**Date:** 2026-11-15
**Target:** `internal/apps/terminal/roadmap`, `frontend/apps/terminal`, `ROADMAP.md`

---

## 1. Context & Objectives
In `ROADMAP.md`:
- `[RM-010] Separate out RoadmapApp.kt from frontend/apps/terminal`
- Goal: Extract roadmap management logic (`RoadmapApp`, `RoadmapParser`, `RoadmapWriter`, `RoadmapItem`, `RoadmapChangelogEntry`) out of the user-facing job tracker terminal app into a dedicated internal standalone application module located at `internal/apps/terminal/roadmap`.
- Ensure clean Gradle multi-module / composite build integration, strict Kotlin `explicitApi()` compliance, complete KDocs on public declarations, and preserve all `mise roadmap:edit` and CLI ergonomics.

## 2. Proposed Architecture & Steps
1. **Module Creation in `internal/apps/terminal/roadmap`**:
   - Establish `internal/` subsystem as an included build in root `settings.gradle.kts`.
   - Setup `internal/settings.gradle.kts` declaring `:apps-terminal-roadmap` (at directory `apps/terminal/roadmap`).
   - Create `internal/apps/terminal/roadmap/build.gradle.kts` using Kotlin JVM + application plugin with `mainClass.set("dev.fnc314.tools.jobtracker.internal.terminal.roadmap.MainKt")`.
   - Relocate `RoadmapApp.kt` to `internal/apps/terminal/roadmap/src/main/kotlin/dev/fnc314/tools/jobtracker/internal/terminal/roadmap/RoadmapApp.kt`.
   - Provide `main(args: Array<String>)` entry point in `Main.kt`.
2. **Update `frontend/apps/terminal`**:
   - Remove `RoadmapApp.kt` from `frontend/apps/terminal/src/main/kotlin/.../roadmap/`.
   - In `TrackerApp.kt` and `Main.kt`, remove dispatch to roadmap or shell out if invoked with `--roadmap`.
3. **Update Tasks & Automation**:
   - Update `.config/mise/tasks/roadmap/edit.sh` to target `:internal:apps-terminal-roadmap:installDist` and binary at `internal/apps/terminal/roadmap/build/install/apps-terminal-roadmap/bin/apps-terminal-roadmap`.
4. **Verification**:
   - Run Konsist tests (`mise run dev:konsist`).
   - Run Detekt (`mise run dev:detekt`).
   - Run security & naming guards (`mise run dev:guards`).
   - Run `mise run roadmap:edit --help` / `mise run roadmap:status`.
   - Update `ROADMAP.md` marking RM-010 as DONE (100%).
