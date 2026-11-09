# Implementation Plan: Android Date Display, Timestamp Deserialization, and Typography/Color Contrast Resolution

**Status:** Completed  
**Date:** 2026-11-08  
**Target:** `ui/kmp` (`features/job-applications-list`, `features/job-application-details`, `core/data`, `core/design-system`)  
**Depends On:** [`ui/kmp/.agents/plans/2026-11-08/162542_sorting-and-timestamp-investigation-and-fix.md`](./162542_sorting-and-timestamp-investigation-and-fix.md)  

---

## 1. Executive Summary & Problem Analysis

### Problem Statement
On Android, the UI fails to display dates across cards, timelines, and details screens:
- Closed cards show dangling prefixes like `"Created on "` with no trailing date.
- Expanded tiles show blank spaces for "Applied on" and "Updated".
- Details screens either drop timeline events or omit relative update timestamps.
- Dates rendered on cards and timeline events use `MaterialTheme.colorScheme.outline`, a subtle border token with substandard contrast (~4.4:1 on card backgrounds) that borders on WCAG AA non-compliance.
- Relative timestamp formatting previously hardcoded English strings (`"just now"`, `"$totalMinutes hours ago"`, `"1 day ago"`) inside `core/data`, violating UI localization standards.

### Root Cause & Firestore SDK Return Types Analysis
`RemoteDataSource` calls `doc.data` on Firebase `DocumentSnapshot`. In the official SDKs:
- **Android Firebase SDK (`com.google.firebase:firebase-firestore:27.1.0`)**:
  - `DocumentSnapshot.getData()` returns `Map<String, Any?>` where values map to native Java types:
    - **Firestore Timestamp** -> `com.google.firebase.Timestamp` (properties: `seconds: Long`, `nanoseconds: Int`, method: `toDate(): Date`)
    - **Firestore Integer** -> `java.lang.Long`
    - **Firestore Float** -> `java.lang.Double`
    - **Firestore String** -> `java.lang.String`
    - **Firestore Map** -> `java.util.Map<String, Any?>`
    - **Firestore List** -> `java.util.List<Any?>`
    - **Firestore GeoPoint** -> `com.google.firebase.firestore.GeoPoint`
    - **Firestore Reference** -> `com.google.firebase.firestore.DocumentReference`
    - **Firestore Blob** -> `com.google.firebase.firestore.Blob`
  - In `RemoteJobApplicationsListDataSourceImpl`, calling `Instant.parse(this.toString())` on a `com.google.firebase.Timestamp` passed a string shaped like `"Timestamp(seconds=1731089200, nanoseconds=0)"`. Kotlin's standard `Instant.parse()` expects an ISO-8601 string, threw a `DateTimeFormatException`, and silently resolved to `null`.
- **JVM Cloud Firestore SDK (`com.google.firebase:firebase-admin:9.5.0` -> `com.google.cloud:google-cloud-firestore`)**:
  - `DocumentSnapshot.getData()` returns `Map<String, Any?>` where timestamps are `com.google.cloud.Timestamp` (properties: `seconds: Long`, `nanos: Int`, method: `toDate(): Date`).

### User Directives & Refinements
1. **Typed SDK Boundary Conversion (No Regex or String Assumptions)**:
   - Convert typed `Timestamp` instances into `kotlin.time.Instant` at the platform boundary (`RemoteDataSource.android.kt` and `RemoteDataSource.jvm.kt`) calling official SDK accessors directly (`.seconds` and `.nanoseconds` / `.nanos`).
   - Platform SDK types never cross into common multiplatform code.
2. **Domain Non-Null Philosophy ("NO NULL")**:
   - Domain models (`JobApplication`, `Timeline`, `TrackerEvent`) use non-null `Instant`. Missing or unparseable database values resolve to `Instant.DISTANT_PAST`.
3. **Cross-Platform Room Isolation (Zero Schema Changes / Zero Migrations)**:
   - `JobApplicationEntity` (Room SQLite) **remains unchanged** with `Instant?`. This guarantees zero SQLite schema divergence across Android, Desktop (JVM), and iOS, avoiding database migrations.
   - `JobApplicationMapper` handles translation: mapping `Instant?` in SQLite to `Instant.DISTANT_PAST` in domain, and `Instant.DISTANT_PAST` back to `null` in SQLite.
