package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.data.model.*
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.time.Clock
import kotlin.time.Instant
import android.util.Log

private const val COLLECTION_APPLICATIONS = "applications"
private const val COLLECTION_COMPANIES = "companies"
private const val SUBCOLLECTION_EVENTS = "events"
private const val TAG = "FirestoreRepo"

class AndroidFirestoreJobApplicationRepository : JobApplicationRepository {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun getAllJobApplications(): List<JobApplication> {
        return try {
            Log.d(TAG, "getAllJobApplications: Querying Firestore applications collection")
            val snapshot = db.collection(COLLECTION_APPLICATIONS).get().await()
            Log.d(TAG, "getAllJobApplications: Received ${snapshot.size()} documents")
            val apps = snapshot.documents.mapNotNull { document ->
                try {
                    document.toJobApplication()
                } catch (e: Exception) {
                    Log.e(TAG, "getAllJobApplications: Failed to parse document ${document.id}", e)
                    null
                }
            }
            Log.i(TAG, "getAllJobApplications: Successfully parsed ${apps.size} job applications")
            apps
        } catch (e: Exception) {
            Log.e(TAG, "getAllJobApplications: Query failed", e)
            emptyList()
        }
    }

    override suspend fun getJobApplication(id: String): JobApplication? {
        return try {
            val document = db.collection(COLLECTION_APPLICATIONS).document(id).get().await()
            try {
                document.toJobApplication()
            } catch (e: Exception) {
                Log.e(TAG, "getJobApplication: Failed to parse document $id", e)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getJobApplication: Query failed for $id", e)
            null
        }
    }

    override suspend fun getCompanies(): List<Company> {
        return try {
            val snapshot = db.collection(COLLECTION_COMPANIES).get().await()
            snapshot.documents.mapNotNull { document ->
                document.toCompany()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun saveJobApplication(app: JobApplication): String {
        return try {
            val docRef = db.collection(COLLECTION_APPLICATIONS).document(app.id)
            docRef.set(app.toMap()).await()
            app.id
        } catch (e: Exception) {
            e.printStackTrace()
            app.id
        }
    }

    override suspend fun updateStatus(
        id: String,
        status: TrackerStatus,
        date: String?,
        note: String?
    ): JobApplication {
        return try {
            val docRef = db.collection(COLLECTION_APPLICATIONS).document(id)
            val updates = mutableMapOf<String, Any>(
                "status" to statusToFirestore(status),
                "updatedAt" to com.google.firebase.Timestamp(Clock.System.now().toJavaInstant())
            )
            if (date != null) updates["statusDate"] = date
            if (note != null) updates["statusNote"] = note
            docRef.update(updates).await()
            getJobApplication(id) ?: Factory.createJobApplication(
                id = id,
                company = "",
                status = status,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Factory.createJobApplication(
                id = id,
                company = "",
                status = status,
            )
        }
    }

    override suspend fun recordEvent(event: TrackerEvent): String {
        return try {
            val eventsRef = db.collection(COLLECTION_APPLICATIONS)
                .document(event.applicationId)
                .collection(SUBCOLLECTION_EVENTS)
                .document(event.id)
            eventsRef.set(event.toMap()).await()
            event.id
        } catch (e: Exception) {
            e.printStackTrace()
            event.id
        }
    }

    override suspend fun updateFitScore(id: String, score: Int): JobApplication {
        return try {
            val docRef = db.collection(COLLECTION_APPLICATIONS).document(id)
            docRef.update("fitScore", score).await()
            getJobApplication(id) ?: Factory.createJobApplication(
                id = id,
                company = "",
                fitEvaluation = Factory.createTrackerFit(rubricVersion = score),
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Factory.createJobApplication(
                id = id,
                company = "",
                fitEvaluation = Factory.createTrackerFit(rubricVersion = score),
            )
        }
    }
}

// ── Firestore ↔ Model conversion helpers ──

private fun java.time.Instant.toKotlinInstant(): Instant =
    Instant.fromEpochSeconds(epochSecond, nano.toLong())

// Safe timestamp extraction: handles both Firestore Timestamp objects and ISO-8601 strings.
// getTimestamp() throws RuntimeException if the field exists but is not a Timestamp type,
// so we check the raw value first.
private fun com.google.firebase.firestore.DocumentSnapshot.safeGetTimestamp(field: String): java.time.Instant? {
    val raw = get(field)
    return when (raw) {
        is com.google.firebase.Timestamp -> raw.toInstant()
        is String -> {
            try {
                java.time.Instant.parse(raw)
            } catch (e: Exception) {
                Log.w(TAG, "safeGetTimestamp: Failed to parse string timestamp for '$field': $raw")
                null
            }
        }
        is Number -> {
            // Epoch seconds (Unix timestamp)
            java.time.Instant.ofEpochSecond(raw.toLong())
        }
        null -> null
        else -> {
            Log.w(TAG, "safeGetTimestamp: Unknown type for '$field': ${raw.javaClass.simpleName}")
            null
        }
    }
}

private fun Instant.toJavaInstant(): java.time.Instant =
    java.time.Instant.ofEpochSecond(epochSeconds, (nanosecondsOfSecond / 1_000_000).toLong())

// Safe string extraction: getString() throws RuntimeException if the field exists but is
// not a String type (e.g., some documents store "location" as a HashMap/object).
private fun com.google.firebase.firestore.DocumentSnapshot.safeGetString(field: String): String? {
    val raw = get(field)
    return when (raw) {
        is String -> raw
        null -> null
        else -> {
            Log.w(TAG, "safeGetString: Field '$field' is not a String, got ${raw.javaClass.simpleName}")
            null
        }
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toJobApplication(): JobApplication? {
    val docId = id ?: return null
    // Firestore documents from the Python sync script have a different schema than the
    // Kotlin model expects. Many fields are missing or have different types.
    // This mapping handles the actual Firestore document structure gracefully.
    
    // Company is required; skip documents without it
    val companyVal = get("company")
    if (companyVal == null || companyVal is String && companyVal.isEmpty()) return null
    val company = companyVal as? String ?: ""
    
    // String fields that may be null or have unexpected types in Firestore
    val role = safeGetString("role")
    val location = safeGetString("location")
    val notesRendered = safeGetString("notesRendered")
    val jobId = safeGetString("jobId")
    val jobUrl = safeGetString("jobUrl")
    val description = safeGetString("description")
    
    // Status: stored as "status:applied", "status:interview", etc. May be null.
    val statusStr = safeGetString("status")
    val status = EnumParsing.parseTrackerStatus(statusStr)
    
    // Channel: stored as "channel:greenhouse", etc., or null
    val channelStr = safeGetString("channel")
    val channel = EnumParsing.parseApplicationChannel(channelStr)
    
    // Tags: array of strings, may be empty array
    val tags = (get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    
    // ContactIds: array of strings, may be empty array
    val contactIds = (get("contactIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    
    // Compensation: map or null
    val compensationMap = get("compensation") as? Map<String, Any>
    val compensation = compensationMap?.toCompensation()
    
    // LocationDetail: map or null (separate from the plain "location" string)
    val locationDetailMap = get("locationDetail") as? Map<String, Any>
    val locationDetail = locationDetailMap?.toLocation()
    
    // Fit evaluation: may be stored as "fitEvaluation" map or as "fitRating" integer
    val fitEvaluationMap = get("fitEvaluation") as? Map<String, Any>
    val fitRating = (get("fitRating") as? Number)?.toInt()
    val fitEvaluation = fitEvaluationMap?.toTrackerFit() ?: run {
        if (fitRating != null) {
            TrackerFit(rubricVersion = fitRating, evaluatedAt = null, gaps = emptyList())
        } else {
            null
        }
    }
    
    // Counters: map or null
    val countersMap = get("counters") as? Map<String, Any>
    val counters = countersMap?.toTrackerCounts()
    
    // Timeline: map or null
    val timelineMap = get("timeline") as? Map<String, Any>
    val timeline = timelineMap?.toTimeline()
    
    // Timestamps: Firestore documents may store timestamps as com.google.firebase.Timestamp
    // objects (written by the Android app) or as ISO-8601 strings (written by the Python
    // sync script). getTimestamp() throws RuntimeException if the field exists but is not
    // a Timestamp, so we use safeGetTimestamp() to handle both cases.
    val createdAt = safeGetTimestamp("createdAt")?.toKotlinInstant()
    val updatedAt = safeGetTimestamp("updatedAt")?.toKotlinInstant()
    
    return JobApplication(
        id = docId,
        company = company,
        role = role,
        location = location,
        status = status,
        channel = channel,
        jobId = jobId,
        jobUrl = jobUrl,
        description = description,
        tags = tags,
        contactIds = contactIds,
        compensation = compensation,
        locationDetail = locationDetail,
        fitEvaluation = fitEvaluation,
        counters = counters,
        timeline = timeline,
        notesRendered = notesRendered,
        createdAt = createdAt,
        updatedAt = updatedAt,
        jobApplicationUrl = jobUrl,
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toCompany(): Company? {
    val id = id ?: return null
    return Company(
        id = id,
        name = safeGetString("name") ?: "",
        aliases = (get("aliases") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        website = safeGetString("website"),
        industry = safeGetString("industry"),
        createdAt = safeGetTimestamp("createdAt")?.toKotlinInstant(),
        updatedAt = safeGetTimestamp("updatedAt")?.toKotlinInstant(),
    )
}

private fun Map<String, Any>.toCompensation(): Compensation? {
    return Compensation(
        min = (get("min") as? Number)?.toDouble(),
        max = (get("max") as? Number)?.toDouble(),
        currency = get("currency") as? String,
        period = get("period") as? String,
        bandSource = get("bandSource") as? String,
        vsTarget = get("vsTarget") as? String,
    )
}

private fun Map<String, Any>.toLocation(): Location? {
    return Location(
        city = get("city") as? String,
        region = get("region") as? String,
        country = get("country") as? String,
        workMode = EnumParsing.parseWorkMode(get("workMode") as? String),
        relocation = (get("relocation") as? Boolean) ?: false,
        superCommuter = (get("superCommuter") as? Boolean) ?: false,
    )
}

private fun Map<String, Any>.toTrackerFit(): TrackerFit? {
    return TrackerFit(
        rubricVersion = (get("rubricVersion") as? Number)?.toInt() ?: 1,
        evaluatedAt = (get("evaluatedAt") as? com.google.firebase.Timestamp)?.toInstant()?.toKotlinInstant(),
        gaps = (get("gaps") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
    )
}

private fun Map<String, Any>.toTrackerCounts(): TrackerCounts? {
    return TrackerCounts(
        events = (get("events") as? Number)?.toInt() ?: 0,
        followUpsSent = (get("followUpsSent") as? Number)?.toInt() ?: 0,
        interviews = (get("interviews") as? Number)?.toInt() ?: 0,
        rejections = (get("rejections") as? Number)?.toInt() ?: 0,
    )
}

private fun Map<String, Any>.toTimeline(): Timeline? {
    return Timeline(
        firstActivityAt = (get("firstActivityAt") as? com.google.firebase.Timestamp)?.toInstant()?.toKotlinInstant(),
        lastActivityAt = (get("lastActivityAt") as? com.google.firebase.Timestamp)?.toInstant()?.toKotlinInstant(),
        daysSinceActivity = (get("daysSinceActivity") as? Number)?.toInt() ?: 0,
        nextFollowUpAt = (get("nextFollowUpAt") as? com.google.firebase.Timestamp)?.toInstant()?.toKotlinInstant(),
    )
}

private fun JobApplication.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["company"] = company
    role?.let { map["role"] = it }
    location?.let { map["location"] = it }
    map["status"] = statusToFirestore(status)
    map["channel"] = channelToFirestore(channel)
    jobId?.let { map["jobId"] = it }
    jobUrl?.let { map["jobUrl"] = it }
    description?.let { map["description"] = it }
    map["tags"] = tags
    map["contactIds"] = contactIds
    compensation?.let { map["compensation"] = it.toMap() }
    locationDetail?.let { map["locationDetail"] = it.toMap() }
    fitEvaluation?.let { map["fitEvaluation"] = it.toMap() }
    counters?.let { map["counters"] = it.toMap() }
    timeline?.let { map["timeline"] = it.toMap() }
    notesRendered?.let { map["notesRendered"] = it }
    createdAt?.let { map["createdAt"] = com.google.firebase.Timestamp(it.toJavaInstant()) }
    updatedAt?.let { map["updatedAt"] = com.google.firebase.Timestamp(it.toJavaInstant()) }
    return map
}

private fun TrackerEvent.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["applicationId"] = applicationId
    map["type"] = eventTypeToFirestore(type)
    statusFrom?.let { map["statusFrom"] = statusToFirestore(it) }
    statusTo?.let { map["statusTo"] = statusToFirestore(it) }
    note?.let { map["note"] = it }
    emailMessageId?.let { map["emailMessageId"] = it }
    map["actor"] = actorToFirestore(actor)
    map["derivedFrom"] = derivedFromToFirestore(derivedFrom)
    map["confidence"] = confidenceToFirestore(confidence)
    createdAt?.let { map["createdAt"] = com.google.firebase.Timestamp(it.toJavaInstant()) }
    return map
}

private fun Compensation.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    min?.let { map["min"] = it }
    max?.let { map["max"] = it }
    currency?.let { map["currency"] = it }
    period?.let { map["period"] = it }
    bandSource?.let { map["bandSource"] = it }
    vsTarget?.let { map["vsTarget"] = it }
    return map
}

private fun Location.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    city?.let { map["city"] = it }
    region?.let { map["region"] = it }
    country?.let { map["country"] = it }
    map["workMode"] = workModeToFirestore(workMode)
    map["relocation"] = relocation
    map["superCommuter"] = superCommuter
    return map
}

private fun TrackerFit.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["rubricVersion"] = rubricVersion
    evaluatedAt?.let { map["evaluatedAt"] = com.google.firebase.Timestamp(it.toJavaInstant()) }
    map["gaps"] = gaps
    return map
}

private fun TrackerCounts.toMap(): Map<String, Any> {
    return mapOf(
        "events" to events,
        "followUpsSent" to followUpsSent,
        "interviews" to interviews,
        "rejections" to rejections,
    )
}

private fun Timeline.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    firstActivityAt?.let { map["firstActivityAt"] = com.google.firebase.Timestamp(it.toJavaInstant()) }
    lastActivityAt?.let { map["lastActivityAt"] = com.google.firebase.Timestamp(it.toJavaInstant()) }
    map["daysSinceActivity"] = daysSinceActivity
    nextFollowUpAt?.let { map["nextFollowUpAt"] = com.google.firebase.Timestamp(it.toJavaInstant()) }
    return map
}

// ── Enum ↔ Firestore string conversion ──

private fun statusToFirestore(status: TrackerStatus): String {
    return when (status) {
        TrackerStatus.DRAFTING -> "status:drafting"
        TrackerStatus.APPLIED -> "status:applied"
        TrackerStatus.INTERVIEW -> "status:interview"
        TrackerStatus.OFFER_EXPECTED -> "status:offer"
        TrackerStatus.HIRED -> "status:hired"
        TrackerStatus.REJECTED -> "status:rejected"
        TrackerStatus.NO_RESPONSE -> "status:no-response"
        TrackerStatus.OFFER_DECLINED -> "status:offer-declined"
        TrackerStatus.APPLICATION_WITHDRAWN -> "status:withdrawn"
        else -> "status:unspecified"
    }
}

private fun channelToFirestore(channel: ApplicationChannel): String {
    return when (channel) {
        ApplicationChannel.GREENHOUSE -> "channel:greenhouse"
        ApplicationChannel.WORKDAY -> "channel:workday"
        ApplicationChannel.LEVER -> "channel:lever"
        ApplicationChannel.ASHBY -> "channel:ashby"
        ApplicationChannel.SMARTRECRUITERS -> "channel:smartrecruiters"
        ApplicationChannel.LINKEDIN -> "channel:linkedin"
        ApplicationChannel.REFERRAL -> "channel:referral"
        ApplicationChannel.DIRECT -> "channel:direct"
        ApplicationChannel.OTHER -> "channel:other"
        else -> "channel:unspecified"
    }
}

private fun eventTypeToFirestore(type: TrackerEventType): String {
    return when (type) {
        TrackerEventType.APPLY -> "event:applied"
        TrackerEventType.ACKNOWLEDGE -> "event:acknowledged"
        TrackerEventType.RECRUITER_SCREEN -> "event:recruiter-screen"
        TrackerEventType.SCHEDULE_INTERVIEW -> "event:interview-scheduled"
        TrackerEventType.COMPLETE_INTERVIEW -> "event:interview-completed"
        TrackerEventType.ASSESS -> "event:assessment"
        TrackerEventType.VISIT_ONSITE -> "event:onsite"
        TrackerEventType.OFFER -> "event:offer"
        TrackerEventType.ACCEPT_OFFER -> "event:offer-accepted"
        TrackerEventType.DECLINE_OFFER -> "event:offer-declined"
        TrackerEventType.REJECT -> "event:rejection"
        TrackerEventType.GHOST -> "event:no-response"
        TrackerEventType.WITHDRAW -> "event:withdrawn"
        TrackerEventType.SEND_FOLLOW_UP -> "event:follow-up-sent"
        TrackerEventType.TAILOR_DOCUMENT -> "event:document-tailored"
        TrackerEventType.RECALCULATE_FIT -> "event:fit-recalculated"
        TrackerEventType.ADD_ARTIFACT -> "event:artifact-added"
        TrackerEventType.ADD_NOTE -> "event:note-added"
        TrackerEventType.IMPORT -> "event:imported"
        TrackerEventType.CORRECT_STATUS -> "event:status-corrected"
        else -> "event:unspecified"
    }
}

private fun actorToFirestore(actor: EventActor): String {
    return when (actor) {
        EventActor.USER -> "actor:user"
        EventActor.AGENT -> "actor:agent"
        EventActor.EMAIL_SYNC -> "actor:email-sync"
        EventActor.IMPORT -> "actor:import"
        EventActor.SCHEDULER -> "actor:scheduler"
        else -> "actor:unspecified"
    }
}

private fun derivedFromToFirestore(derived: DerivedFrom): String {
    return when (derived) {
        DerivedFrom.MANUAL -> "derived:manual"
        DerivedFrom.NOTES_REGEX -> "derived:notes-regex"
        DerivedFrom.EMAIL -> "derived:email"
        DerivedFrom.CSV_IMPORT -> "derived:csv-import"
        else -> "derived:unspecified"
    }
}

private fun confidenceToFirestore(confidence: Confidence): String {
    return when (confidence) {
        Confidence.HIGH -> "confidence:high"
        Confidence.MEDIUM -> "confidence:medium"
        Confidence.LOW -> "confidence:low"
        else -> "confidence:unspecified"
    }
}

private fun workModeToFirestore(mode: WorkMode): String {
    return when (mode) {
        WorkMode.ONSITE -> "work-mode:onsite"
        WorkMode.HYBRID -> "work-mode:hybrid"
        WorkMode.REMOTE -> "work-mode:remote"
        else -> "work-mode:unspecified"
    }
}
