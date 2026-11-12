package dev.fnc314.tools.jobtracker.backend

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import dev.fnc314.tools.jobsearch.parser.ApplicationTimelineProjector
import dev.fnc314.tools.jobsearch.parser.CompanyMetricsParser
import dev.fnc314.tools.jobsearch.parser.CompanySectorNormalizer
import dev.fnc314.tools.jobsearch.parser.DocumentIdGenerator
import dev.fnc314.tools.jobsearch.parser.EventChainBuilder
import dev.fnc314.tools.jobsearch.parser.EventChainVerifier
import dev.fnc314.tools.jobsearch.tracker.v1.ApplicationChannel
import dev.fnc314.tools.jobsearch.tracker.v1.CompanyMetrics
import dev.fnc314.tools.jobsearch.tracker.v1.CompanySector
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.SubmissionConfirmation
import dev.fnc314.tools.jobsearch.tracker.v1.Timeline
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import java.io.File
import java.io.FileInputStream
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

public object BackendService {
    private var initialized = false

    public val firestore: Firestore by lazy {
        initFirebase()
        FirestoreClient.getFirestore(FirebaseApp.getInstance())
    }

    private fun initFirebase() {
        if (initialized) return
        try {
            val saKeyEnv = System.getenv("TRACKER_SA_KEY")
            val canonicalSaFile = File(System.getProperty("user.home"), ".config/job-tracker/sa.json")
            val gAppCredsEnv = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")

            val credentials = when {
                !saKeyEnv.isNullOrBlank() && File(saKeyEnv).exists() -> {
                    FileInputStream(File(saKeyEnv)).use { GoogleCredentials.fromStream(it) }
                }
                canonicalSaFile.exists() -> {
                    FileInputStream(canonicalSaFile).use { GoogleCredentials.fromStream(it) }
                }
                !gAppCredsEnv.isNullOrBlank() && File(gAppCredsEnv).exists() -> {
                    FileInputStream(File(gAppCredsEnv)).use { GoogleCredentials.fromStream(it) }
                }
                else -> {
                    GoogleCredentials.getApplicationDefault()
                }
            }.createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

            val projectId = System.getenv("TRACKER_PROJECT")
                ?: System.getenv("REMOTE_GCP_PROJECT")
                ?: "job-search-fnc314-dev"

            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build()

            if (FirebaseApp.getApps().none { it.name == FirebaseApp.DEFAULT_APP_NAME }) {
                FirebaseApp.initializeApp(options)
            }
            initialized = true
        } catch (e: Exception) {
            println("Warning: Firebase initialization in BackendService: ${e.message}")
        }
    }

    public fun appendEventTransactional(
        applicationId: String,
        eventPayload: Map<String, Any?>,
        idempotencyKey: String? = null,
    ): Map<String, Any?> {
        val db = firestore
        val appRef = db.collection("applications").document(applicationId)
        val eventsColl = appRef.collection("events")

        val targetEventId = (idempotencyKey?.takeIf { it.isNotBlank() }
            ?: eventPayload["eventId"] as? String
            ?: java.util.UUID.randomUUID().toString())

        val incomingMessageId = eventPayload["emailMessageId"] as? String

        return db.runTransaction<Map<String, Any?>> { transaction ->
            val appSnap = transaction.get(appRef).get()
            if (!appSnap.exists()) {
                throw IllegalArgumentException("Application not found: $applicationId")
            }

            // Check if this event already exists by eventId
            val existingEventSnap = transaction.get(eventsColl.document(targetEventId)).get()
            if (existingEventSnap.exists()) {
                val data = existingEventSnap.data ?: emptyMap<String, Any?>()
                return@runTransaction mapOf(
                    "status" to "DUPLICATE",
                    "event" to data,
                    "applicationId" to applicationId,
                    "eventId" to targetEventId
                )
            }

            // Read all existing events for hash chaining and projection
            val allEventsQuery = eventsColl.get().get()
            val existingEvents = mutableListOf<TrackerEvent>()

            for (doc in allEventsQuery.documents) {
                val data = doc.data
                // Check idempotency by emailMessageId
                if (!incomingMessageId.isNullOrBlank() && data["emailMessageId"] == incomingMessageId) {
                    return@runTransaction mapOf(
                        "status" to "DUPLICATE",
                        "event" to data,
                        "applicationId" to applicationId,
                        "eventId" to doc.id
                    )
                }

                val protoEv = mapToTrackerEvent(doc.id, data)
                existingEvents.add(protoEv)
            }

            // Build new Proto Event from eventPayload
            val newProtoBuilder = TrackerEvent.newBuilder()
                .setId(targetEventId)
                .setApplicationId(applicationId)

            val eventTypeStr = (eventPayload["type"] as? String)?.uppercase() ?: "ADD_NOTE"
            try {
                newProtoBuilder.setType(TrackerEventType.valueOf(eventTypeStr))
            } catch (e: Exception) {
                newProtoBuilder.setType(TrackerEventType.ADD_NOTE)
            }

            val actorStr = (eventPayload["actor"] as? String)?.uppercase() ?: "ACTOR_USER"
            try {
                newProtoBuilder.setActor(EventActor.valueOf(actorStr))
            } catch (e: Exception) {
                newProtoBuilder.setActor(EventActor.ACTOR_USER)
            }

            (eventPayload["statusTo"] as? String)?.let { sTo ->
                try {
                    newProtoBuilder.setStatusTo(TrackerStatus.valueOf(sTo.uppercase()))
                } catch (ignored: Exception) {}
            }

            (eventPayload["statusFrom"] as? String)?.let { sFrom ->
                try {
                    newProtoBuilder.setStatusFrom(TrackerStatus.valueOf(sFrom.uppercase()))
                } catch (ignored: Exception) {}
            }

            (eventPayload["note"] as? String)?.let { newProtoBuilder.setNote(it) }
            incomingMessageId?.let { newProtoBuilder.setEmailMessageId(it) }

            val tsIso = (eventPayload["ts"] as? String)
                ?: (eventPayload["date"] as? String)
                ?: Instant.now().toString()

            val instant = try {
                Instant.parse(tsIso)
            } catch (e: Exception) {
                Instant.now()
            }

            newProtoBuilder.setCreatedAt(
                com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(instant.epochSecond)
                    .setNanos(instant.nano)
                    .build()
            )

            // Compute next cryptographic link
            val chained = EventChainBuilder.computeNextLink(
                existingEvents = existingEvents,
                newEvent = newProtoBuilder.build(),
                applicationId = applicationId
            )

            // Prepare event doc map
            val eventDoc = mutableMapOf<String, Any?>(
                "eventId" to chained.id,
                "type" to chained.type.name,
                "actor" to chained.actor.name,
                "ts" to tsIso,
                "date" to tsIso.substringBefore("T"),
                "sequenceNumber" to chained.sequenceNumber,
                "prevEventHash" to chained.prevEventHash,
                "eventHash" to chained.eventHash
            )
            if (chained.statusTo != TrackerStatus.TRACKER_STATUS_UNSPECIFIED) {
                eventDoc["statusTo"] = chained.statusTo.name
            }
            if (chained.statusFrom != TrackerStatus.TRACKER_STATUS_UNSPECIFIED) {
                eventDoc["statusFrom"] = chained.statusFrom.name
            }
            if (chained.note.isNotBlank()) {
                eventDoc["note"] = chained.note
            }
            if (chained.emailMessageId.isNotBlank()) {
                eventDoc["emailMessageId"] = chained.emailMessageId
            }

            transaction.set(eventsColl.document(chained.id), eventDoc)

            // Re-project timeline, counters, notes, and submission confirmation
            val updatedEvents = existingEvents + chained
            val timeline = ApplicationTimelineProjector.projectTimeline(updatedEvents)
            val submissionConfirmation = ApplicationTimelineProjector.projectSubmissionConfirmation(
                null,
                updatedEvents
            )
            val counters = ApplicationTimelineProjector.projectCounters(updatedEvents)
            val renderedNotes = ApplicationTimelineProjector.projectNotes(updatedEvents)

            // Update parent application
            val parentUpdate = mutableMapOf<String, Any?>(
                "updatedAt" to Timestamp.now(),
                "notesRendered" to renderedNotes,
                "timeline" to mapOf(
                    "submittedAt" to if (timeline.hasSubmittedAt() && timeline.submittedAt.seconds > 0)
                        Instant.ofEpochSecond(timeline.submittedAt.seconds).toString() else null,
                    "submissionConfirmedAt" to if (timeline.hasSubmissionConfirmedAt() && timeline.submissionConfirmedAt.seconds > 0)
                        Instant.ofEpochSecond(timeline.submissionConfirmedAt.seconds).toString() else null,
                    "firstActivityAt" to if (timeline.hasFirstActivityAt() && timeline.firstActivityAt.seconds > 0)
                        Instant.ofEpochSecond(timeline.firstActivityAt.seconds).toString() else null,
                    "lastActivityAt" to if (timeline.hasLastActivityAt() && timeline.lastActivityAt.seconds > 0)
                        Instant.ofEpochSecond(timeline.lastActivityAt.seconds).toString() else null,
                    "daysSinceActivity" to timeline.daysSinceActivity
                ),
                "submissionConfirmation" to mapOf(
                    "isConfirmed" to submissionConfirmation.isConfirmed,
                    "confidence" to submissionConfirmation.confidence.name,
                    "submittedAt" to if (submissionConfirmation.hasSubmittedAt() && submissionConfirmation.submittedAt.seconds > 0)
                        Instant.ofEpochSecond(submissionConfirmation.submittedAt.seconds).toString() else null,
                    "confirmedAt" to if (submissionConfirmation.hasConfirmedAt() && submissionConfirmation.confirmedAt.seconds > 0)
                        Instant.ofEpochSecond(submissionConfirmation.confirmedAt.seconds).toString() else null,
                    "confirmationEmailMessageId" to submissionConfirmation.confirmationEmailMessageId.takeIf { it.isNotBlank() },
                    "sourceSubject" to submissionConfirmation.sourceSubject.takeIf { it.isNotBlank() },
                    "notes" to submissionConfirmation.notes.takeIf { it.isNotBlank() }
                ),
                "counters" to counters
            )

            if (chained.statusTo != TrackerStatus.TRACKER_STATUS_UNSPECIFIED) {
                parentUpdate["status"] = chained.statusTo.name
            }

            transaction.set(appRef, parentUpdate, SetOptions.merge())

            mapOf(
                "status" to "SUCCESS",
                "applicationId" to applicationId,
                "eventId" to chained.id,
                "sequenceNumber" to chained.sequenceNumber,
                "eventHash" to chained.eventHash,
                "prevEventHash" to chained.prevEventHash,
                "event" to eventDoc
            )
        }.get()
    }