4. **Dedicated `InstantParser` Utility**:
   - Scoped to `InstantParser.parse(raw: Any?, default: Instant = Instant.DISTANT_PAST): Instant`. Avoid broad `Any?` extensions.
5. **All UI Text in Compose Resources (`Res.string`)**:
   - All text displayed to the user—including relative time units (`time_just_now`, `time_minutes_ago`, `time_hours_ago`, `time_days_ago`, `time_weeks_ago`), headers, and fallback states (`data_missing`)—must come from `Res.string`. Zero hardcoded English strings.
6. **Encapsulate Fallback Formatting in UI Models**:
   - Composables must not execute raw ternary null/blank checks.
   - Encapsulate formatting methods directly on the UI models: `model.createdDateDisplay(backup = stringResource(Res.string.data_missing))`, `model.appliedDateDisplay(backup = ...)`, `model.updatedDateDisplay(backup = ...)`, `model.fitScoreDisplay(backup = ...)`.
7. **Flatten Hierarchy with `ConstraintLayout`**:
   - Flatten the compact card's expanded state using `ConstraintLayout` (from `androidx.constraintlayout:constraintlayout-compose:1.1.2`). This eliminates nested `Row`/`Column` passes, guarantees alignment, and enables smoother expand/collapse animations.

---

## 2. Architecture & Design Principles

```mermaid
flowchart TD
    subgraph AndroidPlatform [Android Platform: AndroidRemoteDataSource]
        DocDataAndroid["doc.data (Firebase Document)"] --> SanitizeAndroid["sanitizeFirestoreValue()"]
        FirebaseTs["com.google.firebase.Timestamp"] -->|Official SDK: .seconds, .nanoseconds| KmpInstantAndroid["Instant.fromEpochSeconds(sec, nano)"]
    end

    subgraph JvmPlatform [JVM Platform: JvmRemoteDataSource]
        DocDataJvm["doc.data (Cloud Firestore Document)"] --> SanitizeJvm["sanitizeFirestoreValue()"]
        CloudTs["com.google.cloud.Timestamp"] -->|Official SDK: .seconds, .nanos| KmpInstantJvm["Instant.fromEpochSeconds(sec, nanos)"]
    end

    subgraph CommonDataLayer [Common Data Layer: core/data & features]
        SanitizeAndroid --> MapRecord["MapRemoteRecord.fields (kotlin.time.Instant)"]
        SanitizeJvm --> MapRecord
        MapRecord --> InstantParser["InstantParser.parse(raw, default)"]
        InstantParser --> DomainEntity["JobApplication & Timeline (Non-Null Instant)"]
        DomainEntity <-->|JobApplicationMapper: Instant <-> Instant? (Zero Migrations)| RoomEntity["JobApplicationEntity (Room SQLite: Instant?)"]
    end

    subgraph PresentationLayer [Presentation Layer: features/job-applications-list/ui]
        DomainEntity --> UiModel["JobApplicationItemUiModel (with .display(backup) methods)"]
        ResStrings["Res.string (time_*, data_missing, headers)"] --> UiModel
        UiModel --> ConstraintRow["JobApplicationRow (ConstraintLayout: Flattened Hierarchy & Grid)"]
    end
```

### 1. Platform Boundary Conversion Details
- Android: `com.google.firebase.Timestamp` -> `Instant.fromEpochSeconds(value.seconds, value.nanoseconds.toLong())`.
- JVM: `com.google.cloud.Timestamp` -> `Instant.fromEpochSeconds(value.seconds, value.nanos.toLong())`.
- Collections (maps, lists) are traversed recursively so nested timestamps inside `timeline` or audit blocks are converted before reaching domain repositories.

### 2. Room Cross-Platform Preservation (No Migrations)
- In `JobApplicationEntity.kt`: `createdAt: Instant?` and `updatedAt: Instant?` remain nullable. SQLite schema version remains `8`.
- In `JobApplicationMapper.kt`:
  - `toDomain`: `createdAt = entity.createdAt ?: Instant.DISTANT_PAST`, `updatedAt = entity.updatedAt ?: Instant.DISTANT_PAST`
  - `toEntity`: `createdAt = if (domain.createdAt == Instant.DISTANT_PAST) null else domain.createdAt`, `updatedAt = if (domain.updatedAt == Instant.DISTANT_PAST) null else domain.updatedAt`

