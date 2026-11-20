# Implementation Plan: Backend Firebase Cloud Messaging (FCM) Push Notifications

**Status:** Completed  
**Date:** 2026-11-20  
**Target:** `backend`  
**Harness:** Antigravity  
**Model:** gemini-3.8-flash  
**Session:** abbfb0e8-5323-4b16-ab25-0ca4d047341d  
**Depends On:** [`../../.agents/plans/2026-11-20/145500_firebase-push-notifications-and-proposal-override-architecture.md`](../../.agents/plans/2026-11-20/145500_firebase-push-notifications-and-proposal-override-architecture.md)  

---

## 1. Executive Summary & Goals
This plan details the backend implementation for Firebase Cloud Messaging (FCM) integration in the Cloud Run service (`:backend:service`). Whenever an email sweep (`/tasks/sweep` or `/emails/tasks/backfill`) detects and stages new proposals or low-confidence status transitions in Firestore, the backend dispatches targeted FCM push notifications to registered user devices.

## 2. Architecture & Design Principles
- **Protobuf Contract Authority:** Defined in `records/proto/src/main/proto/tracker/notification.proto` (`TrackerNotificationPayload` and `DeviceRegistration`).
- **Scale-to-Zero Stateless Delivery:** Uses `FirebaseMessaging.getInstance()` from `com.google.firebase:firebase-admin` (already available in the backend classpath).
- **Device Registration Subcollection:** Stores active device push tokens under `users/{userId}/devices/{deviceId}`:
  ```json
  {
    "token": "fcm_token_string",
    "platform": "DEVICE_PLATFORM_ANDROID",
    "updatedAt": "2026-11-20T19:50:00Z"
  }
  ```
- **Payload Contract:** Data messages contain navigation keys and payload summaries matching `TrackerNotificationPayload` so the client can navigate directly to `@frontend/features/tracker-edits/ui` without extra lookups:
  ```json
  {
    "route": "trackeredits",
    "proposalId": "ba6b7784-2ebf-480f-b628-42af1e535ee7",
    "title": "Review Staged Email Updates",
    "body": "3 new status transitions staged from recent emails",
    "stagedChangesCount": "3",
    "click_action": "FLUTTER_NOTIFICATION_CLICK"
  }
  ```
- **Quiet Period & Throttling:** Debounces batch sweeps so backfilled emails don't spam the user with dozens of notifications; sends a single grouped summary notification per sweep.

## 3. Proposed Changes & Implementation Steps

### 3.1 Records Proto Definition
- Create `records/proto/src/main/proto/tracker/notification.proto`.
- Define `DevicePlatform`, `DeviceRegistration`, and `TrackerNotificationPayload`.
- Compile via `mise run records:build`.

### 3.2 Device Lifecycle & Heartbeat Endpoints
- **Register:** `POST /notifications/device/register`
  - Re-registers matching existing devices.
  - Enforces a 5-device maximum per user by querying `users/{userId}/devices` ordered by `updatedAt`/`lastInteractedAt` ascending and revoking the oldest/least-recently-seen device before adding the new one.
- **Deregister:** `POST /notifications/device/deregister`
  - Tapped from Settings "Deregister from Notifications" button or sign-out.
  - Removes device record from `users/{userId}/devices/{deviceId}`.
- **Heartbeat:** `POST /notifications/device/heartbeat`
  - Sent by client upon notification tap.
  - Receives *bare bones* device identifiers (make, model, OS version, app version, tapped proposal ID; no user PII).
  - Stored at: `users/{userId}/devices/{deviceId}/heartbeats/{epochSecondsTimestamp}`.
  - Updates `lastInteractedAt` on the parent device document.
  - Enforces a maximum retention cap of **20 heartbeat entries** per device, purging older entries on insert.
  - Serves as interactive telemetry proving notification engagement.
- **Admin Purge:** `DELETE /notifications/devices` and `GET /notifications/devices` for maintenance.

### 3.2 Notification Service in Backend (`NotificationDispatchService`)
- Create `dev.fnc314.aijobtracker.backend.notifications.NotificationDispatchService`:
  ```kotlin
  public class NotificationDispatchService(
      private val db: Firestore,
      private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
  ) {
      public suspend fun notifyNewProposal(proposalId: String, changeCount: Int, changesSummary: String)
  }
  ```

### 3.3 Hook into Cloud Run Sweep Route (`/tasks/sweep`)
- When `stagedChanges.isNotEmpty()` in `BackendService.kt`, invoke `notificationDispatchService.notifyNewProposal(...)`.

## 4. Verification Plan & Testing Checklist
- **Unit Tests:** Mock `FirebaseMessaging` to verify payload formatting and token retrieval logic.
- **Integration Test:** Local run with test device token verifying message build via Firebase Admin SDK.
- **Cloud Run Verification:** Trigger `/tasks/sweep` remotely and verify Cloud Logging reports successful FCM delivery.
