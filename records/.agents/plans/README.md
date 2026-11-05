# Records Subsystem Architecture & Design Plans

This directory contains technical design and implementation plans oriented specifically to the `records` subsystem (`records-proto` and `records-parser`).

> [!NOTE]
> For repository-wide cross-subsystem plans that span multiple components (such as `records` + `ui/kmp` or root tracker CLI), refer to the root architectural plans directory at [`.agents/plans/`](../../.agents/plans/README.md).

---

## Plan Invariants (MANDATORY)
1. **Date Subdirectory**: Plans must be saved in a `YYYY-MM-DD/` subdirectory matching the creation date.
2. **24-Hour Timestamp Prefix**: File names must be formatted as `HHMMSS_<plan-name>.md` using a 24-hour clock (`00`-`23` for `HH`).
3. **Registry Synchronization**: Register the plan in the table below with its relative path, status, and scope.

---

### Records Plans

| Plan | Status | Scope / Focus Area |
| :--- | :--- | :--- |
<!-- Future records subsystem plans will be registered here -->