    private fun mapToTrackerEvent(id: String, data: Map<String, Any?>): TrackerEvent {
        val builder = TrackerEvent.newBuilder().setId(id)
        val typeStr = (data["type"] as? String)?.uppercase() ?: "ADD_NOTE"
        try {
            builder.setType(TrackerEventType.valueOf(typeStr))
        } catch (e: Exception) {
            builder.setType(TrackerEventType.ADD_NOTE)
        }

        val actorStr = (data["actor"] as? String)?.uppercase() ?: "ACTOR_USER"
        try {
            builder.setActor(EventActor.valueOf(actorStr))
        } catch (e: Exception) {
            builder.setActor(EventActor.ACTOR_USER)
        }

        (data["statusTo"] as? String)?.let { sTo ->
            try {
                builder.setStatusTo(TrackerStatus.valueOf(sTo.uppercase()))
            } catch (ignored: Exception) {}
        }
        (data["statusFrom"] as? String)?.let { sFrom ->
            try {
                builder.setStatusFrom(TrackerStatus.valueOf(sFrom.uppercase()))
            } catch (ignored: Exception) {}
        }
        (data["note"] as? String)?.let { builder.setNote(it) }
        (data["emailMessageId"] as? String)?.let { builder.setEmailMessageId(it) }
        (data["sequenceNumber"] as? Number)?.let { builder.setSequenceNumber(it.toLong()) }
        (data["prevEventHash"] as? String)?.let { builder.setPrevEventHash(it) }
        (data["eventHash"] as? String)?.let { builder.setEventHash(it) }

        val ts = (data["ts"] as? String) ?: (data["date"] as? String)
        if (!ts.isNullOrBlank()) {
            try {
                val inst = Instant.parse(ts)
                builder.setCreatedAt(
                    com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(inst.epochSecond)
                        .setNanos(inst.nano)
                        .build()
                )
            } catch (e: Exception) {}
        }
        return builder.build()
    }

    public fun expireStaleProposals(expirationDays: Long = 14): Int {
        val db = firestore
        val cutoff = Timestamp.ofTimeSecondsAndNanos(
            Instant.now().minus(expirationDays, ChronoUnit.DAYS).epochSecond,
            0
        )
        val proposals = db.collection("pending-proposals")
            .whereEqualTo("state", "PENDING")
            .whereLessThan("createdAt", cutoff)
            .get()
            .get()

        var count = 0
        val now = Timestamp.now()
        for (doc in proposals.documents) {
            doc.reference.update(
                mapOf(
                    "state" to "EXPIRED",
                    "updatedAt" to now
                )
            ).get()
            count++
        }
        return count
    }

    public fun processDueSyncs(): Int {
        val db = firestore
        val now = Timestamp.now()
        val syncs = db.collection("sync-requests")
            .whereEqualTo("state", "PENDING")
            .whereLessThanOrEqualTo("dueAt", now)
            .get()
            .get()

        var count = 0
        for (doc in syncs.documents) {
            doc.reference.update(
                mapOf(
                    "state" to "DONE",
                    "processedAt" to now
                )
            ).get()
            count++
        }
        return count
    }

    /**
     * Pure function to calculate [CompanyMetrics] from an array of application document maps.
     */
    @Suppress("UNCHECKED_CAST", "MagicNumber")
    public fun calculateMetricsForApplications(applications: List<Map<String, Any?>>): Map<String, Any> {
        var draft = 0
        var submitted = 0
        var interviewing = 0
        var offer = 0
        var rejected = 0

        val fitScores = mutableListOf<Int>()
        var minSalary: Double? = null
        var maxSalary: Double? = null
        var salaryCurrency = "USD"
        val channelCounts = mutableMapOf<String, Int>()
        val locations = linkedSetOf<String>()

        for (app in applications) {
            val status = (app["status"] as? String)?.uppercase() ?: ""
            when {
                status in listOf("DRAFTING", "LEAD") -> draft++
                status in listOf("APPLIED", "NO_RESPONSE") -> submitted++
                status in listOf("INTERVIEW") -> interviewing++
                status in listOf("OFFER_EXPECTED", "HIRED", "OFFER_DECLINED") -> offer++
                status in listOf("REJECTED", "APPLICATION_WITHDRAWN") -> rejected++
                else -> draft++ // Default unmapped to draft category
            }

            // Fit Rating
            val fitRating = (app["fitRating"] as? Number)?.toInt()
                ?: (app["fitScore"] as? Number)?.toInt()
            if (fitRating != null && fitRating >= 0) {
                fitScores.add(fitRating)
            }

            // Compensation
            val compMap = app["compensation"] as? Map<String, Any?>
            val appMin = (compMap?.get("min") as? Number)?.toDouble()
                ?: (app["compensationMin"] as? Number)?.toDouble()
            val appMax = (compMap?.get("max") as? Number)?.toDouble()
                ?: (app["compensationMax"] as? Number)?.toDouble()
            val appCurrency = (compMap?.get("currency") as? String)
                ?: (app["compensationCurrency"] as? String)
            if (!appCurrency.isNullOrBlank()) {
                salaryCurrency = appCurrency
            }

            if (appMin != null && appMin > 0) {
                minSalary = if (minSalary == null) appMin else minOf(minSalary, appMin)
            }
            if (appMax != null && appMax > 0) {
                maxSalary = if (maxSalary == null) appMax else maxOf(maxSalary, appMax)
            }

            // Channel
            val channelStr = (app["channel"] as? String)?.uppercase()
            if (!channelStr.isNullOrBlank()) {
                val cleanChannel = channelStr.removePrefix("CHANNEL:")
                channelCounts[cleanChannel] = (channelCounts[cleanChannel] ?: 0) + 1
            }

            // Locations
            val loc = (app["location"] as? String)?.trim()
            if (!loc.isNullOrBlank()) {
                locations.add(loc)
            }
        }

        val total = draft + submitted + interviewing + offer + rejected
        val percentages = if (total > 0) {
            mapOf(
                "draft" to (draft.toFloat() / total),
                "submitted" to (submitted.toFloat() / total),
                "interviewing" to (interviewing.toFloat() / total),
                "offer" to (offer.toFloat() / total),
                "rejected" to (rejected.toFloat() / total)
            )
        } else {
            mapOf(
                "draft" to 0f,
                "submitted" to 0f,
                "interviewing" to 0f,
                "offer" to 0f,
                "rejected" to 0f
            )
        }

        val avgFit = if (fitScores.isNotEmpty()) {
            fitScores.average().toInt()
        } else {
            null
        }

        val salaryRangeMap = mutableMapOf<String, Any>(
            "currency" to salaryCurrency,
            "display" to formatSalaryDisplay(minSalary, maxSalary, salaryCurrency)
        )
        if (minSalary != null) salaryRangeMap["min"] = minSalary
        if (maxSalary != null) salaryRangeMap["max"] = maxSalary

        val primaryChannel = channelCounts.maxByOrNull { it.value }?.key ?: "DIRECT"

        val protoBuilder = CompanyMetrics.newBuilder()
            .setCounts(
                CompanyMetrics.Counts.newBuilder()
                    .setDraft(draft)
                    .setSubmitted(submitted)
                    .setInterviewing(interviewing)
                    .setOffer(offer)
                    .setRejected(rejected)
                    .setTotal(total)
                    .build()
            )
            .setPercentages(
                CompanyMetrics.Percentages.newBuilder()
                    .setDraft(percentages["draft"] ?: 0f)
                    .setSubmitted(percentages["submitted"] ?: 0f)
                    .setInterviewing(percentages["interviewing"] ?: 0f)
                    .setOffer(percentages["offer"] ?: 0f)
                    .setRejected(percentages["rejected"] ?: 0f)
                    .build()
            )
            .setSalaryRange(
                CompanyMetrics.SalaryRange.newBuilder()
                    .setCurrency(salaryCurrency)
                    .setDisplay(salaryRangeMap["display"] as String)
                    .apply {
                        if (minSalary != null) setMin(minSalary)
                        if (maxSalary != null) setMax(maxSalary)
                    }
                    .build()
            )
            .addAllLocations(locations)
            .setLastCalculatedAt(
                com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(Instant.now().epochSecond)
                    .setNanos(0)
                    .build()
            )

        if (avgFit != null) {
            protoBuilder.setAverageFitScore(avgFit)
        }

        val channelEnum = try {
            ApplicationChannel.valueOf(primaryChannel)
        } catch (_: IllegalArgumentException) {
            ApplicationChannel.DIRECT
        }
        protoBuilder.setPrimaryChannel(channelEnum)

        return CompanyMetricsParser.toMap(protoBuilder.build())
    }

    private fun formatSalaryDisplay(min: Double?, max: Double?, currency: String): String {
        val symbol = if (currency.equals("USD", ignoreCase = true)) "$" else "$currency "
        return when {
            min != null && max != null && min != max -> {
                "${symbol}${formatNumber(min)} - ${symbol}${formatNumber(max)}"
            }
            min != null -> "${symbol}${formatNumber(min)}"
            max != null -> "${symbol}${formatNumber(max)}"
            else -> ""
        }
    }

    private fun formatNumber(num: Double): String {
        return if (num >= 1000.0) {
            val inK = num / 1000.0
            if (inK == inK.toLong().toDouble()) {
                "${inK.toLong()}k"
            } else {
                String.format(Locale.ROOT, "%.1fk", inK)
            }
        } else {
            num.toLong().toString()
        }
    }

