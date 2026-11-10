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
import dev.fnc314.tools.jobsearch.parser.EventChainBuilder
import dev.fnc314.tools.jobsearch.parser.EventChainVerifier
import dev.fnc314.tools.jobsearch.tracker.v1.ApplicationChannel
import dev.fnc314.tools.jobsearch.tracker.v1.CompanyMetrics
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
            doc.getString("companyId")?.takeIf { it.isNotBlank() }
                ?: doc.getString("company")?.lowercase()?.replace(Regex("[^a-z0-9]+"), "-")?.trim('-')
                ?: "unknown"
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
     * Batch-enriches all company records in Firestore that are missing `sector` or `linkedinUrl`.
     * Idempotent: skips companies that already have both fields populated, and skips companies
     * enriched within the last 24 hours (based on `lastEnrichedAt`).
     */
    public fun batchEnrichCompanies(): Map<String, Any> {
        val db = firestore
        val snapshot = db.collection("companies").get().get()
        var enriched = 0
        var skipped = 0
        var errors = 0
        val errorDetails = mutableListOf<String>()

        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val companyId = doc.id
            val hasSector = !(data["sector"] as? String).isNullOrBlank()
            val hasLinkedinUrl = !(data["linkedinUrl"] as? String).isNullOrBlank()

            // Skip if both fields are already populated
            if (hasSector && hasLinkedinUrl) {
                skipped++
                continue
            }

            // Skip if enriched within the last 24 hours
            val lastEnrichedAt = data["lastEnrichedAt"] as? Timestamp
            if (lastEnrichedAt != null) {
                val elapsed = ChronoUnit.HOURS.between(lastEnrichedAt.toDate().toInstant(), Instant.now())
                if (elapsed < 24) {
                    skipped++
                    continue
                }
            }

            // Try to enrich via Hunter.io logo API (returns industry data)
            try {
                val companyName = (data["name"] as? String) ?: companyId
                val website = (data["website"] as? String) ?: run {
                    // Try to derive website from company name
                    val domain = companyName.lowercase().replace(Regex("[^a-z0-9]+"), ".")
                    "https://www.$domain.com"
                }

                val hunterResult = fetchHunterCompanyInfo(website)
                val hunterIndustry = hunterResult["industry"] as? String
                val hunterLinkedinUrl = hunterResult["linkedin_url"] as? String

                val enrichResult = enrichCompany(
                    companyId = companyId,
                    companyName = companyName,
                    linkedinUrl = hunterLinkedinUrl,
                    industry = hunterIndustry,
                    logoUrl = hunterResult["logo_url"] as? String
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
            "errorDetails" to errorDetails,
            "timestamp" to Timestamp.now().toString()
        )
    }

    /**
     * Fetches company info from Hunter.io API, including industry and LinkedIn URL.
     */
    private fun fetchHunterCompanyInfo(website: String): Map<String, Any?> {
        val apiKey = System.getenv("HUNTER_API_KEY")
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

        val apiUrl = "https://api.hunter.io/v2/companies?domain=$domain&api_key=$apiKey"
        val connection = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 10000

        try {
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.parseToJsonElement(response)
                val data = json.jsonObject["data"]?.jsonObject?.get("company")?.jsonObject ?: return emptyMap()

                return mapOf(
                    "industry" to (data["industry"]?.jsonPrimitive?.contentOrNull),
                    "linkedin_url" to (data["linkedin"]?.jsonPrimitive?.contentOrNull),
                    "logo_url" to (data["website_pages"]?.jsonObject?.get("logo")?.jsonPrimitive?.contentOrNull)
                )
            }
        } finally {
            connection.disconnect()
        }

        return emptyMap()
    }

    /**
     * Creates a new company record from name and optional website.
     * Derives slug from name, resolves careersUrl from website if provided.
     */
    public fun addCompany(name: String, website: String? = null): Map<String, Any?> {
        val db = firestore
        val slug = name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .takeIf { it.isNotBlank() }
            ?: "unknown-company"

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
     * Enriches a company record in Firestore with sector, logoUrl, and metadata.
     * Non-destructive and idempotent.
     */
    public fun enrichCompany(
        companyId: String,
        companyName: String,
        linkedinUrl: String? = null,
        industry: String? = null,
        logoUrl: String? = null,
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

        if (linkedinUrl != null && (existingData["linkedinUrl"] as? String).isNullOrBlank()) {
            updates["linkedinUrl"] = linkedinUrl
        }

        // Sector / Industry enrichment
        if (industry != null && (existingData["sector"] as? String).isNullOrBlank()) {
            val normalizedSector = when {
                industry.contains("finance", ignoreCase = true) || industry.contains("bank", ignoreCase = true) -> "FinTech"
                industry.contains("software", ignoreCase = true) || industry.contains("tech", ignoreCase = true) -> "Technology"
                industry.contains("health", ignoreCase = true) || industry.contains("medical", ignoreCase = true) -> "Healthcare"
                else -> industry
            }
            updates["sector"] = normalizedSector

            val existingMeta = (existingData["metadata"] as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
            existingMeta["linkedinIndustry"] = industry
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

    /**
     * Ingests and correlates raw messages from generated/gmail-inbox.json into the top-level emails collection.
     */
    public fun backfillEmails(
        inboxFile: File? = null,
        dryRun: Boolean = false,
    ): Map<String, Any> {
        val candidates = listOfNotNull(
            inboxFile,
            File("generated/gmail-inbox.json"),
            File("../generated/gmail-inbox.json"),
            File("../../generated/gmail-inbox.json")
        )
        val file = candidates.firstOrNull { it.exists() } ?: File("generated/gmail-inbox.json")
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

        val noiseKeywords = listOf(
            "receipt", "payment", "order", "invoice", "statement", "deposit",
            "security alert", "sign-in", "free cloud", "cookie", "talent scout digest",
            "digest", "newsletter"
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

            // Check noise
            val isNoise = noiseKeywords.any { combinedLower.contains(it) }

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
                combinedLower.contains("regret") || combinedLower.contains("not moving forward") || combinedLower.contains("unfortunately") -> "REJECTION"
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
