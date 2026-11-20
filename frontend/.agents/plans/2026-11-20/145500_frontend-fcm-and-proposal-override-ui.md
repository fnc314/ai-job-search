# Implementation Plan: Frontend FCM Token Management, Notification Routing & Proposal Inline Overrides

**Status:** Completed  
**Date:** 2026-11-20  
**Target:** `frontend`  
**Harness:** Antigravity  
**Model:** gemini-3.8-flash  
**Session:** abbfb0e8-5323-4b16-ab25-0ca4d047341d  
**Depends On:** [`../../.agents/plans/2026-11-20/145500_firebase-push-notifications-and-proposal-override-architecture.md`](../../.agents/plans/2026-11-20/145500_firebase-push-notifications-and-proposal-override-architecture.md)  

---

## 1. Executive Summary & Goals
This plan details frontend updates across Android/iOS/Desktop clients to:
1. Register push notification device tokens with the Cloud Run backend upon authentication.
2. Route incoming FCM push notifications directly to `JobApplicationEditsRoute` (`@frontend/features/tracker-edits/ui`).
3. Enhance `TrackerEditsScreen` with inline edit capabilities, allowing the user to override or correct parsed email event types and status transitions (e.g., correcting an `ACKNOWLEDGE` mistakenly flagged as `REJECT`) directly inside the UI before clicking Approve.

## 2. Architecture & Design Principles
- **Multiplatform Abstraction:** Define an expect/actual `PushNotificationManager` interface in `frontend/core/network` or `frontend/core/data`:
  ```kotlin
  public interface PushNotificationManager {
      public suspend fun initialize()
      public suspend fun getDeviceToken(): String?
  }
  ```
- **Deep-Link Navigation:** Use the shared `tracker.TopLevelDestination` proto mapped to the local UI navigation graph. When a notification with `destination = EDITS` or `route = "trackeredits"` is opened, `LocalAppNavigator` navigates to `JobApplicationEditsRoute`.
- **Inline Editing in TrackerEditsScreen (Full-Page Experience, No Dialogs):**
  - **No Dialogs for Forms:** Forms and overrides live directly on the page within the proposal cards or an inline editable pane. Users tweak the event type (dropdown or segmented buttons) and target status inline without leaving the content stream.
  - **Swipe-to-Action for Proposals (`SwipeToDismissBox`):**
    - Quick actions per proposal item:
      - Swipe Right: "Accept as is" (Approve).
      - Swipe Left: "Remove proposal entry" (Reject / Dismiss).
      - Action $\leftrightarrow$ direction pairing configurable via `AppSettings`.
  - **A11y Standards: Explicit Dismissal Stacked Snackbars with UNDO:**
    - Per A11y guidelines, nuclear/destructive actions cannot be irreversible without confirmation or undo capability.
    - Dismissed items enter a pending state and trigger a long-term/explicitly dismissable Snackbar (`SnackbarDuration.Indefinite`).
    - When multiple swipe actions occur in succession, snackbar actions stack with the top-most displaying the most recent action details.
    - Tapping **UNDO** restores the proposal item locally without committing to Firestore.
    - Explicit dismissal or expiration commits the change to Firestore `pending-proposals/{proposalId}`.

## 3. Proposed Changes & Implementation Steps

### 3.1 Android Push Service (`frontend/apps/android`)
- Implement `FirebaseMessagingService` in `apps/android`:
  - `onNewToken(token: String)`: Dispatches token update to backend `POST /api/notifications/register-device`.
  - `onMessageReceived(remoteMessage: RemoteMessage)`: Shows system notification with pending intent directed to `MainActivity` containing deep-link extra.

### 3.2 Tracker Edits Feature Enhancements (`frontend/features/tracker-edits`)
- **Domain Layer (`frontend/features/tracker-edits/domain`):**
  - Add `EditProposedChangeUseCase` (inheriting from `UseCase<Params, TrackerEdit>` imported from `dev.fnc314.aijobtracker.frontend.core.data.repository.UseCase`):
    ```kotlin
    import dev.fnc314.aijobtracker.frontend.core.data.repository.UseCase
    import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit
    import dev.fnc314.aijobtracker.records.TrackerEventType
    import dev.fnc314.aijobtracker.records.TrackerStatus

    public fun interface EditProposedChangeUseCase : UseCase<EditProposedChangeUseCase.Params, TrackerEdit> {
        public data class Params(
            public val proposalId: String,
            public val itemIndex: Int,
            public val newEventType: TrackerEventType,
            public val newStatusTo: TrackerStatus?,
            public val reason: String? = null,
        )
    }
    ```
- **UI Layer (`frontend/features/tracker-edits/ui`):**
  - Add `OnEditProposedChange(index: Int, eventType: String, statusTo: String?, note: String?)` to `TrackerEditsUiIntent`.
  - In `TrackerEditsScreen.kt`, add an "Edit" button next to "Approve" / "Reject" on each change card.
  - Clicking "Edit" opens a modal bottom sheet / dialog with segmented buttons or dropdowns for Event Type and Status Transition.

## 4. Verification Plan & Testing Checklist
- **Unit Tests:** Test `EditProposedChangeUseCaseTest` verifying proposal item updates.
- **Compose Preview & Manual Verification:**
  - Open `TrackerEditsScreen` via `JobApplicationEditsRoute`.
  - Tap "Edit" on a test change, update status transition to `NONE`, confirm proposal updates in Firestore.
  - Trigger test FCM notification from Firebase Console and verify deep linking directly to the edits screen.