    /**
     * Recalculates metrics for all companies (or a specific [targetCompanyId])
     * and atomically writes them to Firestore `companies/{companyId}.metrics`.
     */
    public fun recalculateCompanyMetrics(targetCompanyId: String? = null): Map<String, Any> {
        val db = firestore
        var query = db.collection("applications") as com.google.cloud.firestore.Query
        if (!targetCompanyId.isNullOrBlank()) {
            query = query.whereEqualTo("companyId", targetCompanyId)
        }

        val snapshot = query.get().get()
        val appsByCompany = snapshot.documents.groupBy { doc ->
            val rawCompanyId = doc.getString("companyId")?.takeIf { it.isNotBlank() }
            val rawCompanyName = doc.getString("company")?.takeIf { it.isNotBlank() }
            when {
                rawCompanyId != null -> DocumentIdGenerator.slug(rawCompanyId, fallback = rawCompanyId)
                rawCompanyName != null -> DocumentIdGenerator.slug(rawCompanyName, fallback = "unknown")
                else -> "unknown"
            }
        }

        var companiesUpdated = 0
        for ((companyId, docs) in appsByCompany) {
            if (companyId == "unknown") continue
            val appMaps = docs.map { it.data }
            val metricsMap = calculateMetricsForApplications(appMaps)

            val companyDoc = db.collection("companies").document(companyId)
            val companyName = docs.firstOrNull()?.getString("company") ?: companyId

            val updatePayload = mapOf(
                "companyId" to companyId,
                "name" to companyName,
                "metrics" to metricsMap,
                "updatedAt" to Timestamp.now()
            )

            companyDoc.set(updatePayload, SetOptions.merge()).get()
            companiesUpdated++
        }

        return mapOf(
            "status" to "SUCCESS",
            "companiesUpdated" to companiesUpdated,
            "targetCompanyId" to (targetCompanyId ?: "ALL"),
            "timestamp" to Timestamp.now().toString()
        )
    }

    public fun performRoutineSweep(): Map<String, Any> {
        val expiredCount = try {
            expireStaleProposals()
        } catch (e: Exception) {
            println("Warning: expireStaleProposals in routine sweep: ${e.message}")
            0
        }
        val syncCount = try {
            processDueSyncs()
        } catch (e: Exception) {
            println("Warning: processDueSyncs in routine sweep: ${e.message}")
            0
        }
        val metricsResult = recalculateCompanyMetrics()

        val db = firestore
        val sweepTimestamp = Timestamp.now()
        db.collection("meta").document("sync").set(
            mapOf(
                "lastSweepAt" to sweepTimestamp,
                "staleProposalsExpired" to expiredCount,
                "dueSyncsProcessed" to syncCount,
                "companiesMetricsUpdated" to metricsResult["companiesUpdated"],
                "actor" to "CLOUD_RUN_SWEEP"
            ),
            SetOptions.merge()
        ).get()

        return mapOf(
            "status" to "SUCCESS",
            "message" to "Routine sweep completed successfully",
            "sweepAt" to sweepTimestamp.toString(),
            "staleProposalsExpired" to expiredCount,
            "dueSyncsProcessed" to syncCount,
            "companiesMetricsUpdated" to (metricsResult["companiesUpdated"] ?: 0)
        )
    }

    /**
     * Batch-enriches all company records in Firestore using Hunter.io Company Enrichment API.
     * Credit-aware: stops at 45 API calls to stay within the free tier (50 credits/month).
     * Skips companies enriched within 6 months (based on `lastHunterEnrichedAt`).
     * A company is "fully enriched" if it has `sector`, `linkedinUrl`, and `hunterIoId`.
     */
    public fun batchEnrichCompanies(): Map<String, Any> {
        val db = firestore
        val snapshot = db.collection("companies").get().get()
        var enriched = 0
        var skipped = 0
        var errors = 0
        var creditsUsed = 0
        val MAX_CREDITS = 45 // Safety margin for free tier (50 credits/month)
        val errorDetails = mutableListOf<String>()

        // Calculate 6 months ago for skip logic
        val sixMonthsAgo = java.time.Instant.now().minus(java.time.Duration.ofDays(183))

        for (doc in snapshot.documents) {
            // Check credit budget
            if (creditsUsed >= MAX_CREDITS) {
                println("Credit budget reached ($MAX_CREDITS credits). Stopping enrichment.")
                skipped++
                continue
            }

            val data = doc.data ?: continue
            val companyId = doc.id

            // Check if fully enriched recently
            val hasSector = !((data["sector"] as? String).isNullOrBlank())
            val hasLinkedinUrl = !((data["linkedinUrl"] as? String).isNullOrBlank())
            val hasHunterIoId = !((data["hunterIoId"] as? String).isNullOrBlank())

            // Check lastHunterEnrichedAt timestamp
            val lastEnrichedAt = data["lastHunterEnrichedAt"]
            val enrichedRecently = when (lastEnrichedAt) {
                is Timestamp -> {
                    lastEnrichedAt.toDate().toInstant().isAfter(sixMonthsAgo)
                }
                is String -> {
                    try {
                        Instant.parse(lastEnrichedAt).isAfter(sixMonthsAgo)
                    } catch (_: Exception) {
                        false
                    }
                }
                else -> false
            }

            // Skip if fully enriched and recent
            if (hasSector && hasLinkedinUrl && hasHunterIoId && enrichedRecently) {
                skipped++
                continue
            }

            // Try to enrich via Hunter.io Company Enrichment API
            try {
                val companyName = (data["name"] as? String) ?: companyId
                val website = (data["website"] as? String) ?: run {
                    // Try to derive website from company name
                    val domain = companyName.lowercase().replace(Regex("[^a-z0-9]+"), ".")
                    "https://www.$domain.com"
                }

                creditsUsed++
                val hunterResult = fetchHunterCompanyInfo(website)

                if (hunterResult.isEmpty()) {
                    // Hunter returned no data — use fallbacks only
                    val resolvedSector = CompanySectorNormalizer.normalize(
                        companyId = companyId,
                        companyName = companyName
                    ).name
                    val enrichResult = enrichCompany(
                        companyId = companyId,
                        companyName = companyName,
                        linkedinUrl = null,
                        industry = resolvedSector
                    )

                    if (enrichResult["updated"] as? Boolean == true) {
                        enriched++
                    } else {
                        skipped++
                    }
                    continue
                }

                // Extract all fields from Hunter.io result
                val hunterIndustry = hunterResult["industry"] as? String
                val hunterSector = hunterResult["sector"] as? String
                val hunterLinkedinHandle = hunterResult["linkedin_handle"] as? String
                val hunterLinkedinUrl = if (!hunterLinkedinHandle.isNullOrBlank()) {
                    "https://www.linkedin.com/$hunterLinkedinHandle"
                } else {
                    null
                }

                // Resolve sector and linkedinUrl using fallbacks if Hunter didn't return them
                val resolvedSector = CompanySectorNormalizer.normalize(
                    companyId = companyId,
                    industryOrSector = hunterIndustry ?: hunterSector,
                    companyName = companyName
                ).name
                val enrichResult = enrichCompany(
                    companyId = companyId,
                    companyName = companyName,
                    linkedinUrl = hunterLinkedinUrl,
                    industry = hunterIndustry ?: hunterSector ?: resolvedSector,
                    logoUrl = hunterResult["logo_url"] as? String,
                    hunterIoId = hunterResult["hunter_io_id"] as? String,
                    description = hunterResult["description"] as? String,
                    foundedYear = hunterResult["foundedYear"] as? Int,
                    companyType = hunterResult["companyType"] as? String,
                    employees = hunterResult["employees"] as? String,
                    employeesCount = hunterResult["employeesCount"] as? Int,
                    location = hunterResult["location"] as? String,
                    city = hunterResult["city"] as? String,
                    state = hunterResult["state"] as? String,
                    country = hunterResult["country"] as? String,
                    subIndustry = hunterResult["subIndustry"] as? String,
                    twitterHandle = hunterResult["twitter_handle"] as? String,
                    crunchbaseHandle = hunterResult["crunchbase_handle"] as? String,
                    tags = hunterResult["tags"] as? List<String>,
                    raised = hunterResult["raised"] as? Long
                )

                if (enrichResult["updated"] as? Boolean == true) {
                    enriched++
                } else {
                    skipped++
                }
            } catch (e: Exception) {
                errors++
                errorDetails.add("$companyId: ${e.message}")
            }
        }

        return mapOf(
            "status" to "SUCCESS",
            "enriched" to enriched,
            "skipped" to skipped,
            "errors" to errors,
            "creditsUsed" to creditsUsed,
            "creditsRemaining" to (MAX_CREDITS - creditsUsed),
            "errorDetails" to errorDetails,
            "timestamp" to Timestamp.now().toString()
        )
    }