### 3. Model Encapsulation & Localized Relative Time
- In `JobApplicationItemUiModel`:
  - Holds non-null raw domain properties (`createdAt: Instant`, `updatedAt: Instant`, `firstActivityAt: Instant`, `fitScore: Int?`).
  - Encapsulates display methods accepting localized fallback strings:
    - `@Composable fun createdDateDisplay(backup: String = stringResource(Res.string.data_missing)): String`
    - `@Composable fun appliedDateDisplay(backup: String = stringResource(Res.string.data_missing)): String`
    - `@Composable fun updatedDateDisplay(backup: String = stringResource(Res.string.data_missing)): String`
    - `fun fitScoreDisplay(backup: String): String = fitScore?.toString() ?: backup`
- Relative time formatting (`formatRelativeInstant`) resolves all strings (`time_just_now`, `time_minutes_ago`, `time_hours_ago`, `time_days_ago`, `time_weeks_days_ago`, `time_months_ago`) via `stringResource(Res.string.<key>)`.

---

## 3. Concrete Implementation Steps

### 3.1 `core/data`: Platform Boundary Conversions

#### [MODIFY] `core/data/src/androidMain/kotlin/dev/fnc314/tools/jobtracker/core/data/source/RemoteDataSource.android.kt`
Use imports for `com.google.firebase.Timestamp` and `kotlin.time.Instant`. Recursively sanitize incoming Firestore document data:
```kotlin
package dev.fnc314.tools.jobtracker.core.data.source

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlin.time.Instant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

internal class AndroidRemoteDataSource(
    private val firebase: FirebaseApp = FirebaseApp.getInstance(),
) : RemoteDataSource {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(firebase)

    override fun observeCollection(collectionPath: String): Flow<List<RemoteRecord>> = callbackFlow {
        val listener = firestore.collection(collectionPath).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val records = snapshot?.documents?.map { doc ->
                MapRemoteRecord(id = doc.id, fields = sanitizeFirestoreMap(doc.data ?: emptyMap()))
            } ?: emptyList()
            trySend(records)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getDocument(collectionPath: String, documentId: String): RemoteRecord? {
        val doc = firestore.collection(collectionPath).document(documentId).get().await()
        if (!doc.exists()) return null
        return MapRemoteRecord(id = doc.id, fields = sanitizeFirestoreMap(doc.data ?: emptyMap()))
    }

    override suspend fun setDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult {
        return try {
            firestore.collection(collectionPath).document(documentId).set(record.fields).await()
            DataResult.Success
        } catch (e: Throwable) {
            DataResult.Failure(e)
        }
    }

    override suspend fun updateDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult {
        return try {
            firestore.collection(collectionPath).document(documentId).update(record.fields).await()
            DataResult.Success
        } catch (e: Throwable) {
            DataResult.Failure(e)
        }
    }
}

private fun sanitizeFirestoreMap(map: Map<String, Any?>): Map<String, Any?> {
    return map.mapValues { (_, value) -> sanitizeFirestoreValue(value) }
}

private fun sanitizeFirestoreValue(value: Any?): Any? {
    return when (value) {
        is Timestamp -> Instant.fromEpochSeconds(value.seconds, value.nanoseconds.toLong())
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            sanitizeFirestoreMap(value as Map<String, Any?>)
        }
        is List<*> -> value.map { sanitizeFirestoreValue(it) }
        else -> value
    }
}
```

