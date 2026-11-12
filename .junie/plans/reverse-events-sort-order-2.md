---
sessionId: session-261110-184215-1j6w
---

# Technical Design

### Current Implementation

In `JobApplicationDetailUiModel.kt` (line 155), the `deriveTimelineEvents()` function sorts events by `createdAt` descending (newest first):

```kotlin
if (events.isNotEmpty()) return events.sortedByDescending { it.createdAt }.toImmutableList()
```

This means the timeline in the job application details screen shows the most recent events at the top.

### Proposed Change

Reverse the sort order to ascending (oldest first) so the timeline reads chronologically from top to bottom:

```kotlin
if (events.isNotEmpty()) return events.sortedBy { it.createdAt }.toImmutableList()
```

**File modified:** `frontend/features/job-application-details/ui/src/commonMain/kotlin/.../JobApplicationDetailUiModel.kt` (line 155)

No tests exist for `deriveTimelineEvents()`, and no other files reference the sort order. This is a single-line change with no cascading impacts.


# Delivery Steps

###   Step 1: Reverse event sort order in JobApplicationDetailUiModel
Change `sortedByDescending` to `sortedBy` in `deriveTimelineEvents()` so events display chronologically (oldest first).
- Update line 155 in `JobApplicationDetailUiModel.kt` to sort ascending by `createdAt`.
- Verify the build passes (`./gradlew :frontend:features:job-application-details:ui:compileKotlin`).