    /**
     * Fetches company info from Hunter.io Company Enrichment API.
     * Uses the `/v2/companies/find` endpoint which returns full company metadata.
     * Extracts: hunter_io_id, name, industry, subIndustry, sector, linkedin_handle,
     * twitter_handle, crunchbase_handle, logo_url, description, foundedYear, companyType,
     * employees, employeesCount, location, city, state, country, tags, and raised.
     */
    private fun fetchHunterCompanyInfo(website: String): Map<String, Any?> {
        val apiKey = System.getenv("HUNTER_IO_API_KEY")
        if (apiKey.isNullOrBlank()) {
            return emptyMap()
        }

        // Normalize website to domain
        val domain = try {
            val url = java.net.URL(website)
            url.host.removePrefix("www.")
        } catch (_: Exception) {
            website.removePrefix("https://").removePrefix("http://").removePrefix("www.").split("/").first()
        }

        val apiUrl = "https://api.hunter.io/v2/companies/find?domain=$domain&api_key=$apiKey"
        val connection = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 10000

        try {
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.parseToJsonElement(response)
                val data = json.jsonObject["data"]?.jsonObject ?: return emptyMap()

                // Extract category fields
                val category = data["category"]?.jsonObject
                val sector = category?.get("sector")?.jsonPrimitive?.contentOrNull
                val industry = category?.get("industry")?.jsonPrimitive?.contentOrNull
                val subIndustry = category?.get("subIndustry")?.jsonPrimitive?.contentOrNull

                // Extract geo fields
                val geo = data["geo"]?.jsonObject
                val city = geo?.get("city")?.jsonPrimitive?.contentOrNull
                val state = geo?.get("state")?.jsonPrimitive?.contentOrNull
                val country = geo?.get("country")?.jsonPrimitive?.contentOrNull

                // Extract metrics fields
                val metrics = data["metrics"]?.jsonObject
                val employees = metrics?.get("employees")?.jsonPrimitive?.contentOrNull
                val employeesCount = metrics?.get("employeesCount")?.jsonPrimitive?.contentOrNull?.toInt()
                val raised = metrics?.get("raised")?.jsonPrimitive?.contentOrNull?.toLong()

                // Extract social handles
                val linkedinHandle = data["linkedin"]?.jsonObject?.get("handle")?.jsonPrimitive?.contentOrNull
                val twitterHandle = data["twitter"]?.jsonObject?.get("handle")?.jsonPrimitive?.contentOrNull
                val crunchbaseHandle = data["crunchbase"]?.jsonObject?.get("handle")?.jsonPrimitive?.contentOrNull

                // Extract tags array
                val tags = data["tags"]?.jsonArray?.map { it.jsonPrimitive.content }

                return mapOf(
                    "hunter_io_id" to (data["id"]?.jsonPrimitive?.contentOrNull),
                    "name" to (data["name"]?.jsonPrimitive?.contentOrNull),
                    "industry" to industry,
                    "subIndustry" to subIndustry,
                    "sector" to sector,
                    "linkedin_handle" to linkedinHandle,
                    "twitter_handle" to twitterHandle,
                    "crunchbase_handle" to crunchbaseHandle,
                    "logo_url" to (data["logo"]?.jsonPrimitive?.contentOrNull),
                    "description" to (data["description"]?.jsonPrimitive?.contentOrNull),
                    "foundedYear" to (data["foundedYear"]?.jsonPrimitive?.contentOrNull?.toInt()),
                    "companyType" to (data["companyType"]?.jsonPrimitive?.contentOrNull),
                    "employees" to employees,
                    "employeesCount" to employeesCount,
                    "location" to (data["location"]?.jsonPrimitive?.contentOrNull),
                    "city" to city,
                    "state" to state,
                    "country" to country,
                    "tags" to tags,
                    "raised" to raised
                )
            }
        } finally {
            connection.disconnect()
        }