#### [MODIFY] `core/data/src/jvmMain/kotlin/dev/fnc314/tools/jobtracker/core/data/source/RemoteDataSource.jvm.kt`
Use imports for `com.google.cloud.Timestamp` and `kotlin.time.Instant`. Recursively sanitize incoming Cloud Firestore document data:
```kotlin
package dev.fnc314.tools.jobtracker.core.data.source

import com.google.cloud.firestore.Firestore
import com.google.cloud.Timestamp
import com.google.firebase.FirebaseApp
import com.google.firebase.cloud.FirestoreClient
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class JvmRemoteDataSource(
    private val firebase: FirebaseApp = FirebaseApp.getInstance(),
) : RemoteDataSource {
    private val firestore: Firestore = FirestoreClient.getFirestore(firebase)

    override fun observeCollection(collectionPath: String): Flow<List<RemoteRecord>> = flow {
        val snapshot = firestore.collection(collectionPath).get().get()
        val records = snapshot.documents.map { doc ->
            MapRemoteRecord(id = doc.id, fields = sanitizeFirestoreMap(doc.data ?: emptyMap()))
        }
        emit(records)
    }

    override suspend fun getDocument(collectionPath: String, documentId: String): RemoteRecord? {
        val doc = firestore.collection(collectionPath).document(documentId).get().get()
        if (!doc.exists()) return null
        return MapRemoteRecord(id = doc.id, fields = sanitizeFirestoreMap(doc.data ?: emptyMap()))
    }

    override suspend fun setDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult {
        return try {
            firestore.collection(collectionPath).document(documentId).set(record.fields).get()
            DataResult.Success
        } catch (e: Throwable) {
            DataResult.Failure(e)
        }
    }

    override suspend fun updateDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult {
        return try {
            firestore.collection(collectionPath).document(documentId).update(record.fields).get()
            DataResult.Success
        } catch (e: Throwable) {
            DataResult.Failure(e)
        }
    }
}

private fun sanitizeFirestoreMap(map: Map<String, Any?>): Map<String, Any?> {
    return map.mapValues { (_, value) -> sanitizeFirestoreValue(value) }
}

private fun sanitizeFirestoreValue(value: Any?): Any? {
    return when (value) {
        is Timestamp -> Instant.fromEpochSeconds(value.seconds, value.nanos.toLong())
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            sanitizeFirestoreMap(value as Map<String, Any?>)
        }
        is List<*> -> value.map { sanitizeFirestoreValue(it) }
        else -> value
    }
}
```

---

### 3.2 `core/data`: Dedicated Utility & Formatter

#### [NEW] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/util/InstantParser.kt`
Dedicated utility object parsing known multiplatform types:
```kotlin
package dev.fnc314.tools.jobtracker.core.data.util

import kotlin.time.Instant

/**
 * Utility parser converting known multiplatform raw types to [Instant].
 *
 * If the input is null, missing, or unparseable, returns [default] (defaults to [Instant.DISTANT_PAST]).
 * Avoids broad Any? extensions or regex matching on platform SDK objects.
 */
public object InstantParser {
    private val DATE_ONLY_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")

    public fun parse(raw: Any?, default: Instant = Instant.DISTANT_PAST): Instant {
        if (raw == null) return default
        return when (raw) {
            is Instant -> raw
            is Long -> {
                if (raw < 100_000_000_000L) Instant.fromEpochSeconds(raw) else Instant.fromEpochMilliseconds(raw)
            }
            is Number -> {
                val longVal = raw.toLong()
                if (longVal < 100_000_000_000L) Instant.fromEpochSeconds(longVal) else Instant.fromEpochMilliseconds(longVal)
            }
            is String -> {
                val trimmed = raw.trim()
                if (trimmed.isEmpty()) return default
                if (DATE_ONLY_REGEX.matches(trimmed)) {
                    runCatching { Instant.parse("${trimmed}T00:00:00Z") }.getOrDefault(default)
                } else {
                    runCatching { Instant.parse(trimmed) }.getOrDefault(default)
                }
            }
            else -> default
        }
    }
}
```

---

### 3.3 `core/data`: Domain Model Alignment & Room Isolation

#### [MODIFY] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/model/JobApplication.kt`
Convert `createdAt` and `updatedAt` to non-null `Instant`:
```kotlin
public val createdAt: Instant = Instant.DISTANT_PAST,
public val updatedAt: Instant = Instant.DISTANT_PAST,
```

#### [MODIFY] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/model/Timeline.kt`
Convert timestamp properties to non-null `Instant`:
```kotlin
public data class Timeline(
    public val firstActivityAt: Instant = Instant.DISTANT_PAST,
    public val lastActivityAt: Instant = Instant.DISTANT_PAST,
    public val daysSinceActivity: Int = 0,
    public val nextFollowUpAt: Instant = Instant.DISTANT_PAST,
)
```

#### [MODIFY] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/model/TrackerEvent.kt`
Convert `createdAt` to non-null `Instant`:
```kotlin
public val createdAt: Instant = Instant.DISTANT_PAST,
```

