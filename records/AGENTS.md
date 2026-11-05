# Agent Guidelines: Records Workspace (`records`)

This document defines architecture invariants, development workflows, and mandatory rules for AI coding assistants working in the `records` workspace (`records-proto` and `records-parser`).

---

## 1. Workspace Architecture Map

The `records` project provides the canonical protobuf schema contracts and JVM parsing engines for job search records:
- **`records-proto`**: Protobuf schema contracts (`src/main/proto/tracker/v1/application.proto`) compiled to Java/Kotlin models.
- **`records-parser`**: Multiplatform parsing utilities, timestamp conversions, status normalizers, JPype Python bridge, and legacy row projectors.

---

## 2. Invariants & Rules for AI Agents (MANDATORY)

### Rule 1: Architecture & Design Plans in `.agents/plans/` (MANDATORY)
All technical architecture, design, and migration plans for `records` must adhere to these rules:
- **Location**: Technical architecture and design plans oriented specifically to `records` must be housed in `records/.agents/plans/` and indexed in `records/.agents/plans/README.md`. Any plan that spans across multiple subsystems (e.g. `records` + `ui/kmp` or root tracker CLI) belongs in root `.agents/plans/`.
- **Date Subdirectory & 24-Hour Timestamp Filename**: Plans must **NEVER** be stored as flat files. Each plan must be placed in a `YYYY-MM-DD/` folder based on its creation date and prefixed with a 24-hour clock timestamp: `HHMMSS_<plan-name>.md` (using `00`-`23` for `HH`).
- **Registry Synchronization**: Upon creating or updating any plan, the agent must immediately update `records/.agents/plans/README.md` with the link (`./YYYY-MM-DD/HHMMSS_<plan-name>.md`), status, and scope description.

### Rule 2: Strict Kotlin Explicit API Mode
All declarations in `records-parser` must enforce explicit visibility (`public`, `internal`, `private`) and explicit return types on all functions and properties.

### Rule 3: Zero Fabrication & Factual Grounding
Every change to parsing rules, status normalizers, or protobuf schemas must adhere strictly to verified data contracts in `tracker.schema.json` and Firestore schema v7 specifications.