        return emptyMap()
    }

    /**
     * Creates a new company record from name and optional website.
     * Derives canonical slug via [DocumentIdGenerator.slug] from name, resolves careersUrl from website if provided.
     *
     * @param name Display name of the company.
     * @param website Optional company website URL.
     * @return Result map containing status (`EXISTS` or `CREATED`), `companyId`, and `company` fields.
     */
    public fun addCompany(name: String, website: String? = null): Map<String, Any?> {
        val db = firestore
        val slug = DocumentIdGenerator.slug(name, fallback = "unknown-company")

        val companyDoc = db.collection("companies").document(slug)
        val existingSnap = companyDoc.get().get()

        if (existingSnap.exists()) {
            return mapOf(
                "status" to "EXISTS",
                "companyId" to slug,
                "company" to existingSnap.data
            )
        }

        val careersUrl = when {
            !website.isNullOrBlank() -> {
                val normalizedWebsite = website.removeSuffix("/")
                "$normalizedWebsite/careers"
            }
            else -> null
        }

        val now = Timestamp.now()
        val payload = mutableMapOf<String, Any>(
            "companyId" to slug,
            "name" to name,
            "createdAt" to now,
            "updatedAt" to now
        )

        if (!website.isNullOrBlank()) {
            payload["website"] = website.removeSuffix("/")
        }

        if (!careersUrl.isNullOrBlank()) {
            payload["careersUrl"] = careersUrl
        }

        companyDoc.set(payload).get()

        val finalDoc = companyDoc.get().get().data ?: payload
        return mapOf(
            "status" to "CREATED",
            "companyId" to slug,
            "company" to finalDoc
        )
    }

    /**
     * Deduplicates records in the Firestore `companies` collection and aligns `applications.companyId`
     * to canonical slugs derived via [DocumentIdGenerator.slug].
     *
     * For any duplicate group (e.g., `ford-motor-company` and `ford-motor`, or `meta-platforms` and `meta`):
     * 1. Merges non-blank fields (`logoUrl`, `linkedinUrl`, `website`, `sector`, `aliases`, etc.) into
     *    the canonical `companies/{canonicalId}` document.
     * 2. Re-points any `applications` documents referencing a non-canonical `companyId` to `canonicalId`.
     * 3. Renames application document IDs when the company slug prefix has changed (e.g.,
     *    `ford-motor--staff-android-app` → `ford--staff-android-app`), migrating all subcollections
     *    (events, artifacts) and updating email references.
     * 4. Deletes the non-canonical `companies/{duplicateId}` document when [dryRun] is `false`.
     * 5. Recomputes company metrics across all merged companies.
     *
     * @param dryRun When `true`, computes and returns all planned merges without mutating Firestore.
     * @return Summary map detailing merged groups, updated applications, renamed documents, and deleted duplicate IDs.
     */
    public fun deduplicateCompanies(dryRun: Boolean = false): Map<String, Any> {
        val db = firestore
        val companiesSnapshot = db.collection("companies").get().get()
        val appsSnapshot = db.collection("applications").get().get()

        data class CompanyEntry(
            val docId: String,
            val data: Map<String, Any?>,
            val canonicalId: String,
        )

        val entries = companiesSnapshot.documents.map { doc ->
            val data: Map<String, Any?> = doc.data
            val name = (data["name"] as? String)?.takeIf { it.isNotBlank() }
                ?: (data["company"] as? String)?.takeIf { it.isNotBlank() }
                ?: doc.id
            val byName = DocumentIdGenerator.slug(name)
            val byId = DocumentIdGenerator.slug(doc.id)
            val canonicalId = byName.ifEmpty { byId.ifEmpty { doc.id } }
            CompanyEntry(docId = doc.id, data = data, canonicalId = canonicalId)
        }

        val grouped = entries.groupBy { it.canonicalId }
        val mergedGroups = mutableListOf<String>()
        val deletedIds = mutableListOf<String>()
        var applicationsUpdated = 0
        val renamedApplications = mutableListOf<String>()

        for ((canonicalId, group) in grouped) {
            val needsConsolidation = group.size > 1 || group.any { it.docId != canonicalId }
            if (!needsConsolidation) continue

            val canonicalEntry = group.find { it.docId == canonicalId }
            val mergedData = mutableMapOf<String, Any?>()

            // Seed from non-canonical entries first, then overlay canonical entry so canonical values win when present
            val ordered = group.sortedBy { if (it.docId == canonicalId) 1 else 0 }
            val collectedAliases = linkedSetOf<String>()

            for (entry in ordered) {
                for ((k, v) in entry.data) {
                    if (v != null && (v !is String || v.isNotBlank())) {
                        mergedData[k] = v
                    }
                }
                val entryAliases = (entry.data["aliases"] as? List<*>)
                    ?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
                    .orEmpty()
                collectedAliases.addAll(entryAliases)
                val entryName = (entry.data["name"] as? String)?.trim()
                if (!entryName.isNullOrBlank()) {
                    collectedAliases.add(entryName)
                }
                if (entry.docId != canonicalId) {
                    collectedAliases.add(entry.docId)
                }
            }

            val canonicalName = (canonicalEntry?.data?.get("name") as? String)?.takeIf { it.isNotBlank() }
                ?: (mergedData["name"] as? String)?.takeIf { it.isNotBlank() }
                ?: canonicalId
            collectedAliases.remove(canonicalName)
            collectedAliases.remove(canonicalId)

            mergedData["companyId"] = canonicalId
            mergedData["name"] = canonicalName
            if (collectedAliases.isNotEmpty()) {
                mergedData["aliases"] = collectedAliases.toList()
            }
            mergedData["updatedAt"] = Timestamp.now()

            val duplicateDocIds = group.map { it.docId }.filter { it != canonicalId }
            mergedGroups.add("$canonicalId <- ${duplicateDocIds.ifEmpty { listOf(canonicalId) }}")

            if (!dryRun) {
                val filteredPayload = mergedData.filterValues { it != null }.mapValues { it.value!! }
                db.collection("companies").document(canonicalId)
                    .set(filteredPayload, SetOptions.merge())
                    .get()

                for (dupId in duplicateDocIds) {
                    db.collection("companies").document(dupId).delete().get()
                    deletedIds.add(dupId)
                }
            } else {
                deletedIds.addAll(duplicateDocIds)
            }
        }

        // Build a map of old company slugs to canonical slugs for document ID renaming
        val oldSlugsToCanonical = mutableMapOf<String, String>()
        for ((canonicalId, group) in grouped) {
            val needsConsolidation = group.size > 1 || group.any { it.docId != canonicalId }
            if (!needsConsolidation) continue
            for (entry in group) {
                if (entry.docId != canonicalId) {
                    oldSlugsToCanonical[entry.docId] = canonicalId
                }
            }
        }

        // Also ensure all applications have canonical companyId and rename document IDs where needed
        for (appDoc in appsSnapshot.documents) {
            val existingCompanyId = appDoc.getString("companyId")?.trim().orEmpty()
            val companyName = appDoc.getString("company")?.trim().orEmpty()
            val role = appDoc.getString("role")?.trim().orEmpty()
            val expectedCompanyId = when {
                companyName.isNotEmpty() -> DocumentIdGenerator.slug(companyName)
                existingCompanyId.isNotEmpty() -> DocumentIdGenerator.slug(existingCompanyId)
                else -> ""
            }

            // Check if companyId field needs update
            val needsCompanyIdUpdate = expectedCompanyId.isNotEmpty() && existingCompanyId != expectedCompanyId

            // Check if document ID needs renaming (company slug prefix changed)
            val currentDocId = appDoc.id
            val expectedDocId = if (role.isNotEmpty()) {
                DocumentIdGenerator.documentId(companyName, role)
            } else {
                expectedCompanyId
            }
            val needsDocIdRename = currentDocId != expectedDocId && expectedDocId.isNotEmpty()

            if (needsCompanyIdUpdate) {
                applicationsUpdated++
            }

            if (needsDocIdRename) {
                renamedApplications.add("$currentDocId -> $expectedDocId")
                if (!dryRun) {
                    renameApplicationDocument(db, appDoc, expectedDocId, expectedCompanyId, companyName)
                }
            } else if (needsCompanyIdUpdate && !dryRun) {
                // Only companyId field update, no document rename needed
                appDoc.reference.set(
                    mapOf(
                        "companyId" to expectedCompanyId,
                        "updatedAt" to Timestamp.now(),
                    ),
                    SetOptions.merge(),
                ).get()
            }
        }

        if (!dryRun && (deletedIds.isNotEmpty() || applicationsUpdated > 0 || renamedApplications.isNotEmpty())) {
            recalculateCompanyMetrics()
        }

        return mapOf(
            "status" to "SUCCESS",
            "dryRun" to dryRun,
            "mergedGroupsCount" to mergedGroups.size,
            "mergedGroups" to mergedGroups,
            "deletedCompanyIdsCount" to deletedIds.size,
            "deletedCompanyIds" to deletedIds,
            "applicationsUpdated" to applicationsUpdated,
            "applicationsRenamedCount" to renamedApplications.size,
            "applicationsRenamed" to renamedApplications,
            "timestamp" to Timestamp.now().toString(),
        )
    }

    /**
     * Renames a Firestore application document, migrating all subcollections and updating email references.
     *
     * This is the core migration primitive: it creates the target document with all data and subcollections,
     * updates all `emails` documents that reference the old application ID, then deletes the source document.
     *
     * @param db The Firestore instance.
     * @param sourceDoc The source document snapshot.
     * @param targetDocId The new document ID.
     * @param canonicalCompanyId The canonical company ID to set.
     * @param companyName The company name to preserve.
     */
    private fun renameApplicationDocument(
        db: Firestore,
        sourceDoc: com.google.cloud.firestore.DocumentSnapshot,
        targetDocId: String,
        canonicalCompanyId: String,
        companyName: String,
    ) {
        val sourceRef = sourceDoc.reference
        val targetRef = db.collection("applications").document(targetDocId)
        val sourceData = (sourceDoc.data ?: emptyMap<String, Any?>()).toMutableMap()

        // Update companyId and id fields in the target document
        sourceData["companyId"] = canonicalCompanyId
        sourceData["id"] = targetDocId
        sourceData["updatedAt"] = Timestamp.now()

        // Discover all subcollections under the source document
        // Known subcollections: events, artifacts
        val knownSubcollections = listOf("events", "artifacts")
        val subcollectionsData = mutableMapOf<String, List<Pair<String, Map<String, Any?>>>>()

        for (collName in knownSubcollections) {
            val collRef = sourceRef.collection(collName)
            val subdocsSnap = collRef.get().get()
            val subdocs = subdocsSnap.documents.map { doc: com.google.cloud.firestore.DocumentSnapshot ->
                doc.id to (doc.data ?: emptyMap<String, Any?>())
            }
            if (subdocs.isNotEmpty()) {
                subcollectionsData[collName] = subdocs
            }
        }

        // Write new document and copy all subcollections in a batch
        val batch = db.batch()
        batch.set(targetRef, sourceData)

        subcollectionsData.forEach { (collName, subdocs) ->
            subdocs.forEach { (subdocId, subdocData) ->
                batch.set(targetRef.collection(collName).document(subdocId), subdocData)
            }
        }
        batch.commit()

        // Update all emails that reference the old application ID
        val emailsSnapshot = db.collection("emails").whereEqualTo("applicationId", sourceDoc.id).get().get()
        if (!emailsSnapshot.documents.isEmpty()) {
            val emailBatch = db.batch()
            for (emailDoc in emailsSnapshot.documents) {
                emailBatch.set(
                    emailDoc.reference,
                    mapOf("applicationId" to targetDocId, "updatedAt" to Instant.now().toString()),
                    SetOptions.merge(),
                )
            }
            emailBatch.commit()
        }

        // Delete old subcollections and old document
        val deleteBatch = db.batch()
        subcollectionsData.forEach { (collName, subdocs) ->
            subdocs.forEach { (subdocId, _) ->
                deleteBatch.delete(sourceRef.collection(collName).document(subdocId))
            }
        }
        deleteBatch.delete(sourceRef)
        deleteBatch.commit()

        println("Migrated application: $sourceDoc.id -> $targetDocId")
    }

    /**
     * Enriches a company record in Firestore with Hunter.io metadata.
     * Non-destructive and idempotent — only writes fields that are missing or null.
     */
    public fun enrichCompany(
        companyId: String,
        companyName: String,
        linkedinUrl: String? = null,
        industry: String? = null,
        logoUrl: String? = null,
        hunterIoId: String? = null,
        description: String? = null,
        foundedYear: Int? = null,
        companyType: String? = null,
        employees: String? = null,
        employeesCount: Int? = null,
        location: String? = null,
        city: String? = null,
        state: String? = null,
        country: String? = null,
        subIndustry: String? = null,
        twitterHandle: String? = null,
        crunchbaseHandle: String? = null,
        tags: List<String>? = null,
        raised: Long? = null,
    ): Map<String, Any?> {
        val db = firestore
        val compRef = db.collection("companies").document(companyId)
        val snap = compRef.get().get()

        val existingData = if (snap.exists()) snap.data ?: emptyMap() else emptyMap()
        val updates = mutableMapOf<String, Any?>()

        if (!existingData.containsKey("companyId")) {
            updates["companyId"] = companyId
        }
        if (!existingData.containsKey("name") || existingData["name"] == null) {
            updates["name"] = companyName
        }

        // LinkedIn URL
        if (linkedinUrl != null && (existingData["linkedinUrl"] as? String).isNullOrBlank()) {
            updates["linkedinUrl"] = linkedinUrl
        }

        // Twitter URL
        if (twitterHandle != null && (existingData["twitterUrl"] as? String).isNullOrBlank()) {
            updates["twitterUrl"] = "https://twitter.com/$twitterHandle"
        }

        // Crunchbase URL
        if (crunchbaseHandle != null && (existingData["crunchbaseUrl"] as? String).isNullOrBlank()) {
            updates["crunchbaseUrl"] = "https://www.crunchbase.com/organization/$crunchbaseHandle"
        }

        // Sector / Industry enrichment
        val normalizedSector = CompanySectorNormalizer.normalize(
            companyId = companyId,
            industryOrSector = industry,
            companyName = companyName
        ).name
        if (existingData["sector"] != normalizedSector) {
            updates["sector"] = normalizedSector
        }
        if (industry != null) {
            val existingMeta = (existingData["metadata"] as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
            existingMeta["hunterIndustry"] = industry
            updates["metadata"] = existingMeta
        }

        // Sub-industry
        if (subIndustry != null && (existingData["subIndustry"] as? String).isNullOrBlank()) {
            updates["subIndustry"] = subIndustry
        }

        // Hunter.io stable ID
        if (hunterIoId != null && (existingData["hunterIoId"] as? String).isNullOrBlank()) {
            updates["hunterIoId"] = hunterIoId
        }

        // Description
        if (description != null && (existingData["description"] as? String).isNullOrBlank()) {
            updates["description"] = description
        }

        // Founded year
        if (foundedYear != null && !existingData.containsKey("foundedYear")) {
            updates["foundedYear"] = foundedYear
        }

        // Company type
        if (companyType != null && (existingData["companyType"] as? String).isNullOrBlank()) {
            updates["companyType"] = companyType
        }

        // Employee range string
        if (employees != null && (existingData["employees"] as? String).isNullOrBlank()) {
            updates["employees"] = employees
        }

        // Employee count
        if (employeesCount != null && !existingData.containsKey("employeesCount")) {
            updates["employeesCount"] = employeesCount
        }

        // Location
        if (location != null && (existingData["location"] as? String).isNullOrBlank()) {
            updates["location"] = location
        }

        // City
        if (city != null && (existingData["city"] as? String).isNullOrBlank()) {
            updates["city"] = city
        }

        // State
        if (state != null && (existingData["state"] as? String).isNullOrBlank()) {
            updates["state"] = state
        }

        // Country
        if (country != null && (existingData["country"] as? String).isNullOrBlank()) {
            updates["country"] = country
        }

        // Tags and raised go into metadata map
        if (tags != null && tags.isNotEmpty()) {
            val existingMeta = (existingData["metadata"] as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
            existingMeta["hunterTags"] = tags
            updates["metadata"] = existingMeta
        }
        if (raised != null) {
            val existingMeta = (existingData["metadata"] as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
            existingMeta["hunterRaised"] = raised
            updates["metadata"] = existingMeta
        }

        // Logo enrichment
        if (logoUrl != null && (existingData["logoUrl"] as? String).isNullOrBlank()) {
            updates["logoUrl"] = logoUrl
            updates["logoFetchedAt"] = Timestamp.now()
        }

        if (updates.isNotEmpty()) {
            val now = Timestamp.now()
            updates["updatedAt"] = now
            updates["lastEnrichedAt"] = now
            updates["lastHunterEnrichedAt"] = now
            compRef.set(updates, SetOptions.merge()).get()
        }

        val finalDoc = compRef.get().get().data ?: updates
        return mapOf(
            "status" to "SUCCESS",
            "companyId" to companyId,
            "updated" to updates.isNotEmpty(),
            "company" to finalDoc
        )
    }

    /**
     * Aligns an application's parent record with its events subcollection:
     * recalculates derived notesRendered, timeline, counters, and submissionConfirmation,
     * merging updates atomically into Firestore.
     */
    public fun alignApplication(
        applicationId: String,
        dryRun: Boolean = false,
    ): Map<String, Any?> {
        val db = firestore
        val appRef = db.collection("applications").document(applicationId)
        val snap = appRef.get().get()
        if (!snap.exists()) {
            throw IllegalArgumentException("Application not found: $applicationId")
        }

        val eventsDocs = appRef.collection("events").get().get().documents
        val existingEvents = eventsDocs.map { mapToTrackerEvent(it.id, it.data) }

        val timeline = ApplicationTimelineProjector.projectTimeline(existingEvents)
        val submissionConfirmation = ApplicationTimelineProjector.projectSubmissionConfirmation(null, existingEvents)
        val counters = ApplicationTimelineProjector.projectCounters(existingEvents)
        val notesRendered = ApplicationTimelineProjector.projectNotes(existingEvents)
        val latestStatus = ApplicationTimelineProjector.latestStatus(existingEvents)

        val updates = mutableMapOf<String, Any?>(
            "notesRendered" to notesRendered,
            "timeline" to mapOf(
                "submittedAt" to if (timeline.hasSubmittedAt() && timeline.submittedAt.seconds > 0)
                    Instant.ofEpochSecond(timeline.submittedAt.seconds).toString() else null,
                "submissionConfirmedAt" to if (timeline.hasSubmissionConfirmedAt() && timeline.submissionConfirmedAt.seconds > 0)
                    Instant.ofEpochSecond(timeline.submissionConfirmedAt.seconds).toString() else null,
                "firstActivityAt" to if (timeline.hasFirstActivityAt() && timeline.firstActivityAt.seconds > 0)
                    Instant.ofEpochSecond(timeline.firstActivityAt.seconds).toString() else null,
                "lastActivityAt" to if (timeline.hasLastActivityAt() && timeline.lastActivityAt.seconds > 0)
                    Instant.ofEpochSecond(timeline.lastActivityAt.seconds).toString() else null,
                "daysSinceActivity" to timeline.daysSinceActivity
            ),
            "submissionConfirmation" to mapOf(
                "isConfirmed" to submissionConfirmation.isConfirmed,
                "confidence" to submissionConfirmation.confidence.name,
                "submittedAt" to if (submissionConfirmation.hasSubmittedAt() && submissionConfirmation.submittedAt.seconds > 0)
                    Instant.ofEpochSecond(submissionConfirmation.submittedAt.seconds).toString() else null,
                "confirmedAt" to if (submissionConfirmation.hasConfirmedAt() && submissionConfirmation.confirmedAt.seconds > 0)
                    Instant.ofEpochSecond(submissionConfirmation.confirmedAt.seconds).toString() else null,
                "confirmationEmailMessageId" to submissionConfirmation.confirmationEmailMessageId.takeIf { it.isNotBlank() },
                "sourceSubject" to submissionConfirmation.sourceSubject.takeIf { it.isNotBlank() },
                "notes" to submissionConfirmation.notes.takeIf { it.isNotBlank() }
            ),
            "counters" to counters
        )

        if (latestStatus != null && latestStatus != TrackerStatus.TRACKER_STATUS_UNSPECIFIED) {
            updates["status"] = latestStatus.name
        }

        if (!dryRun) {
            updates["updatedAt"] = Timestamp.now()
            appRef.set(updates, SetOptions.merge()).get()
        }

        return mapOf(
            "status" to "SUCCESS",
            "applicationId" to applicationId,
            "dryRun" to dryRun,
            "updates" to updates
        )
    }

    /**
     * Reassigns an email to an application or updates its evidentiary classification status.
     *
     * Ensures transactional consistency by removing the message ID from the former application's
     * emailMessageIds list and adding it to the new application's emailMessageIds list.
     */
    public fun rectifyEmail(
        messageId: String,
        targetApplicationId: String?,
        status: String,
        isNoise: Boolean = false,
        dryRun: Boolean = false,
    ): Map<String, Any?> {
        val db = firestore
        val emailRef = db.collection("emails").document(messageId)
        val emailSnap = emailRef.get().get()

        val normalizedStatus = if (isNoise) "NOISE" else status.trim().uppercase()
        val effectiveAppId = if (normalizedStatus == "NOISE") null else targetApplicationId?.trim()?.takeIf { it.isNotBlank() }

        val previousAppId = if (emailSnap.exists()) emailSnap.getString("applicationId") else null

        var resolvedCompanyId: String? = null
        if (!effectiveAppId.isNullOrBlank()) {
            val appDoc = db.collection("applications").document(effectiveAppId).get().get()
            if (!appDoc.exists()) {
                throw IllegalArgumentException("Target application $effectiveAppId not found.")
            }
            resolvedCompanyId = appDoc.getString("companyId")
        }

        val updates = mutableMapOf<String, Any?>(
            "id" to messageId,
            "status" to normalizedStatus,
            "applicationId" to effectiveAppId,
            "companyId" to resolvedCompanyId,
            "rectifiedAt" to Instant.now().toString(),
            "updatedAt" to Instant.now().toString(),
        )

        if (!dryRun) {
            db.runTransaction { tx ->
                // 1. Remove from previous application if changed
                if (!previousAppId.isNullOrBlank() && previousAppId != effectiveAppId) {
                    val prevRef = db.collection("applications").document(previousAppId)
                    val prevSnap = tx.get(prevRef).get()
                    if (prevSnap.exists()) {
                        val existingIds = ((prevSnap.get("emailMessageIds") as? List<*>) ?: emptyList<Any>())
                            .map { it.toString() }
                            .filter { it != messageId }
                        tx.update(prevRef, mapOf(
                            "emailMessageIds" to existingIds,
                            "updatedAt" to Timestamp.now()
                        ))
                    }
                }

                // 2. Add to target application if non-null and not already present
                if (!effectiveAppId.isNullOrBlank() && effectiveAppId != previousAppId) {
                    val targetRef = db.collection("applications").document(effectiveAppId)
                    val targetSnap = tx.get(targetRef).get()
                    if (targetSnap.exists()) {
                        val existingIds = ((targetSnap.get("emailMessageIds") as? List<*>) ?: emptyList<Any>())
                            .map { it.toString() }
                            .toMutableSet()
                        existingIds.add(messageId)
                        tx.update(targetRef, mapOf(
                            "emailMessageIds" to existingIds.toList(),
                            "updatedAt" to Timestamp.now()
                        ))
                    }
                }

                // 3. Update the email document
                tx.set(emailRef, updates, SetOptions.merge())
                null
            }.get()
        }

        return mapOf(
            "status" to "SUCCESS",
            "messageId" to messageId,
            "applicationId" to effectiveAppId,
            "previousApplicationId" to previousAppId,
            "emailStatus" to normalizedStatus,
            "companyId" to resolvedCompanyId,
            "dryRun" to dryRun
        )
    }

    // ============================================================================
    // Discovery Classifier: finds untracked job applications from email evidence
    // ============================================================================

    /**
     * A candidate discovered application inferred from email evidence.
     */
    public data class DiscoveryCandidate(
        val company: String,
        val companyId: String,
        val role: String,
        val status: String,
        val messageId: String,
        val subject: String,
        val sender: String,
        val receivedAt: String?,
        val confidence: String,
        val relatedMessageIds: List<String>,
        val sourceLabel: String?
    )

    /**
     * Scans the emails collection and inbox file for untracked job applications.
     * Returns a list of discovery candidates that can be staged as proposals.
     */
    public fun discoverUntrackedApplications(
        inboxFile: File? = null,
        since: String? = null,
    ): List<DiscoveryCandidate> {
        val db = firestore

        // Build index of tracked company IDs
        val appsSnap = db.collection("applications").get().get()
        val trackedCompanyIds = mutableSetOf<String>()
        val trackedCompanies = mutableSetOf<String>()
        for (doc in appsSnap.documents) {
            val cid = doc.getString("companyId")
            if (!cid.isNullOrBlank()) {
                trackedCompanyIds.add(cid)
                val name = doc.getString("company")
                if (!name.isNullOrBlank()) {
                    trackedCompanies.add(name.lowercase())
                }
            }
        }

        // Load inbox file for label-based signals
        val inboxMessages = loadInboxMessages(inboxFile)
        val labelToMessages = mutableMapOf<String, MutableList<Map<String, Any?>>>()
        for (msg in inboxMessages) {
            val labels = (msg["labels"] as? List<*>)?.map { it.toString() } ?: emptyList()
            for (label in labels) {
                if (label.startsWith("JOB/")) {
                    labelToMessages.computeIfAbsent(label) { mutableListOf() }.add(msg)
                }
            }
        }

        // Scan emails collection for uncorrelated messages with job signals
        val emailsSnap = db.collection("emails").get().get()
        val uncorrelatedEmails = mutableListOf<Map<String, Any?>>()
        for (doc in emailsSnap.documents) {
            val data = doc.data ?: continue
            val appId = data["applicationId"] as? String
            val status = (data["status"] as? String) ?: "UNCLASSIFIED"
            if (appId.isNullOrBlank() && status != "NOISE") {
                val receivedAt = data["receivedAt"] as? String
                // Filter by date if specified
                if (!since.isNullOrBlank() && !receivedAt.isNullOrBlank()) {
                    if (receivedAt.substringBefore("T").substringBefore("t") < since) continue
                }
                uncorrelatedEmails.add(data)
            }
        }

        // Also scan inbox file for JOB/<Firm> labeled messages
        val jobLabelEmails = mutableListOf<Map<String, Any?>>()
        for ((label, messages) in labelToMessages) {
            for (msg in messages) {
                val messageId = msg["messageId"] as? String ?: continue
                // Check if this message is already correlated
                val emailDoc = db.collection("emails").document(messageId).get().get()
                val emailData = emailDoc.data ?: continue
                val appId = emailData["applicationId"] as? String
                if (appId.isNullOrBlank()) {
                    jobLabelEmails.add(emailData)
                }
            }
        }

        // Merge and deduplicate by messageId
        val allUncorrelated = mutableMapOf<String, Map<String, Any?>>()
        for (email in uncorrelatedEmails + jobLabelEmails) {
            val mid = (email["id"] as? String) ?: (email["messageId"] as? String) ?: continue
            allUncorrelated[mid] = email
        }

        // Group by company and extract candidates
        val candidates = mutableListOf<DiscoveryCandidate>()
        val companyGroups = mutableMapOf<String, MutableList<Map<String, Any?>>>()

        for (email in allUncorrelated.values) {
            val company = extractCompanyFromEmail(email, inboxMessages, labelToMessages)
            if (!company.isNullOrBlank() && !trackedCompanies.contains(company.lowercase())) {
                companyGroups.computeIfAbsent(company.lowercase()) { mutableListOf() }.add(email)
            }
        }

        for ((companyLower, emails) in companyGroups) {
            val company = companyLower.replaceFirstChar { it.uppercase() }
            val companyId = DocumentIdGenerator.slug(company)
            if (trackedCompanyIds.contains(companyId)) continue

            val status = inferStatusFromEmails(emails)
            val role = extractRoleFromEmails(emails, inboxMessages)
            val primaryEmail = emails.first()
            val messageId = (primaryEmail["id"] as? String) ?: (primaryEmail["messageId"] as? String) ?: ""
            val subject = primaryEmail["subject"] as? String ?: ""
            val sender = primaryEmail["from"] as? String ?: ""
            val receivedAt = primaryEmail["receivedAt"] as? String
            val sourceLabel = extractSourceLabel(primaryEmail, labelToMessages)

            // Find related emails
            val relatedIds = findRelatedEmails(companyId, messageId, allUncorrelated)

            candidates.add(
                DiscoveryCandidate(
                    company = company,
                    companyId = companyId,
                    role = role,
                    status = status,
                    messageId = messageId,
                    subject = subject,
                    sender = sender,
                    receivedAt = receivedAt,
                    confidence = if (sourceLabel != null) "HIGH_CONFIDENCE" else "MEDIUM_CONFIDENCE",
                    relatedMessageIds = relatedIds,
                    sourceLabel = sourceLabel
                )
            )
        }

        return candidates
    }

    private fun loadInboxMessages(inboxFile: File?): List<Map<String, Any?>> {
        val candidates = listOfNotNull(
            inboxFile,
            File("generated/email-inbox.json"),
            File("generated/gmail-inbox.json"),
            File("../generated/email-inbox.json"),
            File("../generated/gmail-inbox.json"),
            File("../../generated/email-inbox.json"),
            File("../../generated/gmail-inbox.json")
        )
        val file = candidates.firstOrNull { it.exists() } ?: return emptyList()
        return try {
            val content = file.readText()
            val parsed = Json.parseToJsonElement(content).jsonObject
            val messages = parsed["messages"]?.jsonArray ?: JsonArray(emptyList())
            messages.mapNotNull { msgElem ->
                val msg = msgElem.jsonObject
                mapOf<String, Any?>(
                    "messageId" to (msg["messageId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null),
                    "subject" to (msg["subject"]?.jsonPrimitive?.contentOrNull ?: ""),
                    "sender" to (msg["sender"]?.jsonPrimitive?.contentOrNull ?: ""),
                    "snippet" to (msg["snippet"]?.jsonPrimitive?.contentOrNull ?: ""),
                    "receivedAt" to (msg["receivedAt"]?.jsonPrimitive?.contentOrNull),
                    "labels" to (msg["labels"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()),
                    "body" to (msg["body"]?.jsonPrimitive?.contentOrNull ?: "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractCompanyFromEmail(
        email: Map<String, Any?>,
        inboxMessages: List<Map<String, Any?>>,
        labelToMessages: Map<String, MutableList<Map<String, Any?>>>
    ): String? {
        val messageId = (email["id"] as? String) ?: (email["messageId"] as? String) ?: ""
        val subject = (email["subject"] as? String) ?: ""
        val sender = (email["from"] as? String) ?: ""
        val snippet = (email["snippet"] as? String) ?: ""

        // 1. Check JOB/<Firm> labels from inbox file
        for ((label, messages) in labelToMessages) {
            if (label.startsWith("JOB/")) {
                for (msg in messages) {
                    if ((msg["messageId"] as? String) == messageId) {
                        return label.substringAfter("/")
                    }
                }
            }
        }

        // 2. Check inbox messages for label match
        for (msg in inboxMessages) {
            if ((msg["messageId"] as? String) == messageId) {
                val labels = (msg["labels"] as? List<*>)?.map { it.toString() } ?: emptyList()
                for (label in labels) {
                    if (label.startsWith("JOB/")) {
                        return label.substringAfter("/")
                    }
                }
            }
        }

        // 3. Extract from sender domain (ATS patterns)
        val atsDomains = mapOf(
            "greenhouse.io" to null,
            "lever.co" to null,
            "myworkday.com" to null,
            "ashbyhq.com" to null,
            "smartrecruiters.com" to null,
            "icims.com" to null,
            "bamboohr.com" to null,
            "workday.com" to null,
            "talent.neptun.so" to null
        )
        for ((domain, _) in atsDomains) {
            if (sender.contains(domain, ignoreCase = true)) {
                // Try to extract company from subject
                val companyPatterns = listOf(
                    Regex("update on your (?:job )?application at ([^\\s,]+)", RegexOption.IGNORE_CASE),
                    Regex("your application to ([^\\s,]+)", RegexOption.IGNORE_CASE),
                    Regex("application status at ([^\\s,]+)", RegexOption.IGNORE_CASE),
                    Regex("from ([^\\s,]+) recruiting", RegexOption.IGNORE_CASE),
                    Regex("from ([^\\s,]+) careers", RegexOption.IGNORE_CASE),
                    Regex("from ([^\\s,]+) talent", RegexOption.IGNORE_CASE),
                    Regex("from ([^\\s,]+) hiring", RegexOption.IGNORE_CASE),
                    Regex("from ([^\\s,]+) hr", RegexOption.IGNORE_CASE)
                )
                for (pattern in companyPatterns) {
                    val match = pattern.find(subject + " " + snippet)
                    if (match != null) {
                        val company = match.groupValues[1].trim()
                        if (company.length > 2) return company
                    }
                }
            }
        }

        // 4. Try to extract company name from subject line patterns
        val subjectCompanyPatterns = listOf(
            Regex("update on your (?:job )?application at ([^\\s,]+)", RegexOption.IGNORE_CASE),
            Regex("your application to ([^\\s,]+)", RegexOption.IGNORE_CASE),
            Regex("application status at ([^\\s,]+)", RegexOption.IGNORE_CASE),
            Regex("regarding your application at ([^\\s,]+)", RegexOption.IGNORE_CASE),
            Regex("your application with ([^\\s,]+)", RegexOption.IGNORE_CASE)
        )
        for (pattern in subjectCompanyPatterns) {
            val match = pattern.find(subject + " " + snippet)
            if (match != null) {
                val company = match.groupValues[1].trim()
                if (company.length > 2) return company
            }
        }

        return null
    }

    private fun extractRoleFromEmails(
        emails: List<Map<String, Any?>>,
        inboxMessages: List<Map<String, Any?>>
    ): String {
        val rolePatterns = listOf(
            Regex("position[:\\s]+([^\\n,]+)", RegexOption.IGNORE_CASE),
            Regex("role[:\\s]+([^\\n,]+)", RegexOption.IGNORE_CASE),
            Regex("job title[:\\s]+([^\\n,]+)", RegexOption.IGNORE_CASE),
            Regex("for the ([^\\s,]+) (?:position|role|opening)", RegexOption.IGNORE_CASE),
            Regex("as ([^\\s,]+) (?:engineer|developer|architect|lead|manager)", RegexOption.IGNORE_CASE),
            Regex("([A-Za-z]+ (?:Engineer|Developer|Architect|Lead|Manager|Director|Principal)) (?:position|role|opening)", RegexOption.IGNORE_CASE),
            Regex("application for ([^\\n,]+)", RegexOption.IGNORE_CASE),
            Regex("re: ([^\\n,]+) (?:application|position|role)", RegexOption.IGNORE_CASE)
        )

        for (email in emails) {
            val subject = (email["subject"] as? String) ?: ""
            val snippet = (email["snippet"] as? String) ?: ""
            val combined = subject + " " + snippet

            // Check inbox body for more context
            val messageId = (email["id"] as? String) ?: (email["messageId"] as? String) ?: ""
            for (msg in inboxMessages) {
                if ((msg["messageId"] as? String) == messageId) {
                    val body = msg["body"] as? String ?: ""
                    for (pattern in rolePatterns) {
                        val match = pattern.find(body)
                        if (match != null) {
                            val role = match.groupValues[1].trim()
                            if (role.length > 2 && role.length < 100) return role
                        }
                    }
                }
            }

            // Check subject and snippet
            for (pattern in rolePatterns) {
                val match = pattern.find(combined)
                if (match != null) {
                    val role = match.groupValues[1].trim()
                    if (role.length > 2 && role.length < 100) return role
                }
            }
        }

        return "Unknown / To Be Confirmed"
    }

    private fun inferStatusFromEmails(emails: List<Map<String, Any?>>): String {
        val rejectionKeywords = listOf(
            "unfortunately", "not moving forward", "regret", "no longer under consideration",
            "not selected", "won't be moving forward", "will not be moving forward",
            "decided to move forward with other", "decision to move forward with other",
            "move forward with other candidates", "difficult decision",
            "other candidates were more aligned",
            "position has been filled", "position was filled"
        )
        val offerKeywords = listOf("job offer", "pleased to offer", "offer letter", "we would like to offer")
        val interviewKeywords = listOf("interview", "phone screen", "schedule a call", "schedule a chat")
        val acknowledgementKeywords = listOf("thank you for applying", "application received", "we have received")

        for (email in emails) {
            val subject = (email["subject"] as? String) ?: ""
            val snippet = (email["snippet"] as? String) ?: ""
            val combined = "$subject $snippet".lowercase()

            if (rejectionKeywords.any { combined.contains(it) }) return "REJECTED"
            if (offerKeywords.any { combined.contains(it) }) return "OFFER"
            if (interviewKeywords.any { combined.contains(it) }) return "INTERVIEWING"
            if (acknowledgementKeywords.any { combined.contains(it) }) return "SUBMITTED"
        }

        return "SUBMITTED"
    }

    private fun extractSourceLabel(
        email: Map<String, Any?>,
        labelToMessages: Map<String, MutableList<Map<String, Any?>>>
    ): String? {
        val messageId = (email["id"] as? String) ?: (email["messageId"] as? String) ?: ""
        for ((label, messages) in labelToMessages) {
            if (label.startsWith("JOB/")) {
                for (msg in messages) {
                    if ((msg["messageId"] as? String) == messageId) {
                        return label
                    }
                }
            }
        }
        return null
    }

    /**
     * Searches for related emails by company/ATS sender to find missing breadcrumbs.
     */
    public fun findRelatedEmails(
        companyId: String,
        excludeMessageId: String,
        allEmails: Map<String, Map<String, Any?>>? = null
    ): List<String> {
        val db = firestore
        val related = mutableListOf<String>()

        // Search emails collection for messages from the same company
        val emailsSnap = db.collection("emails").get().get()
        for (doc in emailsSnap.documents) {
            val data = doc.data ?: continue
            val mid = (data["id"] as? String) ?: (data["messageId"] as? String) ?: continue
            if (mid == excludeMessageId) continue
            if ((data["companyId"] as? String) == companyId) {
                related.add(mid)
                continue
            }
            // Check by company name in subject/snippet
            val subject = (data["subject"] as? String) ?: ""
            val snippet = (data["snippet"] as? String) ?: ""
            val combined = "$subject $snippet".lowercase()
            val companyWords = companyId.split("-").filter { it.length >= 3 }
            if (companyWords.all { combined.contains(it) }) {
                related.add(mid)
            }
        }

        return related.distinct()
    }

    /**
     * Stages discovery proposals in the pending-proposals queue.
     */
    public fun stageDiscoveryProposals(
        candidates: List<DiscoveryCandidate>,
        dryRun: Boolean = false
    ): Map<String, Any> {
        if (candidates.isEmpty()) {
            return mapOf(
                "status" to "SUCCESS",
                "proposalsStaged" to 0,
                "message" to "No discovery candidates found"
            )
        }

        val db = firestore
        val timestamp = Instant.now().toString()
        val changes = mutableListOf<Map<String, Any?>>()

        for (candidate in candidates) {
            changes.add(
                mapOf(
                    "applicationId" to null,
                    "eventType" to "DISCOVER",
                    "statusFrom" to null,
                    "statusTo" to candidate.status,
                    "confidence" to candidate.confidence,
                    "discoveredCompany" to candidate.company,
                    "discoveredCompanyId" to candidate.companyId,
                    "discoveredRole" to candidate.role,
                    "sourceEmail" to mapOf(
                        "messageId" to candidate.messageId,
                        "subject" to candidate.subject,
                        "sender" to candidate.sender,
                        "receivedAt" to candidate.receivedAt
                    ),
                    "relatedEmails" to candidate.relatedMessageIds,
                    "sourceLabel" to candidate.sourceLabel
                )
            )
        }

        val proposalId = java.util.UUID.randomUUID().toString()
        val proposal = mapOf(
            "proposalId" to proposalId,
            "createdAt" to timestamp,
            "dueAt" to Instant.now().plus(1, ChronoUnit.DAYS).toString(),
            "collectedAt" to timestamp,
            "kind" to "DISCOVER",
            "state" to "PENDING",
            "trigger" to "DISCOVERY",
            "scope" to mapOf(
                "applicationIds" to emptyList<String>(),
                "lookbackFrom" to null,
                "query" to null
            ),
            "changes" to changes,
            "unmatched" to emptyList<Any>()
        )

        if (!dryRun) {
            db.collection("pending-proposals").document(proposalId).set(proposal)
        }

        return mapOf(
            "status" to "SUCCESS",
            "proposalsStaged" to 1,
            "proposalId" to proposalId,
            "candidates" to candidates.size,
            "dryRun" to dryRun
        )
    }

    /**
     * Ingests and correlates raw messages from generated/email-inbox.json into the top-level emails collection.
     */
    public fun backfillEmails(
        inboxFile: File? = null,
        dryRun: Boolean = false,
    ): Map<String, Any> {
        val candidates = listOfNotNull(
            inboxFile,
            File("generated/email-inbox.json"),
            File("generated/gmail-inbox.json"),
            File("../generated/email-inbox.json"),
            File("../generated/gmail-inbox.json"),
            File("../../generated/email-inbox.json"),
            File("../../generated/gmail-inbox.json")
        )
        val file = candidates.firstOrNull { it.exists() } ?: File("generated/email-inbox.json")
        if (!file.exists()) {
            return mapOf(
                "status" to "ERROR",
                "message" to "Inbox file not found at ${file.path}",
                "processed" to 0
            )
        }

        val db = firestore
        val content = file.readText()
        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(content).jsonObject
        val messages = parsed["messages"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())

        // Build application messageId index and company name index
        val appsSnap = db.collection("applications").get().get()
        val messageToApp = mutableMapOf<String, Pair<String, String?>>() // messageId -> (appId, companyId)
        val companyToApp = mutableMapOf<String, Pair<String, String?>>() // normalized company -> (appId, companyId)

        for (doc in appsSnap.documents) {
            val appId = doc.id
            val companyId = doc.getString("companyId")
            val companyName = doc.getString("company")?.trim() ?: ""
            if (companyName.isNotBlank()) {
                companyToApp[companyName.lowercase()] = appId to companyId
            }
            val ids = ((doc.get("emailMessageIds") as? List<*>) ?: emptyList<Any>()).map { it.toString() }
            for (id in ids) {
                messageToApp[id] = appId to companyId
            }

            // Check events subcollection for emailMessageId
            val eventsSnap = doc.reference.collection("events").get().get()
            for (ev in eventsSnap.documents) {
                val evEmailId = ev.getString("emailMessageId")
                if (!evEmailId.isNullOrBlank()) {
                    messageToApp[evEmailId] = appId to companyId
                }
            }
        }

        var created = 0
        var updated = 0
        var noiseCount = 0

        // Noise keywords use word-boundary matching to avoid false positives
        // (e.g., "order" matching "border" in HTML/CSS from ATS emails)
        val noiseKeywords = listOf(
            "receipt", "payment", "\\border\\b", "invoice", "statement", "deposit",
            "security alert", "sign-in", "free cloud", "cookie", "talent scout digest",
            "digest", "newsletter"
        )
        val noisePatterns = noiseKeywords.map { keyword ->
            if (keyword.startsWith("\\b")) {
                Regex(keyword.removePrefix("\\b").removeSuffix("\\b").let { "\\b$it\\b" }, RegexOption.IGNORE_CASE)
            } else {
                Regex(keyword, RegexOption.IGNORE_CASE)
            }
        }

        // Job signal phrases — if present, override noise classification
        val jobSignalPhrases = listOf(
            "update on your job application",
            "your application to",
            "application status",
            "thank you for applying",
            "application received",
            "unfortunately",
            "not moving forward",
            "no longer under consideration",
            "not selected",
            "position has been filled",
            "job offer",
            "pleased to offer",
            "interview",
            "phone screen",
            "schedule a call",
            "assessment",
            "offer letter",
        )
        val atsSenderDomains = listOf(
            "greenhouse.io", "greenhouse-mail.io", "lever.co", "myworkday.com", "ashbyhq.com",
            "smartrecruiters.com", "icims.com", "bamboohr.com",
            "workday.com", "talent.neptun.so",
        )

        val batch = db.batch()
        var batchSize = 0

        for (msgElem in messages) {
            val msg = msgElem.jsonObject
            val messageId = msg["messageId"]?.jsonPrimitive?.contentOrNull?.trim() ?: continue
            if (messageId.isEmpty()) continue

            val subject = msg["subject"]?.jsonPrimitive?.contentOrNull ?: ""
            val sender = msg["sender"]?.jsonPrimitive?.contentOrNull ?: ""
            val snippet = msg["snippet"]?.jsonPrimitive?.contentOrNull ?: ""
            val receivedAt = msg["receivedAt"]?.jsonPrimitive?.contentOrNull

            val combinedLower = "$subject $sender $snippet".lowercase()

            // Correlate with application
            val appCorrelation = messageToApp[messageId]
            var matchedAppId: String? = appCorrelation?.first
            var matchedCompanyId: String? = appCorrelation?.second

            // Check for job signals FIRST — overrides noise classification
            val hasJobSignals = jobSignalPhrases.any { combinedLower.contains(it) } ||
                    atsSenderDomains.any { sender.lowercase().contains(it) }

            // Check noise (only if no job signals)
            val isNoise = !hasJobSignals && noisePatterns.any { pattern -> pattern.containsMatchIn("$subject $snippet") }

            val initialStatus = when {
                isNoise -> {
                    noiseCount++
                    "NOISE"
                }
                combinedLower.contains("interview") || combinedLower.contains("phone screen") -> {
                    if (combinedLower.contains("confirmed") || combinedLower.contains("calendar")) {
                        "INTERVIEW_CONFIRMATION"
                    } else {
                        "INTERVIEW_REQUEST"
                    }
                }
                combinedLower.contains("offer") -> "OFFER"
                combinedLower.contains("regret") || combinedLower.contains("not moving forward") || combinedLower.contains("unfortunately") || combinedLower.contains("decision to move forward with other") || combinedLower.contains("move forward with other candidates") || combinedLower.contains("difficult decision") || combinedLower.contains("other candidates were more aligned") -> "REJECTION"
                combinedLower.contains("received your application") || combinedLower.contains("thank you for applying") -> "ACKNOWLEDGEMENT"
                combinedLower.contains("assessment") || combinedLower.contains("coding challenge") -> "ASSESSMENT"
                matchedAppId != null -> "CORRESPONDENCE"
                else -> "UNCLASSIFIED"
            }

            val docRef = db.collection("emails").document(messageId)
            val docData = mutableMapOf<String, Any?>(
                "id" to messageId,
                "from" to sender,
                "subject" to subject,
                "snippet" to snippet,
                "receivedAt" to receivedAt,
                "status" to initialStatus,
                "applicationId" to (if (initialStatus == "NOISE") null else matchedAppId),
                "companyId" to (if (initialStatus == "NOISE") null else matchedCompanyId),
                "updatedAt" to Instant.now().toString(),
            )

            if (!dryRun) {
                batch.set(docRef, docData, SetOptions.merge())
                batchSize++
                if (batchSize >= 450) {
                    batch.commit().get()
                    batchSize = 0
                }
            }
            created++
        }

        if (!dryRun && batchSize > 0) {
            batch.commit().get()
        }

        return mapOf(
            "status" to "SUCCESS",
            "processed" to messages.size,
            "createdOrUpdated" to created,
            "noise" to noiseCount,
            "dryRun" to dryRun
        )
    }
}