#### [NO CHANGE] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/entity/JobApplicationEntity.kt`
Leave `createdAt: Instant?` and `updatedAt: Instant?` nullable in Room entity to preserve SQLite table schema across Android, JVM, and iOS without migrations.

#### [MODIFY] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/JobApplicationMapper.kt`
Map between domain `Instant` and Room `Instant?`:
```kotlin
public fun toEntity(domain: JobApplication): JobApplicationEntity {
    return JobApplicationEntity(
        // ...
        createdAt = if (domain.createdAt == Instant.DISTANT_PAST) null else domain.createdAt,
        updatedAt = if (domain.updatedAt == Instant.DISTANT_PAST) null else domain.updatedAt,
        // ...
    )
}

public fun toDomain(entity: JobApplicationEntity): JobApplication {
    return Factory.createJobApplication(
        // ...
        createdAt = entity.createdAt ?: Instant.DISTANT_PAST,
        updatedAt = entity.updatedAt ?: Instant.DISTANT_PAST,
        // ...
    )
}
```

#### [MODIFY] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/factory/Factory.kt`
Update factory default values for `createdAt` and `updatedAt` to `Clock.System.now()`.

---

### 3.4 `features/job-applications-list`: Deserialization & Localized UI Model

#### [MODIFY] `RemoteJobApplicationsListDataSourceImpl.kt`
Parse `createdAt`, `updatedAt`, and `timeline` using `InstantParser.parse()`:
```kotlin
val createdAt = InstantParser.parse(record.fields["createdAt"] ?: record.fields["created_at"])
val updatedAt = InstantParser.parse(record.fields["updatedAt"] ?: record.fields["updated_at"])

val timelineMap = record.fields["timeline"] as? Map<*, *>
val timeline = timelineMap?.let { map ->
    Timeline(
        firstActivityAt = InstantParser.parse(map["firstActivityAt"] ?: map["first_activity_at"]),
        lastActivityAt = InstantParser.parse(map["lastActivityAt"] ?: map["last_activity_at"]),
        daysSinceActivity = ((map["daysSinceActivity"] ?: map["days_since_activity"]) as? Number)?.toInt() ?: 0,
        nextFollowUpAt = InstantParser.parse(map["nextFollowUpAt"] ?: map["next_follow_up_at"]),
    )
}
```

#### [MODIFY] `ui/kmp/features/job-applications-list/ui/src/commonMain/composeResources/values/strings.xml`
Add localized string resources for all displayed text, relative time units, and fallbacks:
```xml
    <string name="list_row_created">Created %s</string>
    <string name="list_row_created_applied">Created %1$s • Applied %2$s</string>
    <string name="list_row_header_fit_score">Fit Score</string>
    <string name="list_row_header_applied_on">Applied on</string>
    <string name="list_row_header_updated">Updated</string>
    <string name="list_row_action_view_details">View Details</string>
    <string name="data_missing">DATA MISSING</string>
    <string name="parsing_error">PARSING ERROR</string>

    <!-- Localized Relative Time Resources -->
    <string name="time_just_now">just now</string>
    <string name="time_minute_single">1 minute ago</string>
    <string name="time_minutes_ago">%d minutes ago</string>
    <string name="time_hour_single">1 hour ago</string>
    <string name="time_hours_ago">%d hours ago</string>
    <string name="time_day_single">1 day ago</string>
    <string name="time_days_ago">%d days ago</string>
    <string name="time_week_single">1 week ago</string>
    <string name="time_weeks_ago">%d weeks ago</string>
    <string name="time_weeks_days_ago">%1$d weeks and %2$d days ago</string>
    <string name="time_months_ago">%1$s (%2$d months ago)</string>
```

