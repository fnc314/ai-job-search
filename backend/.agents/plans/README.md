# Backend Subsystem Architecture & Design Plans

This directory contains design and implementation plans for the `backend` subsystem (Cloud Run, scheduled sweeps, email classification, and push notifications).

> [!NOTE]
> For repository-wide cross-subsystem plans that span `records`, `backend`, and `frontend`, refer to the root architectural plans directory at [`.agents/plans/`](../../.agents/plans/README.md).

---

## Plan Invariants (MANDATORY)
1. **Target-Derived Location**: Subsystem plans specific to `backend` live here. Cross-subsystem plans belong in root `.agents/plans/`.
2. **Date Subdirectory**: Plans must be saved in a `YYYY-MM-DD/` folder matching creation date.
3. **24-Hour Timestamp Prefix**: Filenames must be formatted as `HHMMSS_<plan-name>.md` using a 24-hour clock (`00`-`23` for `HH`).
4. **Registry Synchronization**: Register the plan in the table below with its relative path (`./YYYY-MM-DD/HHMMSS_<plan-name>.md`), status, and scope.
5. **Mandatory Opening Frontmatter**: Start every plan from [`TEMPLATE.md`](../../.agents/plans/TEMPLATE.md) (`Status`, `Date`, `Target` block); `Status` must match the registry row.

---

### Backend Plans

| Plan | Status | Scope / Focus Area |
| :--- | :--- | :--- |
| [`145500_backend-fcm-push-notifications.md`](./2026-11-20/145500_backend-fcm-push-notifications.md) | Completed | Backend FCM push notification dispatch for staged email proposals, device token registration, and Cloud Run sweep triggers |