#### [NEW] `features/job-applications-list/ui/.../util/RelativeTimeFormatter.kt`
Fully localized relative time formatter resolving strictly through `Res.string`:
```kotlin
package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.util

import androidx.compose.runtime.Composable
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.time_day_single
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.time_days_ago
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.time_hour_single
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.time_hours_ago
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.time_just_now
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.time_minutes_ago
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.time_months_ago
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.time_week_single
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.time_weeks_ago
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.time_weeks_days_ago
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
public fun formatRelativeInstant(
    instant: Instant,
    now: Instant = Clock.System.now(),
): String? {
    if (instant == Instant.DISTANT_PAST) return null
    val duration = now - instant
    val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
    val totalMinutes = totalSeconds / 60
    val totalHours = totalMinutes / 60
    val totalDays = totalHours / 24

    return when {
        totalHours < 1 -> {
            if (totalMinutes <= 1) {
                stringResource(Res.string.time_just_now)
            } else {
                stringResource(Res.string.time_minutes_ago, totalMinutes)
            }
        }
        totalHours < 24 -> {
            if (totalHours == 1L) {
                stringResource(Res.string.time_hour_single)
            } else {
                stringResource(Res.string.time_hours_ago, totalHours)
            }
        }
        totalDays < 7 -> {
            if (totalDays == 1L) {
                stringResource(Res.string.time_day_single)
            } else {
                stringResource(Res.string.time_days_ago, totalDays)
            }
        }
        totalDays < 28 -> {
            val weeks = totalDays / 7
            val remainingDays = totalDays % 7
            if (remainingDays == 0L) {
                if (weeks == 1L) {
                    stringResource(Res.string.time_week_single)
                } else {
                    stringResource(Res.string.time_weeks_ago, weeks)
                }
            } else {
                stringResource(Res.string.time_weeks_days_ago, weeks, remainingDays)
            }
        }
        else -> {
            val approxMonths = totalDays / 30
            val datePart = instant.toString().substringBefore('T')
            stringResource(Res.string.time_months_ago, datePart, approxMonths)
        }
    }
}
```

#### [MODIFY] `JobApplicationItemUiModel.kt`
Encapsulate fallback formatting directly on the model:
```kotlin
package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.util.formatRelativeInstant
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.data_missing
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@Immutable
public data class JobApplicationItemUiModel(
    public val id: String,
    public val companyName: String,
    public val roleTitle: String,
    public val locationText: String?,
    public val status: TrackerStatus,
    public val statusDisplayName: String,
    public val fitScore: Int?,
    public val fitScoreText: String?,
    public val createdAt: Instant = Instant.DISTANT_PAST,
    public val updatedAt: Instant = Instant.DISTANT_PAST,
    public val firstActivityAt: Instant = Instant.DISTANT_PAST,
    public val isExpanded: Boolean = false,
    public val onClickIntent: JobApplicationsListUiIntent,
) {
    @Composable
    public fun createdDateDisplay(backup: String = stringResource(Res.string.data_missing)): String {
        return if (createdAt != Instant.DISTANT_PAST) {
            createdAt.toString().substringBefore('T')
        } else {
            backup
        }
    }

    @Composable
    public fun appliedDateDisplay(backup: String = stringResource(Res.string.data_missing)): String {
        return if (firstActivityAt != Instant.DISTANT_PAST) {
            firstActivityAt.toString().substringBefore('T')
        } else if (createdAt != Instant.DISTANT_PAST) {
            createdAt.toString().substringBefore('T')
        } else {
            backup
        }
    }

    @Composable
    public fun updatedDateDisplay(backup: String = stringResource(Res.string.data_missing)): String {
        val targetInstant = if (updatedAt != Instant.DISTANT_PAST) {
            updatedAt
        } else if (createdAt != Instant.DISTANT_PAST) {
            createdAt
        } else {
            return backup
        }
        return formatRelativeInstant(targetInstant) ?: backup
    }

    public fun fitScoreDisplay(backup: String): String =
        fitScore?.toString() ?: backup
}
```

#### [MODIFY] `JobApplicationsListViewModel.kt`
Pass domain `Instant`s directly into `JobApplicationItemUiModel`:
```kotlin
JobApplicationItemUiModel(
    id = id,
    companyName = company,
    roleTitle = role ?: "",
    locationText = location,
    status = status,
    statusDisplayName = status.displayName(),
    fitScore = fitScore,
    fitScoreText = fitScore?.let { "Fit: $it%" },
    createdAt = createdAt,
    updatedAt = updatedAt,
    firstActivityAt = timeline?.firstActivityAt ?: Instant.DISTANT_PAST,
    isExpanded = isExpanded,
    onClickIntent = JobApplicationsListUiIntent.ToggleExpand(id),
)
```

#### [MODIFY] `JobApplicationRow.kt`
Flatten the expanded hierarchy using `ConstraintLayout` and bind display methods directly:
```kotlin
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.data_missing
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_action_view_details
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_created
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_created_applied
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_header_applied_on
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_header_fit_score
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_header_updated
import org.jetbrains.compose.resources.stringResource

// In Closed Compact View:
Text(
    text = stringResource(
        Res.string.list_row_created,
        model.createdDateDisplay(backup = stringResource(Res.string.data_missing)),
    ),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

// In Expanded State: Flattened with ConstraintLayout
AnimatedVisibility(visible = isExpanded) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.medium),
    ) {
        val (
            fitHeader, fitVal,
            appliedHeader, appliedVal,
            updatedHeader, updatedVal,
            btnDetails,
        ) = createRefs()

        val guideStart = createGuidelineFromStart(0f)
        val guideFirstThird = createGuidelineFromStart(0.33f)
        val guideSecondThird = createGuidelineFromStart(0.66f)
        val guideEnd = createGuidelineFromStart(1f)

        // Column 1: Fit Score
        Text(
            text = stringResource(Res.string.list_row_header_fit_score),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.constrainAs(fitHeader) {
                start.linkTo(guideStart)
                end.linkTo(guideFirstThird)
                width = Dimension.fillToConstraints
            },
        )
        Text(
            text = model.fitScoreDisplay(backup = stringResource(Res.string.data_missing)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.constrainAs(fitVal) {
                top.linkTo(fitHeader.bottom, margin = 4.dp)
                start.linkTo(guideStart)
                end.linkTo(guideFirstThird)
                width = Dimension.fillToConstraints
            },
        )

        // Column 2: Applied on
        Text(
            text = stringResource(Res.string.list_row_header_applied_on),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.constrainAs(appliedHeader) {
                start.linkTo(guideFirstThird, margin = 8.dp)
                end.linkTo(guideSecondThird)
                width = Dimension.fillToConstraints
            },
        )
        Text(
            text = model.appliedDateDisplay(backup = stringResource(Res.string.data_missing)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.constrainAs(appliedVal) {
                top.linkTo(appliedHeader.bottom, margin = 4.dp)
                start.linkTo(guideFirstThird, margin = 8.dp)
                end.linkTo(guideSecondThird)
                width = Dimension.fillToConstraints
            },
        )

        // Column 3: Updated
        Text(
            text = stringResource(Res.string.list_row_header_updated),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.constrainAs(updatedHeader) {
                start.linkTo(guideSecondThird, margin = 8.dp)
                end.linkTo(guideEnd)
                width = Dimension.fillToConstraints
            },
        )
        Text(
            text = model.updatedDateDisplay(backup = stringResource(Res.string.data_missing)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.constrainAs(updatedVal) {
                top.linkTo(updatedHeader.bottom, margin = 4.dp)
                start.linkTo(guideSecondThird, margin = 8.dp)
                end.linkTo(guideEnd)
                width = Dimension.fillToConstraints
            },
        )

        val bottomBarrier = createBottomBarrier(fitVal, appliedVal, updatedVal)

        Button(
            onClick = { onIntent(model.onClickIntent) },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.constrainAs(btnDetails) {
                top.linkTo(bottomBarrier, margin = 12.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
        ) {
            Text(stringResource(Res.string.list_row_action_view_details))
        }
    }
}

// In Wider Screens:
Text(
    text = stringResource(
        Res.string.list_row_created_applied,
        model.createdDateDisplay(backup = stringResource(Res.string.data_missing)),
        model.appliedDateDisplay(backup = stringResource(Res.string.data_missing)),
    ),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)
Text(
    text = model.updatedDateDisplay(backup = stringResource(Res.string.data_missing)),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

---

### 3.5 `features/job-application-details`: Timeline & Header Presentation

#### [MODIFY] `ui/kmp/features/job-application-details/ui/src/commonMain/composeResources/values/strings.xml`
Add localized string resources:
```xml
    <string name="detail_last_updated">Last updated %s</string>
    <string name="data_missing">DATA MISSING</string>
    <string name="parsing_error">PARSING ERROR</string>

    <string name="time_just_now">just now</string>
    <string name="time_minute_single">1 minute ago</string>
    <string name="time_minutes_ago">%d minutes ago</string>
    <string name="time_hour_single">1 hour ago</string>
    <string name="time_hours_ago">%d hours ago</string>
    <string name="time_day_single">1 day ago</string>
    <string name="time_days_ago">%d days ago</string>
    <string name="time_week_single">1 week ago</string>
    <string name="time_weeks_ago">%d weeks ago</string>
    <string name="time_weeks_days_ago">%1$d weeks and %2$d days ago</string>
    <string name="time_months_ago">%1$s (%2$d months ago)</string>
```

#### [MODIFY] `JobApplicationDetailUiModel.kt`
Encapsulate fallback formatting on the detail UI model:
```kotlin
@Composable
public fun lastUpdatedDisplay(backup: String = stringResource(Res.string.data_missing)): String {
    val targetInstant = if (updatedAt != Instant.DISTANT_PAST) {
        updatedAt
    } else if (createdAt != Instant.DISTANT_PAST) {
        createdAt
    } else {
        return backup
    }
    return formatRelativeInstant(targetInstant) ?: backup
}
```

#### [MODIFY] `EventsTimeline.kt`
- Replace `MaterialTheme.colorScheme.outline` with `MaterialTheme.colorScheme.onSurfaceVariant`.
- Render localized date using `formatRelativeInstant` and fallback:
```kotlin
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.data_missing
import org.jetbrains.compose.resources.stringResource

// In timeline event card:
val eventDateText = if (event.createdAt != Instant.DISTANT_PAST) {
    formatRelativeInstant(event.createdAt) ?: stringResource(Res.string.data_missing)
} else {
    stringResource(Res.string.data_missing)
}
Text(
    text = eventDateText,
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

#### [MODIFY] `JobApplicationDetailsScreen.kt`
- Display localized `detail_last_updated`:
```kotlin
Text(
    text = stringResource(
        Res.string.detail_last_updated,
        model.lastUpdatedDisplay(backup = stringResource(Res.string.data_missing)),
    ),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

---

## 4. Verification Plan & Testing Checklist

### Automated Unit Tests
1. **`InstantParserTest`**:
   - Verify `InstantParser.parse(instant)` passthrough.
   - Verify epoch milliseconds and epoch seconds conversion.
   - Verify ISO-8601 strings and Date-only (`YYYY-MM-DD`) strings.
   - Verify that invalid strings or nulls resolve to `Instant.DISTANT_PAST`.
2. **`RelativeTimeFormatterTest`**:
   - Verify minute formatting (< 1 hr) maps to `time_minutes_ago`.
   - Verify that `Instant.DISTANT_PAST` returns `null` (allowing Composable to render `Res.string.data_missing`).
3. **`JobApplicationsListViewModelTest`**:
   - Verify that when `createdAt` or `timeline` are missing, domain models carry `Instant.DISTANT_PAST`.
   - Verify that `JobApplicationItemUiModel` outputs clean strings that allow Composables to render `Res.string.data_missing`.
4. **Room Database Mapping Test (`RoomDatabaseMappingTest`)**:
   - Verify that `JobApplicationMapper` cleanly maps between SQLite nullable `Instant?` and domain non-null `Instant` (`Instant.DISTANT_PAST`).
   - Verify that Room database compilation succeeds with zero schema version conflicts or missing migration errors.
5. **Execute Multiplatform Tests**:
   ```bash
   mise run ui:kmp:test
   ```

### Manual & Build Verification
1. Assemble Android application:
   ```bash
   mise run ui:kmp:android:assemble
   ```
2. Verify visual rendering:
   - On compact Android devices, "Created YYYY-MM-DD" is clearly legible in `onSurfaceVariant` without string concatenation bugs.
   - Expanded tile rendered via `ConstraintLayout` shows three aligned columns with headers and values loaded via `Res.string`.
   - Timeline nodes display relative timestamps in high contrast `onSurfaceVariant`.

---

## 5. Bookkeeping & Execution Commitments

- [ ] Plan persisted at `ui/kmp/.agents/plans/2026-11-08/191000_android-date-display-and-style-fix-plan.md`
- [ ] Registry `ui/kmp/.agents/plans/README.md` row verified and synchronized
- [ ] Receive user review & approval on revised plan
- [ ] Execute file changes across `core/data`, `features/job-applications-list`, and `features/job-application-details`
- [ ] Run test suite (`mise run ui:kmp:test`) and build Android APK (`mise run ui:kmp:android:assemble`)
- [ ] Commit all changes under message: `191000_android-date-display-and-style-fix-plan.md`
- [ ] Push to GitHub: `git push origin main`
